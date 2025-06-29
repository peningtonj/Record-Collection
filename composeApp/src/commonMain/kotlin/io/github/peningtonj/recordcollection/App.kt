package io.github.peningtonj.recordcollection

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import io.github.peningtonj.recordcollection.di.DependencyContainer
import io.github.peningtonj.recordcollection.navigation.Navigator
import io.github.peningtonj.recordcollection.ui.RecordCollectionApp

@Composable
fun App(dependencyContainer: DependencyContainer, navigator: Navigator) {
    MaterialTheme {
        RecordCollectionApp(navigator)
    }
}
