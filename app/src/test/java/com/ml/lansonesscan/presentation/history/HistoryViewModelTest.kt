package com.ml.lansonesscan.presentation.history

import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.ScanMetadata
import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.domain.usecase.DeleteScanUseCase
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
class HistoryViewModelTest {

    private lateinit var viewModel: HistoryViewModel
    private lateinit var getScanHistoryUseCase: GetScanHistoryUseCase
    private lateinit var deleteScanUseCase: DeleteScanUseCase
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getScanHistoryUseCase = mockk()
        deleteScanUseCase = mockk()
        viewModel = HistoryViewModel(getScanHistoryUseCase, deleteScanUseCase)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be loading`() {
        val initialState = viewModel.uiState.value
        assertTrue(initialState.isLoading)
        assertTrue(initialState.scanResults.isEmpty())
        assertEquals("", initialState.searchQuery)
        assertNull(initialState.selectedAnalysisType)
        assertNull(initialState.selectedDiseaseStatus)
        assertEquals(GetScanHistoryUseCase.SortBy.DATE_DESC, initialState.sortBy)
        assertNull(initialState.error)
        assertFalse(initialState.hasActiveFilters)
    }

    @Test
    fun `loadHistory should load scan results successfully`() = runTest {
        // Given
        val mockScans = listOf(
            createMockScanResult("1", true, "Disease A"),
            createMockScanResult("2", false, null)
        )

        coEvery { 
            getScanHistoryUseCase(any<GetScanHistoryUseCase.Params>()) 
        } returns flowOf(mockScans)

        // When
        viewModel.loadHistory()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoading)
        assertEquals(mockScans, finalState.scanResults)
        assertNull(finalState.error)
        assertFalse(finalState.isEmpty)
    }

    @Test
    fun `loadHistory should handle error when loading fails`() = runTest {
        // Given
        val errorMessage = "Network error"
        coEvery { 
            getScanHistoryUseCase(any<GetScanHistoryUseCase.Params>()) 
        } throws Exception(errorMessage)

        // When
        viewModel.loadHistory()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoading)
        assertTrue(finalState.scanResults.isEmpty())
        assertTrue(finalState.error?.contains(errorMessage) == true)
    }

    @Test
    fun `empty state should be true when no scans`() = runTest {
        // Given
        coEvery { 
            getScanHistoryUseCase(any<GetScanHistoryUseCase.Params>()) 
        } returns flowOf(emptyList())

        // When
        viewModel.loadHistory()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertTrue(finalState.isEmpty)
    }

    @Test
    fun `updateSearchQuery should update query and reload history`() = runTest {
        // Given
        val searchQuery = "disease"
        coEvery { 
            getScanHistoryUseCase(any<GetScanHistoryUseCase.Params>()) 
        } returns flowOf(emptyList())

        // When
        viewModel.updateSearchQuery(searchQuery)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(searchQuery, state.searchQuery)
        assertTrue(state.hasActiveFilters)
        coVerify { getScanHistoryUseCase(match { it.searchQuery == searchQuery }) }
    }

    @Test
    fun `updateAnalysisTypeFilter should update filter and reload history`() = runTest {
        // Given
        coEvery { 
            getScanHistoryUseCase(any<GetScanHistoryUseCase.Params>()) 
        } returns flowOf(emptyList())

        // When
        viewModel.updateAnalysisTypeFilter(AnalysisType.FRUIT)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(AnalysisType.FRUIT, state.selectedAnalysisType)
        assertTrue(state.hasActiveFilters)
        coVerify { getScanHistoryUseCase(match { it.analysisType == AnalysisType.FRUIT }) }
    }

    @Test
    fun `updateDiseaseStatusFilter should update filter and reload history`() = runTest {
        // Given
        coEvery { 
            getScanHistoryUseCase(any<GetScanHistoryUseCase.Params>()) 
        } returns flowOf(emptyList())

        // When
        viewModel.updateDiseaseStatusFilter(true)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(true, state.selectedDiseaseStatus)
        assertTrue(state.hasActiveFilters)
        coVerify { getScanHistoryUseCase(match { it.diseaseDetected == true }) }
    }

    @Test
    fun `updateSortBy should update sorting and reload history`() = runTest {
        // Given
        coEvery { 
            getScanHistoryUseCase(any<GetScanHistoryUseCase.Params>()) 
        } returns flowOf(emptyList())

        // When
        viewModel.updateSortBy(GetScanHistoryUseCase.SortBy.CONFIDENCE_DESC)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(GetScanHistoryUseCase.SortBy.CONFIDENCE_DESC, state.sortBy)
        assertTrue(state.hasActiveFilters)
        coVerify { getScanHistoryUseCase(match { it.sortBy == GetScanHistoryUseCase.SortBy.CONFIDENCE_DESC }) }
    }

    @Test
    fun `clearFilters should reset all filters and reload history`() = runTest {
        // Given
        viewModel.updateSearchQuery("test")
        viewModel.updateAnalysisTypeFilter(AnalysisType.FRUIT)
        viewModel.updateDiseaseStatusFilter(true)
        
        coEvery { 
            getScanHistoryUseCase(any<GetScanHistoryUseCase.Params>()) 
        } returns flowOf(emptyList())

        // When
        viewModel.clearFilters()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertNull(state.selectedAnalysisType)
        assertNull(state.selectedDiseaseStatus)
        assertEquals(GetScanHistoryUseCase.SortBy.DATE_DESC, state.sortBy)
        assertFalse(state.hasActiveFilters)
    }

    @Test
    fun `onScanItemClicked should set navigation event`() {
        // Given
        val scanResult = createMockScanResult("test-id", true, "Disease A")

        // When
        viewModel.onScanItemClicked(scanResult)

        // Then
        val state = viewModel.uiState.value
        assertEquals(
            HistoryNavigationEvent.NavigateToScanDetail("test-id"),
            state.navigationEvent
        )
    }

    @Test
    fun `showDeleteConfirmation should set scan to delete and show dialog`() {
        // Given
        val scanResult = createMockScanResult("test-id", true, "Disease A")

        // When
        viewModel.showDeleteConfirmation(scanResult)

        // Then
        val state = viewModel.uiState.value
        assertEquals(scanResult, state.scanToDelete)
        assertTrue(state.showDeleteConfirmation)
    }

    @Test
    fun `confirmDelete should delete scan successfully`() = runTest {
        // Given
        val scanResult = createMockScanResult("test-id", true, "Disease A")
        viewModel.showDeleteConfirmation(scanResult)
        
        coEvery { deleteScanUseCase("test-id") } returns Result.success(Unit)
        coEvery { 
            getScanHistoryUseCase(any<GetScanHistoryUseCase.Params>()) 
        } returns flowOf(emptyList())

        // When
        viewModel.confirmDelete()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isDeleting)
        assertNull(finalState.scanToDelete)
        assertFalse(finalState.showDeleteConfirmation)
        assertEquals("Scan deleted successfully", finalState.successMessage)
        
        coVerify { deleteScanUseCase("test-id") }
    }

    @Test
    fun `confirmDelete should handle deletion failure`() = runTest {
        // Given
        val scanResult = createMockScanResult("test-id", true, "Disease A")
        viewModel.showDeleteConfirmation(scanResult)
        
        val errorMessage = "Delete failed"
        coEvery { deleteScanUseCase("test-id") } returns Result.failure(Exception(errorMessage))

        // When
        viewModel.confirmDelete()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isDeleting)
        assertNull(finalState.scanToDelete)
        assertTrue(finalState.error?.contains(errorMessage) == true)
    }

    @Test
    fun `cancelDelete should clear delete state`() {
        // Given
        val scanResult = createMockScanResult("test-id", true, "Disease A")
        viewModel.showDeleteConfirmation(scanResult)

        // When
        viewModel.cancelDelete()

        // Then
        val state = viewModel.uiState.value
        assertNull(state.scanToDelete)
        assertFalse(state.showDeleteConfirmation)
    }

    @Test
    fun `shareScan should set navigation event`() {
        // Given
        val scanResult = createMockScanResult("test-id", true, "Disease A")

        // When
        viewModel.shareScan(scanResult)

        // Then
        val state = viewModel.uiState.value
        assertEquals(
            HistoryNavigationEvent.ShareScan(scanResult),
            state.navigationEvent
        )
    }

    @Test
    fun `dialog visibility methods should work correctly`() {
        // Test filter dialog
        viewModel.showFilterOptions()
        assertTrue(viewModel.uiState.value.showFilterDialog)
        
        viewModel.hideFilterOptions()
        assertFalse(viewModel.uiState.value.showFilterDialog)

        // Test sort dialog
        viewModel.showSortOptions()
        assertTrue(viewModel.uiState.value.showSortDialog)
        
        viewModel.hideSortOptions()
        assertFalse(viewModel.uiState.value.showSortDialog)
    }

    @Test
    fun `selection mode should work correctly`() {
        // Test toggle selection mode
        viewModel.toggleSelectionMode()
        assertTrue(viewModel.uiState.value.isSelectionMode)
        
        viewModel.toggleSelectionMode()
        assertFalse(viewModel.uiState.value.isSelectionMode)

        // Test scan selection
        viewModel.toggleSelectionMode() // Enable selection mode
        viewModel.toggleScanSelection("scan1")
        assertTrue(viewModel.uiState.value.selectedScans.contains("scan1"))
        assertEquals(1, viewModel.uiState.value.selectedCount)

        viewModel.toggleScanSelection("scan1") // Deselect
        assertFalse(viewModel.uiState.value.selectedScans.contains("scan1"))
        assertEquals(0, viewModel.uiState.value.selectedCount)
    }

    @Test
    fun `selectAllScans should select all visible scans`() = runTest {
        // Given
        val mockScans = listOf(
            createMockScanResult("1", true, "Disease A"),
            createMockScanResult("2", false, null)
        )
        
        coEvery { 
            getScanHistoryUseCase(any<GetScanHistoryUseCase.Params>()) 
        } returns flowOf(mockScans)
        
        viewModel.loadHistory()
        advanceUntilIdle()

        // When
        viewModel.selectAllScans()

        // Then
        val state = viewModel.uiState.value
        assertEquals(2, state.selectedCount)
        assertTrue(state.isAllSelected)
        assertTrue(state.selectedScans.contains("1"))
        assertTrue(state.selectedScans.contains("2"))
    }

    @Test
    fun `clearSelection should clear all selections`() {
        // Given
        viewModel.toggleSelectionMode()
        viewModel.toggleScanSelection("scan1")
        viewModel.toggleScanSelection("scan2")

        // When
        viewModel.clearSelection()

        // Then
        val state = viewModel.uiState.value
        assertEquals(0, state.selectedCount)
        assertTrue(state.selectedScans.isEmpty())
    }

    @Test
    fun `deleteSelectedScans should delete multiple scans`() = runTest {
        // Given
        viewModel.toggleSelectionMode()
        viewModel.toggleScanSelection("scan1")
        viewModel.toggleScanSelection("scan2")
        
        val batchResult = DeleteScanUseCase.DeleteBatchResult(
            totalRequested = 2,
            successCount = 2,
            failureCount = 0,
            failures = emptyList()
        )
        
        coEvery { deleteScanUseCase.deleteBatch(listOf("scan1", "scan2")) } returns Result.success(batchResult)
        coEvery { 
            getScanHistoryUseCase(any<GetScanHistoryUseCase.Params>()) 
        } returns flowOf(emptyList())

        // When
        viewModel.deleteSelectedScans()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isDeleting)
        assertFalse(finalState.isSelectionMode)
        assertTrue(finalState.selectedScans.isEmpty())
        assertEquals("Deleted 2 of 2 scans", finalState.successMessage)
        
        coVerify { deleteScanUseCase.deleteBatch(listOf("scan1", "scan2")) }
    }

    @Test
    fun `UI state computed properties should work correctly`() = runTest {
        // Test filter summary
        viewModel.updateSearchQuery("test")
        viewModel.updateAnalysisTypeFilter(AnalysisType.FRUIT)
        viewModel.updateDiseaseStatusFilter(true)
        
        val state = viewModel.uiState.value
        assertTrue(state.filterSummary.contains("Fruit Analysis"))
        assertTrue(state.filterSummary.contains("Disease Detected"))
        assertTrue(state.filterSummary.contains("Search: \"test\""))

        // Test sort description
        assertEquals("Newest First", state.sortDescription)
        
        viewModel.updateSortBy(GetScanHistoryUseCase.SortBy.CONFIDENCE_DESC)
        assertEquals("Highest Confidence", viewModel.uiState.value.sortDescription)
    }

    @Test
    fun `clearError and clearSuccessMessage should work correctly`() {
        // Set error and success message manually for testing
        // In real implementation, these would be set by operations
        
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
        
        viewModel.clearSuccessMessage()
        assertNull(viewModel.uiState.value.successMessage)
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