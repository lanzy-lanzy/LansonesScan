package com.ml.lansonesscan.data.local.preprocessing

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for ImagePreprocessor focusing on preprocessing logic without Android dependencies
 */
class ImagePreprocessorUnitTest {

    private lateinit var mockContext: Context
    private lateinit var mockContentResolver: ContentResolver
    private lateinit var imagePreprocessor: ImagePreprocessor

    @BeforeEach
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockContentResolver = mockk(relaxed = true)
        
        every { mockContext.contentResolver } returns mockContentResolver
        
        imagePreprocessor = ImagePreprocessor(mockContext)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `preprocessForApi should fail for invalid URI`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        every { mockContentResolver.openInputStream(mockUri) } returns null
        
        // When
        val result = imagePreprocessor.preprocessForApi(mockUri)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(PreprocessingError.INVALID_URI, result.error)
    }

    @Test
    fun `preprocessForApi should fail for corrupted image`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        val corruptedBytes = ByteArray(1024) { 0xFF.toByte() }
        val inputStream = ByteArrayInputStream(corruptedBytes)
        
        every { mockContentResolver.openInputStream(mockUri) } returns inputStream
        
        // When
        val result = imagePreprocessor.preprocessForApi(mockUri)
        
        // Then
        assertTrue(result.isFailure)
        // The error might be DECODE_FAILED or UNKNOWN_ERROR depending on how BitmapFactory handles it
        assertTrue(result.error == PreprocessingError.DECODE_FAILED || result.error == PreprocessingError.UNKNOWN_ERROR)
    }

    @Test
    fun `preprocessForApi should handle IO exception`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        every { mockContentResolver.openInputStream(mockUri) } throws IOException("Test IO error")
        
        // When
        val result = imagePreprocessor.preprocessForApi(mockUri)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(PreprocessingError.IO_ERROR, result.error)
    }

    @Test
    fun `preprocessForApi should handle out of memory error`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        every { mockContentResolver.openInputStream(mockUri) } throws OutOfMemoryError("Test OOM error")
        
        // When
        val result = imagePreprocessor.preprocessForApi(mockUri)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(PreprocessingError.OUT_OF_MEMORY, result.error)
    }

    @Test
    fun `compressForStorage should compress bitmap successfully`() = runTest {
        // Given - Mock a bitmap since we can't create real ones in unit tests
        val mockBitmap = mockk<Bitmap>()
        every { mockBitmap.compress(any(), any(), any()) } answers {
            val outputStream = thirdArg<java.io.ByteArrayOutputStream>()
            outputStream.write(ByteArray(100) { 0x01 }) // Write some test data
            true
        }
        
        // When
        val compressedBytes = imagePreprocessor.compressForStorage(mockBitmap, 85)
        
        // Then
        assertNotNull(compressedBytes)
        assertTrue(compressedBytes.isNotEmpty())
    }

    @Test
    fun `compressForStorage should return null for invalid bitmap`() = runTest {
        // Given - Mock a bitmap that fails to compress
        val mockBitmap = mockk<Bitmap>()
        every { mockBitmap.compress(any(), any(), any()) } returns false
        
        // When
        val compressedBytes = imagePreprocessor.compressForStorage(mockBitmap)
        
        // Then
        assertNull(compressedBytes)
    }

    @Test
    fun `compressForStorage should handle exception`() = runTest {
        // Given - Mock a bitmap that throws exception
        val mockBitmap = mockk<Bitmap>()
        every { mockBitmap.compress(any(), any(), any()) } throws RuntimeException("Test error")
        
        // When
        val compressedBytes = imagePreprocessor.compressForStorage(mockBitmap)
        
        // Then
        assertNull(compressedBytes)
    }

    @Test
    fun `createPreview should return null for invalid URI`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        every { mockContentResolver.openInputStream(mockUri) } returns null
        
        // When
        val previewBitmap = imagePreprocessor.createPreview(mockUri)
        
        // Then
        assertNull(previewBitmap)
    }

    @Test
    fun `createPreview should handle exception`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        every { mockContentResolver.openInputStream(mockUri) } throws IOException("Test error")
        
        // When
        val previewBitmap = imagePreprocessor.createPreview(mockUri)
        
        // Then
        assertNull(previewBitmap)
    }

    @Test
    fun `PreprocessingResult success should have correct properties`() {
        // Given
        val processedData = ProcessedImageData(
            imageBytes = byteArrayOf(1, 2, 3, 4),
            width = 800,
            height = 600,
            fileSizeKB = 50,
            compressionQuality = 85
        )
        
        // When
        val result = PreprocessingResult.success(processedData)
        
        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
        assertEquals(processedData, result.processedData)
        assertEquals(null, result.error)
    }

    @Test
    fun `PreprocessingResult failure should have correct properties`() {
        // Given
        val error = PreprocessingError.DECODE_FAILED
        
        // When
        val result = PreprocessingResult.failure(error)
        
        // Then
        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)
        assertEquals(null, result.processedData)
        assertEquals(error, result.error)
        assertEquals(error.message, result.errorMessage)
    }

    @Test
    fun `ProcessedImageData should calculate file size in MB correctly`() {
        // Given
        val processedData = ProcessedImageData(
            imageBytes = ByteArray(2048 * 1024), // 2MB in KB
            width = 800,
            height = 600,
            fileSizeKB = 2048,
            compressionQuality = 85
        )
        
        // When
        val sizeInMB = processedData.getFileSizeMB()
        
        // Then
        assertEquals(2.0f, sizeInMB, 0.01f)
    }

    @Test
    fun `ProcessedImageData should format dimensions correctly`() {
        // Given
        val processedData = ProcessedImageData(
            imageBytes = byteArrayOf(),
            width = 1920,
            height = 1080,
            fileSizeKB = 100,
            compressionQuality = 85
        )
        
        // When
        val dimensionsString = processedData.getDimensionsString()
        
        // Then
        assertEquals("1920x1080", dimensionsString)
    }

    @Test
    fun `ProcessedImageData equals should work correctly`() {
        // Given
        val data1 = ProcessedImageData(
            imageBytes = byteArrayOf(1, 2, 3),
            width = 800,
            height = 600,
            fileSizeKB = 50,
            compressionQuality = 85
        )
        
        val data2 = ProcessedImageData(
            imageBytes = byteArrayOf(1, 2, 3),
            width = 800,
            height = 600,
            fileSizeKB = 50,
            compressionQuality = 85
        )
        
        val data3 = ProcessedImageData(
            imageBytes = byteArrayOf(1, 2, 4), // Different bytes
            width = 800,
            height = 600,
            fileSizeKB = 50,
            compressionQuality = 85
        )
        
        // Then
        assertEquals(data1, data2)
        assertTrue(data1 != data3)
    }

    @Test
    fun `ProcessedImageData hashCode should be consistent`() {
        // Given
        val data1 = ProcessedImageData(
            imageBytes = byteArrayOf(1, 2, 3),
            width = 800,
            height = 600,
            fileSizeKB = 50,
            compressionQuality = 85
        )
        
        val data2 = ProcessedImageData(
            imageBytes = byteArrayOf(1, 2, 3),
            width = 800,
            height = 600,
            fileSizeKB = 50,
            compressionQuality = 85
        )
        
        // Then
        assertEquals(data1.hashCode(), data2.hashCode())
    }

    @Test
    fun `PreprocessingError should have correct messages`() {
        // Test all preprocessing error messages
        assertEquals("Invalid image URI or file not accessible", PreprocessingError.INVALID_URI.message)
        assertEquals("Failed to decode image", PreprocessingError.DECODE_FAILED.message)
        assertEquals("Image processing failed", PreprocessingError.PROCESSING_FAILED.message)
        assertEquals("Error reading image file", PreprocessingError.IO_ERROR.message)
        assertEquals("Not enough memory to process image", PreprocessingError.OUT_OF_MEMORY.message)
        assertEquals("Unknown error occurred during preprocessing", PreprocessingError.UNKNOWN_ERROR.message)
    }
}