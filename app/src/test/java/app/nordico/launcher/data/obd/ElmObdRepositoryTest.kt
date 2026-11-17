package app.nordico.launcher.data.obd

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private val ALL_NO_DATA = mapOf(
    "ATZ" to "ELM327 v1.5", "ATE0" to "OK", "ATL0" to "OK", "ATS0" to "OK", "ATH0" to "OK", "ATSP0" to "OK",
    "ATI" to "ELM327 v1.5", "ATDPN" to "A6",
    "0100" to "41 00 BE 3F A8 13", "0120" to "NO DATA", "0140" to "NO DATA", "0160" to "NO DATA",
    "010D" to "NO DATA", "010C" to "NO DATA", "0105" to "NO DATA", "010F" to "NO DATA",
    "0104" to "NO DATA", "0111" to "NO DATA", "0142" to "NO DATA", "012F" to "NO DATA",
)

private val CAR = ALL_NO_DATA + mapOf(
    "010D" to "41 0D 3C", "010C" to "41 0C 1A F8", "0105" to "41 05 7B", "010F" to "41 0F 3C",
    "0104" to "41 04 FF", "0111" to "41 11 00", "0142" to "NO DATA", "012F" to "NO DATA",
    "03" to "43 01 01 33 00 00 00",
)

private val ADAPTER = ObdAdapter("OBDII", "00:1D:A5:68:98:8B")

@OptIn(ExperimentalCoroutinesApi::class)
class ElmObdRepositoryTest {
    private class Rig(scope: TestScope, script: Map<String, String>, address: String? = ADAPTER.address) {
        val selected = MutableStateFlow(address)
        val visible = MutableStateFlow(true)
        val created = mutableListOf<FakeTransport>()
        var nextScript = script
        val repo = ElmObdRepository(
            scope = scope.backgroundScope,
            visible = visible,
            adapters = { listOf(ADAPTER) },
            selected = selected,
            select = { selected.value = it },
            transportFor = { FakeTransport(nextScript).also { created += it } },
            now = { scope.testScheduler.currentTime },
        )
    }

    @Test fun without_a_selected_adapter_the_state_is_no_adapter() = runTest {
        val r = Rig(this, CAR, address = null)
        runCurrent()
        assertEquals(ObdState.NoAdapter, r.repo.state.value)
        assertTrue(r.created.isEmpty())
    }

    @Test fun connects_and_publishes_telemetry() = runTest {
        val r = Rig(this, CAR)
        advanceTimeBy(3_000)
        assertEquals(ObdState.Connected("A6", "ELM327 v1.5"), r.repo.state.value)
        val t = r.repo.telemetry.value
        assertEquals(60, t.speedKmh)
        assertEquals(1726, t.rpm)
        assertEquals(83, t.coolantC)
        assertEquals(20, t.intakeC)
        assertEquals(100, t.loadPct)
        assertEquals(0, t.throttlePct)
    }

    @Test fun unsupported_pid_stays_null_and_stops_being_requested_after_three_no_data() = runTest {
        val r = Rig(this, CAR + ("0105" to "NO DATA"))
        advanceTimeBy(20_000)
        assertNull(r.repo.telemetry.value.coolantC)
        assertEquals(3, r.created.first().sent.count { it == "0105" })
    }

    @Test fun a_car_without_any_pid_does_not_spin() = runTest {
        val r = Rig(this, ALL_NO_DATA)
        advanceTimeBy(30_000)
        assertTrue("sent=${r.created.first().sent.size}", r.created.first().sent.size < 60)
        assertEquals(VehicleTelemetry(), r.repo.telemetry.value)
        assertTrue(r.repo.state.value is ObdState.Connected)
    }

    @Test fun losing_the_adapter_goes_to_lost_and_reconnects_with_backoff() = runTest {
        val r = Rig(this, CAR)
        advanceTimeBy(1_000)
        assertTrue(r.repo.state.value is ObdState.Connected)
        r.created.first().failWith = IOException("socket closed")
        advanceTimeBy(1_000)
        assertEquals(ObdState.Lost, r.repo.state.value)
        assertEquals(1, r.created.size)
        advanceTimeBy(3_000) // 2 s de espera + nova sessão
        assertEquals(2, r.created.size)
        assertTrue(r.repo.state.value is ObdState.Connected)
    }

    @Test fun failing_to_open_keeps_retrying_with_growing_waits() = runTest {
        val r = Rig(this, ALL_NO_DATA + ("0100" to "UNABLE TO CONNECT"))
        advanceTimeBy(1_000)
        assertEquals(1, r.created.size)
        advanceTimeBy(2_100) // 2 s
        assertEquals(2, r.created.size)
        advanceTimeBy(4_100) // + 4 s
        assertEquals(3, r.created.size)
    }

    @Test fun backoff_grows_and_caps_at_thirty_seconds() {
        assertEquals(listOf(2_000L, 4_000L, 8_000L, 16_000L, 30_000L, 30_000L), (0..5).map { backoffMs(it) })
    }

    @Test fun pauses_while_the_app_is_not_visible_and_reconnects_after() = runTest {
        val r = Rig(this, CAR)
        advanceTimeBy(1_000)
        r.visible.value = false
        runCurrent() // a pausa é processada antes de medir
        val before = r.created.first().sent.size
        advanceTimeBy(10_000)
        assertEquals(before, r.created.first().sent.size)
        r.visible.value = true
        advanceTimeBy(3_000)
        assertEquals(2, r.created.size)
        assertTrue(r.repo.state.value is ObdState.Connected)
    }

    @Test fun selecting_an_adapter_starts_the_connection() = runTest {
        val r = Rig(this, CAR, address = null)
        runCurrent()
        r.repo.select(ADAPTER.address)
        advanceTimeBy(3_000)
        assertTrue(r.repo.state.value is ObdState.Connected)
    }

    @Test fun reads_faults_without_interleaving_with_polling() = runTest {
        val r = Rig(this, CAR)
        advanceTimeBy(1_000)
        assertNull(r.repo.faults.value)
        r.repo.readFaults()
        advanceTimeBy(500)
        assertEquals(listOf(Dtc("P0133")), r.repo.faults.value)
    }

    @Test fun lists_the_bonded_adapters() = runTest {
        assertEquals(listOf(ADAPTER), Rig(this, CAR).repo.bondedAdapters())
    }
}
