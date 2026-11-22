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
    val ambientC: Int? = null,
    /** Luz do motor acesa e quantidade de falhas armazenadas (PID 01). */
    val milOn: Boolean? = null,
    val faultCount: Int? = null,
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

    /** Códigos de falha; `null` enquanto não foram lidos. */
    val faults: StateFlow<List<Dtc>?>

    /** Andamento e resultado de "apagar falhas" (modo 04). */
    val clearState: StateFlow<ClearState>

    /** Dispositivos Bluetooth já pareados (vazio sem permissão ou sem Bluetooth). */
    fun bondedAdapters(): List<ObdAdapter>

    fun select(address: String)

    fun readFaults()

    /** Apaga **todas** as falhas, só com o carro parado e conectado. O resultado vem em [clearState]. */
    fun clearFaults()

    /** Esconde o resultado de [clearFaults]. */
    fun dismissClearResult()
}
