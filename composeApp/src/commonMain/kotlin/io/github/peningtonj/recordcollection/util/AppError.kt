package io.github.peningtonj.recordcollection.util

sealed class AppError {
    data class NetworkError(val message: String) : AppError()
    data class AuthError(val message: String) : AppError()
    data class ProfileError(val message: String) : AppError()
    data class DatabaseError(val message: String) : AppError()
    data object UnknownError : AppError()
}
