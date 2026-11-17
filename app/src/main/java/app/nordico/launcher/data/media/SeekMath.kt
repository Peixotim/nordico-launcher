package app.nordico.launcher.data.media

/** Posição em ms para uma fração (0..1) da faixa. Duração desconhecida (≤ 0) dá 0. */
fun seekTarget(fraction: Float, durationMs: Long): Long =
    if (durationMs <= 0) 0L else (fraction.coerceIn(0f, 1f) * durationMs).toLong()
