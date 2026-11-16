package app.nordico.launcher.device

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import app.nordico.launcher.data.location.LatLng
import app.nordico.launcher.data.location.LocationRepository
import app.nordico.launcher.data.location.locationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Localização via LocationManager (sem Google Play Services): GPS se precisa, rede se aproximada. */
class DeviceLocationRepository(private val context: Context) : LocationRepository {
    private val _location = MutableStateFlow<LatLng?>(null)
    override val location: StateFlow<LatLng?> = _location
    private val manager = context.getSystemService(LocationManager::class.java)
    private val listener = LocationListener { _location.value = LatLng(it.latitude, it.longitude) }
    private var started = false

    @SuppressLint("MissingPermission")
    fun start() {
        if (started) return
        val granted = { p: String -> context.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED }
        val provider = locationProvider(
            fine = granted(Manifest.permission.ACCESS_FINE_LOCATION),
            coarse = granted(Manifest.permission.ACCESS_COARSE_LOCATION),
            available = manager.allProviders.toSet(),
        ) ?: return
        started = runCatching {
            manager.getLastKnownLocation(provider)?.let { _location.value = LatLng(it.latitude, it.longitude) }
            manager.requestLocationUpdates(provider, 1000L, 5f, listener, Looper.getMainLooper())
        }.isSuccess
    }
}
