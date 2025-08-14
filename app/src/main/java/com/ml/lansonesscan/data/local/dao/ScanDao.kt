package com.ml.lansonesscan.data.local.dao

import androidx.room.*
import com.ml.lansonesscan.data.local.entities.ScanResultEntity
import com.ml.lansonesscan.domain.model.AnalysisType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for scan results
 */
@Dao
interface ScanDao {
    
    /**
     * Insert a new scan result
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanResultEntity)
    
    /**
     * Insert multiple scan results
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScans(scans: List<ScanResultEntity>)
    
    /**
     * Update an existing scan result
     */
    @Update
    suspend fun updateScan(scan: ScanResultEntity)
    
    /**
     * Delete a scan result by ID
     */
    @Query("DELETE FROM scan_results WHERE id = :scanId")
    suspend fun deleteScanById(scanId: String)
    
    /**
     * Delete a scan result entity
     */
    @Delete
    suspend fun deleteScan(scan: ScanResultEntity)
    
    /**
     * Delete all scan results
     */
    @Query("DELETE FROM scan_results")
    suspend fun deleteAllScans()
    
    /**
     * Get a scan result by ID
     */
    @Query("SELECT * FROM scan_results WHERE id = :scanId")
    suspend fun getScanById(scanId: String): ScanResultEntity?
    
    /**
     * Get all scan results ordered by timestamp (newest first)
     */
    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanResultEntity>>
    
    /**
     * Get all scan results as a list (for testing)
     */
    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC")
    suspend fun getAllScansAsList(): List<ScanResultEntity>
    
    /**
     * Get scan results by analysis type
     */
    @Query("SELECT * FROM scan_results WHERE analysisType = :analysisType ORDER BY timestamp DESC")
    fun getScansByAnalysisType(analysisType: AnalysisType): Flow<List<ScanResultEntity>>
    
    /**
     * Get scan results by disease detection status
     */
    @Query("SELECT * FROM scan_results WHERE diseaseDetected = :diseaseDetected ORDER BY timestamp DESC")
    fun getScansByDiseaseStatus(diseaseDetected: Boolean): Flow<List<ScanResultEntity>>
    
    /**
     * Get recent scan results (limited count)
     */
    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentScans(limit: Int): Flow<List<ScanResultEntity>>
    
    /**
     * Get scan results within a date range
     */
    @Query("SELECT * FROM scan_results WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getScansInDateRange(startTime: Long, endTime: Long): Flow<List<ScanResultEntity>>
    
    /**
     * Get scan results with disease detected and confidence above threshold
     */
    @Query("SELECT * FROM scan_results WHERE diseaseDetected = 1 AND confidenceLevel >= :minConfidence ORDER BY confidenceLevel DESC")
    fun getHighConfidenceDiseaseScans(minConfidence: Float): Flow<List<ScanResultEntity>>
    
    /**
     * Get count of all scans
     */
    @Query("SELECT COUNT(*) FROM scan_results")
    suspend fun getScanCount(): Int
    
    /**
     * Get count of scans by analysis type
     */
    @Query("SELECT COUNT(*) FROM scan_results WHERE analysisType = :analysisType")
    suspend fun getScanCountByType(analysisType: AnalysisType): Int
    
    /**
     * Get count of scans with disease detected
     */
    @Query("SELECT COUNT(*) FROM scan_results WHERE diseaseDetected = 1")
    suspend fun getDiseaseDetectedCount(): Int
    
    /**
     * Get count of healthy scans
     */
    @Query("SELECT COUNT(*) FROM scan_results WHERE diseaseDetected = 0")
    suspend fun getHealthyScansCount(): Int
    
    /**
     * Get the most recent scan
     */
    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecentScan(): ScanResultEntity?
    
    /**
     * Get scans with specific disease name
     */
    @Query("SELECT * FROM scan_results WHERE diseaseName = :diseaseName ORDER BY timestamp DESC")
    fun getScansByDiseaseName(diseaseName: String): Flow<List<ScanResultEntity>>
    
    /**
     * Search scans by disease name (case insensitive)
     */
    @Query("SELECT * FROM scan_results WHERE diseaseName LIKE '%' || :searchTerm || '%' ORDER BY timestamp DESC")
    fun searchScansByDiseaseName(searchTerm: String): Flow<List<ScanResultEntity>>
    
    /**
     * Get total storage size of all images
     */
    @Query("SELECT SUM(imageSize) FROM scan_results")
    suspend fun getTotalImageSize(): Long?
    
    /**
     * Get average confidence level for all scans
     */
    @Query("SELECT AVG(confidenceLevel) FROM scan_results")
    suspend fun getAverageConfidenceLevel(): Float?
    
    /**
     * Get scans paginated
     */
    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getScansPaginated(limit: Int, offset: Int): List<ScanResultEntity>
    
    /**
     * Delete old scans beyond a certain count (keep only the most recent)
     */
    @Query("""
        DELETE FROM scan_results 
        WHERE id NOT IN (
            SELECT id FROM scan_results 
            ORDER BY timestamp DESC 
            LIMIT :keepCount
        )
    """)
    suspend fun deleteOldScans(keepCount: Int)
    
    /**
     * Delete scans older than a specific timestamp
     */
    @Query("DELETE FROM scan_results WHERE timestamp < :cutoffTime")
    suspend fun deleteScansOlderThan(cutoffTime: Long)
}