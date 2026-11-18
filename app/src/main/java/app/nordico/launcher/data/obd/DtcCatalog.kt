package app.nordico.launcher.data.obd

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Quão urgente é a falha. Vermelho de sistema só para [Stop]. */
enum class Severity { Ok, Attention, Stop }

/** Quanta informação o catálogo tem: completa em português, só título em português, só título em inglês. */
enum class Tier { Full, TitlePt, Basic }

data class DtcInfo(
    val code: String,
    val title: String,
    /** `pt` ou `en`. */
    val titleLang: String,
    val severity: Severity,
    val causes: List<String>,
    val symptoms: List<String>,
    val solution: String,
    /** `powertrain`, `body`, `chassis` ou `network`. */
    val category: String,
    val tier: Tier,
    /** `false` = código que não está no catálogo (texto genérico da categoria). */
    val known: Boolean,
)

private const val UNKNOWN_TITLE = "Descrição não disponível no Nórdico"
private const val MANUFACTURER_TITLE = "Código específico do fabricante, sem descrição no Nórdico"

private fun categoryOf(code: String): String = when (code.firstOrNull()) {
    'B' -> "body"
    'C' -> "chassis"
    'U' -> "network"
    else -> "powertrain"
}

private fun genericSolution(category: String): String = when (category) {
    "body" -> "Código de carroceria (conforto e segurança). Procure o código na internet junto com o modelo do carro ou leve a uma oficina."
    "chassis" -> "Código de chassi (freios, direção, suspensão). Procure uma oficina antes de rodar muito."
    "network" -> "Código de comunicação entre módulos do carro. Costuma ser bateria fraca, fiação ou um módulo; leve a uma oficina."
    else -> "Procure o código na internet junto com o modelo do carro ou leve a uma oficina."
}

/**
 * Catálogo de códigos de falha embutido no app (offline). O `catalog.json` é gerado por `tools/dtc`.
 * [lookup] nunca devolve nulo: o que não está no catálogo recebe um texto genérico pela categoria.
 */
class DtcCatalog(private val entries: Map<String, DtcInfo>) {
    val size: Int get() = entries.size

    fun lookup(code: String): DtcInfo {
        val c = code.trim().uppercase()
        return entries[c] ?: fallback(c)
    }

    private fun fallback(code: String): DtcInfo {
        val category = categoryOf(code)
        val title = if (code.startsWith("P1")) MANUFACTURER_TITLE else UNKNOWN_TITLE
        return DtcInfo(code, title, "pt", Severity.Attention, emptyList(), emptyList(), genericSolution(category), category, Tier.Basic, known = false)
    }

    companion object {
        /** JSON malformado devolve um catálogo vazio (só o fallback), nunca lança. */
        fun parse(json: String): DtcCatalog = try {
            val codes = JSONObject(json).getJSONObject("codes")
            val map = HashMap<String, DtcInfo>(codes.length() * 2)
            for (key in codes.keys()) {
                val o = codes.getJSONObject(key)
                val code = key.uppercase()
                val category = o.optString("g").ifEmpty { categoryOf(code) }
                map[code] = DtcInfo(
                    code = code,
                    title = o.getString("t"),
                    titleLang = o.optString("l", "pt"),
                    severity = severityOf(o.optString("s")),
                    causes = o.optJSONArray("c").strings(),
                    symptoms = o.optJSONArray("y").strings(),
                    solution = o.optString("x").ifEmpty { genericSolution(category) },
                    category = category,
                    tier = when (o.optInt("r", 3)) { 1 -> Tier.Full; 2 -> Tier.TitlePt; else -> Tier.Basic },
                    known = true,
                )
            }
            DtcCatalog(map)
        } catch (_: JSONException) {
            DtcCatalog(emptyMap())
        }

        private fun severityOf(s: String) = when (s) {
            "ok" -> Severity.Ok
            "stop" -> Severity.Stop
            else -> Severity.Attention
        }

        private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else List(length()) { getString(it) }
    }
}

/** Lê o `assets/dtc/catalog.json`. Chamar fora da thread principal: o arquivo tem alguns MB. */
fun loadCatalog(context: Context): DtcCatalog = try {
    context.assets.open("dtc/catalog.json").bufferedReader().use { DtcCatalog.parse(it.readText()) }
} catch (_: java.io.IOException) {
    DtcCatalog(emptyMap())
}
