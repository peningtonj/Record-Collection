package io.github.peningtonj.recordcollection.ui.components.filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.filter.AlbumFilter
import sun.tools.jconsole.LabeledComponent.layout
 import androidx.compose.foundation.layout.*
 import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

@Composable
fun TextSearchBar(
    currentFilter: AlbumFilter,
    options : Map<String, List<String>>,
    onFilterChange: (AlbumFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val onSelectionChange = { category: String, option: String ->
        // Create completely new collections to ensure state change detection
        val newTags = currentFilter.tags.mapValues { (_, tagList) ->
            tagList.toList() // Create new immutable list
        }.toMutableMap()

        // Add the new option to the appropriate category
        val existingTags = newTags[category]?.toMutableList() ?: mutableListOf()
        if (!existingTags.contains(option)) {
            existingTags.add(option)
            newTags[category] = existingTags.toList() // Convert back to immutable list
        }
        onFilterChange(currentFilter.copy(tags = newTags))
    }

    var searchText by remember { mutableStateOf("") }
    var isDropdownVisible by remember { mutableStateOf(false) }
    var textFieldWidth by remember { mutableStateOf(0) }
    var textFieldHeight by remember { mutableStateOf(0) }

    val filteredOptions = remember(searchText, options) {
        options.mapValues { (category, options) ->
            options.filter { option ->
                restrictedContains(option, searchText)
            }
        }
    }


    val groupedResults = remember(filteredOptions) {
        filteredOptions.filter { (_, options) -> options.isNotEmpty() }
            .mapValues { (_, options) -> options.take(2) }
    }

    Box(modifier = modifier) {
        // Text field
        OutlinedTextField(
            value = searchText,
            onValueChange = { newText ->
                searchText = newText
                isDropdownVisible = newText.isNotBlank() && groupedResults.isNotEmpty()
            },
            placeholder = {
                Text(
                    text = "Search artists & genres",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = if (searchText.isNotBlank()) {
                {
                    IconButton(
                        onClick = {
                            searchText = ""
                            isDropdownVisible = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size ->
                    textFieldWidth = size.width
                    textFieldHeight = size.height
                },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Dropdown using Popup
        if (isDropdownVisible && groupedResults.isNotEmpty()) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, textFieldHeight)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 8.dp
                    ),
                    modifier = Modifier.width(with(LocalDensity.current) { textFieldWidth.toDp() })
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        groupedResults.entries.forEachIndexed { categoryIndex, (category, options) ->
                            // Category header with divider
                            if (categoryIndex > 0) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )

                            // Options for this category
                            options.forEach { option ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectionChange(category, option)
                                            searchText = ""
                                            isDropdownVisible = false
                                        },
                                    color = Color.Transparent
                                ) {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
fun restrictedContains(option: String, searchText: String) : Boolean {
    return option.split(" ").any { subString ->
        subString.lowercase().startsWith(searchText.lowercase())
    } || option.lowercase().startsWith(searchText.lowercase())
}