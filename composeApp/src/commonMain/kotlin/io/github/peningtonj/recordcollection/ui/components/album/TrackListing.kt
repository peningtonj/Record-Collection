package io.github.peningtonj.recordcollection.ui.components.album

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.peningtonj.recordcollection.db.domain.Track
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun TrackListing(
    tracks: List<Track>,
    onPlayClick: (Track) -> Unit,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: (Track) -> Boolean = { false },
) {
    // Group tracks by disc number
    val tracksByDisc = tracks.groupBy { it.discNumber }.toSortedMap()
    val hasMultipleDiscs = tracksByDisc.size > 1

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            TrackListingHeader()
        }
        
        tracksByDisc.forEach { (discNumber, discTracks) ->
            // Add disc divider for multi-disc albums
            if (hasMultipleDiscs) {
                item {
                    DiscDivider(discNumber = discNumber.toInt())
                }
            }
            
            items(
                items = discTracks,
                key = { it.id }
            ) { track ->
                TrackListingItem(
                    track = track,
                    onPlayClick = onPlayClick,
                    isPlaying = isPlaying,
                    onPauseClick = onPauseClick
                )
            }
        }
    }
}

@Composable
fun DiscDivider(
    discNumber: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
            thickness = 1.dp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Disc $discNumber",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
fun TrackListingHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track number header
        Text(
            text = "#",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp)
        )

        // Track title header
        Text(
            text = "Title",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        // Duration header
        Text(
            text = "Duration",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TrackListingItem(
    track: Track,
    modifier: Modifier = Modifier,
    isPlaying: (Track) -> Boolean = { false },
    onPlayClick: (Track) -> Unit = {},
    onPauseClick: () -> Unit = {}
) {

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .hoverable(interactionSource = interactionSource),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,

    ) {
        // Track number
        Box(
            modifier = Modifier.width(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying(track)) {
                IconButton(
                    onClick = { onPauseClick() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Play ${track.name}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            else if (isHovered) {
                IconButton(
                    onClick = { onPlayClick(track) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play ${track.name}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Text(
                    text = track.trackNumber.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Track title and artists
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (track.isExplicit) {
                    Text(
                        text = "E",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.extraSmall
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            if (track.artists.isNotEmpty()) {
                Text(
                    text = track.artists.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Duration
        Text(
            text = formatDuration(track.durationMs),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val duration = durationMs.milliseconds
    val minutes = duration.inWholeMinutes
    val seconds = duration.inWholeSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}