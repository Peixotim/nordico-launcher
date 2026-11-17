package app.nordico.launcher.sim

import app.nordico.launcher.data.radio.Band
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimRadioRepositoryTest {
    @Test fun starts_available_on_a_known_fm_station() {
        val r = SimRadioRepository()
        assertTrue(r.available.value)
        assertEquals(Band.Fm, r.state.value.band)
        assertEquals(98500, r.state.value.khz)
        assertEquals("Rádio Nórdico", r.state.value.station)
    }

    @Test fun seek_forward_goes_to_the_next_station() {
        val r = SimRadioRepository()
        r.seek(+1)
        assertEquals(102100, r.state.value.khz)
    }

    @Test fun seek_backward_goes_to_the_previous_station() {
        val r = SimRadioRepository()
        r.seek(-1)
        assertEquals(94700, r.state.value.khz)
    }

    @Test fun switching_to_am_uses_an_am_frequency_and_remembers_fm() {
        val r = SimRadioRepository()
        r.tune(88100)
        r.setBand(Band.Am)
        assertEquals(Band.Am, r.state.value.band)
        assertTrue(r.state.value.khz in 520..1710)
        r.setBand(Band.Fm)
        assertEquals(88100, r.state.value.khz)
    }

    @Test fun tuning_snaps_and_clears_the_station_name_off_station() {
        val r = SimRadioRepository()
        r.tune(99040)
        assertEquals(99000, r.state.value.khz)
        assertNull(r.state.value.station)
    }
}
