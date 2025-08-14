package com.ml.lansonesscan.presentation.scandetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.domain.usecase.DeleteScanUseCase
import com.ml.lansonesscan.domain.usecase.GetScanByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScanDetailUiState(
    val isLoading: Boolean = true,
    val scanResult: ScanResult? = null,
    val error: String? = null,
    val isDeleted: Boolean = false
)

class ScanDetailViewModel(
    private val getScanByIdUseCase: GetScanByIdUseCase,
    private val deleteScanUseCase: DeleteScanUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanDetailUiState())
    val uiState: StateFlow<ScanDetailUiState> = _uiState.asStateFlow()

    fun loadScanDetails(scanId: String) {
        if (scanId.isBlank()) {
            _uiState.value = ScanDetailUiState(isLoading = false, error = "Invalid Scan ID.")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = ScanDetailUiState(isLoading = true)
            getScanByIdUseCase(scanId).fold(
                onSuccess = { scanResult ->
                    if (scanResult != null) {
                        _uiState.value = ScanDetailUiState(isLoading = false, scanResult = scanResult)
                    } else {
                        _uiState.value = ScanDetailUiState(isLoading = false, error = "Scan not found.")
                    }
                },
                onFailure = {
                    _uiState.value = ScanDetailUiState(isLoading = false, error = "Failed to load scan details.")
                }
            )
        }
    }

    fun deleteScan(scanId: String) {
        viewModelScope.launch {
            deleteScanUseCase(scanId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isDeleted = true)
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(error = "Failed to delete scan.")
                }
            )
        }
    }
}
