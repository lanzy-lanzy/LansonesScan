package com.ml.lansonesscan.data.local.validation

import android.content.ContentResolver
import android.content.Context
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
import kotlin.test.assertTrue

/**
 * Unit tests for ImageValidator focusing on validation logic without Android dependencies
 */
class ImageValidatorUnitTest {

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
    fun `validateImage should handle IO exception`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        every { mockContentResolver.openInputStream(mockUri) } throws IOException("Test IO error")
        
        // When
        val result = imageValidator.validateImage(mockUri)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(ValidationError.IO_ERROR, result.error)
    }

    @Test
    fun `validateImage should handle security exception`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        every { mockContentResolver.openInputStream(mockUri) } throws SecurityException("Test security error")
        
        // When
        val result = imageValidator.validateImage(mockUri)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(ValidationError.PERMISSION_DENIED, result.error)
    }

    @Test
    fun `quickValidateImage should succeed for valid format and size`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        val validImageBytes = ByteArray(1024) { 0x01 }
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

    @Test
    fun `ValidationError should have correct messages`() {
        // Test all validation error messages
        assertEquals("Invalid image URI or file not accessible", ValidationError.INVALID_URI.message)
        assertEquals("Image format not supported. Please use JPEG or PNG", ValidationError.UNSUPPORTED_FORMAT.message)
        assertEquals("Image file is too large. Maximum size is 10MB", ValidationError.FILE_TOO_LARGE.message)
        assertEquals("Image file is empty or corrupted", ValidationError.EMPTY_FILE.message)
        assertEquals("Image is too small. Minimum size is 100x100", ValidationError.IMAGE_TOO_SMALL.message)
        assertEquals("Image is too large. Maximum size is 4096x4096", ValidationError.IMAGE_TOO_LARGE.message)
        assertEquals("Image aspect ratio is not suitable for analysis", ValidationError.INVALID_ASPECT_RATIO.message)
        assertEquals("Image file is corrupted or cannot be decoded", ValidationError.CORRUPTED_IMAGE.message)
        assertEquals("Error reading image file", ValidationError.IO_ERROR.message)
        assertEquals("Permission denied to access image file", ValidationError.PERMISSION_DENIED.message)
        assertEquals("Unknown error occurred during validation", ValidationError.UNKNOWN_ERROR.message)
    }
}