package app.nordico.launcher.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.nordico.launcher.designsystem.LocalNordic
import app.nordico.launcher.designsystem.nordicSpring
import app.nordico.launcher.designsystem.pressClick

/** Aviso flutuante no topo central (spec 10.6): entra com mola, sai com fade de 220 ms. */
@Composable
fun Hud(
    visible: Boolean,
    height: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val offsetPx = with(LocalDensity.current) { 16.dp.roundToPx() }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.padding(top = 18.dp),
        enter = fadeIn(tween(220)) + slideInVertically(nordicSpring()) { -offsetPx } + scaleIn(nordicSpring(), initialScale = 0.94f),
        exit = fadeOut(tween(220)),
    ) {
        val shape = RoundedCornerShape(height / 2)
        val tint = Color.Black.copy(alpha = 0.18f)
        Row(
            Modifier
                .height(height)
                .shadow(18.dp, shape, ambientColor = tint, spotColor = tint)
                .clip(shape)
                .background(LocalNordic.current.material)
                .then(if (onClick != null) Modifier.pressClick(onClick) else Modifier)
                .padding(start = 20.dp, end = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}
