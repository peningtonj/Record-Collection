// commonMain/ui/LocalDependencyContainer.kt
package io.github.peningtonj.recordcollection.navigation

import androidx.compose.runtime.compositionLocalOf
import io.github.peningtonj.recordcollection.di.DependencyContainer

val LocalDependencyContainer = compositionLocalOf<DependencyContainer> { 
    error("No DependencyContainer provided") 
}