package app.nordico.launcher.feature.radio

import app.nordico.launcher.data.radio.Band
import org.junit.Assert.assertEquals
import org.junit.Test

class RadioFormatTest {
    @Test fun formats_frequencies() {
        assertEquals("98,5", formatFrequency(Band.Fm, 98_500))
        assertEquals("108,0", formatFrequency(Band.Fm, 108_000))
        assertEquals("76,0", formatFrequency(Band.Fm, 76_000))
        assertEquals("1040", formatFrequency(Band.Am, 1040))
    }

    @Test fun units() {
        assertEquals("MHz", frequencyUnit(Band.Fm))
        assertEquals("kHz", frequencyUnit(Band.Am))
    }

    @Test fun dial_fraction_spans_the_band() {
        assertEquals(0f, dialFraction(Band.Fm, 76_000), 0f)
        assertEquals(1f, dialFraction(Band.Fm, 108_000), 0f)
        assertEquals(0.703125f, dialFraction(Band.Fm, 98_500), 0.0001f)
        assertEquals(0f, dialFraction(Band.Am, 520), 0f)
        assertEquals(1f, dialFraction(Band.Am, 1710), 0f)
    }

    @Test fun fraction_to_frequency_snaps_and_clamps() {
        assertEquals(92_000, khzAtFraction(Band.Fm, 0.5f))
        assertEquals(108_000, khzAtFraction(Band.Fm, 2f))
        assertEquals(76_000, khzAtFraction(Band.Fm, -1f))
        assertEquals(520, khzAtFraction(Band.Am, 0f))
        assertEquals(1710, khzAtFraction(Band.Am, 1f))
    }

    @Test fun preset_pages_are_six_wide() {
        assertEquals(3, presetPages(Band.Fm))
        assertEquals(2, presetPages(Band.Am))
    }
}
