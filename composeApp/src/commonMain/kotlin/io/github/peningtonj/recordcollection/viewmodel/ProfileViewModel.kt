package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peningtonj.recordcollection.db.Profile
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.network.spotify.model.SpotifyProfile
import io.github.peningtonj.recordcollection.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: ProfileRepository,
    private val spotifyApi: SpotifyApi
) : ViewModel() {
    private val _profile = MutableStateFlow<Profile?>(null)
    val profile = _profile.asStateFlow()

    suspend fun fetchAndSaveProfile(): Result<SpotifyProfile> {
        return spotifyApi.user.getCurrentUserProfile().also { result ->
            result.getOrNull()?.let { profile ->
                repository.saveProfile(profile)
            }
        }
    }

    init {
        // Load cached profile on init
        viewModelScope.launch {
            _profile.value = repository.getProfile()
        }
    }
}