package io.github.peningtonj.recordcollection.ui.components.playback

import androidx.compose.runtime.Composable
import io.github.peningtonj.recordcollection.viewmodel.PlaybackViewModel

data class PlaybackActions(
    val togglePlayPause: () -> Unit
)

@Composable
fun rememberPlaybackActions(
    playbackViewModel: PlaybackViewModel
) : PlaybackActions {
    return PlaybackActions(
        togglePlayPause = { playbackViewModel.togglePlayPause() }
    )
}