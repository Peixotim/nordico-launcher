package app.nordico.launcher.data.audio

import kotlin.math.roundToInt

/** Escala de volume mostrada na UI (spec 10.1). */
const val VOLUME_MAX = 40

enum class VolumeOrigin { Internal, External }

data class VolumeState(val level: Int, val muted: Boolean, val origin: VolumeOrigin = VolumeOrigin.Internal)

/** Passo pelos botões da tela: limita a 0..40 e subir sai do mudo. */
fun VolumeState.stepped(delta: Int): VolumeState = VolumeState(
    level = (level + delta).coerceIn(0, VOLUME_MAX),
    muted = muted && delta <= 0,
    origin = VolumeOrigin.Internal,
)

fun VolumeState.icon(): String = when {
    muted || level == 0 -> "volume_off"
    level < 14 -> "volume_down"
    else -> "volume_up"
}

/** Volume do sistema (0..sysMax) → escala da UI (0..40). */
fun toUiLevel(sys: Int, sysMax: Int): Int = (sys * VOLUME_MAX.toDouble() / sysMax).roundToInt()

/** Escala da UI (0..40) → volume do sistema (0..sysMax). */
fun toSysLevel(ui: Int, sysMax: Int): Int = (ui * sysMax.toDouble() / VOLUME_MAX).roundToInt()

/**
 * Passo pelos botões da tela no volume real. Com máximo do sistema < 40, cada toque anda
 * 1 índice do sistema (o número na tela pula 2–3), senão haveria toques sem efeito.
 * Retorna o novo estado da UI e o índice a gravar no sistema.
 */
fun stepWithSystem(state: VolumeState, delta: Int, sysMax: Int): Pair<VolumeState, Int> {
    if (sysMax >= VOLUME_MAX) {
        val next = state.stepped(delta)
        return next to toSysLevel(next.level, sysMax)
    }
    val sys = (toSysLevel(state.level, sysMax) + delta).coerceIn(0, sysMax)
    return VolumeState(toUiLevel(sys, sysMax), state.muted && delta <= 0) to sys
}

/**
 * Mudança informada pelo sistema. `null` = eco do que nós mesmos fizemos (ignorar).
 * Enquanto mudo o Android informa volume 0: o nível é mantido (fica cinza na tela).
 */
fun onSystemVolume(current: VolumeState, sys: Int, muted: Boolean, sysMax: Int, lastWritten: Int?): VolumeState? {
    if (muted) return if (current.muted) null else current.copy(muted = true, origin = VolumeOrigin.External)
    if (!current.muted && (sys == lastWritten || sys == toSysLevel(current.level, sysMax))) return null
    return VolumeState(toUiLevel(sys, sysMax), false, VolumeOrigin.External)
}
