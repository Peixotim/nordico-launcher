package app.nordico.launcher.designsystem

import androidx.compose.ui.graphics.Color
import app.nordico.launcher.data.prefs.Accent
import org.junit.Assert.assertEquals
import org.junit.Test

class TokensTest {
    @Test fun orange_accent_per_mode() {
        assertEquals(Color(0xFFF2620F), nordicColors(false, Accent.Orange).accent)
        assertEquals(Color(0xFFFF7A2E), nordicColors(true, Accent.Orange).accent)
    }

    @Test fun backgrounds() {
        assertEquals(Color(0xFFF2F2F7), nordicColors(false, Accent.Blue).bg)
        assertEquals(Color.Black, nordicColors(true, Accent.Blue).bg)
    }
}
