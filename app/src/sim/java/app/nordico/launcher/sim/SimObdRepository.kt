package app.nordico.launcher.sim

import app.nordico.launcher.data.obd.ClearOutcome
import app.nordico.launcher.data.obd.ClearState
import app.nordico.launcher.data.obd.Dtc
import app.nordico.launcher.data.obd.clearGate
import app.nordico.launcher.data.obd.ObdAdapter
import app.nordico.launcher.data.obd.ObdRepository
import app.nordico.launcher.data.obd.ObdState
import app.nordico.launcher.data.obd.VehicleTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class ObdScenario { NoAdapter, Idle, Highway, Overheating, WeakBattery, Faults, FaultsPersistent, ClearRefused }

private enum class ClearMode { Success, Persistent, Refused }

/** OBD simulado para a interface: cenários trocados pelo painel de simulação. */
class SimObdRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined),
    private val settleMs: Long = 1_200,
) : ObdRepository {
    private val connected = ObdState.Connected(protocol = "A6", adapter = "ELM327 v1.5 (sim)")
    private val _state = MutableStateFlow<ObdState>(connected)
    private val _telemetry = MutableStateFlow(IDLE)
    private val _faults = MutableStateFlow<List<Dtc>?>(null)
    override val state: StateFlow<ObdState> = _state
    override val telemetry: StateFlow<VehicleTelemetry> = _telemetry
    override val faults: StateFlow<List<Dtc>?> = _faults
    private val _clear = MutableStateFlow<ClearState>(ClearState.Idle)
    override val clearState: StateFlow<ClearState> = _clear
    private var clearMode = ClearMode.Success

    fun scenario(s: ObdScenario) {
        _clear.value = ClearState.Idle
        clearMode = when (s) {
            ObdScenario.FaultsPersistent -> ClearMode.Persistent
            ObdScenario.ClearRefused -> ClearMode.Refused
            else -> ClearMode.Success
        }
        if (s == ObdScenario.NoAdapter) {
            _state.value = ObdState.NoAdapter
            _telemetry.value = VehicleTelemetry()
            return
        }
        _state.value = connected
        _telemetry.value = when (s) {
            ObdScenario.Idle -> IDLE
            ObdScenario.Highway -> IDLE.copy(speedKmh = 92, rpm = 2400, loadPct = 45, throttlePct = 28, intakeC = 34)
            ObdScenario.Overheating -> IDLE.copy(coolantC = 108, rpm = 900)
            ObdScenario.WeakBattery -> IDLE.copy(batteryV = 11.6)
            ObdScenario.Faults, ObdScenario.FaultsPersistent, ObdScenario.ClearRefused -> IDLE.copy(milOn = true, faultCount = 2)
            ObdScenario.NoAdapter -> VehicleTelemetry()
        }
    }

    override fun bondedAdapters(): List<ObdAdapter> =
        listOf(ObdAdapter("OBDII", "00:1D:A5:68:98:8B"), ObdAdapter("Viecar", "AA:BB:CC:DD:EE:FF"))

    override fun select(address: String) {
        if (_state.value == ObdState.NoAdapter) scenario(ObdScenario.Idle)
    }

    override fun readFaults() {
        _faults.value = CODES
    }

    override fun clearFaults() {
        if (_clear.value == ClearState.Clearing) return
        clearGate(_state.value, _telemetry.value, _faults.value)?.let {
            _clear.value = ClearState.Done(ClearOutcome.Blocked(it))
            return
        }
        _clear.value = ClearState.Clearing
        scope.launch {
            delay(settleMs)
            _clear.value = ClearState.Done(
                when (clearMode) {
                    ClearMode.Success -> {
                        _faults.value = emptyList()
                        _telemetry.value = _telemetry.value.copy(milOn = false, faultCount = 0)
                        ClearOutcome.Cleared
                    }
                    ClearMode.Persistent -> {
                        _faults.value = CODES
                        ClearOutcome.Returned(CODES)
                    }
                    ClearMode.Refused -> ClearOutcome.Refused(0x22)
                },
            )
        }
    }

    override fun dismissClearResult() {
        if (_clear.value != ClearState.Clearing) _clear.value = ClearState.Idle
    }

    private companion object {
        val CODES = listOf(Dtc("P0133"), Dtc("P0420"))
        val IDLE = VehicleTelemetry(
            speedKmh = 0, rpm = 800, coolantC = 88, intakeC = 31,
            loadPct = 22, throttlePct = 14, fuelPct = 62, batteryV = 14.1, milOn = false, faultCount = 0,
        )
    }
}
