package app.nordico.launcher.feature.vehicle

import app.nordico.launcher.data.obd.ClearBlock
import app.nordico.launcher.data.obd.ClearOutcome
import app.nordico.launcher.data.obd.Dtc
import app.nordico.launcher.data.obd.Severity
import app.nordico.launcher.data.prefs.ClearResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FaultsLogicTest {
    @Test fun summary_counts_and_mentions_the_engine_light() {
        assertEquals("Nenhuma falha registrada", faultsSummary(0, false))
        assertEquals("1 falha · luz do motor acesa", faultsSummary(1, true))
        assertEquals("2 falhas", faultsSummary(2, false))
        assertEquals("3 falhas · luz do motor acesa", faultsSummary(3, true))
        assertEquals("2 falhas", faultsSummary(2, null))
    }

    @Test fun severity_labels() {
        assertEquals("Pode seguir", severityLabel(Severity.Ok))
        assertEquals("Atenção", severityLabel(Severity.Attention))
        assertEquals("Pare e procure ajuda", severityLabel(Severity.Stop))
    }

    @Test fun the_video_link_is_a_youtube_search_with_the_code_and_the_car() {
        val url = videoUrl("P0420", "Honda Civic")
        assertTrue(url.startsWith("https://www.youtube.com/results?search_query="))
        assertTrue(url.contains("P0420"))
        assertTrue(url.contains("Honda%20Civic") || url.contains("Honda+Civic"))
        assertFalse(url.contains(" "))
    }

    @Test fun the_video_is_only_for_a_parked_car() {
        assertTrue(canOpenVideo(0))
        assertFalse(canOpenVideo(5))
        assertFalse(canOpenVideo(null))
    }

    @Test fun the_clear_hint_explains_why_it_is_blocked() {
        assertEquals("Apaga todas de uma vez. O carro não permite escolher uma só.", clearHint(null))
        assertEquals("Pare o carro para apagar falhas.", clearHint(ClearBlock.Moving))
        assertEquals("Conecte o adaptador OBD para apagar falhas.", clearHint(ClearBlock.NotConnected))
        assertEquals("Não há falhas para apagar.", clearHint(ClearBlock.NothingToClear))
    }

    @Test fun every_outcome_has_a_title_a_message_and_a_button() {
        val outcomes = listOf(
            ClearOutcome.Cleared, ClearOutcome.Returned(listOf(Dtc("P0420"))), ClearOutcome.Refused(0x22), ClearOutcome.Refused(0x11),
            ClearOutcome.Unverified, ClearOutcome.NoResponse,
            ClearOutcome.Blocked(ClearBlock.Moving), ClearOutcome.Blocked(ClearBlock.NotConnected), ClearOutcome.Blocked(ClearBlock.NothingToClear),
        )
        for (o in outcomes) {
            val t = outcomeText(o)
            assertTrue("$o título", t.title.isNotBlank())
            assertTrue("$o mensagem", t.message.isNotBlank())
            assertTrue("$o botão", t.button.isNotBlank())
        }
    }

    @Test fun outcome_texts_say_what_matters() {
        assertTrue(outcomeText(ClearOutcome.Cleared).message.contains("prontidão"))
        assertTrue(outcomeText(ClearOutcome.Returned(listOf(Dtc("P0420"), Dtc("P0133")))).message.contains("P0420, P0133"))
        val refused = outcomeText(ClearOutcome.Refused(0x22)).message
        assertTrue(refused.contains("motor") && refused.contains("chave"))
        assertTrue(outcomeText(ClearOutcome.Refused(0x11)).message.contains("0x11"))
        assertTrue(outcomeText(ClearOutcome.NoResponse).message.contains("Nada foi reenviado"))
    }

    @Test fun only_sent_commands_reach_the_history() {
        assertEquals(ClearResult.Cleared, ClearOutcome.Cleared.toResult())
        assertEquals(ClearResult.Returned, ClearOutcome.Returned(emptyList()).toResult())
        assertEquals(ClearResult.Refused, ClearOutcome.Refused(0x22).toResult())
        assertEquals(ClearResult.Unverified, ClearOutcome.Unverified.toResult())
        assertEquals(ClearResult.NoResponse, ClearOutcome.NoResponse.toResult())
        assertNull(ClearOutcome.Blocked(ClearBlock.Moving).toResult())
    }

    @Test fun selection_toggles_and_keeps_order_independent() {
        assertEquals(setOf("P0420"), toggled(emptySet(), "P0420"))
        assertEquals(emptySet<String>(), toggled(setOf("P0420"), "P0420"))
        assertEquals(setOf("P0420", "P0133"), toggled(setOf("P0420"), "P0133"))
    }
}
