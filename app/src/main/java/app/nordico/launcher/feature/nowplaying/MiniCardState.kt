package app.nordico.launcher.feature.nowplaying

import androidx.compose.ui.graphics.ImageBitmap
import app.nordico.launcher.data.media.NowPlaying

sealed interface MiniCardState {
    /** Sem acesso a notificações: "Permitir acesso à música". */
    data object NeedsAccess : MiniCardState

    /** Nenhuma sessão de mídia: "Nada tocando". */
    data object Empty : MiniCardState

    data class Playing(
        val title: String,
        val artist: String,
        val art: ImageBitmap?,
        val progress: Float,
        val playing: Boolean,
        val source: String,
    ) : MiniCardState
}

fun miniCardState(hasAccess: Boolean, np: NowPlaying?): MiniCardState = when {
    !hasAccess -> MiniCardState.NeedsAccess
    np == null -> MiniCardState.Empty
    // Sessão parada sem título nem artista (ex.: app que só registrou a sessão) não é "tocando".
    !np.playing && np.title.isBlank() && np.artist.isBlank() -> MiniCardState.Empty
    else -> MiniCardState.Playing(
        title = np.title.ifBlank { np.source },
        artist = np.artist,
        art = np.art,
        progress = if (np.durationMs > 0) (np.positionMs.toFloat() / np.durationMs).coerceIn(0f, 1f) else 0f,
        playing = np.playing,
        source = np.source,
    )
}
