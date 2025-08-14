package com.ml.lansonesscan.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ml.lansonesscan.data.local.database.LansonesDatabase
import com.ml.lansonesscan.data.local.storage.ImageStorageManager
import com.ml.lansonesscan.data.remote.api.GeminiApiClient
import com.ml.lansonesscan.data.remote.api.GeminiRequestBuilder
import com.ml.lansonesscan.data.remote.service.AnalysisService
import com.ml.lansonesscan.data.repository.ScanRepositoryImpl
import com.ml.lansonesscan.domain.usecase.*
import com.ml.lansonesscan.presentation.analysis.AnalysisViewModel
import com.ml.lansonesscan.presentation.dashboard.DashboardViewModel
import com.ml.lansonesscan.presentation.history.HistoryViewModel
import com.ml.lansonesscan.presentation.scandetail.ScanDetailViewModel
import com.ml.lansonesscan.presentation.settings.SettingsViewModel

/**
 * Factory for creating ViewModels with their dependencies
 */
class ViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    private val database by lazy { LansonesDatabase.getDatabase(context) }
    private val apiClient by lazy { GeminiApiClient() }
    private val requestBuilder by lazy { GeminiRequestBuilder() }
    private val analysisService by lazy { AnalysisService(apiClient, requestBuilder) }
    private val imageStorageManager by lazy { ImageStorageManager(context) }
    
    private val repository by lazy { 
        ScanRepositoryImpl(
            scanDao = database.scanDao(),
            analysisService = analysisService,
            imageStorageManager = imageStorageManager,
            context = context
        )
    }

    // Use cases
    private val analyzeImageUseCase by lazy { AnalyzeImageUseCase(repository) }
    private val saveScanResultUseCase by lazy { SaveScanResultUseCase(repository) }
    private val getScanHistoryUseCase by lazy { GetScanHistoryUseCase(repository) }
    private val deleteScanUseCase by lazy { DeleteScanUseCase(repository) }
    private val clearHistoryUseCase by lazy { ClearHistoryUseCase(repository) }
    private val getStorageInfoUseCase by lazy { GetStorageInfoUseCase(repository) }
    private val getScanByIdUseCase by lazy { GetScanByIdUseCase(repository) }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AnalysisViewModel::class.java) -> {
                AnalysisViewModel(
                    analyzeImageUseCase = analyzeImageUseCase,
                    saveScanResultUseCase = saveScanResultUseCase
                ) as T
            }
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(
                    getScanHistoryUseCase = getScanHistoryUseCase
                ) as T
            }
            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> {
                HistoryViewModel(
                    getScanHistoryUseCase = getScanHistoryUseCase,
                    deleteScanUseCase = deleteScanUseCase
                ) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    clearHistoryUseCase = clearHistoryUseCase,
                    getStorageInfoUseCase = getStorageInfoUseCase
                ) as T
            }
            modelClass.isAssignableFrom(ScanDetailViewModel::class.java) -> {
                ScanDetailViewModel(
                    getScanByIdUseCase = getScanByIdUseCase,
                    deleteScanUseCase = deleteScanUseCase
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}