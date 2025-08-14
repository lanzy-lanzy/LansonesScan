package com.ml.lansonesscan.domain.usecase

import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.repository.ScanRepository
/**
 * Use case for retrieving storage information for settings display
 * Provides comprehensive storage statistics and management information
 */
class GetStorageInfoUseCase(
    private val scanRepository: ScanRepository
) {
    
    /**
     * Gets comprehensive storage information
     * @return Storage information for display in settings
     */
    suspend operator fun invoke(): StorageInfo {
        return try {
            val totalScans = scanRepository.getScanCount()
            val diseaseDetectedCount = scanRepository.getDiseaseDetectedCount()
            val healthyScansCount = scanRepository.getHealthyScansCount()
            val totalStorageSize = scanRepository.getTotalStorageSize()
            
            // Get breakdown by analysis type
            val fruitScansFlow = scanRepository.getScansByAnalysisType(AnalysisType.FRUIT)
            val leafScansFlow = scanRepository.getScansByAnalysisType(AnalysisType.LEAVES)
            
            // Since we need the count, we'll collect the flows
            // Note: In a real implementation, you might want to add count methods to repository
            var fruitScansCount = 0
            var leafScansCount = 0
            
            fruitScansFlow.collect { fruitScans ->
                fruitScansCount = fruitScans.size
            }
            
            leafScansFlow.collect { leafScans ->
                leafScansCount = leafScans.size
            }
            
            StorageInfo(
                totalScans = totalScans,
                diseaseDetectedCount = diseaseDetectedCount,
                healthyScansCount = healthyScansCount,
                totalStorageSize = totalStorageSize,
                fruitScansCount = fruitScansCount,
                leafScansCount = leafScansCount,
                lastUpdated = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            // Return empty storage info on error
            StorageInfo(
                totalScans = 0,
                diseaseDetectedCount = 0,
                healthyScansCount = 0,
                totalStorageSize = 0L,
                fruitScansCount = 0,
                leafScansCount = 0,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Gets storage breakdown by analysis type
     * @return Breakdown of storage usage by analysis type
     */
    suspend fun getStorageBreakdown(): StorageBreakdown {
        return try {
            val storageInfo = invoke()
            
            StorageBreakdown(
                fruitAnalysis = AnalysisTypeStorage(
                    type = AnalysisType.FRUIT,
                    scanCount = storageInfo.fruitScansCount,
                    // Estimated storage per type (this would need actual calculation in real implementation)
                    estimatedStorageSize = (storageInfo.totalStorageSize * storageInfo.fruitScansCount) / 
                        maxOf(storageInfo.totalScans, 1)
                ),
                leafAnalysis = AnalysisTypeStorage(
                    type = AnalysisType.LEAVES,
                    scanCount = storageInfo.leafScansCount,
                    estimatedStorageSize = (storageInfo.totalStorageSize * storageInfo.leafScansCount) / 
                        maxOf(storageInfo.totalScans, 1)
                )
            )
        } catch (e: Exception) {
            StorageBreakdown(
                fruitAnalysis = AnalysisTypeStorage(AnalysisType.FRUIT, 0, 0L),
                leafAnalysis = AnalysisTypeStorage(AnalysisType.LEAVES, 0, 0L)
            )
        }
    }
    
    /**
     * Gets storage recommendations based on current usage
     * @return List of storage management recommendations
     */
    suspend fun getStorageRecommendations(): List<StorageRecommendation> {
        val storageInfo = invoke()
        val recommendations = mutableListOf<StorageRecommendation>()
        
        // Check if storage is getting high (this would need actual device storage info)
        if (storageInfo.totalStorageSize > 100 * 1024 * 1024) { // > 100MB
            recommendations.add(
                StorageRecommendation(
                    type = RecommendationType.HIGH_STORAGE_USAGE,
                    title = "High Storage Usage",
                    description = "Consider clearing old scan results to free up space",
                    priority = RecommendationPriority.MEDIUM
                )
            )
        }
        
        // Check for old scans (would need timestamp analysis)
        if (storageInfo.totalScans > 100) {
            recommendations.add(
                StorageRecommendation(
                    type = RecommendationType.TOO_MANY_SCANS,
                    title = "Many Stored Scans",
                    description = "You have ${storageInfo.totalScans} scans. Consider removing old ones.",
                    priority = RecommendationPriority.LOW
                )
            )
        }
        
        // Check for orphaned files
        try {
            val orphanedCount = scanRepository.cleanupOrphanedImages().getOrDefault(0)
            if (orphanedCount > 0) {
                recommendations.add(
                    StorageRecommendation(
                        type = RecommendationType.ORPHANED_FILES,
                        title = "Orphaned Files Detected",
                        description = "$orphanedCount orphaned files found. Clean them up to free space.",
                        priority = RecommendationPriority.HIGH
                    )
                )
            }
        } catch (e: Exception) {
            // Ignore orphaned file check errors
        }
        
        return recommendations
    }
    
    /**
     * Comprehensive storage information
     */
    data class StorageInfo(
        val totalScans: Int,
        val diseaseDetectedCount: Int,
        val healthyScansCount: Int,
        val totalStorageSize: Long,
        val fruitScansCount: Int,
        val leafScansCount: Int,
        val lastUpdated: Long
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
        
        /**
         * Returns average storage per scan
         */
        fun getAverageStoragePerScan(): Long {
            return if (totalScans > 0) totalStorageSize / totalScans else 0L
        }
        
        /**
         * Returns formatted average storage per scan
         */
        fun getFormattedAverageStoragePerScan(): String {
            val avgSize = getAverageStoragePerScan()
            return when {
                avgSize < 1024 -> "$avgSize B"
                avgSize < 1024 * 1024 -> "${avgSize / 1024} KB"
                else -> "${avgSize / (1024 * 1024)} MB"
            }
        }
    }
    
    /**
     * Storage breakdown by analysis type
     */
    data class StorageBreakdown(
        val fruitAnalysis: AnalysisTypeStorage,
        val leafAnalysis: AnalysisTypeStorage
    )
    
    /**
     * Storage information for a specific analysis type
     */
    data class AnalysisTypeStorage(
        val type: AnalysisType,
        val scanCount: Int,
        val estimatedStorageSize: Long
    ) {
        fun getFormattedStorageSize(): String {
            return when {
                estimatedStorageSize < 1024 -> "$estimatedStorageSize B"
                estimatedStorageSize < 1024 * 1024 -> "${estimatedStorageSize / 1024} KB"
                estimatedStorageSize < 1024 * 1024 * 1024 -> "${estimatedStorageSize / (1024 * 1024)} MB"
                else -> "${estimatedStorageSize / (1024 * 1024 * 1024)} GB"
            }
        }
    }
    
    /**
     * Storage management recommendation
     */
    data class StorageRecommendation(
        val type: RecommendationType,
        val title: String,
        val description: String,
        val priority: RecommendationPriority
    )
    
    /**
     * Types of storage recommendations
     */
    enum class RecommendationType {
        HIGH_STORAGE_USAGE,
        TOO_MANY_SCANS,
        ORPHANED_FILES,
        OLD_SCANS
    }
    
    /**
     * Priority levels for recommendations
     */
    enum class RecommendationPriority {
        LOW,
        MEDIUM,
        HIGH
    }
}