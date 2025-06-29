// commonMain/ui/screens/LoginScreen.kt
package io.github.peningtonj.recordcollection.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.navigation.Screen

@Composable
fun LoginScreen() {
    val navigator = LocalNavigator.current
    
    Column {
        Text("Login Screen")
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { 
                navigator.navigateTo(Screen.Profile)
            }
        ) {
            Text("Login with Spotify")
        }
    }
}