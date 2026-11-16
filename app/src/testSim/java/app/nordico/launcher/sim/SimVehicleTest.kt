package app.nordico.launcher.sim

import app.nordico.launcher.data.vehicle.PanelButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SimVehicleTest {
    @Test fun panel_updates_climate() {
        val v = SimVehicleRepository()
        v.press(PanelButton.TempUp)
        assertEquals(23.0, v.climate.value!!.setTemp, 0.0)
    }

    @Test fun climate_can_disappear_and_come_back() {
        val v = SimVehicleRepository()
        v.press(PanelButton.TempUp)
        v.setClimateAvailable(false)
        assertNull(v.climate.value)
        v.setClimateAvailable(true)
        assertEquals(23.0, v.climate.value!!.setTemp, 0.0)
    }

    @Test fun headlights() {
        val v = SimVehicleRepository()
        assertNull(v.headlightsOn.value)
        v.setHeadlights(true)
        assertEquals(true, v.headlightsOn.value)
    }
}
