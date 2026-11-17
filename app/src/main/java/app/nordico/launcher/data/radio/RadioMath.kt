package app.nordico.launcher.data.radio

enum class Band { Fm, Am }

const val FM_PRESETS = 18
const val AM_PRESETS = 12

/** Faixa e passo em kHz. FM 76–108 MHz de 0,1 em 0,1 MHz; AM 520–1710 kHz de 10 em 10 (spec 10.3). */
fun minKhz(b: Band) = if (b == Band.Fm) 76_000 else 520
fun maxKhz(b: Band) = if (b == Band.Fm) 108_000 else 1_710
fun stepSizeKhz(b: Band) = if (b == Band.Fm) 100 else 10

/** Frequência mais próxima da grade da banda, dentro dos limites. */
fun snapKhz(band: Band, khz: Int): Int {
    val min = minKhz(band)
    val step = stepSizeKhz(band)
    val clamped = khz.coerceIn(min, maxKhz(band))
    return min + ((clamped - min + step / 2) / step) * step
}

/** Um passo na banda (`dir` > 0 sobe, < 0 desce) com volta circular nos extremos. */
fun stepKhz(band: Band, khz: Int, dir: Int): Int {
    val next = snapKhz(band, khz) + if (dir >= 0) stepSizeKhz(band) else -stepSizeKhz(band)
    return when {
        next > maxKhz(band) -> minKhz(band)
        next < minKhz(band) -> maxKhz(band)
        else -> next
    }
}

/** Próxima estação em [dir] dentro de [stations], com volta circular; `null` se não há estações. */
fun nextStation(band: Band, khz: Int, dir: Int, stations: List<Int>): Int? {
    val sorted = stations.filter { it in minKhz(band)..maxKhz(band) }.sorted()
    if (sorted.isEmpty()) return null
    return if (dir >= 0) sorted.firstOrNull { it > khz } ?: sorted.first()
    else sorted.lastOrNull { it < khz } ?: sorted.last()
}

/** Memórias da banda: 18 em FM e 12 em AM. Imutável; cada alteração devolve um novo [Presets]. */
data class Presets(
    val fm: List<Int?> = List(FM_PRESETS) { null },
    val am: List<Int?> = List(AM_PRESETS) { null },
) {
    private fun slots(b: Band) = if (b == Band.Fm) fm else am

    fun recall(band: Band, index: Int): Int? = slots(band).getOrNull(index)

    /** Índice fora da faixa não altera nada. A frequência é ajustada à grade. */
    fun save(band: Band, index: Int, khz: Int): Presets {
        val list = slots(band)
        if (index !in list.indices) return this
        val updated = list.toMutableList().also { it[index] = snapKhz(band, khz) }
        return if (band == Band.Fm) copy(fm = updated) else copy(am = updated)
    }

    /** Uma lista por banda, com slots vazios em branco: `"98500,,88100"`. */
    fun encode(): Pair<String, String> = fm.joinToString(",") { it?.toString().orEmpty() } to
        am.joinToString(",") { it?.toString().orEmpty() }

    companion object {
        fun decode(encoded: Pair<String, String>) = decode(encoded.first, encoded.second)

        /** Valores inválidos ou fora da banda viram slot vazio; o tamanho é sempre 18 e 12. */
        fun decode(fm: String?, am: String?): Presets = Presets(parse(fm, Band.Fm, FM_PRESETS), parse(am, Band.Am, AM_PRESETS))

        private fun parse(raw: String?, band: Band, size: Int): List<Int?> {
            val parts = raw?.split(",").orEmpty()
            return List(size) { i ->
                parts.getOrNull(i)?.toIntOrNull()?.takeIf { it in minKhz(band)..maxKhz(band) }
            }
        }
    }
}
