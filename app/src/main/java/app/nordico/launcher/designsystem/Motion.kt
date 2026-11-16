package app.nordico.launcher.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/** Mola padrão (≈ resposta 0,5 s, ~1% de passagem). Spec 9.6. */
fun <T> nordicSpring(): SpringSpec<T> = spring(dampingRatio = 0.82f, stiffness = 158f)

/** Encolhe para 0.94 em 120 ms ao tocar e volta com a mola padrão. */
fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = if (pressed) tween(120, easing = FastOutSlowInEasing) else nordicSpring(),
        label = "press",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Toque com a animação de pressão, sem ripple. */
fun Modifier.pressClick(onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    pressScale(interaction).clickable(interaction, indication = null, onClick = onClick)
}
