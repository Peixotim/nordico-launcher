package app.nordico.launcher.feature.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.nordico.launcher.designsystem.LocalNordic
import app.nordico.launcher.designsystem.NText
import app.nordico.launcher.designsystem.SquircleShape
import app.nordico.launcher.designsystem.Sym
import app.nordico.launcher.designsystem.nordicText
import app.nordico.launcher.designsystem.pressClick

/** Capa do álbum (ou placa neutra quando não há capa). */
@Composable
fun AlbumArt(art: ImageBitmap?, size: Dp, radius: Dp, modifier: Modifier = Modifier) {
    val shape = SquircleShape(radius)
    if (art != null) {
        Image(art, null, modifier.size(size).clip(shape), contentScale = ContentScale.Crop)
    } else {
        Box(modifier.size(size).clip(shape).background(LocalNordic.current.fill3), contentAlignment = Alignment.Center) {
            Sym("music_note", size = size * 0.4f, tint = LocalNordic.current.label3)
        }
    }
}

/** Barra de progresso fina; a largura anima linearmente em 1 s (avanço de 1 s por tick). */
@Composable
fun ProgressBar(fraction: Float, track: Color, fill: Color, height: Dp, modifier: Modifier = Modifier) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(1000, easing = LinearEasing), label = "progress")
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(track)) {
        Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(fill))
    }
}

/** Anterior / tocar-pausar / próxima. */
@Composable
fun TransportControls(
    playing: Boolean,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    small: Dp,
    big: Dp,
    spacing: Dp,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.pressClick(onPrev).semantics { contentDescription = "Anterior" }.padding(8.dp)) {
            Sym("skip_previous", size = small, filled = true, tint = tint)
        }
        Box(Modifier.pressClick(onPlayPause).semantics { contentDescription = if (playing) "Pausar" else "Tocar" }.padding(4.dp)) {
            Sym(if (playing) "pause" else "play_arrow", size = big, filled = true, tint = tint)
        }
        Box(Modifier.pressClick(onNext).semantics { contentDescription = "Próxima" }.padding(8.dp)) {
            Sym("skip_next", size = small, filled = true, tint = tint)
        }
    }
}

/** Conteúdo do card de música do Início (usado dentro de um NordicCard). */
@Composable
fun MiniPlayerContent(
    title: String,
    artist: String,
    art: ImageBitmap?,
    progress: Float,
    playing: Boolean,
    artModifier: Modifier,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    val c = LocalNordic.current
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AlbumArt(art, 80.dp, 14.dp, artModifier)
            Column {
                NText(title, nordicText(21, FontWeight.SemiBold, trackingEm = -0.015f))
                NText(artist, nordicText(16), color = c.label2, modifier = Modifier.padding(top = 2.dp))
            }
        }
        ProgressBar(progress, c.fill3, c.label, 6.dp, Modifier.padding(top = 16.dp))
        TransportControls(playing, onPrev, onPlayPause, onNext, small = 36.dp, big = 46.dp, spacing = 40.dp, tint = c.label, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}
