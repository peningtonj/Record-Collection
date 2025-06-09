package io.github.peningtonj.recordstore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview


import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthState
import io.github.peningtonj.recordcollection.viewmodel.SplashViewModel

@Composable
fun App() {
    MaterialTheme {
        val viewModel = remember { SplashViewModel() }
        val authState by viewModel.authState.collectAsState()
        val profile by viewModel.profileState.collectAsState()

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (authState) {
                is AuthState.NotAuthenticated -> {
                    Button(onClick = { viewModel.startAuth() }) {
                        Text("Login with Spotify")
                    }
                }
                is AuthState.Authenticating -> {
                    CircularProgressIndicator()
                }
                is AuthState.Authenticated -> {
                    profile?.let { userProfile ->
                        Text("Welcome, ${userProfile.displayName ?: "Spotify User"}!")
                    } ?: Text("Loading profile...")
                }
                is AuthState.Error -> {
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}