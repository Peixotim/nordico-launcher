package app.nordico.launcher.feature.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nordico.launcher.AppContainer
import app.nordico.launcher.data.status.StatusState
import app.nordico.launcher.data.vehicle.ClimateState
import app.nordico.launcher.designsystem.LocalNordic
import app.nordico.launcher.designsystem.NText
import app.nordico.launcher.designsystem.Sym
import app.nordico.launcher.designsystem.components.NordicCard
import app.nordico.launcher.designsystem.nordicText
import app.nordico.launcher.designsystem.pressClick
import app.nordico.launcher.feature.climate.chipLabel
import app.nordico.launcher.feature.nowplaying.MiniCardState
import app.nordico.launcher.feature.nowplaying.miniCardState
import app.nordico.launcher.navigation.AppViewModel
import app.nordico.launcher.navigation.Screen
import app.nordico.launcher.navigation.SharedKeys
import app.nordico.launcher.navigation.sharedItem
import app.nordico.launcher.navigation.sharedSheet
import kotlin.math.roundToInt

/** Início: mapa à esquerda; coluna direita com cabeçalho, música, atalhos e painel A (protótipo 03). */
@Composable
fun HomeScreen(vm: AppViewModel, c: AppContainer, onClockLongPress: () -> Unit, mapSlot: @Composable () -> Unit) {
    val np by c.media.nowPlaying.collectAsStateWithLifecycle()
    val access by c.media.hasAccess.collectAsStateWithLifecycle()
    val volume by c.volume.state.collectAsStateWithLifecycle()
    val climate by c.vehicle.climate.collectAsStateWithLifecycle()
    val outside by c.vehicle.outsideTemp.collectAsStateWithLifecycle()
    val status by c.status.state.collectAsStateWithLifecycle()
    val prefs by vm.prefs.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.offset(16.dp, 16.dp).size(760.dp, 688.dp)) { mapSlot() }
        Column(
            Modifier.offset(798.dp, 20.dp).width(460.dp).height(684.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Header(outside, status, climate, onClockLongPress) { vm.go(Screen.Climate) }
            MiniPlayerCard(miniCardState(access, np), c) { vm.go(Screen.NowPlaying) }
            Tiles { vm.go(Screen.Apps) }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VolumePanel(volume, c.volume::step, c.volume::toggleMute)
                ScreenModeRow(prefs.screenMode, vm::setScreenMode) { vm.setScreenOff(true) }
            }
        }
    }
}

@Composable
private fun Header(
    outside: Double?,
    status: StatusState,
    climate: ClimateState?,
    onClockLongPress: () -> Unit,
    onOpenClimate: () -> Unit,
) {
    val c = LocalNordic.current
    val now = rememberNow()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            NText(
                formatClock(now.toLocalTime()),
                nordicText(88, FontWeight.Light, display = true, trackingEm = -0.045f, tnum = true)
                    .copy(lineHeight = 80.sp, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)),
                modifier = Modifier.offset(x = (-4).dp).pointerInput(Unit) { detectTapGestures(onLongPress = { onClockLongPress() }) },
            )
            val temp = outside?.let { " · ${it.roundToInt()} °C" } ?: ""
            NText(formatDate(now.toLocalDate()) + temp, nordicText(17), color = c.label2, modifier = Modifier.padding(top = 10.dp))
            if (climate != null) {
                Row(
                    Modifier
                        .padding(top = 12.dp)
                        .sharedSheet(SharedKeys.SHEET_CLIMA)
                        .pressClick(onOpenClimate)
                        .clip(RoundedCornerShape(18.dp))
                        .background(c.fill3)
                        .height(36.dp)
                        .padding(start = 11.dp, end = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Sym(if (climate.ac) "ac_unit" else "mode_fan", size = 20.dp, filled = true, tint = c.accent)
                    NText(chipLabel(climate), nordicText(15, FontWeight.SemiBold, tnum = true))
                    Sym("chevron_right", size = 18.dp, tint = c.label3)
                }
            }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (status.bluetooth) Sym("bluetooth", size = 20.dp)
            if (status.cellular) Sym("signal_cellular_alt", size = 20.dp)
            if (status.wifi) Sym("wifi", size = 20.dp)
        }
    }
}

@Composable
private fun MiniPlayerCard(state: MiniCardState, c: AppContainer, onOpen: () -> Unit) {
    val context = LocalContext.current
    when (state) {
        is MiniCardState.Playing -> NordicCard(Modifier.fillMaxWidth().sharedSheet(SharedKeys.NP_CARD).pressClick(onOpen), radius = 26.dp) {
            MiniPlayerContent(
                title = state.title,
                artist = state.artist,
                art = state.art,
                progress = state.progress,
                playing = state.playing,
                artModifier = Modifier.sharedItem(SharedKeys.NP_ART),
                onPrev = c.media::previous,
                onPlayPause = c.media::playPause,
                onNext = c.media::next,
            )
        }
        MiniCardState.NeedsAccess -> MessageCard("music_note", "Permitir acesso à música", "chevron_right") {
            context.startSafely(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        MiniCardState.Empty -> {
            val spotify = remember { context.packageManager.getLaunchIntentForPackage(SPOTIFY) }
            MessageCard("music_note", "Nada tocando", if (spotify != null) "play_arrow" else null) {
                spotify?.let { context.startSafely(it) }
            }
        }
    }
}

private const val SPOTIFY = "com.spotify.music"

@Composable
private fun MessageCard(glyph: String, text: String, trailing: String?, onClick: () -> Unit) {
    val c = LocalNordic.current
    NordicCard(Modifier.fillMaxWidth().height(116.dp).pressClick(onClick), radius = 26.dp, contentAlignment = Alignment.CenterStart) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AlbumArt(null, 80.dp, 14.dp)
            NText(text, nordicText(19, FontWeight.SemiBold), modifier = Modifier.weight(1f))
            if (trailing != null) Sym(trailing, tint = c.label3)
        }
    }
}

private fun Context.startSafely(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
    }
}

@Composable
private fun Tiles(onApps: () -> Unit) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Tile("call", "Telefone", Modifier.weight(1f)) {
            try {
                context.startActivity(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: ActivityNotFoundException) {
            }
        }
        // fase 3: abrir app do fabricante (pacote desconhecido até a central chegar)
        Tile("radio", "Rádio", Modifier.weight(1f)) {}
        // fase 3: abrir app do fabricante (pacote desconhecido até a central chegar)
        Tile("photo_camera", "Câmera", Modifier.weight(1f)) {}
        Tile("apps", "Apps", Modifier.weight(1f).sharedSheet(SharedKeys.SHEET_APPS), onApps)
    }
}

@Composable
private fun Tile(glyph: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    NordicCard(modifier.height(96.dp).pressClick(onClick), radius = 24.dp, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Sym(glyph, size = 28.dp)
            NText(label, nordicText(14, FontWeight.Medium))
        }
    }
}
