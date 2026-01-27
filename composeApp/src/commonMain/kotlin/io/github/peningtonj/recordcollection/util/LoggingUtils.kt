package io.github.peningtonj.recordcollection.util

import io.github.aakira.napier.Napier

/**
 * Centralized logging utilities for the application.
 * Wraps Napier with additional context and structured logging.
 */
object LoggingUtils {
    
    /**
     * Log categories for better filtering and organization
     */
    enum class Category(val tag: String) {
        NETWORK("Network"),
        DATABASE("Database"),
        AUTH("Auth"),
        PLAYBACK("Playback"),
        UI("UI"),
        REPOSITORY("Repository"),
        VIEWMODEL("ViewModel"),
        MIGRATION("Migration"),
        SYNC("Sync")
    }
    
    /**
     * Log a debug message with category
     */
    fun d(category: Category, message: String, throwable: Throwable? = null) {
        Napier.d(message, throwable, category.tag)
    }
    
    /**
     * Log an info message with category
     */
    fun i(category: Category, message: String, throwable: Throwable? = null) {
        Napier.i(message, throwable, category.tag)
    }
    
    /**
     * Log a warning message with category
     */
    fun w(category: Category, message: String, throwable: Throwable? = null) {
        Napier.w(message, throwable, category.tag)
    }
    
    /**
     * Log an error message with category
     */
    fun e(category: Category, message: String, throwable: Throwable? = null) {
        Napier.e(message, throwable, category.tag)
    }
    
    /**
     * Log a verbose message with category
     */
    fun v(category: Category, message: String, throwable: Throwable? = null) {
        Napier.v(message, throwable, category.tag)
    }
    
    /**
     * Log the start of an operation
     */
    fun logOperationStart(category: Category, operation: String, details: Map<String, Any>? = null) {
        val detailsStr = details?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: ""
        d(category, "Starting: $operation ${if (detailsStr.isNotEmpty()) "[$detailsStr]" else ""}")
    }
    
    /**
     * Log the success of an operation
     */
    fun logOperationSuccess(category: Category, operation: String, duration: Long? = null, details: Map<String, Any>? = null) {
        val durationStr = duration?.let { " (${it}ms)" } ?: ""
        val detailsStr = details?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: ""
        i(category, "Success: $operation$durationStr ${if (detailsStr.isNotEmpty()) "[$detailsStr]" else ""}")
    }
    
    /**
     * Log the failure of an operation
     */
    fun logOperationFailure(category: Category, operation: String, error: Throwable, details: Map<String, Any>? = null) {
        val detailsStr = details?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: ""
        e(category, "Failed: $operation ${if (detailsStr.isNotEmpty()) "[$detailsStr]" else ""}", error)
    }
    
    /**
     * Measure and log the execution time of a block
     */
    inline fun <T> measureAndLog(category: Category, operation: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        logOperationStart(category, operation)
        
        return try {
            val result = block()
            val duration = System.currentTimeMillis() - startTime
            logOperationSuccess(category, operation, duration)
            result
        } catch (e: Exception) {
            logOperationFailure(category, operation, e)
            throw e
        }
    }
    
    /**
     * Log network request details
     */
    fun logNetworkRequest(method: String, url: String, headers: Map<String, String>? = null) {
        val headersStr = headers?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: "none"
        d(Category.NETWORK, "Request: $method $url [headers: $headersStr]")
    }
    
    /**
     * Log network response details
     */
    fun logNetworkResponse(url: String, statusCode: Int, duration: Long) {
        i(Category.NETWORK, "Response: $url [status: $statusCode, duration: ${duration}ms]")
    }
    
    /**
     * Log database query execution
     */
    fun logDatabaseQuery(query: String, params: List<Any>? = null) {
        val paramsStr = params?.joinToString(", ") ?: "none"
        d(Category.DATABASE, "Query: $query [params: $paramsStr]")
    }
    
    /**
     * Log authentication events
     */
    fun logAuthEvent(event: String, success: Boolean, details: String? = null) {
        val message = "Auth: $event - ${if (success) "Success" else "Failed"}"
        val fullMessage = details?.let { "$message [$it]" } ?: message
        if (success) i(Category.AUTH, fullMessage) else w(Category.AUTH, fullMessage)
    }
}
