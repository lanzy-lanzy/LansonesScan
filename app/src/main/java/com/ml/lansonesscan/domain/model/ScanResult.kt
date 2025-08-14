package com.ml.lansonesscan.domain.model

import java.util.UUID

/**
 * Domain model representing a scan result with disease analysis
 */
data class ScanResult(
    val id: String,
    val imagePath: String,
    val analysisType: AnalysisType,
    val diseaseDetected: Boolean,
    val diseaseName: String?,
    val confidenceLevel: Float,
    val recommendations: List<String>,
    val timestamp: Long,
    val metadata: ScanMetadata
) {
    init {
        require(id.isNotBlank()) { "ID cannot be blank" }
        require(imagePath.isNotBlank()) { "Image path cannot be blank" }
        require(confidenceLevel in 0.0f..1.0f) { "Confidence level must be between 0.0 and 1.0" }
        require(timestamp > 0) { "Timestamp must be positive" }
        
        // If disease is detected, disease name should not be null or blank
        if (diseaseDetected) {
            require(!diseaseName.isNullOrBlank()) { 
                "Disease name cannot be null or blank when disease is detected" 
            }
        }
    }

    /**
     * Returns confidence level as a percentage
     */
    fun getConfidencePercentage(): Int {
        return (confidenceLevel * 100).toInt()
    }

    /**
     * Returns a human-readable status string
     */
    fun getStatusText(): String {
        return if (diseaseDetected) {
            "Disease Detected: $diseaseName"
        } else {
            "Healthy"
        }
    }

    /**
     * Returns the severity level based on confidence
     */
    fun getSeverityLevel(): SeverityLevel {
        return when {
            !diseaseDetected -> SeverityLevel.HEALTHY
            confidenceLevel >= 0.8f -> SeverityLevel.HIGH
            confidenceLevel >= 0.6f -> SeverityLevel.MEDIUM
            else -> SeverityLevel.LOW
        }
    }

    /**
     * Checks if the scan result is valid
     */
    fun isValid(): Boolean {
        return try {
            // Validate all constraints
            require(id.isNotBlank())
            require(imagePath.isNotBlank())
            require(confidenceLevel in 0.0f..1.0f)
            require(timestamp > 0)
            require(metadata.isValidImageFormat())
            
            if (diseaseDetected) {
                require(!diseaseName.isNullOrBlank())
            }
            
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /**
     * Creates a copy with updated recommendations
     */
    fun withRecommendations(newRecommendations: List<String>): ScanResult {
        return copy(recommendations = newRecommendations)
    }

    companion object {
        /**
         * Creates a new ScanResult with generated ID and current timestamp
         */
        fun create(
            imagePath: String,
            analysisType: AnalysisType,
            diseaseDetected: Boolean,
            diseaseName: String?,
            confidenceLevel: Float,
            recommendations: List<String>,
            metadata: ScanMetadata
        ): ScanResult {
            return ScanResult(
                id = UUID.randomUUID().toString(),
                imagePath = imagePath,
                analysisType = analysisType,
                diseaseDetected = diseaseDetected,
                diseaseName = diseaseName,
                confidenceLevel = confidenceLevel,
                recommendations = recommendations,
                timestamp = System.currentTimeMillis(),
                metadata = metadata
            )
        }

        /**
         * Creates a healthy scan result
         */
        fun createHealthy(
            imagePath: String,
            analysisType: AnalysisType,
            confidenceLevel: Float,
            metadata: ScanMetadata
        ): ScanResult {
            return create(
                imagePath = imagePath,
                analysisType = analysisType,
                diseaseDetected = false,
                diseaseName = null,
                confidenceLevel = confidenceLevel,
                recommendations = listOf("Plant appears healthy. Continue regular care."),
                metadata = metadata
            )
        }

        /**
         * Creates a diseased scan result
         */
        fun createDiseased(
            imagePath: String,
            analysisType: AnalysisType,
            diseaseName: String,
            confidenceLevel: Float,
            recommendations: List<String>,
            metadata: ScanMetadata
        ): ScanResult {
            return create(
                imagePath = imagePath,
                analysisType = analysisType,
                diseaseDetected = true,
                diseaseName = diseaseName,
                confidenceLevel = confidenceLevel,
                recommendations = recommendations,
                metadata = metadata
            )
        }
    }
}

/**
 * Enum representing the severity level of a detected disease
 */
enum class SeverityLevel {
    HEALTHY,
    LOW,
    MEDIUM,
    HIGH;

    fun getDisplayName(): String {
        return when (this) {
            HEALTHY -> "Healthy"
            LOW -> "Low Risk"
            MEDIUM -> "Medium Risk"
            HIGH -> "High Risk"
        }
    }

    fun getColor(): String {
        return when (this) {
            HEALTHY -> "#4CAF50" // Green
            LOW -> "#FFC107" // Amber
            MEDIUM -> "#FF9800" // Orange
            HIGH -> "#F44336" // Red
        }
    }
}