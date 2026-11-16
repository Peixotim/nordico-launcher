package app.nordico.launcher.feature.nowplaying

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import app.nordico.launcher.feature.home.ProgressBar
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
            ProgressBar(state.progress, Color.White.copy(alpha = 0.22f), Color.White, 7.dp, Modifier.padding(top = 40.dp))
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
