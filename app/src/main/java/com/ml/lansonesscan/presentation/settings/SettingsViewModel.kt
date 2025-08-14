package com.ml.lansonesscan.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ml.lansonesscan.domain.usecase.ClearHistoryUseCase
import com.ml.lansonesscan.domain.usecase.GetStorageInfoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Settings screen
 * Manages configuration, storage information, and cleanup operations
 */
class SettingsViewModel(
    private val getStorageInfoUseCase: GetStorageInfoUseCase,
    private val clearHistoryUseCase: ClearHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadStorageInfo()
    }

    /**
     * Loads storage information for display
     */
    fun loadStorageInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingStorage = true, error = null)

            try {
                val storageInfo = getStorageInfoUseCase()
                val storageBreakdown = getStorageInfoUseCase.getStorageBreakdown()
                val recommendations = getStorageInfoUseCase.getStorageRecommendations()

                _uiState.value = _uiState.value.copy(
                    isLoadingStorage = false,
                    storageInfo = storageInfo,
                    storageBreakdown = storageBreakdown,
                    storageRecommendations = recommendations,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingStorage = false,
                    error = "Failed to load storage information: ${e.message}"
                )
            }
        }
    }

    /**
     * Refreshes storage information
     */
    fun refreshStorageInfo() {
        loadStorageInfo()
    }

    /**
     * Shows clear history confirmation dialog
     */
    fun showClearHistoryConfirmation() {
        viewModelScope.launch {
            try {
                val preview = clearHistoryUseCase.getPreview()
                _uiState.value = _uiState.value.copy(
                    clearHistoryPreview = preview,
                    showClearHistoryDialog = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to get clear history preview: ${e.message}"
                )
            }
        }
    }

    /**
     * Confirms and executes clear history operation
     */
    fun confirmClearHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isClearingHistory = true,
                showClearHistoryDialog = false
            )

            clearHistoryUseCase().fold(
                onSuccess = { result ->
                    _uiState.value = _uiState.value.copy(
                        isClearingHistory = false,
                        clearHistoryPreview = null,
                        successMessage = "Cleared ${result.scansCleared} scans and freed ${result.getFormattedStorageFreed()}"
                    )
                    // Reload storage info to reflect changes
                    loadStorageInfo()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isClearingHistory = false,
                        clearHistoryPreview = null,
                        error = "Failed to clear history: ${error.message}"
                    )
                }
            )
        }
    }

    /**
     * Cancels clear history operation
     */
    fun cancelClearHistory() {
        _uiState.value = _uiState.value.copy(
            showClearHistoryDialog = false,
            clearHistoryPreview = null
        )
    }

    /**
     * Cleans up orphaned files
     */
    fun cleanupOrphanedFiles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCleaningOrphaned = true, error = null)

            clearHistoryUseCase.cleanupOrphanedFiles().fold(
                onSuccess = { orphanedCount ->
                    _uiState.value = _uiState.value.copy(
                        isCleaningOrphaned = false,
                        successMessage = if (orphanedCount > 0) {
                            "Cleaned up $orphanedCount orphaned files"
                        } else {
                            "No orphaned files found"
                        }
                    )
                    // Reload storage info to reflect changes
                    loadStorageInfo()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isCleaningOrphaned = false,
                        error = "Failed to cleanup orphaned files: ${error.message}"
                    )
                }
            )
        }
    }

    /**
     * Shows app information dialog
     */
    fun showAppInfo() {
        _uiState.value = _uiState.value.copy(showAppInfoDialog = true)
    }

    /**
     * Hides app information dialog
     */
    fun hideAppInfo() {
        _uiState.value = _uiState.value.copy(showAppInfoDialog = false)
    }

    /**
     * Shows privacy policy
     */
    fun showPrivacyPolicy() {
        _uiState.value = _uiState.value.copy(
            navigationEvent = SettingsNavigationEvent.ShowPrivacyPolicy
        )
    }

    /**
     * Shows terms of service
     */
    fun showTermsOfService() {
        _uiState.value = _uiState.value.copy(
            navigationEvent = SettingsNavigationEvent.ShowTermsOfService
        )
    }

    /**
     * Opens developer website
     */
    fun openDeveloperWebsite() {
        _uiState.value = _uiState.value.copy(
            navigationEvent = SettingsNavigationEvent.OpenDeveloperWebsite
        )
    }

    /**
     * Shares app with others
     */
    fun shareApp() {
        _uiState.value = _uiState.value.copy(
            navigationEvent = SettingsNavigationEvent.ShareApp
        )
    }

    /**
     * Opens app in Play Store for rating
     */
    fun rateApp() {
        _uiState.value = _uiState.value.copy(
            navigationEvent = SettingsNavigationEvent.RateApp
        )
    }

    /**
     * Sends feedback email
     */
    fun sendFeedback() {
        _uiState.value = _uiState.value.copy(
            navigationEvent = SettingsNavigationEvent.SendFeedback
        )
    }

    /**
     * Toggles theme preference (light/dark/system)
     */
    fun toggleTheme() {
        val currentTheme = _uiState.value.themePreference
        val newTheme = when (currentTheme) {
            ThemePreference.LIGHT -> ThemePreference.DARK
            ThemePreference.DARK -> ThemePreference.SYSTEM
            ThemePreference.SYSTEM -> ThemePreference.LIGHT
        }
        
        _uiState.value = _uiState.value.copy(themePreference = newTheme)
        
        // In a real implementation, you would save this preference to DataStore or SharedPreferences
        // For now, we'll just update the UI state
    }

    /**
     * Toggles analysis notifications
     */
    fun toggleAnalysisNotifications(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(analysisNotificationsEnabled = enabled)
        
        // In a real implementation, you would save this preference
    }

    /**
     * Toggles automatic cleanup
     */
    fun toggleAutomaticCleanup(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(automaticCleanupEnabled = enabled)
        
        // In a real implementation, you would save this preference and configure cleanup
    }

    /**
     * Updates cleanup threshold
     */
    fun updateCleanupThreshold(threshold: Int) {
        _uiState.value = _uiState.value.copy(cleanupThresholdDays = threshold)
        
        // In a real implementation, you would save this preference
    }

    /**
     * Clears navigation event after handling
     */
    fun onNavigationEventHandled() {
        _uiState.value = _uiState.value.copy(navigationEvent = null)
    }

    /**
     * Clears error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Clears success message
     */
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    /**
     * Exports scan data
     */
    fun exportScanData() {
        _uiState.value = _uiState.value.copy(
            navigationEvent = SettingsNavigationEvent.ExportData
        )
    }

    /**
     * Shows storage breakdown details
     */
    fun showStorageBreakdown() {
        _uiState.value = _uiState.value.copy(showStorageBreakdownDialog = true)
    }

    /**
     * Hides storage breakdown details
     */
    fun hideStorageBreakdown() {
        _uiState.value = _uiState.value.copy(showStorageBreakdownDialog = false)
    }
}

/**
 * UI state for the Settings screen
 */
data class SettingsUiState(
    val isLoadingStorage: Boolean = false,
    val storageInfo: GetStorageInfoUseCase.StorageInfo? = null,
    val storageBreakdown: GetStorageInfoUseCase.StorageBreakdown? = null,
    val storageRecommendations: List<GetStorageInfoUseCase.StorageRecommendation> = emptyList(),
    val clearHistoryPreview: ClearHistoryUseCase.ClearHistoryPreview? = null,
    val showClearHistoryDialog: Boolean = false,
    val isClearingHistory: Boolean = false,
    val isCleaningOrphaned: Boolean = false,
    val showAppInfoDialog: Boolean = false,
    val showStorageBreakdownDialog: Boolean = false,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val analysisNotificationsEnabled: Boolean = true,
    val automaticCleanupEnabled: Boolean = false,
    val cleanupThresholdDays: Int = 30,
    val error: String? = null,
    val successMessage: String? = null,
    val navigationEvent: SettingsNavigationEvent? = null
) {
    /**
     * Returns formatted storage information
     */
    val formattedStorageInfo: String
        get() = storageInfo?.let { info ->
            buildString {
                appendLine("Total Scans: ${info.totalScans}")
                appendLine("Storage Used: ${info.getFormattedStorageSize()}")
                appendLine("Disease Detection Rate: ${info.getDiseaseDetectionRate().toInt()}%")
                appendLine("Average per Scan: ${info.getFormattedAverageStoragePerScan()}")
            }
        } ?: "No storage information available"

    /**
     * Returns true if there are high priority recommendations
     */
    val hasHighPriorityRecommendations: Boolean
        get() = storageRecommendations.any { 
            it.priority == GetStorageInfoUseCase.RecommendationPriority.HIGH 
        }

    /**
     * Returns the number of high priority recommendations
     */
    val highPriorityRecommendationCount: Int
        get() = storageRecommendations.count { 
            it.priority == GetStorageInfoUseCase.RecommendationPriority.HIGH 
        }

    /**
     * Returns theme preference display name
     */
    val themeDisplayName: String
        get() = when (themePreference) {
            ThemePreference.LIGHT -> "Light"
            ThemePreference.DARK -> "Dark"
            ThemePreference.SYSTEM -> "System Default"
        }

    /**
     * Returns cleanup threshold display text
     */
    val cleanupThresholdDisplayText: String
        get() = when (cleanupThresholdDays) {
            1 -> "1 day"
            7 -> "1 week"
            30 -> "1 month"
            90 -> "3 months"
            else -> "$cleanupThresholdDays days"
        }

    /**
     * Returns app version information
     */
    val appVersionInfo: AppVersionInfo
        get() = AppVersionInfo(
            versionName = "1.0.0", // This would come from BuildConfig in real implementation
            versionCode = 1,
            buildDate = "2024-01-01", // This would come from build configuration
            developer = "Lansones Scanner Team"
        )
}

/**
 * Theme preference options
 */
enum class ThemePreference {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * App version information
 */
data class AppVersionInfo(
    val versionName: String,
    val versionCode: Int,
    val buildDate: String,
    val developer: String
)

/**
 * Navigation events for the Settings screen
 */
sealed class SettingsNavigationEvent {
    object ShowPrivacyPolicy : SettingsNavigationEvent()
    object ShowTermsOfService : SettingsNavigationEvent()
    object OpenDeveloperWebsite : SettingsNavigationEvent()
    object ShareApp : SettingsNavigationEvent()
    object RateApp : SettingsNavigationEvent()
    object SendFeedback : SettingsNavigationEvent()
    object ExportData : SettingsNavigationEvent()
}