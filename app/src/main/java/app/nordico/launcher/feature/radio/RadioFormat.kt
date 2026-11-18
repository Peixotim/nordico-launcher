package app.nordico.launcher.feature.radio

import app.nordico.launcher.data.radio.AM_PRESETS
import app.nordico.launcher.data.radio.Band
import app.nordico.launcher.data.radio.FM_PRESETS
import app.nordico.launcher.data.radio.maxKhz
import app.nordico.launcher.data.radio.minKhz
import app.nordico.launcher.data.radio.snapKhz
import java.util.Locale

const val PRESETS_PER_PAGE = 6

/** FM em MHz com vírgula ("98,5"); AM em kHz inteiro ("1040"). */
fun formatFrequency(band: Band, khz: Int): String =
    if (band == Band.Fm) String.format(Locale.US, "%.1f", khz / 1000.0).replace('.', ',') else khz.toString()

fun frequencyUnit(band: Band): String = if (band == Band.Fm) "MHz" else "kHz"

/** Onde a frequência cai na escala (0 = início da banda, 1 = fim). */
fun dialFraction(band: Band, khz: Int): Float {
    val min = minKhz(band)
    return ((khz - min).toFloat() / (maxKhz(band) - min)).coerceIn(0f, 1f)
}

/** Inverso de [dialFraction]: arrastar a escala sintoniza a frequência (ajustada à grade). */
fun khzAtFraction(band: Band, fraction: Float): Int {
    val min = minKhz(band)
    return snapKhz(band, min + (fraction.coerceIn(0f, 1f) * (maxKhz(band) - min)).toInt())
}

/** Memórias aparecem de 6 em 6: 3 páginas em FM, 2 em AM. */
fun presetPages(band: Band): Int = (if (band == Band.Fm) FM_PRESETS else AM_PRESETS) / PRESETS_PER_PAGE
