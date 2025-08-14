package com.ml.lansonesscan.domain.usecase

import android.net.Uri
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
import org.junit.jupiter.api.assertThrows

class AnalyzeImageUseCaseTest {

    private lateinit var scanRepository: ScanRepository
    private lateinit var analyzeImageUseCase: AnalyzeImageUseCase
    
    private val mockUri = mockk<Uri>()
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
        analyzeImageUseCase = AnalyzeImageUseCase(scanRepository)
        
        // Setup mock URI
        coEvery { mockUri.scheme } returns "content"
        coEvery { mockUri } returns mockUri
    }

    @Test
    fun `invoke should return success when repository analysis succeeds`() = runTest {
        // Given
        coEvery { 
            scanRepository.analyzeLansonesImage(mockUri, AnalysisType.FRUIT) 
        } returns Result.success(validScanResult)

        // When
        val result = analyzeImageUseCase(mockUri, AnalysisType.FRUIT)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(validScanResult, result.getOrNull())
        coVerify { scanRepository.analyzeLansonesImage(mockUri, AnalysisType.FRUIT) }
    }

    @Test
    fun `invoke should return failure when repository analysis fails`() = runTest {
        // Given
        val error = RuntimeException("Network error")
        coEvery { 
            scanRepository.analyzeLansonesImage(mockUri, AnalysisType.FRUIT) 
        } returns Result.failure(error)

        // When
        val result = analyzeImageUseCase(mockUri, AnalysisType.FRUIT)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun `invoke should return failure when scan result is invalid`() = runTest {
        // Given
        val invalidScanResult = validScanResult.copy(confidenceLevel = 1.5f) // Invalid confidence
        coEvery { 
            scanRepository.analyzeLansonesImage(mockUri, AnalysisType.FRUIT) 
        } returns Result.success(invalidScanResult)

        // When
        val result = analyzeImageUseCase(mockUri, AnalysisType.FRUIT)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals(
            "Invalid scan result received from analysis",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun `invoke should validate empty URI`() = runTest {
        // Given
        val emptyUri = Uri.EMPTY

        // When & Then
        val result = analyzeImageUseCase(emptyUri, AnalysisType.FRUIT)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Image URI cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke should validate URI with null scheme`() = runTest {
        // Given
        val invalidUri = mockk<Uri>()
        coEvery { invalidUri.scheme } returns null
        coEvery { invalidUri } returns invalidUri

        // When & Then
        val result = analyzeImageUseCase(invalidUri, AnalysisType.FRUIT)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Image URI must have a valid scheme", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke should work with LEAVES analysis type`() = runTest {
        // Given
        val leafScanResult = validScanResult.copy(analysisType = AnalysisType.LEAVES)
        coEvery { 
            scanRepository.analyzeLansonesImage(mockUri, AnalysisType.LEAVES) 
        } returns Result.success(leafScanResult)

        // When
        val result = analyzeImageUseCase(mockUri, AnalysisType.LEAVES)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(AnalysisType.LEAVES, result.getOrNull()?.analysisType)
        coVerify { scanRepository.analyzeLansonesImage(mockUri, AnalysisType.LEAVES) }
    }

    @Test
    fun `invoke should handle diseased scan result`() = runTest {
        // Given
        val diseasedScanResult = ScanResult.createDiseased(
            imagePath = "/test/path/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseName = "Brown Spot",
            confidenceLevel = 0.92f,
            recommendations = listOf("Apply fungicide", "Remove affected fruits"),
            metadata = ScanMetadata.create(
                imageSize = 1024L,
                imageFormat = "JPEG",
                analysisTime = 2000L,
                apiVersion = "1.0"
            )
        )
        coEvery { 
            scanRepository.analyzeLansonesImage(mockUri, AnalysisType.FRUIT) 
        } returns Result.success(diseasedScanResult)

        // When
        val result = analyzeImageUseCase(mockUri, AnalysisType.FRUIT)

        // Then
        assertTrue(result.isSuccess)
        val scanResult = result.getOrNull()!!
        assertTrue(scanResult.diseaseDetected)
        assertEquals("Brown Spot", scanResult.diseaseName)
        assertEquals(0.92f, scanResult.confidenceLevel)
        assertEquals(2, scanResult.recommendations.size)
    }

    @Test
    fun `invoke should handle repository exceptions`() = runTest {
        // Given
        coEvery { 
            scanRepository.analyzeLansonesImage(mockUri, AnalysisType.FRUIT) 
        } throws RuntimeException("Unexpected error")

        // When
        val result = analyzeImageUseCase(mockUri, AnalysisType.FRUIT)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
        assertEquals("Unexpected error", result.exceptionOrNull()?.message)
    }
}