// commonMain/ui/screens/SettingsScreen.kt
package io.github.peningtonj.recordcollection.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.peningtonj.recordcollection.repository.CacheSize
import io.github.peningtonj.recordcollection.repository.SortOrder
import io.github.peningtonj.recordcollection.repository.SyncInterval
import io.github.peningtonj.recordcollection.repository.Theme
import io.github.peningtonj.recordcollection.viewmodel.SettingsViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberSettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = rememberSettingsViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        // Appearance Section
        SettingsSection(title = "Appearance") {
            SettingsDropdown(
                title = "Theme",
                subtitle = "Choose the theme for the app",
                currentValue = settings.theme,
                options = Theme.entries.toList(),
                onSelectionChange = { viewModel.updateTheme(it) }
            )

        }

        // Library Section
        SettingsSection(title = "Library") {
            SettingsDropdown(
                title = "Default Sort Order",
                subtitle = "How albums are sorted by default",
                currentValue = settings.defaultSortOrder,
                options = SortOrder.entries.toList(),
                onSelectionChange = { viewModel.updateDefaultSortOrder(it) }
            )
            SettingsRow(
                title = "Default add to Library",
                subtitle = "Default setting for Collections"
            ) {
                Switch(
                    checked = settings.defaultOnAddToCollection,
                    onCheckedChange = { viewModel.toggleDefaultOnAddToCollection() }
                )
            }

            SettingsRow(
                title = "Save all tracks on 4.5+ ratings",
                subtitle = "Auto add tracks for favourite albums"
            ) {
                Switch(
                    checked = settings.addTracksOnMaxRating,
                    onCheckedChange = { viewModel.toggleAddTracksOnMaxRating() }
                )
            }
        }

        SettingsSection(title = "Playback") {
            SettingsRow(
                title = "Play transition between albums",
                subtitle = "Play a short transition sound effect between albums"
            ) {
                Switch(
                    checked = settings.transitionTrack,
                    onCheckedChange = { viewModel.toggleTransitionTrack() }
                )
            }
        }

        SettingsSection(title = "Collection") {
            SettingsRow(
                title = "OpenAI API Key",
                subtitle = "Required for importing collections from articles"
            ) {
                OpenAiKeySettingRow(
                    key = settings.openAiApiKey,
                    onKeyChange = { viewModel.updateOpenAiApiKey(it) },
                    onValidate = { viewModel.validateOpenAiApiKey() }
                )
            }
        }

        // Reset Section
        SettingsSection(title = "Reset") {
            Button(
                onClick = { viewModel.resetToDefaults() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset to Defaults")
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    action: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        action()
    }
}
@Composable
private fun <T> SettingsDropdown(
    title: String,
    subtitle: String? = null,
    currentValue: T,
    options: List<T>,
    onSelectionChange: (T) -> Unit
) where T : Enum<T> {
    var expanded by remember { mutableStateOf(false) }

    SettingsRow(
        title = title,
        subtitle = subtitle
    ) {
        // Anchor the dropdown to this Box
        Box {
            TextButton(
                onClick = { expanded = true }
            ) {
                Text(getDisplayName(currentValue))
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(getDisplayName(option)) },
                        onClick = {
                            onSelectionChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun <T> getDisplayName(value: T): String = when (value) {
    is SyncInterval -> value.displayName
    is SortOrder -> value.displayName
    is CacheSize -> value.displayName
    is Theme -> value.displayName
    else -> value.toString()
}

@Composable
fun OpenAiKeySettingRow(
    key: String,
    onKeyChange: (String) -> Unit,
    onValidate: () -> Unit,
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(key)) }

    // Sync state when it comes from ViewModel
    LaunchedEffect(key) {
        if (key != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(text = key)
        }
    }

    val scrollState = rememberScrollState()

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            textFieldValue = newValue
            onKeyChange(newValue.text)
            onValidate()
        },
        modifier = Modifier
            .width(280.dp)
            .horizontalScroll(scrollState),
        singleLine = true,
        visualTransformation = VisualTransformation.None
    )
}
