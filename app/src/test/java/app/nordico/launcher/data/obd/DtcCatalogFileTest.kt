package app.nordico.launcher.data.obd

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Valida o `catalog.json` gerado por `tools/dtc/build_catalog.py` (o que o app realmente embute). */
class DtcCatalogFileTest {
    private val file = File("src/main/assets/dtc/catalog.json")
    private val text by lazy { file.readText(Charsets.UTF_8) }
    private val root by lazy { JSONObject(text).getJSONObject("codes") }
    private val catalog by lazy { DtcCatalog.parse(text) }
    private val codeFormat = Regex("^[PCBU][0-3][0-9A-F]{3}$")

    @Test fun the_file_is_large_and_every_code_is_well_formed() {
        assertTrue("faltam códigos: ${catalog.size}", catalog.size > 9_000)
        assertEquals(root.length(), catalog.size)
        for (code in root.keys()) assertTrue("código inválido: $code", codeFormat.matches(code))
    }

    @Test fun every_entry_has_a_title_a_known_severity_and_a_known_tier() {
        for (code in root.keys()) {
            val e = root.getJSONObject(code)
            assertTrue("$code sem título", e.getString("t").isNotBlank())
            assertTrue("$code gravidade inválida", e.optString("s", "attention") in setOf("ok", "attention", "stop"))
            assertTrue("$code camada inválida", e.optInt("r", 3) in 1..3)
            assertTrue("$code idioma inválido", e.optString("l", "pt") in setOf("pt", "en"))
        }
    }

    @Test fun full_entries_have_causes_and_a_solution_in_portuguese() {
        for (code in root.keys()) {
            val e = root.getJSONObject(code)
            if (e.optInt("r", 3) != 1) continue
            assertTrue("$code sem causas", e.getJSONArray("c").length() > 0)
            assertTrue("$code sem solução", e.getString("x").isNotBlank())
            assertEquals("$code deveria estar em português", "pt", e.optString("l", "pt"))
        }
    }

    @Test fun manufacturer_codes_never_get_a_generic_title() {
        // P1xxx depende da marca: só entra com título em inglês da lista Honda ou curado à mão.
        for (code in root.keys().asSequence().filter { it.startsWith("P1") }) {
            val tier = root.getJSONObject(code).optInt("r", 3)
            assertTrue("$code com título genérico (camada $tier)", tier != 2)
        }
    }

    @Test fun common_codes_are_present() {
        for (code in listOf("P0420", "P0171", "P0300", "P0133", "P0442", "P0455", "P1456")) {
            assertTrue("$code ausente", catalog.lookup(code).known)
        }
    }

    @Test fun portuguese_titles_exist_for_the_most_common_generic_codes() {
        assertEquals("pt", catalog.lookup("P0420").titleLang)
        assertFalse(catalog.lookup("P0420").title.contains("Catalyst"))
    }
}
