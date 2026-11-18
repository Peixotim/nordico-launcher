package app.nordico.launcher.sim

import app.nordico.launcher.data.obd.Dtc
import app.nordico.launcher.data.obd.ObdAdapter
import app.nordico.launcher.data.obd.ObdRepository
import app.nordico.launcher.data.obd.ObdState
import app.nordico.launcher.data.obd.VehicleTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ObdScenario { NoAdapter, Idle, Highway, Overheating, WeakBattery, Faults }

/** OBD simulado para a interface: cenários trocados pelo painel de simulação. */
class SimObdRepository : ObdRepository {
    private val connected = ObdState.Connected(protocol = "A6", adapter = "ELM327 v1.5 (sim)")
    private val _state = MutableStateFlow<ObdState>(connected)
    private val _telemetry = MutableStateFlow(IDLE)
    private val _faults = MutableStateFlow<List<Dtc>?>(null)
    override val state: StateFlow<ObdState> = _state
    override val telemetry: StateFlow<VehicleTelemetry> = _telemetry
    override val faults: StateFlow<List<Dtc>?> = _faults

    fun scenario(s: ObdScenario) {
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
            ObdScenario.Faults -> IDLE.copy(milOn = true, faultCount = 2)
            ObdScenario.NoAdapter -> VehicleTelemetry()
        }
    }

    override fun bondedAdapters(): List<ObdAdapter> =
        listOf(ObdAdapter("OBDII", "00:1D:A5:68:98:8B"), ObdAdapter("Viecar", "AA:BB:CC:DD:EE:FF"))

    override fun select(address: String) {
        if (_state.value == ObdState.NoAdapter) scenario(ObdScenario.Idle)
    }

    override fun readFaults() {
        _faults.value = listOf(Dtc("P0133"), Dtc("P0420"))
    }

    private companion object {
        val IDLE = VehicleTelemetry(
            speedKmh = 0, rpm = 800, coolantC = 88, intakeC = 31,
            loadPct = 22, throttlePct = 14, fuelPct = 62, batteryV = 14.1, milOn = false, faultCount = 0,
        )
    }
}
