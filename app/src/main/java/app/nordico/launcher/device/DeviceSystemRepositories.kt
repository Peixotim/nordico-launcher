package app.nordico.launcher.device

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import app.nordico.launcher.data.audio.VolumeOrigin
import app.nordico.launcher.data.audio.VolumeRepository
import app.nordico.launcher.data.audio.VolumeState
import app.nordico.launcher.data.audio.onSystemVolume
import app.nordico.launcher.data.audio.stepWithSystem
import app.nordico.launcher.data.audio.toUiLevel
import app.nordico.launcher.data.status.StatusRepository
import app.nordico.launcher.data.status.StatusState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

private const val VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
private const val MUTE_CHANGED = "android.media.STREAM_MUTE_CHANGED_ACTION"

/**
 * Volume do Android (STREAM_MUSIC) reescalado para 0–40.
 * Mudança que não foi gravada por nós (volante, teclas) vira origem External → aviso.
 * Fase 3: na central o volume pode ser do MCU, não do AudioManager.
 */
class DeviceVolumeRepository(context: Context) : VolumeRepository {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val stream = AudioManager.STREAM_MUSIC
    private val max = audio.getStreamMaxVolume(stream)
    private var lastWritten: Int? = null
    private val _state = MutableStateFlow(read(VolumeOrigin.Internal))
    override val state: StateFlow<VolumeState> = _state

    init {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) = onSystemChange()
        }
        val filter = IntentFilter().apply {
            addAction(VOLUME_CHANGED)
            addAction(MUTE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    // Trade-off: se o app abrir já mudo, o Android informa 0 e o nível real só aparece ao desmutar
    // (getLastAudibleStreamVolume é API oculta); raro o suficiente para não guardar o nível em disco.
    private fun read(origin: VolumeOrigin) =
        VolumeState(toUiLevel(audio.getStreamVolume(stream), max), audio.isStreamMute(stream), origin)

    private fun onSystemChange() {
        val next = onSystemVolume(_state.value, audio.getStreamVolume(stream), audio.isStreamMute(stream), max, lastWritten) ?: return
        lastWritten = null
        _state.value = next
    }

    override fun step(delta: Int) {
        val current = _state.value
        val (next, sys) = stepWithSystem(current, delta, max)
        lastWritten = sys
        if (current.muted && !next.muted) audio.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0)
        audio.setStreamVolume(stream, sys, 0)
        _state.value = next
    }

    override fun toggleMute() {
        audio.adjustStreamVolume(stream, AudioManager.ADJUST_TOGGLE_MUTE, 0)
        _state.update { it.copy(muted = !it.muted, origin = VolumeOrigin.Internal) }
    }
}

/** Ícones de Bluetooth, celular e Wi-Fi com o estado real. */
class DeviceStatusRepository(context: Context) : StatusRepository {
    private val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val _state = MutableStateFlow(StatusState(bluetoothOn(), cellular = false, wifi = false))
    override val state: StateFlow<StatusState> = _state

    init {
        context.getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = _state.update {
                    it.copy(
                        wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
                        cellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
                    )
                }

                override fun onLost(network: Network) = _state.update { it.copy(wifi = false, cellular = false) }
            },
        )
        ContextCompat.registerReceiver(
            context,
            object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) = _state.update { it.copy(bluetooth = bluetoothOn()) }
            },
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private fun bluetoothOn(): Boolean = runCatching { adapter?.isEnabled == true }.getOrDefault(false)
}
