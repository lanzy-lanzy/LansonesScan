package com.ml.lansonesscan.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.domain.usecase.GetScanHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel for the Dashboard screen
 * Manages recent scans, statistics, and quick actions
 */
class DashboardViewModel(
    private val getScanHistoryUseCase: GetScanHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    /**
     * Loads dashboard data including recent scans and statistics
     */
    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load recent scans (limit to 5 for dashboard)
                getScanHistoryUseCase.getRecentScans(5)
                    .catch { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load recent scans: ${error.message}"
                        )
                    }
                    .collect { recentScans ->
                        // Load statistics
                        val statistics = getScanHistoryUseCase.getScanStatistics()
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            recentScans = recentScans,
                            statistics = statistics,
                            error = null
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load dashboard data: ${e.message}"
                )
            }
        }
    }

    /**
     * Refreshes dashboard data
     */
    fun refresh() {
        loadDashboardData()
    }

    /**
     * Handles navigation to analysis screen
     */
    fun onQuickScanClicked() {
        _uiState.value = _uiState.value.copy(
            navigationEvent = DashboardNavigationEvent.NavigateToAnalysis
        )
    }

    /**
     * Handles navigation to history screen
     */
    fun onViewAllHistoryClicked() {
        _uiState.value = _uiState.value.copy(
            navigationEvent = DashboardNavigationEvent.NavigateToHistory
        )
    }

    /**
     * Handles navigation to scan details
     */
    fun onScanItemClicked(scanResult: ScanResult) {
        _uiState.value = _uiState.value.copy(
            navigationEvent = DashboardNavigationEvent.NavigateToScanDetail(scanResult.id)
        )
    }

    /**
     * Clears navigation event after handling
     */
    fun onNavigationEventHandled() {
        _uiState.value = _uiState.value.copy(navigationEvent = null)
    }

    /**
     * Clears error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for the Dashboard screen
 */
data class DashboardUiState(
    val isLoading: Boolean = false,
    val recentScans: List<ScanResult> = emptyList(),
    val statistics: GetScanHistoryUseCase.ScanStatistics? = null,
    val error: String? = null,
    val navigationEvent: DashboardNavigationEvent? = null
) {
    /**
     * Returns true if there are no scans (empty state)
     */
    val isEmpty: Boolean
        get() = !isLoading && recentScans.isEmpty() && statistics?.totalScans == 0

    /**
     * Returns true if data is available
     */
    val hasData: Boolean
        get() = !isLoading && (recentScans.isNotEmpty() || statistics?.totalScans ?: 0 > 0)

    /**
     * Returns formatted total scans text
     */
    val totalScansText: String
        get() = statistics?.totalScans?.toString() ?: "0"

    /**
     * Returns formatted disease detection rate
     */
    val diseaseDetectionRateText: String
        get() = statistics?.getFormattedDetectionRate() ?: "0.0%"

    /**
     * Returns formatted storage size
     */
    val storageSizeText: String
        get() = statistics?.getFormattedStorageSize() ?: "0 B"
}

/**
 * Navigation events for the Dashboard screen
 */
sealed class DashboardNavigationEvent {
    object NavigateToAnalysis : DashboardNavigationEvent()
    object NavigateToHistory : DashboardNavigationEvent()
    data class NavigateToScanDetail(val scanId: String) : DashboardNavigationEvent()
}