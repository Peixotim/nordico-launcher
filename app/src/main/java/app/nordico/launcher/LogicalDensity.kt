package app.nordico.launcher

/** Largura lógica da tela em dp. Todo o layout é desenhado nesse canvas (spec 10.4). */
const val LOGICAL_WIDTH_DP = 1280

/** Densidade que faz [widthPx] pixels físicos valerem exatamente [LOGICAL_WIDTH_DP] dp. */
fun logicalDensity(widthPx: Int): Float = widthPx / LOGICAL_WIDTH_DP.toFloat()
