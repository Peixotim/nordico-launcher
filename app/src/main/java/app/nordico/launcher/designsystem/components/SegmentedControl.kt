package app.nordico.launcher.designsystem.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.nordico.launcher.designsystem.LocalNordic
import app.nordico.launcher.designsystem.NText
import app.nordico.launcher.designsystem.SquircleShape
import app.nordico.launcher.designsystem.Sym
import app.nordico.launcher.designsystem.nordicSpring
import app.nordico.launcher.designsystem.nordicText

/** Segmentado estilo iOS: o indicador desliza com a mola padrão. `options` = (glifo, rótulo). */
@Composable
fun SegmentedControl(
    options: List<Pair<String, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalNordic.current
    BoxWithConstraints(
        modifier
            .clip(SquircleShape(20.dp))
            .background(c.fill3)
            .padding(4.dp),
    ) {
        val segment = maxWidth / options.size
        val x by animateDpAsState(segment * selected, nordicSpring(), label = "segment")
        Box(
            Modifier
                .offset(x = x)
                .width(segment)
                .fillMaxHeight()
                .shadow(2.dp, SquircleShape(16.dp), ambientColor = Color.Black.copy(0.12f), spotColor = Color.Black.copy(0.12f))
                .clip(SquircleShape(16.dp))
                .background(c.segmentOn),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, (glyph, label) ->
                val tint = if (i == selected) c.label else c.label2
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.RadioButton) { onSelect(i) },
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (glyph.isNotEmpty()) Sym(glyph, size = 20.dp, tint = tint)
                    NText(label, nordicText(15, FontWeight.Medium), color = tint)
                }
            }
        }
    }
}
