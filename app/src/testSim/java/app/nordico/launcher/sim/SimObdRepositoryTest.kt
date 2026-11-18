package app.nordico.launcher.sim

import app.nordico.launcher.data.obd.ClearBlock
import app.nordico.launcher.data.obd.ClearOutcome
import app.nordico.launcher.data.obd.ClearState
import app.nordico.launcher.data.obd.Dtc
import app.nordico.launcher.data.obd.ObdState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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

    @Test fun faults_scenario_clears_for_real() = runTest {
        val r = SimObdRepository(backgroundScope, settleMs = 1_000).apply { scenario(ObdScenario.Faults) }
        r.readFaults()
        r.clearFaults()
        runCurrent()
        assertEquals(ClearState.Clearing, r.clearState.value)
        advanceTimeBy(1_100)
        assertEquals(ClearState.Done(ClearOutcome.Cleared), r.clearState.value)
        assertEquals(emptyList<Dtc>(), r.faults.value)
        assertEquals(false, r.telemetry.value.milOn)
        assertEquals(0, r.telemetry.value.faultCount)
    }

    @Test fun persistent_scenario_brings_the_codes_back() = runTest {
        val r = SimObdRepository(backgroundScope, settleMs = 1_000).apply { scenario(ObdScenario.FaultsPersistent) }
        r.clearFaults()
        advanceTimeBy(1_100)
        assertEquals(ClearState.Done(ClearOutcome.Returned(listOf(Dtc("P0133"), Dtc("P0420")))), r.clearState.value)
        assertEquals(true, r.telemetry.value.milOn)
    }

    @Test fun refused_scenario_says_conditions_not_correct() = runTest {
        val r = SimObdRepository(backgroundScope, settleMs = 1_000).apply { scenario(ObdScenario.ClearRefused) }
        r.clearFaults()
        advanceTimeBy(1_100)
        assertEquals(ClearState.Done(ClearOutcome.Refused(0x22)), r.clearState.value)
    }

    @Test fun a_moving_car_cannot_clear() = runTest {
        val r = SimObdRepository(backgroundScope).apply { scenario(ObdScenario.Highway) }
        r.clearFaults()
        assertEquals(ClearState.Done(ClearOutcome.Blocked(ClearBlock.Moving)), r.clearState.value)
    }

    @Test fun without_an_adapter_clearing_is_blocked_and_can_be_dismissed() = runTest {
        val r = SimObdRepository(backgroundScope).apply { scenario(ObdScenario.NoAdapter) }
        r.clearFaults()
        assertEquals(ClearState.Done(ClearOutcome.Blocked(ClearBlock.NotConnected)), r.clearState.value)
        r.dismissClearResult()
        assertEquals(ClearState.Idle, r.clearState.value)
    }

    @Test fun a_healthy_car_has_nothing_to_clear() = runTest {
        val r = SimObdRepository(backgroundScope)
        r.clearFaults()
        assertEquals(ClearState.Done(ClearOutcome.Blocked(ClearBlock.NothingToClear)), r.clearState.value)
    }
}
