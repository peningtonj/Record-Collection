package io.github.peningtonj.recordcollection.ui.components.filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.filter.AlbumFilter

@Composable
fun TextSearchBar(
    currentFilter: AlbumFilter,
    options : Map<String, List<String>>,
    onFilterChange: (AlbumFilter) -> Unit,
    maxDropdownHeight: androidx.compose.ui.unit.Dp = 200.dp,
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val filteredOptions = remember(searchText, options) {
        options.mapValues { (category, options) ->
            options.filter { option ->
                restrictedContains(option, searchText)
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                isDropdownExpanded = it.isNotEmpty() && isFocused
            },
            label = { Text("Search artists & genres") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    isDropdownExpanded = focusState.isFocused && searchText.isNotEmpty()
                },
        )

        if (isDropdownExpanded && filteredOptions.isNotEmpty()) {
            val density = LocalDensity.current
            androidx.compose.ui.window.Popup(
                alignment = Alignment.TopStart,
                offset = with(density) { IntOffset(0, 60.dp.roundToPx()) },
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                ),
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .heightIn(max = maxDropdownHeight)
                        .shadow(8.dp, RoundedCornerShape(12.dp))
                        .zIndex(1000f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {

                    FilterDropdown(
                        filteredOptions = filteredOptions,
                        onSelectionChange = { category, option ->
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

                            searchText = ""
                            isDropdownExpanded = false
                            onFilterChange(currentFilter.copy(tags = newTags))
                            Napier.d { "New filter tags: ${newTags}" }
                            focusManager.clearFocus()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FilterDropdown(
    filteredOptions: Map<String, List<String>>,
    onSelectionChange: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        filteredOptions.map { (category, options) ->
            if (options.isNotEmpty()) {
                item {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                items(options.take(2)) { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Napier.d { "Clicked on item $option" }
                                onSelectionChange(category, option)
                            }
                            .padding(vertical = 6.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
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