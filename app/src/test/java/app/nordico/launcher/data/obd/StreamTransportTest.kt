package app.nordico.launcher.data.obd

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Adaptador de mentira: cada comando terminado em `\r` enfileira a resposta devolvida por [reply] (null = nenhuma). */
private class Loopback(private val reply: (String) -> String?) {
    private val queue = ArrayDeque<Byte>()
    val written = StringBuilder()
    var closed = false

    fun enqueue(text: String) = text.toByteArray().forEach { queue.addLast(it) }

    val input = object : InputStream() {
        override fun available() = queue.size
        override fun read() = if (queue.isEmpty()) -1 else queue.removeFirst().toInt() and 0xFF
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (queue.isEmpty()) return -1
            val n = minOf(len, queue.size)
            repeat(n) { b[off + it] = queue.removeFirst() }
            return n
        }
    }

    val output = object : OutputStream() {
        private val line = StringBuilder()
        override fun write(b: Int) {
            written.append(b.toChar())
            if (b == '\r'.code) {
                reply(line.toString())?.let(::enqueue)
                line.clear()
            } else {
                line.append(b.toChar())
            }
        }
    }
}

private class TestStream(val adapter: Loopback) : StreamTransport() {
    init {
        attach(adapter.input, adapter.output) { adapter.closed = true }
    }
}

class StreamTransportTest {
    @Test fun sends_the_command_with_carriage_return_and_reads_until_the_prompt() = runTest {
        val a = Loopback { "41 0C 1A F8\r\r>" }
        val reply = TestStream(a).send("010C", 300)
        assertEquals("010C\r", a.written.toString())
        assertEquals("41 0C 1A F8", reply.trim())
        assertFalse(reply.contains('>'))
    }

    @Test fun stops_at_the_first_prompt() = runTest {
        val a = Loopback { "OK\r>" }
        assertEquals("OK", TestStream(a).send("ATE0", 300).trim())
    }

    @Test fun times_out_when_the_prompt_never_comes() = runTest {
        val t = TestStream(Loopback { "SEARCHING..." })
        try {
            t.send("0100", 300)
            fail("expected IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("timeout"))
        }
    }

    @Test fun late_reply_of_a_timed_out_command_is_discarded_before_the_next_one() = runTest {
        var calls = 0
        val a = Loopback { if (++calls == 1) null else "ELM327 v1.5\r\r>" }
        val t = TestStream(a)
        try {
            t.send("0100", 100)
            fail("expected IOException")
        } catch (_: IOException) {
        }
        a.enqueue("\r4100BE3FA813\r\r>") // chega tarde, depois do tempo
        assertEquals("ELM327 v1.5", t.send("ATI", 300).trim())
    }

    @Test fun times_out_when_nothing_arrives() = runTest {
        val t = TestStream(Loopback { null })
        try {
            t.send("0100", 100)
            fail("expected IOException")
        } catch (_: IOException) {
        }
    }

    @Test fun close_runs_the_close_callback() {
        val a = Loopback { null }
        TestStream(a).close()
        assertTrue(a.closed)
    }

    @Test fun send_before_attach_fails_cleanly() = runTest {
        try {
            StreamTransport().send("ATZ")
            fail("expected IOException")
        } catch (_: IOException) {
        }
    }
}
