package io.github.peningtonj.recordcollection.ui.components.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.db.domain.filter.AlbumFilter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollectionFilterChips(
    collections: List<AlbumCollection>,
    currentFilter: AlbumFilter,
    onFilterChange: (AlbumFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val maxCollapsedItems = 4
    
    if (collections.isEmpty()) return
    
    val displayedCollections = if (isExpanded) collections else collections.take(maxCollapsedItems)
    val hasMore = collections.size > maxCollapsedItems

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Collection label
        Text(
            text = "Collections:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(end = 4.dp)
        )

        // Collection chips
        // TODO: Implement collection filtering in AlbumFilter
        // collections.forEach { collection ->
        //     val isSelected = false
        //     
        //     FilterChip(
        //         onClick = {
        //             // Collection filtering not yet implemented
        //         },
        //         label = { Text(collection.name) },
        //         selected = isSelected
        //     )
        // }

        // Expand/collapse button
        if (hasMore) {
            IconButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Show less" else "Show more"
                )
            }
        }
    }
}