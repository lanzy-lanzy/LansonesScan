package com.ml.lansonesscan.domain.model

/**
 * Metadata associated with a scan operation
 */
data class ScanMetadata(
    val imageSize: Long,
    val imageFormat: String,
    val analysisTime: Long,
    val apiVersion: String
) {
    init {
        require(imageSize >= 0) { "Image size cannot be negative" }
        require(imageFormat.isNotBlank()) { "Image format cannot be blank" }
        require(analysisTime >= 0) { "Analysis time cannot be negative" }
        require(apiVersion.isNotBlank()) { "API version cannot be blank" }
    }

    /**
     * Returns a human-readable file size string
     */
    fun getFormattedImageSize(): String {
        return when {
            imageSize < 1024 -> "$imageSize B"
            imageSize < 1024 * 1024 -> "${imageSize / 1024} KB"
            else -> "${imageSize / (1024 * 1024)} MB"
        }
    }

    /**
     * Returns a human-readable analysis time string
     */
    fun getFormattedAnalysisTime(): String {
        return when {
            analysisTime < 1000 -> "${analysisTime}ms"
            analysisTime < 60000 -> "${analysisTime / 1000}s"
            else -> "${analysisTime / 60000}m ${(analysisTime % 60000) / 1000}s"
        }
    }

    /**
     * Validates that the image format is supported
     */
    fun isValidImageFormat(): Boolean {
        val supportedFormats = setOf("JPEG", "JPG", "PNG", "WEBP")
        return imageFormat.uppercase() in supportedFormats
    }

    companion object {
        /**
         * Creates a ScanMetadata instance with validation
         */
        fun create(
            imageSize: Long,
            imageFormat: String,
            analysisTime: Long,
            apiVersion: String
        ): ScanMetadata {
            return ScanMetadata(
                imageSize = imageSize,
                imageFormat = imageFormat.uppercase(),
                analysisTime = analysisTime,
                apiVersion = apiVersion
            )
        }
    }
}