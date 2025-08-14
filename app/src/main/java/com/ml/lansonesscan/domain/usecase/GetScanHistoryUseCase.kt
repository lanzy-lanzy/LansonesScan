package com.ml.lansonesscan.domain.usecase

import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.domain.repository.ScanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
/**
 * Use case for retrieving scan history with filtering and sorting capabilities
 */
class GetScanHistoryUseCase(
    private val scanRepository: ScanRepository
) {
    
    /**
     * Gets all scan history with optional filtering and sorting
     * @param params Parameters for filtering and sorting
     * @return Flow of filtered and sorted scan results
     */
    operator fun invoke(params: Params = Params()): Flow<List<ScanResult>> {
        return scanRepository.getAllScans().map { scanResults ->
            var filteredResults = scanResults
            
            // Apply filters
            params.analysisType?.let { type ->
                filteredResults = filteredResults.filter { it.analysisType == type }
            }
            
            params.diseaseDetected?.let { diseaseStatus ->
                filteredResults = filteredResults.filter { it.diseaseDetected == diseaseStatus }
            }
            
            params.dateRange?.let { range ->
                filteredResults = filteredResults.filter { 
                    it.timestamp >= range.startTime && it.timestamp <= range.endTime 
                }
            }
            
            params.searchQuery?.let { query ->
                if (query.isNotBlank()) {
                    filteredResults = filteredResults.filter { scanResult ->
                        scanResult.diseaseName?.contains(query, ignoreCase = true) == true ||
                        scanResult.recommendations.any { it.contains(query, ignoreCase = true) }
                    }
                }
            }
            
            // Apply sorting
            when (params.sortBy) {
                SortBy.DATE_DESC -> filteredResults.sortedByDescending { it.timestamp }
                SortBy.DATE_ASC -> filteredResults.sortedBy { it.timestamp }
                SortBy.CONFIDENCE_DESC -> filteredResults.sortedByDescending { it.confidenceLevel }
                SortBy.CONFIDENCE_ASC -> filteredResults.sortedBy { it.confidenceLevel }
                SortBy.ANALYSIS_TYPE -> filteredResults.sortedBy { it.analysisType.name }
                SortBy.DISEASE_STATUS -> filteredResults.sortedWith(
                    compareByDescending<ScanResult> { it.diseaseDetected }
                        .thenByDescending { it.confidenceLevel }
                )
            }
        }
    }
    
    /**
     * Gets recent scans with a specified limit
     * @param limit Maximum number of recent scans to retrieve
     * @return Flow of recent scan results
     */
    fun getRecentScans(limit: Int): Flow<List<ScanResult>> {
        require(limit > 0) { "Limit must be positive" }
        return scanRepository.getRecentScans(limit)
    }
    
    /**
     * Gets scans filtered by analysis type
     * @param analysisType The type of analysis to filter by
     * @return Flow of filtered scan results
     */
    fun getScansByAnalysisType(analysisType: AnalysisType): Flow<List<ScanResult>> {
        return scanRepository.getScansByAnalysisType(analysisType)
    }
    
    /**
     * Gets scans filtered by disease detection status
     * @param diseaseDetected Whether to filter for scans with disease detected
     * @return Flow of filtered scan results
     */
    fun getScansByDiseaseStatus(diseaseDetected: Boolean): Flow<List<ScanResult>> {
        return scanRepository.getScansByDiseaseStatus(diseaseDetected)
    }
    
    /**
     * Gets scan statistics
     * @return Flow of scan statistics
     */
    suspend fun getScanStatistics(): ScanStatistics {
        val totalScans = scanRepository.getScanCount()
        val diseaseDetectedCount = scanRepository.getDiseaseDetectedCount()
        val nonLansonesCount = scanRepository.getNonLansonesCount()
        val healthyScansCount = scanRepository.getHealthyScansCount()
        val totalStorageSize = scanRepository.getTotalStorageSize()

        return ScanStatistics(
            totalScans = totalScans,
            diseaseDetectedCount = diseaseDetectedCount,
            healthyScansCount = healthyScansCount,
            nonLansonesCount = nonLansonesCount,
            totalStorageSize = totalStorageSize,
            diseaseDetectionRate = if (totalScans > 0) {
                (diseaseDetectedCount.toFloat() / totalScans.toFloat()) * 100f
            } else 0f
        )
    }
    
    /**
     * Parameters for filtering and sorting scan history
     */
    data class Params(
        val analysisType: AnalysisType? = null,
        val diseaseDetected: Boolean? = null,
        val dateRange: DateRange? = null,
        val searchQuery: String? = null,
        val sortBy: SortBy = SortBy.DATE_DESC
    )
    
    /**
     * Date range for filtering scans
     */
    data class DateRange(
        val startTime: Long,
        val endTime: Long
    ) {
        init {
            require(startTime <= endTime) { "Start time must be before or equal to end time" }
            require(startTime > 0) { "Start time must be positive" }
            require(endTime > 0) { "End time must be positive" }
        }
    }
    
    /**
     * Sorting options for scan history
     */
    enum class SortBy {
        DATE_DESC,
        DATE_ASC,
        CONFIDENCE_DESC,
        CONFIDENCE_ASC,
        ANALYSIS_TYPE,
        DISEASE_STATUS
    }
    
    /**
     * Statistics about scan history
     */
    data class ScanStatistics(
        val totalScans: Int,
        val diseaseDetectedCount: Int,
        val healthyScansCount: Int,
        val nonLansonesCount: Int,
        val totalStorageSize: Long,
        val diseaseDetectionRate: Float
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
         * Returns formatted disease detection rate
         */
        fun getFormattedDetectionRate(): String {
            return "%.1f%%".format(diseaseDetectionRate)
        }
    }
}