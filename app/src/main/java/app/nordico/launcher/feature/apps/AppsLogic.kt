package app.nordico.launcher.feature.apps

import app.nordico.launcher.data.apps.AppEntry
import app.nordico.launcher.data.apps.AppSection
import app.nordico.launcher.data.apps.InternalRoute
import java.text.Normalizer

data class AppSections(val mine: List<AppEntry>, val car: List<AppEntry>, val hidden: List<AppEntry>)

private val ACCENTS = Regex("\\p{Mn}+")

/** Sem acento e minúsculo ("Câmera" → "camera"). */
fun normalize(s: String): String = ACCENTS.replace(Normalizer.normalize(s, Normalizer.Form.NFD), "").lowercase()

/** Separa em Seus apps / Do carro / Ocultos, filtrando pela busca. Ids ocultos que não existem mais são ignorados. */
fun sectionApps(apps: List<AppEntry>, hidden: Set<String>, query: String): AppSections {
    val q = normalize(query.trim())
    val matching = apps.filter { q.isEmpty() || normalize(it.label).contains(q) }
    val (hiddenApps, visible) = matching.partition { it.id in hidden }
    return AppSections(
        mine = visible.filter { it.section == AppSection.Mine },
        car = visible.filter { it.section == AppSection.Car },
        hidden = hiddenApps,
    )
}

/** A tela Clima aparece na gaveta como app do carro (só quando há dados do ar). */
val CLIMATE_APP = AppEntry(id = "internal.climate", label = "Clima", section = AppSection.Car, glyph = "ac_unit", route = InternalRoute.Climate)

/** A tela Veículo aparece na gaveta como app do carro (sempre; sem adaptador ela mostra como conectar). */
val VEHICLE_APP = AppEntry(id = "internal.vehicle", label = "Veículo", section = AppSection.Car, glyph = "speed", route = InternalRoute.Vehicle)
