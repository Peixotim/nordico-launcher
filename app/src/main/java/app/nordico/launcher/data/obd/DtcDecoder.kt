package app.nordico.launcher.data.obd

data class Dtc(val code: String)

private val LETTERS = charArrayOf('P', 'C', 'B', 'U')

/**
 * Códigos de falha da resposta do modo 03 (somente leitura). Em CAN o primeiro byte depois do `43`
 * é a quantidade. Pares `0000` são ignorados.
 */
// Trade-off: só a primeira linha de resposta; respostas de vários quadros (mais de 3 códigos) ficam de fora.
fun decodeDtcs(raw: String, canCount: Boolean): List<Dtc> {
    val line = cleanReply(raw).firstOrNull { it.startsWith("43") } ?: return emptyList()
    var bytes = line.removePrefix("43").let { hex ->
        if (hex.length % 2 != 0 || !hex.all { it in '0'..'9' || it in 'A'..'F' }) return emptyList()
        hex.chunked(2).map { it.toInt(16) }
    }
    if (canCount && bytes.isNotEmpty()) bytes = bytes.drop(1)
    return bytes.chunked(2)
        .filter { it.size == 2 && (it[0] != 0 || it[1] != 0) }
        .map { (a, b) ->
            val letter = LETTERS[a shr 6]
            val digit = (a shr 4) and 0x3
            Dtc("%c%d%X%02X".format(letter, digit, a and 0xF, b))
        }
}
