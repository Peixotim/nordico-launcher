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
