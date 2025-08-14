package com.ml.lansonesscan.domain.usecase

import android.net.Uri
import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.domain.repository.ScanRepository
/**
 * Use case for analyzing lansones images for disease detection
 * Handles validation and processing logic for image analysis
 */
class AnalyzeImageUseCase(
    private val scanRepository: ScanRepository
) {
    
    /**
     * Analyzes an image for disease detection
     * @param imageUri URI of the image to analyze
     * @param analysisType Type of analysis (FRUIT or LEAVES)
     * @return Result containing the scan result or error
     */
    suspend operator fun invoke(
        imageUri: Uri,
        analysisType: AnalysisType
    ): Result<ScanResult> {
        return try {
            // Validate input parameters
            validateInput(imageUri, analysisType)
            
            // Perform the analysis through repository
            val result = scanRepository.analyzeLansonesImage(imageUri, analysisType)
            
            // Validate the result before returning
            result.fold(
                onSuccess = { scanResult ->
                    if (scanResult.isValid()) {
                        Result.success(scanResult)
                    } else {
                        Result.failure(
                            IllegalStateException("Invalid scan result received from analysis")
                        )
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Validates the input parameters for analysis
     */
    private fun validateInput(imageUri: Uri, analysisType: AnalysisType) {
        require(imageUri != Uri.EMPTY) { "Image URI cannot be empty" }
        require(imageUri.scheme != null) { "Image URI must have a valid scheme" }
        
        // Additional validation could be added here for:
        // - File existence
        // - File format validation
        // - File size limits
        // - Network connectivity (for API calls)
    }
    
    /**
     * Data class representing the parameters for image analysis
     */
    data class Params(
        val imageUri: Uri,
        val analysisType: AnalysisType
    )
}