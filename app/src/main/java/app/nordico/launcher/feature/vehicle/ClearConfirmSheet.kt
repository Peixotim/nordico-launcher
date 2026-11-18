package app.nordico.launcher.feature.vehicle

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.nordico.launcher.data.obd.ClearBlock
import app.nordico.launcher.data.obd.ClearOutcome
import app.nordico.launcher.designsystem.FixedColors
import app.nordico.launcher.designsystem.LocalNordic
import app.nordico.launcher.designsystem.NText
import app.nordico.launcher.designsystem.Sym
import app.nordico.launcher.designsystem.nordicText
import app.nordico.launcher.designsystem.pressClick

private val OkGreen = Color(0xFF30D158)

@Composable
private fun EffectLine(glyph: String, text: String) {
    val n = LocalNordic.current
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Sym(glyph, size = 20.dp, tint = n.label2)
        NText(text, nordicText(16), maxLines = 3, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun EffectBlock(title: String, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val n = LocalNordic.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NText(title.uppercase(), nordicText(13, FontWeight.Bold, trackingEm = 0.06f), color = n.label2)
        content()
    }
}

/** Confirmação antes de apagar: o que será apagado, o que acontece e em que condições (spec 8.4). */
@Composable
fun ConfirmPanel(codes: List<String>, onCancel: () -> Unit, onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    val n = LocalNordic.current
    Column(modifier.fillMaxSize().padding(top = 12.dp)) {
        NText("Apagar todas as falhas?", nordicText(30, FontWeight.Bold, trackingEm = -0.02f))
        Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            codes.take(8).forEach {
                Box(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(n.fill3).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    NText(it, nordicText(15, FontWeight.Bold, tnum = true))
                }
            }
            if (codes.size > 8) NText("+${codes.size - 8}", nordicText(15, FontWeight.Bold), color = n.label2, modifier = Modifier.align(Alignment.CenterVertically))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            EffectBlock("O que acontece", Modifier.weight(1f)) {
                EffectLine("delete", "As ${codes.size} ${if (codes.size == 1) "falha é apagada" else "falhas são apagadas"} do carro.")
                EffectLine("photo_camera", "O registro do momento da falha (freeze frame) é perdido.")
                EffectLine("restart_alt", "Os monitores de prontidão são zerados. Precisam de alguns dias de uso para completar.")
                EffectLine("lightbulb", "Se a falha persistir, a luz do motor volta.")
            }
            EffectBlock("Antes de apagar", Modifier.weight(1f)) {
                EffectLine("local_parking", "Carro parado.")
                EffectLine("key", "Em muitos carros: motor desligado e chave na posição ligada.")
                EffectLine("history", "Guardamos os códigos e a data neste aparelho.")
            }
        }
        Box(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(bottom = 28.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.weight(1f).height(60.dp).pressClick(onCancel).clip(RoundedCornerShape(20.dp)).background(n.fill3),
                contentAlignment = Alignment.Center,
            ) { NText("Cancelar", nordicText(19, FontWeight.Bold)) }
            Box(
                Modifier.weight(1.4f).height(60.dp).pressClick(onConfirm).clip(RoundedCornerShape(20.dp)).background(n.accent),
                contentAlignment = Alignment.Center,
            ) { NText("Apagar falhas", nordicText(19, FontWeight.Bold), color = Color.White) }
        }
    }
}

/** Enquanto o `04` está em andamento. Não há botão: o app não aceita outro toque. */
@Composable
fun ClearingPanel(modifier: Modifier = Modifier) {
    val n = LocalNordic.current
    val turn by rememberInfiniteTransition(label = "clearing").animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart), label = "turn")
    Column(modifier.fillMaxSize().padding(bottom = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
        Canvas(Modifier.size(64.dp)) {
            val w = 6.dp.toPx()
            val s = Size(size.width - w, size.height - w)
            drawArc(n.fill2, 0f, 360f, false, androidx.compose.ui.geometry.Offset(w / 2, w / 2), s, style = Stroke(w))
            drawArc(n.accent, turn, 90f, false, androidx.compose.ui.geometry.Offset(w / 2, w / 2), s, style = Stroke(w, cap = StrokeCap.Round))
        }
        NText("Apagando falhas…", nordicText(30, FontWeight.Bold, trackingEm = -0.02f))
        NText("Não desligue o adaptador. Isso leva alguns segundos.", nordicText(17), color = n.label2)
    }
}

/** Resultado de apagar. O botão volta para a lista (e limpa o resultado). */
@Composable
fun OutcomePanel(outcome: ClearOutcome, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val n = LocalNordic.current
    val text = outcomeText(outcome)
    val (glyph, color) = when (outcome) {
        ClearOutcome.Cleared -> "check_circle" to OkGreen
        ClearOutcome.NoResponse -> "bluetooth_disabled" to FixedColors.Danger
        is ClearOutcome.Refused -> "key" to FixedColors.Warm
        ClearOutcome.Unverified -> "help" to FixedColors.Warm
        is ClearOutcome.Returned -> "error" to FixedColors.Warm
        is ClearOutcome.Blocked -> (if (outcome.reason == ClearBlock.Moving) "block" else "info") to FixedColors.Warm
    }
    Column(modifier.fillMaxSize().padding(bottom = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
        Sym(glyph, size = 76.dp, filled = true, tint = color)
        NText(text.title, nordicText(30, FontWeight.Bold, trackingEm = -0.02f, align = TextAlign.Center))
        NText(text.message, nordicText(17, align = TextAlign.Center), color = n.label2, maxLines = 4, modifier = Modifier.widthIn(max = 620.dp))
        Box(
            Modifier.padding(top = 8.dp).pressClick(onDone).height(52.dp).clip(RoundedCornerShape(26.dp)).background(n.accent).padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center,
        ) { NText(text.button, nordicText(18, FontWeight.Bold), color = Color.White) }
    }
}
