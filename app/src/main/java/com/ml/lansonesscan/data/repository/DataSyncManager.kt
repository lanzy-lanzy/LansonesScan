package com.ml.lansonesscan.data.repository

import android.content.Context
import android.util.Log
import com.ml.lansonesscan.data.local.dao.ScanDao
import com.ml.lansonesscan.data.local.storage.ImageStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
/**
 * Manages data synchronization, cleanup, and export operations
 */
class DataSyncManager(
    private val scanDao: ScanDao,
    private val imageStorageManager: ImageStorageManager,
    private val context: Context
) {
    
    companion object {
        private const val TAG = "DataSyncManager"
        private const val EXPORT_FILE_PREFIX = "lansones_scan_export"
        private const val MAX_SCANS_TO_KEEP = 1000
        private const val DAYS_TO_KEEP_OLD_SCANS = 365L
    }
    
    /**
     * Performs comprehensive data cleanup including orphaned images and old scans
     * @param maxScansToKeep Maximum number of scans to keep (oldest will be deleted)
     * @param daysToKeepOldScans Number of days to keep old scans
     * @return CleanupResult with details of cleanup operations
     */
    suspend fun performDataCleanup(
        maxScansToKeep: Int = MAX_SCANS_TO_KEEP,
        daysToKeepOldScans: Long = DAYS_TO_KEEP_OLD_SCANS
    ): CleanupResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting comprehensive data cleanup")
            
            var totalDeletedScans = 0
            var totalDeletedImages = 0
            var totalFreedSpace = 0L
            
            // 1. Clean up orphaned images (images without database entries)
            val orphanedImagesResult = cleanupOrphanedImages()
            totalDeletedImages += orphanedImagesResult.deletedFiles
            totalFreedSpace += orphanedImagesResult.freedSpace
            
            // 2. Clean up old scans beyond retention period
            val cutoffTime = System.currentTimeMillis() - (daysToKeepOldScans * 24 * 60 * 60 * 1000)
            val oldScansResult = cleanupOldScans(cutoffTime)
            totalDeletedScans += oldScansResult.deletedScans
            totalDeletedImages += oldScansResult.deletedImages
            totalFreedSpace += oldScansResult.freedSpace
            
            // 3. Limit total number of scans (keep most recent)
            val excessScansResult = limitTotalScans(maxScansToKeep)
            totalDeletedScans += excessScansResult.deletedScans
            totalDeletedImages += excessScansResult.deletedImages
            totalFreedSpace += excessScansResult.freedSpace
            
            // 4. Clean up cache files
            val cacheCleared = imageStorageManager.clearCache()
            
            val result = CleanupResult(
                success = true,
                deletedScans = totalDeletedScans,
                deletedImages = totalDeletedImages,
                freedSpaceBytes = totalFreedSpace,
                cacheCleared = cacheCleared,
                message = "Cleanup completed successfully"
            )
            
            Log.d(TAG, "Cleanup completed: $result")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Data cleanup failed", e)
            CleanupResult(
                success = false,
                message = "Cleanup failed: ${e.message}"
            )
        }
    }
    
    /**
     * Cleans up orphaned images that don't have corresponding database entries
     */
    private suspend fun cleanupOrphanedImages(): OrphanedCleanupResult {
        return try {
            Log.d(TAG, "Cleaning up orphaned images")
            
            // Get all image paths from database
            val allScans = scanDao.getAllScansAsList()
            val databaseImagePaths = allScans.map { it.imagePath }.toSet()
            
            // Get all image files from storage
            val storageImagePaths = imageStorageManager.getAllImagePaths()
            
            // Find orphaned files (in storage but not in database)
            val orphanedPaths = storageImagePaths - databaseImagePaths
            
            var deletedFiles = 0
            var freedSpace = 0L
            
            // Delete orphaned files and calculate freed space
            orphanedPaths.forEach { path ->
                try {
                    val file = File(path)
                    val fileSize = if (file.exists()) file.length() else 0L
                    
                    if (imageStorageManager.deleteImage(path)) {
                        deletedFiles++
                        freedSpace += fileSize
                        Log.d(TAG, "Deleted orphaned image: $path (${fileSize} bytes)")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete orphaned image: $path", e)
                }
            }
            
            OrphanedCleanupResult(deletedFiles, freedSpace)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup orphaned images", e)
            OrphanedCleanupResult(0, 0L)
        }
    }
    
    /**
     * Cleans up scans older than the specified cutoff time
     */
    private suspend fun cleanupOldScans(cutoffTime: Long): ScanCleanupResult {
        return try {
            Log.d(TAG, "Cleaning up old scans before ${Date(cutoffTime)}")
            
            // Get scans older than cutoff time
            val oldScans = scanDao.getAllScansAsList().filter { it.timestamp < cutoffTime }
            
            var deletedScans = 0
            var deletedImages = 0
            var freedSpace = 0L
            
            oldScans.forEach { scan ->
                try {
                    // Calculate image file size before deletion
                    val imageFile = File(scan.imagePath)
                    val imageSize = if (imageFile.exists()) imageFile.length() else 0L
                    
                    // Delete from database
                    scanDao.deleteScanById(scan.id)
                    deletedScans++
                    
                    // Delete associated image file
                    if (imageStorageManager.deleteImage(scan.imagePath)) {
                        deletedImages++
                        freedSpace += imageSize
                    }
                    
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete old scan: ${scan.id}", e)
                }
            }
            
            ScanCleanupResult(deletedScans, deletedImages, freedSpace)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old scans", e)
            ScanCleanupResult(0, 0, 0L)
        }
    }
    
    /**
     * Limits the total number of scans by deleting the oldest ones
     */
    private suspend fun limitTotalScans(maxScansToKeep: Int): ScanCleanupResult {
        return try {
            val totalScans = scanDao.getScanCount()
            
            if (totalScans <= maxScansToKeep) {
                return ScanCleanupResult(0, 0, 0L)
            }
            
            Log.d(TAG, "Limiting scans to $maxScansToKeep (current: $totalScans)")
            
            // Get oldest scans to delete
            val allScans = scanDao.getAllScansAsList().sortedBy { it.timestamp }
            val scansToDelete = allScans.take(totalScans - maxScansToKeep)
            
            var deletedScans = 0
            var deletedImages = 0
            var freedSpace = 0L
            
            scansToDelete.forEach { scan ->
                try {
                    // Calculate image file size before deletion
                    val imageFile = File(scan.imagePath)
                    val imageSize = if (imageFile.exists()) imageFile.length() else 0L
                    
                    // Delete from database
                    scanDao.deleteScanById(scan.id)
                    deletedScans++
                    
                    // Delete associated image file
                    if (imageStorageManager.deleteImage(scan.imagePath)) {
                        deletedImages++
                        freedSpace += imageSize
                    }
                    
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete excess scan: ${scan.id}", e)
                }
            }
            
            ScanCleanupResult(deletedScans, deletedImages, freedSpace)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to limit total scans", e)
            ScanCleanupResult(0, 0, 0L)
        }
    }
    
    /**
     * Exports all scan data to a formatted string for backup or sharing
     * @param includeStatistics Whether to include summary statistics
     * @return ExportResult containing the exported data or error
     */
    suspend fun exportScanData(includeStatistics: Boolean = true): ExportResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Exporting scan data")
            
            val allScans = scanDao.getAllScansAsList()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val exportDate = dateFormat.format(Date())
            
            val exportData = buildString {
                appendLine("=".repeat(60))
                appendLine("LANSONES DISEASE SCANNER - DATA EXPORT")
                appendLine("=".repeat(60))
                appendLine("Export Date: $exportDate")
                appendLine("Total Scans: ${allScans.size}")
                appendLine()
                
                if (includeStatistics && allScans.isNotEmpty()) {
                    appendStatistics(allScans)
                    appendLine()
                }
                
                appendLine("SCAN DETAILS")
                appendLine("-".repeat(40))
                
                if (allScans.isEmpty()) {
                    appendLine("No scans found.")
                } else {
                    allScans.sortedByDescending { it.timestamp }.forEachIndexed { index, scan ->
                        appendLine("${index + 1}. Scan ID: ${scan.id}")
                        appendLine("   Date: ${dateFormat.format(Date(scan.timestamp))}")
                        appendLine("   Analysis Type: ${scan.analysisType.getDisplayName()}")
                        appendLine("   Disease Detected: ${if (scan.diseaseDetected) "Yes" else "No"}")
                        
                        if (scan.diseaseDetected && scan.diseaseName != null) {
                            appendLine("   Disease Name: ${scan.diseaseName}")
                        }
                        
                        appendLine("   Confidence: ${(scan.confidenceLevel * 100).toInt()}%")
                        appendLine("   Image Size: ${formatFileSize(scan.imageSize)}")
                        appendLine("   Analysis Time: ${formatDuration(scan.analysisTime)}")
                        
                        if (scan.recommendations.isNotEmpty()) {
                            appendLine("   Recommendations:")
                            scan.recommendations.forEach { recommendation ->
                                appendLine("     • $recommendation")
                            }
                        }
                        
                        appendLine()
                    }
                }
                
                appendLine("=".repeat(60))
                appendLine("End of Export")
            }
            
            ExportResult(
                success = true,
                data = exportData,
                scanCount = allScans.size,
                exportSize = exportData.length,
                message = "Export completed successfully"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export scan data", e)
            ExportResult(
                success = false,
                message = "Export failed: ${e.message}"
            )
        }
    }
    
    /**
     * Saves exported data to a file in the app's external files directory
     */
    suspend fun saveExportToFile(exportData: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${EXPORT_FILE_PREFIX}_$timestamp.txt"
            val externalFilesDir = context.getExternalFilesDir(null)
            val exportFile = File(externalFilesDir, fileName)
            
            exportFile.writeText(exportData)
            
            Log.d(TAG, "Export saved to: ${exportFile.absolutePath}")
            Result.success(exportFile.absolutePath)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save export to file", e)
            Result.failure(e)
        }
    }
    
    /**
     * Gets comprehensive storage statistics
     */
    suspend fun getStorageStatistics(): StorageStatistics = withContext(Dispatchers.IO) {
        try {
            val scanCount = scanDao.getScanCount()
            val diseaseDetectedCount = scanDao.getDiseaseDetectedCount()
            val healthyScansCount = scanDao.getHealthyScansCount()
            val totalImageSize = scanDao.getTotalImageSize() ?: 0L
            val storageInfo = imageStorageManager.getStorageInfo()
            
            StorageStatistics(
                totalScans = scanCount,
                diseaseDetectedScans = diseaseDetectedCount,
                healthyScans = healthyScansCount,
                totalImageSize = totalImageSize,
                originalImagesSize = storageInfo.originalImagesSize,
                thumbnailsSize = storageInfo.thumbnailsSize,
                cacheSize = storageInfo.cacheSize,
                totalStorageSize = storageInfo.totalSize
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get storage statistics", e)
            StorageStatistics()
        }
    }
    
    // Helper methods
    
    private fun StringBuilder.appendStatistics(scans: List<com.ml.lansonesscan.data.local.entities.ScanResultEntity>) {
        val diseaseDetectedCount = scans.count { it.diseaseDetected }
        val healthyCount = scans.size - diseaseDetectedCount
        val fruitScans = scans.count { it.analysisType == com.ml.lansonesscan.domain.model.AnalysisType.FRUIT }
        val leafScans = scans.count { it.analysisType == com.ml.lansonesscan.domain.model.AnalysisType.LEAVES }
        val totalImageSize = scans.sumOf { it.imageSize }
        val avgConfidence = if (scans.isNotEmpty()) scans.map { it.confidenceLevel }.average() else 0.0
        
        appendLine("STATISTICS")
        appendLine("-".repeat(20))
        appendLine("Disease Detected: $diseaseDetectedCount (${(diseaseDetectedCount * 100.0 / scans.size).toInt()}%)")
        appendLine("Healthy Scans: $healthyCount (${(healthyCount * 100.0 / scans.size).toInt()}%)")
        appendLine("Fruit Analysis: $fruitScans")
        appendLine("Leaf Analysis: $leafScans")
        appendLine("Total Image Size: ${formatFileSize(totalImageSize)}")
        appendLine("Average Confidence: ${(avgConfidence * 100).toInt()}%")
        
        if (diseaseDetectedCount > 0) {
            val diseaseNames = scans.filter { it.diseaseDetected && it.diseaseName != null }
                .groupBy { it.diseaseName }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
            
            if (diseaseNames.isNotEmpty()) {
                appendLine("Most Common Diseases:")
                diseaseNames.take(5).forEach { (disease, count) ->
                    appendLine("  • $disease: $count cases")
                }
            }
        }
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
    
    private fun formatDuration(millis: Long): String {
        return when {
            millis < 1000 -> "${millis}ms"
            millis < 60000 -> "${millis / 1000}s"
            else -> "${millis / 60000}m ${(millis % 60000) / 1000}s"
        }
    }
}

// Data classes for results

data class CleanupResult(
    val success: Boolean,
    val deletedScans: Int = 0,
    val deletedImages: Int = 0,
    val freedSpaceBytes: Long = 0L,
    val cacheCleared: Boolean = false,
    val message: String
) {
    fun getFreedSpaceMB(): Float = freedSpaceBytes / (1024f * 1024f)
}

data class OrphanedCleanupResult(
    val deletedFiles: Int,
    val freedSpace: Long
)

data class ScanCleanupResult(
    val deletedScans: Int,
    val deletedImages: Int,
    val freedSpace: Long
)

data class ExportResult(
    val success: Boolean,
    val data: String = "",
    val scanCount: Int = 0,
    val exportSize: Int = 0,
    val message: String
)

data class StorageStatistics(
    val totalScans: Int = 0,
    val diseaseDetectedScans: Int = 0,
    val healthyScans: Int = 0,
    val totalImageSize: Long = 0L,
    val originalImagesSize: Long = 0L,
    val thumbnailsSize: Long = 0L,
    val cacheSize: Long = 0L,
    val totalStorageSize: Long = 0L
) {
    fun getTotalStorageMB(): Float = totalStorageSize / (1024f * 1024f)
    fun getDiseaseDetectionRate(): Float = if (totalScans > 0) diseaseDetectedScans.toFloat() / totalScans else 0f
}