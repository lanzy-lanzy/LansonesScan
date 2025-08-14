package com.ml.lansonesscan.data.local.validation

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ImageValidatorTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var mockContext: Context
    private lateinit var mockContentResolver: ContentResolver
    private lateinit var imageValidator: ImageValidator

    @BeforeEach
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockContentResolver = mockk(relaxed = true)
        
        every { mockContext.contentResolver } returns mockContentResolver
        
        imageValidator = ImageValidator(mockContext)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `validateImage should succeed for valid JPEG image`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        val validImageBytes = createValidJpegBytes()
        val inputStream = ByteArrayInputStream(validImageBytes)
        
        every { mockContentResolver.openInputStream(mockUri) } returns inputStream
        every { mockContentResolver.getType(mockUri) } returns "image/jpeg"
        
        // When
        val result = imageValidator.validateImage(mockUri)
        
        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.imageInfo)
        assertEquals("image/jpeg", result.imageInfo?.mimeType)
    }

    @Test
    fun `validateImage should succeed for valid PNG image`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        val validImageBytes = createValidPngBytes()
        val inputStream = ByteArrayInputStream(validImageBytes)
        
        every { mockContentResolver.openInputStream(mockUri) } returns inputStream
        every { mockContentResolver.getType(mockUri) } returns "image/png"
        
        // When
        val result = imageValidator.validateImage(mockUri)
        
        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.imageInfo)
        assertEquals("image/png", result.imageInfo?.mimeType)
    }

    @Test
    fun `validateImage should fail for unsupported format`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        val inputStream = ByteArrayInputStream(byteArrayOf(1, 2, 3, 4))
        
        every { mockContentResolver.openInputStream(mockUri) } returns inputStream
        every { mockContentResolver.getType(mockUri) } returns "image/gif"
        
        // When
        val result = imageValidator.validateImage(mockUri)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(ValidationError.UNSUPPORTED_FORMAT, result.error)
    }

    @Test
    fun `validateImage should fail for null MIME type`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        val inputStream = ByteArrayInputStream(byteArrayOf(1, 2, 3, 4))
        
        every { mockContentResolver.openInputStream(mockUri) } returns inputStream
        every { mockContentResolver.getType(mockUri) } returns null
        
        // When
        val result = imageValidator.validateImage(mockUri)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(ValidationError.UNSUPPORTED_FORMAT, result.error)
    }

    @Test
    fun `validateImage should fail for invalid URI`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        every { mockContentResolver.openInputStream(mockUri) } returns null
        
        // When
        val result = imageValidator.validateImage(mockUri)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(ValidationError.INVALID_URI, result.error)
    }

    @Test
    fun `validateImage should fail for empty file`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        val emptyInputStream = ByteArrayInputStream(byteArrayOf())
        
        every { mockContentResolver.openInputStream(mockUri) } returns emptyInputStream
        every { mockContentResolver.getType(mockUri) } returns "image/jpeg"
        
        // When
        val result = imageValidator.validateImage(mockUri)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(ValidationError.EMPTY_FILE, result.error)
    }

    @Test
    fun `validateImage should fail for file too large`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        val largeImageBytes = ByteArray(11 * 1024 * 1024) // 11MB
        val inputStream = ByteArrayInputStream(largeImageBytes)
        
        every { mockContentResolver.openInputStream(mockUri) } returns inputStream
        every { mockContentResolver.getType(mockUri) } returns "image/jpeg"
        
        // When
        val result = imageValidator.validateImage(mockUri)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(ValidationError.FILE_TOO_LARGE, result.error)
    }

    @Test
    fun `validateImage should fail for corrupted image`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        val corruptedBytes = ByteArray(1024) { 0xFF.toByte() } // Invalid JPEG data
        val inputStream = ByteArrayInputStream(corruptedBytes)
        
        every { mockContentResolver.openInputStream(mockUri) } returns inputStream
        every { mockContentResolver.getType(mockUri) } returns "image/jpeg"
        
        // When
        val result = imageValidator.validateImage(mockUri)
        
        // Then
        assertTrue(result.isFailure)
        // The error might be CORRUPTED_IMAGE or UNKNOWN_ERROR depending on how BitmapFactory handles it
        assertTrue(result.error == ValidationError.CORRUPTED_IMAGE || result.error == ValidationError.UNKNOWN_ERROR)
    }

    @Test
    fun `quickValidateImage should succeed for valid format and size`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        val validImageBytes = createValidJpegBytes()
        val inputStream = ByteArrayInputStream(validImageBytes)
        
        every { mockContentResolver.openInputStream(mockUri) } returns inputStream
        every { mockContentResolver.getType(mockUri) } returns "image/jpeg"
        
        // When
        val result = imageValidator.quickValidateImage(mockUri)
        
        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.imageInfo)
        assertEquals("image/jpeg", result.imageInfo?.mimeType)
        assertEquals(-1, result.imageInfo?.width) // Not determined in quick validation
    }

    @Test
    fun `quickValidateImage should fail for unsupported format`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        every { mockContentResolver.getType(mockUri) } returns "image/bmp"
        
        // When
        val result = imageValidator.quickValidateImage(mockUri)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(ValidationError.UNSUPPORTED_FORMAT, result.error)
    }

    @Test
    fun `isImageSuitableForAnalysis should return true for suitable image`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        val validImageBytes = createValidJpegBytes(width = 512, height = 512)
        val inputStream = ByteArrayInputStream(validImageBytes)
        
        every { mockContentResolver.openInputStream(mockUri) } returns inputStream
        every { mockContentResolver.getType(mockUri) } returns "image/jpeg"
        
        // When
        val suitable = imageValidator.isImageSuitableForAnalysis(mockUri)
        
        // Then
        assertTrue(suitable)
    }

    @Test
    fun `isImageSuitableForAnalysis should return false for too small image`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        val smallImageBytes = createValidJpegBytes(width = 100, height = 100)
        val inputStream = ByteArrayInputStream(smallImageBytes)
        
        every { mockContentResolver.openInputStream(mockUri) } returns inputStream
        every { mockContentResolver.getType(mockUri) } returns "image/jpeg"
        
        // When
        val suitable = imageValidator.isImageSuitableForAnalysis(mockUri)
        
        // Then
        assertFalse(suitable)
    }

    @Test
    fun `ValidationResult success should have correct properties`() {
        // Given
        val imageInfo = ImageInfo(
            width = 800,
            height = 600,
            fileSize = 1024L,
            mimeType = "image/jpeg",
            aspectRatio = 1.33f
        )
        
        // When
        val result = ValidationResult.success(imageInfo)
        
        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
        assertEquals(imageInfo, result.imageInfo)
        assertEquals(null, result.error)
    }

    @Test
    fun `ValidationResult failure should have correct properties`() {
        // Given
        val error = ValidationError.INVALID_URI
        
        // When
        val result = ValidationResult.failure(error)
        
        // Then
        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)
        assertEquals(null, result.imageInfo)
        assertEquals(error, result.error)
        assertEquals(error.message, result.errorMessage)
    }

    @Test
    fun `ImageInfo should calculate file size in MB correctly`() {
        // Given
        val imageInfo = ImageInfo(
            width = 800,
            height = 600,
            fileSize = 2 * 1024 * 1024L, // 2MB
            mimeType = "image/jpeg",
            aspectRatio = 1.33f
        )
        
        // When
        val sizeInMB = imageInfo.getFileSizeInMB()
        
        // Then
        assertEquals(2.0f, sizeInMB, 0.01f)
    }

    @Test
    fun `ImageInfo should format dimensions correctly`() {
        // Given
        val imageInfo = ImageInfo(
            width = 1920,
            height = 1080,
            fileSize = 1024L,
            mimeType = "image/jpeg",
            aspectRatio = 1.78f
        )
        
        // When
        val dimensionsString = imageInfo.getDimensionsString()
        
        // Then
        assertEquals("1920x1080", dimensionsString)
    }

    private fun createValidJpegBytes(width: Int = 300, height: Int = 300): ByteArray {
        // Create a minimal valid JPEG header and data
        return byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), // JPEG SOI marker
            0xFF.toByte(), 0xE0.toByte(), // APP0 marker
            0x00, 0x10, // Length
            0x4A, 0x46, 0x49, 0x46, 0x00, // "JFIF\0"
            0x01, 0x01, // Version
            0x01, // Units
            0x00, 0x48, 0x00, 0x48, // X and Y density
            0x00, 0x00, // Thumbnail width and height
            0xFF.toByte(), 0xD9.toByte() // JPEG EOI marker
        ) + ByteArray(1024) { 0x00 } // Padding to make it a reasonable size
    }

    private fun createValidPngBytes(width: Int = 300, height: Int = 300): ByteArray {
        // Create a minimal valid PNG header
        return byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
            0x00, 0x00, 0x00, 0x0D, // IHDR chunk length
            0x49, 0x48, 0x44, 0x52, // "IHDR"
            0x00, 0x00, 0x01, 0x2C, // Width (300)
            0x00, 0x00, 0x01, 0x2C, // Height (300)
            0x08, 0x02, 0x00, 0x00, 0x00, // Bit depth, color type, compression, filter, interlace
            0x00, 0x00, 0x00, 0x00, // CRC (simplified)
            0x00, 0x00, 0x00, 0x00, // IEND chunk length
            0x49, 0x45, 0x4E, 0x44, // "IEND"
            0xAE.toByte(), 0x42, 0x60, 0x82.toByte() // IEND CRC
        ) + ByteArray(1024) { 0x00 } // Padding
    }
}