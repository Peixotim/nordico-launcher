package app.nordico.launcher.feature.vehicle

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.nordico.launcher.data.obd.VehicleTelemetry
import app.nordico.launcher.designsystem.FixedColors
import app.nordico.launcher.designsystem.LocalNordic
import app.nordico.launcher.designsystem.NText
import app.nordico.launcher.designsystem.Sym
import app.nordico.launcher.designsystem.components.NordicCard
import app.nordico.launcher.designsystem.nordicText

private const val REDLINE = 6500f / 8000f

/** Velocidade grande dentro do arco de rotação (0–8.000 rpm, zona vermelha acima de 6.500). */
@Composable
fun SpeedCard(t: VehicleTelemetry, modifier: Modifier = Modifier) {
    val n = LocalNordic.current
    val fraction by animateFloatAsState(rpmFraction(t.rpm), tween(250, easing = LinearEasing), label = "rpm")
    NordicCard(modifier, radius = 30.dp) {
        Canvas(Modifier.align(Alignment.Center).size(480.dp)) {
            val stroke = 16.dp.toPx()
            val inset = 22.dp.toPx()
            val topLeft = Offset(inset, inset)
            val arc = Size(size.width - inset * 2, size.height - inset * 2)
            val style = Stroke(stroke, cap = StrokeCap.Round)
            drawArc(n.fill2, 135f, 270f, false, topLeft, arc, style = style)
            drawArc(FixedColors.Danger.copy(alpha = 0.35f), 135f + 270f * REDLINE, 270f * (1 - REDLINE), false, topLeft, arc, style = style)
            if (fraction > 0f) {
                val color = if (fraction > REDLINE) FixedColors.Danger else n.label
                drawArc(color, 135f, 270f * fraction, false, topLeft, arc, style = style)
            }
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            NText(
                formatOrDash(t.speedKmh),
                nordicText(176, FontWeight.Light, display = true, trackingEm = -0.03f, tnum = true),
                color = if (t.speedKmh == null) n.label3 else n.label,
            )
            NText("km/h", nordicText(22, FontWeight.Medium), color = n.label2)
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp)) {
            NText(formatRpm(t.rpm), nordicText(17, FontWeight.SemiBold, tnum = true), color = if (t.rpm == null) n.label3 else n.label)
            NText(" rpm", nordicText(17), color = n.label2)
        }
    }
}

/** Grade de 6 cartões (spec 9.2). Valores que o carro não informa viram "—". */
@Composable
fun VehicleTiles(t: VehicleTelemetry, modifier: Modifier = Modifier) {
    val tone = coolantTone(t.coolantC)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Tile("device_thermostat", "Arrefecimento", t.coolantC?.toString(), "°C", Modifier.weight(1f),
                valueColor = if (tone == CoolantTone.Hot) FixedColors.Danger else null) {
                CoolantZones(t.coolantC, tone)
                if (tone == CoolantTone.Hot) {
                    NText("Temperatura alta", nordicText(14, FontWeight.SemiBold), color = FixedColors.Danger, modifier = Modifier.padding(top = 8.dp))
                }
            }
            Tile("battery_charging_full", "Bateria", t.batteryV?.let(::formatVolts), "V", Modifier.weight(1f)) {
                batteryHint(t.batteryV, t.rpm)?.let { NText(it, nordicText(14), color = LocalNordic.current.label2, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Tile("local_gas_station", "Combustível", t.fuelPct?.toString(), "%", Modifier.weight(1f)) { LevelBar(t.fuelPct) }
            Tile("speed", "Carga do motor", t.loadPct?.toString(), "%", Modifier.weight(1f)) { LevelBar(t.loadPct) }
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Tile("tune", "Acelerador", t.throttlePct?.toString(), "%", Modifier.weight(1f)) { LevelBar(t.throttlePct) }
            Tile("air", "Ar de admissão", t.intakeC?.toString(), "°C", Modifier.weight(1f)) {}
        }
    }
}

@Composable
private fun Tile(
    glyph: String,
    label: String,
    value: String?,
    unit: String,
    modifier: Modifier,
    valueColor: Color? = null,
    extra: @Composable ColumnScope.() -> Unit,
) {
    val n = LocalNordic.current
    NordicCard(modifier.fillMaxHeight(), radius = 26.dp) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Sym(glyph, size = 20.dp, tint = n.label2)
                NText(label, nordicText(15, FontWeight.SemiBold), color = n.label2)
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                NText(
                    value ?: "—",
                    nordicText(64, FontWeight.Light, display = true, trackingEm = -0.03f, tnum = true),
                    color = if (value == null) n.label3 else (valueColor ?: n.label),
                )
                if (value != null) NText(unit, nordicText(24), color = n.label2, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            }
            extra()
        }
    }
}

@Composable
private fun LevelBar(pct: Int?) {
    val n = LocalNordic.current
    val f by animateFloatAsState(((pct ?: 0) / 100f).coerceIn(0f, 1f), tween(400), label = "level")
    Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(n.fill2)) {
        Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(n.label))
    }
}

@Composable
private fun CoolantZones(c: Int?, tone: CoolantTone) {
    val n = LocalNordic.current
    val lit = coolantSegments(c)
    val on = when (tone) {
        CoolantTone.Cool -> FixedColors.Cool
        CoolantTone.Normal -> n.label
        CoolantTone.Warm -> FixedColors.Warm
        CoolantTone.Hot -> FixedColors.Danger
    }
    Row(Modifier.padding(top = 12.dp).fillMaxWidth().height(10.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(COOLANT_SEGMENTS) { i ->
            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(if (i < lit) on else n.fill2))
        }
    }
}
