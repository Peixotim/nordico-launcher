package app.nordico.launcher.feature.nowplaying

/** 102_000 ms → "1:42". Negativo vira "0:00". */
fun formatElapsed(ms: Long): String {
    val s = (ms.coerceAtLeast(0) / 1000)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}
