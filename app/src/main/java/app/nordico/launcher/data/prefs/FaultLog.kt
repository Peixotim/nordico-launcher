package app.nordico.launcher.data.prefs

/** Resultado de uma limpeza no histórico. `Attempt` = enviada, ainda sem resultado (por exemplo, o app caiu no meio). */
enum class ClearResult { Attempt, Cleared, Returned, Refused, Unverified, NoResponse }

data class ClearRecord(val atMs: Long, val codes: List<String>, val result: ClearResult)

private const val HISTORY_LIMIT = 20

/**
 * Registro local das falhas (só no aparelho, nunca no carro):
 * - [marks]: códigos que o usuário marcou como corrigidos (código → quando). **Não apaga nada no carro.**
 * - [history]: as últimas 20 limpezas, gravadas **antes** do envio para uma queda não perder o que existia.
 */
data class FaultLog(
    val marks: Map<String, Long> = emptyMap(),
    val history: List<ClearRecord> = emptyList(),
) {
    /** Marca como corrigidos; uma marca já existente mantém a data original. */
    fun mark(codes: Collection<String>, atMs: Long): FaultLog =
        copy(marks = marks + codes.map(::norm).filter { it.isNotEmpty() && it !in marks }.associateWith { atMs })

    fun unmark(codes: Collection<String>): FaultLog = copy(marks = marks - codes.map(::norm).toSet())

    /** Descarta as marcas de códigos que não existem mais no carro. */
    fun prune(present: Collection<String>): FaultLog {
        val keep = present.map(::norm).toSet()
        return copy(marks = marks.filterKeys { it in keep })
    }

    fun begin(codes: List<String>, atMs: Long): FaultLog =
        copy(history = (history + ClearRecord(atMs, codes.map(::norm), ClearResult.Attempt)).takeLast(HISTORY_LIMIT))

    /** Completa o registro mais recente, se ele ainda for uma tentativa. */
    fun finish(result: ClearResult): FaultLog {
        val last = history.lastOrNull() ?: return this
        if (last.result != ClearResult.Attempt) return this
        return copy(history = history.dropLast(1) + last.copy(result = result))
    }

    /** Remove a tentativa mais recente se ela ainda estiver pendente (o comando foi bloqueado e nada foi enviado). */
    fun discard(): FaultLog {
        val last = history.lastOrNull() ?: return this
        return if (last.result == ClearResult.Attempt) copy(history = history.dropLast(1)) else this
    }

    /** Uma linha por item: `M|código|quando` e `H|quando|RESULTADO|código,código`. */
    fun encode(): String = buildString {
        marks.forEach { (code, at) -> append("M|").append(code).append('|').append(at).append('\n') }
        history.forEach { r -> append("H|").append(r.atMs).append('|').append(r.result.name).append('|').append(r.codes.joinToString(",")).append('\n') }
    }

    companion object {
        /** Linhas inválidas são ignoradas; texto ausente ou corrompido dá um registro vazio. */
        fun decode(text: String?): FaultLog {
            val marks = linkedMapOf<String, Long>()
            val history = mutableListOf<ClearRecord>()
            text.orEmpty().lineSequence().forEach { line ->
                val p = line.split('|', limit = 4)
                when (p.firstOrNull()) {
                    "M" -> if (p.size == 3) {
                        val code = norm(p[1])
                        val at = p[2].toLongOrNull()
                        if (code.isNotEmpty() && at != null) marks[code] = at
                    }
                    "H" -> if (p.size == 4) {
                        val at = p[1].toLongOrNull()
                        val result = ClearResult.entries.firstOrNull { it.name.equals(p[2], ignoreCase = true) }
                        if (at != null && result != null) {
                            history += ClearRecord(at, p[3].split(',').map(::norm).filter { it.isNotEmpty() }, result)
                        }
                    }
                }
            }
            return FaultLog(marks, history.takeLast(HISTORY_LIMIT))
        }

        private fun norm(code: String) = code.trim().uppercase()
    }
}
