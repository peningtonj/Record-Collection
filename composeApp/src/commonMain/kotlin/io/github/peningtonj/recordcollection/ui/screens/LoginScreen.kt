package io.github.peningtonj.recordcollection.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthState
import io.github.peningtonj.recordcollection.viewmodel.rememberLoginViewModel

@Composable
fun LoginScreen() {
    val viewModel = rememberLoginViewModel()
    val authState by viewModel.authState.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (authState) {
            is AuthState.NotAuthenticated -> {
                Button(
                    onClick = { viewModel.startAuth() }
                ) {
                    Text("Login with Spotify")
                }
            }
            
            is AuthState.Authenticating -> {
                CircularProgressIndicator()
            }
            
            is AuthState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.startAuth() }
                    ) {
                        Text("Try Again")
                    }
                }
            }
            
            is AuthState.Authenticated -> {
                // This state should be brief as we navigate away
                CircularProgressIndicator()
            }
        }
    }
}