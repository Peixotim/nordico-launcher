package app.nordico.launcher.data.obd

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Leituras do carro. `null` = o carro não suporta ou ainda não houve leitura. */
data class VehicleTelemetry(
    val speedKmh: Int? = null,
    val rpm: Int? = null,
    val coolantC: Int? = null,
    val intakeC: Int? = null,
    val loadPct: Int? = null,
    val throttlePct: Int? = null,
    val fuelPct: Int? = null,
    val batteryV: Double? = null,
)

sealed interface ObdState {
    data object NoAdapter : ObdState
    data object Connecting : ObdState
    data class Connected(val protocol: String, val adapter: String) : ObdState
    data object Lost : ObdState
}

data class ObdAdapter(val name: String, val address: String)

interface ObdRepository {
    val state: StateFlow<ObdState>
    val telemetry: StateFlow<VehicleTelemetry>

    /** Códigos de falha; `null` enquanto não foram lidos. Somente leitura. */
    val faults: StateFlow<List<Dtc>?>

    /** Dispositivos Bluetooth já pareados (vazio sem permissão ou sem Bluetooth). */
    fun bondedAdapters(): List<ObdAdapter>

    fun select(address: String)

    fun readFaults()
}

/** Sem adaptador: o estado do flavor `device` até existir um adaptador selecionado. */
class NoAdapterObdRepository : ObdRepository {
    override val state: StateFlow<ObdState> = MutableStateFlow(ObdState.NoAdapter)
    override val telemetry: StateFlow<VehicleTelemetry> = MutableStateFlow(VehicleTelemetry())
    override val faults: StateFlow<List<Dtc>?> = MutableStateFlow(null)
    override fun bondedAdapters(): List<ObdAdapter> = emptyList()
    override fun select(address: String) = Unit
    override fun readFaults() = Unit
}
