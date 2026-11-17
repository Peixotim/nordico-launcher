package app.nordico.launcher.sim

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import app.nordico.launcher.R
import app.nordico.launcher.data.apps.AppEntry
import app.nordico.launcher.data.apps.AppSection
import app.nordico.launcher.data.apps.AppsRepository
import app.nordico.launcher.data.audio.VolumeOrigin
import app.nordico.launcher.data.audio.VolumeRepository
import app.nordico.launcher.data.audio.VolumeState
import app.nordico.launcher.data.audio.stepped
import app.nordico.launcher.data.location.LatLng
import app.nordico.launcher.data.location.LocationRepository
import app.nordico.launcher.data.media.MediaAction
import app.nordico.launcher.data.media.MediaRepository
import app.nordico.launcher.data.media.NowPlaying
import app.nordico.launcher.data.status.StatusRepository
import app.nordico.launcher.data.status.StatusState
import app.nordico.launcher.data.vehicle.ClimateState
import app.nordico.launcher.data.vehicle.DEFAULT_CLIMATE
import app.nordico.launcher.data.vehicle.PanelButton
import app.nordico.launcher.data.vehicle.VehicleRepository
import app.nordico.launcher.data.vehicle.press
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SimVolumeRepository : VolumeRepository {
    private val _state = MutableStateFlow(VolumeState(18, false))
    override val state: StateFlow<VolumeState> = _state

    override fun step(delta: Int) = _state.update { it.stepped(delta) }

    override fun toggleMute() = _state.update { it.copy(muted = !it.muted, origin = VolumeOrigin.Internal) }

    /** Simula o comando de volume do volante. */
    fun simulateExternal(delta: Int) = _state.update { it.stepped(delta).copy(origin = VolumeOrigin.External) }
}

class SimVehicleRepository : VehicleRepository {
    private var lastClimate: ClimateState = DEFAULT_CLIMATE
    private val _climate = MutableStateFlow<ClimateState?>(DEFAULT_CLIMATE)
    private val _headlights = MutableStateFlow<Boolean?>(null)
    override val climate: StateFlow<ClimateState?> = _climate
    override val headlightsOn: StateFlow<Boolean?> = _headlights
    override val outsideTemp: StateFlow<Double?> = MutableStateFlow(27.0)

    fun press(b: PanelButton) {
        _climate.value?.let { _climate.value = it.press(b).also { c -> lastClimate = c } }
    }

    fun setHeadlights(on: Boolean?) {
        _headlights.value = on
    }

    fun setClimateAvailable(available: Boolean) {
        _climate.value = if (available) lastClimate else null
    }
}

class SimMediaRepository(context: Context, scope: CoroutineScope) : MediaRepository {
    private val art = BitmapFactory.decodeResource(context.resources, R.drawable.sim_art)?.asImageBitmap()
    private val _now = MutableStateFlow<NowPlaying?>(
        NowPlaying("Evidências", "Chitãozinho & Xororó", art, 102_000, 278_000, playing = true, source = "iPhone · Bluetooth"),
    )
    override val nowPlaying: StateFlow<NowPlaying?> = _now
    override val hasAccess: StateFlow<Boolean> = MutableStateFlow(true)
    private val _actions = MutableStateFlow(
        listOf(
            MediaAction("shuffle", "Aleatório", "shuffle", false),
            MediaAction("repeat", "Repetir", "repeat", false),
            MediaAction("like", "Curtir", "favorite", false),
        ),
    )
    override val actions: StateFlow<List<MediaAction>> = _actions

    init {
        scope.launch {
            while (isActive) {
                delay(1000)
                _now.update { np ->
                    if (np == null || !np.playing) np else np.copy(positionMs = (np.positionMs + 1000) % np.durationMs)
                }
            }
        }
    }

    override fun playPause() = _now.update { it?.copy(playing = !it.playing) }
    override fun next() = _now.update { it?.copy(positionMs = 0) }
    override fun previous() = _now.update { it?.copy(positionMs = 0) }
    override fun seekTo(ms: Long) = _now.update { np -> np?.copy(positionMs = ms.coerceIn(0, np.durationMs)) }
    override fun perform(id: String) = _actions.update { list -> list.map { if (it.id == id) it.copy(active = !it.active) else it } }
}

class SimAppsRepository : AppsRepository {
    override val apps: StateFlow<List<AppEntry>> = MutableStateFlow(
        listOf(
            brand("spotify", "Spotify", "music_note", 0xFF1DB954),
            brand("waze", "Waze", "navigation", 0xFF2BB3E8),
            brand("maps", "Google Maps", "map", 0xFF4285F4),
            brand("ytmusic", "YouTube Music", "play_circle", 0xFFE62117),
            brand("zlink", "CarPlay / AA", "cast", 0xFF3A3A3C),
            brand("igo", "iGO", "explore", 0xFF00897B),
            brand("youtube", "YouTube", "smart_display", 0xFFE62117),
            brand("playstore", "Play Store", "shop", 0xFF01875F),
            car("radio", "Rádio", "radio"),
            car("music", "Música", "library_music"),
            car("videos", "Vídeos", "movie"),
            car("bluetooth", "Bluetooth", "bluetooth"),
            car("phone", "Telefone", "call"),
            car("eq", "Equalizador", "equalizer"),
            car("camera360", "Câmera 360", "360"),
            car("aux", "AUX / AV", "input"),
            car("voice", "Voz", "mic"),
            car("files", "Arquivos", "folder"),
            car("gallery", "Galeria", "photo_library"),
            car("car", "Carro", "directions_car"),
            car("settings", "Ajustes", "settings"),
        ),
    )

    override fun launch(app: AppEntry) = Unit

    private fun brand(id: String, label: String, glyph: String, color: Long) =
        AppEntry(id, label, AppSection.Mine, glyph = glyph, brandColor = Color(color))

    private fun car(id: String, label: String, glyph: String) = AppEntry(id, label, AppSection.Car, glyph = glyph)
}

class SimStatusRepository : StatusRepository {
    override val state: StateFlow<StatusState> = MutableStateFlow(StatusState(bluetooth = true, cellular = true, wifi = true))
}

class SimLocationRepository : LocationRepository {
    override val location: StateFlow<LatLng?> = MutableStateFlow(LatLng(-23.5614, -46.6558))
}
