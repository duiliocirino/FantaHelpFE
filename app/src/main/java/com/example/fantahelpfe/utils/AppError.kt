package com.example.fantahelpfe.utils

// In a common 'util' package, or within your 'domain' or 'data' layer,
// e.g., com.example.fantahelpfe.util.AppError.kt or com.example.fantahelpfe.model.AppError.kt

/**
 * A sealed class representing different types of errors that can occur within the application,
 * particularly from the data layer (Repository).
 */
sealed class AppError(
    open val displayMessage: String? = null,
    override val cause: Throwable? = null
) : Exception(displayMessage, cause) {

    data class NetworkError(
        override val displayMessage: String = "Network connection unavailable. Please check your internet.",
        override val cause: Throwable? = null
    ) : AppError(displayMessage, cause)

    data class ApiError(
        val statusCode: Int?,
        val apiErrorMessage: String?,
        override val displayMessage: String = apiErrorMessage ?: "An error occurred with the server.",
        override val cause: Throwable? = null
    ) : AppError(displayMessage, cause)

    data class NotFoundError(
        override val displayMessage: String = "The requested item could not be found.",
        override val cause: Throwable? = null
    ) : AppError(displayMessage, cause)

    data class AuthorizationError(
        override val displayMessage: String = "You are not authorized to perform this action.",
        override val cause: Throwable? = null
    ) : AppError(displayMessage, cause)

    data class DataMappingError(
        override val displayMessage: String = "There was an issue processing the data.",
        override val cause: Throwable? = null
    ) : AppError(displayMessage, cause)

    data class UnknownError(
        override val displayMessage: String = "An unexpected error occurred.",
        override val cause: Throwable? = null
    ) : AppError(displayMessage, cause)

    // --- You can add more specific error types as needed ---
    // e.g., DatabaseError, UserInputValidationError (if handled at this layer)

    /**
     * Helper to get a developer-facing message including the cause for logging.
     */
    fun getDetailedMessage(): String {
        return "${this::class.simpleName}: $displayMessage" + (cause?.let { " | Cause: ${it.message}" } ?: "")
    }
}
