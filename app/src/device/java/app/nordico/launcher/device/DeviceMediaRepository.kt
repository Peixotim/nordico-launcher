package app.nordico.launcher.device

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import app.nordico.launcher.data.media.MediaRepository
import app.nordico.launcher.data.media.NowPlaying
import app.nordico.launcher.data.media.livePosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Serviço vazio exigido pelo Android para ler as sessões de mídia de outros apps. */
class MediaListenerService : NotificationListenerService()

/**
 * Mídia real via MediaSession (Spotify, Bluetooth, Chrome…). Precisa de acesso a notificações.
 * Metadados e estado ficam em cache (chegam pelos callbacks); o relógio de 1 s só recalcula a
 * posição a partir do cache e só roda enquanto alguma tela está coletando `nowPlaying`.
 */
class DeviceMediaRepository(private val context: Context, scope: CoroutineScope) : MediaRepository {
    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val component = ComponentName(context, MediaListenerService::class.java)
    private val main = Handler(Looper.getMainLooper())
    private val _now = MutableStateFlow<NowPlaying?>(null)
    private val _access = MutableStateFlow(false)
    override val nowPlaying: StateFlow<NowPlaying?> = _now
    override val hasAccess: StateFlow<Boolean> = _access

    private var controller: MediaController? = null
    private var registered = false

    // Cache: atualizado só pelos callbacks.
    private var title = ""
    private var artist = ""
    private var art: ImageBitmap? = null
    private var durationMs = 0L
    private var source = ""
    private var state: PlaybackState? = null

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { pick(it.orEmpty()) }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            cacheMetadata(metadata)
            publish()
        }

        override fun onPlaybackStateChanged(s: PlaybackState?) {
            state = s
            publish()
        }

        override fun onSessionDestroyed() = pick(activeSessions())
    }

    init {
        scope.launch {
            _now.subscriptionCount.map { it > 0 }.distinctUntilChanged().collectLatest { watched ->
                while (watched) {
                    delay(1000)
                    if (_now.value?.playing == true) publish()
                }
            }
        }
    }

    /** Chamado ao voltar para o launcher: o usuário pode ter concedido ou revogado o acesso. */
    fun checkAccess() {
        val ok = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        _access.value = ok
        if (ok && !registered) {
            registered = runCatching { manager.addOnActiveSessionsChangedListener(sessionsListener, component, main) }.isSuccess
            pick(activeSessions())
        } else if (!ok && registered) {
            runCatching { manager.removeOnActiveSessionsChangedListener(sessionsListener) }
            registered = false
            pick(emptyList())
        }
    }

    /** Com o acesso revogado o sistema lança SecurityException; nesse caso não há sessões. */
    private fun activeSessions(): List<MediaController> = runCatching { manager.getActiveSessions(component) }.getOrDefault(emptyList())

    private fun pick(sessions: List<MediaController>) {
        val chosen = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: sessions.firstOrNull()
        if (chosen?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = chosen
            chosen?.registerCallback(callback, main)
            source = chosen?.let { appLabel(it.packageName) }.orEmpty()
            cacheMetadata(chosen?.metadata)
            state = chosen?.playbackState
        }
        publish()
    }

    private fun cacheMetadata(meta: MediaMetadata?) {
        title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
        durationMs = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        art = (meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART))?.asImageBitmap()
    }

    /** Monta o NowPlaying a partir do cache (sem chamadas ao sistema). */
    private fun publish() {
        if (controller == null) {
            _now.value = null
            return
        }
        val s = state
        val playing = s?.state == PlaybackState.STATE_PLAYING
        _now.value = NowPlaying(
            title = title,
            artist = artist,
            art = art,
            positionMs = if (s == null) 0L else livePosition(s.position, s.lastPositionUpdateTime, s.playbackSpeed, SystemClock.elapsedRealtime(), playing, durationMs),
            durationMs = durationMs,
            playing = playing,
            source = source,
        )
    }

    private fun appLabel(pkg: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    override fun playPause() {
        val c = controller ?: return
        if (state?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
    }

    override fun next() {
        controller?.transportControls?.skipToNext()
    }

    override fun previous() {
        controller?.transportControls?.skipToPrevious()
    }
}
