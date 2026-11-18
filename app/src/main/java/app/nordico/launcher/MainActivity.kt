package app.nordico.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.nordico.launcher.data.location.LOCATION_PERMISSIONS
import app.nordico.launcher.navigation.AppShell
import app.nordico.launcher.navigation.AppViewModel
import app.nordico.launcher.navigation.Screen

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels {
        viewModelFactory { initializer { AppViewModel((application as App).container) } }
    }

    /** Tecla HOME com o launcher já aberto (singleTask): volta ao Início, como todo launcher. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) vm.go(Screen.Home)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        // Tela inicial: no Início, voltar não sai do launcher (as outras telas tratam no AppShell).
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })
        val container = (application as App).container
        val askBluetooth = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
        container.requestBluetoothPermission = { askBluetooth.launch(android.Manifest.permission.BLUETOOTH_CONNECT) }
        val askLocation = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.any { it }) container.onLocationPermissionGranted()
        }
        if (LOCATION_PERMISSIONS.any { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
            container.onLocationPermissionGranted()
        } else {
            askLocation.launch(LOCATION_PERMISSIONS)
        }
        setContent {
            val density = Density(logicalDensity(resources.displayMetrics.widthPixels), fontScale = 1f)
            CompositionLocalProvider(LocalDensity provides density) {
                AppShell(vm, container)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as App).container.onResume()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
