package com.ml.lansonesscan.data.local.storage

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImageStorageManagerTest {

    private lateinit var tempDir: File
    private lateinit var mockContext: Context
    private lateinit var imageStorageManager: ImageStorageManager

    @BeforeEach
    fun setup() {
        tempDir = File.createTempFile("test", "dir").apply {
            delete()
            mkdirs()
        }
        
        mockContext = mockk(relaxed = true)
        every { mockContext.getExternalFilesDir(null) } returns tempDir
        
        imageStorageManager = ImageStorageManager(mockContext)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        tempDir.deleteRecursively()
    }

    @Test
    fun `getThumbnailPath should return correct path`() {
        // Given
        val originalPath = "/path/to/image.jpg"
        
        // When
        val thumbnailPath = imageStorageManager.getThumbnailPath(originalPath)
        
        // Then
        assertTrue(thumbnailPath.contains("thumbnails"))
        assertTrue(thumbnailPath.endsWith("image.jpg"))
    }

    @Test
    fun `getStorageInfo should return default values when no images exist`() = runTest {
        // When
        val storageInfo = imageStorageManager.getStorageInfo()
        
        // Then
        assertEquals(0, storageInfo.originalImagesCount)
        assertEquals(0, storageInfo.thumbnailsCount)
        assertEquals(0, storageInfo.cacheFilesCount)
        assertEquals(0L, storageInfo.originalImagesSize)
        assertEquals(0L, storageInfo.thumbnailsSize)
        assertEquals(0L, storageInfo.cacheSize)
        assertEquals(0L, storageInfo.totalSize)
    }

    @Test
    fun `clearCache should return true when cache directory exists`() = runTest {
        // Given - Create cache directory
        val cacheDir = File(tempDir, "images/cache")
        cacheDir.mkdirs()
        
        // When
        val result = imageStorageManager.clearCache()
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `clearAllImages should return true when directories exist`() = runTest {
        // Given - Create image directories
        val originalsDir = File(tempDir, "images/originals")
        val thumbnailsDir = File(tempDir, "images/thumbnails")
        val cacheDir = File(tempDir, "images/cache")
        originalsDir.mkdirs()
        thumbnailsDir.mkdirs()
        cacheDir.mkdirs()
        
        // When
        val result = imageStorageManager.clearAllImages()
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `deleteImage should return true for non-existent file`() = runTest {
        // When
        val result = imageStorageManager.deleteImage("/non/existent/path.jpg")
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `cleanupOrphanedImages should return zero when no files exist`() = runTest {
        // Given
        val referencedPaths = listOf("/path1.jpg", "/path2.jpg")
        
        // When
        val cleanedCount = imageStorageManager.cleanupOrphanedImages(referencedPaths)
        
        // Then
        assertEquals(0, cleanedCount)
    }

    @Test
    fun `getImage should return null for invalid path`() = runTest {
        // When
        val result = imageStorageManager.getImage("/invalid/path/image.jpg")
        
        // Then
        assertNull(result)
    }

    @Test
    fun `getThumbnail should return null for non-existent thumbnail`() = runTest {
        // When
        val result = imageStorageManager.getThumbnail("/non/existent/image.jpg")
        
        // Then
        assertNull(result)
    }

    @Test
    fun `saveImage should return null for invalid URI`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        every { mockContext.contentResolver.openInputStream(mockUri) } returns null
        
        // When
        val result = imageStorageManager.saveImage(mockUri)
        
        // Then
        assertNull(result)
    }

    @Test
    fun `StorageInfo getTotalSizeInMB should calculate correctly`() {
        // Given
        val storageInfo = StorageInfo(
            originalImagesCount = 2,
            thumbnailsCount = 2,
            cacheFilesCount = 1,
            originalImagesSize = 1024 * 1024, // 1 MB
            thumbnailsSize = 512 * 1024, // 0.5 MB
            cacheSize = 256 * 1024, // 0.25 MB
            totalSize = 1024 * 1024 + 512 * 1024 + 256 * 1024 // 1.75 MB
        )
        
        // When
        val sizeInMB = storageInfo.getTotalSizeInMB()
        
        // Then
        assertEquals(1.75f, sizeInMB, 0.01f)
    }

    @Test
    fun `StorageInfo default constructor should initialize with zeros`() {
        // When
        val storageInfo = StorageInfo()
        
        // Then
        assertEquals(0, storageInfo.originalImagesCount)
        assertEquals(0, storageInfo.thumbnailsCount)
        assertEquals(0, storageInfo.cacheFilesCount)
        assertEquals(0L, storageInfo.originalImagesSize)
        assertEquals(0L, storageInfo.thumbnailsSize)
        assertEquals(0L, storageInfo.cacheSize)
        assertEquals(0L, storageInfo.totalSize)
        assertEquals(0f, storageInfo.getTotalSizeInMB())
    }
}