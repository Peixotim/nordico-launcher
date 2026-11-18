package app.nordico.launcher.feature.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.nordico.launcher.data.audio.VolumeState
import app.nordico.launcher.data.audio.icon
import app.nordico.launcher.data.vehicle.ClimateState
import app.nordico.launcher.designsystem.FixedColors
import app.nordico.launcher.designsystem.LocalNordic
import app.nordico.launcher.designsystem.NText
import app.nordico.launcher.designsystem.Sym
import app.nordico.launcher.designsystem.components.Hud
import app.nordico.launcher.designsystem.components.SegmentMeter
import app.nordico.launcher.designsystem.nordicText
import app.nordico.launcher.feature.climate.AutoBadge
import app.nordico.launcher.feature.climate.FanBars
import app.nordico.launcher.feature.climate.formatTemp
import kotlin.math.roundToInt

/** Aviso de volume (volante/botão físico). */
@Composable
fun VolumeHud(visible: Boolean, state: VolumeState, modifier: Modifier = Modifier) {
    val c = LocalNordic.current
    Hud(visible, 60.dp, modifier) {
        Sym(state.icon(), size = 24.dp, filled = true)
        SegmentMeter(20, (state.level / 2.0).roundToInt(), state.muted, segmentWidth = 5.dp)
        NText(
            state.level.toString(),
            nordicText(22, FontWeight.SemiBold, tnum = true, align = TextAlign.End),
            modifier = Modifier.widthIn(min = 28.dp),
            color = if (state.muted) c.label3 else c.label,
        )
    }
}

/** Aviso do ar-condicionado; o toque abre o Clima. */
@Composable
fun ClimateHud(visible: Boolean, state: ClimateState, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalNordic.current
    Hud(visible, 72.dp, modifier, onClick = onOpen) {
        Sym(if (state.ac) "ac_unit" else "mode_fan", size = 26.dp, filled = true, tint = c.accent)
        Row(verticalAlignment = Alignment.Bottom) {
            NText(formatTemp(state.setTemp), nordicText(32, FontWeight.SemiBold, display = true, trackingEm = -0.03f, tnum = true))
            NText("°C", nordicText(18, FontWeight.Medium), color = c.label2, modifier = Modifier.padding(start = 1.dp, bottom = 4.dp))
        }
        Divider()
        Sym("mode_fan", size = 26.dp)
        FanBars(state.fanSpeed, barWidth = 5.dp, base = 6.dp, step = 2.3.dp, gap = 3.dp, radius = 1.5.dp)
        if (state.auto) {
            Divider()
            AutoBadge()
        }
    }
}

/** Alerta do carro (temperatura alta, bateria). Vermelho de sistema, nunca a cor de destaque. */
@Composable
fun AlertHud(visible: Boolean, text: String, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Hud(visible, 60.dp, modifier, onClick = onOpen) {
        Sym("warning", size = 26.dp, filled = true, tint = FixedColors.Danger)
        NText(text, nordicText(22, FontWeight.SemiBold))
    }
}

@Composable
private fun Divider() {
    Box(Modifier.size(0.5.dp, 32.dp).background(LocalNordic.current.separator))
}
