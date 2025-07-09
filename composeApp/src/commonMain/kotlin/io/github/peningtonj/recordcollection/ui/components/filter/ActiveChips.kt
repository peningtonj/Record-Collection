package io.github.peningtonj.recordcollection.ui.components.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.filter.AlbumFilter
import io.github.peningtonj.recordcollection.db.domain.filter.DateRange
import io.github.peningtonj.recordcollection.db.domain.filter.toLabel
import kotlin.collections.component1
import kotlin.collections.component2


@Composable
fun ActiveChips(
    currentFilter: AlbumFilter,
    onFilterChange: (AlbumFilter) -> Unit
) {

    val allTags = remember(currentFilter.tags) {
        currentFilter.tags.flatMap { (category, tags) ->
            tags.map { tag -> category to tag }
        }
    }
    val yearFilter = currentFilter.releaseDateRange

    Napier.d { "Displaying ${allTags.size} tags" }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { ClearAllChip(onFilterChange) }
        items(allTags) { (category, tag) ->
            FilterChip(
                onClick = {
                    // Create completely new collections to ensure state change detection
                    val newTags = currentFilter.tags.mapValues { (_, tagList) ->
                        tagList.toList() // Create new immutable list
                    }.toMutableMap()

                    // Remove the tag from the appropriate category
                    newTags[category]?.let { tagList ->
                        val updatedList = tagList.toMutableList().apply { remove(tag) }
                        if (updatedList.isEmpty()) {
                            newTags.remove(category)
                        } else {
                            newTags[category] = updatedList.toList() // Convert back to immutable list
                        }
                    }

                    Napier.d { "Removing $tag from $category" }
                    onFilterChange(currentFilter.copy(tags = newTags))
                },
                label = { Text("$category: $tag") },
                selected = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove filter",
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
        yearFilter?.let { filter ->
            item{ReleaseYearChip(filter, onFilterChange, currentFilter)}
        }
    }
}

@Composable
fun ReleaseYearChip(
    releaseYearFilter: DateRange,
    onFilterChange: (AlbumFilter) -> Unit,
    currentFilter: AlbumFilter
) {
    FilterChip(
        onClick = {
            onFilterChange(currentFilter.copy(releaseDateRange = null))
        },
        label = { Text(releaseYearFilter.toLabel()) },
        selected = true,
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove filter",
                modifier = Modifier.size(16.dp)
            )
        }

    )
}

@Composable
fun ClearAllChip(
    onFilterChange: (AlbumFilter) -> Unit,
) {
    FilterChip(
        onClick = { onFilterChange(AlbumFilter()) },
        label = { Text("Clear All") },
        selected = false,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    )
}

