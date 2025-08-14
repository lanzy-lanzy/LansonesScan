package com.ml.lansonesscan.presentation.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.ScanMetadata
import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.presentation.history.components.ScanHistoryItem
import com.ml.lansonesscan.ui.theme.LansonesScanTheme

/**
 * History screen showing all previous scans
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToScanDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle navigation events
    LaunchedEffect(uiState.navigationEvent) {
        when (val event = uiState.navigationEvent) {
            is HistoryNavigationEvent.NavigateToScanDetail -> {
                onNavigateToScanDetail(event.scanId)
                viewModel.onNavigationEventHandled()
            }
            is HistoryNavigationEvent.ShareScan -> {
                // TODO: Handle share scan functionality
                viewModel.onNavigationEventHandled()
            }
            null -> { /* No navigation event */ }
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Scan History",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.scanResults.size} scans total",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(
                        onClick = { /* TODO: Implement search */ }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                }
            }
        }

        when {
            uiState.isLoading -> {
                LoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            }
            uiState.isEmpty -> {
                EmptyHistoryState(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.scanResults,
                        key = { it.id }
                    ) { scan ->
                        ScanHistoryItem(
                            scanResult = scan,
                            onClick = { viewModel.onScanItemClicked(scan) },
                            onDelete = { viewModel.showDeleteConfirmation(scan) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (uiState.showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelDelete() },
                title = {
                    Text("Delete Scan")
                },
                text = {
                    Text("Are you sure you want to delete this scan result? This action cannot be undone.")
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmDelete() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.cancelDelete() }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

/**
 * Loading state component
 */
@Composable
private fun LoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Loading scan history...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Empty history state component
 */
@Composable
private fun EmptyHistoryState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Illustration placeholder
            Card(
                modifier = Modifier.size(120.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📋",
                        style = MaterialTheme.typography.displayMedium
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "No Scans Yet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Start analyzing your lansones to see your scan history here",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    LansonesScanTheme {
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

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sampleScans) { scan ->
                ScanHistoryItem(
                    scanResult = scan,
                    onClick = {},
                    onDelete = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyHistoryStatePreview() {
    LansonesScanTheme {
        EmptyHistoryState()
    }
}