package app.nordico.launcher.feature.hud

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class HudKind(val visibleMillis: Long) {
    Volume(1800),
    Climate(2600),
    Alert(6000),
}

/** Um aviso por vez; um novo evento reinicia o tempo e outro tipo substitui o atual. */
class HudController(private val scope: CoroutineScope) {
    private val _visible = MutableStateFlow<HudKind?>(null)
    val visible: StateFlow<HudKind?> = _visible
    private var hideJob: Job? = null

    fun show(kind: HudKind) {
        _visible.value = kind
        hideJob?.cancel()
        hideJob = scope.launch {
            delay(kind.visibleMillis)
            _visible.value = null
        }
    }
}
