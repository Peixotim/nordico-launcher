package app.nordico.launcher.device

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import app.nordico.launcher.LOGICAL_WIDTH_DP
import app.nordico.launcher.data.apps.AppEntry
import app.nordico.launcher.data.apps.AppSection
import app.nordico.launcher.data.apps.AppsRepository
import app.nordico.launcher.data.apps.VENDOR_APPS
import app.nordico.launcher.feature.apps.normalize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Apps instalados com ícone de abertura (LauncherApps). `id` = componente, único mesmo com 2 atividades no pacote. */
class DeviceAppsRepository(private val context: Context, private val scope: CoroutineScope) : AppsRepository {
    private val launcher = context.getSystemService(LauncherApps::class.java)
    private val user = Process.myUserHandle()
    private val iconPx = (88 * context.resources.displayMetrics.widthPixels / LOGICAL_WIDTH_DP.toFloat()).roundToInt()
    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    override val apps: StateFlow<List<AppEntry>> = _apps

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = reload()
        override fun onPackageAdded(packageName: String, user: UserHandle) = reload()
        override fun onPackageChanged(packageName: String, user: UserHandle) = reload()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
    }

    init {
        launcher.registerCallback(callback, Handler(Looper.getMainLooper()))
        reload()
    }

    private fun reload() {
        scope.launch(Dispatchers.Default) {
            _apps.value = launcher.getActivityList(null, user)
                .filter { it.componentName.packageName != context.packageName }
                .map(::entry)
                .sortedBy { normalize(it.label) }
        }
    }

    private fun entry(info: LauncherActivityInfo): AppEntry {
        val pkg = info.componentName.packageName
        val vendor = VENDOR_APPS[pkg]
        val system = info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
        return AppEntry(
            id = info.componentName.flattenToString(),
            label = vendor?.second ?: info.label.toString(),
            section = if (system) AppSection.Car else AppSection.Mine,
            icon = if (vendor == null) iconBitmap(info.getIcon(0)) else null,
            glyph = vendor?.first,
        )
    }

    /** Ícone adaptativo desenhado sem a máscara do sistema (o recorte squircle é feito na UI). */
    private fun iconBitmap(d: Drawable): ImageBitmap {
        if (d !is AdaptiveIconDrawable) return d.toBitmap(iconPx, iconPx).asImageBitmap()
        val bmp = Bitmap.createBitmap(iconPx, iconPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val inset = iconPx / 4 // camadas têm 108 dp, área visível 72 dp: 1,5× o ícone, centralizadas
        listOfNotNull(d.background, d.foreground).forEach {
            it.setBounds(-inset, -inset, iconPx + inset, iconPx + inset)
            it.draw(canvas)
        }
        return bmp.asImageBitmap()
    }

    override fun launch(app: AppEntry) {
        val component = ComponentName.unflattenFromString(app.id) ?: return
        runCatching { launcher.startMainActivity(component, user, null, null) }
    }
}
