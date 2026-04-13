package io.github.peningtonj.recordcollection

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.peningtonj.recordcollection.navigation.AndroidNavigator
import io.github.peningtonj.recordcollection.network.oauth.spotify.AndroidAuthHandler
import io.github.peningtonj.recordcollection.service.PlaybackMonitorService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // DI container lives in the Application so it is shared with PlaybackMonitorService
        val dependencyContainer =
            (application as RecordCollectionApplication).dependencyContainer
        val navigator = AndroidNavigator()

        // Handle deep-link that may arrive at cold-start
        intent?.data?.let { uri ->
            if (uri.scheme == "recordcollection") {
                AndroidAuthHandler.handleCallback(uri)
            }
        }

        // Start / stop the foreground service based on whether there is an active
        // queue session.  repeatOnLifecycle(CREATED) keeps collecting even while
        // the Activity is stopped (screen off) — it only cancels on onDestroy.
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                dependencyContainer.playbackSessionManager.currentSession.collect { session ->
                    if (session != null) {
                        PlaybackMonitorService.start(this@MainActivity)
                    } else {
                        PlaybackMonitorService.stop(this@MainActivity)
                    }
                }
            }
        }

        setContent {
            App(dependencyContainer, navigator)
        }
    }

    /** Handle OAuth redirect when the app is already running (singleTop re-launch) */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { uri ->
            if (uri.scheme == "recordcollection") {
                AndroidAuthHandler.handleCallback(uri)
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    // Lightweight preview placeholder; full DI not available in preview
}