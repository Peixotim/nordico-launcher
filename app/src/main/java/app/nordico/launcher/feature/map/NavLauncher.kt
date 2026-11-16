package app.nordico.launcher.feature.map

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

enum class NavApp(val pkg: String, val label: String) {
    Waze("com.waze", "Waze"),
    Maps("com.google.android.apps.maps", "Google Maps"),
}

enum class Favorite { Home, Work }

/** App de navegação: o salvo, se instalado; senão Waze; senão Google Maps. */
fun chooseNavApp(pref: String?, installed: Set<String>): NavApp? {
    NavApp.entries.firstOrNull { it.pkg == pref && it.pkg in installed }?.let { return it }
    return NavApp.entries.firstOrNull { it.pkg in installed }
}

/** Casa/Trabalho usam os favoritos já salvos no Waze; o Maps não tem link equivalente. */
fun favoriteUri(app: NavApp, fav: Favorite): String? = when (app) {
    NavApp.Waze -> "waze://?favorite=" + if (fav == Favorite.Home) "home" else "work"
    NavApp.Maps -> null
}

fun showSuggestions(app: NavApp?): Boolean = app == NavApp.Waze

fun Context.openNavApp(app: NavApp?) {
    val intent = app?.let { packageManager.getLaunchIntentForPackage(it.pkg) } ?: return
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun Context.openFavorite(app: NavApp?, fav: Favorite) {
    val uri = app?.let { favoriteUri(it, fav) } ?: return
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
    }
}
