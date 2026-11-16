package app.nordico.launcher.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.nordico.launcher.R

private fun symbols(fill: Float) = FontFamily(
    Font(
        R.font.material_symbols_rounded,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("FILL", fill),
            FontVariation.weight(400),
            FontVariation.Setting("opsz", 24f),
        ),
    ),
)

private val OutlineSymbols = symbols(0f)
private val FilledSymbols = symbols(1f)

/** Ícone Material Symbols Rounded desenhado por ligadura (ex.: "volume_up"). Decorativo para leitores de tela. */
@Composable
fun Sym(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    filled: Boolean = false,
    tint: Color = LocalNordic.current.label,
) {
    val fontSize = with(LocalDensity.current) { size.toSp() }
    Box(modifier.size(size).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        BasicText(
            text = name,
            softWrap = false,
            maxLines = 1,
            style = TextStyle(
                color = tint,
                fontFamily = if (filled) FilledSymbols else OutlineSymbols,
                fontSize = fontSize,
                lineHeight = fontSize,
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            ),
        )
    }
}
