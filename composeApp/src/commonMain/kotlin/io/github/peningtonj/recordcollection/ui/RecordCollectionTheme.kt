package io.github.peningtonj.recordcollection.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.peningtonj.recordcollection.repository.SettingsRepository
import io.github.peningtonj.recordcollection.repository.Theme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.viewmodel.SettingsViewModel

@Composable
fun RecordCollectionTheme(
    viewModel: SettingsViewModel,
    content: @Composable () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val systemInDarkTheme = isSystemInDarkTheme()

    val darkTheme = when (settings.theme) {
        Theme.LIGHT -> false
        Theme.DARK -> true
        Theme.SYSTEM -> systemInDarkTheme
    }



    // Use Material 3's built-in color schemes
    val colorScheme = if (darkTheme) {
        darkColorScheme() // Built-in dark colors
    } else {
        lightColorScheme() // Built-in light colors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
