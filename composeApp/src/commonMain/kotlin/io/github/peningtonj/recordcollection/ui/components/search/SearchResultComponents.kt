package io.github.peningtonj.recordcollection.ui.components.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Artist
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.navigation.Navigator
import io.github.peningtonj.recordcollection.navigation.Screen
import io.github.peningtonj.recordcollection.util.RankedAlbum
import io.github.peningtonj.recordcollection.util.RankedArtist

@Composable
fun AlbumSearchItem(
    album: RankedAlbum,
    modifier: Modifier = Modifier
) {
    val navigator = LocalNavigator.current
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = {
            navigator.navigateTo(Screen.Album(album.album.id))
        }
    ) {
        Row {
            AsyncImage(
                model = album.album.images.firstOrNull()?.url,
                contentDescription = "Album cover for ${album.album.name}",
                modifier = Modifier
                    .width(60.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = album.album.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = album.album.artists.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "Ranking score ${album.relevanceScore}"
            )

        }
    }
}

@Composable
fun ArtistSearchItem(
    artist: RankedArtist,
    modifier: Modifier = Modifier
) {
    val navigator = LocalNavigator.current
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = {
            navigator.navigateTo(Screen.Artist(artist.artist.id))
        }
    ) {
        Row {
            AsyncImage(
                model = artist.artist.images.firstOrNull()?.url,
                contentDescription = "Album image for ${artist.artist.name}",
                modifier = Modifier
                    .width(60.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = artist.artist.name,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                "Ranking score ${artist.relevanceScore}"
            )
        }
    }
}
