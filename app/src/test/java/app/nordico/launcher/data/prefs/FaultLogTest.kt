package app.nordico.launcher.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FaultLogTest {
    @Test fun marking_is_idempotent_and_unmarking_removes() {
        val log = FaultLog().mark(listOf("P0420", "P0133"), 100).mark(listOf("P0420"), 200)
        assertEquals(setOf("P0420", "P0133"), log.marks.keys)
        assertEquals(100L, log.marks.getValue("P0420")) // a primeira marca é mantida
        assertEquals(setOf("P0133"), log.unmark(listOf("P0420", "P9999")).marks.keys)
    }

    @Test fun codes_are_normalised() {
        assertEquals(setOf("P0420"), FaultLog().mark(listOf(" p0420 "), 1).marks.keys)
    }

    @Test fun prune_drops_marks_of_codes_that_are_gone() {
        val log = FaultLog().mark(listOf("P0420", "P0133"), 1).prune(listOf("P0420"))
        assertEquals(setOf("P0420"), log.marks.keys)
        assertTrue(log.prune(emptyList()).marks.isEmpty())
    }

    @Test fun begin_records_an_attempt_before_sending() {
        val log = FaultLog().begin(listOf("P0420", "P0133"), 1_000)
        assertEquals(listOf(ClearRecord(1_000, listOf("P0420", "P0133"), ClearResult.Attempt)), log.history)
    }

    @Test fun finish_completes_the_most_recent_record() {
        val log = FaultLog().begin(listOf("P0420"), 1).finish(ClearResult.Cleared).begin(listOf("P0133"), 2).finish(ClearResult.Returned)
        assertEquals(ClearResult.Cleared, log.history[0].result)
        assertEquals(ClearResult.Returned, log.history[1].result)
    }

    @Test fun finish_without_an_attempt_does_nothing() {
        assertEquals(FaultLog(), FaultLog().finish(ClearResult.Cleared))
        val done = FaultLog().begin(listOf("P0420"), 1).finish(ClearResult.Cleared)
        assertEquals(done, done.finish(ClearResult.Refused))
    }

    @Test fun history_keeps_only_the_latest_twenty() {
        var log = FaultLog()
        for (i in 1..25) log = log.begin(listOf("P0420"), i.toLong()).finish(ClearResult.Cleared)
        assertEquals(20, log.history.size)
        assertEquals(6L, log.history.first().atMs)
        assertEquals(25L, log.history.last().atMs)
    }

    @Test fun encode_and_decode_round_trip() {
        val log = FaultLog()
            .mark(listOf("P0420", "P0133"), 5)
            .begin(listOf("P0420", "P0133"), 10).finish(ClearResult.Returned)
            .begin(emptyList(), 20)
        assertEquals(log, FaultLog.decode(log.encode()))
    }

    @Test fun corrupted_text_decodes_to_an_empty_log() {
        assertEquals(FaultLog(), FaultLog.decode(null))
        assertEquals(FaultLog(), FaultLog.decode(""))
        assertEquals(FaultLog(), FaultLog.decode("garbage\nM|\nH|x|y|z\nM|P0420|notanumber"))
    }

    @Test fun decode_keeps_the_valid_lines_around_a_bad_one() {
        val decoded = FaultLog.decode("M|P0420|5\nbroken\nH|10|CLEARED|P0420")
        assertEquals(mapOf("P0420" to 5L), decoded.marks)
        assertEquals(listOf(ClearRecord(10, listOf("P0420"), ClearResult.Cleared)), decoded.history)
    }
}
