package app.nordico.launcher.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import app.nordico.launcher.data.prefs.Accent

@Composable
private fun animated(target: Color): Color = animateColorAsState(target, tween(450), label = "theme").value

/** Provê os tokens; ao trocar Dia/Noite cada cor anima em 450 ms (spec 9.6). */
@Composable
fun NordicTheme(dark: Boolean, accent: Accent, content: @Composable () -> Unit) {
    val t = nordicColors(dark, accent)
    val colors = NordicColors(
        dark = t.dark,
        bg = animated(t.bg),
        surface = animated(t.surface),
        label = animated(t.label),
        label2 = animated(t.label2),
        label3 = animated(t.label3),
        separator = animated(t.separator),
        fill3 = animated(t.fill3),
        fill2 = animated(t.fill2),
        material = animated(t.material),
        segmentOn = animated(t.segmentOn),
        accent = animated(t.accent),
    )
    CompositionLocalProvider(LocalNordic provides colors, content = content)
}
