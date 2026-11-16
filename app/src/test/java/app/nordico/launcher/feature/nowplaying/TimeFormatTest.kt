package app.nordico.launcher.feature.nowplaying

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {
    @Test fun elapsed() {
        assertEquals("1:42", formatElapsed(102_000))
        assertEquals("0:00", formatElapsed(0))
        assertEquals("10:05", formatElapsed(605_999))
        assertEquals("0:00", formatElapsed(-5))
    }
}
