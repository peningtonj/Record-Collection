package io.github.peningtonj.recordcollection.ui.components.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.peningtonj.recordcollection.service.SyncAction
import io.github.peningtonj.recordcollection.viewmodel.LibraryDifferences
import io.github.peningtonj.recordcollection.viewmodel.SyncState

@Composable
fun SyncLibraryUi(
    onClick: () -> Unit,
    syncState: SyncState,
    launchSync: (SyncAction, Boolean) -> Unit
) {
    var alertVisible by remember { mutableStateOf(false) }

    SyncLibraryButton(
        onClick = {
            alertVisible = true
            onClick()
        },
        syncState
    )

    if (alertVisible) {
        SyncLibraryDialog(
            syncState = syncState,
            onDismiss = {
                alertVisible = false
            },
            onSyncAction = { syncAction, removeDuplicates ->
                launchSync(syncAction, removeDuplicates)
            },
        )
    }
}

@Composable
fun SyncLibraryButton(
    onClick: () -> Unit,
    syncState: SyncState
) {
    AssistChip(
        onClick =  onClick,
        trailingIcon = {
            when (syncState) {
                SyncState.Syncing -> {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                    )
                }

                else -> {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                    )

                }
            }
        },
        label = { Text("Sync with Spotify Saved Albums") },
    )
}
@Composable
fun SyncLibraryDialog(
    syncState: SyncState,
    onDismiss: () -> Unit,
    onSyncAction: (SyncAction, Boolean) -> Unit,
) {
    var removeDuplicates by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {

            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = "Sync Library",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                when (syncState) {
                    SyncState.Idle -> {
                        Text(
                            text = "Ready to sync your library",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    SyncState.Syncing -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Analyzing differences...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    is SyncState.Error -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = syncState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    is SyncState.Ready -> {
                        LibraryDifferencesCard(
                            differences = syncState.differences,
                        )



                        SyncOptionsSection(
                            onSyncAction = { syncAction ->
                                onSyncAction(syncAction, removeDuplicates)
                            },
                            differences = syncState.differences,
                            onDismiss = onDismiss,
                        )

                        // Show duplicate removal option at the bottom
                        val totalDuplicates = syncState.differences.localDuplicates.size + syncState.differences.userSavedAlbumsDuplicates.size
                        if (totalDuplicates > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Remove $totalDuplicates duplicate${if (totalDuplicates > 1) "s" else ""}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Checkbox(
                                    checked = removeDuplicates,
                                    onCheckedChange = {
                                        removeDuplicates = !removeDuplicates
                                    }
                                )
                            }

                            if (removeDuplicates) {
                                Text(
                                    text = "Most recently added albums will be kept",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 24.dp)
                                )
                            }
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }


        }
    }
}



@Composable
private fun LibraryDifferencesCard(differences: LibraryDifferences) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Library Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            DifferenceRow(
                icon = Icons.Default.LibraryMusic,
                label = "Local Library",
                count = differences.localCount,
                color = MaterialTheme.colorScheme.primary
            )

            DifferenceRow(
                icon = Icons.Default.CloudDownload,
                label = "Spotify Library",
                count = differences.spotifyCount,
                color = MaterialTheme.colorScheme.secondary
            )

            if (differences.onlyInLocal > 0) {
                DifferenceRow(
                    icon = Icons.Default.Remove,
                    label = "Only in Local",
                    count = differences.onlyInLocal,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            if (differences.onlyInSpotify > 0) {
                DifferenceRow(
                    icon = Icons.Default.Add,
                    label = "Only in Spotify",
                    count = differences.onlyInSpotify,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            if (differences.inBoth > 0) {
                DifferenceRow(
                    icon = Icons.Default.Check,
                    label = "In Both",
                    count = differences.inBoth,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Show duplicates if any exist
            val totalDuplicates = differences.localDuplicates.size + differences.userSavedAlbumsDuplicates.size
            if (totalDuplicates > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                if (differences.localDuplicates.isNotEmpty()) {
                    DifferenceRow(
                        icon = Icons.Default.ContentCopy,
                        label = "Local Duplicates",
                        count = differences.localDuplicates.size,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (differences.userSavedAlbumsDuplicates.isNotEmpty()) {
                    DifferenceRow(
                        icon = Icons.Default.ContentCopy,
                        label = "Spotify Duplicates",
                        count = differences.userSavedAlbumsDuplicates.size,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun DifferenceRow(
    icon: ImageVector,
    label: String,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun SyncOptionsSection(
    onSyncAction: (SyncAction) -> Unit,
    differences: LibraryDifferences,
    onDismiss: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Sync Options",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        SyncOptionButton(
            icon = Icons.Default.Merge,
            title = "Combine Both",
            subtitle = "Keep all albums from both libraries",
            resultCount = differences.localCount + differences.onlyInSpotify,
            onClick = {
                onSyncAction(SyncAction.Combine)
                onDismiss()
            }
        )

        SyncOptionButton(
            icon = Icons.Default.CloudDownload,
            title = "Use Spotify Only",
            subtitle = "Replace local library with Spotify",
            resultCount = differences.spotifyCount,
            onClick = {
                onSyncAction(SyncAction.UseSpotify)
                onDismiss()
            }
        )

        SyncOptionButton(
            icon = Icons.Default.LibraryMusic,
            title = "Keep Local Only",
            subtitle = "Keep current local library",
            resultCount = differences.localCount,
            onClick = {
                onSyncAction(SyncAction.UseLocal)
                onDismiss()
            }
        )

        if (differences.inBoth > 0) {
            SyncOptionButton(
                icon = Icons.Default.Add,
                title = "Common Albums Only",
                subtitle = "Keep only albums in both libraries",
                resultCount = differences.inBoth,
                onClick = {
                    onSyncAction(SyncAction.Intersection)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun SyncOptionButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    resultCount: Int,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "$resultCount albums",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
