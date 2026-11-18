package app.nordico.launcher.feature.vehicle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.nordico.launcher.data.obd.DtcInfo
import app.nordico.launcher.data.obd.Severity
import app.nordico.launcher.data.obd.Tier
import app.nordico.launcher.designsystem.FixedColors
import app.nordico.launcher.designsystem.LocalNordic
import app.nordico.launcher.designsystem.NText
import app.nordico.launcher.designsystem.Sym
import app.nordico.launcher.designsystem.nordicText
import app.nordico.launcher.designsystem.pressClick

private val OkGreen = Color(0xFF30D158)

private fun severityColor(s: Severity): Color = when (s) {
    Severity.Ok -> OkGreen
    Severity.Attention -> FixedColors.Warm
    Severity.Stop -> FixedColors.Danger
}

@Composable
fun SeverityChip(s: Severity) {
    val color = severityColor(s)
    Row(
        Modifier.height(28.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.16f)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        NText(severityLabel(s), nordicText(13, FontWeight.Bold), color = color)
    }
}

/** Botão em pílula das ações de uma falha. */
@Composable
fun ActionPill(glyph: String, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val n = LocalNordic.current
    Row(
        Modifier
            .then(if (enabled) Modifier.pressClick(onClick) else Modifier)
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(n.fill3)
            .padding(start = 12.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Sym(glyph, size = 20.dp, tint = if (enabled) n.label else n.label3)
        NText(label, nordicText(15, FontWeight.SemiBold), color = if (enabled) n.label else n.label3)
    }
}

@Composable
private fun CheckCircle(on: Boolean) {
    val n = LocalNordic.current
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .then(if (on) Modifier.background(n.accent) else Modifier.border(2.dp, n.label3, CircleShape)),
        contentAlignment = Alignment.Center,
    ) {
        if (on) Sym("check", size = 20.dp, filled = true, tint = Color.White)
    }
}

@Composable
private fun Block(title: String, content: @Composable () -> Unit) {
    val n = LocalNordic.current
    Column(Modifier.fillMaxWidth()) {
        NText(title.uppercase(), nordicText(13, FontWeight.Bold, trackingEm = 0.06f), color = n.label2, modifier = Modifier.padding(bottom = 6.dp))
        content()
    }
}

@Composable
private fun Bullets(items: List<String>) {
    val n = LocalNordic.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.padding(top = 9.dp).size(6.dp).clip(CircleShape).background(n.label3))
                NText(it, nordicText(16), maxLines = 4)
            }
        }
    }
}

/**
 * Uma falha da lista: código, título e gravidade; ao abrir, causas, o que fazer, vídeo e "marcar como corrigida".
 * Marcar é só uma anotação no app: não apaga nada no carro (e a interface diz isso).
 */
@Composable
fun FaultRow(
    info: DtcInfo,
    expanded: Boolean,
    marked: Boolean,
    selecting: Boolean,
    selected: Boolean,
    canVideo: Boolean,
    videoMissing: Boolean,
    onClick: () -> Unit,
    onMark: () -> Unit,
    onVideo: () -> Unit,
) {
    val n = LocalNordic.current
    val dim = if (marked) n.label2 else n.label
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(n.separator))
        Row(
            Modifier.fillMaxWidth().height(76.dp).pressClick(onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (selecting) CheckCircle(selected)
            NText(info.code, nordicText(22, FontWeight.Bold, tnum = true), color = dim, modifier = Modifier.width(92.dp))
            Column(Modifier.weight(1f)) {
                NText(info.title, nordicText(19, FontWeight.SemiBold), color = dim, maxLines = 1)
                if (info.tier != Tier.Full) {
                    val note = if (info.titleLang == "en") "Descrição completa não disponível no Nórdico · título em inglês" else "Descrição completa não disponível no Nórdico"
                    NText(note, nordicText(14), color = n.label2, maxLines = 1)
                }
            }
            if (marked) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Sym("check_circle", size = 18.dp, filled = true, tint = OkGreen)
                    NText("Corrigida, aguardando apagar", nordicText(13, FontWeight.SemiBold), color = n.label2)
                }
            } else {
                SeverityChip(info.severity)
            }
            if (!selecting) Sym(if (expanded) "expand_less" else "expand_more", tint = n.label3)
        }
        if (expanded && !selecting) {
            Column(Modifier.fillMaxWidth().padding(start = 108.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (info.causes.isNotEmpty()) {
                            Block("Causas prováveis") { Bullets(info.causes) }
                        } else {
                            Block("O que é") { NText(if (info.known) info.title else "Código sem descrição neste catálogo.", nordicText(16), maxLines = 3) }
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Block("O que fazer") { NText(info.solution, nordicText(16), maxLines = 12) }
                        if (info.symptoms.isNotEmpty()) Block("O que você pode notar") { Bullets(info.symptoms) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionPill("smart_display", if (canVideo) "Ver vídeo" else "Ver vídeo (carro parado)", enabled = canVideo, onClick = onVideo)
                    ActionPill(if (marked) "undo" else "task_alt", if (marked) "Desmarcar" else "Marcar como corrigida", onClick = onMark)
                    NText(
                        if (videoMissing) "Nenhum app para abrir o vídeo." else "Marcar é só no app. Não apaga nada no carro.",
                        nordicText(13),
                        color = n.label2,
                    )
                }
            }
        }
    }
}
