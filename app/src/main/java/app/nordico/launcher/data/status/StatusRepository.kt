package app.nordico.launcher.data.status

import kotlinx.coroutines.flow.StateFlow

data class StatusState(val bluetooth: Boolean, val cellular: Boolean, val wifi: Boolean)

interface StatusRepository {
    val state: StateFlow<StatusState>
}
