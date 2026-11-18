package app.nordico.launcher.data.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DtcCatalogTest {
    private val json = """
        {"version":1,"codes":{
          "P0420":{"t":"Eficiência do catalisador abaixo do limite (banco 1)","l":"pt","s":"attention",
                   "c":["Catalisador envelhecido","Sensor traseiro lento"],"y":["Luz do motor acesa"],
                   "x":"Confira vazamentos no escape.","g":"powertrain","r":1},
          "P0303":{"t":"Falha de ignição no cilindro 3","s":"stop","c":["Vela"],"x":"Procure uma oficina.","g":"powertrain","r":1},
          "P0001":{"t":"Controle regulador de volume de combustível – circuito aberto","l":"pt","g":"powertrain","r":2},
          "P1456":{"t":"Evaporative Emission Control System Leak","l":"en","g":"powertrain","r":3}
        }}
    """.trimIndent()

    private val catalog = DtcCatalog.parse(json)

    @Test fun reads_a_full_entry() {
        val i = catalog.lookup("P0420")
        assertTrue(i.known)
        assertEquals("Eficiência do catalisador abaixo do limite (banco 1)", i.title)
        assertEquals("pt", i.titleLang)
        assertEquals(Severity.Attention, i.severity)
        assertEquals(listOf("Catalisador envelhecido", "Sensor traseiro lento"), i.causes)
        assertEquals(listOf("Luz do motor acesa"), i.symptoms)
        assertEquals("Confira vazamentos no escape.", i.solution)
        assertEquals(Tier.Full, i.tier)
    }

    @Test fun lookup_ignores_case_and_spaces() {
        assertTrue(catalog.lookup(" p0420 ").known)
    }

    @Test fun severity_and_language_have_defaults() {
        assertEquals(Severity.Stop, catalog.lookup("P0303").severity)
        assertEquals("pt", catalog.lookup("P0303").titleLang)
        assertEquals(Severity.Attention, catalog.lookup("P0001").severity)
    }

    @Test fun lighter_tiers_have_no_causes_but_a_generic_solution() {
        val title = catalog.lookup("P0001")
        assertEquals(Tier.TitlePt, title.tier)
        assertTrue(title.causes.isEmpty())
        assertTrue(title.solution.isNotBlank())
        val basic = catalog.lookup("P1456")
        assertEquals(Tier.Basic, basic.tier)
        assertEquals("en", basic.titleLang)
        assertTrue(basic.solution.isNotBlank())
    }

    @Test fun unknown_codes_get_a_fallback_by_category() {
        val p = catalog.lookup("P0999")
        assertFalse(p.known)
        assertEquals("Descrição não disponível no Nórdico", p.title)
        assertEquals(Severity.Attention, p.severity)
        assertEquals("powertrain", p.category)
        assertEquals("body", catalog.lookup("B1234").category)
        assertEquals("chassis", catalog.lookup("C0123").category)
        assertEquals("network", catalog.lookup("U0100").category)
        assertTrue(catalog.lookup("P1ABC").solution.isNotBlank())
        assertEquals("Código específico do fabricante, sem descrição no Nórdico", catalog.lookup("P1ABC").title)
    }

    @Test fun malformed_json_gives_an_empty_catalog_that_still_answers() {
        val empty = DtcCatalog.parse("{not json")
        assertEquals(0, empty.size)
        assertFalse(empty.lookup("P0420").known)
    }

    @Test fun counts_the_entries() {
        assertEquals(4, catalog.size)
    }
}
