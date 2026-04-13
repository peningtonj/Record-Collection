package io.github.peningtonj.recordcollection

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.peningtonj.recordcollection.di.AndroidDependencyContainerFactory
import io.github.peningtonj.recordcollection.navigation.AndroidNavigator
import io.github.peningtonj.recordcollection.network.oauth.spotify.AndroidAuthHandler

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val dependencyContainer = AndroidDependencyContainerFactory.create(this)
        val navigator = AndroidNavigator()

        // Handle deep-link that may arrive at cold-start
        intent?.data?.let { uri ->
            if (uri.scheme == "recordcollection") {
                AndroidAuthHandler.handleCallback(uri)
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