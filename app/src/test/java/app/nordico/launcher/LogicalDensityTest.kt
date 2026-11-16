package app.nordico.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class LogicalDensityTest {
    @Test fun `1280 px wide gives density 1`() = assertEquals(1f, logicalDensity(1280), 0.0001f)

    @Test fun `1920 px wide gives density 1_5`() = assertEquals(1.5f, logicalDensity(1920), 0.0001f)
}
