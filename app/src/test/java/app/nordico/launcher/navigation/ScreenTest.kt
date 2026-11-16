package app.nordico.launcher.navigation

import app.nordico.launcher.data.vehicle.DEFAULT_CLIMATE
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenTest {
    @Test fun climate_null_on_climate_goes_home() {
        assertEquals(Screen.Home, nextScreenOnClimate(Screen.Climate, null))
    }

    @Test fun climate_null_elsewhere_stays() {
        assertEquals(Screen.Apps, nextScreenOnClimate(Screen.Apps, null))
    }

    @Test fun climate_present_stays() {
        assertEquals(Screen.Climate, nextScreenOnClimate(Screen.Climate, DEFAULT_CLIMATE))
    }
}
