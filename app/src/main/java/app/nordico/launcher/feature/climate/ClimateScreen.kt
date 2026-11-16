package app.nordico.launcher.feature.climate

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.nordico.launcher.data.vehicle.Airflow
import app.nordico.launcher.data.vehicle.ClimateState
import app.nordico.launcher.designsystem.FixedColors
import app.nordico.launcher.designsystem.LocalNordic
import app.nordico.launcher.designsystem.NText
import app.nordico.launcher.designsystem.SquircleShape
import app.nordico.launcher.designsystem.Sym
import app.nordico.launcher.designsystem.components.NordicCard
import app.nordico.launcher.designsystem.components.PageHeader
import app.nordico.launcher.designsystem.nordicText
import app.nordico.launcher.navigation.SharedKeys
import app.nordico.launcher.navigation.sharedSheet
import kotlin.math.roundToInt

/** Clima somente leitura (spec 10.3). */
@Composable
fun ClimateScreen(c: ClimateState, outsideTemp: Double?, onBack: () -> Unit) {
    val n = LocalNordic.current
    Column(Modifier.fillMaxSize().sharedSheet(SharedKeys.SHEET_CLIMA).background(n.bg)) {
        PageHeader("Clima", onBack) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Sym("info", size = 18.dp, tint = n.label2)
                NText("Somente leitura · o ajuste continua nos botões do painel", nordicText(14), color = n.label2)
            }
        }
        Row(
            Modifier.fillMaxSize().padding(start = 28.dp, end = 28.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TemperatureCard(c, Modifier.width(560.dp).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoCard("mode_fan", "Ventilação", Modifier.weight(1f), spinMillis = fanSpinMillis(c.fanSpeed)) {
                        FanBars(c.fanSpeed, barWidth = 24.dp, base = 16.dp, step = 8.dp, gap = 8.dp, radius = 6.dp)
                        NText(fanLabel(c), nordicText(14), color = n.label2, modifier = Modifier.padding(top = 12.dp))
                    }
                    InfoCard("device_thermostat", "Externa", Modifier.weight(1f)) {
                        NText(outsideTemp?.let { "${it.roundToInt()}°" } ?: "—", nordicText(76, FontWeight.Light, display = true, trackingEm = -0.04f, tnum = true))
                        NText("Sensor do carro", nordicText(14), color = n.label2, modifier = Modifier.padding(top = 12.dp))
                    }
                }
                NordicCard(Modifier.fillMaxWidth(), radius = 26.dp) {
                    Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 16.dp)) {
                        Label("air", "Direção do ar")
                        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Airflow.entries.forEach { a -> Pill(a.label, a == c.airflow, Modifier.weight(1f)) }
                        }
                    }
                }
                NordicCard(Modifier.fillMaxWidth(), radius = 26.dp) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StateChip("hdr_auto", "AUTO", c.auto, Modifier.weight(1f))
                        StateChip("ac_unit", "A/C", c.ac, Modifier.weight(1f))
                        StateChip("sync", "Recircular", c.recirc, Modifier.weight(1f))
                        StateChip("windshield_defrost_front", "Desemb. frontal", c.defrostFront, Modifier.weight(1f))
                        StateChip("windshield_defrost_rear", "Desemb. traseiro", c.defrostRear, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TemperatureCard(c: ClimateState, modifier: Modifier) {
    val n = LocalNordic.current
    NordicCard(modifier, radius = 30.dp) {
        Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 28.dp)) {
            Label("thermostat", "Temperatura definida")
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Top) {
                NText(formatTemp(c.setTemp), nordicText(176, FontWeight.ExtraLight, display = true, trackingEm = -0.06f, tnum = true))
                NText("°C", nordicText(44, FontWeight.Light), color = n.label2, modifier = Modifier.padding(start = 8.dp, top = 30.dp))
            }
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (c.auto) AutoBadge()
                NText(if (c.ac) "A/C ligado" else "A/C desligado", nordicText(17), color = n.label2)
            }
            val lit = when (toneFor(c.setTemp)) {
                TempTone.Cool -> FixedColors.Cool
                TempTone.Mid -> n.label
                TempTone.Warm -> FixedColors.Warm
            }
            Row(Modifier.fillMaxWidth().padding(top = 40.dp).height(26.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(29) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (18.0 + i * 0.5 <= c.setTemp) lit else n.fill2),
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("18°", "25°", "32°").forEach { NText(it, nordicText(13, FontWeight.Medium, tnum = true), color = n.label3) }
            }
        }
    }
}

@Composable
private fun InfoCard(glyph: String, title: String, modifier: Modifier, spinMillis: Int? = null, content: @Composable ColumnScope.() -> Unit) {
    NordicCard(modifier.fillMaxHeight(), radius = 26.dp) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 22.dp)) {
            Label(glyph, title, spinMillis)
            Spacer(Modifier.weight(1f))
            content()
        }
    }
}

@Composable
private fun Label(glyph: String, text: String, spinMillis: Int? = null) {
    val n = LocalNordic.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (spinMillis != null) {
            val t = rememberInfiniteTransition(label = "fan")
            val r by t.animateFloat(0f, 360f, infiniteRepeatable(tween(spinMillis, easing = LinearEasing), RepeatMode.Restart), label = "fan")
            Sym(glyph, Modifier.graphicsLayer { rotationZ = r }, size = 20.dp, tint = n.label2)
        } else {
            Sym(glyph, size = 20.dp, tint = n.label2)
        }
        NText(text, nordicText(15, FontWeight.SemiBold), color = n.label2)
    }
}

/** 7 barras crescentes; acesas até a velocidade. */
@Composable
fun FanBars(speed: Int, barWidth: Dp, base: Dp, step: Dp, gap: Dp, radius: Dp) {
    val n = LocalNordic.current
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(gap)) {
        repeat(7) { i ->
            Box(
                Modifier
                    .size(barWidth, base + step * i)
                    .clip(RoundedCornerShape(radius))
                    .background(if (i < speed) n.label else n.fill2),
            )
        }
    }
}

@Composable
fun AutoBadge() {
    val n = LocalNordic.current
    NText(
        "AUTO",
        nordicText(12, FontWeight.Bold, trackingEm = 0.06f),
        color = n.accent,
        modifier = Modifier.clip(RoundedCornerShape(7.dp)).background(n.accent.copy(alpha = 0.16f)).padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun Pill(text: String, active: Boolean, modifier: Modifier) {
    val n = LocalNordic.current
    Box(
        modifier.height(52.dp).clip(SquircleShape(16.dp)).background(if (active) n.accent.copy(alpha = 0.16f) else n.fill3),
        contentAlignment = Alignment.Center,
    ) {
        NText(text, nordicText(15, if (active) FontWeight.SemiBold else FontWeight.Medium), color = if (active) n.accent else n.label2)
    }
}

@Composable
private fun StateChip(glyph: String, text: String, active: Boolean, modifier: Modifier) {
    val n = LocalNordic.current
    val tint: Color = if (active) n.accent else n.label2
    Column(
        modifier.height(86.dp).clip(SquircleShape(18.dp)).background(if (active) n.accent.copy(alpha = 0.16f) else n.fill3),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Sym(glyph, size = 26.dp, tint = tint)
        NText(text, nordicText(13, FontWeight.SemiBold), color = tint)
    }
}
