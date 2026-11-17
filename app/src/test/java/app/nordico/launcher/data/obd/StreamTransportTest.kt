package app.nordico.launcher.data.obd

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

private class TestStream(input: InputStream, val out: ByteArrayOutputStream = ByteArrayOutputStream()) : StreamTransport(timeoutMs = 300) {
    var closed = false

    init {
        attach(input, out) { closed = true }
    }
}

class StreamTransportTest {
    @Test fun sends_the_command_with_carriage_return_and_reads_until_the_prompt() = runTest {
        val t = TestStream(ByteArrayInputStream("41 0C 1A F8\r\r>".toByteArray()))
        val reply = t.send("010C")
        assertEquals("010C\r", t.out.toString())
        assertEquals("41 0C 1A F8", reply.trim())
        assertFalse(reply.contains('>'))
    }

    @Test fun stops_at_the_first_prompt() = runTest {
        val t = TestStream(ByteArrayInputStream("OK\r>trailing".toByteArray()))
        assertEquals("OK", t.send("ATE0").trim())
    }

    @Test fun times_out_when_the_prompt_never_comes() = runTest {
        val t = TestStream(ByteArrayInputStream("SEARCHING...".toByteArray()))
        try {
            t.send("0100")
            fail("expected IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("timeout"))
        }
    }

    @Test fun times_out_when_nothing_arrives() = runTest {
        val t = TestStream(ByteArrayInputStream(ByteArray(0)))
        try {
            t.send("0100")
            fail("expected IOException")
        } catch (_: IOException) {
        }
    }

    @Test fun close_runs_the_close_callback() {
        val t = TestStream(ByteArrayInputStream(ByteArray(0)))
        t.close()
        assertTrue(t.closed)
    }

    @Test fun send_before_attach_fails_cleanly() = runTest {
        val t = StreamTransport()
        try {
            t.send("ATZ")
            fail("expected IOException")
        } catch (_: IOException) {
        }
    }
}
