package app.nordico.launcher.data.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeMathTest {
    @Test fun step_clamps_and_unmutes() {
        assertEquals(40, VolumeState(39, false).stepped(5).level)
        assertEquals(0, VolumeState(1, false).stepped(-3).level)
        assertFalse(VolumeState(10, true).stepped(1).muted)
        assertTrue(VolumeState(10, true).stepped(-1).muted)
    }

    @Test fun step_is_internal() {
        assertEquals(VolumeOrigin.Internal, VolumeState(10, false, VolumeOrigin.External).stepped(1).origin)
    }

    @Test fun icons() {
        assertEquals("volume_off", VolumeState(0, false).icon())
        assertEquals("volume_down", VolumeState(13, false).icon())
        assertEquals("volume_up", VolumeState(14, false).icon())
        assertEquals("volume_off", VolumeState(30, true).icon())
    }

    @Test fun ends_map_exactly_for_any_system_max() {
        for (max in listOf(15, 30, 40)) {
            assertEquals(0, toUiLevel(0, max))
            assertEquals(40, toUiLevel(max, max))
            assertEquals(0, toSysLevel(0, max))
            assertEquals(max, toSysLevel(40, max))
        }
    }

    // I-2: com máximo do sistema < 40, cada toque precisa mudar o volume real.
    @Test fun every_press_changes_system_volume_when_max_below_40() {
        for (max in listOf(15, 25, 30)) {
            for (s in 0 until max) {
                val state = VolumeState(toUiLevel(s, max), false)
                val (up, sysUp) = stepWithSystem(state, 1, max)
                assertEquals(s + 1, sysUp)
                assertEquals(toUiLevel(s + 1, max), up.level)
                val (_, sysDown) = stepWithSystem(VolumeState(toUiLevel(s + 1, max), false), -1, max)
                assertEquals(s, sysDown)
            }
        }
    }

    @Test fun step_with_system_at_40_is_plain_step_and_unmute_rules_hold() {
        val (st, sys) = stepWithSystem(VolumeState(18, false), 1, 40)
        assertEquals(VolumeState(19, false), st)
        assertEquals(19, sys)
        assertFalse(stepWithSystem(VolumeState(10, true), 1, 15).first.muted)
        assertTrue(stepWithSystem(VolumeState(10, true), -1, 15).first.muted)
        assertEquals(0, stepWithSystem(VolumeState(0, false), -1, 15).second)
    }

    // I-1: o Android informa volume 0 enquanto está mudo; o nível não pode zerar nem virar aviso externo.
    @Test fun own_mute_echo_is_ignored() {
        assertNull(onSystemVolume(VolumeState(18, true), sys = 0, muted = true, sysMax = 25, lastWritten = null))
    }

    @Test fun external_mute_keeps_level() {
        assertEquals(
            VolumeState(18, true, VolumeOrigin.External),
            onSystemVolume(VolumeState(18, false), sys = 0, muted = true, sysMax = 25, lastWritten = null),
        )
    }

    @Test fun own_unmute_echo_is_ignored() {
        assertNull(onSystemVolume(VolumeState(18, false), sys = toSysLevel(18, 25), muted = false, sysMax = 25, lastWritten = null))
    }

    @Test fun echo_of_written_value_is_ignored() {
        assertNull(onSystemVolume(VolumeState(19, false), sys = 12, muted = false, sysMax = 25, lastWritten = 12))
    }

    @Test fun external_change_and_external_unmute_are_reported() {
        assertEquals(
            VolumeState(toUiLevel(14, 25), false, VolumeOrigin.External),
            onSystemVolume(VolumeState(18, false), sys = 14, muted = false, sysMax = 25, lastWritten = null),
        )
        assertEquals(
            VolumeState(18, false, VolumeOrigin.External),
            onSystemVolume(VolumeState(18, true), sys = toSysLevel(18, 25), muted = false, sysMax = 25, lastWritten = null),
        )
    }

    @Test fun every_ui_step_up_never_lowers_system_and_round_trips() {
        for (max in listOf(15, 30)) {
            for (ui in 0 until 40) {
                assertTrue(toSysLevel(ui + 1, max) >= toSysLevel(ui, max))
                val s = toSysLevel(ui, max)
                assertEquals(s, toSysLevel(toUiLevel(s, max), max))
            }
        }
    }
}
