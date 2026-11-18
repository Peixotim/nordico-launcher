package app.nordico.launcher.data.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PollSchedulerTest {
    private val all = ObdPid.entries.toSet()

    @Test fun never_read_pids_come_in_priority_order() {
        assertEquals(ObdPid.Speed, nextPid(0, emptyMap(), all))
        assertEquals(ObdPid.Rpm, nextPid(0, mapOf(ObdPid.Speed to 0L), all))
    }

    @Test fun picks_the_most_overdue() {
        val last = ObdPid.entries.associateWith { 1_000L }.toMutableMap()
        last[ObdPid.Speed] = 900L   // 100 ms atrás, intervalo 250: ainda não venceu
        last[ObdPid.Rpm] = 700L     // 300 ms atrás, intervalo 250: venceu
        assertEquals(ObdPid.Rpm, nextPid(1_000, last, all))
    }

    @Test fun nothing_due_returns_null() {
        val last = ObdPid.entries.associateWith { 1_000L }
        assertNull(nextPid(1_001, last, all))
    }

    @Test fun inactive_pids_never_come_out() {
        assertEquals(ObdPid.Fuel, nextPid(0, emptyMap(), setOf(ObdPid.Fuel)))
        assertNull(nextPid(0, emptyMap(), emptySet()))
    }

    @Test fun intervals_follow_the_spec() {
        assertEquals(250L, POLL_INTERVALS.getValue(ObdPid.Speed))
        assertEquals(250L, POLL_INTERVALS.getValue(ObdPid.Rpm))
        assertEquals(1000L, POLL_INTERVALS.getValue(ObdPid.Load))
        assertEquals(1000L, POLL_INTERVALS.getValue(ObdPid.Throttle))
        assertEquals(2000L, POLL_INTERVALS.getValue(ObdPid.Coolant))
        assertEquals(5000L, POLL_INTERVALS.getValue(ObdPid.Status))
    }

    @Test fun strike_counter_disables_after_three_no_data_in_a_row() {
        val s = StrikeCounter()
        assertFalse(s.noData(ObdPid.Fuel))
        assertFalse(s.noData(ObdPid.Fuel))
        assertTrue(s.noData(ObdPid.Fuel))
    }

    @Test fun ok_resets_the_strikes() {
        val s = StrikeCounter()
        s.noData(ObdPid.Fuel)
        s.noData(ObdPid.Fuel)
        s.ok(ObdPid.Fuel)
        assertFalse(s.noData(ObdPid.Fuel))
        assertFalse(s.noData(ObdPid.Fuel))
        assertTrue(s.noData(ObdPid.Fuel))
    }

    @Test fun strikes_are_per_pid() {
        val s = StrikeCounter()
        s.noData(ObdPid.Fuel)
        s.noData(ObdPid.Fuel)
        assertFalse(s.noData(ObdPid.Intake))
    }
}
