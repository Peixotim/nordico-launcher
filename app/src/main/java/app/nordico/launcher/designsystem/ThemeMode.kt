package app.nordico.launcher.designsystem

import app.nordico.launcher.data.prefs.ScreenMode

/**
 * Decide se a interface fica no modo Noite.
 * Auto segue o farol quando o carro informa; sem sinal, Noite das 18h (inclusive) às 6h (exclusive).
 */
fun resolveDark(mode: ScreenMode, headlightsOn: Boolean?, hour: Int): Boolean = when (mode) {
    ScreenMode.Day -> false
    ScreenMode.Night -> true
    // Trade-off: horário fixo; trocar por pôr do sol calculado se incomodar no inverno
    ScreenMode.Auto -> headlightsOn ?: (hour >= 18 || hour < 6)
}
