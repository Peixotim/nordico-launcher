package app.nordico.launcher.data.obd

import kotlinx.coroutines.delay
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

private const val POLL_MS = 10L

/**
 * Transporte ELM327 sobre um par de streams (socket TCP, socket Bluetooth…).
 * Pergunta e resposta, uma por vez: escreve `cmd\r` e lê até o prompt `>`.
 */
open class StreamTransport(private val timeoutMs: Long = 3000) : ObdTransport {
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var onClose: () -> Unit = {}

    protected fun attach(input: InputStream, output: OutputStream, onClose: () -> Unit = {}) {
        this.input = input
        this.output = output
        this.onClose = onClose
    }

    override suspend fun open() = Unit

    override suspend fun send(cmd: String): String {
        val inp = input ?: throw IOException("not connected")
        val out = output ?: throw IOException("not connected")
        out.write("$cmd\r".toByteArray())
        out.flush()

        val reply = StringBuilder()
        // Sondagem por available(): serve a sockets e fluxos em memória, e o laço tem fim mesmo sem resposta.
        repeat((timeoutMs / POLL_MS).toInt()) {
            val n = inp.available()
            if (n > 0) {
                val buf = ByteArray(n)
                val read = inp.read(buf)
                if (read < 0) throw IOException("closed")
                reply.append(String(buf, 0, read))
                val prompt = reply.indexOf('>')
                if (prompt >= 0) return reply.substring(0, prompt)
            } else {
                delay(POLL_MS)
            }
        }
        throw IOException("timeout waiting for the adapter prompt")
    }

    override fun close() {
        runCatching { input?.close() }
        runCatching { output?.close() }
        onClose()
        input = null
        output = null
    }
}
