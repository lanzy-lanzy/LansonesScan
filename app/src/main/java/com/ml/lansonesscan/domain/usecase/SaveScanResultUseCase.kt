package com.ml.lansonesscan.domain.usecase

import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.domain.repository.ScanRepository
/**
 * Use case for saving scan results with proper error handling
 * Handles validation and persistence logic for scan results
 */
class SaveScanResultUseCase(
    private val scanRepository: ScanRepository
) {
    
    /**
     * Saves a scan result to local storage
     * @param scanResult The scan result to save
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(scanResult: ScanResult): Result<Unit> {
        return try {
            // Validate the scan result before saving
            validateScanResult(scanResult)
            
            // Save through repository
            val result = scanRepository.saveScanResult(scanResult)
            
            result.fold(
                onSuccess = { 
                    Result.success(Unit)
                },
                onFailure = { error ->
                    // Log the error and return a more user-friendly message
                    when (error) {
                        is SecurityException -> {
                            Result.failure(
                                IllegalStateException("Permission denied while saving scan result")
                            )
                        }
                        is java.io.IOException -> {
                            Result.failure(
                                IllegalStateException("Storage error while saving scan result")
                            )
                        }
                        else -> {
                            Result.failure(
                                IllegalStateException("Failed to save scan result: ${error.message}")
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
     * Validates the scan result before saving
     */
    private fun validateScanResult(scanResult: ScanResult) {
        require(scanResult.isValid()) { "Scan result is not valid" }
        require(scanResult.id.isNotBlank()) { "Scan result ID cannot be blank" }
        require(scanResult.imagePath.isNotBlank()) { "Image path cannot be blank" }
        require(scanResult.timestamp > 0) { "Timestamp must be positive" }
        require(scanResult.metadata.isValidImageFormat()) { 
            "Invalid image format: ${scanResult.metadata.imageFormat}" 
        }
        
        // Validate disease detection logic
        if (scanResult.diseaseDetected) {
            require(!scanResult.diseaseName.isNullOrBlank()) {
                "Disease name cannot be null or blank when disease is detected"
            }
            require(scanResult.recommendations.isNotEmpty()) {
                "Recommendations cannot be empty when disease is detected"
            }
        }
        
        // Validate confidence level
        require(scanResult.confidenceLevel in 0.0f..1.0f) {
            "Confidence level must be between 0.0 and 1.0"
        }
    }
    
    /**
     * Saves multiple scan results in a batch operation
     * @param scanResults List of scan results to save
     * @return Result containing the number of successfully saved results
     */
    suspend fun saveBatch(scanResults: List<ScanResult>): Result<Int> {
        return try {
            require(scanResults.isNotEmpty()) { "Scan results list cannot be empty" }
            
            var successCount = 0
            val failures = mutableListOf<Exception>()
            
            scanResults.forEach { scanResult ->
                invoke(scanResult).fold(
                    onSuccess = { successCount++ },
                    onFailure = { error -> failures.add(Exception(error)) }
                )
            }
            
            if (failures.isEmpty()) {
                Result.success(successCount)
            } else {
                Result.failure(
                    IllegalStateException(
                        "Batch save completed with $successCount successes and ${failures.size} failures"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}