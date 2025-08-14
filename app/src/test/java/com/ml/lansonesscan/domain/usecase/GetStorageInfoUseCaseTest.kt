package com.ml.lansonesscan.domain.usecase

import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.ScanMetadata
import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.domain.repository.ScanRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetStorageInfoUseCaseTest {

    private lateinit var scanRepository: ScanRepository
    private lateinit var getStorageInfoUseCase: GetStorageInfoUseCase
    
    private val fruitScan = ScanResult.createHealthy(
        imagePath = "/test/fruit.jpg",
        analysisType = AnalysisType.FRUIT,
        confidenceLevel = 0.85f,
        metadata = ScanMetadata.create(1024L, "JPEG", 2000L, "1.0")
    )
    
    private val leafScan = ScanResult.createDiseased(
        imagePath = "/test/leaf.jpg",
        analysisType = AnalysisType.LEAVES,
        diseaseName = "Brown Spot",
        confidenceLevel = 0.92f,
        recommendations = listOf("Apply treatment"),
        metadata = ScanMetadata.create(2048L, "PNG", 3000L, "1.0")
    )

    @BeforeEach
    fun setUp() {
        scanRepository = mockk()
        getStorageInfoUseCase = GetStorageInfoUseCase(scanRepository)
    }

    @Test
    fun `invoke should return correct storage information`() = runTest {
        // Given
        coEvery { scanRepository.getScanCount() } returns 10
        coEvery { scanRepository.getDiseaseDetectedCount() } returns 3
        coEvery { scanRepository.getHealthyScansCount() } returns 7
        coEvery { scanRepository.getTotalStorageSize() } returns 5120L
        coEvery { scanRepository.getScansByAnalysisType(AnalysisType.FRUIT) } returns flowOf(listOf(fruitScan))
        coEvery { scanRepository.getScansByAnalysisType(AnalysisType.LEAVES) } returns flowOf(listOf(leafScan))

        // When
        val storageInfo = getStorageInfoUseCase()

        // Then
        assertEquals(10, storageInfo.totalScans)
        assertEquals(3, storageInfo.diseaseDetectedCount)
        assertEquals(7, storageInfo.healthyScansCount)
        assertEquals(5120L, storageInfo.totalStorageSize)
        assertEquals(1, storageInfo.fruitScansCount)
        assertEquals(1, storageInfo.leafScansCount)
        assertEquals("5 KB", storageInfo.getFormattedStorageSize())
        assertEquals(30.0f, storageInfo.getDiseaseDetectionRate())
        assertEquals(512L, storageInfo.getAverageStoragePerScan())
        assertEquals("512 B", storageInfo.getFormattedAverageStoragePerScan())
    }

    @Test
    fun `invoke should handle empty storage gracefully`() = runTest {
        // Given
        coEvery { scanRepository.getScanCount() } returns 0
        coEvery { scanRepository.getDiseaseDetectedCount() } returns 0
        coEvery { scanRepository.getHealthyScansCount() } returns 0
        coEvery { scanRepository.getTotalStorageSize() } returns 0L
        coEvery { scanRepository.getScansByAnalysisType(AnalysisType.FRUIT) } returns flowOf(emptyList())
        coEvery { scanRepository.getScansByAnalysisType(AnalysisType.LEAVES) } returns flowOf(emptyList())

        // When
        val storageInfo = getStorageInfoUseCase()

        // Then
        assertEquals(0, storageInfo.totalScans)
        assertEquals(0, storageInfo.diseaseDetectedCount)
        assertEquals(0, storageInfo.healthyScansCount)
        assertEquals(0L, storageInfo.totalStorageSize)
        assertEquals(0, storageInfo.fruitScansCount)
        assertEquals(0, storageInfo.leafScansCount)
        assertEquals("0 B", storageInfo.getFormattedStorageSize())
        assertEquals(0.0f, storageInfo.getDiseaseDetectionRate())
        assertEquals(0L, storageInfo.getAverageStoragePerScan())
    } 
   @Test
    fun `invoke should handle repository errors gracefully`() = runTest {
        // Given
        coEvery { scanRepository.getScanCount() } throws RuntimeException("Database error")

        // When
        val storageInfo = getStorageInfoUseCase()

        // Then
        assertEquals(0, storageInfo.totalScans)
        assertEquals(0, storageInfo.diseaseDetectedCount)
        assertEquals(0, storageInfo.healthyScansCount)
        assertEquals(0L, storageInfo.totalStorageSize)
        assertEquals(0, storageInfo.fruitScansCount)
        assertEquals(0, storageInfo.leafScansCount)
    }

    @Test
    fun `getStorageBreakdown should return correct breakdown`() = runTest {
        // Given
        coEvery { scanRepository.getScanCount() } returns 4
        coEvery { scanRepository.getDiseaseDetectedCount() } returns 1
        coEvery { scanRepository.getHealthyScansCount() } returns 3
        coEvery { scanRepository.getTotalStorageSize() } returns 4096L
        coEvery { scanRepository.getScansByAnalysisType(AnalysisType.FRUIT) } returns flowOf(listOf(fruitScan, fruitScan))
        coEvery { scanRepository.getScansByAnalysisType(AnalysisType.LEAVES) } returns flowOf(listOf(leafScan, leafScan))

        // When
        val breakdown = getStorageInfoUseCase.getStorageBreakdown()

        // Then
        assertEquals(AnalysisType.FRUIT, breakdown.fruitAnalysis.type)
        assertEquals(2, breakdown.fruitAnalysis.scanCount)
        assertEquals(2048L, breakdown.fruitAnalysis.estimatedStorageSize) // (4096 * 2) / 4
        assertEquals("2 KB", breakdown.fruitAnalysis.getFormattedStorageSize())
        
        assertEquals(AnalysisType.LEAVES, breakdown.leafAnalysis.type)
        assertEquals(2, breakdown.leafAnalysis.scanCount)
        assertEquals(2048L, breakdown.leafAnalysis.estimatedStorageSize) // (4096 * 2) / 4
        assertEquals("2 KB", breakdown.leafAnalysis.getFormattedStorageSize())
    }

    @Test
    fun `getStorageRecommendations should return high storage usage recommendation`() = runTest {
        // Given - High storage usage (> 100MB)
        coEvery { scanRepository.getScanCount() } returns 50
        coEvery { scanRepository.getDiseaseDetectedCount() } returns 10
        coEvery { scanRepository.getHealthyScansCount() } returns 40
        coEvery { scanRepository.getTotalStorageSize() } returns 150 * 1024 * 1024L // 150MB
        coEvery { scanRepository.getScansByAnalysisType(AnalysisType.FRUIT) } returns flowOf(listOf(fruitScan))
        coEvery { scanRepository.getScansByAnalysisType(AnalysisType.LEAVES) } returns flowOf(listOf(leafScan))
        coEvery { scanRepository.cleanupOrphanedImages() } returns Result.success(0)

        // When
        val recommendations = getStorageInfoUseCase.getStorageRecommendations()

        // Then
        assertTrue(recommendations.any { it.type == GetStorageInfoUseCase.RecommendationType.HIGH_STORAGE_USAGE })
        val highStorageRec = recommendations.first { it.type == GetStorageInfoUseCase.RecommendationType.HIGH_STORAGE_USAGE }
        assertEquals("High Storage Usage", highStorageRec.title)
        assertEquals(GetStorageInfoUseCase.RecommendationPriority.MEDIUM, highStorageRec.priority)
    }

    @Test
    fun `getStorageRecommendations should return too many scans recommendation`() = runTest {
        // Given - Many scans (> 100)
        coEvery { scanRepository.getScanCount() } returns 150
        coEvery { scanRepository.getDiseaseDetectedCount() } returns 30
        coEvery { scanRepository.getHealthyScansCount() } returns 120
        coEvery { scanRepository.getTotalStorageSize() } returns 50 * 1024 * 1024L // 50MB
        coEvery { scanRepository.getScansByAnalysisType(AnalysisType.FRUIT) } returns flowOf(listOf(fruitScan))
        coEvery { scanRepository.getScansByAnalysisType(AnalysisType.LEAVES) } returns flowOf(listOf(leafScan))
        coEvery { scanRepository.cleanupOrphanedImages() } returns Result.success(0)

        // When
        val recommendations = getStorageInfoUseCase.getStorageRecommendations()

        // Then
        assertTrue(recommendations.any { it.type == GetStorageInfoUseCase.RecommendationType.TOO_MANY_SCANS })
        val tooManyScansRec = recommendations.first { it.type == GetStorageInfoUseCase.RecommendationType.TOO_MANY_SCANS }
        assertEquals("Many Stored Scans", tooManyScansRec.title)
        assertTrue(tooManyScansRec.description.contains("150 scans"))
        assertEquals(GetStorageInfoUseCase.RecommendationPriority.LOW, tooManyScansRec.priority)
    }

    @Test
    fun `getStorageRecommendations should return orphaned files recommendation`() = runTest {
        // Given - Orphaned files detected
        coEvery { scanRepository.getScanCount() } returns 10
        coEvery { scanRepository.getDiseaseDetectedCount() } returns 2
        coEvery { scanRepository.getHealthyScansCount() } returns 8
        coEvery { scanRepository.getTotalStorageSize() } returns 10 * 1024 * 1024L // 10MB
        coEvery { scanRepository.getScansByAnalysisType(AnalysisType.FRUIT) } returns flowOf(listOf(fruitScan))
        coEvery { scanRepository.getScansByAnalysisType(AnalysisType.LEAVES) } returns flowOf(listOf(leafScan))
        coEvery { scanRepository.cleanupOrphanedImages() } returns Result.success(5)

        // When
        val recommendations = getStorageInfoUseCase.getStorageRecommendations()

        // Then
        assertTrue(recommendations.any { it.type == GetStorageInfoUseCase.RecommendationType.ORPHANED_FILES })
        val orphanedFilesRec = recommendations.first { it.type == GetStorageInfoUseCase.RecommendationType.ORPHANED_FILES }
        assertEquals("Orphaned Files Detected", orphanedFilesRec.title)
        assertTrue(orphanedFilesRec.description.contains("5 orphaned files"))
        assertEquals(GetStorageInfoUseCase.RecommendationPriority.HIGH, orphanedFilesRec.priority)
    }

    @Test
    fun `StorageInfo should format different storage sizes correctly`() {
        val storageInfo1 = GetStorageInfoUseCase.StorageInfo(1, 0, 1, 512L, 1, 0, System.currentTimeMillis())
        assertEquals("512 B", storageInfo1.getFormattedStorageSize())

        val storageInfo2 = GetStorageInfoUseCase.StorageInfo(1, 0, 1, 2048L, 1, 0, System.currentTimeMillis())
        assertEquals("2 KB", storageInfo2.getFormattedStorageSize())

        val storageInfo3 = GetStorageInfoUseCase.StorageInfo(1, 0, 1, 3145728L, 1, 0, System.currentTimeMillis())
        assertEquals("3 MB", storageInfo3.getFormattedStorageSize())

        val storageInfo4 = GetStorageInfoUseCase.StorageInfo(1, 0, 1, 2147483648L, 1, 0, System.currentTimeMillis())
        assertEquals("2 GB", storageInfo4.getFormattedStorageSize())
    }
}