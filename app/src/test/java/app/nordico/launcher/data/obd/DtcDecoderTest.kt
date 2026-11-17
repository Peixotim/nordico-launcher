package app.nordico.launcher.data.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DtcDecoderTest {
    @Test fun decodes_powertrain_code_on_can() {
        assertEquals(listOf(Dtc("P0133")), decodeDtcs("43010133000000", canCount = true))
    }

    @Test fun first_letter_comes_from_the_two_top_bits() {
        assertEquals(listOf(Dtc("C0123")), decodeDtcs("4301412300", canCount = true))
        assertEquals(listOf(Dtc("B0123")), decodeDtcs("4301812300", canCount = true))
        assertEquals(listOf(Dtc("U0100")), decodeDtcs("4301C100", canCount = true))
    }

    @Test fun decodes_several_codes_and_ignores_empty_pairs() {
        assertEquals(listOf(Dtc("P0133"), Dtc("P0420")), decodeDtcs("430201330420", canCount = true))
    }

    @Test fun works_without_the_count_byte() {
        assertEquals(listOf(Dtc("P0133")), decodeDtcs("430133", canCount = false))
    }

    @Test fun no_codes_gives_empty_list() {
        assertTrue(decodeDtcs("430000000000", canCount = true).isEmpty())
        assertTrue(decodeDtcs("NO DATA", canCount = true).isEmpty())
        assertTrue(decodeDtcs("", canCount = true).isEmpty())
    }
}
