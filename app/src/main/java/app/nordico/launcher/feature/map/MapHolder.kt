package app.nordico.launcher.feature.map

import android.content.Context
import android.view.Gravity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.nordico.launcher.data.location.LatLng
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.geometry.LatLng as MlLatLng

const val MAP_ZOOM = 15.4

/**
 * Um único MapView para o app inteiro, ligado ao ciclo de vida da activity.
 * Recriar a cada volta ao Início custava contexto GL + estilo e ~2,7 MB por visita.
 */
class MapHolder(context: Context) {
    val view: MapView
    private var map: MapLibreMap? = null
    private var style: String? = null
    private var wantedStyle: String? = null
    private var wantedTarget: LatLng? = null
    private var started = false
    private var resumed = false

    init {
        MapLibre.getInstance(context)
        // TextureView: respeita o recorte arredondado do Compose (SurfaceView ignora).
        view = MapView(context, MapLibreMapOptions.createFromAttributes(context).textureMode(true))
        view.onCreate(null)
        view.getMapAsync { m ->
            m.uiSettings.setAllGesturesEnabled(false)
            m.uiSettings.isLogoEnabled = false
            m.uiSettings.attributionGravity = Gravity.TOP or Gravity.END
            map = m
            wantedStyle?.let(::setStyle)
            wantedTarget?.let(::moveTo)
        }
    }

    fun setStyle(url: String) {
        wantedStyle = url
        val m = map ?: return
        if (style == url) return
        style = url
        m.setStyle(Style.Builder().fromUri(url))
    }

    fun moveTo(target: LatLng) {
        wantedTarget = target
        map?.cameraPosition = CameraPosition.Builder().target(MlLatLng(target.lat, target.lng)).zoom(MAP_ZOOM).build()
    }

    fun recenter(target: LatLng) {
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(MlLatLng(target.lat, target.lng), MAP_ZOOM))
    }

    fun onEvent(e: Lifecycle.Event) {
        when (e) {
            Lifecycle.Event.ON_START -> if (!started) { view.onStart(); started = true }
            Lifecycle.Event.ON_RESUME -> if (!resumed) { view.onResume(); resumed = true }
            Lifecycle.Event.ON_PAUSE -> if (resumed) { view.onPause(); resumed = false }
            Lifecycle.Event.ON_STOP -> if (started) { view.onStop(); started = false }
            else -> Unit
        }
    }

    /** Sempre na ordem pause → stop → destroy. */
    fun destroy() {
        onEvent(Lifecycle.Event.ON_PAUSE)
        onEvent(Lifecycle.Event.ON_STOP)
        view.onDestroy()
    }
}

/** Cria o MapHolder uma vez por activity e o liga ao ciclo de vida dela. */
@Composable
fun rememberMapHolder(): MapHolder {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val holder = remember { MapHolder(context) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, e -> holder.onEvent(e) }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            holder.destroy()
        }
    }
    return holder
}
