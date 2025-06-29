package io.github.peningtonj.recordcollection

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import io.github.peningtonj.recordcollection.di.DependencyContainer
import io.github.peningtonj.recordcollection.navigation.Navigator
import io.github.peningtonj.recordcollection.ui.RecordCollectionApp
import androidx.compose.runtime.CompositionLocalProvider
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.navigation.LocalDependencyContainer

// commonMain/ui/App.kt
@Composable
fun App(dependencyContainer: DependencyContainer, navigator: Navigator) {
    MaterialTheme {
        CompositionLocalProvider(
            LocalDependencyContainer provides dependencyContainer,
            LocalNavigator provides navigator
        ) {
            RecordCollectionApp(navigator)
        }
    }
}