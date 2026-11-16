package app.nordico.launcher.data.vehicle

enum class Airflow(val label: String) {
    Face("Rosto"),
    FaceFeet("Rosto e pés"),
    Feet("Pés"),
    FeetDefrost("Pés e para-brisa"),
}

/** Estado do ar-condicionado digital (Civic LXR, zona única). Spec 11.1. */
data class ClimateState(
    val setTemp: Double,
    val fanSpeed: Int,
    val auto: Boolean,
    val ac: Boolean,
    val airflow: Airflow,
    val recirc: Boolean,
    val defrostFront: Boolean,
    val defrostRear: Boolean,
)

val DEFAULT_CLIMATE = ClimateState(22.5, 3, auto = true, ac = true, airflow = Airflow.FaceFeet, recirc = false, defrostFront = false, defrostRear = false)

/** Botões físicos do painel do ar (usados pelo painel de simulação). */
enum class PanelButton { TempDown, TempUp, FanDown, FanUp, Auto, Ac, Airflow, Recirc, DefrostFront, DefrostRear }

fun ClimateState.press(b: PanelButton): ClimateState = when (b) {
    PanelButton.TempDown -> copy(setTemp = (setTemp - 0.5).coerceAtLeast(18.0))
    PanelButton.TempUp -> copy(setTemp = (setTemp + 0.5).coerceAtMost(32.0))
    PanelButton.FanDown -> copy(fanSpeed = (fanSpeed - 1).coerceAtLeast(1), auto = false)
    PanelButton.FanUp -> copy(fanSpeed = (fanSpeed + 1).coerceAtMost(7), auto = false)
    PanelButton.Auto -> copy(
        auto = true,
        fanSpeed = DEFAULT_CLIMATE.fanSpeed,
        airflow = DEFAULT_CLIMATE.airflow,
        ac = true,
    )
    PanelButton.Ac -> copy(ac = !ac)
    PanelButton.Airflow -> copy(airflow = Airflow.entries[(airflow.ordinal + 1) % Airflow.entries.size], auto = false)
    PanelButton.Recirc -> copy(recirc = !recirc)
    PanelButton.DefrostFront -> copy(defrostFront = !defrostFront)
    PanelButton.DefrostRear -> copy(defrostRear = !defrostRear)
}
