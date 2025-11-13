package com.ml.lansonesscan.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.ml.lansonesscan.data.local.dao.ScanDao
import com.ml.lansonesscan.data.local.entities.ScanResultEntity
import com.ml.lansonesscan.data.local.storage.ImageStorageManager
import com.ml.lansonesscan.data.remote.service.AnalysisService
import com.ml.lansonesscan.data.remote.service.VarietyAnalysisResult
import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.LansonesVariety
import com.ml.lansonesscan.domain.model.ScanMetadata
import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.domain.repository.ScanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
/**
 * Implementation of ScanRepository that handles both local and remote data sources
 */
class ScanRepositoryImpl(
    private val scanDao: ScanDao,
    private val analysisService: AnalysisService,
    private val imageStorageManager: ImageStorageManager,
    private val context: Context
) : ScanRepository {
    
    companion object {
        private const val TAG = "ScanRepositoryImpl"
        private const val API_VERSION = "gemini-1.5-flash"
    }
    
    override suspend fun analyzeLansonesImage(
        imageUri: Uri,
        analysisType: AnalysisType
    ): Result<ScanResult> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting image analysis for type: $analysisType")
                
                // Validate image URI
                if (!isValidImageUri(imageUri)) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Invalid image URI provided")
                    )
                }
                
                // Get image bytes and metadata
                val imageBytes = getImageBytesFromUri(imageUri)
                val mimeType = getMimeTypeFromUri(imageUri)
                val imageSize = imageBytes.size.toLong()
                
                Log.d(TAG, "Image loaded: size=${imageSize} bytes, mimeType=$mimeType")
                
                // Perform analysis using remote service
                val startTime = System.currentTimeMillis()
                val analysisResult = analysisService.analyzeImage(
                    imageUri = imageUri,
                    getImageBytes = { getImageBytesFromUri(it) },
                    getMimeType = { getMimeTypeFromUri(it) }
                )
                val analysisTime = System.currentTimeMillis() - startTime
                
                if (analysisResult.isFailure) {
                    Log.e(TAG, "Analysis failed", analysisResult.exceptionOrNull())
                    return@withContext Result.failure(
                        analysisResult.exceptionOrNull() ?: Exception("Analysis failed")
                    )
                }
                
                val analysis = analysisResult.getOrThrow()
                Log.d(TAG, "Analysis completed: diseaseDetected=${analysis.diseaseDetected}")
                
                // Save image to local storage
                val savedImagePath = imageStorageManager.saveImage(imageBytes, "jpg")
                    ?: return@withContext Result.failure(
                        Exception("Failed to save image to local storage")
                    )
                
                // Create scan metadata
                val metadata = ScanMetadata.create(
                    imageSize = imageSize,
                    imageFormat = getImageFormatFromMimeType(mimeType),
                    analysisTime = analysisTime,
                    apiVersion = API_VERSION
                )
                
                // Extract variety information if available
                val (variety, varietyConfidence) = if (analysis.varietyResult != null) {
                    analysis.varietyResult.variety to analysis.varietyResult.confidenceLevel
                } else {
                    null to null
                }
                
                // CRITICAL: Ensure disease name is never null when disease is detected
                // This prevents validation errors in ScanResult constructor
                val finalDiseaseDetected: Boolean
                val finalDiseaseName: String?
                
                if (analysis.diseaseDetected) {
                    if (analysis.diseaseName.isNullOrBlank()) {
                        // Disease detected but no name - use fallback
                        finalDiseaseDetected = true
                        finalDiseaseName = "Unidentified Disease"
                        Log.w(TAG, "Disease detected but no name in analysis result. Using fallback.")
                    } else {
                        finalDiseaseDetected = true
                        finalDiseaseName = analysis.diseaseName
                    }
                } else {
                    finalDiseaseDetected = false
                    finalDiseaseName = null
                }
                
                // Create scan result using the detected analysis type
                val scanResult = ScanResult.create(
                    imagePath = savedImagePath,
                    analysisType = analysis.detectedAnalysisType,
                    diseaseDetected = finalDiseaseDetected,
                    diseaseName = finalDiseaseName,
                    confidenceLevel = analysis.confidenceLevel,
                    recommendations = analysis.recommendations,
                    metadata = metadata,
                    variety = variety,
                    varietyConfidence = varietyConfidence
                )
                
                // Save to database
                val saveResult = saveScanResult(scanResult)
                if (saveResult.isFailure) {
                    // Clean up saved image if database save fails
                    imageStorageManager.deleteImage(savedImagePath)
                    return@withContext saveResult.map { scanResult }
                }
                
                Log.d(TAG, "Scan result saved successfully with ID: ${scanResult.id}")
                Result.success(scanResult)
                
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during image analysis", e)
                Result.failure(e)
            }
        }
    }
    
    override suspend fun saveScanResult(scanResult: ScanResult): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Saving scan result with ID: ${scanResult.id}")
                
                // Validate scan result
                if (!scanResult.isValid()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Invalid scan result provided")
                    )
                }
                
                // Convert to entity and save
                val entity = ScanResultEntity.fromDomainModel(scanResult)
                scanDao.insertScan(entity)
                
                Log.d(TAG, "Scan result saved successfully")
                Result.success(Unit)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save scan result", e)
                Result.failure(e)
            }
        }
    }
    
    override fun getAllScans(): Flow<List<ScanResult>> {
        return scanDao.getAllScans().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun getScanById(id: String): ScanResult? {
        return withContext(Dispatchers.IO) {
            try {
                scanDao.getScanById(id)?.toDomainModel()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get scan by ID: $id", e)
                null
            }
        }
    }
    
    override suspend fun deleteScan(id: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Deleting scan with ID: $id")
                
                // Get scan to retrieve image path
                val scan = scanDao.getScanById(id)
                if (scan == null) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Scan with ID $id not found")
                    )
                }
                
                // Delete from database first
                scanDao.deleteScanById(id)
                
                // Delete associated image file
                val imageDeleted = imageStorageManager.deleteImage(scan.imagePath)
                if (!imageDeleted) {
                    Log.w(TAG, "Failed to delete image file: ${scan.imagePath}")
                }
                
                Log.d(TAG, "Scan deleted successfully")
                Result.success(Unit)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete scan", e)
                Result.failure(e)
            }
        }
    }
    
    override suspend fun clearAllScans(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Clearing all scans")
                
                // Get all scans to retrieve image paths
                val allScans = scanDao.getAllScansAsList()
                
                // Delete all from database
                scanDao.deleteAllScans()
                
                // Delete all associated image files
                var deletedImages = 0
                allScans.forEach { scan ->
                    if (imageStorageManager.deleteImage(scan.imagePath)) {
                        deletedImages++
                    }
                }
                
                Log.d(TAG, "Cleared ${allScans.size} scans and $deletedImages image files")
                Result.success(Unit)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear all scans", e)
                Result.failure(e)
            }
        }
    }
    
    override fun getRecentScans(limit: Int): Flow<List<ScanResult>> {
        return scanDao.getRecentScans(limit).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getScansByAnalysisType(analysisType: AnalysisType): Flow<List<ScanResult>> {
        return scanDao.getScansByAnalysisType(analysisType).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getScansByDiseaseStatus(diseaseDetected: Boolean): Flow<List<ScanResult>> {
        return scanDao.getScansByDiseaseStatus(diseaseDetected).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun getScanCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                scanDao.getScanCount()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get scan count", e)
                0
            }
        }
    }
    
    override suspend fun getDiseaseDetectedCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                scanDao.getDiseaseDetectedCount()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get disease detected count", e)
                0
            }
        }
    }
    
    override suspend fun getHealthyScansCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                scanDao.getHealthyLansonesScansCount()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get healthy scans count", e)
                0
            }
        }
    }

    override suspend fun getNonLansonesCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                scanDao.getNonLansonesCount()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get non-lansones count", e)
                0
            }
        }
    }
    
    override suspend fun getTotalStorageSize(): Long {
        return withContext(Dispatchers.IO) {
            try {
                scanDao.getTotalImageSize() ?: 0L
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get total storage size", e)
                0L
            }
        }
    }
    
    override suspend fun cleanupOrphanedImages(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting cleanup of orphaned images")
                
                // Get all image paths from database
                val allScans = scanDao.getAllScansAsList()
                val databaseImagePaths = allScans.map { it.imagePath }.toSet()
                
                // Get all image files from storage
                val storageImagePaths = imageStorageManager.getAllImagePaths()
                
                // Find orphaned files (in storage but not in database)
                val orphanedPaths = storageImagePaths - databaseImagePaths
                
                // Delete orphaned files
                var deletedCount = 0
                orphanedPaths.forEach { path ->
                    if (imageStorageManager.deleteImage(path)) {
                        deletedCount++
                        Log.d(TAG, "Deleted orphaned image: $path")
                    }
                }
                
                Log.d(TAG, "Cleanup completed: deleted $deletedCount orphaned images")
                Result.success(deletedCount)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cleanup orphaned images", e)
                Result.failure(e)
            }
        }
    }
    
    override suspend fun exportScanData(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Exporting scan data")
                
                val allScans = scanDao.getAllScansAsList()
                val exportData = buildString {
                    appendLine("Lansones Disease Scanner - Scan Export")
                    appendLine("Generated: ${java.util.Date()}")
                    appendLine("Total Scans: ${allScans.size}")
                    appendLine()
                    
                    allScans.forEach { scan ->
                        val domainScan = scan.toDomainModel()
                        appendLine("Scan ID: ${scan.id}")
                        appendLine("Date: ${java.util.Date(scan.timestamp)}")
                        appendLine("Analysis Type: ${scan.analysisType.getDisplayName()}")
                        appendLine("Disease Detected: ${if (scan.diseaseDetected) "Yes" else "No"}")
                        if (scan.diseaseDetected && scan.diseaseName != null) {
                            appendLine("Disease Name: ${scan.diseaseName}")
                        }
                        // Add variety information if available
                        if (domainScan.variety != null && domainScan.variety != LansonesVariety.UNKNOWN) {
                            appendLine("Variety: ${domainScan.variety.getDisplayName()}")
                            if (domainScan.varietyConfidence != null) {
                                appendLine("Variety Confidence: ${(domainScan.varietyConfidence * 100).toInt()}%")
                            }
                        }
                        appendLine("Confidence: ${domainScan.getConfidencePercentage()}%")
                        appendLine("Recommendations:")
                        scan.recommendations.forEach { recommendation ->
                            appendLine("  - $recommendation")
                        }
                        appendLine("---")
                        appendLine()
                    }
                }
                
                Log.d(TAG, "Export completed: ${exportData.length} characters")
                Result.success(exportData)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export scan data", e)
                Result.failure(e)
            }
        }
    }
    
    // Helper methods
    
    private suspend fun isValidImageUri(uri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { it.available() > 0 } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to validate image URI", e)
            false
        }
    }
    
    private suspend fun getImageBytesFromUri(uri: Uri): ByteArray {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        } ?: throw IllegalArgumentException("Cannot read image from URI")
    }
    
    private fun getMimeTypeFromUri(uri: Uri): String {
        return context.contentResolver.getType(uri) ?: "image/jpeg"
    }
    
    private fun getImageFormatFromMimeType(mimeType: String): String {
        return when {
            mimeType.contains("jpeg") || mimeType.contains("jpg") -> "JPEG"
            mimeType.contains("png") -> "PNG"
            mimeType.contains("webp") -> "WEBP"
            else -> "JPEG"
        }
    }
}