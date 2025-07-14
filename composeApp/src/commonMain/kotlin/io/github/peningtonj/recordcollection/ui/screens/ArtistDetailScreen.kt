import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.peningtonj.recordcollection.db.domain.Artist
import io.github.peningtonj.recordcollection.db.domain.AlbumType
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.navigation.Screen
import io.github.peningtonj.recordcollection.ui.components.album.AlbumGrid
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import io.github.peningtonj.recordcollection.viewmodel.PlaybackViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberArtistDetailViewModel

@Composable
fun ArtistDetailScreen(
    artistId: String,
    playbackViewModel: PlaybackViewModel,
    viewModel: ArtistDetailViewModel = rememberArtistDetailViewModel(artistId = artistId),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                ArtistDetailContent(
                    artist = uiState.artist,
                    albums = uiState.albums,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun ArtistDetailContent(
    artist: Artist?,
    albums: List<AlbumDetailUiState>,
    modifier: Modifier = Modifier
) {
    val navigator = LocalNavigator.current
    
    // Group albums by type
    val albumsByType = albums.groupBy { it.album.albumType }
    
    // Define preferred order and filter out empty types
    val preferredOrder = listOf(
        AlbumType.ALBUM,
        AlbumType.SINGLE,
        AlbumType.COMPILATION
    )
    
    val availableAlbumTypes = preferredOrder.filter { type ->
        albumsByType[type]?.isNotEmpty() == true
    }
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    Column(modifier = modifier) {
        // Artist Header
        ArtistHeader(
            artist = artist,
            modifier = Modifier.padding(16.dp)
        )
        
        // Tabs for album types
        if (availableAlbumTypes.isNotEmpty()) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                availableAlbumTypes.forEachIndexed { index, albumType ->
                    val albumsOfType = albumsByType[albumType] ?: emptyList()
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = "${getAlbumTypeDisplayName(albumType)} (${albumsOfType.size})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                }
            }
            
            // Content for selected tab
            if (selectedTabIndex < availableAlbumTypes.size) {
                val selectedAlbumType = availableAlbumTypes[selectedTabIndex]
                val selectedAlbums = albumsByType[selectedAlbumType] ?: emptyList()
                
                AlbumGrid(
                    albums = selectedAlbums,
                    onAlbumClick = { album ->
                        navigator.navigateTo(Screen.Album(album.album.id))
                    },
                    onPlayClick = { album -> },
                    onRatingChange = { album, rating -> },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // No albums found
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No albums found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getAlbumTypeDisplayName(albumType: AlbumType): String {
    return when (albumType) {
        AlbumType.ALBUM -> "Albums"
        AlbumType.SINGLE -> "Singles"
        AlbumType.COMPILATION -> "Compilations"
        else -> albumType.name.lowercase().replaceFirstChar { it.titlecase() } + "s"
    }
}

@Composable
private fun ArtistHeader(
    artist: Artist?,
    modifier: Modifier = Modifier
) {
    artist?.let {
        Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Artist Image
                    AsyncImage(
                        model = artist.images.firstOrNull()?.url,
                        contentDescription = "Artist image",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )

                    // Artist Info
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "${artist.followers} followers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = "Popularity: ${artist.popularity}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Genres
                if (artist.genres.isNotEmpty()) {
                    Text(
                        text = "Genres",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(artist.genres) { genre ->
                            FilterChip(
                                onClick = { /* Handle genre click */ },
                                label = { Text(genre) },
                                selected = false
                            )
                        }
                    }
                }
            }
        }
    }
}