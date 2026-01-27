package io.github.peningtonj.recordcollection.util

import io.ktor.client.plugins.*
import kotlinx.coroutines.CancellationException

/**
 * Maps exceptions to user-friendly error messages
 */
object ErrorMessageMapper {
    
    /**
     * Error categories for UI handling
     */
    enum class ErrorCategory {
        NETWORK,
        AUTHENTICATION,
        NOT_FOUND,
        RATE_LIMIT,
        PERMISSION_DENIED,
        VALIDATION,
        SERVER_ERROR,
        UNKNOWN
    }
    
    data class UserFriendlyError(
        val message: String,
        val category: ErrorCategory,
        val canRetry: Boolean = true,
        val technicalDetails: String? = null
    )
    
    /**
     * Convert a Throwable to a user-friendly error message
     */
    fun toUserFriendlyError(throwable: Throwable): UserFriendlyError {
        return when (throwable) {
            // Network errors - connection issues
            is HttpRequestTimeoutException -> UserFriendlyError(
                message = "Request took too long. Please try again.",
                category = ErrorCategory.NETWORK,
                canRetry = true,
                technicalDetails = throwable.message
            )
            
            // Ktor client errors
            is ClientRequestException -> {
                when (throwable.response.status.value) {
                    401 -> UserFriendlyError(
                        message = "Authentication failed. Please log in again.",
                        category = ErrorCategory.AUTHENTICATION,
                        canRetry = false,
                        technicalDetails = "HTTP 401: ${throwable.message}"
                    )
                    403 -> UserFriendlyError(
                        message = "Access denied. You don't have permission to perform this action.",
                        category = ErrorCategory.PERMISSION_DENIED,
                        canRetry = false,
                        technicalDetails = "HTTP 403: ${throwable.message}"
                    )
                    404 -> UserFriendlyError(
                        message = "The requested item was not found.",
                        category = ErrorCategory.NOT_FOUND,
                        canRetry = false,
                        technicalDetails = "HTTP 404: ${throwable.message}"
                    )
                    429 -> UserFriendlyError(
                        message = "Too many requests. Please wait a moment and try again.",
                        category = ErrorCategory.RATE_LIMIT,
                        canRetry = true,
                        technicalDetails = "HTTP 429: ${throwable.message}"
                    )
                    else -> UserFriendlyError(
                        message = "Something went wrong. Please try again.",
                        category = ErrorCategory.UNKNOWN,
                        canRetry = true,
                        technicalDetails = "HTTP ${throwable.response.status.value}: ${throwable.message}"
                    )
                }
            }
            
            is ServerResponseException -> UserFriendlyError(
                message = "The server encountered an error. Please try again later.",
                category = ErrorCategory.SERVER_ERROR,
                canRetry = true,
                technicalDetails = "HTTP ${throwable.response.status.value}: ${throwable.message}"
            )
            
            // Domain exceptions
            is DomainException.AlbumNotFoundException -> UserFriendlyError(
                message = "Album not found in your library.",
                category = ErrorCategory.NOT_FOUND,
                canRetry = false,
                technicalDetails = throwable.message
            )
            
            is DomainException.ArtistNotFoundException -> UserFriendlyError(
                message = "Artist not found.",
                category = ErrorCategory.NOT_FOUND,
                canRetry = false,
                technicalDetails = throwable.message
            )
            
            is DomainException.AuthenticationException -> UserFriendlyError(
                message = "Authentication failed. Please log in again.",
                category = ErrorCategory.AUTHENTICATION,
                canRetry = false,
                technicalDetails = throwable.message
            )
            
            is DomainException.NetworkException -> UserFriendlyError(
                message = "Network connection issue. Please check your internet connection.",
                category = ErrorCategory.NETWORK,
                canRetry = true,
                technicalDetails = throwable.message
            )
            
            is DomainException.RateLimitException -> UserFriendlyError(
                message = "Too many requests. Please wait a moment and try again.",
                category = ErrorCategory.RATE_LIMIT,
                canRetry = true,
                technicalDetails = throwable.message
            )
            
            is DomainException -> UserFriendlyError(
                message = throwable.message ?: "An error occurred",
                category = ErrorCategory.UNKNOWN,
                canRetry = throwable.isRetryable(),
                technicalDetails = throwable.message
            )
            
            // Validation errors
            is IllegalArgumentException -> UserFriendlyError(
                message = throwable.message ?: "Invalid input provided.",
                category = ErrorCategory.VALIDATION,
                canRetry = false,
                technicalDetails = throwable.message
            )
            
            // Cancellation - don't show as error
            is CancellationException -> UserFriendlyError(
                message = "Operation cancelled",
                category = ErrorCategory.UNKNOWN,
                canRetry = false,
                technicalDetails = throwable.message
            )
            
            // Generic fallback
            else -> UserFriendlyError(
                message = "An unexpected error occurred. Please try again.",
                category = ErrorCategory.UNKNOWN,
                canRetry = true,
                technicalDetails = "${throwable.javaClass.simpleName}: ${throwable.message}"
            )
        }
    }
    
    /**
     * Get a short message suitable for toasts/snackbars
     */
    fun getShortMessage(throwable: Throwable): String {
        return toUserFriendlyError(throwable).message
    }
    
    /**
     * Check if an error is retryable
     */
    fun isRetryable(throwable: Throwable): Boolean {
        return toUserFriendlyError(throwable).canRetry
    }
    
    /**
     * Get error category
     */
    fun getErrorCategory(throwable: Throwable): ErrorCategory {
        return toUserFriendlyError(throwable).category
    }
    
    /**
     * Create a detailed error message for logging
     */
    fun toDetailedMessage(throwable: Throwable): String {
        val error = toUserFriendlyError(throwable)
        return buildString {
            append("Category: ${error.category}")
            append(" | Message: ${error.message}")
            error.technicalDetails?.let { 
                append(" | Details: $it")
            }
            append(" | Retryable: ${error.canRetry}")
        }
    }
}
