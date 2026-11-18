package app.nordico.launcher.data.obd

import kotlinx.coroutines.CancellationException
import java.io.IOException

const val DEFAULT_TIMEOUT_MS = 3_000L

/** A primeira consulta (`0100`) dispara a busca automática de protocolo, que pode levar vários segundos. */
const val SEARCH_TIMEOUT_MS = 10_000L

/** Canal com o adaptador ELM327 (Bluetooth, TCP…). `send` devolve a resposta completa, sem o prompt `>`. */
interface ObdTransport {
    suspend fun open()
    suspend fun send(cmd: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): String
    fun close()
}

data class InitResult(val adapter: String, val protocol: String, val supported: Set<Int>)

/** Comandos que o Nórdico pode enviar: configuração `AT`, leitura (`01xx`, `03`) e o apagar (`04`). */
private val ALLOWED = Regex("^(AT[A-Z0-9 ]{1,8}|01[0-9A-F]{2}|03|04)$")

/**
 * Conversa com o ELM327. Leitura (`AT...`, modo 01 e 03) e, só em [clearFaults], o modo 04.
 * Nunca lança por falha do adaptador; cancelamento de corrotina continua propagando.
 */
class ObdSession(private val t: ObdTransport) {
    private val configCommands = listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")

    suspend fun init(): ObdResult<InitResult> = safe {
        // Clones v2.1 rejeitam alguns comandos de configuração com "?": ignoramos a resposta.
        configCommands.forEach { send(it) }
        val adapter = firstLine(send("ATI")).ifEmpty { "ELM327" }
        val protocol = firstLine(send("ATDPN"))

        val supported = mutableSetOf<Int>()
        when (val r = parseData(send("0100", SEARCH_TIMEOUT_MS), 1, 0x00)) {
            is ObdResult.Err -> return@safe r
            is ObdResult.Ok -> supported += supportedPids(0x00, r.value)
        }
        // Blocos seguintes são opcionais: falha neles não derruba a inicialização.
        for (base in listOf(0x20, 0x40, 0x60)) {
            val reply = send("01%02X".format(base))
            (parseData(reply, 1, base) as? ObdResult.Ok)?.let { supported += supportedPids(base, it.value) }
        }
        ObdResult.Ok(InitResult(adapter, protocol, supported))
    }

    suspend fun read(pid: ObdPid): ObdResult<Double> = safe {
        when (val r = parseData(send("01%02X".format(pid.id)), 1, pid.id)) {
            is ObdResult.Err -> r
            is ObdResult.Ok ->
                if (r.value.size < pid.bytes) ObdResult.Err(ObdError.Unknown) else ObdResult.Ok(pid.decode(r.value))
        }
    }

    /** Códigos de falha armazenados. `NO DATA` significa "nenhum código", não erro. */
    suspend fun readFaults(canCount: Boolean = true): ObdResult<List<Dtc>> = safe {
        val raw = send("03")
        when (val r = parseData(raw, 3, null)) {
            is ObdResult.Ok -> ObdResult.Ok(decodeDtcs(raw, canCount))
            is ObdResult.Err ->
                if (r.error == ObdError.NoData) ObdResult.Ok(emptyList()) else r
        }
    }

    /**
     * Apaga os códigos de falha (modo 04): o **único** comando de escrita do Nórdico. Só deve ser chamado
     * depois da confirmação do usuário e com o carro parado (guardas em [ElmObdRepository]). Nunca lança.
     */
    suspend fun clearFaults(): ClearReply = try {
        parseClear(send("04", CLEAR_TIMEOUT_MS))
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        ClearReply.NoResponse
    }

    /** Único ponto de envio: nenhum comando fora da lista permitida chega ao adaptador. */
    private suspend fun send(cmd: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): String {
        require(ALLOWED.matches(cmd)) { "command not allowed: $cmd" }
        return t.send(cmd, timeoutMs)
    }

    private fun firstLine(raw: String): String =
        raw.split('\r', '\n').map { it.replace(">", "").trim() }.firstOrNull { it.isNotEmpty() && it != "?" }.orEmpty()

    private inline fun <T> safe(block: () -> ObdResult<T>): ObdResult<T> = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        ObdResult.Err(ObdError.Unknown)
    }
}
