package app.nordico.launcher.feature.hud

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HudControllerTest {
    @Test fun volume_hides_after_1800() = runTest {
        val h = HudController(backgroundScope)
        h.show(HudKind.Volume)
        runCurrent()
        advanceTimeBy(1799)
        assertEquals(HudKind.Volume, h.visible.value)
        advanceTimeBy(2)
        assertNull(h.visible.value)
    }

    @Test fun repeated_events_restart_timer() = runTest {
        val h = HudController(backgroundScope)
        repeat(3) {
            h.show(HudKind.Volume)
            advanceTimeBy(1000)
        } // t = 3000, último evento em 2000
        assertEquals(HudKind.Volume, h.visible.value)
        advanceTimeBy(801) // 2000 + 1800 = 3800
        assertNull(h.visible.value)
    }

    @Test fun alert_lasts_6000() = runTest {
        val h = HudController(backgroundScope)
        h.show(HudKind.Alert)
        runCurrent()
        advanceTimeBy(5999)
        assertEquals(HudKind.Alert, h.visible.value)
        advanceTimeBy(2)
        assertNull(h.visible.value)
    }

    @Test fun climate_lasts_2600_and_replaces_volume() = runTest {
        val h = HudController(backgroundScope)
        h.show(HudKind.Volume)
        h.show(HudKind.Climate)
        runCurrent()
        assertEquals(HudKind.Climate, h.visible.value)
        advanceTimeBy(2599)
        assertEquals(HudKind.Climate, h.visible.value)
        advanceTimeBy(2)
        assertNull(h.visible.value)
    }
}
