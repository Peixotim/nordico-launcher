package app.nordico.launcher.navigation

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.nordico.launcher.AppContainer
import app.nordico.launcher.data.audio.VolumeOrigin
import app.nordico.launcher.data.obd.Alert
import app.nordico.launcher.data.obd.alertFor
import app.nordico.launcher.data.obd.alertText
import app.nordico.launcher.data.obd.shouldShowAlert
import app.nordico.launcher.data.prefs.Accent
import app.nordico.launcher.data.prefs.Prefs
import app.nordico.launcher.data.prefs.ScreenMode
import app.nordico.launcher.designsystem.resolveDark
import app.nordico.launcher.feature.hud.HudController
import app.nordico.launcher.feature.hud.HudKind
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

/** Estado global: tela atual, tema, tela desligada e avisos. */
class AppViewModel(private val c: AppContainer) : ViewModel() {
    private val _screen = MutableStateFlow(Screen.Home)
    val screen: StateFlow<Screen> = _screen

    private val _screenOff = MutableStateFlow(false)
    val screenOff: StateFlow<Boolean> = _screenOff

    val hud = HudController(viewModelScope)

    val prefs: StateFlow<Prefs> = c.prefs.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, Prefs())

    private val hour = flow {
        while (true) {
            emit(LocalTime.now().hour)
            delay(60_000)
        }
    }

    val dark: StateFlow<Boolean> = combine(prefs, c.vehicle.headlightsOn, hour) { p, lights, h ->
        resolveDark(p.screenMode, lights, h)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, resolveDark(ScreenMode.Auto, null, LocalTime.now().hour))

    val accent: StateFlow<Accent> = prefs.map { it.accent }.stateIn(viewModelScope, SharingStarted.Eagerly, Accent.Orange)

    init {
        viewModelScope.launch {
            c.volume.state.drop(1).collect { if (it.origin == VolumeOrigin.External) hud.show(HudKind.Volume) }
        }
        viewModelScope.launch {
            c.vehicle.climate.drop(1).collect { climate ->
                _screen.value = nextScreenOnClimate(_screen.value, climate)
                if (climate != null && _screen.value != Screen.Climate) hud.show(HudKind.Climate)
            }
        }
    }

    private val _alertText = MutableStateFlow("")
    val alertText: StateFlow<String> = _alertText
    private var alert = Alert.None
    private var alertShownAt: Long? = null

    init {
        // O ticker repete o aviso a cada minuto mesmo com a telemetria parada (StateFlow não reemite valor igual).
        val tick = flow {
            while (true) {
                emit(Unit)
                delay(1000)
            }
        }
        viewModelScope.launch {
            combine(c.obd.state, c.obd.telemetry, tick) { st, t, _ -> st to t }.collect { (st, t) ->
                val next = alertFor(st, t, alert)
                val now = SystemClock.elapsedRealtime()
                if (next != Alert.None) _alertText.value = alertText(next, t)
                if (shouldShowAlert(alert, next, alertShownAt, now)) {
                    alertShownAt = now
                    hud.show(HudKind.Alert)
                }
                if (next == Alert.None) alertShownAt = null
                alert = next
            }
        }
    }

    fun go(s: Screen) {
        _screen.value = s
    }

    fun setScreenOff(off: Boolean) {
        _screenOff.value = off
    }

    fun setScreenMode(m: ScreenMode) {
        viewModelScope.launch { c.prefs.setScreenMode(m) }
    }
}
