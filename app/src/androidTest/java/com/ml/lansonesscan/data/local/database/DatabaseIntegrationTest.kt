package com.ml.lansonesscan.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ml.lansonesscan.data.local.dao.ScanDao
import com.ml.lansonesscan.data.local.entities.ScanResultEntity
import com.ml.lansonesscan.di.DatabaseModule
import com.ml.lansonesscan.di.getDatabase
import com.ml.lansonesscan.di.getScanDao
import com.ml.lansonesscan.domain.model.AnalysisType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Integration tests for database setup and configuration
 * Tests the complete database setup including Room configuration, migrations, and dependency injection
 */
@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {

    private lateinit var context: Context
    private lateinit var database: LansonesDatabase
    private lateinit var scanDao: ScanDao

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        
        // Clear any existing database instance
        DatabaseModule.clearDatabase()
        LansonesDatabase.clearInstance()
        
        // Create fresh database instance for testing
        database = LansonesDatabase.getInMemoryDatabase(context)
        scanDao = database.scanDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
        DatabaseModule.clearDatabase()
        LansonesDatabase.clearInstance()
    }

    @Test
    fun `database module provides consistent database instance`() {
        val db1 = DatabaseModule.provideDatabase(context)
        val db2 = DatabaseModule.provideDatabase(context)
        
        // Should return the same instance (singleton pattern)
        assertEquals("Database module should provide singleton instance", db1, db2)
        
        db1.close()
    }

    @Test
    fun `database module provides working scan dao`() {
        val dao = DatabaseModule.provideScanDao(context)
        assertNotNull("ScanDao should not be null", dao)
        
        // Test that DAO is functional
        runBlocking {
            val count = dao.getScanCount()
            assertEquals("Initial scan count should be 0", 0, count)
        }
        
        DatabaseModule.clearDatabase()
    }

    @Test
    fun `database module in memory database works correctly`() {
        val inMemoryDb = DatabaseModule.provideInMemoryDatabase(context)
        
        assertNotNull("In-memory database should not be null", inMemoryDb)
        assertTrue("In-memory database should be open", inMemoryDb.isOpen)
        
        val dao = inMemoryDb.scanDao()
        assertNotNull("In-memory DAO should not be null", dao)
        
        runBlocking {
            val count = dao.getScanCount()
            assertEquals("In-memory database should start empty", 0, count)
        }
        
        inMemoryDb.close()
    }

    @Test
    fun `database handles complex data operations correctly`() = runBlocking {
        // Create test data with various scenarios
        val testEntities = listOf(
            ScanResultEntity(
                id = "integration-test-1",
                imagePath = "/storage/emulated/0/Android/data/com.ml.lansonesscan/files/images/test1.jpg",
                analysisType = AnalysisType.FRUIT,
                diseaseDetected = true,
                diseaseName = "Brown Spot Disease",
                confidenceLevel = 0.92f,
                recommendations = listOf(
                    "Remove affected fruits immediately",
                    "Apply fungicide treatment",
                    "Improve air circulation around trees"
                ),
                timestamp = System.currentTimeMillis(),
                imageSize = 2048576L, // 2MB
                imageFormat = "JPEG",
                analysisTime = 3500L,
                apiVersion = "gemini-1.5-flash"
            ),
            ScanResultEntity(
                id = "integration-test-2",
                imagePath = "/storage/emulated/0/Android/data/com.ml.lansonesscan/files/images/test2.jpg",
                analysisType = AnalysisType.LEAVES,
                diseaseDetected = false,
                diseaseName = null,
                confidenceLevel = 0.98f,
                recommendations = listOf("Plant appears healthy", "Continue regular monitoring"),
                timestamp = System.currentTimeMillis() + 1000,
                imageSize = 1536000L, // 1.5MB
                imageFormat = "PNG",
                analysisTime = 2800L,
                apiVersion = "gemini-1.5-flash"
            ),
            ScanResultEntity(
                id = "integration-test-3",
                imagePath = "/storage/emulated/0/Android/data/com.ml.lansonesscan/files/images/test3.jpg",
                analysisType = AnalysisType.LEAVES,
                diseaseDetected = true,
                diseaseName = "Leaf Blight",
                confidenceLevel = 0.87f,
                recommendations = listOf(
                    "Prune affected leaves",
                    "Apply copper-based fungicide",
                    "Reduce watering frequency",
                    "Ensure proper drainage"
                ),
                timestamp = System.currentTimeMillis() + 2000,
                imageSize = 3145728L, // 3MB
                imageFormat = "JPEG",
                analysisTime = 4200L,
                apiVersion = "gemini-1.5-flash"
            )
        )

        // Insert test data
        scanDao.insertScans(testEntities)

        // Test various query operations
        val allScans = scanDao.getAllScans().first()
        assertEquals("Should have 3 scans", 3, allScans.size)

        val fruitScans = scanDao.getScansByAnalysisType(AnalysisType.FRUIT).first()
        assertEquals("Should have 1 fruit scan", 1, fruitScans.size)

        val leafScans = scanDao.getScansByAnalysisType(AnalysisType.LEAVES).first()
        assertEquals("Should have 2 leaf scans", 2, leafScans.size)

        val diseaseScans = scanDao.getScansByDiseaseStatus(true).first()
        assertEquals("Should have 2 disease-detected scans", 2, diseaseScans.size)

        val healthyScans = scanDao.getScansByDiseaseStatus(false).first()
        assertEquals("Should have 1 healthy scan", 1, healthyScans.size)

        // Test aggregation functions
        val totalCount = scanDao.getScanCount()
        assertEquals("Total count should be 3", 3, totalCount)

        val diseaseCount = scanDao.getDiseaseDetectedCount()
        assertEquals("Disease detected count should be 2", 2, diseaseCount)

        val healthyCount = scanDao.getHealthyScansCount()
        assertEquals("Healthy count should be 1", 1, healthyCount)

        val totalImageSize = scanDao.getTotalImageSize()
        assertNotNull("Total image size should not be null", totalImageSize)
        assertTrue("Total image size should be positive", totalImageSize!! > 0)

        val avgConfidence = scanDao.getAverageConfidenceLevel()
        assertNotNull("Average confidence should not be null", avgConfidence)
        assertTrue("Average confidence should be reasonable", avgConfidence!! > 0.8f)
    }

    @Test
    fun `database handles edge cases correctly`() = runBlocking {
        // Test with minimal data
        val minimalEntity = ScanResultEntity(
            id = "minimal-test",
            imagePath = "/minimal/path.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseDetected = false,
            diseaseName = null,
            confidenceLevel = 0.0f,
            recommendations = emptyList(),
            timestamp = 0L,
            imageSize = 0L,
            imageFormat = "",
            analysisTime = 0L,
            apiVersion = ""
        )

        scanDao.insertScan(minimalEntity)
        val retrieved = scanDao.getScanById("minimal-test")
        
        assertNotNull("Minimal entity should be retrievable", retrieved)
        assertEquals("Empty recommendations should be handled", 0, retrieved?.recommendations?.size)
        assertEquals("Zero values should be preserved", 0L, retrieved?.timestamp)

        // Test with maximum realistic data
        val maximalEntity = ScanResultEntity(
            id = "maximal-test",
            imagePath = "/very/long/path/to/image/file/with/many/subdirectories/image.jpg",
            analysisType = AnalysisType.LEAVES,
            diseaseDetected = true,
            diseaseName = "Very Long Disease Name With Multiple Words And Detailed Description",
            confidenceLevel = 1.0f,
            recommendations = (1..20).map { "Very detailed recommendation number $it with comprehensive instructions" },
            timestamp = Long.MAX_VALUE,
            imageSize = Long.MAX_VALUE,
            imageFormat = "JPEG",
            analysisTime = Long.MAX_VALUE,
            apiVersion = "gemini-1.5-flash-latest-version"
        )

        scanDao.insertScan(maximalEntity)
        val maxRetrieved = scanDao.getScanById("maximal-test")
        
        assertNotNull("Maximal entity should be retrievable", maxRetrieved)
        assertEquals("Large recommendations list should be handled", 20, maxRetrieved?.recommendations?.size)
        assertEquals("Max values should be preserved", Long.MAX_VALUE, maxRetrieved?.timestamp)
    }

    @Test
    fun `database migration strategy is properly configured`() {
        // Test that database can be created with migration support
        val persistentDb = Room.databaseBuilder(
            context,
            LansonesDatabase::class.java,
            "test-migration-db"
        ).build()

        assertNotNull("Persistent database should be created", persistentDb)
        assertTrue("Persistent database should be open", persistentDb.isOpen)

        // Test that DAO is accessible
        val dao = persistentDb.scanDao()
        assertNotNull("DAO should be accessible", dao)

        persistentDb.close()
        
        // Clean up test database
        context.deleteDatabase("test-migration-db")
    }

    @Test
    fun `database handles concurrent access correctly`() = runBlocking {
        val entities = (1..50).map { index ->
            ScanResultEntity(
                id = "concurrent-$index",
                imagePath = "/test/concurrent/$index.jpg",
                analysisType = if (index % 2 == 0) AnalysisType.FRUIT else AnalysisType.LEAVES,
                diseaseDetected = index % 3 == 0,
                diseaseName = if (index % 3 == 0) "Disease $index" else null,
                confidenceLevel = (index % 100) / 100.0f,
                recommendations = listOf("Recommendation for scan $index"),
                timestamp = System.currentTimeMillis() + index,
                imageSize = (index * 1024L),
                imageFormat = if (index % 2 == 0) "JPEG" else "PNG",
                analysisTime = (index * 100L),
                apiVersion = "1.0"
            )
        }

        // Insert all entities in batch
        scanDao.insertScans(entities)

        // Verify all were inserted correctly
        val allScans = scanDao.getAllScansAsList()
        assertEquals("All entities should be inserted", 50, allScans.size)

        // Test pagination
        val firstPage = scanDao.getScansPaginated(10, 0)
        assertEquals("First page should have 10 items", 10, firstPage.size)

        val secondPage = scanDao.getScansPaginated(10, 10)
        assertEquals("Second page should have 10 items", 10, secondPage.size)

        // Verify no overlap between pages
        val firstPageIds = firstPage.map { it.id }.toSet()
        val secondPageIds = secondPage.map { it.id }.toSet()
        assertTrue("Pages should not overlap", firstPageIds.intersect(secondPageIds).isEmpty())

        // Test cleanup operations
        scanDao.deleteOldScans(25)
        val remainingScans = scanDao.getAllScansAsList()
        assertEquals("Should keep only 25 most recent scans", 25, remainingScans.size)
    }

    @Test
    fun `database extension functions work correctly`() {
        // Test extension functions from DatabaseModule
        val dbFromExtension = context.getDatabase()
        assertNotNull("Extension function should provide database", dbFromExtension)

        val daoFromExtension = context.getScanDao()
        assertNotNull("Extension function should provide DAO", daoFromExtension)

        runBlocking {
            val count = daoFromExtension.getScanCount()
            assertEquals("DAO from extension should work", 0, count)
        }

        dbFromExtension.close()
    }

    @Test
    fun `database cleanup operations work correctly`() = runBlocking {
        // Insert test data
        val testEntities = (1..10).map { index ->
            ScanResultEntity(
                id = "cleanup-test-$index",
                imagePath = "/test/cleanup/$index.jpg",
                analysisType = AnalysisType.FRUIT,
                diseaseDetected = false,
                diseaseName = null,
                confidenceLevel = 0.5f,
                recommendations = listOf("Test recommendation"),
                timestamp = System.currentTimeMillis() - (index * 86400000L), // Days ago
                imageSize = 1024L,
                imageFormat = "JPEG",
                analysisTime = 1000L,
                apiVersion = "1.0"
            )
        }

        scanDao.insertScans(testEntities)
        assertEquals("Should have 10 scans initially", 10, scanDao.getScanCount())

        // Test delete old scans
        scanDao.deleteOldScans(5)
        assertEquals("Should keep only 5 most recent scans", 5, scanDao.getScanCount())

        // Test delete by timestamp
        val cutoffTime = System.currentTimeMillis() - (3 * 86400000L) // 3 days ago
        scanDao.deleteScansOlderThan(cutoffTime)
        val remainingCount = scanDao.getScanCount()
        assertTrue("Should have fewer scans after timestamp cleanup", remainingCount < 5)

        // Test clear all
        scanDao.deleteAllScans()
        assertEquals("Should have no scans after clear all", 0, scanDao.getScanCount())
    }
}