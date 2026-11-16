package app.nordico.launcher.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaPositionTest {
    @Test fun paused_stays_at_base() {
        assertEquals(10_000L, livePosition(10_000, baseTimeMs = 1_000, speed = 1f, nowMs = 9_000, playing = false, durationMs = 278_000))
    }

    @Test fun playing_advances_with_elapsed_time_and_speed() {
        assertEquals(15_000L, livePosition(10_000, baseTimeMs = 1_000, speed = 1f, nowMs = 6_000, playing = true, durationMs = 278_000))
        assertEquals(20_000L, livePosition(10_000, baseTimeMs = 1_000, speed = 2f, nowMs = 6_000, playing = true, durationMs = 278_000))
    }

    @Test fun clamps_to_duration_and_zero() {
        assertEquals(278_000L, livePosition(277_000, baseTimeMs = 0, speed = 1f, nowMs = 60_000, playing = true, durationMs = 278_000))
        assertEquals(0L, livePosition(-500, baseTimeMs = 0, speed = 1f, nowMs = 0, playing = false, durationMs = 278_000))
    }

    @Test fun unknown_duration_is_not_clamped_above() {
        assertEquals(70_000L, livePosition(10_000, baseTimeMs = 0, speed = 1f, nowMs = 60_000, playing = true, durationMs = 0))
    }
}
