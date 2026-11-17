package app.nordico.launcher.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeekMathTest {
    @Test fun maps_a_fraction_to_a_position() {
        assertEquals(100_000L, seekTarget(0.5f, 200_000))
    }

    @Test fun clamps_to_the_track() {
        assertEquals(200_000L, seekTarget(1.5f, 200_000))
        assertEquals(0L, seekTarget(-0.2f, 200_000))
    }

    @Test fun unknown_duration_gives_zero() {
        assertEquals(0L, seekTarget(0.5f, 0))
        assertEquals(0L, seekTarget(0.5f, -10))
    }

    @Test fun classifies_known_actions_from_their_ids() {
        assertEquals("Aleatório", classifyAction("TURN_SHUFFLE_ON", null)?.label)
        assertEquals("shuffle", classifyAction("com.spotify.shuffle", null)?.glyph)
        assertEquals("Repetir", classifyAction("TURN_REPEAT_ALL", null)?.label)
        assertEquals("Curtir", classifyAction("LIKE", null)?.label)
        assertEquals("favorite", classifyAction("HEART", null)?.glyph)
    }

    @Test fun falls_back_to_the_display_name_when_the_id_is_opaque() {
        assertEquals("Aleatório", classifyAction("action_7", "Shuffle")?.label)
    }

    @Test fun ignores_actions_it_does_not_know() {
        assertNull(classifyAction("ACTION_DO_SOMETHING", "Weird"))
        assertNull(classifyAction("", null))
    }

    @Test fun off_actions_mean_the_feature_is_currently_on() {
        assertTrue(classifyAction("TURN_SHUFFLE_OFF", null)!!.active)
        assertFalse(classifyAction("TURN_SHUFFLE_ON", null)!!.active)
    }

    @Test fun keeps_the_original_id_so_perform_can_send_it_back() {
        assertEquals("TURN_SHUFFLE_ON", classifyAction("TURN_SHUFFLE_ON", null)!!.id)
    }
}
