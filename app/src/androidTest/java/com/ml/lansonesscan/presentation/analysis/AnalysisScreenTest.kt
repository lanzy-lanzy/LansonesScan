package com.ml.lansonesscan.presentation.analysis

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.ScanMetadata
import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.ui.theme.LansonesScanTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive UI tests for AnalysisScreen
 * Tests the complete analysis workflow including:
 * - Initial state display
 * - Analysis type selection
 * - Image selection
 * - Analysis progress
 * - Results display
 * - Error handling
 * - Navigation
 */
@RunWith(AndroidJUnit4::class)
class AnalysisScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: AnalysisViewModel
    private lateinit var uiStateFlow: MutableStateFlow<AnalysisUiState>
    private var navigateToScanDetailCalled = false
    private var navigateToDashboardCalled = false
    private var scanDetailId: String? = null

    @Before
    fun setup() {
        mockViewModel = mockk(relaxed = true)
        uiStateFlow = MutableStateFlow(AnalysisUiState())
        
        every { mockViewModel.uiState } returns uiStateFlow
        
        navigateToScanDetailCalled = false
        navigateToDashboardCalled = false
        scanDetailId = null
    }

    @Test
    fun analysisScreen_initialState_displaysCorrectly() {
        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisScreen(
                    viewModel = mockViewModel,
                    onNavigateToScanDetail = { scanDetailId = it; navigateToScanDetailCalled = true },
                    onNavigateToDashboard = { navigateToDashboardCalled = true }
                )
            }
        }

        // Verify initial UI elements are displayed
        composeTestRule.onNodeWithTag("analysis_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("analysis_header").assertIsDisplayed()
        composeTestRule.onNodeWithText("Disease Analysis").assertIsDisplayed()
        composeTestRule.onNodeWithText("Capture or upload an image to analyze your lansones for diseases").assertIsDisplayed()
        
        // Verify analysis type selector is displayed
        composeTestRule.onNodeWithTag("analysis_type_selector").assertIsDisplayed()
        
        // Verify image selection section is displayed
        composeTestRule.onNodeWithTag("image_selection_section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("camera_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gallery_button").assertIsDisplayed()
        
        // Verify analysis button is displayed but disabled
        composeTestRule.onNodeWithTag("start_analysis_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("start_analysis_button").assertIsNotEnabled()
    }

    @Test
    fun analysisScreen_selectAnalysisType_updatesViewModel() {
        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisScreen(
                    viewModel = mockViewModel,
                    onNavigateToScanDetail = { scanDetailId = it; navigateToScanDetailCalled = true },
                    onNavigateToDashboard = { navigateToDashboardCalled = true }
                )
            }
        }

        // Click on fruit analysis type
        composeTestRule.onNodeWithText("Fruit Analysis").performClick()
        
        // Verify ViewModel method was called
        verify { mockViewModel.setAnalysisType(AnalysisType.FRUIT) }
    }

    @Test
    fun analysisScreen_withSelectedTypeAndImage_enablesAnalysisButton() {
        // Set up state with selected type and image
        uiStateFlow.value = AnalysisUiState(
            selectedAnalysisType = AnalysisType.FRUIT,
            selectedImageUri = Uri.parse("content://test/image.jpg")
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisScreen(
                    viewModel = mockViewModel,
                    onNavigateToScanDetail = { scanDetailId = it; navigateToScanDetailCalled = true },
                    onNavigateToDashboard = { navigateToDashboardCalled = true }
                )
            }
        }

        // Verify analysis button is enabled
        composeTestRule.onNodeWithTag("start_analysis_button").assertIsEnabled()
        composeTestRule.onNodeWithText("Start Analysis").assertIsDisplayed()
    }

    @Test
    fun analysisScreen_startAnalysis_callsViewModel() {
        // Set up state with selected type and image
        uiStateFlow.value = AnalysisUiState(
            selectedAnalysisType = AnalysisType.FRUIT,
            selectedImageUri = Uri.parse("content://test/image.jpg")
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisScreen(
                    viewModel = mockViewModel,
                    onNavigateToScanDetail = { scanDetailId = it; navigateToScanDetailCalled = true },
                    onNavigateToDashboard = { navigateToDashboardCalled = true }
                )
            }
        }

        // Click start analysis button
        composeTestRule.onNodeWithTag("start_analysis_button").performClick()
        
        // Verify ViewModel method was called
        verify { mockViewModel.startAnalysis() }
    }

    @Test
    fun analysisScreen_duringAnalysis_showsProgressIndicator() {
        // Set up analyzing state
        uiStateFlow.value = AnalysisUiState(
            selectedAnalysisType = AnalysisType.FRUIT,
            selectedImageUri = Uri.parse("content://test/image.jpg"),
            isAnalyzing = true,
            analysisProgress = 0.5f,
            analysisStatus = "Analyzing image..."
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisScreen(
                    viewModel = mockViewModel,
                    onNavigateToScanDetail = { scanDetailId = it; navigateToScanDetailCalled = true },
                    onNavigateToDashboard = { navigateToDashboardCalled = true }
                )
            }
        }

        // Verify progress indicator is displayed
        composeTestRule.onNodeWithTag("analysis_progress").assertIsDisplayed()
        composeTestRule.onNodeWithTag("analysis_progress_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("progress_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Analysis in Progress").assertIsDisplayed()
        composeTestRule.onNodeWithText("50%").assertIsDisplayed()
        
        // Verify analysis type selector and image selection are hidden
        composeTestRule.onNodeWithTag("analysis_type_selector").assertIsNotDisplayed()
        composeTestRule.onNodeWithTag("image_selection_section").assertIsNotDisplayed()
        composeTestRule.onNodeWithTag("start_analysis_button").assertIsNotDisplayed()
    }

    @Test
    fun analysisScreen_withResults_displaysAnalysisResults() {
        val sampleResult = ScanResult(
            id = "test-scan-1",
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseDetected = true,
            diseaseName = "Anthracnose",
            confidenceLevel = 0.85f,
            recommendations = listOf(
                "Remove affected fruits immediately",
                "Apply fungicide spray"
            ),
            timestamp = System.currentTimeMillis(),
            metadata = ScanMetadata(
                imageSize = 2048576,
                imageFormat = "jpeg",
                analysisTime = 1250,
                apiVersion = "1.0"
            )
        )

        // Set up state with analysis results
        uiStateFlow.value = AnalysisUiState(
            selectedAnalysisType = AnalysisType.FRUIT,
            selectedImageUri = Uri.parse("content://test/image.jpg"),
            isAnalyzing = false,
            analysisResult = sampleResult
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisScreen(
                    viewModel = mockViewModel,
                    onNavigateToScanDetail = { scanDetailId = it; navigateToScanDetailCalled = true },
                    onNavigateToDashboard = { navigateToDashboardCalled = true }
                )
            }
        }

        // Verify results are displayed
        composeTestRule.onNodeWithTag("analysis_results").assertIsDisplayed()
        composeTestRule.onNodeWithText("Anthracnose").assertIsDisplayed()
        composeTestRule.onNodeWithText("85%").assertIsDisplayed()
        
        // Verify action buttons are displayed
        composeTestRule.onNodeWithText("View Details").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start New Scan").assertIsDisplayed()
    }

    @Test
    fun analysisScreen_viewDetailsButton_triggersNavigation() {
        val sampleResult = ScanResult(
            id = "test-scan-1",
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseDetected = false,
            diseaseName = null,
            confidenceLevel = 0.92f,
            recommendations = listOf("Plant appears healthy"),
            timestamp = System.currentTimeMillis(),
            metadata = ScanMetadata(
                imageSize = 1536000,
                imageFormat = "png",
                analysisTime = 980,
                apiVersion = "1.0"
            )
        )

        // Set up state with analysis results
        uiStateFlow.value = AnalysisUiState(
            selectedAnalysisType = AnalysisType.LEAVES,
            selectedImageUri = Uri.parse("content://test/image.jpg"),
            analysisResult = sampleResult,
            navigationEvent = AnalysisNavigationEvent.NavigateToScanDetail("test-scan-1")
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisScreen(
                    viewModel = mockViewModel,
                    onNavigateToScanDetail = { scanDetailId = it; navigateToScanDetailCalled = true },
                    onNavigateToDashboard = { navigateToDashboardCalled = true }
                )
            }
        }

        // Verify navigation was triggered
        composeTestRule.waitUntil(timeoutMillis = 1000) {
            navigateToScanDetailCalled
        }
        assert(scanDetailId == "test-scan-1")
        verify { mockViewModel.onNavigationEventHandled() }
    }

    @Test
    fun analysisScreen_startNewScanButton_resetsAnalysis() {
        val sampleResult = ScanResult(
            id = "test-scan-1",
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseDetected = true,
            diseaseName = "Test Disease",
            confidenceLevel = 0.75f,
            recommendations = listOf("Test recommendation"),
            timestamp = System.currentTimeMillis(),
            metadata = ScanMetadata(
                imageSize = 1024000,
                imageFormat = "jpeg",
                analysisTime = 800,
                apiVersion = "1.0"
            )
        )

        // Set up state with analysis results
        uiStateFlow.value = AnalysisUiState(
            analysisResult = sampleResult
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisScreen(
                    viewModel = mockViewModel,
                    onNavigateToScanDetail = { scanDetailId = it; navigateToScanDetailCalled = true },
                    onNavigateToDashboard = { navigateToDashboardCalled = true }
                )
            }
        }

        // Click start new scan button
        composeTestRule.onNodeWithText("Start New Scan").performClick()
        
        // Verify ViewModel method was called
        verify { mockViewModel.resetAnalysis() }
    }

    @Test
    fun analysisScreen_withError_displaysErrorCard() {
        // Set up error state
        uiStateFlow.value = AnalysisUiState(
            selectedAnalysisType = AnalysisType.FRUIT,
            selectedImageUri = Uri.parse("content://test/image.jpg"),
            error = "Network connection failed. Please check your internet connection."
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisScreen(
                    viewModel = mockViewModel,
                    onNavigateToScanDetail = { scanDetailId = it; navigateToScanDetailCalled = true },
                    onNavigateToDashboard = { navigateToDashboardCalled = true }
                )
            }
        }

        // Verify error card is displayed
        composeTestRule.onNodeWithTag("error_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Analysis Failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Network connection failed. Please check your internet connection.").assertIsDisplayed()
        
        // Verify error action buttons
        composeTestRule.onNodeWithTag("retry_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dismiss_error_button").assertIsDisplayed()
    }

    @Test
    fun analysisScreen_retryButton_retriesAnalysis() {
        // Set up error state with retry capability
        uiStateFlow.value = AnalysisUiState(
            selectedAnalysisType = AnalysisType.FRUIT,
            selectedImageUri = Uri.parse("content://test/image.jpg"),
            error = "Analysis failed. Please try again."
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisScreen(
                    viewModel = mockViewModel,
                    onNavigateToScanDetail = { scanDetailId = it; navigateToScanDetailCalled = true },
                    onNavigateToDashboard = { navigateToDashboardCalled = true }
                )
            }
        }

        // Click retry button
        composeTestRule.onNodeWithTag("retry_button").performClick()
        
        // Verify ViewModel retry method was called
        verify { mockViewModel.retryAnalysis() }
    }

    @Test
    fun analysisScreen_dismissErrorButton_clearsError() {
        // Set up error state
        uiStateFlow.value = AnalysisUiState(
            error = "Test error message"
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisScreen(
                    viewModel = mockViewModel,
                    onNavigateToScanDetail = { scanDetailId = it; navigateToScanDetailCalled = true },
                    onNavigateToDashboard = { navigateToDashboardCalled = true }
                )
            }
        }

        // Click dismiss button
        composeTestRule.onNodeWithTag("dismiss_error_button").performClick()
        
        // Verify ViewModel clear error method was called
        verify { mockViewModel.clearError() }
    }

    @Test
    fun analysisScreen_cameraButton_triggersPermissionCheck() {
        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisScreen(
                    viewModel = mockViewModel,
                    onNavigateToScanDetail = { scanDetailId = it; navigateToScanDetailCalled = true },
                    onNavigateToDashboard = { navigateToDashboardCalled = true }
                )
            }
        }

        // Click camera button
        composeTestRule.onNodeWithTag("camera_button").performClick()
        
        // Note: Permission handling would be tested in integration tests
        // as it requires actual permission system interaction
    }

    @Test
    fun analysisScreen_galleryButton_triggersImagePicker() {
        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisScreen(
                    viewModel = mockViewModel,
                    onNavigateToScanDetail = { scanDetailId = it; navigateToScanDetailCalled = true },
                    onNavigateToDashboard = { navigateToDashboardCalled = true }
                )
            }
        }

        // Click gallery button
        composeTestRule.onNodeWithTag("gallery_button").performClick()
        
        // Note: Image picker would be tested in integration tests
        // as it requires actual system interaction
    }

    @Test
    fun analysisScreen_duringAnalysis_disablesInteractions() {
        // Set up analyzing state
        uiStateFlow.value = AnalysisUiState(
            selectedAnalysisType = AnalysisType.FRUIT,
            selectedImageUri = Uri.parse("content://test/image.jpg"),
            isAnalyzing = true,
            analysisProgress = 0.3f,
            analysisStatus = "Processing image..."
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisScreen(
                    viewModel = mockViewModel,
                    onNavigateToScanDetail = { scanDetailId = it; navigateToScanDetailCalled = true },
                    onNavigateToDashboard = { navigateToDashboardCalled = true }
                )
            }
        }

        // Verify interactive elements are hidden or disabled during analysis
        composeTestRule.onNodeWithTag("analysis_type_selector").assertIsNotDisplayed()
        composeTestRule.onNodeWithTag("image_selection_section").assertIsNotDisplayed()
        composeTestRule.onNodeWithTag("start_analysis_button").assertIsNotDisplayed()
        
        // Verify progress is shown
        composeTestRule.onNodeWithTag("analysis_progress").assertIsDisplayed()
        composeTestRule.onNodeWithText("30%").assertIsDisplayed()
    }

    @Test
    fun analysisScreen_navigationToDashboard_triggersCallback() {
        // Set up navigation event
        uiStateFlow.value = AnalysisUiState(
            navigationEvent = AnalysisNavigationEvent.NavigateToDashboard
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisScreen(
                    viewModel = mockViewModel,
                    onNavigateToScanDetail = { scanDetailId = it; navigateToScanDetailCalled = true },
                    onNavigateToDashboard = { navigateToDashboardCalled = true }
                )
            }
        }

        // Verify navigation was triggered
        composeTestRule.waitUntil(timeoutMillis = 1000) {
            navigateToDashboardCalled
        }
        verify { mockViewModel.onNavigationEventHandled() }
    }

    @Test
    fun analysisScreen_accessibility_hasProperContentDescriptions() {
        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisScreen(
                    viewModel = mockViewModel,
                    onNavigateToScanDetail = { scanDetailId = it; navigateToScanDetailCalled = true },
                    onNavigateToDashboard = { navigateToDashboardCalled = true }
                )
            }
        }

        // Verify accessibility content descriptions
        composeTestRule.onNodeWithContentDescription("Take photo with camera").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Select image from gallery").assertIsDisplayed()
    }
}