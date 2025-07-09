package io.github.peningtonj.recordcollection.ui.components.rating

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun StarRating(
    rating: Int, // Rating out of 10 (0-10, where 5 = 2.5 stars)
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 24.dp,
    interactive: Boolean = true,
    halfStars : Boolean = true
) {
    var hoveredStarIndex by remember { mutableStateOf(-1) }
    val displayRating = if (hoveredStarIndex >= 0 && halfStars) {
        getPreviewRating(hoveredStarIndex, rating)
    } else {
        rating
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(5) { index ->
            val starValue = (index + 1) * 2 // Each star represents 2 points
            val currentStarRating = when {
                displayRating >= starValue -> StarState.FULL
                displayRating >= starValue - 1 -> StarState.HALF
                else -> StarState.EMPTY
            }
            
            StarIcon(
                state = currentStarRating,
                size = starSize,
                isHovered = hoveredStarIndex == index,
                onClick = if (interactive) {
                    { handleStarClick(index, rating, onRatingChange, halfStars) }
                } else null,
                onHoverChange = if (interactive) {
                    { isHovered -> 
                        hoveredStarIndex = if (isHovered) index else -1
                    }
                } else null
            )
        }
    }
}

@Composable
fun StarRatingDisplay(
    rating: Int,
    modifier: Modifier = Modifier,
    starSize: Dp = 20.dp,
) {
    StarRating(
        rating = rating,
        onRatingChange = { },
        modifier = modifier,
        starSize = starSize,
        interactive = false
    )
}

@Composable
private fun StarIcon(
    state: StarState,
    size: Dp,
    isHovered: Boolean = false,
    onClick: (() -> Unit)? = null,
    onHoverChange: ((Boolean) -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHoveredState by interactionSource.collectIsHoveredAsState()
    
    // Notify parent about hover state changes
    if (onHoverChange != null && isHoveredState != isHovered) {
        onHoverChange(isHoveredState)
    }
    
    val icon = when (state) {
        StarState.EMPTY -> Icons.Default.StarBorder
        StarState.HALF -> Icons.AutoMirrored.Default.StarHalf
        StarState.FULL -> Icons.Default.Star
    }
    
    val color = when (state) {
        StarState.EMPTY -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        StarState.HALF -> MaterialTheme.colorScheme.primary
        StarState.FULL -> MaterialTheme.colorScheme.primary
    }
    
    // Add slight transparency when hovering to show preview
    val finalColor = if (isHoveredState && onClick != null) {
        color.copy(alpha = 0.7f)
    } else {
        color
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = finalColor,
        modifier = Modifier
            .size(size)
            .then(
                if (onClick != null) {
                    Modifier
                        .hoverable(interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onClick() }
                } else {
                    Modifier
                }
            )
    )
}

private enum class StarState {
    EMPTY, HALF, FULL
}

private fun handleStarClick(
    starIndex: Int,
    currentRating: Int,
    onRatingChange: (Int) -> Unit,
    halfStars: Boolean = true
) {
    val fullStarValue = (starIndex + 1) * 2
    val halfStarValue = fullStarValue - 1
    var newRating = 0

    if (halfStars) {
        newRating = when (currentRating) {
            fullStarValue -> halfStarValue // Full star -> Half star
            else -> fullStarValue // Empty or different rating -> Full star
        }
    } else {
        newRating = when (currentRating) {
            fullStarValue -> 0
            else -> fullStarValue
        }
    }
    onRatingChange(newRating)
}

private fun getPreviewRating(hoveredStarIndex: Int, currentRating: Int): Int {
    val fullStarValue = (hoveredStarIndex + 1) * 2
    val halfStarValue = fullStarValue - 1
    
    return when (currentRating) {
        fullStarValue -> halfStarValue // Show half star preview
        else -> fullStarValue // Show full star preview
    }
}