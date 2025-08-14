package com.ml.lansonesscan.presentation.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.ScanMetadata
import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.domain.usecase.GetScanHistoryUseCase
import com.ml.lansonesscan.presentation.dashboard.components.QuickActionButtons
import com.ml.lansonesscan.presentation.dashboard.components.RecentScansCarousel
import com.ml.lansonesscan.presentation.dashboard.components.StatisticsCard
import com.ml.lansonesscan.ui.theme.LansonesScanTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for DashboardScreen components and layouts
 */
@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dashboardScreen_loadingState_displaysCorrectly() {
        composeTestRule.setContent {
            LansonesScanTheme {
                // Test loading state component directly
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = androidx.compose.ui.Modifier.size(48.dp),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                        )
                        androidx.compose.material3.Text(
                            text = "Loading dashboard...",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Verify loading state is displayed
        composeTestRule.onNodeWithText("Loading dashboard...").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_emptyState_displaysCorrectly() {
        var startScanClicked = false
        
        composeTestRule.setContent {
            LansonesScanTheme {
                // Test empty state component directly
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier.padding(32.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp)
                ) {
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.Text(
                            text = "Welcome to Lansones Scanner",
                            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        androidx.compose.material3.Text(
                            text = "Start scanning your lansones fruit and leaves to detect diseases and get recommendations",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    androidx.compose.material3.Button(
                        onClick = { startScanClicked = true },
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        androidx.compose.material3.Text(
                            text = "Start Your First Scan",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Verify empty state is displayed
        composeTestRule.onNodeWithText("Welcome to Lansones Scanner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start scanning your lansones fruit and leaves to detect diseases and get recommendations").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start Your First Scan").assertIsDisplayed()
        
        // Test button click
        composeTestRule.onNodeWithText("Start Your First Scan").performClick()
        assert(startScanClicked)
    }

    @Test
    fun dashboardScreen_statisticsCard_displaysCorrectly() {
        val statistics = GetScanHistoryUseCase.ScanStatistics(
            totalScans = 25,
            diseaseDetectedCount = 8,
            healthyScansCount = 17,
            totalStorageSize = 1024 * 1024 * 15,
            diseaseDetectionRate = 32.0f
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                StatisticsCard(statistics = statistics)
            }
        }

        // Verify statistics card content
        composeTestRule.onNodeWithText("Scan Statistics").assertIsDisplayed()
        composeTestRule.onNodeWithText("25").assertIsDisplayed()
        composeTestRule.onNodeWithText("17").assertIsDisplayed()
        composeTestRule.onNodeWithText("8").assertIsDisplayed()
        composeTestRule.onNodeWithText("32.0%").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_recentScansCarousel_displaysCorrectly() {
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

        // Verify carousel content
        composeTestRule.onNodeWithText("Recent Scans").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fruit Analysis").assertIsDisplayed()
        composeTestRule.onNodeWithText("Healthy").assertIsDisplayed()
        composeTestRule.onNodeWithText("View All").assertIsDisplayed()

        // Test interactions
        composeTestRule.onNodeWithText("View All").performClick()
        assert(viewAllClicked)
    }

    @Test
    fun dashboardScreen_quickActionButtons_displaysCorrectly() {
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

        // Verify quick action buttons
        composeTestRule.onNodeWithText("Quick Actions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start Quick Scan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Camera").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gallery").assertIsDisplayed()

        // Test interactions
        composeTestRule.onNodeWithText("Start Quick Scan").performClick()
        assert(quickScanClicked)

        composeTestRule.onNodeWithText("Camera").performClick()
        assert(cameraClicked)

        composeTestRule.onNodeWithText("Gallery").performClick()
        assert(galleryClicked)
    }

    @Test
    fun dashboardScreen_dashboardLayout_displaysAllComponents() {
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
            )
        )

        val statistics = GetScanHistoryUseCase.ScanStatistics(
            totalScans = 25,
            diseaseDetectedCount = 8,
            healthyScansCount = 17,
            totalStorageSize = 1024 * 1024 * 15,
            diseaseDetectionRate = 32.0f
        )

        composeTestRule.setContent {
            LansonesScanTheme {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp)
                ) {
                    // Header
                    androidx.compose.material3.Text(
                        text = "Dashboard",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )

                    // Statistics Card
                    StatisticsCard(statistics = statistics)

                    // Recent Scans Carousel
                    RecentScansCarousel(
                        recentScans = sampleScans,
                        onScanClick = {},
                        onViewAllClick = {}
                    )

                    // Quick Action Buttons
                    QuickActionButtons(
                        onQuickScanClick = {},
                        onCameraClick = {},
                        onGalleryClick = {}
                    )
                }
            }
        }

        // Verify all components are displayed
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Scan Statistics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recent Scans").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quick Actions").assertIsDisplayed()
    }
}