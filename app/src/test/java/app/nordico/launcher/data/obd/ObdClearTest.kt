package app.nordico.launcher.data.obd

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class ObdClearTest {
    @Test fun a_positive_reply_is_accepted() {
        assertEquals(ClearReply.Accepted, parseClear("44"))
        assertEquals(ClearReply.Accepted, parseClear("04\r44\r\r>"))
    }

    @Test fun several_ecus_are_accepted_even_when_one_refuses() {
        assertEquals(ClearReply.Accepted, parseClear("44\r44"))
        assertEquals(ClearReply.Accepted, parseClear("44\r7F0422"))
    }

    @Test fun a_negative_reply_carries_the_response_code() {
        assertEquals(ClearReply.Refused(0x22), parseClear("7F0422"))
        assertEquals(ClearReply.Refused(0x11), parseClear("7F 04 11"))
        assertEquals(ClearReply.Refused(0x21), parseClear("7F0421"))
    }

    @Test fun no_answer_is_not_an_error_of_the_adapter() {
        assertEquals(ClearReply.NoResponse, parseClear("NO DATA"))
        assertEquals(ClearReply.NoResponse, parseClear(""))
    }

    @Test fun adapter_errors_are_typed() {
        assertEquals(ClearReply.Failed(ObdError.CanError), parseClear("BUS BUSY"))
        assertEquals(ClearReply.Failed(ObdError.CanError), parseClear("CAN ERROR"))
        assertEquals(ClearReply.Failed(ObdError.UnableToConnect), parseClear("UNABLE TO CONNECT"))
        assertEquals(ClearReply.Failed(ObdError.Unknown), parseClear("?"))
    }

    @Test fun a_reply_from_another_mode_is_never_accepted() {
        assertEquals(ClearReply.Failed(ObdError.Unknown), parseClear("43010133"))
        assertEquals(ClearReply.Failed(ObdError.Unknown), parseClear("41 0C 1A F8"))
    }

    @Test fun garbage_never_throws() {
        listOf("\u0000\u0001", "44ZZ", "7F04", "7F04GG", ">>>>", "\r\r\r").forEach { parseClear(it) }
    }

    @Test fun clear_sends_exactly_one_04_with_the_long_timeout() = runTest {
        val t = FakeTransport(mapOf("04" to "44"))
        val reply = ObdSession(t).clearFaults()
        assertEquals(ClearReply.Accepted, reply)
        assertEquals(listOf("04"), t.sent)
        assertEquals(CLEAR_TIMEOUT_MS, t.timeouts.getValue("04"))
        assertTrue(CLEAR_TIMEOUT_MS > DEFAULT_TIMEOUT_MS)
    }

    @Test fun clear_reports_a_refusal() = runTest {
        assertEquals(ClearReply.Refused(0x22), ObdSession(FakeTransport(mapOf("04" to "7F 04 22"))).clearFaults())
    }

    @Test fun an_io_error_during_clear_is_no_response() = runTest {
        val t = FakeTransport(emptyMap()).apply { failWith = IOException("socket closed") }
        assertEquals(ClearReply.NoResponse, ObdSession(t).clearFaults())
    }

    @Test fun cancellation_is_not_swallowed() = runTest {
        val cancelling = object : ObdTransport {
            override suspend fun open() = Unit
            override suspend fun send(cmd: String, timeoutMs: Long): String = throw CancellationException("cancelled")
            override fun close() = Unit
        }
        try {
            ObdSession(cancelling).clearFaults()
            fail("expected cancellation")
        } catch (_: CancellationException) {
        }
    }

    @Test fun only_allowed_commands_ever_reach_the_adapter() = runTest {
        val script = mapOf(
            "ATZ" to "ELM327 v1.5", "ATE0" to "OK", "ATL0" to "OK", "ATS0" to "OK", "ATH0" to "OK", "ATSP0" to "OK",
            "ATI" to "ELM327 v1.5", "ATDPN" to "A6",
            "0100" to "41 00 FF FF FF FF", "0120" to "41 20 FF FF FF FF", "0140" to "41 40 FF FF FF FF", "0160" to "41 60 FF FF FF FF",
            "03" to "43 00 00 00 00 00 00", "04" to "44",
        )
        val t = FakeTransport(script)
        val s = ObdSession(t)
        s.init()
        ObdPid.entries.forEach { s.read(it) }
        s.readFaults()
        val beforeClear = t.sent.toList()
        s.clearFaults()

        val allowed = Regex("^(AT[A-Z0-9 ]{1,8}|01[0-9A-F]{2}|03|04)$")
        t.sent.forEach { assertTrue("command not allowed: $it", allowed.matches(it)) }
        // O 04 só aparece depois da chamada explícita de clearFaults.
        assertFalse(beforeClear.contains("04"))
        assertEquals(1, t.sent.count { it == "04" })
    }
}
