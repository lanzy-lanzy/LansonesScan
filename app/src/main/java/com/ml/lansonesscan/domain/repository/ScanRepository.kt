package com.ml.lansonesscan.domain.repository

import android.net.Uri
import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.ScanResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for scan operations
 * Defines the contract for data operations related to scan results
 */
interface ScanRepository {
    
    /**
     * Analyzes a lansones image for diseases and health issues
     * @param imageUri URI of the image to analyze
     * @param analysisType Type of analysis (FRUIT or LEAVES)
     * @return Result containing the scan result or error
     */
    suspend fun analyzeLansonesImage(
        imageUri: Uri, 
        analysisType: AnalysisType
    ): Result<ScanResult>
    
    /**
     * Saves a scan result to local storage
     * @param scanResult The scan result to save
     * @return Result indicating success or failure
     */
    suspend fun saveScanResult(scanResult: ScanResult): Result<Unit>
    
    /**
     * Retrieves all scan results as a Flow for reactive updates
     * @return Flow of list of scan results ordered by timestamp (newest first)
     */
    fun getAllScans(): Flow<List<ScanResult>>
    
    /**
     * Retrieves a specific scan result by ID
     * @param id The unique identifier of the scan
     * @return The scan result or null if not found
     */
    suspend fun getScanById(id: String): ScanResult?
    
    /**
     * Deletes a scan result and associated image file
     * @param id The unique identifier of the scan to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteScan(id: String): Result<Unit>
    
    /**
     * Deletes all scan results and associated image files
     * @return Result indicating success or failure
     */
    suspend fun clearAllScans(): Result<Unit>
    
    /**
     * Retrieves recent scan results with a limit
     * @param limit Maximum number of recent scans to retrieve
     * @return Flow of list of recent scan results
     */
    fun getRecentScans(limit: Int): Flow<List<ScanResult>>
    
    /**
     * Retrieves scan results filtered by analysis type
     * @param analysisType The type of analysis to filter by
     * @return Flow of list of filtered scan results
     */
    fun getScansByAnalysisType(analysisType: AnalysisType): Flow<List<ScanResult>>
    
    /**
     * Retrieves scan results filtered by disease detection status
     * @param diseaseDetected Whether to filter for scans with disease detected
     * @return Flow of list of filtered scan results
     */
    fun getScansByDiseaseStatus(diseaseDetected: Boolean): Flow<List<ScanResult>>
    
    /**
     * Gets the total count of all scans
     * @return Total number of scans
     */
    suspend fun getScanCount(): Int
    
    /**
     * Gets the count of scans with disease detected
     * @return Number of scans with disease detected
     */
    suspend fun getDiseaseDetectedCount(): Int
    
    /**
     * Gets the count of healthy scans
     * @return Number of healthy scans
     */
    suspend fun getHealthyScansCount(): Int
    
    /**
     * Gets the total storage size of all scan images
     * @return Total size in bytes, or 0 if no scans exist
     */
    suspend fun getTotalStorageSize(): Long
    
    /**
     * Cleans up orphaned image files that don't have corresponding database entries
     * @return Result indicating success or failure with count of cleaned files
     */
    suspend fun cleanupOrphanedImages(): Result<Int>
    
    /**
     * Exports scan data for backup or sharing
     * @return Result containing exported data or error
     */
    suspend fun exportScanData(): Result<String>
}