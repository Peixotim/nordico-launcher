package app.nordico.launcher.feature.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.nordico.launcher.data.audio.VolumeState
import app.nordico.launcher.data.audio.icon
import app.nordico.launcher.data.prefs.ScreenMode
import app.nordico.launcher.designsystem.LocalNordic
import app.nordico.launcher.designsystem.NText
import app.nordico.launcher.designsystem.SquircleShape
import app.nordico.launcher.designsystem.Sym
import app.nordico.launcher.designsystem.components.NordicCard
import app.nordico.launcher.designsystem.components.SegmentMeter
import app.nordico.launcher.designsystem.components.SegmentedControl
import app.nordico.launcher.designsystem.nordicSpring
import app.nordico.launcher.designsystem.nordicText
import app.nordico.launcher.designsystem.pressClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Painel A: − / número + régua / +. Segurar repete após 380 ms, a cada 85 ms. */
@Composable
fun VolumePanel(
    state: VolumeState,
    onStep: (Int) -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalNordic.current
    NordicCard(modifier.fillMaxWidth(), radius = 24.dp) {
        Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RepeatButton("remove", "Diminuir volume") { onStep(-1) }
            Column(
                Modifier
                    .weight(1f)
                    .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onToggleMute)
                    .semantics { contentDescription = if (state.muted) "Volume ${state.level}, mudo" else "Volume ${state.level}" },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Sym(state.icon(), size = 22.dp, filled = true, tint = c.label2)
                    NText(
                        state.level.toString(),
                        nordicText(28, FontWeight.SemiBold, display = true, trackingEm = -0.02f, tnum = true),
                        modifier = Modifier.widthIn(min = 38.dp),
                        color = if (state.muted) c.label3 else c.label,
                    )
                }
                SegmentMeter(40, state.level, state.muted)
            }
            RepeatButton("add", "Aumentar volume") { onStep(1) }
        }
    }
}

@Composable
private fun RepeatButton(glyph: String, description: String, onStep: () -> Unit) {
    val c = LocalNordic.current
    val scope = rememberCoroutineScope()
    val step by rememberUpdatedState(onStep)
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (pressed) 0.94f else 1f,
        if (pressed) tween(120, easing = FastOutSlowInEasing) else nordicSpring(),
        label = "press",
    )
    Box(
        Modifier
            .size(58.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(SquircleShape(18.dp))
            .background(c.fill3)
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    pressed = true
                    step()
                    val repeat = scope.launch {
                        delay(380)
                        while (true) {
                            step()
                            delay(85)
                        }
                    }
                    waitForUpOrCancellation()
                    repeat.cancel()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Sym(glyph, size = 30.dp)
    }
}

/** Linha de baixo do painel A: modo da tela + botão Tela. */
@Composable
fun ScreenModeRow(mode: ScreenMode, onMode: (ScreenMode) -> Unit, onScreenOff: () -> Unit) {
    val c = LocalNordic.current
    Row(Modifier.fillMaxWidth().height(58.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SegmentedControl(
            options = listOf("brightness_auto" to "Auto", "light_mode" to "Dia", "dark_mode" to "Noite"),
            selected = mode.ordinal,
            onSelect = { onMode(ScreenMode.entries[it]) },
            modifier = Modifier.weight(1f).height(58.dp),
        )
        NordicCard(Modifier.size(58.dp).pressClick(onScreenOff), radius = 20.dp, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Sym("power_settings_new", size = 22.dp)
                NText("Tela", nordicText(11, FontWeight.SemiBold), color = c.label2)
            }
        }
    }
}
