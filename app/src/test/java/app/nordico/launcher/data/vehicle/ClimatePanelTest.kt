package app.nordico.launcher.data.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ClimatePanelTest {
    @Test fun temp_steps_half_degree_within_18_32() {
        assertEquals(23.0, DEFAULT_CLIMATE.press(PanelButton.TempUp).setTemp, 0.0)
        assertEquals(22.0, DEFAULT_CLIMATE.press(PanelButton.TempDown).setTemp, 0.0)
        assertEquals(18.0, DEFAULT_CLIMATE.copy(setTemp = 18.0).press(PanelButton.TempDown).setTemp, 0.0)
        assertEquals(32.0, DEFAULT_CLIMATE.copy(setTemp = 32.0).press(PanelButton.TempUp).setTemp, 0.0)
    }

    @Test fun manual_fan_or_airflow_leaves_auto() {
        val f = DEFAULT_CLIMATE.press(PanelButton.FanUp)
        assertEquals(4, f.fanSpeed)
        assertFalse(f.auto)
        val a = DEFAULT_CLIMATE.press(PanelButton.Airflow)
        assertEquals(Airflow.Feet, a.airflow)
        assertFalse(a.auto)
        assertEquals(7, DEFAULT_CLIMATE.copy(fanSpeed = 7).press(PanelButton.FanUp).fanSpeed)
        assertEquals(1, DEFAULT_CLIMATE.copy(fanSpeed = 1).press(PanelButton.FanDown).fanSpeed)
    }

    @Test fun auto_restores_fan_airflow_and_ac() {
        val c = DEFAULT_CLIMATE.copy(auto = false, fanSpeed = 6, airflow = Airflow.Face, ac = false).press(PanelButton.Auto)
        assertEquals(DEFAULT_CLIMATE.copy(setTemp = c.setTemp), c)
    }

    @Test fun airflow_cycles() {
        assertEquals(Airflow.Face, DEFAULT_CLIMATE.copy(airflow = Airflow.FeetDefrost).press(PanelButton.Airflow).airflow)
    }

    @Test fun toggles() {
        assertFalse(DEFAULT_CLIMATE.press(PanelButton.Ac).ac)
        assertEquals(true, DEFAULT_CLIMATE.press(PanelButton.Recirc).recirc)
        assertEquals(true, DEFAULT_CLIMATE.press(PanelButton.DefrostFront).defrostFront)
        assertEquals(true, DEFAULT_CLIMATE.press(PanelButton.DefrostRear).defrostRear)
    }
}
