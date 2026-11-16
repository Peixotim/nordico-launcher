package app.nordico.launcher.data.media

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.StateFlow

data class NowPlaying(
    val title: String,
    val artist: String,
    val art: ImageBitmap?,
    val positionMs: Long,
    val durationMs: Long,
    val playing: Boolean,
    val source: String,
)

interface MediaRepository {
    val nowPlaying: StateFlow<NowPlaying?>
    /** Acesso a notificações concedido (necessário para ler a mídia no Android). */
    val hasAccess: StateFlow<Boolean>
    fun playPause()
    fun next()
    fun previous()
}
