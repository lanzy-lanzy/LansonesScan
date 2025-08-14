package com.ml.lansonesscan.domain.usecase

import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.ScanMetadata
import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.domain.repository.ScanRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

class SaveScanResultUseCaseTest {

    private lateinit var scanRepository: ScanRepository
    private lateinit var saveScanResultUseCase: SaveScanResultUseCase
    
    private val validScanResult = ScanResult.createHealthy(
        imagePath = "/test/path/image.jpg",
        analysisType = AnalysisType.FRUIT,
        confidenceLevel = 0.85f,
        metadata = ScanMetadata.create(
            imageSize = 1024L,
            imageFormat = "JPEG",
            analysisTime = 2000L,
            apiVersion = "1.0"
        )
    )

    @BeforeEach
    fun setUp() {
        scanRepository = mockk()
        saveScanResultUseCase = SaveScanResultUseCase(scanRepository)
    }

    @Test
    fun `invoke should return success when repository save succeeds`() = runTest {
        // Given
        coEvery { scanRepository.saveScanResult(validScanResult) } returns Result.success(Unit)

        // When
        val result = saveScanResultUseCase(validScanResult)

        // Then
        assertTrue(result.isSuccess)
        coVerify { scanRepository.saveScanResult(validScanResult) }
    }

    @Test
    fun `invoke should return failure when repository save fails`() = runTest {
        // Given
        val error = RuntimeException("Database error")
        coEvery { scanRepository.saveScanResult(validScanResult) } returns Result.failure(error)

        // When
        val result = saveScanResultUseCase(validScanResult)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("Failed to save scan result: Database error", result.exceptionOrNull()?.message)
    }    @Test

    fun `invoke should handle SecurityException`() = runTest {
        // Given
        val error = SecurityException("Permission denied")
        coEvery { scanRepository.saveScanResult(validScanResult) } returns Result.failure(error)

        // When
        val result = saveScanResultUseCase(validScanResult)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("Permission denied while saving scan result", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke should handle IOException`() = runTest {
        // Given
        val error = IOException("Storage full")
        coEvery { scanRepository.saveScanResult(validScanResult) } returns Result.failure(error)

        // When
        val result = saveScanResultUseCase(validScanResult)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("Storage error while saving scan result", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke should validate invalid scan result`() = runTest {
        // Given
        val invalidScanResult = validScanResult.copy(confidenceLevel = 1.5f) // Invalid confidence

        // When
        val result = saveScanResultUseCase(invalidScanResult)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `invoke should validate diseased scan result without disease name`() = runTest {
        // Given
        val invalidScanResult = validScanResult.copy(
            diseaseDetected = true,
            diseaseName = null // Invalid: disease detected but no name
        )

        // When
        val result = saveScanResultUseCase(invalidScanResult)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(
            "Disease name cannot be null or blank when disease is detected",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun `saveBatch should save multiple scan results successfully`() = runTest {
        // Given
        val scanResults = listOf(validScanResult, validScanResult.copy(id = "test-2"))
        coEvery { scanRepository.saveScanResult(any()) } returns Result.success(Unit)

        // When
        val result = saveScanResultUseCase.saveBatch(scanResults)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
        coVerify(exactly = 2) { scanRepository.saveScanResult(any()) }
    }

    @Test
    fun `saveBatch should handle partial failures`() = runTest {
        // Given
        val scanResults = listOf(validScanResult, validScanResult.copy(id = "test-2"))
        coEvery { scanRepository.saveScanResult(validScanResult) } returns Result.success(Unit)
        coEvery { 
            scanRepository.saveScanResult(validScanResult.copy(id = "test-2")) 
        } returns Result.failure(RuntimeException("Error"))

        // When
        val result = saveScanResultUseCase.saveBatch(scanResults)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("1 successes and 1 failures") == true)
    }

    @Test
    fun `saveBatch should validate empty list`() = runTest {
        // When
        val result = saveScanResultUseCase.saveBatch(emptyList())

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Scan results list cannot be empty", result.exceptionOrNull()?.message)
    }
}