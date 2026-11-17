package app.nordico.launcher.data.radio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioMathTest {
    @Test fun fm_steps_by_one_tenth_with_wrap_around() {
        assertEquals(108000, stepKhz(Band.Fm, 107900, +1))
        assertEquals(76000, stepKhz(Band.Fm, 108000, +1))
        assertEquals(108000, stepKhz(Band.Fm, 76000, -1))
        assertEquals(98100, stepKhz(Band.Fm, 98000, +1))
    }

    @Test fun am_steps_by_ten_with_wrap_around() {
        assertEquals(1710, stepKhz(Band.Am, 520, -1))
        assertEquals(520, stepKhz(Band.Am, 1710, +1))
        assertEquals(1010, stepKhz(Band.Am, 1000, +1))
    }

    @Test fun stepping_from_an_off_grid_frequency_snaps_first() {
        assertEquals(98100, stepKhz(Band.Fm, 98040, +1))
    }

    @Test fun snap_rounds_to_the_grid_and_clamps_to_the_band() {
        assertEquals(98000, snapKhz(Band.Fm, 98040))
        assertEquals(98100, snapKhz(Band.Fm, 98060))
        assertEquals(108000, snapKhz(Band.Fm, 200000))
        assertEquals(76000, snapKhz(Band.Fm, 0))
        assertEquals(1000, snapKhz(Band.Am, 1000))
        assertEquals(1000, snapKhz(Band.Am, 1004))
        assertEquals(520, snapKhz(Band.Am, -5))
        assertEquals(1710, snapKhz(Band.Am, 99999))
    }

    @Test fun presets_save_and_recall_per_band() {
        val p = Presets().save(Band.Fm, 0, 98500).save(Band.Am, 11, 1040)
        assertEquals(98500, p.recall(Band.Fm, 0))
        assertEquals(1040, p.recall(Band.Am, 11))
        assertNull(p.recall(Band.Fm, 1))
    }

    @Test fun presets_ignore_out_of_range_indexes() {
        val p = Presets()
        assertEquals(p, p.save(Band.Fm, 18, 98500))
        assertEquals(p, p.save(Band.Am, 12, 1040))
        assertEquals(p, p.save(Band.Fm, -1, 98500))
        assertNull(p.recall(Band.Fm, 18))
        assertNull(p.recall(Band.Am, -1))
    }

    @Test fun presets_have_18_fm_and_12_am_slots() {
        assertEquals(18, Presets().fm.size)
        assertEquals(12, Presets().am.size)
    }

    @Test fun saving_snaps_the_frequency() {
        assertEquals(98000, Presets().save(Band.Fm, 3, 98040).recall(Band.Fm, 3))
    }

    @Test fun presets_survive_encode_and_decode() {
        val p = Presets().save(Band.Fm, 0, 98500).save(Band.Fm, 17, 88100).save(Band.Am, 2, 790)
        assertEquals(p, Presets.decode(p.encode()))
    }

    @Test fun decoding_garbage_gives_empty_slots_not_a_crash() {
        val p = Presets.decode("abc,98500,,x", "")
        assertNull(p.recall(Band.Fm, 0))
        assertEquals(98500, p.recall(Band.Fm, 1))
        assertEquals(18, p.fm.size)
        assertEquals(12, p.am.size)
        assertTrue(Presets.decode(null, null).fm.all { it == null })
    }

    @Test fun next_station_goes_forward_and_wraps() {
        val s = listOf(88100, 94700, 98500)
        assertEquals(94700, nextStation(Band.Fm, 88100, +1, s))
        assertEquals(88100, nextStation(Band.Fm, 98500, +1, s))
        assertEquals(98500, nextStation(Band.Fm, 96000, +1, s))
    }

    @Test fun next_station_goes_backward_and_wraps() {
        val s = listOf(88100, 94700, 98500)
        assertEquals(88100, nextStation(Band.Fm, 94700, -1, s))
        assertEquals(98500, nextStation(Band.Fm, 88100, -1, s))
    }

    @Test fun seek_without_stations_returns_null() {
        assertNull(nextStation(Band.Fm, 98500, +1, emptyList()))
    }

    @Test fun seek_with_a_single_station_returns_that_station() {
        assertEquals(98500, nextStation(Band.Fm, 98500, +1, listOf(98500)))
    }
}
