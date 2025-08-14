package com.ml.lansonesscan.domain.usecase

import com.ml.lansonesscan.domain.repository.ScanRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

class ClearHistoryUseCaseTest {

    private lateinit var scanRepository: ScanRepository
    private lateinit var clearHistoryUseCase: ClearHistoryUseCase

    @BeforeEach
    fun setUp() {
        scanRepository = mockk()
        clearHistoryUseCase = ClearHistoryUseCase(scanRepository)
    }

    @Test
    fun `invoke should return success when clearing succeeds`() = runTest {
        // Given
        coEvery { scanRepository.getScanCount() } returns 5
        coEvery { scanRepository.getTotalStorageSize() } returns 1024L
        coEvery { scanRepository.clearAllScans() } returns Result.success(Unit)

        // When
        val result = clearHistoryUseCase()

        // Then
        assertTrue(result.isSuccess)
        val clearResult = result.getOrNull()!!
        assertEquals(5, clearResult.scansCleared)
        assertEquals(1024L, clearResult.storageFreed)
        assertTrue(clearResult.operationTime >= 0)
        assertEquals("1 KB", clearResult.getFormattedStorageFreed())
        coVerify { scanRepository.clearAllScans() }
    }

    @Test
    fun `invoke should handle empty history`() = runTest {
        // Given
        coEvery { scanRepository.getScanCount() } returns 0
        coEvery { scanRepository.getTotalStorageSize() } returns 0L

        // When
        val result = clearHistoryUseCase()

        // Then
        assertTrue(result.isSuccess)
        val clearResult = result.getOrNull()!!
        assertEquals(0, clearResult.scansCleared)
        assertEquals(0L, clearResult.storageFreed)
        assertEquals(0L, clearResult.operationTime)
        coVerify(exactly = 0) { scanRepository.clearAllScans() }
    }

    @Test
    fun `invoke should handle SecurityException`() = runTest {
        // Given
        coEvery { scanRepository.getScanCount() } returns 5
        coEvery { scanRepository.getTotalStorageSize() } returns 1024L
        coEvery { scanRepository.clearAllScans() } returns Result.failure(SecurityException("Permission denied"))

        // When
        val result = clearHistoryUseCase()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("Permission denied while clearing history", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke should handle IOException`() = runTest {
        // Given
        coEvery { scanRepository.getScanCount() } returns 5
        coEvery { scanRepository.getTotalStorageSize() } returns 1024L
        coEvery { scanRepository.clearAllScans() } returns Result.failure(IOException("Storage error"))

        // When
        val result = clearHistoryUseCase()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("File system error while clearing history", result.exceptionOrNull()?.message)
    }

    @Test
    fun `clearWithConfirmation should proceed when confirmed`() = runTest {
        // Given
        coEvery { scanRepository.getScanCount() } returns 3
        coEvery { scanRepository.getDiseaseDetectedCount() } returns 1
        coEvery { scanRepository.getHealthyScansCount() } returns 2
        coEvery { scanRepository.getTotalStorageSize() } returns 2048L
        coEvery { scanRepository.clearAllScans() } returns Result.success(Unit)

        val confirmationCallback: suspend (ClearHistoryUseCase.ClearHistoryPreview) -> Boolean = { true }

        // When
        val result = clearHistoryUseCase.clearWithConfirmation(confirmationCallback)

        // Then
        assertTrue(result.isSuccess)
        val clearResult = result.getOrNull()!!
        assertEquals(3, clearResult.scansCleared)
        assertEquals(2048L, clearResult.storageFreed)
    }

    @Test
    fun `clearWithConfirmation should cancel when not confirmed`() = runTest {
        // Given
        coEvery { scanRepository.getScanCount() } returns 3
        coEvery { scanRepository.getDiseaseDetectedCount() } returns 1
        coEvery { scanRepository.getHealthyScansCount() } returns 2
        coEvery { scanRepository.getTotalStorageSize() } returns 2048L

        val confirmationCallback: suspend (ClearHistoryUseCase.ClearHistoryPreview) -> Boolean = { false }

        // When
        val result = clearHistoryUseCase.clearWithConfirmation(confirmationCallback)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("Operation cancelled by user", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { scanRepository.clearAllScans() }
    }

    @Test
    fun `getPreview should return correct preview information`() = runTest {
        // Given
        coEvery { scanRepository.getScanCount() } returns 10
        coEvery { scanRepository.getDiseaseDetectedCount() } returns 3
        coEvery { scanRepository.getHealthyScansCount() } returns 7
        coEvery { scanRepository.getTotalStorageSize() } returns 5120L

        // When
        val preview = clearHistoryUseCase.getPreview()

        // Then
        assertEquals(10, preview.totalScans)
        assertEquals(3, preview.diseaseDetectedCount)
        assertEquals(7, preview.healthyScansCount)
        assertEquals(5120L, preview.totalStorageSize)
        assertEquals("5 KB", preview.getFormattedStorageSize())
        assertEquals(30.0f, preview.getDiseaseDetectionRate())
    }

    @Test
    fun `cleanupOrphanedFiles should return orphaned file count`() = runTest {
        // Given
        coEvery { scanRepository.cleanupOrphanedImages() } returns Result.success(5)

        // When
        val result = clearHistoryUseCase.cleanupOrphanedFiles()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrNull())
        coVerify { scanRepository.cleanupOrphanedImages() }
    }

    @Test
    fun `ClearHistoryResult should format storage and time correctly`() {
        // Test storage formatting
        val result1 = ClearHistoryUseCase.ClearHistoryResult(5, 512L, 1500L)
        assertEquals("512 B", result1.getFormattedStorageFreed())
        assertEquals("1s", result1.getFormattedOperationTime())

        val result2 = ClearHistoryUseCase.ClearHistoryResult(10, 2048L, 500L)
        assertEquals("2 KB", result2.getFormattedStorageFreed())
        assertEquals("500ms", result2.getFormattedOperationTime())

        val result3 = ClearHistoryUseCase.ClearHistoryResult(20, 1048576L, 65000L)
        assertEquals("1 MB", result3.getFormattedStorageFreed())
        assertEquals("1m 5s", result3.getFormattedOperationTime())
    }

    @Test
    fun `ClearHistoryPreview should calculate disease detection rate correctly`() {
        val preview1 = ClearHistoryUseCase.ClearHistoryPreview(10, 3, 7, 1024L)
        assertEquals(30.0f, preview1.getDiseaseDetectionRate())

        val preview2 = ClearHistoryUseCase.ClearHistoryPreview(0, 0, 0, 0L)
        assertEquals(0.0f, preview2.getDiseaseDetectionRate())

        val preview3 = ClearHistoryUseCase.ClearHistoryPreview(5, 5, 0, 2048L)
        assertEquals(100.0f, preview3.getDiseaseDetectionRate())
    }
}