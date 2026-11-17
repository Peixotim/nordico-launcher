package app.nordico.launcher

import android.content.Context
import androidx.compose.runtime.Composable
import app.nordico.launcher.data.apps.AppsRepository
import app.nordico.launcher.data.audio.VolumeRepository
import app.nordico.launcher.data.location.LocationRepository
import app.nordico.launcher.data.media.MediaRepository
import app.nordico.launcher.data.obd.ObdRepository
import app.nordico.launcher.data.prefs.PrefsRepository
import app.nordico.launcher.data.status.StatusRepository
import app.nordico.launcher.data.vehicle.VehicleRepository
import app.nordico.launcher.sim.SimAppsRepository
import app.nordico.launcher.sim.SimLocationRepository
import app.nordico.launcher.sim.SimMediaRepository
import app.nordico.launcher.sim.SimObdRepository
import app.nordico.launcher.sim.SimPanel
import app.nordico.launcher.sim.SimStatusRepository
import app.nordico.launcher.sim.SimVehicleRepository
import app.nordico.launcher.sim.SimVolumeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Flavor `sim`: tudo simulado, para rodar no emulador. */
class AppContainer(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val simVolume = SimVolumeRepository()
    val simVehicle = SimVehicleRepository()
    val simObd = SimObdRepository()

    val media: MediaRepository = SimMediaRepository(context, scope)
    val apps: AppsRepository = SimAppsRepository()
    val volume: VolumeRepository = simVolume
    val vehicle: VehicleRepository = simVehicle
    val obd: ObdRepository = simObd
    val status: StatusRepository = SimStatusRepository()
    val location: LocationRepository = SimLocationRepository()
    val prefs = PrefsRepository(context)

    fun onLocationPermissionGranted() = Unit

    fun onResume() = Unit

    /** No emulador finge que o Waze está instalado para mostrar Casa/Trabalho como no protótipo. */
    fun installedNavPackages(): Set<String> = setOf("com.waze")

    /** Painel de simulação: toque longo no relógio. */
    val devPanel: (@Composable (onClose: () -> Unit) -> Unit)? = { onClose -> SimPanel(this, onClose) }
}
