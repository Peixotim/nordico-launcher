package app.nordico.launcher.feature.climate

import app.nordico.launcher.data.vehicle.DEFAULT_CLIMATE
import org.junit.Assert.assertEquals
import org.junit.Test

class ClimateFormatTest {
    @Test fun temp_uses_comma() {
        assertEquals("22,5", formatTemp(22.5))
        assertEquals("18,0", formatTemp(18.0))
    }

    @Test fun tones() {
        assertEquals(TempTone.Cool, toneFor(20.5))
        assertEquals(TempTone.Mid, toneFor(21.0))
        assertEquals(TempTone.Mid, toneFor(25.0))
        assertEquals(TempTone.Warm, toneFor(25.5))
    }

    @Test fun labels() {
        assertEquals("Automática · 3 de 7", fanLabel(DEFAULT_CLIMATE))
        assertEquals("Manual · 5 de 7", fanLabel(DEFAULT_CLIMATE.copy(auto = false, fanSpeed = 5)))
        assertEquals("22,5° · Auto", chipLabel(DEFAULT_CLIMATE))
        assertEquals("22,5° · Manual", chipLabel(DEFAULT_CLIMATE.copy(auto = false)))
    }

    @Test fun spin() {
        assertEquals(1330, fanSpinMillis(1))
        assertEquals(310, fanSpinMillis(7))
    }
}
