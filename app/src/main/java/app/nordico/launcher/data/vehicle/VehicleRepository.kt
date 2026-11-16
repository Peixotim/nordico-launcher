package app.nordico.launcher.data.vehicle

import kotlinx.coroutines.flow.StateFlow

/** Sinais do carro. `null` = a central não informa (caixa CAN sem esse dado). */
interface VehicleRepository {
    val climate: StateFlow<ClimateState?>
    val headlightsOn: StateFlow<Boolean?>
    val outsideTemp: StateFlow<Double?>
}
