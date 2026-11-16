package app.nordico.launcher.designsystem

import app.nordico.launcher.data.prefs.ScreenMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
    @Test fun day_and_night_are_fixed() {
        assertFalse(resolveDark(ScreenMode.Day, true, 23))
        assertTrue(resolveDark(ScreenMode.Night, false, 12))
    }

    @Test fun auto_follows_headlights_when_known() {
        assertTrue(resolveDark(ScreenMode.Auto, true, 12))
        assertFalse(resolveDark(ScreenMode.Auto, false, 23))
    }

    @Test fun auto_without_headlights_uses_18_to_6() {
        assertFalse(resolveDark(ScreenMode.Auto, null, 17))
        assertTrue(resolveDark(ScreenMode.Auto, null, 18))
        assertTrue(resolveDark(ScreenMode.Auto, null, 5))
        assertFalse(resolveDark(ScreenMode.Auto, null, 6))
    }
}
