package com.example.fantahelpfe.utils

// In a common 'util' package, or within your 'domain' or 'data' layer,
// e.g., com.example.fantahelpfe.util.AppError.kt or com.example.fantahelpfe.model.AppError.kt

/**
 * A sealed class representing different types of errors that can occur within the application,
 * particularly from the data layer (Repository).
 */
sealed class YourAppError(
    open val displayMessage: String? = null, // A user-friendly message, potentially from a string resource
    override val cause: Throwable? = null       // The original exception, useful for logging
) : Exception(displayMessage, cause) {

    /**
     * Represents network-related errors (e.g., no internet connection, DNS resolution failure).
     */
    data class NetworkError(
        override val displayMessage: String = "Network connection unavailable. Please check your internet.",
        override val cause: Throwable? = null
    ) : YourAppError(displayMessage, cause)

    /**
     * Represents errors returned by the server/API (e.g., HTTP 4xx, 5xx errors).
     */
    data class ApiError(
        val statusCode: Int?, // HTTP status code, if applicable
        val apiErrorMessage: String?, // Message directly from the API response body, if any
        override val displayMessage: String = apiErrorMessage ?: "An error occurred with the server.",
        override val cause: Throwable? = null
    ) : YourAppError(displayMessage, cause)

    /**
     * Represents an error when a requested resource was not found (typically HTTP 404).
     */
    data class NotFoundError(
        override val displayMessage: String = "The requested item could not be found.",
        override val cause: Throwable? = null
    ) : YourAppError(displayMessage, cause)

    /**
     * Represents an authorization or authentication error (typically HTTP 401 or 403).
     */
    data class AuthorizationError(
        override val displayMessage: String = "You are not authorized to perform this action.",
        override val cause: Throwable? = null
    ) : YourAppError(displayMessage, cause)

    /**
     * Represents an error during data parsing or mapping (e.g., unexpected JSON format).
     */
    data class DataMappingError(
        override val displayMessage: String = "There was an issue processing the data.",
        override val cause: Throwable? = null
    ) : YourAppError(displayMessage, cause)

    /**
     * A generic error for cases not covered by more specific types.
     */
    data class UnknownError(
        override val displayMessage: String = "An unexpected error occurred.",
        override val cause: Throwable? = null
    ) : YourAppError(displayMessage, cause)

    // --- You can add more specific error types as needed ---
    // e.g., DatabaseError, UserInputValidationError (if handled at this layer)

    /**
     * Helper to get a developer-facing message including the cause for logging.
     */
    fun getDetailedMessage(): String {
        return "${this::class.simpleName}: $displayMessage" + (cause?.let { " | Cause: ${it.message}" } ?: "")
    }
}
