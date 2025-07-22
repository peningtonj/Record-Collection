package io.github.peningtonj.recordcollection.ui.components.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.peningtonj.recordcollection.repository.OnAddToCollection
import io.github.peningtonj.recordcollection.ui.components.album.PlayButton
import io.github.peningtonj.recordcollection.ui.components.common.SettingsChipRow

@Composable
fun CollectionHeader(
    collectionName: String,
    albumCount: Int,
    onPlayAllClick: () -> Unit,
    onRandomClick: () -> Unit,
    isShuffled: Boolean,
    modifier: Modifier = Modifier,
    addToLibrarySetting: OnAddToCollection,
    onAddToLibrarySettingChange: (OnAddToCollection) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Collection content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = collectionName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayButton(
                            onPlayClick = onPlayAllClick,
                            modifier = Modifier
                                .padding(top = 4.dp)
                        )
                        IconButton(
                            onClick = onRandomClick
                        ) {
                            if (isShuffled) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Shuffle On",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle Off",
                                    tint = MaterialTheme.colorScheme.onSurface

                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    // Album Count
                    Text(
                        text = "$albumCount albums",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                }


                Column {
                    Text(
                        "Auto add albums added to this collection to your library.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SettingsChipRow(
                        options = OnAddToCollection.entries.toList(),
                        selected = addToLibrarySetting,
                        onOptionSelected = onAddToLibrarySettingChange,
                        labelMapper = { setting ->
                            setting.displayName
                        }
                    )
                }
            }
        }
    }
}