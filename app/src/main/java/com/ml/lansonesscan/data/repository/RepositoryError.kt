package com.ml.lansonesscan.data.repository

/**
 * Sealed class representing different types of repository errors
 */
sealed class RepositoryError : Exception() {
    
    /**
     * Network-related errors (API calls, connectivity)
     */
    data class NetworkError(
        override val message: String,
        override val cause: Throwable? = null
    ) : RepositoryError()
    
    /**
     * Database-related errors (storage, queries)
     */
    data class DatabaseError(
        override val message: String,
        override val cause: Throwable? = null
    ) : RepositoryError()
    
    /**
     * File system errors (image storage, file operations)
     */
    data class StorageError(
        override val message: String,
        override val cause: Throwable? = null
    ) : RepositoryError()
    
    /**
     * Validation errors (invalid data, constraints)
     */
    data class ValidationError(
        override val message: String,
        override val cause: Throwable? = null
    ) : RepositoryError()
    
    /**
     * Analysis-specific errors (API processing, parsing)
     */
    data class AnalysisError(
        override val message: String,
        override val cause: Throwable? = null
    ) : RepositoryError()
    
    /**
     * Unknown or unexpected errors
     */
    data class UnknownError(
        override val message: String,
        override val cause: Throwable? = null
    ) : RepositoryError()
    
    companion object {
        /**
         * Creates appropriate error type from exception
         */
        fun fromException(exception: Throwable): RepositoryError {
            return when (exception) {
                is java.net.UnknownHostException,
                is java.net.SocketTimeoutException,
                is java.io.IOException -> NetworkError(
                    message = "Network error: ${exception.message}",
                    cause = exception
                )
                
                is android.database.SQLException -> DatabaseError(
                    message = "Database error: ${exception.message}",
                    cause = exception
                )
                
                is java.io.FileNotFoundException,
                is java.security.AccessControlException -> StorageError(
                    message = "Storage error: ${exception.message}",
                    cause = exception
                )
                
                is IllegalArgumentException,
                is IllegalStateException -> ValidationError(
                    message = "Validation error: ${exception.message}",
                    cause = exception
                )
                
                else -> UnknownError(
                    message = "Unexpected error: ${exception.message}",
                    cause = exception
                )
            }
        }
    }
}