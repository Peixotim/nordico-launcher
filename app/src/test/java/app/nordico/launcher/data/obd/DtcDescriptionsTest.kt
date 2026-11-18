package app.nordico.launcher.data.obd

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DtcDescriptionsTest {
    @Test fun knows_common_codes_in_portuguese() {
        assertTrue(dtcDescription("P0420")!!.contains("catalisador", ignoreCase = true))
        assertNotNull(dtcDescription("P0133"))
        assertNotNull(dtcDescription("P0171"))
    }

    @Test fun lookup_ignores_case() {
        assertNotNull(dtcDescription("p0420"))
    }

    @Test fun unknown_code_has_no_description() {
        assertNull(dtcDescription("P9999"))
        assertNull(dtcDescription(""))
    }
}
