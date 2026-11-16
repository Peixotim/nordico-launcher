package app.nordico.launcher.designsystem

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.nordico.launcher.R

val Inter = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/** Para números grandes (≥ 32): relógio, temperatura, volume. */
val InterDisplay = FontFamily(
    Font(R.font.interdisplay_extralight, FontWeight.ExtraLight),
    Font(R.font.interdisplay_light, FontWeight.Light),
    Font(R.font.interdisplay_regular, FontWeight.Normal),
    Font(R.font.interdisplay_semibold, FontWeight.SemiBold),
    Font(R.font.interdisplay_bold, FontWeight.Bold),
)

const val Tnum = "tnum"

/** Estilo de texto Nórdico. Tamanho em sp (= dp na tela lógica, fontScale 1). */
fun nordicText(
    size: Int,
    weight: FontWeight = FontWeight.Normal,
    display: Boolean = false,
    trackingEm: Float = 0f,
    tnum: Boolean = false,
    align: TextAlign = TextAlign.Unspecified,
): TextStyle = TextStyle(
    fontFamily = if (display) InterDisplay else Inter,
    fontWeight = weight,
    fontSize = size.sp,
    letterSpacing = trackingEm.em,
    fontFeatureSettings = if (tnum) Tnum else null,
    textAlign = align,
)

@Composable
fun NText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = LocalNordic.current.label,
    maxLines: Int = 1,
) = BasicText(
    text = text,
    modifier = modifier,
    style = style.copy(color = color),
    maxLines = maxLines,
    overflow = TextOverflow.Ellipsis,
)
