package io.github.peningtonj.recordcollection.ui.components.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.peningtonj.recordcollection.ui.components.album.PlayButton

@Composable
fun CollectionHeader(
    collectionName: String,
    albumCount: Int,
    onPlayAllClick: () -> Unit,
    onRandomClick: () -> Unit,
    modifier: Modifier = Modifier
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
            // Collection Title row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = collectionName,
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.width(24.dp))

                PlayButton(
                    onPlayClick = onPlayAllClick,
                    modifier = Modifier
                        .padding(top = 4.dp)
                )
                IconButton(
                    onClick = onRandomClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Random Album")
                }

            }

            // Album Count
            Text(
                text = "$albumCount albums",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}