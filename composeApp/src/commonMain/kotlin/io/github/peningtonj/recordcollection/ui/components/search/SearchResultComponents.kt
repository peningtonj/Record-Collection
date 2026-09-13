package io.github.peningtonj.recordcollection.ui.components.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.AlbumType
import io.github.peningtonj.recordcollection.db.domain.Artist
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.navigation.Screen

@Composable
fun AlbumSearchItem(
    album: Album,
    modifier: Modifier = Modifier
) {
    val navigator = LocalNavigator.current
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = {
            navigator.navigateTo(Screen.Album(album.id, album.spotifyId))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = album.images.firstOrNull()?.url,
                contentDescription = "Album cover for ${album.name}",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album.artists.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Uses the space that used to sit empty to the right of the text — also
            // helps tell apart same-named albums/reissues and flags singles/EPs.
            val typeLabel = when (album.albumType) {
                AlbumType.EP -> "EP"
                AlbumType.SINGLE -> "Single"
                AlbumType.COMPILATION -> "Compilation"
                AlbumType.ALBUM -> null
            }
            Text(
                text = listOfNotNull(album.releaseDate.year.toString(), typeLabel).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ArtistSearchItem(
    artist: Artist,
    modifier: Modifier = Modifier
) {
    val navigator = LocalNavigator.current
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = {
            navigator.navigateTo(Screen.Artist(artist.id))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = artist.images.firstOrNull()?.url,
                contentDescription = "Artist image for ${artist.name}",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            )
            // Uses the space that used to sit empty to the right of the name.
            artist.genres.firstOrNull()?.let { genre ->
                Text(
                    text = genre.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
