package app.nordico.launcher.feature.apps

import app.nordico.launcher.data.apps.AppEntry
import app.nordico.launcher.data.apps.AppSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppsLogicTest {
    private val radio = AppEntry("radio", "Rádio", AppSection.Car)
    private val spotify = AppEntry("spotify", "Spotify", AppSection.Mine)
    private val camera = AppEntry("camera", "Câmera 360", AppSection.Car)

    @Test fun splits_by_section_keeping_order() {
        val s = sectionApps(listOf(spotify, radio, camera), emptySet(), "")
        assertEquals(listOf(spotify), s.mine)
        assertEquals(listOf(radio, camera), s.car)
        assertTrue(s.hidden.isEmpty())
    }

    @Test fun hidden_moves_out_of_its_section() {
        val s = sectionApps(listOf(spotify, radio), setOf("radio"), "")
        assertEquals(listOf(radio), s.hidden)
        assertTrue(s.car.isEmpty())
    }

    @Test fun stale_hidden_ids_are_ignored() {
        val s = sectionApps(listOf(spotify), setOf("removido.pkg"), "")
        assertTrue(s.hidden.isEmpty())
        assertEquals(listOf(spotify), s.mine)
    }

    @Test fun search_ignores_accents_and_case() {
        assertEquals(listOf(radio), sectionApps(listOf(spotify, radio), emptySet(), "RADIO").car)
        assertEquals(listOf(camera), sectionApps(listOf(camera), emptySet(), "câm").car)
    }

    @Test fun search_applies_to_hidden_too() {
        assertTrue(sectionApps(listOf(radio), setOf("radio"), "spot").hidden.isEmpty())
    }

    @Test fun blank_query_matches_all() {
        assertEquals(listOf(spotify), sectionApps(listOf(spotify), emptySet(), "   ").mine)
    }
}
