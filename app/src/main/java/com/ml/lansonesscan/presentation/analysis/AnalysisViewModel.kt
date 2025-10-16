package com.ml.lansonesscan.presentation.analysis

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.domain.usecase.AnalyzeImageUseCase
import com.ml.lansonesscan.domain.usecase.SaveScanResultUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Analysis screen
 * Manages image capture, analysis flow, and results display
 */
class AnalysisViewModel(
    private val analyzeImageUseCase: AnalyzeImageUseCase,
    private val saveScanResultUseCase: SaveScanResultUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    /**
     * Sets the selected analysis type
     */
    fun setAnalysisType(analysisType: AnalysisType) {
        _uiState.value = _uiState.value.copy(
            selectedAnalysisType = analysisType,
            error = null
        )
    }

    /**
     * Sets the selected image URI
     */
    fun setImageUri(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            selectedImageUri = uri,
            error = null
        )
    }

    /**
     * Starts the image analysis process
     */
    fun startAnalysis() {
        val currentState = _uiState.value
        
        // Validate required inputs
        if (currentState.selectedImageUri == null) {
            _uiState.value = currentState.copy(
                error = "Please select an image first"
            )
            return
        }
        
        if (currentState.selectedAnalysisType == null) {
            _uiState.value = currentState.copy(
                error = "Please select analysis type first"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAnalyzing = true,
                analysisProgress = 0f,
                error = null,
                analysisResult = null
            )

            try {
                // Update progress to show analysis started
                updateAnalysisProgress(0.05f, "Initializing analysis...")

                // Prepare image
                updateAnalysisProgress(0.1f, "Preparing image...")
                delay(300) // Simulate preparation time

                // Optimize image
                updateAnalysisProgress(0.15f, "Optimizing image...")
                delay(200) // Simulate optimization time

                // Upload image
                updateAnalysisProgress(0.25f, "Uploading image...")
                delay(500) // Simulate upload time

                // Perform the analysis
                updateAnalysisProgress(0.3f, "Analyzing with AI models...")
            
                val result = analyzeImageUseCase(
                    imageUri = currentState.selectedImageUri!!,
                    analysisType = currentState.selectedAnalysisType!!
                )

                result.fold(
                    onSuccess = { scanResult ->
                        updateAnalysisProgress(0.8f, "Processing results...")
                        delay(300) // Simulate processing time
                        
                        updateAnalysisProgress(0.9f, "Saving results...")
                        
                        // Save the scan result
                        saveScanResultUseCase(scanResult).fold(
                            onSuccess = {
                                updateAnalysisProgress(0.95f, "Finalizing...")
                                delay(200) // Simulate finalization time
                                
                                updateAnalysisProgress(1.0f, "Analysis complete!")
                                
                                _uiState.value = _uiState.value.copy(
                                    isAnalyzing = false,
                                    analysisResult = scanResult,
                                    analysisProgress = 1.0f,
                                    analysisStatus = "Analysis complete!",
                                    error = null
                                )
                            },
                            onFailure = { saveError ->
                                // Analysis succeeded but save failed
                                _uiState.value = _uiState.value.copy(
                                    isAnalyzing = false,
                                    analysisResult = scanResult,
                                    analysisProgress = 1.0f,
                                    analysisStatus = "Analysis complete (save failed)",
                                    error = "Analysis completed but failed to save: ${saveError.message}"
                                )
                            }
                        )
                    },
                    onFailure = { analysisError ->
                        _uiState.value = _uiState.value.copy(
                            isAnalyzing = false,
                            analysisProgress = 0f,
                            analysisStatus = "",
                            error = "Analysis failed: ${analysisError.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    analysisProgress = 0f,
                    analysisStatus = "",
                    error = "Unexpected error: ${e.message}"
                )
            }
        }
    }

    /**
     * Updates analysis progress with status message
     */
    private fun updateAnalysisProgress(progress: Float, status: String) {
        _uiState.value = _uiState.value.copy(
            analysisProgress = progress,
            analysisStatus = status
        )
        
        // Add more granular progress updates for better user experience
        when {
            progress <= 0.1f -> {
                // Initial preparation
            }
            progress <= 0.3f -> {
                // Image preparation and upload
            }
            progress <= 0.7f -> {
                // AI analysis in progress
            }
            progress <= 0.9f -> {
                // Processing results
            }
            progress <= 1.0f -> {
                // Finalizing and saving
            }
        }
    }

    /**
     * Retries the analysis with current settings
     */
    fun retryAnalysis() {
        startAnalysis()
    }

    /**
     * Resets the analysis state to start over
     */
    fun resetAnalysis() {
        _uiState.value = AnalysisUiState()
    }

    /**
     * Clears the current error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Handles navigation to scan detail
     */
    fun onViewResultDetails() {
        val scanResult = _uiState.value.analysisResult
        if (scanResult != null) {
            _uiState.value = _uiState.value.copy(
                navigationEvent = AnalysisNavigationEvent.NavigateToScanDetail(scanResult.id)
            )
        }
    }

    /**
     * Handles navigation to dashboard
     */
    fun onNavigateToDashboard() {
        _uiState.value = _uiState.value.copy(
            navigationEvent = AnalysisNavigationEvent.NavigateToDashboard
        )
    }

    /**
     * Clears navigation event after handling
     */
    fun onNavigationEventHandled() {
        _uiState.value = _uiState.value.copy(navigationEvent = null)
    }

    /**
     * Handles camera permission result
     */
    fun onCameraPermissionResult(granted: Boolean) {
        if (!granted) {
            _uiState.value = _uiState.value.copy(
                error = "Camera permission is required to capture images"
            )
        }
    }

    /**
     * Handles storage permission result
     */
    fun onStoragePermissionResult(granted: Boolean) {
        if (!granted) {
            _uiState.value = _uiState.value.copy(
                error = "Storage permission is required to access images"
            )
        }
    }
}

/**
 * UI state for the Analysis screen
 */
data class AnalysisUiState(
    val selectedAnalysisType: AnalysisType? = null,
    val selectedImageUri: Uri? = null,
    val isAnalyzing: Boolean = false,
    val analysisProgress: Float = 0f,
    val analysisStatus: String = "",
    val analysisResult: ScanResult? = null,
    val error: String? = null,
    val navigationEvent: AnalysisNavigationEvent? = null
) {
    /**
     * Returns true if ready to start analysis
     */
    val canStartAnalysis: Boolean
        get() = selectedImageUri != null && 
                selectedAnalysisType != null && 
                !isAnalyzing

    /**
     * Returns true if analysis is complete
     */
    val isAnalysisComplete: Boolean
        get() = !isAnalyzing && analysisResult != null

    /**
     * Returns true if there's an error state
     */
    val hasError: Boolean
        get() = error != null

    /**
     * Returns progress percentage as integer
     */
    val progressPercentage: Int
        get() = (analysisProgress * 100).toInt()

    /**
     * Returns formatted analysis status for display
     */
    val displayStatus: String
        get() = when {
            isAnalyzing -> "$analysisStatus ($progressPercentage%)"
            isAnalysisComplete -> "Analysis Complete"
            hasError -> "Analysis Failed"
            else -> ""
        }
}

/**
 * Navigation events for the Analysis screen
 */
sealed class AnalysisNavigationEvent {
    data class NavigateToScanDetail(val scanId: String) : AnalysisNavigationEvent()
    object NavigateToDashboard : AnalysisNavigationEvent()
}