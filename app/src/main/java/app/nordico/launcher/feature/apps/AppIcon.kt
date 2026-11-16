package app.nordico.launcher.feature.apps

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.nordico.launcher.data.apps.AppEntry
import app.nordico.launcher.designsystem.LocalNordic
import app.nordico.launcher.designsystem.NText
import app.nordico.launcher.designsystem.SquircleShape
import app.nordico.launcher.designsystem.Sym
import app.nordico.launcher.designsystem.nordicSpring
import app.nordico.launcher.designsystem.nordicText

private val IconShape = SquircleShape(24.dp)

/** Ícone de app 88 dp + nome; em edição treme e mostra o selo −/+ (spec 10.5). */
@Composable
fun AppIcon(
    app: AppEntry,
    index: Int,
    editing: Boolean,
    hiddenSection: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tap by rememberUpdatedState(onTap)
    val longPress by rememberUpdatedState(onLongPress)
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, if (pressed) tween(120) else nordicSpring(), label = "press")
    val badge by animateFloatAsState(if (editing) 1f else 0f, nordicSpring(), label = "badge")
    val wiggle = if (editing) {
        val t = rememberInfiniteTransition(label = "wiggle")
        val phase = ((index * 37 % 10) / 30f * 340).toInt()
        t.animateFloat(
            -1.4f,
            1.4f,
            infiniteRepeatable(tween(170, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(phase)),
            label = "wiggle",
        ).value
    } else {
        0f
    }

    Box(
        modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                onClick(app.label) { tap(); true }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onLongPress = { longPress() },
                    onTap = { tap() },
                )
            },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Plate(app, Modifier.graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = wiggle }.alpha(if (hiddenSection) 0.45f else 1f))
            NText(app.label, nordicText(14, FontWeight.Medium, align = TextAlign.Center), modifier = Modifier.widthIn(max = 130.dp))
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(x = (-40).dp)
                .size(28.dp)
                .graphicsLayer { scaleX = badge; scaleY = badge }
                .clip(CircleShape)
                .background(Color(0xEB787880)),
            contentAlignment = Alignment.Center,
        ) {
            Sym(if (hiddenSection) "add" else "remove", size = 20.dp, tint = Color.White)
        }
    }
}

@Composable
private fun Plate(app: AppEntry, modifier: Modifier) {
    val c = LocalNordic.current
    when {
        app.icon != null -> Image(app.icon, null, modifier.size(88.dp).clip(IconShape))
        app.brandColor != null -> Box(modifier.size(88.dp).shadow(4.dp, IconShape).clip(IconShape).background(app.brandColor), contentAlignment = Alignment.Center) {
            Sym(app.glyph ?: "apps", size = 40.dp, filled = true, tint = Color.White)
        }
        else -> Box(
            modifier.size(88.dp).then(if (c.dark) Modifier else Modifier.shadow(6.dp, IconShape, ambientColor = Color.Black.copy(0.06f), spotColor = Color.Black.copy(0.06f))).clip(IconShape).background(c.surface),
            contentAlignment = Alignment.Center,
        ) {
            Sym(app.glyph ?: "apps", size = 40.dp, filled = true)
        }
    }
}
