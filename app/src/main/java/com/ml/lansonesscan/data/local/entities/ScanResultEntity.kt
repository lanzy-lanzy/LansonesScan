package com.ml.lansonesscan.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.LansonesVariety
import com.ml.lansonesscan.domain.model.ScanMetadata
import com.ml.lansonesscan.domain.model.ScanResult

/**
 * Room entity representing a scan result in the database
 */
@Entity(
    tableName = "scan_results",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["analysisType"]),
        Index(value = ["diseaseDetected"])
    ]
)
data class ScanResultEntity(
    @PrimaryKey
    val id: String,
    val imagePath: String,
    val analysisType: AnalysisType,
    val diseaseDetected: Boolean,
    val diseaseName: String?,
    val confidenceLevel: Float,
    val recommendations: List<String>,
    val timestamp: Long,
    val imageSize: Long,
    val imageFormat: String,
    val analysisTime: Long,
    val apiVersion: String,
    val variety: String? = null,
    val varietyConfidence: Float? = null
) {
    /**
     * Convert entity to domain model
     */
    fun toDomainModel(): ScanResult {
        return ScanResult(
            id = id,
            imagePath = imagePath,
            analysisType = analysisType,
            diseaseDetected = diseaseDetected,
            diseaseName = diseaseName,
            confidenceLevel = confidenceLevel,
            recommendations = recommendations,
            timestamp = timestamp,
            metadata = ScanMetadata(
                imageSize = imageSize,
                imageFormat = imageFormat,
                analysisTime = analysisTime,
                apiVersion = apiVersion
            ),
            variety = variety?.let { LansonesVariety.fromString(it) },
            varietyConfidence = varietyConfidence
        )
    }
    
    companion object {
        /**
         * Convert domain model to entity
         */
        fun fromDomainModel(scanResult: ScanResult): ScanResultEntity {
            return ScanResultEntity(
                id = scanResult.id,
                imagePath = scanResult.imagePath,
                analysisType = scanResult.analysisType,
                diseaseDetected = scanResult.diseaseDetected,
                diseaseName = scanResult.diseaseName,
                confidenceLevel = scanResult.confidenceLevel,
                recommendations = scanResult.recommendations,
                timestamp = scanResult.timestamp,
                imageSize = scanResult.metadata.imageSize,
                imageFormat = scanResult.metadata.imageFormat,
                analysisTime = scanResult.metadata.analysisTime,
                apiVersion = scanResult.metadata.apiVersion,
                variety = scanResult.variety?.name,
                varietyConfidence = scanResult.varietyConfidence
            )
        }
    }
}