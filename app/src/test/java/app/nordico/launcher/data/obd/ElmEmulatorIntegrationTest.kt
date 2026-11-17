package app.nordico.launcher.data.obd

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Verificação manual contra um simulador de ELM327 de verdade (`pip install ELM327-emulator`,
 * `python -m elm -n 35000 -s car`). Só roda com `ELM_TCP=127.0.0.1:35000`; sem a variável é ignorado.
 */
class ElmEmulatorIntegrationTest {
    private fun target(): Pair<String, Int> {
        val env = System.getenv("ELM_TCP")
        assumeTrue("defina ELM_TCP=host:porta para rodar", env != null)
        val (host, port) = env!!.split(":")
        return host to port.toInt()
    }

    @Test fun real_emulator_end_to_end() = runBlocking {
        val (host, port) = target()
        val transport = TcpTransport(host, port)
        transport.open()
        try {
            val session = ObdSession(transport)
            val init = session.init()
            assertTrue("init=$init", init is ObdResult.Ok)
            val supported = (init as ObdResult.Ok).value.supported
            println("adapter=${init.value.adapter} protocol=${init.value.protocol} supported=${supported.sorted().map { "%02X".format(it) }}")

            for (pid in ObdPid.entries.filter { it.id in supported }) {
                val r = session.read(pid)
                println("read $pid -> $r")
                assertTrue("$pid -> $r", r is ObdResult.Ok)
            }
            val rpm = session.read(ObdPid.Rpm) as ObdResult.Ok
            assertTrue(rpm.value in 0.0..9000.0)
            val faults = session.readFaults()
            println("faults -> $faults")
            assertTrue("faults=$faults", faults is ObdResult.Ok)
        } finally {
            transport.close()
        }
        assertEquals(true, true)
    }
}
