package app.nordico.launcher.data.obd

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Transporte roteirizado: comando → resposta. Comando ausente = "?". */
class FakeTransport(private val script: Map<String, String>) : ObdTransport {
    val sent = mutableListOf<String>()
    var failWith: IOException? = null

    override suspend fun open() = Unit

    override suspend fun send(cmd: String): String {
        sent += cmd
        failWith?.let { throw it }
        return script[cmd] ?: "?"
    }

    override fun close() = Unit
}

class ObdSessionTest {
    private val healthy = mapOf(
        "ATZ" to "\r\rELM327 v1.5\r\r>",
        "ATE0" to "OK",
        "ATL0" to "OK",
        "ATS0" to "OK",
        "ATH0" to "OK",
        "ATSP0" to "OK",
        "ATI" to "ELM327 v1.5",
        "ATDPN" to "A6",
        "0100" to "SEARCHING...\r41 00 BE 3F A8 13",
        "0120" to "41 20 80 00 00 00",
        "0140" to "NO DATA",
        "0160" to "NO DATA",
        "010C" to "41 0C 1A F8",
        "03" to "43 01 01 33 00 00 00",
    )

    @Test fun init_sends_the_documented_sequence() = runTest {
        val t = FakeTransport(healthy)
        ObdSession(t).init()
        assertEquals(
            listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0", "ATI", "ATDPN", "0100", "0120", "0140", "0160"),
            t.sent,
        )
    }

    @Test fun init_extracts_version_protocol_and_supported_pids() = runTest {
        val r = ObdSession(FakeTransport(healthy)).init() as ObdResult.Ok
        assertEquals("ELM327 v1.5", r.value.adapter)
        assertEquals("A6", r.value.protocol)
        assertTrue(0x05 in r.value.supported)
        assertTrue(0x0C in r.value.supported)
        assertTrue(0x21 in r.value.supported)
    }

    @Test fun rejected_config_commands_are_ignored() = runTest {
        val clone = healthy + mapOf("ATL0" to "?", "ATS0" to "?")
        assertTrue(ObdSession(FakeTransport(clone)).init() is ObdResult.Ok)
    }

    @Test fun unable_to_connect_fails_init() = runTest {
        val dead = healthy + mapOf("0100" to "UNABLE TO CONNECT")
        assertEquals(ObdResult.Err(ObdError.UnableToConnect), ObdSession(FakeTransport(dead)).init())
    }

    @Test fun later_blocks_without_answer_do_not_fail_init() = runTest {
        val r = ObdSession(FakeTransport(healthy + mapOf("0120" to "NO DATA"))).init()
        assertTrue(r is ObdResult.Ok)
    }

    @Test fun reads_a_pid() = runTest {
        assertEquals(ObdResult.Ok(1726.0), ObdSession(FakeTransport(healthy)).read(ObdPid.Rpm))
    }

    @Test fun no_data_is_a_typed_error() = runTest {
        val t = FakeTransport(healthy + mapOf("010C" to "NO DATA"))
        assertEquals(ObdResult.Err(ObdError.NoData), ObdSession(t).read(ObdPid.Rpm))
    }

    @Test fun io_failure_becomes_an_error_instead_of_propagating() = runTest {
        val t = FakeTransport(healthy).apply { failWith = IOException("socket closed") }
        assertEquals(ObdResult.Err(ObdError.Unknown), ObdSession(t).read(ObdPid.Rpm))
        assertEquals(ObdResult.Err(ObdError.Unknown), ObdSession(t).readFaults())
    }

    @Test fun reads_faults() = runTest {
        assertEquals(ObdResult.Ok(listOf(Dtc("P0133"))), ObdSession(FakeTransport(healthy)).readFaults())
    }

    @Test fun only_reads_never_writes() = runTest {
        val t = FakeTransport(healthy)
        val s = ObdSession(t)
        s.init()
        s.read(ObdPid.Rpm)
        s.readFaults()
        // Somente comandos AT, modo 01 e modo 03 (spec 5).
        assertTrue(t.sent.all { it.startsWith("AT") || it.startsWith("01") || it == "03" })
    }
}
