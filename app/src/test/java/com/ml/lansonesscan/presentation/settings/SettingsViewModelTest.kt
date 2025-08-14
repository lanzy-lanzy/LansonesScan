package com.ml.lansonesscan.presentation.settings

import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.usecase.ClearHistoryUseCase
import com.ml.lansonesscan.domain.usecase.GetStorageInfoUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class SettingsViewModelTest {

    private lateinit var viewModel: SettingsViewModel
    private lateinit var getStorageInfoUseCase: GetStorageInfoUseCase
    private lateinit var clearHistoryUseCase: ClearHistoryUseCase
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getStorageInfoUseCase = mockk()
        clearHistoryUseCase = mockk()
        viewModel = SettingsViewModel(getStorageInfoUseCase, clearHistoryUseCase)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be loading storage`() {
        val initialState = viewModel.uiState.value
        assertTrue(initialState.isLoadingStorage)
        assertNull(initialState.storageInfo)
        assertNull(initialState.error)
        assertEquals(ThemePreference.SYSTEM, initialState.themePreference)
        assertTrue(initialState.analysisNotificationsEnabled)
        assertFalse(initialState.automaticCleanupEnabled)
        assertEquals(30, initialState.cleanupThresholdDays)
    }

    @Test
    fun `loadStorageInfo should load storage information successfully`() = runTest {
        // Given
        val mockStorageInfo = createMockStorageInfo()
        val mockStorageBreakdown = createMockStorageBreakdown()
        val mockRecommendations = listOf(
            GetStorageInfoUseCase.StorageRecommendation(
                type = GetStorageInfoUseCase.RecommendationType.HIGH_STORAGE_USAGE,
                title = "High Storage Usage",
                description = "Consider clearing old scans",
                priority = GetStorageInfoUseCase.RecommendationPriority.HIGH
            )
        )

        coEvery { getStorageInfoUseCase() } returns mockStorageInfo
        coEvery { getStorageInfoUseCase.getStorageBreakdown() } returns mockStorageBreakdown
        coEvery { getStorageInfoUseCase.getStorageRecommendations() } returns mockRecommendations

        // When
        viewModel.loadStorageInfo()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoadingStorage)
        assertEquals(mockStorageInfo, finalState.storageInfo)
        assertEquals(mockStorageBreakdown, finalState.storageBreakdown)
        assertEquals(mockRecommendations, finalState.storageRecommendations)
        assertNull(finalState.error)
        assertTrue(finalState.hasHighPriorityRecommendations)
        assertEquals(1, finalState.highPriorityRecommendationCount)
    }

    @Test
    fun `loadStorageInfo should handle error when loading fails`() = runTest {
        // Given
        val errorMessage = "Storage error"
        coEvery { getStorageInfoUseCase() } throws Exception(errorMessage)

        // When
        viewModel.loadStorageInfo()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoadingStorage)
        assertNull(finalState.storageInfo)
        assertTrue(finalState.error?.contains(errorMessage) == true)
    }

    @Test
    fun `refreshStorageInfo should reload storage information`() = runTest {
        // Given
        val mockStorageInfo = createMockStorageInfo()
        coEvery { getStorageInfoUseCase() } returns mockStorageInfo
        coEvery { getStorageInfoUseCase.getStorageBreakdown() } returns createMockStorageBreakdown()
        coEvery { getStorageInfoUseCase.getStorageRecommendations() } returns emptyList()

        // When
        viewModel.refreshStorageInfo()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 2) { getStorageInfoUseCase() } // Initial load + refresh
    }

    @Test
    fun `showClearHistoryConfirmation should load preview and show dialog`() = runTest {
        // Given
        val mockPreview = ClearHistoryUseCase.ClearHistoryPreview(
            totalScans = 10,
            diseaseDetectedCount = 3,
            healthyScansCount = 7,
            totalStorageSize = 1024L
        )
        coEvery { clearHistoryUseCase.getPreview() } returns mockPreview

        // When
        viewModel.showClearHistoryConfirmation()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(mockPreview, state.clearHistoryPreview)
        assertTrue(state.showClearHistoryDialog)
    }

    @Test
    fun `confirmClearHistory should clear history successfully`() = runTest {
        // Given
        val mockResult = ClearHistoryUseCase.ClearHistoryResult(
            scansCleared = 10,
            storageFreed = 1024L,
            operationTime = 1000L
        )
        val mockStorageInfo = createMockStorageInfo()
        
        coEvery { clearHistoryUseCase() } returns Result.success(mockResult)
        coEvery { getStorageInfoUseCase() } returns mockStorageInfo
        coEvery { getStorageInfoUseCase.getStorageBreakdown() } returns createMockStorageBreakdown()
        coEvery { getStorageInfoUseCase.getStorageRecommendations() } returns emptyList()

        // When
        viewModel.confirmClearHistory()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isClearingHistory)
        assertFalse(finalState.showClearHistoryDialog)
        assertNull(finalState.clearHistoryPreview)
        assertTrue(finalState.successMessage?.contains("Cleared 10 scans") == true)
        
        coVerify { clearHistoryUseCase() }
    }

    @Test
    fun `confirmClearHistory should handle failure`() = runTest {
        // Given
        val errorMessage = "Clear failed"
        coEvery { clearHistoryUseCase() } returns Result.failure(Exception(errorMessage))

        // When
        viewModel.confirmClearHistory()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isClearingHistory)
        assertNull(finalState.clearHistoryPreview)
        assertTrue(finalState.error?.contains(errorMessage) == true)
    }

    @Test
    fun `cancelClearHistory should clear dialog state`() {
        // Given
        viewModel.uiState.value.copy(
            showClearHistoryDialog = true,
            clearHistoryPreview = ClearHistoryUseCase.ClearHistoryPreview(0, 0, 0, 0L)
        )

        // When
        viewModel.cancelClearHistory()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showClearHistoryDialog)
        assertNull(state.clearHistoryPreview)
    }

    @Test
    fun `cleanupOrphanedFiles should cleanup successfully`() = runTest {
        // Given
        val orphanedCount = 5
        val mockStorageInfo = createMockStorageInfo()
        
        coEvery { clearHistoryUseCase.cleanupOrphanedFiles() } returns Result.success(orphanedCount)
        coEvery { getStorageInfoUseCase() } returns mockStorageInfo
        coEvery { getStorageInfoUseCase.getStorageBreakdown() } returns createMockStorageBreakdown()
        coEvery { getStorageInfoUseCase.getStorageRecommendations() } returns emptyList()

        // When
        viewModel.cleanupOrphanedFiles()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isCleaningOrphaned)
        assertEquals("Cleaned up 5 orphaned files", finalState.successMessage)
        
        coVerify { clearHistoryUseCase.cleanupOrphanedFiles() }
    }

    @Test
    fun `cleanupOrphanedFiles should handle no orphaned files`() = runTest {
        // Given
        val mockStorageInfo = createMockStorageInfo()
        
        coEvery { clearHistoryUseCase.cleanupOrphanedFiles() } returns Result.success(0)
        coEvery { getStorageInfoUseCase() } returns mockStorageInfo
        coEvery { getStorageInfoUseCase.getStorageBreakdown() } returns createMockStorageBreakdown()
        coEvery { getStorageInfoUseCase.getStorageRecommendations() } returns emptyList()

        // When
        viewModel.cleanupOrphanedFiles()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertEquals("No orphaned files found", finalState.successMessage)
    }

    @Test
    fun `dialog visibility methods should work correctly`() {
        // Test app info dialog
        viewModel.showAppInfo()
        assertTrue(viewModel.uiState.value.showAppInfoDialog)
        
        viewModel.hideAppInfo()
        assertFalse(viewModel.uiState.value.showAppInfoDialog)

        // Test storage breakdown dialog
        viewModel.showStorageBreakdown()
        assertTrue(viewModel.uiState.value.showStorageBreakdownDialog)
        
        viewModel.hideStorageBreakdown()
        assertFalse(viewModel.uiState.value.showStorageBreakdownDialog)
    }

    @Test
    fun `navigation events should be set correctly`() {
        // Test privacy policy
        viewModel.showPrivacyPolicy()
        assertEquals(
            SettingsNavigationEvent.ShowPrivacyPolicy,
            viewModel.uiState.value.navigationEvent
        )

        // Test terms of service
        viewModel.showTermsOfService()
        assertEquals(
            SettingsNavigationEvent.ShowTermsOfService,
            viewModel.uiState.value.navigationEvent
        )

        // Test developer website
        viewModel.openDeveloperWebsite()
        assertEquals(
            SettingsNavigationEvent.OpenDeveloperWebsite,
            viewModel.uiState.value.navigationEvent
        )

        // Test share app
        viewModel.shareApp()
        assertEquals(
            SettingsNavigationEvent.ShareApp,
            viewModel.uiState.value.navigationEvent
        )

        // Test rate app
        viewModel.rateApp()
        assertEquals(
            SettingsNavigationEvent.RateApp,
            viewModel.uiState.value.navigationEvent
        )

        // Test send feedback
        viewModel.sendFeedback()
        assertEquals(
            SettingsNavigationEvent.SendFeedback,
            viewModel.uiState.value.navigationEvent
        )

        // Test export data
        viewModel.exportScanData()
        assertEquals(
            SettingsNavigationEvent.ExportData,
            viewModel.uiState.value.navigationEvent
        )
    }

    @Test
    fun `toggleTheme should cycle through theme preferences`() {
        // Initial state is SYSTEM
        assertEquals(ThemePreference.SYSTEM, viewModel.uiState.value.themePreference)

        // SYSTEM -> LIGHT
        viewModel.toggleTheme()
        assertEquals(ThemePreference.LIGHT, viewModel.uiState.value.themePreference)

        // LIGHT -> DARK
        viewModel.toggleTheme()
        assertEquals(ThemePreference.DARK, viewModel.uiState.value.themePreference)

        // DARK -> SYSTEM
        viewModel.toggleTheme()
        assertEquals(ThemePreference.SYSTEM, viewModel.uiState.value.themePreference)
    }

    @Test
    fun `preference toggles should update state correctly`() {
        // Test analysis notifications
        viewModel.toggleAnalysisNotifications(false)
        assertFalse(viewModel.uiState.value.analysisNotificationsEnabled)

        viewModel.toggleAnalysisNotifications(true)
        assertTrue(viewModel.uiState.value.analysisNotificationsEnabled)

        // Test automatic cleanup
        viewModel.toggleAutomaticCleanup(true)
        assertTrue(viewModel.uiState.value.automaticCleanupEnabled)

        viewModel.toggleAutomaticCleanup(false)
        assertFalse(viewModel.uiState.value.automaticCleanupEnabled)

        // Test cleanup threshold
        viewModel.updateCleanupThreshold(7)
        assertEquals(7, viewModel.uiState.value.cleanupThresholdDays)
    }

    @Test
    fun `onNavigationEventHandled should clear navigation event`() {
        // Given
        viewModel.shareApp()

        // When
        viewModel.onNavigationEventHandled()

        // Then
        assertNull(viewModel.uiState.value.navigationEvent)
    }

    @Test
    fun `clearError and clearSuccessMessage should work correctly`() {
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
        
        viewModel.clearSuccessMessage()
        assertNull(viewModel.uiState.value.successMessage)
    }

    @Test
    fun `UI state computed properties should work correctly`() = runTest {
        // Given
        val mockStorageInfo = createMockStorageInfo()
        coEvery { getStorageInfoUseCase() } returns mockStorageInfo
        coEvery { getStorageInfoUseCase.getStorageBreakdown() } returns createMockStorageBreakdown()
        coEvery { getStorageInfoUseCase.getStorageRecommendations() } returns emptyList()

        viewModel.loadStorageInfo()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Test formatted storage info
        assertTrue(state.formattedStorageInfo.contains("Total Scans: 10"))
        assertTrue(state.formattedStorageInfo.contains("Storage Used: 1 KB"))

        // Test theme display name
        assertEquals("System Default", state.themeDisplayName)
        
        viewModel.toggleTheme() // Should change to LIGHT
        assertEquals("Light", viewModel.uiState.value.themeDisplayName)

        // Test cleanup threshold display text
        assertEquals("1 month", state.cleanupThresholdDisplayText)
        
        viewModel.updateCleanupThreshold(7)
        assertEquals("1 week", viewModel.uiState.value.cleanupThresholdDisplayText)

        // Test app version info
        val appVersionInfo = state.appVersionInfo
        assertEquals("1.0.0", appVersionInfo.versionName)
        assertEquals(1, appVersionInfo.versionCode)
        assertEquals("Lansones Scanner Team", appVersionInfo.developer)
    }

    private fun createMockStorageInfo(): GetStorageInfoUseCase.StorageInfo {
        return GetStorageInfoUseCase.StorageInfo(
            totalScans = 10,
            diseaseDetectedCount = 3,
            healthyScansCount = 7,
            totalStorageSize = 1024L,
            fruitScansCount = 6,
            leafScansCount = 4,
            lastUpdated = System.currentTimeMillis()
        )
    }

    private fun createMockStorageBreakdown(): GetStorageInfoUseCase.StorageBreakdown {
        return GetStorageInfoUseCase.StorageBreakdown(
            fruitAnalysis = GetStorageInfoUseCase.AnalysisTypeStorage(
                type = AnalysisType.FRUIT,
                scanCount = 6,
                estimatedStorageSize = 600L
            ),
            leafAnalysis = GetStorageInfoUseCase.AnalysisTypeStorage(
                type = AnalysisType.LEAVES,
                scanCount = 4,
                estimatedStorageSize = 424L
            )
        )
    }
}