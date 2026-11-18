package app.nordico.launcher.data.obd

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private val ADAPTER = ObdAdapter("OBDII", "00:1D:A5:68:98:8B")
private val CONNECTED = ObdState.Connected("A6", "ELM327 v1.5")

/** Transporte cujas respostas podem mudar durante o teste (por exemplo, a releitura depois do `04`). */
private class CarTransport(private val script: MutableMap<String, String>, private val onSend: (String) -> Unit) : ObdTransport {
    val sent = mutableListOf<String>()
    var failWith: IOException? = null

    override suspend fun open() = Unit

    override suspend fun send(cmd: String, timeoutMs: Long): String {
        sent += cmd
        onSend(cmd)
        failWith?.let { throw it }
        return script[cmd] ?: "?"
    }

    override fun close() = Unit
}

private fun carScript(speedHex: String = "00") = mutableMapOf(
    "ATZ" to "ELM327 v1.5", "ATE0" to "OK", "ATL0" to "OK", "ATS0" to "OK", "ATH0" to "OK", "ATSP0" to "OK",
    "ATI" to "ELM327 v1.5", "ATDPN" to "A6",
    "0100" to "41 00 BE 3F A8 13", "0120" to "NO DATA", "0140" to "NO DATA", "0160" to "NO DATA",
    "010D" to "41 0D $speedHex", "010C" to "41 0C 0C 80", "0105" to "41 05 7B", "010F" to "41 0F 3C",
    "0104" to "41 04 40", "0111" to "41 11 20", "0142" to "NO DATA", "012F" to "NO DATA",
    "0101" to "41 01 82 07 E5 00",
    "03" to "43 02 01 33 04 20",
    "04" to "44",
)

@OptIn(ExperimentalCoroutinesApi::class)
class ElmObdClearTest {
    private class Rig(scope: TestScope, val script: MutableMap<String, String>, afterClear: MutableMap<String, String>.() -> Unit = {}) {
        val created = mutableListOf<CarTransport>()
        val repo = ElmObdRepository(
            scope = scope.backgroundScope,
            visible = MutableStateFlow(true),
            adapters = { listOf(ADAPTER) },
            selected = MutableStateFlow(ADAPTER.address),
            select = { },
            transportFor = { CarTransport(script) { cmd -> if (cmd == "04") script.afterClear() }.also { created += it } },
            now = { scope.testScheduler.currentTime },
        )
        val sent get() = created.flatMap { it.sent }
    }

    private fun clean(): MutableMap<String, String>.() -> Unit = {
        this["03"] = "NO DATA"
        this["0101"] = "41 01 00 07 E5 00"
    }

    // ---- guarda pura ----

    @Test fun the_gate_blocks_what_must_not_be_cleared() {
        val parked = VehicleTelemetry(speedKmh = 0, milOn = true, faultCount = 2)
        assertEquals(ClearBlock.NotConnected, clearGate(ObdState.NoAdapter, parked, listOf(Dtc("P0420"))))
        assertEquals(ClearBlock.NotConnected, clearGate(ObdState.Lost, parked, listOf(Dtc("P0420"))))
        assertEquals(ClearBlock.Moving, clearGate(CONNECTED, parked.copy(speedKmh = 5), listOf(Dtc("P0420"))))
        assertEquals(ClearBlock.Moving, clearGate(CONNECTED, parked.copy(speedKmh = null), listOf(Dtc("P0420"))))
        assertEquals(ClearBlock.NothingToClear, clearGate(CONNECTED, VehicleTelemetry(speedKmh = 0, milOn = false), emptyList()))
    }

    @Test fun the_gate_allows_a_parked_car_with_something_to_clear() {
        val parked = VehicleTelemetry(speedKmh = 0, milOn = false, faultCount = 0)
        assertEquals(null, clearGate(CONNECTED, parked, listOf(Dtc("P0420"))))
        assertEquals(null, clearGate(CONNECTED, parked.copy(milOn = true), null))
        assertEquals(null, clearGate(CONNECTED, parked.copy(faultCount = 1), emptyList()))
    }

    // ---- fluxo ----

    @Test fun clears_waits_rereads_and_reports_cleared() = runTest {
        val r = Rig(this, carScript(), clean())
        advanceTimeBy(3_000)
        r.repo.readFaults()
        runCurrent()
        assertEquals(listOf(Dtc("P0133"), Dtc("P0420")), r.repo.faults.value)

        r.repo.clearFaults()
        runCurrent()
        assertEquals(ClearState.Clearing, r.repo.clearState.value)
        advanceTimeBy(CLEAR_SETTLE_MS - 100)
        assertEquals(ClearState.Clearing, r.repo.clearState.value)
        advanceTimeBy(1_000)

        assertEquals(ClearState.Done(ClearOutcome.Cleared), r.repo.clearState.value)
        assertEquals(emptyList<Dtc>(), r.repo.faults.value)
        assertEquals(false, r.repo.telemetry.value.milOn)
        assertEquals(0, r.repo.telemetry.value.faultCount)
    }

    @Test fun polling_is_paused_from_the_04_until_the_reread() = runTest {
        val r = Rig(this, carScript(), clean())
        advanceTimeBy(3_000)
        r.repo.clearFaults()
        advanceTimeBy(CLEAR_SETTLE_MS + 1_000)
        val sent = r.sent
        val from = sent.indexOf("04")
        val to = sent.subList(from, sent.size).indexOf("03") + from
        val between = sent.subList(from + 1, to)
        assertTrue("sent between: $between", between.none { it.startsWith("01") })
    }

    @Test fun polling_resumes_after_clearing() = runTest {
        val r = Rig(this, carScript(), clean())
        advanceTimeBy(3_000)
        r.repo.clearFaults()
        advanceTimeBy(CLEAR_SETTLE_MS + 1_000)
        val before = r.sent.count { it == "010D" }
        advanceTimeBy(2_000)
        assertTrue(r.sent.count { it == "010D" } > before)
    }

    @Test fun codes_that_come_back_are_reported() = runTest {
        val r = Rig(this, carScript()) // o 04 é aceito, mas a releitura continua com códigos
        advanceTimeBy(3_000)
        r.repo.clearFaults()
        advanceTimeBy(CLEAR_SETTLE_MS + 1_000)
        assertEquals(ClearState.Done(ClearOutcome.Returned(listOf(Dtc("P0133"), Dtc("P0420")))), r.repo.clearState.value)
    }

    @Test fun a_refusal_is_reported_without_rereading() = runTest {
        val s = carScript().apply { this["04"] = "7F 04 22" }
        val r = Rig(this, s)
        advanceTimeBy(3_000)
        r.repo.clearFaults()
        advanceTimeBy(CLEAR_SETTLE_MS + 1_000)
        assertEquals(ClearState.Done(ClearOutcome.Refused(0x22)), r.repo.clearState.value)
        assertEquals(0, r.sent.count { it == "03" })
    }

    @Test fun a_failed_reread_is_unverified() = runTest {
        val r = Rig(this, carScript()) { this["03"] = "?" }
        advanceTimeBy(3_000)
        r.repo.clearFaults()
        advanceTimeBy(CLEAR_SETTLE_MS + 1_000)
        assertEquals(ClearState.Done(ClearOutcome.Unverified), r.repo.clearState.value)
    }

    @Test fun no_answer_is_no_response() = runTest {
        val r = Rig(this, carScript().apply { this["04"] = "NO DATA" })
        advanceTimeBy(3_000)
        r.repo.clearFaults()
        advanceTimeBy(CLEAR_SETTLE_MS + 1_000)
        assertEquals(ClearState.Done(ClearOutcome.NoResponse), r.repo.clearState.value)
    }

    @Test fun losing_the_connection_during_clear_is_no_response_and_it_reconnects() = runTest {
        val s = carScript()
        lateinit var rig: Rig
        rig = Rig(this, s) { rig.created.forEach { it.failWith = IOException("socket closed") } }
        advanceTimeBy(3_000)
        rig.repo.clearFaults()
        advanceTimeBy(1_000)
        assertEquals(ClearState.Done(ClearOutcome.NoResponse), rig.repo.clearState.value)
        advanceTimeBy(8_000)
        assertTrue(rig.created.size >= 2)
        assertTrue(rig.repo.state.value is ObdState.Connected)
    }

    @Test fun a_moving_car_never_receives_the_04() = runTest {
        val r = Rig(this, carScript("05"))
        advanceTimeBy(3_000)
        r.repo.clearFaults()
        advanceTimeBy(CLEAR_SETTLE_MS + 1_000)
        assertEquals(ClearState.Done(ClearOutcome.Blocked(ClearBlock.Moving)), r.repo.clearState.value)
        assertEquals(0, r.sent.count { it == "04" })
    }

    @Test fun a_second_tap_while_clearing_is_ignored() = runTest {
        val r = Rig(this, carScript(), clean())
        advanceTimeBy(3_000)
        r.repo.clearFaults()
        r.repo.clearFaults()
        runCurrent()
        r.repo.clearFaults()
        advanceTimeBy(CLEAR_SETTLE_MS + 1_000)
        assertEquals(1, r.sent.count { it == "04" })
    }

    @Test fun dismissing_the_result_goes_back_to_idle() = runTest {
        val r = Rig(this, carScript(), clean())
        advanceTimeBy(3_000)
        r.repo.clearFaults()
        advanceTimeBy(CLEAR_SETTLE_MS + 1_000)
        r.repo.dismissClearResult()
        assertEquals(ClearState.Idle, r.repo.clearState.value)
    }

    @Test fun without_an_adapter_clearing_is_blocked() = runTest {
        val repo = NoAdapterObdRepository()
        repo.clearFaults()
        assertEquals(ClearState.Done(ClearOutcome.Blocked(ClearBlock.NotConnected)), repo.clearState.value)
    }
}
