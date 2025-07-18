package io.github.peningtonj.recordcollection.ui.components.album

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.ui.components.playback.PlaybackActions
import io.github.peningtonj.recordcollection.ui.components.rating.StarRating
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import io.github.peningtonj.recordcollection.ui.models.formattedTotalDuration
import io.github.peningtonj.recordcollection.viewmodel.ReleaseGroupStatus

@Composable
fun AlbumHeader(
    albumDetailUiState: AlbumDetailUiState,
    releaseGroupStatus: ReleaseGroupStatus,
    collections: List<AlbumCollection>,
    albumActions: AlbumActions,
    playbackActions: PlaybackActions,
    showAddTagDialogClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: () -> Boolean = { false },
    onReleaseSelect: (String) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main album info row
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Album Cover Image
            AsyncImage(
                model = albumDetailUiState.album.images.firstOrNull()?.url,
                contentDescription = "Album cover for ${albumDetailUiState.album.name}",
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )

            // Text content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Album Title row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = albumDetailUiState.album.name,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.width(24.dp))  // Adds fixed space between text and button

                    if (isPlaying()) {
                        PauseButton(
                            onPauseClick = playbackActions.togglePlayPause,
                            modifier = Modifier
                                .padding(top = 4.dp)
                        )
                    } else {
                        PlayButton(
                            onPlayClick = { albumActions.play(albumDetailUiState) },
                            modifier = Modifier
                                .padding(top = 4.dp)
                        )

                    }
                }

                // Artist Name
                Text(
                    text = albumDetailUiState.album.primaryArtist,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable { albumActions.navigateToArtist(albumDetailUiState) },
                )

                // Release Year and Track Count
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = albumDetailUiState.album.releaseDate.year.toString(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${albumDetailUiState.album.totalTracks} tracks",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = formattedTotalDuration(albumDetailUiState.totalDuration),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                StarRating(
                    albumDetailUiState.rating?.rating ?: 0,
                    onRatingChange = { newRating -> albumActions.updateRating(albumDetailUiState, newRating) },
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ReleaseGroupUi(
                albumDetailUiState = albumDetailUiState,
                releaseGroupStatus = releaseGroupStatus,
                onReleaseSelect = onReleaseSelect,
                albumActions = albumActions
            )

            AddToLibraryButton(
                onClick = { albumActions.toggleLibraryStatus(albumDetailUiState) },
                inLibrary = albumDetailUiState.album.inLibrary
            )

            AddToCollectionButton(
                collections = collections,
                album = albumDetailUiState.album,
                onDismiss = {},
                addAlbumToCollection = { collectionName ->
                    albumActions.addToCollection(albumDetailUiState, collectionName)
                },
                createCollectionWithAlbum = { albumActions.addToNewCollection(albumDetailUiState) }
            )
        }

        TagsSection(albumDetailUiState,
            addTag = showAddTagDialogClick,
            removeTag = {tagId ->
                albumActions.removeTag(albumDetailUiState, tagId)
            }
        )

    }
}


@Composable
fun AddToLibraryButton(
    onClick: () -> Unit,
    inLibrary: Boolean
) {
    AssistChip(
        onClick = onClick,
        label = {
            if (inLibrary) {
                Text("Remove From Library")
            } else {
                Text("Add To Library")
            }
        },
        trailingIcon = {
            if (inLibrary) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                    )
                } else {
            Icon(
                Icons.Default.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
            }
        }
    )
}

@Composable
fun AddToCollectionButton(
    collections: List<AlbumCollection>,
    album: Album,
    addAlbumToCollection: (String) -> Unit,
    createCollectionWithAlbum: () -> Unit,
    onDismiss: () -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }

    Box {
        AssistChip(
            onClick = { showDropdown = true },
            label = { Text("Add to Collection") },
            trailingIcon = {
                Icon(
                    Icons.Default.LibraryMusic,
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                )
            }
        )

        CollectionDropdownContent(
            expanded = showDropdown,
            onDismissRequest = {
                showDropdown = false
                onDismiss()
            },
            collections = collections,
            album = album,
            addAlbumToCollection = addAlbumToCollection,
            createCollectionWithAlbum = createCollectionWithAlbum
        )
    }
}