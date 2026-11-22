package app.nordico.launcher

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import app.nordico.launcher.data.apps.AppsRepository
import app.nordico.launcher.data.audio.VolumeRepository
import app.nordico.launcher.data.location.LocationRepository
import app.nordico.launcher.data.media.MediaRepository
import app.nordico.launcher.data.obd.AppVisibility
import app.nordico.launcher.data.obd.ElmObdRepository
import app.nordico.launcher.data.obd.ObdAdapter
import app.nordico.launcher.data.obd.ObdRepository
import app.nordico.launcher.data.prefs.PrefsRepository
import app.nordico.launcher.data.radio.RadioRepository
import app.nordico.launcher.data.status.StatusRepository
import app.nordico.launcher.data.vehicle.ClimateState
import app.nordico.launcher.data.vehicle.VehicleRepository
import app.nordico.launcher.device.BluetoothSppTransport
import app.nordico.launcher.device.DeviceAppsRepository
import app.nordico.launcher.device.DeviceLocationRepository
import app.nordico.launcher.device.DeviceMediaRepository
import app.nordico.launcher.device.DeviceRadioRepository
import app.nordico.launcher.device.DeviceStatusRepository
import app.nordico.launcher.device.DeviceVolumeRepository
import app.nordico.launcher.feature.map.NavApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Liga cada interface à sua implementação real: APIs do Android e o adaptador OBD-II. */
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

    private val bluetooth: BluetoothAdapter? = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val appContext = context

    /** Preenchido pela MainActivity: abre o pedido de permissão do Bluetooth. */
    var requestBluetoothPermission: () -> Unit = {}

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

    private val obdScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val obd: ObdRepository = ElmObdRepository(
        scope = obdScope,
        visible = AppVisibility.visible,
        adapters = ::bondedAdapters,
        selected = prefs.prefs.map { it.obdAdapter }.distinctUntilChanged(),
        select = prefs::setObdAdapter,
        transportFor = { address -> BluetoothSppTransport(bluetooth, address) },
    )

    /**
     * Só a temperatura externa vem do OBD-II (PID 0x46). Ar-condicionado e faróis não existem no OBD-II:
     * dependem da caixa CAN da central (fase 3) e ficam `null`, e a interface esconde o que o carro não informa.
     */
    val vehicle: VehicleRepository = object : VehicleRepository {
        override val climate: StateFlow<ClimateState?> = MutableStateFlow(null)
        override val headlightsOn: StateFlow<Boolean?> = MutableStateFlow(null)
        override val outsideTemp: StateFlow<Double?> =
            obd.telemetry.map { it.ambientC?.toDouble() }.stateIn(obdScope, SharingStarted.Eagerly, null)
    }

    val radio: RadioRepository = DeviceRadioRepository()

    /** Chamado no onResume da MainActivity: o usuário pode ter acabado de conceder acesso à música. */
    fun onResume() = deviceMedia.checkAccess()

    fun onLocationPermissionGranted() = deviceLocation.start()

    /** Apps de navegação instalados (pacotes de NavApp). */
    fun installedNavPackages(): Set<String> =
        NavApp.entries.map { it.pkg }.filter { packageManager.getLaunchIntentForPackage(it) != null }.toSet()
}
