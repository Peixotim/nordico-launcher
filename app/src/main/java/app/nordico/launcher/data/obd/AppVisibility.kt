package app.nordico.launcher.data.obd

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** `true` enquanto alguma tela do Nórdico está visível. O OBD só lê nesse intervalo (spec 7). */
object AppVisibility {
    val visible: StateFlow<Boolean> by lazy {
        val state = MutableStateFlow(false)
        // Precisa rodar na thread principal: o AppContainer é criado no Application.onCreate.
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> state.value = true
                    Lifecycle.Event.ON_STOP -> state.value = false
                    else -> Unit
                }
            },
        )
        state
    }
}
