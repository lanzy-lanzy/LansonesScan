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
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for the complete analysis workflow
 * Tests the end-to-end user journey from initial state to results
 */
@RunWith(AndroidJUnit4::class)
class AnalysisWorkflowIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: AnalysisViewModel
    private lateinit var uiStateFlow: MutableStateFlow<AnalysisUiState>

    @Before
    fun setup() {
        mockViewModel = mockk(relaxed = true)
        uiStateFlow = MutableStateFlow(AnalysisUiState())
        every { mockViewModel.uiState } returns uiStateFlow
    }

    @Test
    fun completeAnalysisWorkflow_fromInitialStateToResults() {
        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisScreen(
                    viewModel = mockViewModel,
                    onNavigateToScanDetail = { },
                    onNavigateToDashboard = { }
                )
            }
        }

        // Step 1: Verify initial state
        composeTestRule.onNodeWithText("Disease Analysis").assertIsDisplayed()
        composeTestRule.onNodeWithTag("start_analysis_button").assertIsNotEnabled()

        // Step 2: Select analysis type
        uiStateFlow.value = uiStateFlow.value.copy(
            selectedAnalysisType = AnalysisType.FRUIT
        )
        composeTestRule.waitForIdle()

        // Step 3: Add image
        uiStateFlow.value = uiStateFlow.value.copy(
            selectedImageUri = Uri.parse("content://test/image.jpg")
        )
        composeTestRule.waitForIdle()

        // Verify analysis button is now enabled
        composeTestRule.onNodeWithTag("start_analysis_button").assertIsEnabled()

        // Step 4: Start analysis (simulate analyzing state)
        uiStateFlow.value = uiStateFlow.value.copy(
            isAnalyzing = true,
            analysisProgress = 0.5f,
            analysisStatus = "Analyzing image..."
        )
        composeTestRule.waitForIdle()

        // Verify progress is shown
        composeTestRule.onNodeWithTag("analysis_progress").assertIsDisplayed()
        composeTestRule.onNodeWithText("50%").assertIsDisplayed()

        // Step 5: Complete analysis with results
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

        uiStateFlow.value = uiStateFlow.value.copy(
            isAnalyzing = false,
            analysisResult = sampleResult,
            analysisProgress = 1.0f
        )
        composeTestRule.waitForIdle()

        // Step 6: Verify results are displayed
        composeTestRule.onNodeWithTag("analysis_results").assertIsDisplayed()
        composeTestRule.onNodeWithText("Anthracnose").assertIsDisplayed()
        composeTestRule.onNodeWithText("85%").assertIsDisplayed()
        composeTestRule.onNodeWithText("View Details").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start New Scan").assertIsDisplayed()

        // Verify progress is no longer shown
        composeTestRule.onNodeWithTag("analysis_progress").assertIsNotDisplayed()
    }

    @Test
    fun analysisWorkflow_withError_showsErrorAndRetry() {
        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisScreen(
                    viewModel = mockViewModel,
                    onNavigateToScanDetail = { },
                    onNavigateToDashboard = { }
                )
            }
        }

        // Set up error state
        uiStateFlow.value = AnalysisUiState(
            selectedAnalysisType = AnalysisType.FRUIT,
            selectedImageUri = Uri.parse("content://test/image.jpg"),
            error = "Network connection failed"
        )
        composeTestRule.waitForIdle()

        // Verify error is displayed
        composeTestRule.onNodeWithTag("error_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Analysis Failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Network connection failed").assertIsDisplayed()
        composeTestRule.onNodeWithTag("retry_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dismiss_error_button").assertIsDisplayed()
    }

    @Test
    fun analysisWorkflow_healthyResult_showsCorrectStatus() {
        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisScreen(
                    viewModel = mockViewModel,
                    onNavigateToScanDetail = { },
                    onNavigateToDashboard = { }
                )
            }
        }

        // Set up healthy result
        val healthyResult = ScanResult(
            id = "test-scan-2",
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.LEAVES,
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

        uiStateFlow.value = AnalysisUiState(
            analysisResult = healthyResult
        )
        composeTestRule.waitForIdle()

        // Verify healthy status is displayed
        composeTestRule.onNodeWithTag("analysis_results").assertIsDisplayed()
        composeTestRule.onNodeWithText("Healthy").assertIsDisplayed()
        composeTestRule.onNodeWithText("92%").assertIsDisplayed()
    }
}