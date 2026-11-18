package app.nordico.launcher.feature.nowplaying

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import app.nordico.launcher.data.media.MediaAction
import app.nordico.launcher.data.media.seekTarget
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nordico.launcher.AppContainer
import app.nordico.launcher.designsystem.NText
import app.nordico.launcher.designsystem.NordicTheme
import app.nordico.launcher.designsystem.SquircleShape
import app.nordico.launcher.designsystem.Sym
import app.nordico.launcher.designsystem.nordicText
import app.nordico.launcher.designsystem.pressClick
import app.nordico.launcher.feature.home.AlbumArt
import app.nordico.launcher.feature.home.TransportControls
import app.nordico.launcher.feature.home.VolumePanel
import app.nordico.launcher.navigation.AppViewModel
import app.nordico.launcher.navigation.SharedKeys
import app.nordico.launcher.navigation.sharedItem
import app.nordico.launcher.navigation.sharedSheet

/** Tocando agora em tela cheia (spec 10.2). */
@Composable
fun NowPlayingScreen(vm: AppViewModel, c: AppContainer, onClose: () -> Unit) {
    val np by c.media.nowPlaying.collectAsStateWithLifecycle()
    val access by c.media.hasAccess.collectAsStateWithLifecycle()
    val volume by c.volume.state.collectAsStateWithLifecycle()
    val accent by vm.accent.collectAsStateWithLifecycle()
    val actions by c.media.actions.collectAsStateWithLifecycle()
    val state = miniCardState(access, np)
    if (state !is MiniCardState.Playing) {
        LaunchedEffect(Unit) { onClose() }
        return
    }
    val white60 = Color.White.copy(alpha = 0.6f)
    Box(Modifier.fillMaxSize().sharedSheet(SharedKeys.NP_CARD).background(Color(0xFF1A1A1A))) {
        BlurredBackdrop(state.art)
        Box(
            Modifier
                .offset(28.dp, 26.dp)
                .size(48.dp)
                .pressClick(onClose)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
                .semantics { contentDescription = "Fechar" },
            contentAlignment = Alignment.Center,
        ) {
            Sym("keyboard_arrow_down", size = 32.dp, tint = Color.White)
        }
        AlbumArt(
            state.art,
            420.dp,
            22.dp,
            Modifier.offset(112.dp, 150.dp).shadow(30.dp, SquircleShape(22.dp)).sharedItem(SharedKeys.NP_ART),
        )
        Column(Modifier.align(Alignment.CenterStart).offset(x = 600.dp).width(568.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Sym("bluetooth", size = 18.dp, tint = white60)
                NText(state.source, nordicText(15, FontWeight.SemiBold), color = white60)
            }
            NText(state.title, nordicText(46, FontWeight.Bold, display = true, trackingEm = -0.03f), color = Color.White, modifier = Modifier.padding(top = 10.dp))
            NText(state.artist, nordicText(24), color = Color.White.copy(alpha = 0.62f), modifier = Modifier.padding(top = 6.dp))
            SeekBar(np?.positionMs ?: 0L, np?.durationMs ?: 0L, c.media::seekTo, Modifier.padding(top = 40.dp))
            TransportControls(
                state.playing,
                c.media::previous,
                c.media::playPause,
                c.media::next,
                small = 52.dp,
                big = 72.dp,
                spacing = 96.dp,
                tint = Color.White,
                modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
            )
            if (actions.isNotEmpty()) MediaActionRow(actions, c.media::perform, Modifier.padding(top = 18.dp))
            NordicTheme(dark = true, accent = accent) {
                VolumePanel(volume, c.volume::step, c.volume::toggleMute, Modifier.padding(top = 30.dp))
            }
        }
    }
}

/**
 * Fundo "desfocado" sem custo de GPU: a capa reduzida a 32 px e esticada,
 * escurecida (brilho 50%) e mais saturada (+60%). Spec 9.5.
 */
@Composable
private fun BlurredBackdrop(art: ImageBitmap?) {
    val small = remember(art) {
        art?.let { Bitmap.createScaledBitmap(it.asAndroidBitmap(), 32, 32, true).asImageBitmap() }
    } ?: return
    val filter = remember {
        val m = ColorMatrix().apply { setToSaturation(1.6f) }
        m.timesAssign(
            ColorMatrix(
                floatArrayOf(
                    0.5f, 0f, 0f, 0f, 0f,
                    0f, 0.5f, 0f, 0f, 0f,
                    0f, 0f, 0.5f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )
        ColorFilter.colorMatrix(m)
    }
    Image(
        small,
        null,
        Modifier.fillMaxSize().graphicsLayer { scaleX = 1.3f; scaleY = 1.3f },
        contentScale = ContentScale.Crop,
        filterQuality = FilterQuality.High,
        colorFilter = filter,
    )
}

/** Barra de progresso arrastável: a posição só é enviada ao soltar; durante o arraste mostra o tempo alvo. */
@Composable
private fun SeekBar(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    var drag by remember { mutableStateOf<Float?>(null) }
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val shown = drag ?: progress
    val shownMs = (shown * durationMs).toLong()
    Column(modifier) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(26.dp)
                .pointerInput(durationMs) {
                    if (durationMs > 0) detectTapGestures { onSeek(seekTarget(it.x / size.width, durationMs)) }
                }
                .pointerInput(durationMs) {
                    if (durationMs > 0) {
                        detectHorizontalDragGestures(
                            onDragStart = { drag = (it.x / size.width).coerceIn(0f, 1f) },
                            onDragEnd = {
                                drag?.let { f -> onSeek(seekTarget(f, durationMs)) }
                                drag = null
                            },
                            onDragCancel = { drag = null },
                            onHorizontalDrag = { change, _ -> drag = (change.position.x / size.width).coerceIn(0f, 1f) },
                        )
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f))) {
                Box(Modifier.fillMaxWidth(shown).fillMaxHeight().background(Color.White))
            }
            Box(Modifier.offset(x = (maxWidth - 26.dp) * shown).size(26.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(Color.White))
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            NText(formatElapsed(shownMs), nordicText(14, FontWeight.Medium, tnum = true), color = Color.White.copy(alpha = 0.6f))
            NText("-" + formatElapsed(durationMs - shownMs), nordicText(14, FontWeight.Medium, tnum = true), color = Color.White.copy(alpha = 0.6f))
        }
    }
}

/** Aleatório, repetir e curtir: só aparecem quando o app de música oferece a ação. */
@Composable
private fun MediaActionRow(actions: List<MediaAction>, onPerform: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally)) {
        actions.forEach { a ->
            Box(
                Modifier
                    .size(64.dp)
                    .pressClick { onPerform(a.id) }
                    .clip(SquircleShape(20.dp))
                    .background(if (a.active) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.14f))
                    .semantics { contentDescription = a.label },
                contentAlignment = Alignment.Center,
            ) {
                Sym(a.glyph, size = 30.dp, filled = a.active, tint = if (a.active) Color(0xFF0B2A52) else Color.White)
            }
        }
    }
}
