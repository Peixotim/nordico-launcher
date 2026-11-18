package app.nordico.launcher.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import app.nordico.launcher.data.radio.Band
import app.nordico.launcher.data.radio.Presets
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ScreenMode { Auto, Day, Night }

enum class Accent { Orange, Blue, Green, Graphite }

data class Prefs(
    val screenMode: ScreenMode = ScreenMode.Auto,
    val accent: Accent = Accent.Orange,
    val hiddenApps: Set<String> = emptySet(),
    val navApp: String? = null,
    val obdAdapter: String? = null,
    val radioPresets: Presets = Presets(),
    val faultLog: FaultLog = FaultLog(),
)

private val Context.dataStore by preferencesDataStore("prefs")

private val MODE = stringPreferencesKey("screen_mode")
private val ACCENT = stringPreferencesKey("accent")
private val HIDDEN = stringSetPreferencesKey("hidden_apps")
private val NAV = stringPreferencesKey("nav_app")
private val OBD = stringPreferencesKey("obd_adapter")
private val PRESETS_FM = stringPreferencesKey("radio_presets_fm")
private val PRESETS_AM = stringPreferencesKey("radio_presets_am")
private val FAULT_LOG = stringPreferencesKey("fault_log")

/** Preferências salvas (iguais nos dois flavors). Valores inválidos voltam ao padrão. */
class PrefsRepository(private val context: Context) {
    val prefs: Flow<Prefs> = context.dataStore.data.map { p ->
        Prefs(
            screenMode = p[MODE]?.let { runCatching { ScreenMode.valueOf(it) }.getOrNull() } ?: ScreenMode.Auto,
            accent = p[ACCENT]?.let { runCatching { Accent.valueOf(it) }.getOrNull() } ?: Accent.Orange,
            hiddenApps = p[HIDDEN] ?: emptySet(),
            navApp = p[NAV],
            obdAdapter = p[OBD],
            radioPresets = Presets.decode(p[PRESETS_FM], p[PRESETS_AM]),
            faultLog = FaultLog.decode(p[FAULT_LOG]),
        )
    }

    suspend fun setScreenMode(m: ScreenMode) {
        context.dataStore.edit { it[MODE] = m.name }
    }

    suspend fun setObdAdapter(address: String) {
        context.dataStore.edit { it[OBD] = address }
    }

    /** Os presets do rádio são do Nórdico, não do fabricante (spec 10.3). */
    suspend fun setPreset(band: Band, index: Int, khz: Int) {
        context.dataStore.edit {
            val current = Presets.decode(it[PRESETS_FM], it[PRESETS_AM]).save(band, index, khz).encode()
            it[PRESETS_FM] = current.first
            it[PRESETS_AM] = current.second
        }
    }

    /** Atualiza as marcas e o histórico de falhas de forma atômica (leitura, mudança e gravação juntas). */
    suspend fun updateFaultLog(change: (FaultLog) -> FaultLog) {
        context.dataStore.edit { it[FAULT_LOG] = change(FaultLog.decode(it[FAULT_LOG])).encode() }
    }

    suspend fun setHidden(id: String, hidden: Boolean) {
        context.dataStore.edit {
            val current = it[HIDDEN] ?: emptySet()
            it[HIDDEN] = if (hidden) current + id else current - id
        }
    }
}
