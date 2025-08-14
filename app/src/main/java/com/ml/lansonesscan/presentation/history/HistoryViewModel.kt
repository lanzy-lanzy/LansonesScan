package com.ml.lansonesscan.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.domain.usecase.DeleteScanUseCase
import com.ml.lansonesscan.domain.usecase.GetScanHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel for the History screen
 * Manages scan list, filtering, sorting, and operations
 */
class HistoryViewModel(
    private val getScanHistoryUseCase: GetScanHistoryUseCase,
    private val deleteScanUseCase: DeleteScanUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    /**
     * Loads scan history with current filters and sorting
     */
    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val params = GetScanHistoryUseCase.Params(
                    analysisType = _uiState.value.selectedAnalysisType,
                    diseaseDetected = _uiState.value.selectedDiseaseStatus,
                    searchQuery = _uiState.value.searchQuery.takeIf { it.isNotBlank() },
                    sortBy = _uiState.value.sortBy
                )

                getScanHistoryUseCase(params)
                    .catch { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load history: ${error.message}"
                        )
                    }
                    .collect { scanResults ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            scanResults = scanResults,
                            error = null
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load history: ${e.message}"
                )
            }
        }
    }

    /**
     * Refreshes the history data
     */
    fun refresh() {
        loadHistory()
    }

    /**
     * Updates the search query and reloads history
     */
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadHistory()
    }

    /**
     * Updates the analysis type filter
     */
    fun updateAnalysisTypeFilter(analysisType: AnalysisType?) {
        _uiState.value = _uiState.value.copy(selectedAnalysisType = analysisType)
        loadHistory()
    }

    /**
     * Updates the disease status filter
     */
    fun updateDiseaseStatusFilter(diseaseDetected: Boolean?) {
        _uiState.value = _uiState.value.copy(selectedDiseaseStatus = diseaseDetected)
        loadHistory()
    }

    /**
     * Updates the sorting option
     */
    fun updateSortBy(sortBy: GetScanHistoryUseCase.SortBy) {
        _uiState.value = _uiState.value.copy(sortBy = sortBy)
        loadHistory()
    }

    /**
     * Clears all filters and reloads history
     */
    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            selectedAnalysisType = null,
            selectedDiseaseStatus = null,
            sortBy = GetScanHistoryUseCase.SortBy.DATE_DESC
        )
        loadHistory()
    }

    /**
     * Handles scan item click for navigation
     */
    fun onScanItemClicked(scanResult: ScanResult) {
        _uiState.value = _uiState.value.copy(
            navigationEvent = HistoryNavigationEvent.NavigateToScanDetail(scanResult.id)
        )
    }

    /**
     * Shows delete confirmation dialog
     */
    fun showDeleteConfirmation(scanResult: ScanResult) {
        _uiState.value = _uiState.value.copy(
            scanToDelete = scanResult,
            showDeleteConfirmation = true
        )
    }

    /**
     * Confirms and executes scan deletion
     */
    fun confirmDelete() {
        val scanToDelete = _uiState.value.scanToDelete
        if (scanToDelete != null) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(
                    isDeleting = true,
                    showDeleteConfirmation = false
                )

                deleteScanUseCase(scanToDelete.id).fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isDeleting = false,
                            scanToDelete = null,
                            successMessage = "Scan deleted successfully"
                        )
                        // Reload history to reflect changes
                        loadHistory()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isDeleting = false,
                            scanToDelete = null,
                            error = "Failed to delete scan: ${error.message}"
                        )
                    }
                )
            }
        }
    }

    /**
     * Cancels delete operation
     */
    fun cancelDelete() {
        _uiState.value = _uiState.value.copy(
            scanToDelete = null,
            showDeleteConfirmation = false
        )
    }

    /**
     * Handles share scan action
     */
    fun shareScan(scanResult: ScanResult) {
        _uiState.value = _uiState.value.copy(
            navigationEvent = HistoryNavigationEvent.ShareScan(scanResult)
        )
    }

    /**
     * Shows filter options dialog
     */
    fun showFilterOptions() {
        _uiState.value = _uiState.value.copy(showFilterDialog = true)
    }

    /**
     * Hides filter options dialog
     */
    fun hideFilterOptions() {
        _uiState.value = _uiState.value.copy(showFilterDialog = false)
    }

    /**
     * Shows sort options dialog
     */
    fun showSortOptions() {
        _uiState.value = _uiState.value.copy(showSortDialog = true)
    }

    /**
     * Hides sort options dialog
     */
    fun hideSortOptions() {
        _uiState.value = _uiState.value.copy(showSortDialog = false)
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
     * Toggles selection mode for batch operations
     */
    fun toggleSelectionMode() {
        val currentMode = _uiState.value.isSelectionMode
        _uiState.value = _uiState.value.copy(
            isSelectionMode = !currentMode,
            selectedScans = if (currentMode) emptySet() else _uiState.value.selectedScans
        )
    }

    /**
     * Toggles selection of a scan item
     */
    fun toggleScanSelection(scanId: String) {
        val currentSelected = _uiState.value.selectedScans
        val newSelected = if (currentSelected.contains(scanId)) {
            currentSelected - scanId
        } else {
            currentSelected + scanId
        }
        
        _uiState.value = _uiState.value.copy(selectedScans = newSelected)
    }

    /**
     * Selects all visible scans
     */
    fun selectAllScans() {
        val allScanIds = _uiState.value.scanResults.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedScans = allScanIds)
    }

    /**
     * Clears all selections
     */
    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedScans = emptySet())
    }

    /**
     * Deletes selected scans
     */
    fun deleteSelectedScans() {
        val selectedIds = _uiState.value.selectedScans.toList()
        if (selectedIds.isNotEmpty()) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isDeleting = true)

                deleteScanUseCase.deleteBatch(selectedIds).fold(
                    onSuccess = { result ->
                        _uiState.value = _uiState.value.copy(
                            isDeleting = false,
                            isSelectionMode = false,
                            selectedScans = emptySet(),
                            successMessage = "Deleted ${result.successCount} of ${result.totalRequested} scans"
                        )
                        
                        if (result.failureCount > 0) {
                            _uiState.value = _uiState.value.copy(
                                error = "Some scans could not be deleted"
                            )
                        }
                        
                        // Reload history to reflect changes
                        loadHistory()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isDeleting = false,
                            error = "Failed to delete selected scans: ${error.message}"
                        )
                    }
                )
            }
        }
    }
}

/**
 * UI state for the History screen
 */
data class HistoryUiState(
    val isLoading: Boolean = false,
    val scanResults: List<ScanResult> = emptyList(),
    val searchQuery: String = "",
    val selectedAnalysisType: AnalysisType? = null,
    val selectedDiseaseStatus: Boolean? = null,
    val sortBy: GetScanHistoryUseCase.SortBy = GetScanHistoryUseCase.SortBy.DATE_DESC,
    val error: String? = null,
    val successMessage: String? = null,
    val navigationEvent: HistoryNavigationEvent? = null,
    val scanToDelete: ScanResult? = null,
    val showDeleteConfirmation: Boolean = false,
    val isDeleting: Boolean = false,
    val showFilterDialog: Boolean = false,
    val showSortDialog: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedScans: Set<String> = emptySet()
) {
    /**
     * Returns true if there are no scans (empty state)
     */
    val isEmpty: Boolean
        get() = !isLoading && scanResults.isEmpty()

    /**
     * Returns true if filters are active
     */
    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() || 
                selectedAnalysisType != null || 
                selectedDiseaseStatus != null ||
                sortBy != GetScanHistoryUseCase.SortBy.DATE_DESC

    /**
     * Returns the number of selected scans
     */
    val selectedCount: Int
        get() = selectedScans.size

    /**
     * Returns true if all visible scans are selected
     */
    val isAllSelected: Boolean
        get() = selectedScans.size == scanResults.size && scanResults.isNotEmpty()

    /**
     * Returns formatted filter summary
     */
    val filterSummary: String
        get() = buildString {
            val filters = mutableListOf<String>()
            
            selectedAnalysisType?.let { 
                filters.add(it.getDisplayName())
            }
            
            selectedDiseaseStatus?.let { diseaseStatus ->
                filters.add(if (diseaseStatus) "Disease Detected" else "Healthy")
            }
            
            if (searchQuery.isNotBlank()) {
                filters.add("Search: \"$searchQuery\"")
            }
            
            if (filters.isNotEmpty()) {
                append("Filtered by: ${filters.joinToString(", ")}")
            }
        }

    /**
     * Returns formatted sort description
     */
    val sortDescription: String
        get() = when (sortBy) {
            GetScanHistoryUseCase.SortBy.DATE_DESC -> "Newest First"
            GetScanHistoryUseCase.SortBy.DATE_ASC -> "Oldest First"
            GetScanHistoryUseCase.SortBy.CONFIDENCE_DESC -> "Highest Confidence"
            GetScanHistoryUseCase.SortBy.CONFIDENCE_ASC -> "Lowest Confidence"
            GetScanHistoryUseCase.SortBy.ANALYSIS_TYPE -> "Analysis Type"
            GetScanHistoryUseCase.SortBy.DISEASE_STATUS -> "Disease Status"
        }
}

/**
 * Navigation events for the History screen
 */
sealed class HistoryNavigationEvent {
    data class NavigateToScanDetail(val scanId: String) : HistoryNavigationEvent()
    data class ShareScan(val scanResult: ScanResult) : HistoryNavigationEvent()
}