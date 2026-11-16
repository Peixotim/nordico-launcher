package app.nordico.launcher.feature.nowplaying

import app.nordico.launcher.data.media.NowPlaying
import org.junit.Assert.assertEquals
import org.junit.Test

class MiniCardStateTest {
    private val np = NowPlaying("Evidências", "Chitãozinho & Xororó", null, 102_000, 278_000, true, "Spotify")

    @Test fun access_first() {
        assertEquals(MiniCardState.NeedsAccess, miniCardState(false, np))
    }

    @Test fun empty_when_nothing() {
        assertEquals(MiniCardState.Empty, miniCardState(true, null))
    }

    @Test fun idle_session_without_metadata_is_empty() {
        assertEquals(MiniCardState.Empty, miniCardState(true, np.copy(title = "", artist = "", playing = false)))
    }

    @Test fun blank_title_falls_back_to_source() {
        assertEquals("Spotify", (miniCardState(true, np.copy(title = " ")) as MiniCardState.Playing).title)
    }

    @Test fun progress_is_clamped_and_safe() {
        assertEquals(0f, (miniCardState(true, np.copy(durationMs = 0)) as MiniCardState.Playing).progress, 0f)
        assertEquals(1f, (miniCardState(true, np.copy(positionMs = 400_000)) as MiniCardState.Playing).progress, 0f)
        assertEquals(102f / 278f, (miniCardState(true, np) as MiniCardState.Playing).progress, 0.0001f)
    }
}
