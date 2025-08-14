package com.ml.lansonesscan.domain.usecase

import com.ml.lansonesscan.domain.repository.ScanRepository
/**
 * Use case for clearing all scan history with bulk operations
 * Handles validation and cleanup logic for complete history clearing
 */
class ClearHistoryUseCase(
    private val scanRepository: ScanRepository
) {
    
    /**
     * Clears all scan history and associated files
     * @return Result containing information about the clearing operation
     */
    suspend operator fun invoke(): Result<ClearHistoryResult> {
        return try {
            // Get current statistics before clearing
            val totalScans = scanRepository.getScanCount()
            val totalStorageSize = scanRepository.getTotalStorageSize()
            
            if (totalScans == 0) {
                return Result.success(
                    ClearHistoryResult(
                        scansCleared = 0,
                        storageFreed = 0L,
                        operationTime = 0L
                    )
                )
            }
            
            val startTime = System.currentTimeMillis()
            
            // Perform the clearing operation
            val result = scanRepository.clearAllScans()
            
            val endTime = System.currentTimeMillis()
            val operationTime = endTime - startTime
            
            result.fold(
                onSuccess = {
                    Result.success(
                        ClearHistoryResult(
                            scansCleared = totalScans,
                            storageFreed = totalStorageSize,
                            operationTime = operationTime
                        )
                    )
                },
                onFailure = { error ->
                    when (error) {
                        is SecurityException -> {
                            Result.failure(
                                IllegalStateException("Permission denied while clearing history")
                            )
                        }
                        is java.io.IOException -> {
                            Result.failure(
                                IllegalStateException("File system error while clearing history")
                            )
                        }
                        else -> {
                            Result.failure(
                                IllegalStateException("Failed to clear history: ${error.message}")
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
     * Clears history with confirmation callback
     * @param confirmationCallback Callback to confirm the operation before proceeding
     * @return Result containing information about the clearing operation
     */
    suspend fun clearWithConfirmation(
        confirmationCallback: suspend (ClearHistoryPreview) -> Boolean
    ): Result<ClearHistoryResult> {
        return try {
            // Get preview information
            val preview = getPreview()
            
            // Ask for confirmation
            val confirmed = confirmationCallback(preview)
            
            if (!confirmed) {
                return Result.failure(
                    IllegalStateException("Operation cancelled by user")
                )
            }
            
            // Proceed with clearing
            invoke()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets a preview of what will be cleared
     * @return Preview information about the clearing operation
     */
    suspend fun getPreview(): ClearHistoryPreview {
        val totalScans = scanRepository.getScanCount()
        val diseaseDetectedCount = scanRepository.getDiseaseDetectedCount()
        val healthyScansCount = scanRepository.getHealthyScansCount()
        val totalStorageSize = scanRepository.getTotalStorageSize()
        
        return ClearHistoryPreview(
            totalScans = totalScans,
            diseaseDetectedCount = diseaseDetectedCount,
            healthyScansCount = healthyScansCount,
            totalStorageSize = totalStorageSize
        )
    }
    
    /**
     * Cleans up orphaned files without clearing the database
     * @return Result containing the number of orphaned files cleaned
     */
    suspend fun cleanupOrphanedFiles(): Result<Int> {
        return try {
            scanRepository.cleanupOrphanedImages()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Result of a clear history operation
     */
    data class ClearHistoryResult(
        val scansCleared: Int,
        val storageFreed: Long,
        val operationTime: Long
    ) {
        /**
         * Returns formatted storage freed
         */
        fun getFormattedStorageFreed(): String {
            return when {
                storageFreed < 1024 -> "$storageFreed B"
                storageFreed < 1024 * 1024 -> "${storageFreed / 1024} KB"
                storageFreed < 1024 * 1024 * 1024 -> "${storageFreed / (1024 * 1024)} MB"
                else -> "${storageFreed / (1024 * 1024 * 1024)} GB"
            }
        }
        
        /**
         * Returns formatted operation time
         */
        fun getFormattedOperationTime(): String {
            return when {
                operationTime < 1000 -> "${operationTime}ms"
                operationTime < 60000 -> "${operationTime / 1000}s"
                else -> "${operationTime / 60000}m ${(operationTime % 60000) / 1000}s"
            }
        }
    }
    
    /**
     * Preview information for clear history operation
     */
    data class ClearHistoryPreview(
        val totalScans: Int,
        val diseaseDetectedCount: Int,
        val healthyScansCount: Int,
        val totalStorageSize: Long
    ) {
        /**
         * Returns formatted storage size
         */
        fun getFormattedStorageSize(): String {
            return when {
                totalStorageSize < 1024 -> "$totalStorageSize B"
                totalStorageSize < 1024 * 1024 -> "${totalStorageSize / 1024} KB"
                totalStorageSize < 1024 * 1024 * 1024 -> "${totalStorageSize / (1024 * 1024)} MB"
                else -> "${totalStorageSize / (1024 * 1024 * 1024)} GB"
            }
        }
        
        /**
         * Returns disease detection rate
         */
        fun getDiseaseDetectionRate(): Float {
            return if (totalScans > 0) {
                (diseaseDetectedCount.toFloat() / totalScans.toFloat()) * 100f
            } else 0f
        }
    }
}