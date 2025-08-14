package com.ml.lansonesscan.domain.usecase

import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.ScanMetadata
import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.domain.repository.ScanRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(JUnit4::class)
class GetScanHistoryUseCaseTest {

    private lateinit var scanRepository: ScanRepository
    private lateinit var getScanHistoryUseCase: GetScanHistoryUseCase
    
    private val healthyScanResult = ScanResult.createHealthy(
        imagePath = "/test/path/healthy.jpg",
        analysisType = AnalysisType.FRUIT,
        confidenceLevel = 0.85f,
        metadata = ScanMetadata.create(1024L, "JPEG", 2000L, "1.0")
    )
    
    private val diseasedScanResult = ScanResult.createDiseased(
        imagePath = "/test/path/diseased.jpg",
        analysisType = AnalysisType.LEAVES,
        diseaseName = "Brown Spot",
        confidenceLevel = 0.92f,
        recommendations = listOf("Apply treatment"),
        metadata = ScanMetadata.create(2048L, "PNG", 3000L, "1.0")
    )

    @Before
    fun setUp() {
        scanRepository = mockk()
        getScanHistoryUseCase = GetScanHistoryUseCase(scanRepository)
    }

    @Test
    fun `invoke should return all scans when no filters applied`() = runTest {
        val allScans = listOf(healthyScanResult, diseasedScanResult)
        coEvery { scanRepository.getAllScans() } returns flowOf(allScans)

        val result = getScanHistoryUseCase().toList()

        assertEquals(1, result.size)
        assertEquals(2, result[0].size)
        assertTrue(result[0].contains(healthyScanResult))
        assertTrue(result[0].contains(diseasedScanResult))
    }
}