package app.nordico.launcher.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DateFormatTest {
    @Test fun ptBr() {
        assertEquals("domingo, 15 de novembro", formatDate(LocalDate.of(2026, 11, 15)))
        assertEquals("09:05", formatClock(LocalTime.of(9, 5)))
        assertEquals("00:00", formatClock(LocalTime.MIDNIGHT))
    }
}
