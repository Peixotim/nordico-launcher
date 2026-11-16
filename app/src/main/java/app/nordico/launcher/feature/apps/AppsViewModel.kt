package app.nordico.launcher.feature.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.nordico.launcher.AppContainer
import app.nordico.launcher.data.apps.AppEntry
import app.nordico.launcher.data.prefs.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppsViewModel(private val c: AppContainer) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _editing = MutableStateFlow(false)
    val editing: StateFlow<Boolean> = _editing

    private val prefs: StateFlow<Prefs> = c.prefs.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, Prefs())

    val sections: StateFlow<AppSections> = combine(c.apps.apps, c.vehicle.climate, prefs, _query) { apps, climate, p, q ->
        sectionApps(if (climate != null) apps + CLIMATE_APP else apps, p.hiddenApps, q)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppSections(emptyList(), emptyList(), emptyList()))

    fun setQuery(q: String) {
        _query.value = q
    }

    fun setEditing(on: Boolean) {
        _editing.value = on
    }

    fun toggleHidden(id: String) {
        val hidden = id in prefs.value.hiddenApps
        viewModelScope.launch { c.prefs.setHidden(id, !hidden) }
    }

    fun launch(app: AppEntry) = c.apps.launch(app)
}
