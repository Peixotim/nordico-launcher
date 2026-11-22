@file:OptIn(ExperimentalSharedTransitionApi::class)

package app.nordico.launcher.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nordico.launcher.AppContainer
import app.nordico.launcher.designsystem.LocalNordic
import app.nordico.launcher.designsystem.NText
import app.nordico.launcher.designsystem.NordicTheme
import app.nordico.launcher.designsystem.SquircleShape
import app.nordico.launcher.designsystem.nordicText
import androidx.compose.ui.platform.LocalContext
import app.nordico.launcher.feature.home.HomeScreen
import app.nordico.launcher.feature.home.formatClock
import app.nordico.launcher.feature.map.MapHolder
import app.nordico.launcher.feature.map.MapPane
import app.nordico.launcher.feature.map.rememberMapHolder
import app.nordico.launcher.data.apps.InternalRoute
import app.nordico.launcher.feature.apps.AppsScreen
import app.nordico.launcher.feature.climate.ClimateScreen
import app.nordico.launcher.feature.hud.AlertHud
import app.nordico.launcher.feature.hud.ClimateHud
import app.nordico.launcher.feature.hud.HudKind
import app.nordico.launcher.feature.hud.VolumeHud
import app.nordico.launcher.feature.nowplaying.NowPlayingScreen
import app.nordico.launcher.feature.radio.RadioScreen
import app.nordico.launcher.feature.vehicle.VehicleScreen
import app.nordico.launcher.feature.map.chooseNavApp
import app.nordico.launcher.feature.map.openFavorite
import app.nordico.launcher.feature.map.openNavApp
import app.nordico.launcher.feature.home.rememberNow

/** Raiz da UI: tema, telas com transições compartilhadas, avisos e tela desligada. */
@Composable
fun AppShell(vm: AppViewModel, c: AppContainer) {
    val dark by vm.dark.collectAsStateWithLifecycle()
    val accent by vm.accent.collectAsStateWithLifecycle()
    val screen by vm.screen.collectAsStateWithLifecycle()
    val off by vm.screenOff.collectAsStateWithLifecycle()

    val mapHolder = rememberMapHolder()

    BackHandler(enabled = screen != Screen.Home) { vm.go(Screen.Home) }

    NordicTheme(dark, accent) {
        Box(Modifier.fillMaxSize().background(LocalNordic.current.bg)) {
            SharedTransitionLayout {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
                    label = "screen",
                ) { s ->
                    CompositionLocalProvider(
                        LocalSharedScope provides this@SharedTransitionLayout,
                        LocalAnimatedScope provides this,
                    ) {
                        ScreenContent(s, vm, c, mapHolder)
                    }
                }
            }
            Huds(vm, c)
            ScreenOffOverlay(off) { vm.setScreenOff(false) }
        }
    }
}

@Composable
private fun ScreenContent(s: Screen, vm: AppViewModel, c: AppContainer, mapHolder: MapHolder) {
    when (s) {
        Screen.Home -> HomeScreen(vm, c) {
            val context = LocalContext.current
            val dark by vm.dark.collectAsStateWithLifecycle()
            val location by c.location.location.collectAsStateWithLifecycle()
            val prefs by vm.prefs.collectAsStateWithLifecycle()
            val navApp = remember(prefs.navApp) { chooseNavApp(prefs.navApp, c.installedNavPackages()) }
            MapPane(
                holder = mapHolder,
                dark = dark,
                location = location,
                navApp = navApp,
                onOpenNav = { context.openNavApp(navApp) },
                onFavorite = { context.openFavorite(navApp, it) },
            )
        }
        Screen.NowPlaying -> NowPlayingScreen(vm, c) { vm.go(Screen.Home) }
        Screen.Climate -> {
            val climate by c.vehicle.climate.collectAsStateWithLifecycle()
            val outside by c.vehicle.outsideTemp.collectAsStateWithLifecycle()
            climate?.let { ClimateScreen(it, outside) { vm.go(Screen.Home) } }
        }
        Screen.Vehicle -> {
            val prefs by vm.prefs.collectAsStateWithLifecycle()
            VehicleScreen(c, prefs.obdAdapter) { vm.go(Screen.Home) }
        }
        Screen.Radio -> RadioScreen(vm, c) { vm.go(Screen.Home) }
        Screen.Apps -> AppsScreen(c, onBack = { vm.go(Screen.Home) }) { route ->
            when (route) {
                InternalRoute.Climate -> vm.go(Screen.Climate)
                InternalRoute.Vehicle -> vm.go(Screen.Vehicle)
            }
        }
    }
}

@Composable
private fun Huds(vm: AppViewModel, c: AppContainer) {
    val kind by vm.hud.visible.collectAsStateWithLifecycle()
    val volume by c.volume.state.collectAsStateWithLifecycle()
    val climate by c.vehicle.climate.collectAsStateWithLifecycle()
    val alertText by vm.alertText.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        VolumeHud(kind == HudKind.Volume, volume)
        AlertHud(kind == HudKind.Alert, alertText, onOpen = { vm.go(Screen.Vehicle) })
        climate?.let { ClimateHud(kind == HudKind.Climate, it, onOpen = { vm.go(Screen.Climate) }) }
    }
}

/** Tela "desligada": preta, relógio fraco; um toque religa (spec 10.1). */
@Composable
private fun ScreenOffOverlay(visible: Boolean, onWake: () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(tween(500)), exit = fadeOut(tween(500))) {
        val now = rememberNow()
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onWake),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val dim = Color.White.copy(alpha = 0.22f)
            NText(formatClock(now.toLocalTime()), nordicText(96, FontWeight.Light, display = true, trackingEm = -0.04f, tnum = true), color = dim)
            NText("Toque para ligar a tela", nordicText(14), color = dim)
        }
    }
}
