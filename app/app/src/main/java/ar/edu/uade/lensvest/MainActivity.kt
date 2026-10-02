package ar.edu.uade.lensvest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.lensvest.presentation.navigation.AppNavigation
import ar.edu.uade.lensvest.presentation.ui.home.BerryCallback
import ar.edu.uade.lensvest.ui.theme.LensVestTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** Latest Berry OAuth redirect, until the Home screen consumes it. */
    private val berryCallback = MutableStateFlow<BerryCallback?>(null)

    private val requestLocalNetwork =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleRedirect(intent)
        requestLocalNetworkForDevServer()

        setContent {
            LensVestTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val callback by berryCallback.collectAsStateWithLifecycle()
                    AppNavigation(
                        berryCallback = callback,
                        onBerryCallbackHandled = { berryCallback.value = null },
                        modifier = Modifier.safeDrawingPadding(),
                    )
                }
            }
        }
    }

    // singleTop: Berry's redirect comes back to this same activity.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleRedirect(intent)
    }

    private fun handleRedirect(intent: Intent?) {
        intent?.data?.let { berryCallback.value = BerryCallback.from(it) }
    }

    /** Debug builds talk to http://10.0.2.2, which Android 17+ treats as local network. */
    private fun requestLocalNetworkForDevServer() {
        if (!BuildConfig.DEBUG || Build.VERSION.SDK_INT < 37) return
        val permission = Manifest.permission.ACCESS_LOCAL_NETWORK
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            requestLocalNetwork.launch(permission)
        }
    }
}
