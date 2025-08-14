package com.ml.lansonesscan.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ml.lansonesscan.data.local.dao.ScanDao
import com.ml.lansonesscan.data.local.database.LansonesDatabase
import com.ml.lansonesscan.data.local.entities.ScanResultEntity
import com.ml.lansonesscan.data.local.storage.ImageStorageManager
import com.ml.lansonesscan.domain.model.AnalysisType
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DataSyncManagerTest {
    
    private lateinit var database: LansonesDatabase
    private lateinit var scanDao: ScanDao
    private lateinit var mockImageStorageManager: ImageStorageManager
    private lateinit var mockContext: Context
    private lateinit var dataSyncManager: DataSyncManager
    
    @Before
    fun setup() {
        mockContext = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            mockContext,
            LansonesDatabase::class.java
        ).allowMainThreadQueries().build()
        
        scanDao = database.scanDao()
        mockImageStorageManager = mockk()
        dataSyncManager = DataSyncManager(
            scanDao = scanDao,
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
        val mockExternalFilesDir = mockk<File>()
        every { mockContext.getExternalFilesDir(null) } returns mockExternalFilesDir
        every { mockExternalFilesDir.absolutePath } returns "/test/external/files"
        
        coEvery { mockImageStorageManager.deleteImage(any()) } returns true
        coEvery { mockImageStorageManager.getAllImagePaths() } returns emptySet()
        coEvery { mockImageStorageManager.clearCache() } returns true
        
        val mockStorageInfo = com.ml.lansonesscan.data.local.storage.StorageInfo()
        coEvery { mockImageStorageManager.getStorageInfo() } returns mockStorageInfo
    }
    
    @Test
    fun `performDataCleanup should clean orphaned images`() = runTest {
        val scan1 = createTestScanEntity("scan1", "/path/image1.jpg")
        val scan2 = createTestScanEntity("scan2", "/path/image2.jpg")
        
        scanDao.insertScan(scan1)
        scanDao.insertScan(scan2)
        
        coEvery { 
            mockImageStorageManager.getAllImagePaths() 
        } returns setOf("/path/image1.jpg", "/path/image2.jpg", "/path/orphaned1.jpg", "/path/orphaned2.jpg")
        
        val result = dataSyncManager.performDataCleanup()
        
        assertTrue(result.success)
        assertEquals(2, result.deletedImages)
        assertTrue(result.cacheCleared)
        
        coVerify { mockImageStorageManager.deleteImage("/path/orphaned1.jpg") }
        coVerify { mockImageStorageManager.deleteImage("/path/orphaned2.jpg") }
        coVerify(exactly = 0) { mockImageStorageManager.deleteImage("/path/image1.jpg") }
        coVerify(exactly = 0) { mockImageStorageManager.deleteImage("/path/image2.jpg") }
    }
    
    private fun createTestScanEntity(id: String, imagePath: String, timestamp: Long = System.currentTimeMillis(), diseaseDetected: Boolean = true, diseaseName: String? = "Test Disease", analysisType: AnalysisType = AnalysisType.FRUIT, imageSize: Long = 1024L): ScanResultEntity {
        return ScanResultEntity(id, imagePath, analysisType, diseaseDetected, diseaseName, 0.85f, listOf("Test recommendation"), timestamp, imageSize, "JPEG", 2000L, "test-1.0")
    }
}