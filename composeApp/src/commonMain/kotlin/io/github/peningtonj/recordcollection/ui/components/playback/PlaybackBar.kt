package io.github.peningtonj.recordcollection.ui.components.playback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.navigation.Screen
import io.github.peningtonj.recordcollection.viewmodel.PlaybackViewModel
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

@Composable
fun PlaybackBar(
    playbackViewModel: PlaybackViewModel,
) {

    val playback by playbackViewModel.playbackState.collectAsState()
    val isLoading by playbackViewModel.isLoading.collectAsState()
    val error by playbackViewModel.error.collectAsState()
    val session by playbackViewModel.currentSession.collectAsState()
    val navigator = LocalNavigator.current

    var interpolatedProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(playback) {
        val currentPlayback = playback // Capture the value to avoid smart cast issues
        if (currentPlayback?.isPlaying == true && currentPlayback.progressMs != null && currentPlayback.track.durationMs > 0) {
            while (true) {
                val latestPlayback = playback // Re-check current state
                if (latestPlayback?.isPlaying != true) break

                val currentTime = Clock.System.now().toEpochMilliseconds()
                val timeSinceLastUpdate = currentTime - latestPlayback.lastUpdated
                val estimatedProgress = latestPlayback.progressMs!! + timeSinceLastUpdate

                interpolatedProgress = (estimatedProgress.toFloat() / latestPlayback.track.durationMs.toFloat())
                    .coerceIn(0f, 1f)

                delay(100) // Update every 100 ms for smooth progress
            }
        } else if (currentPlayback?.progressMs != null && currentPlayback.track.durationMs > 0) {
            // Use actual progress when paused
            interpolatedProgress = (currentPlayback.progressMs.toFloat() / currentPlayback.track.durationMs.toFloat())
                .coerceIn(0f, 1f)
        }
    }
    
    // Handle error display if needed
    LaunchedEffect(error) {
        error?.let {
            // Show snackbar or toast
            playbackViewModel.clearError()
        }
    }

    // Capture the current playback state in a local variable
    val currentPlayback = playback
    val currentSession = session

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Progress bar
            LinearProgressIndicator(
                progress = { interpolatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = if (currentPlayback != null) {
                    ProgressIndicatorDefaults.linearColor
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
                trackColor = ProgressIndicatorDefaults.linearTrackColor,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )

            Spacer(modifier = Modifier.height(8.dp))
            
            // Track info and controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art and track information
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album art - now clickable
                    Card(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(enabled = currentPlayback?.track?.album?.id != null) {
                                currentPlayback?.track?.album?.id?.let { albumId ->
                                    navigator.navigateTo(Screen.Album(albumId))
                                }
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        if (currentPlayback?.track?.album?.images?.isNotEmpty() == true) {
                            AsyncImage(
                                model = currentPlayback.track.album.images.first().url,
                                contentDescription = "Album art for ${currentPlayback.track.album.name}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Fallback icon when no album art is available
                            Icon(
                                imageVector = Icons.Default.Album,
                                contentDescription = "No album art",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Track information
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = currentPlayback?.track?.name ?: "No track playing",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (currentPlayback != null) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = currentPlayback?.track?.artists?.joinToString(", ") { it.name } 
                                ?: "Select a song to start playing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentPlayback?.track?.album?.name ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentSession?.playingFrom != null) {
                            Text(
                                text = "Playing From: ${currentSession.playingFrom.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Playback controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { playbackViewModel.previous() },
                        modifier = Modifier.size(40.dp),
                        enabled = !isLoading && currentPlayback != null
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (currentPlayback != null) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            }
                        )
                    }
                    
                    IconButton(
                        onClick = { playbackViewModel.togglePlayPause() },
                        modifier = Modifier.size(48.dp),
                        enabled = !isLoading && currentPlayback != null
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (currentPlayback?.isPlaying == true) {
                                    Icons.Default.Pause
                                } else {
                                    Icons.Default.PlayArrow
                                },
                                contentDescription = if (currentPlayback?.isPlaying == true) "Pause" else "Play",
                                modifier = Modifier.size(32.dp),
                                tint = if (currentPlayback != null) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                }
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = {
                            Napier.d("Next button clicked")
                            playbackViewModel.next()
                        },
                        modifier = Modifier.size(40.dp),
                        enabled = !isLoading && currentPlayback != null
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = if (currentPlayback != null) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            }
                        )
                    }

                    if (currentSession?.playingFrom != null) {
                        IconButton(
                            onClick = { playbackViewModel.skipToNextAlbumInQueue() },
                            modifier = Modifier.size(40.dp),
                            enabled = !isLoading && currentPlayback != null
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueuePlayNext,
                                contentDescription = "Next",
                                tint = if (currentPlayback != null) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}