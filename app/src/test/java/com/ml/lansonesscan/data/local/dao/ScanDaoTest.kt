package com.ml.lansonesscan.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ml.lansonesscan.data.local.database.Converters
import com.ml.lansonesscan.data.local.database.LansonesDatabase
import com.ml.lansonesscan.data.local.entities.ScanResultEntity
import com.ml.lansonesscan.domain.model.AnalysisType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ScanDaoTest {
    
    private lateinit var database: LansonesDatabase
    private lateinit var scanDao: ScanDao
    
    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LansonesDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        
        scanDao = database.scanDao()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    private fun createTestScanEntity(
        id: String = "test-id",
        analysisType: AnalysisType = AnalysisType.FRUIT,
        diseaseDetected: Boolean = false,
        diseaseName: String? = null,
        confidenceLevel: Float = 0.9f,
        timestamp: Long = System.currentTimeMillis()
    ): ScanResultEntity {
        return ScanResultEntity(
            id = id,
            imagePath = "/path/to/$id.jpg",
            analysisType = analysisType,
            diseaseDetected = diseaseDetected,
            diseaseName = diseaseName,
            confidenceLevel = confidenceLevel,
            recommendations = listOf("Test recommendation"),
            timestamp = timestamp,
            imageSize = 1024L,
            imageFormat = "JPEG",
            analysisTime = 2000L,
            apiVersion = "1.0"
        )
    }
    
    @Test
    fun `insertScan inserts scan successfully`() = runTest {
        // Given
        val scan = createTestScanEntity()
        
        // When
        scanDao.insertScan(scan)
        
        // Then
        val retrievedScan = scanDao.getScanById(scan.id)
        assertNotNull(retrievedScan)
        assertEquals(scan.id, retrievedScan?.id)
        assertEquals(scan.imagePath, retrievedScan?.imagePath)
    }
    
    @Test
    fun `insertScans inserts multiple scans successfully`() = runTest {
        // Given
        val scans = listOf(
            createTestScanEntity("scan1"),
            createTestScanEntity("scan2"),
            createTestScanEntity("scan3")
        )
        
        // When
        scanDao.insertScans(scans)
        
        // Then
        val allScans = scanDao.getAllScansAsList()
        assertEquals(3, allScans.size)
        assertTrue(allScans.any { it.id == "scan1" })
        assertTrue(allScans.any { it.id == "scan2" })
        assertTrue(allScans.any { it.id == "scan3" })
    }
    
    @Test
    fun `updateScan updates existing scan`() = runTest {
        // Given
        val originalScan = createTestScanEntity()
        scanDao.insertScan(originalScan)
        
        val updatedScan = originalScan.copy(
            confidenceLevel = 0.95f,
            diseaseName = "Updated Disease"
        )
        
        // When
        scanDao.updateScan(updatedScan)
        
        // Then
        val retrievedScan = scanDao.getScanById(originalScan.id)
        assertNotNull(retrievedScan)
        assertEquals(0.95f, retrievedScan?.confidenceLevel ?: 0f, 0.001f)
        assertEquals("Updated Disease", retrievedScan?.diseaseName)
    }
    
    @Test
    fun `deleteScanById removes scan successfully`() = runTest {
        // Given
        val scan = createTestScanEntity()
        scanDao.insertScan(scan)
        
        // When
        scanDao.deleteScanById(scan.id)
        
        // Then
        val retrievedScan = scanDao.getScanById(scan.id)
        assertNull(retrievedScan)
    }
    
    @Test
    fun `deleteScan removes scan entity successfully`() = runTest {
        // Given
        val scan = createTestScanEntity()
        scanDao.insertScan(scan)
        
        // When
        scanDao.deleteScan(scan)
        
        // Then
        val retrievedScan = scanDao.getScanById(scan.id)
        assertNull(retrievedScan)
    }
    
    @Test
    fun `deleteAllScans removes all scans`() = runTest {
        // Given
        val scans = listOf(
            createTestScanEntity("scan1"),
            createTestScanEntity("scan2"),
            createTestScanEntity("scan3")
        )
        scanDao.insertScans(scans)
        
        // When
        scanDao.deleteAllScans()
        
        // Then
        val allScans = scanDao.getAllScansAsList()
        assertTrue(allScans.isEmpty())
    }
    
    @Test
    fun `getAllScans returns scans ordered by timestamp desc`() = runTest {
        // Given
        val scan1 = createTestScanEntity("scan1", timestamp = 1000L)
        val scan2 = createTestScanEntity("scan2", timestamp = 3000L)
        val scan3 = createTestScanEntity("scan3", timestamp = 2000L)
        
        scanDao.insertScans(listOf(scan1, scan2, scan3))
        
        // When
        val scans = scanDao.getAllScans().first()
        
        // Then
        assertEquals(3, scans.size)
        assertEquals("scan2", scans[0].id) // Most recent
        assertEquals("scan3", scans[1].id) // Middle
        assertEquals("scan1", scans[2].id) // Oldest
    }
    
    @Test
    fun `getScansByAnalysisType filters correctly`() = runTest {
        // Given
        val fruitScan = createTestScanEntity("fruit", analysisType = AnalysisType.FRUIT)
        val leafScan = createTestScanEntity("leaf", analysisType = AnalysisType.LEAVES)
        
        scanDao.insertScans(listOf(fruitScan, leafScan))
        
        // When
        val fruitScans = scanDao.getScansByAnalysisType(AnalysisType.FRUIT).first()
        val leafScans = scanDao.getScansByAnalysisType(AnalysisType.LEAVES).first()
        
        // Then
        assertEquals(1, fruitScans.size)
        assertEquals("fruit", fruitScans[0].id)
        
        assertEquals(1, leafScans.size)
        assertEquals("leaf", leafScans[0].id)
    }
    
    @Test
    fun `getScansByDiseaseStatus filters correctly`() = runTest {
        // Given
        val healthyScan = createTestScanEntity("healthy", diseaseDetected = false)
        val diseasedScan = createTestScanEntity("diseased", diseaseDetected = true, diseaseName = "Test Disease")
        
        scanDao.insertScans(listOf(healthyScan, diseasedScan))
        
        // When
        val healthyScans = scanDao.getScansByDiseaseStatus(false).first()
        val diseasedScans = scanDao.getScansByDiseaseStatus(true).first()
        
        // Then
        assertEquals(1, healthyScans.size)
        assertEquals("healthy", healthyScans[0].id)
        
        assertEquals(1, diseasedScans.size)
        assertEquals("diseased", diseasedScans[0].id)
    }
    
    @Test
    fun `getRecentScans limits results correctly`() = runTest {
        // Given
        val scans = (1..10).map { 
            createTestScanEntity("scan$it", timestamp = it.toLong() * 1000)
        }
        scanDao.insertScans(scans)
        
        // When
        val recentScans = scanDao.getRecentScans(5).first()
        
        // Then
        assertEquals(5, recentScans.size)
        // Should be the 5 most recent (highest timestamps)
        assertEquals("scan10", recentScans[0].id)
        assertEquals("scan9", recentScans[1].id)
        assertEquals("scan8", recentScans[2].id)
        assertEquals("scan7", recentScans[3].id)
        assertEquals("scan6", recentScans[4].id)
    }
    
    @Test
    fun `getScansInDateRange filters by timestamp correctly`() = runTest {
        // Given
        val scan1 = createTestScanEntity("scan1", timestamp = 1000L)
        val scan2 = createTestScanEntity("scan2", timestamp = 2000L)
        val scan3 = createTestScanEntity("scan3", timestamp = 3000L)
        val scan4 = createTestScanEntity("scan4", timestamp = 4000L)
        
        scanDao.insertScans(listOf(scan1, scan2, scan3, scan4))
        
        // When
        val scansInRange = scanDao.getScansInDateRange(1500L, 3500L).first()
        
        // Then
        assertEquals(2, scansInRange.size)
        assertTrue(scansInRange.any { it.id == "scan2" })
        assertTrue(scansInRange.any { it.id == "scan3" })
    }
    
    @Test
    fun `getHighConfidenceDiseaseScans filters correctly`() = runTest {
        // Given
        val lowConfidenceScan = createTestScanEntity("low", diseaseDetected = true, diseaseName = "Disease", confidenceLevel = 0.5f)
        val highConfidenceScan = createTestScanEntity("high", diseaseDetected = true, diseaseName = "Disease", confidenceLevel = 0.9f)
        val healthyScan = createTestScanEntity("healthy", diseaseDetected = false, confidenceLevel = 0.95f)
        
        scanDao.insertScans(listOf(lowConfidenceScan, highConfidenceScan, healthyScan))
        
        // When
        val highConfidenceScans = scanDao.getHighConfidenceDiseaseScans(0.8f).first()
        
        // Then
        assertEquals(1, highConfidenceScans.size)
        assertEquals("high", highConfidenceScans[0].id)
    }
    
    @Test
    fun `getScanCount returns correct count`() = runTest {
        // Given
        val scans = listOf(
            createTestScanEntity("scan1"),
            createTestScanEntity("scan2"),
            createTestScanEntity("scan3")
        )
        scanDao.insertScans(scans)
        
        // When
        val count = scanDao.getScanCount()
        
        // Then
        assertEquals(3, count)
    }
    
    @Test
    fun `getScanCountByType returns correct counts`() = runTest {
        // Given
        val fruitScans = listOf(
            createTestScanEntity("fruit1", analysisType = AnalysisType.FRUIT),
            createTestScanEntity("fruit2", analysisType = AnalysisType.FRUIT)
        )
        val leafScans = listOf(
            createTestScanEntity("leaf1", analysisType = AnalysisType.LEAVES)
        )
        
        scanDao.insertScans(fruitScans + leafScans)
        
        // When
        val fruitCount = scanDao.getScanCountByType(AnalysisType.FRUIT)
        val leafCount = scanDao.getScanCountByType(AnalysisType.LEAVES)
        
        // Then
        assertEquals(2, fruitCount)
        assertEquals(1, leafCount)
    }
    
    @Test
    fun `getDiseaseDetectedCount returns correct count`() = runTest {
        // Given
        val scans = listOf(
            createTestScanEntity("healthy1", diseaseDetected = false),
            createTestScanEntity("diseased1", diseaseDetected = true, diseaseName = "Disease"),
            createTestScanEntity("diseased2", diseaseDetected = true, diseaseName = "Disease"),
            createTestScanEntity("healthy2", diseaseDetected = false)
        )
        scanDao.insertScans(scans)
        
        // When
        val diseaseCount = scanDao.getDiseaseDetectedCount()
        val healthyCount = scanDao.getHealthyScansCount()
        
        // Then
        assertEquals(2, diseaseCount)
        assertEquals(2, healthyCount)
    }
    
    @Test
    fun `getMostRecentScan returns latest scan`() = runTest {
        // Given
        val scan1 = createTestScanEntity("scan1", timestamp = 1000L)
        val scan2 = createTestScanEntity("scan2", timestamp = 3000L)
        val scan3 = createTestScanEntity("scan3", timestamp = 2000L)
        
        scanDao.insertScans(listOf(scan1, scan2, scan3))
        
        // When
        val mostRecent = scanDao.getMostRecentScan()
        
        // Then
        assertNotNull(mostRecent)
        assertEquals("scan2", mostRecent?.id)
    }
    
    @Test
    fun `getScansByDiseaseName filters correctly`() = runTest {
        // Given
        val scan1 = createTestScanEntity("scan1", diseaseDetected = true, diseaseName = "Brown Spot")
        val scan2 = createTestScanEntity("scan2", diseaseDetected = true, diseaseName = "Leaf Blight")
        val scan3 = createTestScanEntity("scan3", diseaseDetected = true, diseaseName = "Brown Spot")
        
        scanDao.insertScans(listOf(scan1, scan2, scan3))
        
        // When
        val brownSpotScans = scanDao.getScansByDiseaseName("Brown Spot").first()
        
        // Then
        assertEquals(2, brownSpotScans.size)
        assertTrue(brownSpotScans.all { it.diseaseName == "Brown Spot" })
    }
    
    @Test
    fun `searchScansByDiseaseName performs case insensitive search`() = runTest {
        // Given
        val scan1 = createTestScanEntity("scan1", diseaseDetected = true, diseaseName = "Brown Spot Disease")
        val scan2 = createTestScanEntity("scan2", diseaseDetected = true, diseaseName = "Leaf Blight")
        val scan3 = createTestScanEntity("scan3", diseaseDetected = true, diseaseName = "Anthracnose Spot")
        
        scanDao.insertScans(listOf(scan1, scan2, scan3))
        
        // When
        val spotScans = scanDao.searchScansByDiseaseName("spot").first()
        
        // Then
        assertEquals(2, spotScans.size)
        assertTrue(spotScans.any { it.diseaseName == "Brown Spot Disease" })
        assertTrue(spotScans.any { it.diseaseName == "Anthracnose Spot" })
    }
    
    @Test
    fun `getTotalImageSize calculates correctly`() = runTest {
        // Given
        val scans = listOf(
            createTestScanEntity("scan1").copy(imageSize = 1000L),
            createTestScanEntity("scan2").copy(imageSize = 2000L),
            createTestScanEntity("scan3").copy(imageSize = 3000L)
        )
        scanDao.insertScans(scans)
        
        // When
        val totalSize = scanDao.getTotalImageSize()
        
        // Then
        assertEquals(6000L, totalSize)
    }
    
    @Test
    fun `getAverageConfidenceLevel calculates correctly`() = runTest {
        // Given
        val scans = listOf(
            createTestScanEntity("scan1", confidenceLevel = 0.8f),
            createTestScanEntity("scan2", confidenceLevel = 0.9f),
            createTestScanEntity("scan3", confidenceLevel = 0.7f)
        )
        scanDao.insertScans(scans)
        
        // When
        val avgConfidence = scanDao.getAverageConfidenceLevel()
        
        // Then
        assertNotNull(avgConfidence)
        assertEquals(0.8f, avgConfidence!!, 0.01f)
    }
    
    @Test
    fun `getScansPaginated returns correct page`() = runTest {
        // Given
        val scans = (1..10).map { 
            createTestScanEntity("scan$it", timestamp = it.toLong() * 1000)
        }
        scanDao.insertScans(scans)
        
        // When
        val firstPage = scanDao.getScansPaginated(limit = 3, offset = 0)
        val secondPage = scanDao.getScansPaginated(limit = 3, offset = 3)
        
        // Then
        assertEquals(3, firstPage.size)
        assertEquals(3, secondPage.size)
        
        // Should be ordered by timestamp desc
        assertEquals("scan10", firstPage[0].id)
        assertEquals("scan9", firstPage[1].id)
        assertEquals("scan8", firstPage[2].id)
        
        assertEquals("scan7", secondPage[0].id)
        assertEquals("scan6", secondPage[1].id)
        assertEquals("scan5", secondPage[2].id)
    }
    
    @Test
    fun `deleteOldScans keeps only specified count`() = runTest {
        // Given
        val scans = (1..10).map { 
            createTestScanEntity("scan$it", timestamp = it.toLong() * 1000)
        }
        scanDao.insertScans(scans)
        
        // When
        scanDao.deleteOldScans(keepCount = 5)
        
        // Then
        val remainingScans = scanDao.getAllScansAsList()
        assertEquals(5, remainingScans.size)
        
        // Should keep the 5 most recent
        val remainingIds = remainingScans.map { it.id }.toSet()
        assertTrue(remainingIds.contains("scan10"))
        assertTrue(remainingIds.contains("scan9"))
        assertTrue(remainingIds.contains("scan8"))
        assertTrue(remainingIds.contains("scan7"))
        assertTrue(remainingIds.contains("scan6"))
    }
    
    @Test
    fun `deleteScansOlderThan removes old scans`() = runTest {
        // Given
        val scans = listOf(
            createTestScanEntity("old1", timestamp = 1000L),
            createTestScanEntity("old2", timestamp = 2000L),
            createTestScanEntity("new1", timestamp = 5000L),
            createTestScanEntity("new2", timestamp = 6000L)
        )
        scanDao.insertScans(scans)
        
        // When
        scanDao.deleteScansOlderThan(cutoffTime = 3000L)
        
        // Then
        val remainingScans = scanDao.getAllScansAsList()
        assertEquals(2, remainingScans.size)
        
        val remainingIds = remainingScans.map { it.id }.toSet()
        assertTrue(remainingIds.contains("new1"))
        assertTrue(remainingIds.contains("new2"))
    }
}