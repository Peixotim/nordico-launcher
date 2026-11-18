package app.nordico.launcher.feature.vehicle

import java.util.Locale
import kotlin.math.ceil

enum class CoolantTone { Cool, Normal, Warm, Hot }

/** Faixas da barra do arrefecimento (spec 9.2): frio < 70, normal 70–100, atenção 101–104, quente ≥ 105. */
fun coolantTone(c: Int?): CoolantTone = when {
    c == null -> CoolantTone.Normal
    c < 70 -> CoolantTone.Cool
    c <= 100 -> CoolantTone.Normal
    c < 105 -> CoolantTone.Warm
    else -> CoolantTone.Hot
}

/** Valor ou "—" quando o carro não informa. */
fun formatOrDash(v: Number?, suffix: String = ""): String = if (v == null) "—" else "$v$suffix"

fun formatVolts(v: Double?): String =
    if (v == null) "—" else String.format(Locale.US, "%.1f", v).replace('.', ',')

/** Rotação com ponto de milhar: 2400 → "2.400". */
fun formatRpm(rpm: Int?): String = if (rpm == null) "—" else String.format(Locale.US, "%,d", rpm).replace(',', '.')

/** Posição no arco do conta-giros: 0 a 8.000 rpm. */
fun rpmFraction(rpm: Int?): Float = ((rpm ?: 0) / 8000f).coerceIn(0f, 1f)

const val COOLANT_SEGMENTS = 20

/** Segmentos acesos da barra do arrefecimento: 20 passos de 3 °C começando em 60 °C. */
fun coolantSegments(c: Int?): Int = if (c == null) 0 else ceil((c - 60) / 3.0).toInt().coerceIn(0, COOLANT_SEGMENTS)

/** Selo no botão "Falhas": some com zero e para em "9+". */
fun faultBadge(count: Int?): String? = when {
    count == null || count <= 0 -> null
    count > 9 -> "9+"
    else -> count.toString()
}

/** "Alternador carregando" quando o motor roda e a tensão passa de 13 V. */
fun batteryHint(volts: Double?, rpm: Int?): String? =
    if (volts != null && rpm != null && rpm > 0 && volts > 13.0) "Alternador carregando" else null
