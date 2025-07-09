package io.github.peningtonj.recordcollection.ui.components.album

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.peningtonj.recordcollection.ui.components.rating.StarRating
import io.github.peningtonj.recordcollection.ui.models.AlbumDisplayData
import io.github.peningtonj.recordcollection.ui.models.formattedTotalDuration

@Composable
fun AlbumHeader(
    albumDisplayData: AlbumDisplayData,
    onPlayClick: () -> Unit,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Album Cover Image
        AsyncImage(
            model = albumDisplayData.album.images.firstOrNull()?.url,
            contentDescription = "Album cover for ${albumDisplayData.album.name}",
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
                    text = albumDisplayData.album.name,
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.width(24.dp))  // Adds fixed space between text and button

                PlayButton(
                    onPlayClick = onPlayClick,
                    modifier = Modifier
                        .padding(top = 4.dp)
                )
            }

            // Artist Name
            Text(
                text = albumDisplayData.album.primaryArtist,
                style = MaterialTheme.typography.titleMedium
            )

            // Release Year and Track Count
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = albumDisplayData.album.releaseDate.year.toString(),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${albumDisplayData.album.totalTracks} tracks",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formattedTotalDuration(albumDisplayData.totalDuration),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            StarRating(
                albumDisplayData.rating,
                onRatingChange = onRatingChange,
            )
        }
    }
}