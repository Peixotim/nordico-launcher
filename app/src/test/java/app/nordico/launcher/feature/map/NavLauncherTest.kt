package app.nordico.launcher.feature.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavLauncherTest {
    @Test fun prefers_saved_then_waze_then_maps() {
        assertEquals(NavApp.Maps, chooseNavApp("com.google.android.apps.maps", setOf("com.waze", "com.google.android.apps.maps")))
        assertEquals(NavApp.Waze, chooseNavApp("com.ausente", setOf("com.waze", "com.google.android.apps.maps")))
        assertEquals(NavApp.Maps, chooseNavApp(null, setOf("com.google.android.apps.maps")))
        assertNull(chooseNavApp(null, emptySet()))
    }

    @Test fun favorites_only_for_waze() {
        assertEquals("waze://?favorite=home", favoriteUri(NavApp.Waze, Favorite.Home))
        assertEquals("waze://?favorite=work", favoriteUri(NavApp.Waze, Favorite.Work))
        assertNull(favoriteUri(NavApp.Maps, Favorite.Home))
        assertTrue(showSuggestions(NavApp.Waze))
        assertFalse(showSuggestions(NavApp.Maps))
        assertFalse(showSuggestions(null))
    }
}
