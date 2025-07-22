package io.github.peningtonj.recordcollection.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> SettingsChipRow(
    options: List<T>,
    selected: T,
    onOptionSelected: (T) -> Unit,
    labelMapper: (T) -> String = { it.toString() }
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(8.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onOptionSelected(option) },
                label = { Text(labelMapper(option)) }
            )
        }
    }
}
