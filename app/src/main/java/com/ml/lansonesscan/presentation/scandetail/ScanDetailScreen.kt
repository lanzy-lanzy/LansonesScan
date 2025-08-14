package com.ml.lansonesscan.presentation.scandetail

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ml.lansonesscan.ui.theme.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.ScanMetadata
import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.ui.theme.LansonesScanTheme
import java.text.SimpleDateFormat
import java.util.*

/**
 * Detailed scan result screen showing complete analysis information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanDetailScreen(
    scanResult: ScanResult,
    onNavigateBack: () -> Unit,
    onShareResult: () -> Unit = {},
    onDeleteScan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .primaryGradientBackground()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Scan Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                IconButton(onClick = onShareResult) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share"
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete"
                    )
                }
            }
        )

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Analyzed Image
            AnalyzedImageCard(
                imagePath = scanResult.imagePath,
                modifier = Modifier.fillMaxWidth()
            )

            // Analysis Summary
            AnalysisSummaryCard(
                scanResult = scanResult,
                modifier = Modifier.fillMaxWidth()
            )

            // Detailed Results
            DetailedResultsCard(
                scanResult = scanResult,
                modifier = Modifier.fillMaxWidth()
            )

            // Recommendations or Observations
            if (scanResult.recommendations.isNotEmpty()) {
                if (scanResult.analysisType == AnalysisType.NON_LANSONES) {
                    ObservationsDetailCard(
                        observations = scanResult.recommendations,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    RecommendationsDetailCard(
                        recommendations = scanResult.recommendations,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Technical Information
            TechnicalInfoCard(
                scanResult = scanResult,
                modifier = Modifier.fillMaxWidth()
            )

            // Bottom padding
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text("Delete Scan")
            },
            text = {
                Text("Are you sure you want to delete this scan result? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteScan()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Card displaying the analyzed image
 */
@Composable
private fun AnalyzedImageCard(
    imagePath: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Analyzed Image",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (imagePath.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imagePath)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Analyzed lansones image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder when no image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🌿",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Text(
                            text = "Image not available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card displaying analysis summary
 */
@Composable
private fun AnalysisSummaryCard(
    scanResult: ScanResult,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusIcon, statusText) = when {
        scanResult.analysisType == AnalysisType.NON_LANSONES -> Triple(
            MaterialTheme.colorScheme.primary,
            Icons.Default.Info,
            "Non-Lansones Item"
        )
        scanResult.diseaseDetected -> Triple(
            MaterialTheme.colorScheme.error,
            Icons.Default.Warning,
            scanResult.diseaseName ?: "Disease Detected"
        )
        else -> Triple(
            Color(0xFF4CAF50), // Green
            Icons.Default.CheckCircle,
            "Healthy Lansones"
        )
    }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                scanResult.analysisType == AnalysisType.NON_LANSONES -> MaterialTheme.colorScheme.surfaceVariant
                scanResult.diseaseDetected -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Analysis Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = "Status",
                    tint = statusColor,
                    modifier = Modifier.size(32.dp)
                )
                
                Column {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Text(
                        text = scanResult.analysisType.getDisplayName(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Confidence level
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Confidence Level",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${(scanResult.confidenceLevel * 100).toInt()}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
                
                LinearProgressIndicator(
                    progress = { scanResult.confidenceLevel },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

/**
 * Card displaying detailed analysis results
 */
@Composable
private fun DetailedResultsCard(
    scanResult: ScanResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Detailed Results",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            DetailRow(
                label = "Analysis Type",
                value = scanResult.analysisType.getDisplayName()
            )

            DetailRow(
                label = if (scanResult.analysisType == AnalysisType.NON_LANSONES) "Item Status" else "Disease Status",
                value = when {
                    scanResult.analysisType == AnalysisType.NON_LANSONES -> "Non-Lansones Item"
                    scanResult.diseaseDetected -> "Disease Detected"
                    else -> "Healthy Lansones"
                }
            )

            if (scanResult.diseaseDetected && !scanResult.diseaseName.isNullOrBlank()) {
                DetailRow(
                    label = "Disease Name",
                    value = scanResult.diseaseName
                )
            }

            DetailRow(
                label = "Confidence Level",
                value = "${(scanResult.confidenceLevel * 100).toInt()}%"
            )

            DetailRow(
                label = "Analysis Date",
                value = formatTimestamp(scanResult.timestamp)
            )
        }
    }
}

/**
 * Card displaying detailed recommendations
 */
@Composable
private fun RecommendationsDetailCard(
    recommendations: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Recommendations",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Recommendations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            recommendations.forEachIndexed { index, recommendation ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Text(
                        text = recommendation,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (index < recommendations.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Card displaying observations for non-lansones items
 */
@Composable
private fun ObservationsDetailCard(
    observations: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Observations",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Observations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            observations.forEachIndexed { index, observation ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = observation,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (index < observations.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Card displaying technical information
 */
@Composable
private fun TechnicalInfoCard(
    scanResult: ScanResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Technical Information",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DetailRow(
                label = "Scan ID",
                value = scanResult.id
            )

            DetailRow(
                label = "Analysis Time",
                value = "${scanResult.metadata.analysisTime}ms"
            )

            DetailRow(
                label = "Image Format",
                value = scanResult.metadata.imageFormat.uppercase()
            )

            DetailRow(
                label = "Image Size",
                value = formatFileSize(scanResult.metadata.imageSize)
            )

            
        }
    }
}

/**
 * Row displaying detail label and value
 */
@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Format timestamp to readable string
 */
private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

/**
 * Format file size in bytes to human readable format
 */
private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1 -> String.format("%.1f GB", gb)
        mb >= 1 -> String.format("%.1f MB", mb)
        kb >= 1 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanDetailScreenPreview() {
    LansonesScanTheme {
        val sampleResult = ScanResult(
            id = "scan_123",
            imagePath = "",
            analysisType = AnalysisType.FRUIT,
            diseaseDetected = true,
            diseaseName = "Anthracnose",
            confidenceLevel = 0.85f,
            recommendations = listOf(
                "Remove affected fruits immediately to prevent spread",
                "Apply copper-based fungicide spray",
                "Improve air circulation around the tree",
                "Avoid overhead watering to reduce moisture"
            ),
            timestamp = System.currentTimeMillis(),
            metadata = ScanMetadata(
                imageSize = 2048576,
                imageFormat = "jpeg",
                analysisTime = 1250,
                apiVersion = "1.0"
            )
        )
        
        ScanDetailScreen(
            scanResult = sampleResult,
            onNavigateBack = {},
            onShareResult = {},
            onDeleteScan = {}
        )
    }
}