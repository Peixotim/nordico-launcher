package app.nordico.launcher.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.nordico.launcher.designsystem.LocalNordic

/** Régua de N segmentos (volume). Aceso = `label`, mudo = `label3`, apagado = `fill2`. */
@Composable
fun SegmentMeter(
    count: Int,
    lit: Int,
    muted: Boolean,
    modifier: Modifier = Modifier,
    segmentWidth: Dp = 4.dp,
    height: Dp = 8.dp,
) {
    val c = LocalNordic.current
    val on = if (muted) c.label3 else c.label
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
        repeat(count) { i ->
            Box(
                Modifier
                    .size(segmentWidth, height)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(if (i < lit) on else c.fill2),
            )
        }
    }
}
