package app.nordico.launcher.data.obd

enum class ObdError { NoData, UnableToConnect, CanError, Stopped, Unknown, Empty }

sealed interface ObdResult<out T> {
    data class Ok<T>(val value: T) : ObdResult<T>
    data class Err(val error: ObdError) : ObdResult<Nothing>
}

private val HEX = Regex("^[0-9A-F]*$")
private val NOISE_PREFIXES = listOf("SEARCHING", "BUSINIT")

/**
 * Linhas úteis de uma resposta do ELM327: maiúsculas, sem espaços, sem prompt, sem eco
 * (quando [echoOf] é informado) e sem as mensagens de progresso.
 */
fun cleanReply(raw: String, echoOf: String? = null): List<String> {
    val echo = echoOf?.filterNot { it.isWhitespace() }?.uppercase()
    return raw.split('\r', '\n')
        .map { it.replace(">", "").filterNot { c -> c.isWhitespace() }.uppercase() }
        .filter { it.isNotEmpty() && it != echo && NOISE_PREFIXES.none { p -> it.startsWith(p) } }
}

private fun errorOf(line: String): ObdError = when (line) {
    "NODATA" -> ObdError.NoData
    "UNABLETOCONNECT" -> ObdError.UnableToConnect
    "CANERROR", "BUSERROR", "FBERROR", "BUSBUSY" -> ObdError.CanError
    "STOPPED" -> ObdError.Stopped
    else -> ObdError.Unknown
}

private fun hexBytes(s: String): List<Int>? =
    if (s.length % 2 != 0 || !HEX.matches(s)) null else s.chunked(2).map { it.toInt(16) }

/**
 * Bytes de dados depois do cabeçalho `4x [pid]`. Com várias ECUs vale a primeira linha de dados.
 * Nunca lança: qualquer outra coisa vira [ObdResult.Err].
 */
fun parseData(raw: String, mode: Int, pid: Int?): ObdResult<List<Int>> {
    val lines = cleanReply(raw)
    if (lines.isEmpty()) return ObdResult.Err(ObdError.Empty)
    val header = "%02X".format(0x40 + mode) + (pid?.let { "%02X".format(it) } ?: "")
    for (line in lines) {
        if (!line.startsWith(header)) continue
        hexBytes(line.removePrefix(header))?.let { return ObdResult.Ok(it) }
    }
    val first = lines.first()
    return ObdResult.Err(errorOf(first))
}

/** Bits (do mais significativo) da resposta `01 00/20/40/60`: bit i indica o PID `base + 1 + i`. */
fun supportedPids(base: Int, mask: List<Int>): Set<Int> {
    val out = mutableSetOf<Int>()
    mask.take(4).forEachIndexed { byteIndex, b ->
        for (bit in 0..7) if (b and (0x80 shr bit) != 0) out += base + 1 + byteIndex * 8 + bit
    }
    return out
}
