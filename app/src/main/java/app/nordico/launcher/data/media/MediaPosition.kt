package app.nordico.launcher.data.media

/**
 * Posição atual da faixa a partir do último estado informado pelo player, sem perguntar de novo ao sistema.
 * Limitada a 0..duração (duração 0 = desconhecida, sem limite superior).
 */
fun livePosition(basePositionMs: Long, baseTimeMs: Long, speed: Float, nowMs: Long, playing: Boolean, durationMs: Long): Long {
    val pos = if (playing) basePositionMs + ((nowMs - baseTimeMs) * speed).toLong() else basePositionMs
    val upper = if (durationMs > 0) durationMs else Long.MAX_VALUE
    return pos.coerceIn(0L, upper)
}
