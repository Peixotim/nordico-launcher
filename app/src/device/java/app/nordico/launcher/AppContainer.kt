package app.nordico.launcher

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import app.nordico.launcher.data.apps.AppsRepository
import app.nordico.launcher.data.audio.VolumeRepository
import app.nordico.launcher.data.location.LocationRepository
import app.nordico.launcher.data.media.MediaRepository
import app.nordico.launcher.data.obd.AppVisibility
import app.nordico.launcher.data.obd.ElmObdRepository
import app.nordico.launcher.data.obd.ObdAdapter
import app.nordico.launcher.data.obd.ObdRepository
import app.nordico.launcher.data.prefs.PrefsRepository
import app.nordico.launcher.data.status.StatusRepository
import app.nordico.launcher.data.vehicle.ClimateState
import app.nordico.launcher.data.vehicle.VehicleRepository
import app.nordico.launcher.device.BluetoothSppTransport
import app.nordico.launcher.device.DeviceAppsRepository
import app.nordico.launcher.device.DeviceLocationRepository
import app.nordico.launcher.device.DeviceMediaRepository
import app.nordico.launcher.device.DeviceStatusRepository
import app.nordico.launcher.device.DeviceVolumeRepository
import app.nordico.launcher.feature.map.NavApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Flavor `device`: APIs reais do Android. */
class AppContainer(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val deviceMedia = DeviceMediaRepository(context, scope)
    private val deviceLocation = DeviceLocationRepository(context)
    private val packageManager = context.packageManager

    val media: MediaRepository = deviceMedia
    val apps: AppsRepository = DeviceAppsRepository(context, scope)
    val volume: VolumeRepository = DeviceVolumeRepository(context)
    val status: StatusRepository = DeviceStatusRepository(context)
    val location: LocationRepository = deviceLocation
    val prefs = PrefsRepository(context)

    /** Fase 3: caixa CAN. Até lá a central não informa nada do carro. */
    val vehicle: VehicleRepository = object : VehicleRepository {
        override val climate: StateFlow<ClimateState?> = MutableStateFlow(null)
        override val headlightsOn: StateFlow<Boolean?> = MutableStateFlow(null)
        override val outsideTemp: StateFlow<Double?> = MutableStateFlow(null)
    }

    private val bluetooth: BluetoothAdapter? = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val appContext = context

    /** `true` com a permissão BLUETOOTH_CONNECT concedida (Android 12+). */
    fun hasBluetoothPermission(): Boolean =
        appContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    /** Pareados no Android. Vazio sem permissão ou sem Bluetooth; nesse caso o estado fica "sem adaptador". */
    @SuppressLint("MissingPermission")
    private fun bondedAdapters(): List<ObdAdapter> {
        val adapter = bluetooth ?: return emptyList()
        if (!hasBluetoothPermission()) return emptyList()
        return runCatching { adapter.bondedDevices.map { ObdAdapter(it.name ?: it.address, it.address) } }
            .getOrDefault(emptyList())
    }

    val obd: ObdRepository = ElmObdRepository(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        visible = AppVisibility.visible,
        adapters = ::bondedAdapters,
        selected = prefs.prefs.map { it.obdAdapter }.distinctUntilChanged(),
        select = prefs::setObdAdapter,
        transportFor = { address -> BluetoothSppTransport(bluetooth, address) },
    )

    val devPanel: (@Composable (onClose: () -> Unit) -> Unit)? = null

    /** Chamado no onResume da MainActivity: o usuário pode ter acabado de conceder acesso à música. */
    fun onResume() = deviceMedia.checkAccess()

    fun onLocationPermissionGranted() = deviceLocation.start()

    /** Apps de navegação instalados (pacotes de NavApp). */
    fun installedNavPackages(): Set<String> =
        NavApp.entries.map { it.pkg }.filter { packageManager.getLaunchIntentForPackage(it) != null }.toSet()
}
