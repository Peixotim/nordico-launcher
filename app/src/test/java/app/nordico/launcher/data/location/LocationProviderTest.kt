package app.nordico.launcher.data.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationProviderTest {
    private val both = setOf("gps", "network")

    @Test fun requests_fine_and_coarse_together() {
        assertTrue(LOCATION_PERMISSIONS.contains("android.permission.ACCESS_FINE_LOCATION"))
        assertTrue(LOCATION_PERMISSIONS.contains("android.permission.ACCESS_COARSE_LOCATION"))
    }

    @Test fun precise_uses_gps() {
        assertEquals("gps", locationProvider(fine = true, coarse = true, available = both))
    }

    @Test fun approximate_only_uses_network() {
        assertEquals("network", locationProvider(fine = false, coarse = true, available = both))
    }

    @Test fun precise_without_gps_falls_back_to_network() {
        assertEquals("network", locationProvider(fine = true, coarse = true, available = setOf("network")))
    }

    @Test fun nothing_usable_returns_null() {
        assertNull(locationProvider(fine = false, coarse = false, available = both))
        assertNull(locationProvider(fine = false, coarse = true, available = setOf("gps")))
    }
}
