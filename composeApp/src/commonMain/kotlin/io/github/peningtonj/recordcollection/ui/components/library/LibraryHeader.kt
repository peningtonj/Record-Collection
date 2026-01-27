package io.github.peningtonj.recordcollection.ui.components.library

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.db.domain.filter.AlbumFilter
import io.github.peningtonj.recordcollection.db.domain.filter.DateRange
import io.github.peningtonj.recordcollection.service.SyncAction
import io.github.peningtonj.recordcollection.ui.components.filter.ActiveChips
import io.github.peningtonj.recordcollection.ui.components.filter.CollectionFilterChips
import io.github.peningtonj.recordcollection.ui.components.filter.HorizontalFilterBar
import io.github.peningtonj.recordcollection.ui.components.filter.TextSearchBar
import io.github.peningtonj.recordcollection.viewmodel.SyncState
import kotlinx.datetime.LocalDate

@Composable
fun LibraryHeader(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    syncState: SyncState,
    trackSyncState: SyncState,
    currentFilter: AlbumFilter,
    filterOptions: Map<String, MutableList<String>>,
    collections: List<AlbumCollection>,
    startYear: Int,
    onStartSync: () -> Unit,
    onStartTrackSync: () -> Unit,
    onLaunchSync: (SyncAction, Boolean) -> Unit,
    onFilterChange: (AlbumFilter) -> Unit,
    onPlayRandomAlbum: () -> Unit,
    onCreateCollection: (String) -> Unit,
    albumCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .animateContentSize(
                    animationSpec = tween(durationMillis = 300)
                )
        ) {
            // Collapsed header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isExpanded) {
                        Text(
                            text = "$albumCount albums",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    val rotationAngle by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = tween(durationMillis = 300),
                        label = "arrow_rotation"
                    )

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.rotate(rotationAngle)
                    )
                }
            }

            // Expanded content
            if (isExpanded) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sync buttons section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            SyncLibraryUi(
                                onClick = onStartSync,
                                syncState = syncState,
                                launchSync = onLaunchSync
                            )

                            SyncLibraryButton(
                                onClick = onStartTrackSync,
                                label = "Sync Tracks",
                                syncState = trackSyncState
                            )
                        }
                    }

                    // Search and filter section
                    TextSearchBar(
                        currentFilter,
                        options = filterOptions,
                        onFilterChange = onFilterChange,
                    )

                    HorizontalFilterBar(
                        currentFilter = currentFilter,
                        onRatingChange = { newRating ->
                            onFilterChange(currentFilter.copy(minRating = newRating))
                        },
                        onYearFilterChange = { start, end, label ->
                            val newYearRange = DateRange(
                                LocalDate(start, 1, 1),
                                LocalDate(end, 12, 31),
                                name = label
                            )
                            onFilterChange(currentFilter.copy(releaseDateRange = newYearRange))
                        },
                        startYear = startYear
                    )

                    // Collection filter chips - NEW
                    CollectionFilterChips(
                        collections = collections,
                        currentFilter = currentFilter,
                        onFilterChange = onFilterChange
                    )

                    // Active filter chips
                    if (currentFilter.tags.isNotEmpty() || 
                        currentFilter.releaseDateRange != null) {
                        ActiveChips(
                            currentFilter = currentFilter,
                            onFilterChange = onFilterChange,
                        )
                    }

                    // Action buttons row
                    Row {
                        AssistChip(
                            onClick = onPlayRandomAlbum,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Random Album"
                                )
                            },
                            label = { Text("Play Random Album From Filtered Albums") }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        CreateCollectionButton(
                            onCreateCollection = onCreateCollection
                        )
                    }

                    // Album count
                    Text(
                        "$albumCount albums",
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}