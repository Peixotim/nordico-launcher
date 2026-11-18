package app.nordico.launcher.feature.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VehicleFormatTest {
    @Test fun coolant_tone_bands() {
        assertEquals(CoolantTone.Cool, coolantTone(69))
        assertEquals(CoolantTone.Normal, coolantTone(70))
        assertEquals(CoolantTone.Normal, coolantTone(100))
        assertEquals(CoolantTone.Warm, coolantTone(101))
        assertEquals(CoolantTone.Warm, coolantTone(104))
        assertEquals(CoolantTone.Hot, coolantTone(105))
        assertEquals(CoolantTone.Normal, coolantTone(null))
    }

    @Test fun missing_values_show_a_dash() {
        assertEquals("—", formatOrDash(null))
        assertEquals("83 °C", formatOrDash(83, " °C"))
        assertEquals("—", formatVolts(null))
        assertEquals("14,2", formatVolts(14.2))
        assertEquals("14,0", formatVolts(14.0))
    }

    @Test fun rpm_fraction_is_zero_to_eight_thousand() {
        assertEquals(0.5f, rpmFraction(4000), 0.0001f)
        assertEquals(1f, rpmFraction(9000), 0f)
        assertEquals(0f, rpmFraction(null), 0f)
        assertEquals(0f, rpmFraction(-50), 0f)
    }

    @Test fun rpm_uses_a_thousands_dot() {
        assertEquals("2.400", formatRpm(2400))
        assertEquals("900", formatRpm(900))
        assertEquals("—", formatRpm(null))
    }

    @Test fun coolant_segments_from_60_to_120_in_steps_of_3() {
        assertEquals(10, coolantSegments(90))
        assertEquals(11, coolantSegments(91))
        assertEquals(0, coolantSegments(59))
        assertEquals(0, coolantSegments(null))
        assertEquals(20, coolantSegments(120))
        assertEquals(20, coolantSegments(200))
    }

    @Test fun fault_badge_hides_zero_and_caps_at_nine() {
        assertNull(faultBadge(null))
        assertNull(faultBadge(0))
        assertEquals("2", faultBadge(2))
        assertEquals("9+", faultBadge(12))
    }

    @Test fun battery_hint_only_when_charging() {
        assertEquals("Alternador carregando", batteryHint(14.2, 2400))
        assertNull(batteryHint(12.4, 0))
        assertNull(batteryHint(null, 800))
        assertNull(batteryHint(14.2, null))
    }
}
