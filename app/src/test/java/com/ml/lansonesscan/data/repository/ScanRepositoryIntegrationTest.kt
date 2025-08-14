package com.ml.lansonesscan.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ml.lansonesscan.data.local.dao.ScanDao
import com.ml.lansonesscan.data.local.database.LansonesDatabase
import com.ml.lansonesscan.data.local.storage.ImageStorageManager
import com.ml.lansonesscan.data.remote.service.AnalysisResult
import com.ml.lansonesscan.data.remote.service.AnalysisService
import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.ScanMetadata
import com.ml.lansonesscan.domain.model.ScanResult
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ScanRepositoryIntegrationTest {
    
    private lateinit var database: LansonesDatabase
    private lateinit var scanDao: ScanDao
    private lateinit var mockAnalysisService: AnalysisService
    private lateinit var mockImageStorageManager: ImageStorageManager
    private lateinit var mockContext: Context
    private lateinit var repository: ScanRepositoryImpl
    
    private lateinit var mockUri: Uri
    
    @Before
    fun setup() {
        mockContext = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            mockContext,
            LansonesDatabase::class.java
        ).allowMainThreadQueries().build()
        
        scanDao = database.scanDao()
        mockAnalysisService = mockk()
        mockImageStorageManager = mockk()
        mockUri = mockk()
        
        repository = ScanRepositoryImpl(
            scanDao = scanDao,
            analysisService = mockAnalysisService,
            imageStorageManager = mockImageStorageManager,
            context = mockContext
        )
        
        setupCommonMocks()
    }
    
    @After
    fun tearDown() {
        database.close()
        clearAllMocks()
    }
    
    private fun setupCommonMocks() {
        val mockContentResolver = mockk<android.content.ContentResolver>()
        every { mockContext.contentResolver } returns mockContentResolver
        
        val testImageBytes = "test image data".toByteArray()
        every { 
            mockContentResolver.openInputStream(any()) 
        } returns ByteArrayInputStream(testImageBytes)
        
        every { 
            mockContentResolver.getType(any()) 
        } returns "image/jpeg"
        
        coEvery { 
            mockImageStorageManager.saveImage(any<ByteArray>(), any(), any()) 
        } returns "/test/path/image.jpg"
        
        coEvery { 
            mockImageStorageManager.deleteImage(any()) 
        } returns true
        
        coEvery { 
            mockImageStorageManager.getAllImagePaths() 
        } returns emptySet()
    }
    
    @Test
    fun `analyzeLansonesImage should save successful analysis result`() = runTest {
        val analysisResult = AnalysisResult(
            diseaseDetected = true,
            diseaseName = "Anthracnose",
            confidenceLevel = 0.85f,
            affectedPart = "fruit",
            symptoms = listOf("Black spots", "Brown patches"),
            recommendations = listOf("Apply fungicide", "Remove affected parts"),
            severity = "medium",
            rawResponse = "Test response"
        )
        
        coEvery { 
            mockAnalysisService.analyzeImage(any(), any(), any(), any()) 
        } returns Result.success(analysisResult)
        
        val result = repository.analyzeLansonesImage(mockUri, AnalysisType.FRUIT)
        
        assertTrue(result.isSuccess)
        val scanResult = result.getOrThrow()
        
        assertEquals(AnalysisType.FRUIT, scanResult.analysisType)
        assertTrue(scanResult.diseaseDetected)
        assertEquals("Anthracnose", scanResult.diseaseName)
        assertEquals(0.85f, scanResult.confidenceLevel)
        assertEquals(2, scanResult.recommendations.size)
        
        val savedScan = scanDao.getScanById(scanResult.id)
        assertNotNull(savedScan)
        assertEquals(scanResult.id, savedScan!!.id)
    }
    
    private fun createTestScanResult(diseaseDetected: Boolean = true, diseaseName: String? = "Test Disease", analysisType: AnalysisType = AnalysisType.FRUIT): ScanResult {
        val metadata = ScanMetadata.create(1024L, "JPEG", 2000L, "test-1.0")
        return if (diseaseDetected) {
            ScanResult.createDiseased("/test/path/image_${System.currentTimeMillis()}.jpg", analysisType, diseaseName ?: "Test Disease", 0.85f, listOf("Test recommendation"), metadata)
        } else {
            ScanResult.createHealthy("/test/path/image_${System.currentTimeMillis()}.jpg", analysisType, 0.95f, metadata)
        }
    }
}