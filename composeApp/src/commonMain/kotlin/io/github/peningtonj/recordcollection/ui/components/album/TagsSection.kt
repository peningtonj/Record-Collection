package io.github.peningtonj.recordcollection.ui.components.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState

@Composable
fun TagsSection(
    albumDetailUiState: AlbumDetailUiState,
    removeTag: (String) -> Unit = {},
    addTag: () -> Unit = {},
) {
    var editMode by remember { mutableStateOf(false) }
    // Tags section
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy((-10).dp)
        ) {
            albumDetailUiState.tags.forEach { tagUiState ->
                FilterChip(
                    onClick = {
                        if (editMode) {
                            removeTag(tagUiState.tag.id)
                        }
                              },
                    shape = RoundedCornerShape(8.dp),
                    selected = tagUiState.isSelected,
                    leadingIcon = {
                        if (editMode) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove filter",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = tagUiState.displayName,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }

            if (editMode) {
                IconButton(
                    onClick = addTag,
                    modifier = Modifier
                        .size(32.dp)
                        .offset(y = 8.dp) // Adjust this value to align with chip centers

                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add tag",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Edit button as last item
            IconButton(
                onClick = {
                    editMode = !editMode
                },
                modifier = Modifier
                    .size(32.dp)
                    .offset(y = 8.dp) // Adjust this value to align with chip centers
            ) {
                if (editMode) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Edit tags",
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit tags",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
    }
}