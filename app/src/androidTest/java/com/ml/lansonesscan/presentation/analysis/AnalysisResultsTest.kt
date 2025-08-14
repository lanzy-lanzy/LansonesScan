package com.ml.lansonesscan.presentation.analysis

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.ScanMetadata
import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.presentation.analysis.components.AnalysisResults
import com.ml.lansonesscan.ui.theme.LansonesScanTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnalysisResultsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleMetadata = ScanMetadata(
        imageSize = 2048576,
        imageFormat = "jpeg",
        analysisTime = 1250,
        apiVersion = "1.0"
    )

    @Test
    fun analysisResults_displaysCorrectly() {
        val diseaseResult = ScanResult.createDiseased(
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseName = "Anthracnose",
            confidenceLevel = 0.85f,
            recommendations = listOf(
                "Remove affected fruits immediately",
                "Apply copper-based fungicide spray"
            ),
            metadata = sampleMetadata
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisResults(scanResult = diseaseResult)
            }
        }

        // Verify main container is displayed
        composeTestRule.onNodeWithTag("analysis_results").assertIsDisplayed()
        
        // Verify all cards are displayed
        composeTestRule.onNodeWithTag("disease_status_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("confidence_level_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("recommendations_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("metadata_card").assertIsDisplayed()
    }

    @Test
    fun diseaseStatusCard_showsCorrectInformationForDiseaseDetected() {
        val diseaseResult = ScanResult.createDiseased(
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseName = "Anthracnose",
            confidenceLevel = 0.85f,
            recommendations = listOf("Remove affected fruits"),
            metadata = sampleMetadata
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisResults(scanResult = diseaseResult)
            }
        }

        // Verify disease status text
        composeTestRule.onNodeWithTag("status_text")
            .assertIsDisplayed()
            .assertTextContains("Disease Detected: Anthracnose")
        
        // Verify severity level
        composeTestRule.onNodeWithTag("severity_text")
            .assertIsDisplayed()
            .assertTextContains("High Risk")
        
        // Verify analysis type
        composeTestRule.onNodeWithTag("analysis_type_text")
            .assertIsDisplayed()
            .assertTextContains("Fruit Analysis")
        
        // Verify status icon is displayed
        composeTestRule.onNodeWithTag("status_icon").assertIsDisplayed()
    }

    @Test
    fun diseaseStatusCard_showsCorrectInformationForHealthyResult() {
        val healthyResult = ScanResult.createHealthy(
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.LEAVES,
            confidenceLevel = 0.92f,
            metadata = sampleMetadata
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisResults(scanResult = healthyResult)
            }
        }

        // Verify healthy status text
        composeTestRule.onNodeWithTag("status_text")
            .assertIsDisplayed()
            .assertTextContains("Healthy")
        
        // Verify severity level for healthy
        composeTestRule.onNodeWithTag("severity_text")
            .assertIsDisplayed()
            .assertTextContains("Healthy")
        
        // Verify analysis type
        composeTestRule.onNodeWithTag("analysis_type_text")
            .assertIsDisplayed()
            .assertTextContains("Leaf Analysis")
    }

    @Test
    fun confidenceLevelCard_displaysCorrectPercentageAndProgressBar() {
        val result = ScanResult.createDiseased(
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseName = "Test Disease",
            confidenceLevel = 0.75f,
            recommendations = listOf("Test recommendation"),
            metadata = sampleMetadata
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisResults(scanResult = result)
            }
        }

        // Verify confidence percentage
        composeTestRule.onNodeWithTag("confidence_percentage")
            .assertIsDisplayed()
            .assertTextContains("75%")
        
        // Verify progress bar is displayed
        composeTestRule.onNodeWithTag("confidence_progress_bar")
            .assertIsDisplayed()
        
        // Verify confidence description
        composeTestRule.onNodeWithTag("confidence_description")
            .assertIsDisplayed()
            .assertTextContains("Good confidence")
    }

    @Test
    fun confidenceLevelCard_showsCorrectDescriptionForDifferentConfidenceLevels() {
        // Test high confidence
        val highConfidenceResult = ScanResult.createHealthy(
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            confidenceLevel = 0.95f,
            metadata = sampleMetadata
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisResults(scanResult = highConfidenceResult)
            }
        }

        composeTestRule.onNodeWithTag("confidence_description")
            .assertTextContains("Very high confidence")

        // Test low confidence
        val lowConfidenceResult = ScanResult.createHealthy(
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            confidenceLevel = 0.45f,
            metadata = sampleMetadata
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisResults(scanResult = lowConfidenceResult)
            }
        }

        composeTestRule.onNodeWithTag("confidence_description")
            .assertTextContains("Low confidence")
    }

    @Test
    fun recommendationsCard_displaysAllRecommendations() {
        val recommendations = listOf(
            "Remove affected fruits immediately",
            "Apply copper-based fungicide spray",
            "Improve air circulation around the tree",
            "Avoid overhead watering"
        )
        
        val result = ScanResult.createDiseased(
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseName = "Anthracnose",
            confidenceLevel = 0.85f,
            recommendations = recommendations,
            metadata = sampleMetadata
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisResults(scanResult = result)
            }
        }

        // Verify recommendations card is displayed
        composeTestRule.onNodeWithTag("recommendations_card").assertIsDisplayed()
        
        // Verify each recommendation item is displayed
        recommendations.forEachIndexed { index, recommendation ->
            composeTestRule.onNodeWithTag("recommendation_item_$index")
                .assertIsDisplayed()
                .assertTextContains(recommendation)
        }
    }

    @Test
    fun recommendationsCard_showsEmptyStateWhenNoRecommendations() {
        val result = ScanResult.create(
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseDetected = false,
            diseaseName = null,
            confidenceLevel = 0.85f,
            recommendations = emptyList(),
            metadata = sampleMetadata
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisResults(scanResult = result)
            }
        }

        // Verify empty state message is displayed
        composeTestRule.onNodeWithTag("no_recommendations_text")
            .assertIsDisplayed()
            .assertTextContains("No specific recommendations available")
    }

    @Test
    fun metadataCard_displaysCorrectAnalysisDetails() {
        val result = ScanResult.createDiseased(
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseName = "Test Disease",
            confidenceLevel = 0.85f,
            recommendations = listOf("Test recommendation"),
            metadata = sampleMetadata
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisResults(scanResult = result)
            }
        }

        // Verify metadata card is displayed
        composeTestRule.onNodeWithTag("metadata_card").assertIsDisplayed()
        
        // Verify metadata content (checking if the text exists somewhere in the card)
        composeTestRule.onNodeWithTag("metadata_card")
            .assertTextContains("Analysis Time")
            .assertTextContains("1250ms")
            .assertTextContains("Image Format")
            .assertTextContains("JPEG")
            .assertTextContains("Image Size")
            .assertTextContains("2.0 MB")
    }

    @Test
    fun analysisResults_scrollsCorrectly() {
        val result = ScanResult.createDiseased(
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseName = "Test Disease",
            confidenceLevel = 0.85f,
            recommendations = listOf(
                "First recommendation",
                "Second recommendation",
                "Third recommendation",
                "Fourth recommendation",
                "Fifth recommendation"
            ),
            metadata = sampleMetadata
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisResults(scanResult = result)
            }
        }

        // Verify the list is scrollable
        composeTestRule.onNodeWithTag("analysis_results")
            .assertIsDisplayed()
            .performScrollToIndex(3) // Scroll to metadata card
        
        // Verify metadata card is still visible after scrolling
        composeTestRule.onNodeWithTag("metadata_card").assertIsDisplayed()
    }

    @Test
    fun analysisResults_handlesLongRecommendationText() {
        val longRecommendation = "This is a very long recommendation that should wrap properly " +
                "and display correctly in the UI without causing any layout issues or text overflow " +
                "problems. It should maintain proper spacing and readability."
        
        val result = ScanResult.createDiseased(
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseName = "Test Disease",
            confidenceLevel = 0.85f,
            recommendations = listOf(longRecommendation),
            metadata = sampleMetadata
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisResults(scanResult = result)
            }
        }

        // Verify long recommendation is displayed correctly
        composeTestRule.onNodeWithTag("recommendation_item_0")
            .assertIsDisplayed()
            .assertTextContains(longRecommendation)
    }

    @Test
    fun analysisResults_handlesEdgeCaseConfidenceLevels() {
        // Test minimum confidence (0.0)
        val minConfidenceResult = ScanResult.createHealthy(
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            confidenceLevel = 0.0f,
            metadata = sampleMetadata
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisResults(scanResult = minConfidenceResult)
            }
        }

        composeTestRule.onNodeWithTag("confidence_percentage")
            .assertTextContains("0%")

        // Test maximum confidence (1.0)
        val maxConfidenceResult = ScanResult.createHealthy(
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            confidenceLevel = 1.0f,
            metadata = sampleMetadata
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisResults(scanResult = maxConfidenceResult)
            }
        }

        composeTestRule.onNodeWithTag("confidence_percentage")
            .assertTextContains("100%")
    }
}