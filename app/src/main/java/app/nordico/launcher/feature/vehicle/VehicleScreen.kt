package app.nordico.launcher.feature.vehicle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nordico.launcher.AppContainer
import app.nordico.launcher.data.obd.ObdState
import app.nordico.launcher.designsystem.FixedColors
import app.nordico.launcher.designsystem.LocalNordic
import app.nordico.launcher.designsystem.NText
import app.nordico.launcher.designsystem.Sym
import app.nordico.launcher.designsystem.components.NordicCard
import app.nordico.launcher.designsystem.components.PageHeader
import app.nordico.launcher.designsystem.nordicText
import app.nordico.launcher.designsystem.pressClick

private enum class VehicleSheet { None, Adapters, Faults }

/** Painel do carro lido pelo OBD-II (spec 9.2). Somente leitura. */
@Composable
fun VehicleScreen(c: AppContainer, selectedAdapter: String?, onBack: () -> Unit) {
    val n = LocalNordic.current
    val state by c.obd.state.collectAsStateWithLifecycle()
    val t by c.obd.telemetry.collectAsStateWithLifecycle()
    val faults by c.obd.faults.collectAsStateWithLifecycle()
    var sheet by remember { mutableStateOf(VehicleSheet.None) }

    fun chooseAdapter() {
        // Android 12+ exige BLUETOOTH_CONNECT para listar os pareados: pede e o usuário toca de novo.
        if (c.hasBluetoothPermission()) sheet = VehicleSheet.Adapters else c.requestBluetoothPermission()
    }

    Box(Modifier.fillMaxSize().background(n.bg)) {
        Column(Modifier.fillMaxSize()) {
            PageHeader("Veículo", onBack) {
                if (state !is ObdState.NoAdapter) {
                    FaultsButton(t.faultCount, t.milOn == true) { sheet = VehicleSheet.Faults }
                }
                StatusChip(state)
            }
            if (state is ObdState.NoAdapter) {
                NoAdapterCard(::chooseAdapter, Modifier.weight(1f).padding(start = 28.dp, end = 28.dp, bottom = 28.dp))
            } else {
                Row(Modifier.weight(1f).padding(start = 28.dp, end = 28.dp, bottom = 28.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SpeedCard(t, Modifier.width(560.dp).fillMaxHeight())
                    VehicleTiles(t, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
        AdapterSheet(
            visible = sheet == VehicleSheet.Adapters,
            adapters = c.obd.bondedAdapters(),
            selected = selectedAdapter,
            onPick = {
                c.obd.select(it.address)
                sheet = VehicleSheet.None
            },
            onDismiss = { sheet = VehicleSheet.None },
        )
        FaultsSheet(sheet == VehicleSheet.Faults, faults, onRead = c.obd::readFaults, onDismiss = { sheet = VehicleSheet.None })
    }
}

@Composable
private fun StatusChip(state: ObdState) {
    val n = LocalNordic.current
    val (dot, text) = when (state) {
        is ObdState.Connected -> Color(0xFF30D158) to "Conectado · ${state.adapter}"
        ObdState.Connecting -> n.label3 to "Conectando…"
        ObdState.Lost -> FixedColors.Warm to "Conexão perdida · reconectando…"
        ObdState.NoAdapter -> n.label3 to "Sem adaptador"
    }
    Row(
        Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(n.fill3).padding(start = 14.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(dot))
        NText(text, nordicText(15, FontWeight.SemiBold))
    }
}

/** Botão "Falhas" com selo da contagem; o ícone fica âmbar com a luz do motor acesa. */
@Composable
private fun FaultsButton(count: Int?, mil: Boolean, onClick: () -> Unit) {
    val n = LocalNordic.current
    Row(
        Modifier.height(38.dp).pressClick(onClick).clip(RoundedCornerShape(19.dp)).background(n.fill3).padding(start = 12.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Sym("report", size = 20.dp, filled = mil, tint = if (mil) FixedColors.Warm else n.label)
        NText("Falhas", nordicText(15, FontWeight.SemiBold))
        faultBadge(count)?.let {
            Box(Modifier.height(22.dp).widthIn(min = 22.dp).clip(RoundedCornerShape(11.dp)).background(n.accent), contentAlignment = Alignment.Center) {
                NText(it, nordicText(13, FontWeight.Bold, align = TextAlign.Center), color = Color.White, modifier = Modifier.padding(horizontal = 6.dp))
            }
        }
    }
}

@Composable
private fun NoAdapterCard(onChoose: () -> Unit, modifier: Modifier) {
    val n = LocalNordic.current
    NordicCard(modifier.fillMaxSize(), radius = 30.dp, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Sym("bluetooth_searching", size = 72.dp, tint = n.label3)
            NText("Conecte o adaptador OBD", nordicText(30, FontWeight.Bold, trackingEm = -0.02f))
            NText(
                "Plugue o adaptador ELM327 na porta OBD-II do carro, pareie nos ajustes de Bluetooth e escolha-o aqui.",
                nordicText(17, align = TextAlign.Center),
                color = n.label2,
                maxLines = 3,
                modifier = Modifier.widthIn(max = 520.dp),
            )
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .pressClick(onChoose)
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(n.accent.copy(alpha = 0.16f))
                    .padding(horizontal = 26.dp),
                contentAlignment = Alignment.Center,
            ) {
                NText("Escolher adaptador", nordicText(18, FontWeight.SemiBold), color = n.accent)
            }
        }
    }
}
