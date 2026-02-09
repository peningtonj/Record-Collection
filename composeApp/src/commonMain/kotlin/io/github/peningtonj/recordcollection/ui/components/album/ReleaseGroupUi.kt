package io.github.peningtonj.recordcollection.ui.components.album

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import io.github.peningtonj.recordcollection.viewmodel.ReleaseGroupStatus

@Composable
fun ReleaseGroupUi(
    albumDetailUiState: AlbumDetailUiState,
    releaseGroupStatus: ReleaseGroupStatus,
    onReleaseSelect: (String, String) -> Unit,
    albumActions: AlbumActions
) {
    val releaseGroup = albumDetailUiState.releaseGroup
    val isLoading = releaseGroupStatus == ReleaseGroupStatus.Updating
    val shouldShowUpdateButton = releaseGroup.size <= 1 || isLoading

    if (shouldShowUpdateButton) {
        ReleaseUpdateButton(
            onUpdateClick = { albumActions.getReleaseGroup(albumDetailUiState) },
            releaseGroupStatus = releaseGroupStatus,
        )
    } else {
        ReleaseGroupSelector(
            releases = releaseGroup,
            onReleaseSelect = onReleaseSelect,
            onUpdateClick = { albumActions.getReleaseGroup(albumDetailUiState) }
        )
    }
}


@Composable
fun ReleaseGroupSelector(
    releases: List<Album>,
    onReleaseSelect: (String, String) -> Unit,
    onUpdateClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    AssistChip(
        onClick = { expanded = !expanded },
        label = { Text("Album Releases") },
        leadingIcon = {
            Icon(
                Icons.Default.Album,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        },
        trailingIcon = {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        }
    )
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false},
    ) {
        releases.map { release ->
            DropdownMenuItem(
                text = {
                    Text(release.name)
                },
                onClick = {
                    onReleaseSelect(release.id, release.spotifyId)
                    expanded = false
                }
            )
        }
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        DropdownMenuItem(
            text = { Text("Search For Other Releases") },
            onClick = onUpdateClick
        )
    }
}

@Composable
fun ReleaseUpdateButton(
    onUpdateClick: () -> Unit = {},
    releaseGroupStatus: ReleaseGroupStatus
) {
    AssistChip(
        onClick = onUpdateClick,
        label = { Text("Find Other Releases") },
        leadingIcon = {
            Icon(
                Icons.Default.Album,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        },
        trailingIcon = {
            when (releaseGroupStatus) {
                ReleaseGroupStatus.Idle -> {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                    )
                }
                ReleaseGroupStatus.Updating -> {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                    )
                }
            }
        }
    )
}