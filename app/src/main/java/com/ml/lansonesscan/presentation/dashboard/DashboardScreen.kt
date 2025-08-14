package com.ml.lansonesscan.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.ScanMetadata
import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.domain.usecase.GetScanHistoryUseCase
import com.ml.lansonesscan.presentation.dashboard.components.QuickActionButtons
import com.ml.lansonesscan.presentation.dashboard.components.QuickActionFab
import com.ml.lansonesscan.presentation.dashboard.components.RecentScansCarousel
import com.ml.lansonesscan.presentation.dashboard.components.StatisticsCard
import com.ml.lansonesscan.ui.theme.LansonesScanTheme

/**
 * Main Dashboard screen composable
 * Displays scan statistics, recent scans, and quick actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAnalysis: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToScanDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Handle navigation events
    LaunchedEffect(uiState.navigationEvent) {
        when (val event = uiState.navigationEvent) {
            is DashboardNavigationEvent.NavigateToAnalysis -> {
                onNavigateToAnalysis()
                viewModel.onNavigationEventHandled()
            }
            is DashboardNavigationEvent.NavigateToHistory -> {
                onNavigateToHistory()
                viewModel.onNavigationEventHandled()
            }
            is DashboardNavigationEvent.NavigateToScanDetail -> {
                onNavigateToScanDetail(event.scanId)
                viewModel.onNavigationEventHandled()
            }
            null -> { /* No navigation event */ }
        }
    }

    // Show error snackbar if needed
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // In a real app, you might want to show a Snackbar here
            // For now, we'll just clear the error after showing it
            viewModel.clearError()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.recentScans.isEmpty() -> {
                LoadingState(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            uiState.isEmpty -> {
                EmptyDashboardState(
                    onStartScanClick = viewModel::onQuickScanClicked,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            uiState.hasData -> {
                DashboardContent(
                    uiState = uiState,
                    onRefresh = viewModel::refresh,
                    onQuickScanClick = viewModel::onQuickScanClicked,
                    onCameraClick = viewModel::onQuickScanClicked, // Navigate to analysis screen
                    onGalleryClick = viewModel::onQuickScanClicked, // Navigate to analysis screen
                    onScanClick = viewModel::onScanItemClicked,
                    onViewAllHistoryClick = viewModel::onViewAllHistoryClicked,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Floating Action Button for quick access
        if (uiState.hasData) {
            QuickActionFab(
                onQuickScanClick = viewModel::onQuickScanClicked,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }
}

/**
 * Main dashboard content when data is available
 */
@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onRefresh: () -> Unit,
    onQuickScanClick: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onScanClick: (ScanResult) -> Unit,
    onViewAllHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header with refresh button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Monitor your lansones health",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(
                onClick = onRefresh,
                enabled = !uiState.isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }
        }

        // Statistics Card
        uiState.statistics?.let { statistics ->
            StatisticsCard(
                statistics = statistics,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Recent Scans Carousel
        RecentScansCarousel(
            recentScans = uiState.recentScans,
            onScanClick = onScanClick,
            onViewAllClick = onViewAllHistoryClick,
            modifier = Modifier.fillMaxWidth()
        )

        // Quick Action Buttons
        QuickActionButtons(
            onQuickScanClick = onQuickScanClick,
            onCameraClick = onCameraClick,
            onGalleryClick = onGalleryClick,
            modifier = Modifier.fillMaxWidth()
        )

        // Add bottom padding to account for FAB
        Spacer(modifier = Modifier.height(80.dp))
    }
}

/**
 * Loading state component
 */
@Composable
private fun LoadingState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Loading dashboard...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Empty dashboard state when no scans are available
 */
@Composable
private fun EmptyDashboardState(
    onStartScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Welcome message
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Welcome to Lansones Scanner",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Start scanning your lansones fruit and leaves to detect diseases and get recommendations",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Illustration placeholder (you can add an actual illustration here)
        Card(
            modifier = Modifier.size(200.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🌿",
                    style = MaterialTheme.typography.displayLarge
                )
            }
        }

        // Call to action
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onStartScanClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Start Your First Scan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Text(
                text = "Take a photo or upload an image to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    LansonesScanTheme {
        // Mock data for preview
        val sampleScans = listOf(
            ScanResult(
                id = "1",
                imagePath = "",
                analysisType = AnalysisType.FRUIT,
                diseaseDetected = false,
                diseaseName = null,
                confidenceLevel = 0.92f,
                recommendations = listOf("Plant appears healthy"),
                timestamp = System.currentTimeMillis() - 86400000,
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
                timestamp = System.currentTimeMillis() - 172800000,
                metadata = ScanMetadata(2048, "PNG", 3000, "1.0")
            )
        )

        val mockUiState = DashboardUiState(
            isLoading = false,
            recentScans = sampleScans,
            statistics = GetScanHistoryUseCase.ScanStatistics(
                totalScans = 25,
                diseaseDetectedCount = 8,
                healthyScansCount = 17,
                totalStorageSize = 1024 * 1024 * 15,
                diseaseDetectionRate = 32.0f
            ),
            error = null
        )

        DashboardContent(
            uiState = mockUiState,
            onRefresh = {},
            onQuickScanClick = {},
            onCameraClick = {},
            onGalleryClick = {},
            onScanClick = {},
            onViewAllHistoryClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyDashboardStatePreview() {
    LansonesScanTheme {
        EmptyDashboardState(
            onStartScanClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingStatePreview() {
    LansonesScanTheme {
        LoadingState()
    }
}