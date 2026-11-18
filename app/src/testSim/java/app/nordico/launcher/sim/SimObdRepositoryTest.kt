package app.nordico.launcher.sim

import app.nordico.launcher.data.obd.ObdState
import app.nordico.launcher.data.obd.Dtc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimObdRepositoryTest {
    @Test fun highway_is_fast_with_the_engine_running() {
        val r = SimObdRepository().apply { scenario(ObdScenario.Highway) }
        assertTrue(r.telemetry.value.speedKmh!! >= 80)
        assertTrue(r.telemetry.value.rpm!! > 0)
    }

    @Test fun overheating_gives_a_hot_coolant() {
        val r = SimObdRepository().apply { scenario(ObdScenario.Overheating) }
        assertTrue(r.telemetry.value.coolantC!! >= 105)
    }

    @Test fun weak_battery_is_below_the_alert_threshold_with_engine_on() {
        val r = SimObdRepository().apply { scenario(ObdScenario.WeakBattery) }
        assertTrue(r.telemetry.value.batteryV!! < 11.8)
        assertTrue(r.telemetry.value.rpm!! > 0)
    }

    @Test fun no_adapter_clears_everything() {
        val r = SimObdRepository().apply { scenario(ObdScenario.NoAdapter) }
        assertEquals(ObdState.NoAdapter, r.state.value)
        assertNull(r.telemetry.value.speedKmh)
        assertNull(r.telemetry.value.coolantC)
    }

    @Test fun faults_scenario_turns_the_engine_light_on() {
        val r = SimObdRepository().apply { scenario(ObdScenario.Faults) }
        assertEquals(true, r.telemetry.value.milOn)
        assertEquals(2, r.telemetry.value.faultCount)
    }

    @Test fun healthy_scenarios_have_no_faults() {
        val r = SimObdRepository()
        assertEquals(false, r.telemetry.value.milOn)
        assertEquals(0, r.telemetry.value.faultCount)
    }

    @Test fun lists_two_fake_adapters() {
        assertEquals(2, SimObdRepository().bondedAdapters().size)
    }

    @Test fun selecting_an_adapter_connects_when_there_was_none() {
        val r = SimObdRepository().apply { scenario(ObdScenario.NoAdapter) }
        r.select(r.bondedAdapters().first().address)
        assertTrue(r.state.value is ObdState.Connected)
    }

    @Test fun faults_start_unread_and_read_faults_fills_them() {
        val r = SimObdRepository()
        assertNull(r.faults.value)
        r.readFaults()
        assertEquals(listOf(Dtc("P0133"), Dtc("P0420")), r.faults.value)
    }
}
