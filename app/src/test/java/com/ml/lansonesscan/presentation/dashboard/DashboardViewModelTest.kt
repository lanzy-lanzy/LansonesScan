package com.ml.lansonesscan.presentation.dashboard

import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.ScanMetadata
import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.domain.usecase.GetScanHistoryUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private lateinit var viewModel: DashboardViewModel
    private lateinit var getScanHistoryUseCase: GetScanHistoryUseCase
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getScanHistoryUseCase = mockk()
        viewModel = DashboardViewModel(getScanHistoryUseCase)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be loading`() {
        val initialState = viewModel.uiState.value
        assertTrue(initialState.isLoading)
        assertTrue(initialState.recentScans.isEmpty())
        assertNull(initialState.statistics)
        assertNull(initialState.error)
    }

    @Test
    fun `loadDashboardData should load recent scans and statistics successfully`() = runTest {
        // Given
        val mockScans = listOf(
            createMockScanResult("1", true, "Disease A"),
            createMockScanResult("2", false, null)
        )
        val mockStatistics = GetScanHistoryUseCase.ScanStatistics(
            totalScans = 10,
            diseaseDetectedCount = 3,
            healthyScansCount = 7,
            totalStorageSize = 1024L,
            diseaseDetectionRate = 30.0f
        )

        coEvery { getScanHistoryUseCase.getRecentScans(5) } returns flowOf(mockScans)
        coEvery { getScanHistoryUseCase.getScanStatistics() } returns mockStatistics

        // When
        viewModel.loadDashboardData()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoading)
        assertEquals(mockScans, finalState.recentScans)
        assertEquals(mockStatistics, finalState.statistics)
        assertNull(finalState.error)
        assertTrue(finalState.hasData)
        assertFalse(finalState.isEmpty)
    }

    @Test
    fun `loadDashboardData should handle error when loading recent scans fails`() = runTest {
        // Given
        val errorMessage = "Network error"
        coEvery { getScanHistoryUseCase.getRecentScans(5) } returns flowOf()
        coEvery { getScanHistoryUseCase.getScanStatistics() } throws Exception(errorMessage)

        // When
        viewModel.loadDashboardData()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoading)
        assertTrue(finalState.recentScans.isEmpty())
        assertNull(finalState.statistics)
        assertTrue(finalState.error?.contains(errorMessage) == true)
    }

    @Test
    fun `empty state should be true when no scans and no statistics`() = runTest {
        // Given
        val emptyStatistics = GetScanHistoryUseCase.ScanStatistics(
            totalScans = 0,
            diseaseDetectedCount = 0,
            healthyScansCount = 0,
            totalStorageSize = 0L,
            diseaseDetectionRate = 0.0f
        )

        coEvery { getScanHistoryUseCase.getRecentScans(5) } returns flowOf(emptyList())
        coEvery { getScanHistoryUseCase.getScanStatistics() } returns emptyStatistics

        // When
        viewModel.loadDashboardData()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertTrue(finalState.isEmpty)
        assertFalse(finalState.hasData)
    }

    @Test
    fun `refresh should reload dashboard data`() = runTest {
        // Given
        val mockScans = listOf(createMockScanResult("1", true, "Disease A"))
        val mockStatistics = GetScanHistoryUseCase.ScanStatistics(
            totalScans = 1,
            diseaseDetectedCount = 1,
            healthyScansCount = 0,
            totalStorageSize = 512L,
            diseaseDetectionRate = 100.0f
        )

        coEvery { getScanHistoryUseCase.getRecentScans(5) } returns flowOf(mockScans)
        coEvery { getScanHistoryUseCase.getScanStatistics() } returns mockStatistics

        // When
        viewModel.refresh()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 2) { getScanHistoryUseCase.getRecentScans(5) } // Initial load + refresh
        coVerify(exactly = 2) { getScanHistoryUseCase.getScanStatistics() } // Initial load + refresh
    }

    @Test
    fun `onQuickScanClicked should set navigation event to analysis`() {
        // When
        viewModel.onQuickScanClicked()

        // Then
        val state = viewModel.uiState.value
        assertEquals(DashboardNavigationEvent.NavigateToAnalysis, state.navigationEvent)
    }

    @Test
    fun `onViewAllHistoryClicked should set navigation event to history`() {
        // When
        viewModel.onViewAllHistoryClicked()

        // Then
        val state = viewModel.uiState.value
        assertEquals(DashboardNavigationEvent.NavigateToHistory, state.navigationEvent)
    }

    @Test
    fun `onScanItemClicked should set navigation event to scan detail`() {
        // Given
        val scanResult = createMockScanResult("test-id", true, "Disease A")

        // When
        viewModel.onScanItemClicked(scanResult)

        // Then
        val state = viewModel.uiState.value
        assertEquals(
            DashboardNavigationEvent.NavigateToScanDetail("test-id"),
            state.navigationEvent
        )
    }

    @Test
    fun `onNavigationEventHandled should clear navigation event`() {
        // Given
        viewModel.onQuickScanClicked() // Set a navigation event

        // When
        viewModel.onNavigationEventHandled()

        // Then
        val state = viewModel.uiState.value
        assertNull(state.navigationEvent)
    }

    @Test
    fun `clearError should clear error state`() = runTest {
        // Given - Set an error state
        coEvery { getScanHistoryUseCase.getRecentScans(5) } returns flowOf()
        coEvery { getScanHistoryUseCase.getScanStatistics() } throws Exception("Test error")
        
        viewModel.loadDashboardData()
        advanceUntilIdle()
        
        // Verify error is set
        assertTrue(viewModel.uiState.value.error != null)

        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `UI state should format statistics correctly`() = runTest {
        // Given
        val mockStatistics = GetScanHistoryUseCase.ScanStatistics(
            totalScans = 42,
            diseaseDetectedCount = 15,
            healthyScansCount = 27,
            totalStorageSize = 2048L,
            diseaseDetectionRate = 35.7f
        )

        coEvery { getScanHistoryUseCase.getRecentScans(5) } returns flowOf(emptyList())
        coEvery { getScanHistoryUseCase.getScanStatistics() } returns mockStatistics

        // When
        viewModel.loadDashboardData()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("42", state.totalScansText)
        assertEquals("35.7%", state.diseaseDetectionRateText)
        assertEquals("2 KB", state.storageSizeText)
    }

    private fun createMockScanResult(
        id: String,
        diseaseDetected: Boolean,
        diseaseName: String?
    ): ScanResult {
        return ScanResult(
            id = id,
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseDetected = diseaseDetected,
            diseaseName = diseaseName,
            confidenceLevel = 0.85f,
            recommendations = listOf("Test recommendation"),
            timestamp = System.currentTimeMillis(),
            metadata = ScanMetadata(
                imageSize = 1024L,
                imageFormat = "JPEG",
                analysisTime = 2000L,
                apiVersion = "1.0"
            )
        )
    }
}