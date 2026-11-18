package app.nordico.launcher.data.obd

/** O ECU pode levar mais que uma leitura comum para apagar. */
const val CLEAR_TIMEOUT_MS = 8_000L

/** Pausa entre o `04` aceito e a releitura: a ECU ainda está reiniciando seus monitores. */
const val CLEAR_SETTLE_MS = 1_500L

/** Resposta ao comando `04` (apagar códigos), já interpretada. */
sealed interface ClearReply {
    data object Accepted : ClearReply
    data class Refused(val nrc: Int) : ClearReply
    data object NoResponse : ClearReply
    data class Failed(val error: ObdError) : ClearReply
}

private val NEGATIVE = Regex("^7F04([0-9A-F]{2})$")

/**
 * Interpreta a resposta do modo 04: `44` é positivo, `7F 04 xx` é negativo (`22` = condições incorretas).
 * Com várias ECUs, um `44` basta: quem decide se limpou é a releitura. Nunca lança.
 */
// Trade-off: `7F 04 78` (resposta pendente) é tratado como recusa; o ELM327 normalmente espera sozinho por ele.
fun parseClear(raw: String): ClearReply {
    val lines = cleanReply(raw)
    if (lines.isEmpty()) return ClearReply.NoResponse
    if (lines.any { it == "44" }) return ClearReply.Accepted
    lines.firstNotNullOfOrNull { NEGATIVE.matchEntire(it) }?.let {
        return ClearReply.Refused(it.groupValues[1].toInt(16))
    }
    return when (val e = errorOf(lines.first())) {
        ObdError.NoData -> ClearReply.NoResponse
        else -> ClearReply.Failed(e)
    }
}
