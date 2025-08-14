package com.ml.lansonesscan.domain.usecase

import com.ml.lansonesscan.domain.repository.ScanRepository
/**
 * Use case for deleting scan results with proper file cleanup
 * Handles validation and cleanup logic for scan deletion
 */
class DeleteScanUseCase(
    private val scanRepository: ScanRepository
) {
    
    /**
     * Deletes a scan result and associated image file
     * @param scanId The unique identifier of the scan to delete
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(scanId: String): Result<Unit> {
        return try {
            // Validate input
            validateScanId(scanId)
            
            // Check if scan exists before attempting deletion
            val existingScan = scanRepository.getScanById(scanId)
            if (existingScan == null) {
                return Result.failure(
                    NoSuchElementException("Scan with ID '$scanId' not found")
                )
            }
            
            // Perform deletion through repository
            val result = scanRepository.deleteScan(scanId)
            
            result.fold(
                onSuccess = { 
                    Result.success(Unit)
                },
                onFailure = { error ->
                    // Handle specific error types
                    when (error) {
                        is SecurityException -> {
                            Result.failure(
                                IllegalStateException("Permission denied while deleting scan")
                            )
                        }
                        is java.io.IOException -> {
                            Result.failure(
                                IllegalStateException("File system error while deleting scan")
                            )
                        }
                        else -> {
                            Result.failure(
                                IllegalStateException("Failed to delete scan: ${error.message}")
                            )
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Deletes multiple scans in a batch operation
     * @param scanIds List of scan IDs to delete
     * @return Result containing the number of successfully deleted scans
     */
    suspend fun deleteBatch(scanIds: List<String>): Result<DeleteBatchResult> {
        return try {
            require(scanIds.isNotEmpty()) { "Scan IDs list cannot be empty" }
            require(scanIds.all { it.isNotBlank() }) { "All scan IDs must be non-blank" }
            
            var successCount = 0
            val failures = mutableListOf<DeleteFailure>()
            
            scanIds.forEach { scanId ->
                invoke(scanId).fold(
                    onSuccess = { successCount++ },
                    onFailure = { error -> 
                        failures.add(DeleteFailure(scanId, error.message ?: "Unknown error"))
                    }
                )
            }
            
            Result.success(
                DeleteBatchResult(
                    totalRequested = scanIds.size,
                    successCount = successCount,
                    failureCount = failures.size,
                    failures = failures
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Validates the scan ID parameter
     */
    private fun validateScanId(scanId: String) {
        require(scanId.isNotBlank()) { "Scan ID cannot be blank" }
        require(scanId.length <= 255) { "Scan ID is too long" }
        // Additional validation could include UUID format checking
    }
    
    /**
     * Result of a batch delete operation
     */
    data class DeleteBatchResult(
        val totalRequested: Int,
        val successCount: Int,
        val failureCount: Int,
        val failures: List<DeleteFailure>
    ) {
        val isCompleteSuccess: Boolean
            get() = failureCount == 0
            
        val isPartialSuccess: Boolean
            get() = successCount > 0 && failureCount > 0
            
        val isCompleteFailure: Boolean
            get() = successCount == 0
            
        fun getSuccessRate(): Float {
            return if (totalRequested > 0) {
                (successCount.toFloat() / totalRequested.toFloat()) * 100f
            } else 0f
        }
    }
    
    /**
     * Information about a failed deletion
     */
    data class DeleteFailure(
        val scanId: String,
        val errorMessage: String
    )
}