package io.github.peningtonj.recordcollection.util

/**
 * Validation utilities for common input validation patterns
 */
object ValidationUtils {
    
    /**
     * Validation result
     */
    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()
        
        val isValid: Boolean get() = this is Valid
        val errorMessage: String? get() = (this as? Invalid)?.message
    }
    
    /**
     * Validate that a string is not empty or blank
     */
    fun validateNotEmpty(value: String?, fieldName: String = "Field"): ValidationResult {
        return when {
            value.isNullOrBlank() -> ValidationResult.Invalid("$fieldName cannot be empty")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validate minimum length
     */
    fun validateMinLength(value: String?, minLength: Int, fieldName: String = "Field"): ValidationResult {
        return when {
            value == null -> ValidationResult.Invalid("$fieldName cannot be null")
            value.length < minLength -> ValidationResult.Invalid("$fieldName must be at least $minLength characters")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validate maximum length
     */
    fun validateMaxLength(value: String?, maxLength: Int, fieldName: String = "Field"): ValidationResult {
        return when {
            value == null -> ValidationResult.Valid  // null is acceptable, just check max length if present
            value.length > maxLength -> ValidationResult.Invalid("$fieldName must be at most $maxLength characters")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validate a range of values
     */
    fun validateRange(value: Int, min: Int, max: Int, fieldName: String = "Value"): ValidationResult {
        return when {
            value < min -> ValidationResult.Invalid("$fieldName must be at least $min")
            value > max -> ValidationResult.Invalid("$fieldName must be at most $max")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validate a list is not empty
     */
    fun <T> validateListNotEmpty(list: List<T>?, fieldName: String = "List"): ValidationResult {
        return when {
            list.isNullOrEmpty() -> ValidationResult.Invalid("$fieldName cannot be empty")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validate a Spotify ID format
     */
    fun validateSpotifyId(id: String?, fieldName: String = "ID"): ValidationResult {
        return when {
            id.isNullOrBlank() -> ValidationResult.Invalid("$fieldName cannot be empty")
            id.length != 22 -> ValidationResult.Invalid("$fieldName must be 22 characters long")
            !id.all { it.isLetterOrDigit() } -> ValidationResult.Invalid("$fieldName must be alphanumeric")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validate a URL format
     */
    fun validateUrl(url: String?, fieldName: String = "URL"): ValidationResult {
        return when {
            url.isNullOrBlank() -> ValidationResult.Invalid("$fieldName cannot be empty")
            !url.startsWith("http://") && !url.startsWith("https://") -> 
                ValidationResult.Invalid("$fieldName must start with http:// or https://")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validate a Spotify URI format
     */
    fun validateSpotifyUri(uri: String?, fieldName: String = "URI"): ValidationResult {
        return when {
            uri.isNullOrBlank() -> ValidationResult.Invalid("$fieldName cannot be empty")
            !uri.startsWith("spotify:") -> ValidationResult.Invalid("$fieldName must start with 'spotify:'")
            uri.count { it == ':' } != 2 -> ValidationResult.Invalid("$fieldName must have format 'spotify:type:id'")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validate rating value (1-10)
     */
    fun validateRating(rating: Int): ValidationResult {
        return validateRange(rating, 1, 10, "Rating")
    }
    
    /**
     * Validate album name
     */
    fun validateAlbumName(name: String?): ValidationResult {
        val notEmpty = validateNotEmpty(name, "Album name")
        if (!notEmpty.isValid) return notEmpty
        
        return validateMaxLength(name, 255, "Album name")
    }
    
    /**
     * Validate artist name
     */
    fun validateArtistName(name: String?): ValidationResult {
        val notEmpty = validateNotEmpty(name, "Artist name")
        if (!notEmpty.isValid) return notEmpty
        
        return validateMaxLength(name, 255, "Artist name")
    }
    
    /**
     * Validate collection name
     */
    fun validateCollectionName(name: String?): ValidationResult {
        val notEmpty = validateNotEmpty(name, "Collection name")
        if (!notEmpty.isValid) return notEmpty
        
        val minLength = validateMinLength(name, 1, "Collection name")
        if (!minLength.isValid) return minLength
        
        return validateMaxLength(name, 100, "Collection name")
    }
    
    /**
     * Validate search query
     */
    fun validateSearchQuery(query: String?): ValidationResult {
        return when {
            query.isNullOrBlank() -> ValidationResult.Invalid("Search query cannot be empty")
            query.length < 2 -> ValidationResult.Invalid("Search query must be at least 2 characters")
            query.length > 100 -> ValidationResult.Invalid("Search query is too long")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validate tag name
     */
    fun validateTagName(name: String?): ValidationResult {
        val notEmpty = validateNotEmpty(name, "Tag name")
        if (!notEmpty.isValid) return notEmpty
        
        return validateMaxLength(name, 50, "Tag name")
    }
    
    /**
     * Combine multiple validation results
     */
    fun combine(vararg results: ValidationResult): ValidationResult {
        val errors = results.mapNotNull { it.errorMessage }
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors.joinToString("; "))
        }
    }
    
    /**
     * Validate and get result, or throw exception
     */
    fun validateOrThrow(validation: ValidationResult) {
        if (validation is ValidationResult.Invalid) {
            throw IllegalArgumentException(validation.message)
        }
    }
}
