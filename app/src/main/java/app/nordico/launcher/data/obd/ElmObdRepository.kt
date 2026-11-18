package app.nordico.launcher.data.obd

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

/** Espera antes da n-ésima nova tentativa: 2, 4, 8, 16 e no máximo 30 s (spec 10.1). */
fun backoffMs(attempt: Int): Long = minOf(30_000L, 2_000L shl attempt.coerceIn(0, 4))

private const val LOST_AFTER_FAILURES = 3
private const val IDLE_MS = 50L

/**
 * OBD sobre ELM327, independente do transporte. Só lê, e só enquanto o app está visível.
 * Uma sessão por vez: leituras do polling e dos códigos de falha passam pelo mesmo [Mutex].
 */
class ElmObdRepository(
    private val scope: CoroutineScope,
    visible: StateFlow<Boolean>,
    private val adapters: () -> List<ObdAdapter>,
    selected: Flow<String?>,
    private val select: suspend (String) -> Unit,
    private val transportFor: (String) -> ObdTransport,
    private val now: () -> Long = System::currentTimeMillis,
) : ObdRepository {
    private val _state = MutableStateFlow<ObdState>(ObdState.NoAdapter)
    private val _telemetry = MutableStateFlow(VehicleTelemetry())
    private val _faults = MutableStateFlow<List<Dtc>?>(null)
    override val state: StateFlow<ObdState> = _state
    override val telemetry: StateFlow<VehicleTelemetry> = _telemetry
    override val faults: StateFlow<List<Dtc>?> = _faults
    private val _clear = MutableStateFlow<ClearState>(ClearState.Idle)
    override val clearState: StateFlow<ClearState> = _clear

    private val lock = Mutex()
    @Volatile private var session: ObdSession? = null

    init {
        scope.launch {
            combine(selected, visible) { address, isVisible -> address to isVisible }
                .distinctUntilChanged()
                .collectLatest { (address, isVisible) ->
                    when {
                        address == null -> {
                            _state.value = ObdState.NoAdapter
                            _telemetry.value = VehicleTelemetry()
                        }
                        isVisible -> connectLoop(address)
                        // Fora da tela: a sessão foi cancelada e o transporte fechado; volta ao reaparecer.
                    }
                }
        }
    }

    override fun bondedAdapters(): List<ObdAdapter> = adapters()

    override fun select(address: String) {
        scope.launch { select.invoke(address) }
    }

    override fun readFaults() {
        val s = session ?: return
        scope.launch {
            val result = lock.withLock { s.readFaults() }
            if (result is ObdResult.Ok) _faults.value = result.value
        }
    }

    override fun clearFaults() {
        val previous = _clear.value
        if (previous == ClearState.Clearing) return
        val s = session
        val block = if (s == null) ClearBlock.NotConnected else clearGate(_state.value, _telemetry.value, _faults.value)
        if (s == null || block != null) {
            _clear.value = ClearState.Done(ClearOutcome.Blocked(block ?: ClearBlock.NotConnected))
            return
        }
        // Marca "apagando" já aqui: um segundo toque antes da corrotina começar é ignorado.
        if (!_clear.compareAndSet(previous, ClearState.Clearing)) return
        scope.launch { _clear.value = ClearState.Done(runClear(s)) }
    }

    override fun dismissClearResult() {
        if (_clear.value != ClearState.Clearing) _clear.value = ClearState.Idle
    }

    /**
     * Tudo sob o mesmo [lock]: a leitura contínua fica parada do `04` até a releitura, para a ECU ainda
     * reiniciando não gerar `NO DATA` que desativaria PIDs. Só diz "apagado" se a releitura vier vazia.
     */
    private suspend fun runClear(s: ObdSession): ClearOutcome = lock.withLock {
        // Repete a guarda: a velocidade pode ter mudado enquanto esperava a vez.
        clearGate(_state.value, _telemetry.value, _faults.value)?.let { return@withLock ClearOutcome.Blocked(it) }
        when (val reply = s.clearFaults()) {
            is ClearReply.Refused -> ClearOutcome.Refused(reply.nrc)
            ClearReply.NoResponse, is ClearReply.Failed -> ClearOutcome.NoResponse
            ClearReply.Accepted -> {
                delay(CLEAR_SETTLE_MS)
                val reread = s.readFaults()
                (s.read(ObdPid.Status) as? ObdResult.Ok)?.let { _telemetry.value = _telemetry.value.with(ObdPid.Status, it.value) }
                when {
                    reread !is ObdResult.Ok -> ClearOutcome.Unverified
                    else -> {
                        _faults.value = reread.value
                        if (reread.value.isEmpty()) ClearOutcome.Cleared else ClearOutcome.Returned(reread.value)
                    }
                }
            }
        }
    }

    private suspend fun connectLoop(address: String) {
        var attempt = 0
        while (true) {
            _state.value = ObdState.Connecting
            val transport = transportFor(address)
            try {
                transport.open()
                val s = ObdSession(transport)
                val init = lock.withLock { s.init() }
                if (init is ObdResult.Ok) {
                    attempt = 0
                    session = s
                    _telemetry.value = VehicleTelemetry()
                    _state.value = ObdState.Connected(init.value.protocol, init.value.adapter)
                    poll(s, init.value.supported)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: IOException) {
                // Cai para o estado Lost e tenta de novo.
            } finally {
                session = null
                transport.close()
            }
            _state.value = ObdState.Lost
            _telemetry.value = VehicleTelemetry()
            delay(backoffMs(attempt++))
        }
    }

    /** Lê os PIDs vencidos até a sessão falhar [LOST_AFTER_FAILURES] vezes seguidas. */
    private suspend fun poll(s: ObdSession, supported: Set<Int>) {
        val active = ObdPid.entries.filter { it.id in supported }.toMutableSet()
        val last = mutableMapOf<ObdPid, Long>()
        val strikes = StrikeCounter()
        var failures = 0
        while (true) {
            coroutineContext.ensureActive()
            val pid = nextPid(now(), last, active)
            if (pid == null) {
                delay(IDLE_MS)
                continue
            }
            last[pid] = now()
            when (val r = lock.withLock { s.read(pid) }) {
                is ObdResult.Ok -> {
                    failures = 0
                    strikes.ok(pid)
                    _telemetry.value = _telemetry.value.with(pid, r.value)
                }
                is ObdResult.Err ->
                    if (r.error == ObdError.NoData) {
                        failures = 0
                        if (strikes.noData(pid)) {
                            active -= pid
                            _telemetry.value = _telemetry.value.with(pid, null)
                        }
                    } else if (++failures >= LOST_AFTER_FAILURES) {
                        return
                    }
            }
        }
    }
}

private fun VehicleTelemetry.with(pid: ObdPid, v: Double?): VehicleTelemetry {
    val i = v?.roundToInt()
    return when (pid) {
        ObdPid.Speed -> copy(speedKmh = i)
        ObdPid.Rpm -> copy(rpm = i)
        ObdPid.Coolant -> copy(coolantC = i)
        ObdPid.Intake -> copy(intakeC = i)
        ObdPid.Load -> copy(loadPct = i)
        ObdPid.Throttle -> copy(throttlePct = i)
        ObdPid.Fuel -> copy(fuelPct = i)
        ObdPid.Voltage -> copy(batteryV = v)
        ObdPid.Status -> copy(milOn = v?.let { it >= 128 }, faultCount = i?.and(0x7F))
    }
}
