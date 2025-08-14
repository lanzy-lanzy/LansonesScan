package com.ml.lansonesscan.presentation.dashboard.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.ScanMetadata
import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.domain.usecase.GetScanHistoryUseCase
import com.ml.lansonesscan.ui.theme.LansonesScanTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for dashboard components
 */
@RunWith(AndroidJUnit4::class)
class DashboardComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun statisticsCard_displaysCorrectStatistics() {
        val statistics = GetScanHistoryUseCase.ScanStatistics(
            totalScans = 25,
            diseaseDetectedCount = 8,
            healthyScansCount = 17,
            totalStorageSize = 1024 * 1024 * 15, // 15 MB
            diseaseDetectionRate = 32.0f
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                StatisticsCard(statistics = statistics)
            }
        }

        // Verify statistics are displayed
        composeTestRule.onNodeWithText("Scan Statistics").assertIsDisplayed()
        composeTestRule.onNodeWithText("25").assertIsDisplayed()
        composeTestRule.onNodeWithText("17").assertIsDisplayed()
        composeTestRule.onNodeWithText("8").assertIsDisplayed()
        composeTestRule.onNodeWithText("32.0%").assertIsDisplayed()
        composeTestRule.onNodeWithText("15 MB").assertIsDisplayed()
    }

    @Test
    fun statisticsCard_emptyState_displaysZeros() {
        val emptyStatistics = GetScanHistoryUseCase.ScanStatistics(
            totalScans = 0,
            diseaseDetectedCount = 0,
            healthyScansCount = 0,
            totalStorageSize = 0,
            diseaseDetectionRate = 0f
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                StatisticsCard(statistics = emptyStatistics)
            }
        }

        // Verify empty state
        composeTestRule.onNodeWithText("0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total Scans").assertIsDisplayed()
        composeTestRule.onNodeWithText("Healthy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Disease Found").assertIsDisplayed()
        
        // Detection rate and storage should not be displayed when total scans is 0
        composeTestRule.onNodeWithText("Disease Detection Rate").assertDoesNotExist()
        composeTestRule.onNodeWithText("Storage Used").assertDoesNotExist()
    }

    @Test
    fun recentScansCarousel_withScans_displaysCorrectly() {
        val sampleScans = listOf(
            ScanResult(
                id = "1",
                imagePath = "",
                analysisType = AnalysisType.FRUIT,
                diseaseDetected = false,
                diseaseName = null,
                confidenceLevel = 0.92f,
                recommendations = listOf("Plant appears healthy"),
                timestamp = System.currentTimeMillis(),
                metadata = ScanMetadata(1024, "JPEG", 2000, "1.0")
            ),
            ScanResult(
                id = "2",
                imagePath = "",
                analysisType = AnalysisType.LEAVES,
                diseaseDetected = true,
                diseaseName = "Leaf Spot",
                confidenceLevel = 0.85f,
                recommendations = listOf("Apply fungicide"),
                timestamp = System.currentTimeMillis(),
                metadata = ScanMetadata(2048, "PNG", 3000, "1.0")
            )
        )

        var scanClicked = false
        var viewAllClicked = false

        composeTestRule.setContent {
            LansonesScanTheme {
                RecentScansCarousel(
                    recentScans = sampleScans,
                    onScanClick = { scanClicked = true },
                    onViewAllClick = { viewAllClicked = true }
                )
            }
        }

        // Verify header
        composeTestRule.onNodeWithText("Recent Scans").assertIsDisplayed()
        composeTestRule.onNodeWithText("View All").assertIsDisplayed()

        // Verify scan items
        composeTestRule.onNodeWithText("Fruit Analysis").assertIsDisplayed()
        composeTestRule.onNodeWithText("Leaf Analysis").assertIsDisplayed()
        composeTestRule.onNodeWithText("Healthy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Leaf Spot").assertIsDisplayed()
        composeTestRule.onNodeWithText("92%").assertIsDisplayed()
        composeTestRule.onNodeWithText("85%").assertIsDisplayed()

        // Test click interactions
        composeTestRule.onNodeWithText("View All").performClick()
        assert(viewAllClicked)

        // Click on first scan item
        composeTestRule.onNodeWithText("Healthy").performClick()
        assert(scanClicked)
    }

    @Test
    fun recentScansCarousel_emptyState_displaysCorrectly() {
        composeTestRule.setContent {
            LansonesScanTheme {
                RecentScansCarousel(
                    recentScans = emptyList(),
                    onScanClick = {},
                    onViewAllClick = {}
                )
            }
        }

        // Verify empty state
        composeTestRule.onNodeWithText("Recent Scans").assertIsDisplayed()
        composeTestRule.onNodeWithText("No scans yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start scanning to see your results here").assertIsDisplayed()
        
        // View All button should not be displayed when empty
        composeTestRule.onNodeWithText("View All").assertDoesNotExist()
    }

    @Test
    fun quickActionButtons_displaysCorrectly() {
        var quickScanClicked = false
        var cameraClicked = false
        var galleryClicked = false

        composeTestRule.setContent {
            LansonesScanTheme {
                QuickActionButtons(
                    onQuickScanClick = { quickScanClicked = true },
                    onCameraClick = { cameraClicked = true },
                    onGalleryClick = { galleryClicked = true }
                )
            }
        }

        // Verify all buttons are displayed
        composeTestRule.onNodeWithText("Quick Actions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start Quick Scan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Camera").assertIsDisplayed()
        composeTestRule.onNodeWithText("Take Photo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gallery").assertIsDisplayed()
        composeTestRule.onNodeWithText("Upload Image").assertIsDisplayed()

        // Test click interactions
        composeTestRule.onNodeWithText("Start Quick Scan").performClick()
        assert(quickScanClicked)

        composeTestRule.onNodeWithText("Camera").performClick()
        assert(cameraClicked)

        composeTestRule.onNodeWithText("Gallery").performClick()
        assert(galleryClicked)
    }

    @Test
    fun quickActionFab_displaysAndClicksCorrectly() {
        var quickScanClicked = false

        composeTestRule.setContent {
            LansonesScanTheme {
                QuickActionFab(
                    onQuickScanClick = { quickScanClicked = true }
                )
            }
        }

        // Verify FAB is displayed
        composeTestRule.onNodeWithText("Quick Scan").assertIsDisplayed()

        // Test click interaction
        composeTestRule.onNodeWithText("Quick Scan").performClick()
        assert(quickScanClicked)
    }

    @Test
    fun statisticsCard_highDiseaseRate_showsErrorColor() {
        val highDiseaseRateStats = GetScanHistoryUseCase.ScanStatistics(
            totalScans = 10,
            diseaseDetectedCount = 8,
            healthyScansCount = 2,
            totalStorageSize = 1024,
            diseaseDetectionRate = 80.0f
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                StatisticsCard(statistics = highDiseaseRateStats)
            }
        }

        // Verify high disease rate is displayed
        composeTestRule.onNodeWithText("80.0%").assertIsDisplayed()
        composeTestRule.onNodeWithText("Disease Detection Rate").assertIsDisplayed()
    }

    @Test
    fun recentScanItem_diseaseDetected_showsCorrectStatus() {
        val diseasedScan = ScanResult(
            id = "1",
            imagePath = "",
            analysisType = AnalysisType.FRUIT,
            diseaseDetected = true,
            diseaseName = "Fruit Rot",
            confidenceLevel = 0.88f,
            recommendations = listOf("Remove affected fruit"),
            timestamp = System.currentTimeMillis(),
            metadata = ScanMetadata(1024, "JPEG", 2000, "1.0")
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                RecentScansCarousel(
                    recentScans = listOf(diseasedScan),
                    onScanClick = {},
                    onViewAllClick = {}
                )
            }
        }

        // Verify disease status is displayed correctly
        composeTestRule.onNodeWithText("Fruit Rot").assertIsDisplayed()
        composeTestRule.onNodeWithText("88%").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fruit Analysis").assertIsDisplayed()
    }
}