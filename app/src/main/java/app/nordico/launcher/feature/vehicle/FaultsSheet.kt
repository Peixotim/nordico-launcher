package app.nordico.launcher.feature.vehicle

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.nordico.launcher.data.obd.ClearState
import app.nordico.launcher.data.obd.DtcCatalog
import app.nordico.launcher.data.obd.Dtc
import app.nordico.launcher.data.obd.ObdState
import app.nordico.launcher.data.obd.VehicleTelemetry
import app.nordico.launcher.data.obd.clearGate
import app.nordico.launcher.data.prefs.FaultLog
import app.nordico.launcher.designsystem.LocalNordic
import app.nordico.launcher.designsystem.NText
import app.nordico.launcher.designsystem.Sym
import app.nordico.launcher.designsystem.components.BottomSheet
import app.nordico.launcher.designsystem.nordicText
import app.nordico.launcher.designsystem.pressClick

private val OkGreen = Color(0xFF30D158)

private fun openVideo(context: Context, code: String): Boolean = try {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl(code, VEHICLE_QUERY))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (_: ActivityNotFoundException) {
    false
}

/**
 * Folha de falhas: lista com descrição e solução, "marcar como corrigida" (só no app, com seleção múltipla)
 * e "Apagar todas as falhas" (de verdade, com confirmação e resultado verificado).
 */
@Composable
fun FaultsSheet(
    visible: Boolean,
    faults: List<Dtc>?,
    telemetry: VehicleTelemetry,
    state: ObdState,
    catalog: DtcCatalog,
    log: FaultLog,
    clearState: ClearState,
    onRead: () -> Unit,
    onMark: (codes: List<String>, marked: Boolean) -> Unit,
    onConfirmClear: (codes: List<String>) -> Unit,
    onDismissResult: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf<String?>(null) }
    var selecting by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf(emptySet<String>()) }
    var confirming by remember { mutableStateOf(false) }
    var videoMissing by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(visible) {
        if (visible) onRead()
        else {
            expanded = null
            selecting = false
            selection = emptySet()
            confirming = false
            videoMissing = null
        }
    }

    // Enquanto o comando está em andamento a folha não fecha: o resultado precisa ser visto.
    BottomSheet(visible, onDismiss = { if (clearState != ClearState.Clearing) onDismiss() }, height = 640.dp) {
        when {
            clearState == ClearState.Clearing -> ClearingPanel()
            clearState is ClearState.Done -> OutcomePanel(clearState.outcome, onDone = onDismissResult)
            confirming -> ConfirmPanel(
                codes = faults.orEmpty().map { it.code },
                onCancel = { confirming = false },
                onConfirm = {
                    confirming = false
                    onConfirmClear(faults.orEmpty().map { it.code })
                },
            )
            else -> FaultList(
                faults, telemetry, state, catalog, log,
                expanded, selecting, selection, videoMissing,
                onExpand = { code -> expanded = if (expanded == code) null else code },
                onSelecting = {
                    selecting = it
                    selection = emptySet()
                    expanded = null
                },
                onToggleSelect = { selection = toggled(selection, it) },
                onMark = onMark,
                onVideo = { code -> videoMissing = if (openVideo(context, code)) null else code },
                onAskClear = { confirming = true },
            )
        }
    }
}

@Composable
private fun FaultList(
    faults: List<Dtc>?,
    telemetry: VehicleTelemetry,
    state: ObdState,
    catalog: DtcCatalog,
    log: FaultLog,
    expanded: String?,
    selecting: Boolean,
    selection: Set<String>,
    videoMissing: String?,
    onExpand: (String) -> Unit,
    onSelecting: (Boolean) -> Unit,
    onToggleSelect: (String) -> Unit,
    onMark: (List<String>, Boolean) -> Unit,
    onVideo: (String) -> Unit,
    onAskClear: () -> Unit,
) {
    val n = LocalNordic.current
    val count = faults?.size ?: 0
    val block = clearGate(state, telemetry, faults)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                NText("Falhas registradas", nordicText(28, FontWeight.Bold, trackingEm = -0.02f))
                NText(if (faults == null) "Lendo…" else faultsSummary(count, telemetry.milOn), nordicText(15), color = n.label2, modifier = Modifier.padding(top = 2.dp))
            }
            if (count > 0) ActionPill(if (selecting) "close" else "checklist", if (selecting) "Cancelar" else "Selecionar") { onSelecting(!selecting) }
        }

        when {
            faults == null -> Unit
            faults.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            ) {
                Sym("check_circle", size = 76.dp, filled = true, tint = OkGreen)
                NText("Nenhuma falha registrada", nordicText(24, FontWeight.SemiBold))
                NText("O carro não tem códigos armazenados.", nordicText(16), color = n.label2)
            }
            else -> {
                if (!selecting) {
                    val enabled = block == null
                    Box(
                        Modifier
                            .padding(top = 14.dp)
                            .fillMaxWidth()
                            .height(64.dp)
                            .then(if (enabled) Modifier.pressClick(onAskClear) else Modifier)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (enabled) n.accent else n.fill3),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Sym(if (enabled) "delete_sweep" else "block", size = 26.dp, tint = if (enabled) Color.White else n.label3)
                            NText("Apagar todas as falhas", nordicText(20, FontWeight.Bold), color = if (enabled) Color.White else n.label3)
                        }
                    }
                    NText(clearHint(block), nordicText(14, align = TextAlign.Center), color = n.label2, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp))
                } else {
                    Box(Modifier.height(12.dp))
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(faults, key = { it.code }) { f ->
                        FaultRow(
                            info = catalog.lookup(f.code),
                            expanded = expanded == f.code,
                            marked = f.code in log.marks,
                            selecting = selecting,
                            selected = f.code in selection,
                            canVideo = canOpenVideo(telemetry.speedKmh),
                            videoMissing = videoMissing == f.code,
                            onClick = { if (selecting) onToggleSelect(f.code) else onExpand(f.code) },
                            onMark = { onMark(listOf(f.code), f.code !in log.marks) },
                            onVideo = { onVideo(f.code) },
                        )
                    }
                }
                if (selecting) SelectionBar(selection.size) { mark ->
                    onMark(selection.toList(), mark)
                    onSelecting(false)
                }
            }
        }
    }
}

@Composable
private fun SelectionBar(count: Int, onApply: (mark: Boolean) -> Unit) {
    val n = LocalNordic.current
    Row(
        Modifier.fillMaxWidth().height(76.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val on = count > 0
        Box(
            Modifier.height(48.dp).then(if (on) Modifier.pressClick { onApply(true) } else Modifier).clip(RoundedCornerShape(24.dp)).background(if (on) n.accent else n.fill3).padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center,
        ) { NText("Marcar $count como corrigidas", nordicText(17, FontWeight.Bold), color = if (on) Color.White else n.label3) }
        Box(
            Modifier.height(48.dp).then(if (on) Modifier.pressClick { onApply(false) } else Modifier).clip(RoundedCornerShape(24.dp)).background(n.fill3).padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center,
        ) { NText("Desmarcar", nordicText(17, FontWeight.SemiBold), color = if (on) n.label else n.label3) }
        NText("Só no app. Não apaga nada no carro.", nordicText(15, align = TextAlign.End), color = n.label2, modifier = Modifier.weight(1f))
    }
}
