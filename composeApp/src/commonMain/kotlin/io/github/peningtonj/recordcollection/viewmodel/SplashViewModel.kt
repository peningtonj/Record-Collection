package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peningtonj.recordcollection.db.Profile
import io.github.peningtonj.recordcollection.di.DependencyContainer
import io.github.peningtonj.recordcollection.network.common.util.HttpClientProvider
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.network.spotify.model.SpotifyProfile
import io.github.peningtonj.recordcollection.util.AppError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val dependencyContainer: DependencyContainer
) : ViewModel() {
    private val _profileState = MutableStateFlow<ProfileScreenState>(ProfileScreenState())
    val profileState = _profileState.asStateFlow()
    
    val authState = dependencyContainer.authRepository.authState

    private val spotifyApi = SpotifyApi(
        client = HttpClientProvider.create(),
        authRepository = dependencyContainer.authRepository
    )

    init {
        loadProfile()
    }

    fun startAuth() {
        viewModelScope.launch {
            dependencyContainer.authRepository.authenticate()
                .onSuccess { 
                    loadProfile()
                }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = _profileState.value.copy(isLoading = true)
            
            try {
                val dbProfile = dependencyContainer.profileRepository.getProfile()
                val apiProfile = spotifyApi.user.getCurrentUserProfile().getOrNull()
                
                _profileState.value = _profileState.value.copy(
                    isLoading = false,
                    dbProfile = dbProfile,
                    apiProfile = apiProfile
                )
                
                apiProfile?.let { 
                    dependencyContainer.profileRepository.saveProfile(it)
                }
            } catch (e: Exception) {
                _profileState.value = _profileState.value.copy(
                    isLoading = false,
                    error = AppError.ProfileError(e.message ?: "Failed to load profile")
                )
            }
        }
    }
}

data class ProfileScreenState(
    val dbProfile: Profile? = null,
    val apiProfile: SpotifyProfile? = null,
    val isLoading: Boolean = false,
    val error: AppError? = null
)