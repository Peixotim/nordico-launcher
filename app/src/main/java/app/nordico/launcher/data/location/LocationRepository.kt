package app.nordico.launcher.data.location

import kotlinx.coroutines.flow.StateFlow

data class LatLng(val lat: Double, val lng: Double)

interface LocationRepository {
    val location: StateFlow<LatLng?>
}

/** Pedidas juntas (Android 12+): o usuário pode escolher "Precisa" ou "Aproximada". */
val LOCATION_PERMISSIONS = arrayOf(
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
)

/** GPS com permissão precisa; senão rede (aproximada ou central sem GPS); `null` se nada serve. */
fun locationProvider(fine: Boolean, coarse: Boolean, available: Set<String>): String? = when {
    fine && "gps" in available -> "gps"
    (fine || coarse) && "network" in available -> "network"
    else -> null
}
