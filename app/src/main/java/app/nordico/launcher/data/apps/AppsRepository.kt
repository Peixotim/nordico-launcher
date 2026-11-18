package app.nordico.launcher.data.apps

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.StateFlow

enum class AppSection { Mine, Car }

/** Telas internas do launcher que aparecem na gaveta como apps. */
enum class InternalRoute { Climate, Vehicle }

data class AppEntry(
    val id: String,
    val label: String,
    val section: AppSection,
    val icon: ImageBitmap? = null,
    val glyph: String? = null,
    val brandColor: Color? = null,
    val route: InternalRoute? = null,
)

interface AppsRepository {
    val apps: StateFlow<List<AppEntry>>
    fun launch(app: AppEntry)
}
