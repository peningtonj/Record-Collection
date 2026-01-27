package io.github.peningtonj.recordcollection.util

/**
 * Base exception class for domain-specific errors in the application.
 * Provides a type-safe way to handle different error scenarios.
 */
sealed class DomainException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    
    /**
     * Album-related exceptions
     */
    class AlbumNotFoundException(val albumId: String) : 
        DomainException("Album not found: $albumId")
    
    class AlbumFetchException(message: String, cause: Throwable? = null) : 
        DomainException("Failed to fetch album: $message", cause)
    
    /**
     * Artist-related exceptions
     */
    class ArtistNotFoundException(val artistId: String) : 
        DomainException("Artist not found: $artistId")
    
    class ArtistFetchException(message: String, cause: Throwable? = null) : 
        DomainException("Failed to fetch artist: $message", cause)
    
    /**
     * Collection-related exceptions
     */
    class CollectionNotFoundException(val collectionName: String) : 
        DomainException("Collection not found: $collectionName")
    
    class CollectionAlreadyExistsException(val collectionName: String) : 
        DomainException("Collection already exists: $collectionName")
    
    /**
     * Sync-related exceptions
     */
    class SyncException(message: String, cause: Throwable? = null) : 
        DomainException("Sync failed: $message", cause)
    
    class ConflictResolutionException(message: String) : 
        DomainException("Failed to resolve sync conflict: $message")
    
    /**
     * Network-related exceptions
     */
    class NetworkException(message: String, cause: Throwable? = null) : 
        DomainException("Network error: $message", cause)
    
    class RateLimitException(val retryAfter: Long? = null) : 
        DomainException("Rate limit exceeded${retryAfter?.let { ", retry after $it seconds" } ?: ""}")
    
    /**
     * Authentication exceptions
     */
    class AuthenticationException(message: String, cause: Throwable? = null) : 
        DomainException("Authentication failed: $message", cause)
    
    class TokenExpiredException : 
        DomainException("Authentication token has expired")
    
    /**
     * Validation exceptions
     */
    class ValidationException(val errors: List<String>) : 
        DomainException("Validation failed: ${errors.joinToString(", ")}")
    
    class InvalidDataException(message: String) : 
        DomainException("Invalid data: $message")
}

/**
 * Extension function to convert exceptions to user-friendly messages
 */
fun Throwable.toUserFriendlyMessage(): String {
    return when (this) {
        is DomainException.AlbumNotFoundException -> "Album not found. It may have been removed from Spotify."
        is DomainException.ArtistNotFoundException -> "Artist not found. They may have been removed from Spotify."
        is DomainException.CollectionNotFoundException -> "Collection not found."
        is DomainException.NetworkException -> "Network connection issue. Please check your internet connection."
        is DomainException.RateLimitException -> "Too many requests. Please try again in a moment."
        is DomainException.AuthenticationException -> "Authentication failed. Please sign in again."
        is DomainException.TokenExpiredException -> "Your session has expired. Please sign in again."
        is DomainException.ValidationException -> this.errors.joinToString("\n")
        is DomainException -> this.message ?: "An error occurred"
        else -> "An unexpected error occurred. Please try again."
    }
}

/**
 * Determine if an error is retryable
 */
fun Throwable.isRetryable(): Boolean {
    return when (this) {
        is DomainException.NetworkException -> true
        is DomainException.RateLimitException -> true
        is DomainException.SyncException -> true
        is DomainException.AlbumFetchException -> true
        is DomainException.ArtistFetchException -> true
        else -> false
    }
}
