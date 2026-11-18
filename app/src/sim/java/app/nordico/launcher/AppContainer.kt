package app.nordico.launcher

import android.content.Context
import androidx.compose.runtime.Composable
import app.nordico.launcher.data.apps.AppsRepository
import app.nordico.launcher.data.audio.VolumeRepository
import app.nordico.launcher.data.location.LocationRepository
import app.nordico.launcher.data.media.MediaRepository
import app.nordico.launcher.data.obd.AppVisibility
import app.nordico.launcher.data.obd.ElmObdRepository
import app.nordico.launcher.data.obd.ObdAdapter
import app.nordico.launcher.data.obd.ObdRepository
import app.nordico.launcher.data.obd.TcpTransport
import app.nordico.launcher.data.prefs.PrefsRepository
import app.nordico.launcher.data.radio.RadioRepository
import app.nordico.launcher.data.status.StatusRepository
import app.nordico.launcher.data.vehicle.VehicleRepository
import app.nordico.launcher.sim.SimAppsRepository
import app.nordico.launcher.sim.SimLocationRepository
import app.nordico.launcher.sim.SimMediaRepository
import app.nordico.launcher.sim.SimObdRepository
import app.nordico.launcher.sim.SimRadioRepository
import app.nordico.launcher.sim.SimPanel
import app.nordico.launcher.sim.SimStatusRepository
import app.nordico.launcher.sim.SimVehicleRepository
import app.nordico.launcher.sim.SimVolumeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flowOf

/** Flavor `sim`: tudo simulado, para rodar no emulador. */
class AppContainer(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val simVolume = SimVolumeRepository()
    val simVehicle = SimVehicleRepository()
    val simObd = SimObdRepository(scope)

    val media: MediaRepository = SimMediaRepository(context, scope)
    val apps: AppsRepository = SimAppsRepository()
    val volume: VolumeRepository = simVolume
    val vehicle: VehicleRepository = simVehicle
    val obd: ObdRepository = obdOverTcp() ?: simObd
    val radio: RadioRepository = SimRadioRepository()
    val status: StatusRepository = SimStatusRepository()
    val location: LocationRepository = SimLocationRepository()
    val prefs = PrefsRepository(context)

    /** Com `-PobdTcp=host:porta` usa o protocolo real contra um simulador de ELM327 (docs: fase 2, Task 4). */
    private fun obdOverTcp(): ObdRepository? {
        val parts = BuildConfig.OBD_TCP.split(":")
        val port = parts.getOrNull(1)?.toIntOrNull() ?: return null
        val host = parts[0].ifBlank { return null }
        val address = BuildConfig.OBD_TCP
        return ElmObdRepository(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            visible = AppVisibility.visible,
            adapters = { listOf(ObdAdapter("ELM327 (TCP)", address)) },
            selected = flowOf(address),
            select = {},
            transportFor = { TcpTransport(host, port) },
        )
    }

    /** O emulador não usa Bluetooth: a permissão nunca é pedida. */
    var requestBluetoothPermission: () -> Unit = {}

    fun hasBluetoothPermission() = true

    fun onLocationPermissionGranted() = Unit

    fun onResume() = Unit

    /** No emulador finge que o Waze está instalado para mostrar Casa/Trabalho como no protótipo. */
    fun installedNavPackages(): Set<String> = setOf("com.waze")

    /** Painel de simulação: toque longo no relógio. */
    val devPanel: (@Composable (onClose: () -> Unit) -> Unit)? = { onClose -> SimPanel(this, onClose) }
}
