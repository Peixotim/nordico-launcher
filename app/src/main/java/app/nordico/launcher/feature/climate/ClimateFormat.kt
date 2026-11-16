package app.nordico.launcher.feature.climate

import app.nordico.launcher.data.vehicle.ClimateState
import java.util.Locale
import kotlin.math.roundToInt

/** 22.5 → "22,5" (uma casa, vírgula). */
fun formatTemp(t: Double): String = String.format(Locale.US, "%.1f", t).replace('.', ',')

enum class TempTone { Cool, Mid, Warm }

/** Cor da régua: frio < 21, neutro 21–25, quente > 25 (spec 9.1). */
fun toneFor(t: Double): TempTone = when {
    t < 21.0 -> TempTone.Cool
    t > 25.0 -> TempTone.Warm
    else -> TempTone.Mid
}

fun fanLabel(c: ClimateState): String = (if (c.auto) "Automática" else "Manual") + " · ${c.fanSpeed} de 7"

/** Duração de uma volta da ventoinha: 1,5 − 0,17 × velocidade (s). */
fun fanSpinMillis(speed: Int): Int = ((1.5 - 0.17 * speed) * 1000).roundToInt()

fun chipLabel(c: ClimateState): String = "${formatTemp(c.setTemp)}° · " + (if (c.auto) "Auto" else "Manual")
