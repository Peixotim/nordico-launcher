package app.nordico.launcher.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.nordico.launcher.designsystem.LocalNordic
import app.nordico.launcher.designsystem.SquircleShape

private val ShadowTint = Color.Black.copy(alpha = 0.06f)

/** Superfície padrão: `surface`, squircle, sombra leve só no modo Dia (spec 9.1). */
@Composable
fun NordicCard(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalNordic.current
    val shape = SquircleShape(radius)
    Box(
        modifier
            .then(if (colors.dark) Modifier else Modifier.shadow(8.dp, shape, ambientColor = ShadowTint, spotColor = ShadowTint))
            .clip(shape)
            .background(colors.surface),
        contentAlignment = contentAlignment,
        content = content,
    )
}
