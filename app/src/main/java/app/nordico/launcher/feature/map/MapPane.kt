package app.nordico.launcher.feature.map

import android.view.ViewGroup
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.nordico.launcher.data.location.LatLng
import app.nordico.launcher.designsystem.FixedColors
import app.nordico.launcher.designsystem.LocalNordic
import app.nordico.launcher.designsystem.NText
import app.nordico.launcher.designsystem.SquircleShape
import app.nordico.launcher.designsystem.Sym
import app.nordico.launcher.designsystem.nordicText
import app.nordico.launcher.designsystem.pressClick

private const val STYLE_DAY = "https://tiles.openfreemap.org/styles/positron"
private const val STYLE_NIGHT = "https://tiles.openfreemap.org/styles/dark"

/** Mapa do Início com ponto azul, "Para onde?", sugestões Casa/Trabalho e recentralizar (spec 10.1). */
@Composable
fun MapPane(
    holder: MapHolder,
    dark: Boolean,
    location: LatLng?,
    navApp: NavApp?,
    onOpenNav: () -> Unit,
    onFavorite: (Favorite) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalNordic.current
    Box(modifier.fillMaxSize().clip(SquircleShape(30.dp)).background(c.surface)) {
        AndroidView(
            factory = {
                (holder.view.parent as? ViewGroup)?.removeView(holder.view)
                holder.view
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            // Trade-off: troca de estilo pisca/recarrega ao trocar Dia/Noite; dois estilos em memória se incomodar
            holder.setStyle(if (dark) STYLE_NIGHT else STYLE_DAY)
            location?.let(holder::moveTo)
        }
        if (location != null) Puck(Modifier.align(Alignment.Center))

        Row(
            Modifier
                .padding(16.dp)
                .material(CircleShape)
                .pressClick(onOpenNav)
                .size(344.dp, 54.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Sym("search")
            NText("Para onde?", nordicText(17), color = c.label2, modifier = Modifier.weight(1f))
            Sym("mic")
        }

        if (showSuggestions(navApp)) {
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .width(344.dp)
                    .material(SquircleShape(24.dp))
                    .padding(vertical = 6.dp),
            ) {
                Suggestion("home", "Casa", "Abrir no ${navApp?.label}") { onFavorite(Favorite.Home) }
                Box(Modifier.padding(start = 66.dp).fillMaxWidth().height(0.5.dp).background(c.separator))
                Suggestion("work", "Trabalho", "Abrir no ${navApp?.label}") { onFavorite(Favorite.Work) }
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .material(CircleShape)
                .pressClick { location?.let(holder::recenter) }
                .size(54.dp)
                .semantics { contentDescription = "Recentralizar" },
            contentAlignment = Alignment.Center,
        ) {
            Sym("near_me", filled = true, tint = c.accent)
        }
    }
}

/** Superfície "material" sobre o mapa: cor semi-opaca + sombra (sem desfoque na fase 1). */
@Composable
private fun Modifier.material(shape: Shape): Modifier {
    val c = LocalNordic.current
    val tint = Color.Black.copy(alpha = 0.10f)
    return shadow(10.dp, shape, ambientColor = tint, spotColor = tint).clip(shape).background(c.material)
}

@Composable
private fun Suggestion(glyph: String, title: String, subtitle: String, onClick: () -> Unit) {
    val c = LocalNordic.current
    Row(
        Modifier.fillMaxWidth().height(66.dp).pressClick(onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
            Sym(glyph, size = 21.dp, filled = true, tint = Color.White)
        }
        Column(Modifier.weight(1f)) {
            NText(title, nordicText(17, FontWeight.SemiBold))
            NText(subtitle, nordicText(14), color = c.label2, modifier = Modifier.padding(top = 1.dp))
        }
        Sym("chevron_right", tint = c.label3)
    }
}

/** Ponto azul com halo pulsando (2,4 s). */
@Composable
private fun Puck(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "halo")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "halo")
    Box(modifier.size(50.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(50.dp)
                .graphicsLayer {
                    val s = 0.4f + 1.2f * p
                    scaleX = s
                    scaleY = s
                    alpha = 1f - p
                }
                .clip(CircleShape)
                .background(FixedColors.Puck.copy(alpha = 0.22f)),
        )
        Box(
            Modifier
                .size(22.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(FixedColors.Puck)
                .border(3.5.dp, Color.White, CircleShape),
        )
    }
}
