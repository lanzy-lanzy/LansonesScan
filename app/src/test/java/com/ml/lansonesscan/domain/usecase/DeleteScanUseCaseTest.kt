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

class DeleteScanUseCaseTest {

    private lateinit var scanRepository: ScanRepository
    private lateinit var deleteScanUseCase: DeleteScanUseCase
    
    private val testScanId = "test-scan-id"
    private val validScanResult = ScanResult.createHealthy(
        imagePath = "/test/path/image.jpg",
        analysisType = AnalysisType.FRUIT,
        confidenceLevel = 0.85f,
        metadata = ScanMetadata.create(1024L, "JPEG", 2000L, "1.0")
    )

    @BeforeEach
    fun setUp() {
        scanRepository = mockk()
        deleteScanUseCase = DeleteScanUseCase(scanRepository)
    }

    @Test
    fun `invoke should return success when scan exists and deletion succeeds`() = runTest {
        // Given
        coEvery { scanRepository.getScanById(testScanId) } returns validScanResult
        coEvery { scanRepository.deleteScan(testScanId) } returns Result.success(Unit)

        // When
        val result = deleteScanUseCase(testScanId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { scanRepository.getScanById(testScanId) }
        coVerify { scanRepository.deleteScan(testScanId) }
    }

    @Test
    fun `invoke should return failure when scan does not exist`() = runTest {
        // Given
        coEvery { scanRepository.getScanById(testScanId) } returns null

        // When
        val result = deleteScanUseCase(testScanId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
        assertEquals("Scan with ID '$testScanId' not found", result.exceptionOrNull()?.message)
        coVerify { scanRepository.getScanById(testScanId) }
        coVerify(exactly = 0) { scanRepository.deleteScan(any()) }
    }

    @Test
    fun `invoke should handle SecurityException`() = runTest {
        // Given
        coEvery { scanRepository.getScanById(testScanId) } returns validScanResult
        coEvery { scanRepository.deleteScan(testScanId) } returns Result.failure(SecurityException("Permission denied"))

        // When
        val result = deleteScanUseCase(testScanId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("Permission denied while deleting scan", result.exceptionOrNull()?.message)
    } 
   @Test
    fun `invoke should handle IOException`() = runTest {
        // Given
        coEvery { scanRepository.getScanById(testScanId) } returns validScanResult
        coEvery { scanRepository.deleteScan(testScanId) } returns Result.failure(IOException("File error"))

        // When
        val result = deleteScanUseCase(testScanId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("File system error while deleting scan", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke should validate blank scan ID`() = runTest {
        // When
        val result = deleteScanUseCase("")

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Scan ID cannot be blank", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke should validate too long scan ID`() = runTest {
        // Given
        val longScanId = "a".repeat(256)

        // When
        val result = deleteScanUseCase(longScanId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Scan ID is too long", result.exceptionOrNull()?.message)
    }

    @Test
    fun `deleteBatch should delete multiple scans successfully`() = runTest {
        // Given
        val scanIds = listOf("scan1", "scan2", "scan3")
        scanIds.forEach { scanId ->
            coEvery { scanRepository.getScanById(scanId) } returns validScanResult.copy(id = scanId)
            coEvery { scanRepository.deleteScan(scanId) } returns Result.success(Unit)
        }

        // When
        val result = deleteScanUseCase.deleteBatch(scanIds)

        // Then
        assertTrue(result.isSuccess)
        val batchResult = result.getOrNull()!!
        assertEquals(3, batchResult.totalRequested)
        assertEquals(3, batchResult.successCount)
        assertEquals(0, batchResult.failureCount)
        assertTrue(batchResult.isCompleteSuccess)
        assertEquals(100.0f, batchResult.getSuccessRate())
    }

    @Test
    fun `deleteBatch should handle partial failures`() = runTest {
        // Given
        val scanIds = listOf("scan1", "scan2", "scan3")
        coEvery { scanRepository.getScanById("scan1") } returns validScanResult.copy(id = "scan1")
        coEvery { scanRepository.deleteScan("scan1") } returns Result.success(Unit)
        coEvery { scanRepository.getScanById("scan2") } returns null // Not found
        coEvery { scanRepository.getScanById("scan3") } returns validScanResult.copy(id = "scan3")
        coEvery { scanRepository.deleteScan("scan3") } returns Result.success(Unit)

        // When
        val result = deleteScanUseCase.deleteBatch(scanIds)

        // Then
        assertTrue(result.isSuccess)
        val batchResult = result.getOrNull()!!
        assertEquals(3, batchResult.totalRequested)
        assertEquals(2, batchResult.successCount)
        assertEquals(1, batchResult.failureCount)
        assertTrue(batchResult.isPartialSuccess)
        assertEquals(66.7f, batchResult.getSuccessRate(), 0.1f)
        assertEquals(1, batchResult.failures.size)
        assertEquals("scan2", batchResult.failures[0].scanId)
    }

    @Test
    fun `deleteBatch should validate empty list`() = runTest {
        // When
        val result = deleteScanUseCase.deleteBatch(emptyList())

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Scan IDs list cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `deleteBatch should validate blank scan IDs`() = runTest {
        // Given
        val scanIds = listOf("scan1", "", "scan3")

        // When
        val result = deleteScanUseCase.deleteBatch(scanIds)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("All scan IDs must be non-blank", result.exceptionOrNull()?.message)
    }
}