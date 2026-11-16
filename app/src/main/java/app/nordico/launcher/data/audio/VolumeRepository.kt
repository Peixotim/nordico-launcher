package app.nordico.launcher.data.audio

import kotlinx.coroutines.flow.StateFlow

interface VolumeRepository {
    /** Nível 0–40; `origin = External` quando a mudança veio de fora (volante, botão físico). */
    val state: StateFlow<VolumeState>
    fun step(delta: Int)
    fun toggleMute()
}
