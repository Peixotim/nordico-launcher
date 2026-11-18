package app.nordico.launcher.feature.vehicle

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.nordico.launcher.data.obd.ObdAdapter
import app.nordico.launcher.designsystem.LocalNordic
import app.nordico.launcher.designsystem.NText
import app.nordico.launcher.designsystem.Sym
import app.nordico.launcher.designsystem.components.BottomSheet
import app.nordico.launcher.designsystem.nordicText
import app.nordico.launcher.designsystem.pressClick

@Composable
private fun SheetTitle(title: String, hint: String) {
    val n = LocalNordic.current
    NText(title, nordicText(28, FontWeight.Bold, trackingEm = -0.02f), modifier = Modifier.padding(top = 18.dp))
    NText(hint, nordicText(15), color = n.label2, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
}

@Composable
private fun SheetRow(onClick: (() -> Unit)?, content: @Composable () -> Unit) {
    val n = LocalNordic.current
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(n.separator))
        Row(
            Modifier.fillMaxWidth().height(76.dp).then(if (onClick != null) Modifier.pressClick(onClick) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) { content() }
    }
}

@Composable
private fun IconPlate(glyph: String, tint: androidx.compose.ui.graphics.Color? = null, bg: androidx.compose.ui.graphics.Color? = null) {
    val n = LocalNordic.current
    Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(bg ?: n.fill3), contentAlignment = Alignment.Center) {
        Sym(glyph, tint = tint ?: n.label)
    }
}

/** Adaptadores Bluetooth já pareados; escolher conecta (spec 9.2). */
@Composable
fun AdapterSheet(visible: Boolean, adapters: List<ObdAdapter>, selected: String?, onPick: (ObdAdapter) -> Unit, onDismiss: () -> Unit) {
    val n = LocalNordic.current
    val context = LocalContext.current
    BottomSheet(visible, onDismiss) {
        SheetTitle("Escolher adaptador", "Dispositivos Bluetooth já pareados neste aparelho.")
        if (adapters.isEmpty()) {
            NText("Nenhum dispositivo pareado ainda.", nordicText(17), color = n.label2, modifier = Modifier.padding(vertical = 14.dp))
        }
        adapters.forEach { a ->
            SheetRow({ onPick(a) }) {
                IconPlate("bluetooth")
                Column(Modifier.weight(1f)) {
                    NText(a.name, nordicText(19, FontWeight.SemiBold))
                    NText(a.address, nordicText(14), color = n.label2)
                }
                if (a.address == selected) Sym("check_circle", filled = true, tint = n.accent)
            }
        }
        SheetRow({ openBluetoothSettings(context) }) {
            IconPlate("add", tint = n.accent, bg = n.accent.copy(alpha = 0.16f))
            Column {
                NText("Parear novo adaptador", nordicText(19, FontWeight.SemiBold), color = n.accent)
                NText("Abre os ajustes de Bluetooth. O PIN costuma ser 1234 ou 0000.", nordicText(14), color = n.label2)
            }
        }
    }
}

private fun openBluetoothSettings(context: Context) {
    runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
