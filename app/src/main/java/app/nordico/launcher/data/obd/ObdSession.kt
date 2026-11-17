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

/**
 * Conversa com o ELM327. Somente leitura: comandos `AT...`, modo 01 e modo 03 (spec 5).
 * Nunca lança por falha do adaptador; cancelamento de corrotina continua propagando.
 */
class ObdSession(private val t: ObdTransport) {
    private val configCommands = listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")

    suspend fun init(): ObdResult<InitResult> = safe {
        // Clones v2.1 rejeitam alguns comandos de configuração com "?": ignoramos a resposta.
        configCommands.forEach { t.send(it) }
        val adapter = firstLine(t.send("ATI")).ifEmpty { "ELM327" }
        val protocol = firstLine(t.send("ATDPN"))

        val supported = mutableSetOf<Int>()
        when (val r = parseData(t.send("0100", SEARCH_TIMEOUT_MS), 1, 0x00)) {
            is ObdResult.Err -> return@safe r
            is ObdResult.Ok -> supported += supportedPids(0x00, r.value)
        }
        // Blocos seguintes são opcionais: falha neles não derruba a inicialização.
        for (base in listOf(0x20, 0x40, 0x60)) {
            val reply = t.send("01%02X".format(base))
            (parseData(reply, 1, base) as? ObdResult.Ok)?.let { supported += supportedPids(base, it.value) }
        }
        ObdResult.Ok(InitResult(adapter, protocol, supported))
    }

    suspend fun read(pid: ObdPid): ObdResult<Double> = safe {
        when (val r = parseData(t.send("01%02X".format(pid.id)), 1, pid.id)) {
            is ObdResult.Err -> r
            is ObdResult.Ok ->
                if (r.value.size < pid.bytes) ObdResult.Err(ObdError.Unknown) else ObdResult.Ok(pid.decode(r.value))
        }
    }

    /** Códigos de falha armazenados. `NO DATA` significa "nenhum código", não erro. */
    suspend fun readFaults(canCount: Boolean = true): ObdResult<List<Dtc>> = safe {
        val raw = t.send("03")
        when (val r = parseData(raw, 3, null)) {
            is ObdResult.Ok -> ObdResult.Ok(decodeDtcs(raw, canCount))
            is ObdResult.Err ->
                if (r.error == ObdError.NoData) ObdResult.Ok(emptyList()) else r
        }
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
