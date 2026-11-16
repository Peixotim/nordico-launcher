package app.nordico.launcher.sim

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nordico.launcher.AppContainer
import app.nordico.launcher.data.vehicle.PanelButton
import app.nordico.launcher.designsystem.LocalNordic
import app.nordico.launcher.designsystem.NText
import app.nordico.launcher.designsystem.components.NordicCard
import app.nordico.launcher.designsystem.nordicText
import app.nordico.launcher.designsystem.pressClick

/** Painel de simulação (só no flavor `sim`): botões do ar, volante e farol. Abre com toque longo no relógio. */
@Composable
fun SimPanel(c: AppContainer, onClose: () -> Unit) {
    val climate by c.simVehicle.climate.collectAsStateWithLifecycle()
    // Sem fundo clicável: dá para navegar entre telas com o painel aberto e ver o efeito na hora.
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        NordicCard(Modifier.padding(16.dp).fillMaxWidth().height(280.dp), radius = 26.dp) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Line("Painel do ar") {
                    SimButton("Temp −") { c.simVehicle.press(PanelButton.TempDown) }
                    SimButton("Temp +") { c.simVehicle.press(PanelButton.TempUp) }
                    SimButton("Vent −") { c.simVehicle.press(PanelButton.FanDown) }
                    SimButton("Vent +") { c.simVehicle.press(PanelButton.FanUp) }
                    SimButton("AUTO") { c.simVehicle.press(PanelButton.Auto) }
                    SimButton("A/C") { c.simVehicle.press(PanelButton.Ac) }
                    SimButton("Direção") { c.simVehicle.press(PanelButton.Airflow) }
                    SimButton("Recircular") { c.simVehicle.press(PanelButton.Recirc) }
                    SimButton("Desemb. frontal") { c.simVehicle.press(PanelButton.DefrostFront) }
                    SimButton("Desemb. traseiro") { c.simVehicle.press(PanelButton.DefrostRear) }
                }
                Line("Volante") {
                    SimButton("Vol −") { c.simVolume.simulateExternal(-1) }
                    SimButton("Vol +") { c.simVolume.simulateExternal(1) }
                }
                Line("Carro") {
                    SimButton("Farol ligado") { c.simVehicle.setHeadlights(true) }
                    SimButton("Farol desligado") { c.simVehicle.setHeadlights(false) }
                    SimButton("Farol sem sinal") { c.simVehicle.setHeadlights(null) }
                    SimButton(if (climate == null) "Com dados do ar" else "Sem dados do ar") { c.simVehicle.setClimateAvailable(climate == null) }
                    SimButton("Fechar", onClick = onClose)
                }
            }
        }
    }
}

@Composable
private fun Line(title: String, content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        NText(title, nordicText(15, FontWeight.SemiBold), color = LocalNordic.current.label2, modifier = Modifier.width(140.dp).padding(top = 10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun SimButton(text: String, onClick: () -> Unit) {
    val n = LocalNordic.current
    Box(
        Modifier
            .pressClick(onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(n.fill3)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        NText(text, nordicText(14, FontWeight.SemiBold), color = if (text == "Fechar") n.accent else n.label)
    }
}
