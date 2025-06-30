package io.github.peningtonj.recordcollection.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peningtonj.recordcollection.di.DependencyContainer
import io.github.peningtonj.recordcollection.navigation.LocalDependencyContainer
import io.github.peningtonj.recordcollection.repository.BaseSpotifyAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: BaseSpotifyAuthRepository
) : ViewModel() {
    private val _isAuthenticated = MutableStateFlow<Boolean?>(null) // null means "checking"
    val isAuthenticated = _isAuthenticated.asStateFlow()

    init {
        checkAuthState()
    }

    fun checkAuthState() {
        viewModelScope.launch {
            _isAuthenticated.value = authRepository.getAccessToken() != null
        }
    }
}

@Composable
fun rememberAuthViewModel(
    dependencies: DependencyContainer = LocalDependencyContainer.current
) = remember(dependencies) {
    AuthViewModel(dependencies.authRepository)
}