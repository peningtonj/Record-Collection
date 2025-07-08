package io.github.peningtonj.recordcollection.ui.components.album

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

@Composable
fun PlayButton(
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
    onClick = onPlayClick,
    modifier = modifier
    .clip(CircleShape)
        .background(Color(0xFF90CAA7).copy(alpha = 0.9f))  // Option 2: Using a specific green
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = "Play album",
            tint = MaterialTheme.colorScheme.onTertiary,
            modifier = Modifier.fillMaxSize(.8f)  // Make icon fill the button
        )
    }
}