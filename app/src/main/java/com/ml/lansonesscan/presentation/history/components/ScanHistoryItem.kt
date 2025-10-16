package com.ml.lansonesscan.presentation.history.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.LansonesVariety
import com.ml.lansonesscan.domain.model.ScanMetadata
import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.ui.theme.LansonesScanTheme
import java.text.SimpleDateFormat
import java.util.*

/**
 * Individual scan history item component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanHistoryItem(
    scanResult: ScanResult,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image thumbnail
            ScanThumbnail(
                imagePath = scanResult.imagePath,
                modifier = Modifier.size(80.dp)
            )

            // Scan information
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Analysis type and status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnalysisTypeChip(
                        analysisType = scanResult.analysisType
                    )
                    
                    DiseaseStatusIndicator(
                        scanResult = scanResult
                    )
                }

                // Disease name, variety, or status
                Text(
                    text = when {
                        scanResult.diseaseDetected -> scanResult.diseaseName ?: "Disease Detected"
                        scanResult.analysisType == AnalysisType.NON_LANSONES -> "Unknown"
                        scanResult.analysisType == AnalysisType.FRUIT && scanResult.variety != null && scanResult.variety != LansonesVariety.UNKNOWN -> {
                            scanResult.variety.getDisplayName()
                        }
                        else -> "Healthy"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Timestamp and confidence
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(scanResult.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "${(scanResult.confidenceLevel * 100).toInt()}% confidence",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Delete button
            IconButton(
                onClick = onDelete
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete scan",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Scan thumbnail image component
 */
@Composable
private fun ScanThumbnail(
    imagePath: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        if (imagePath.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imagePath)
                    .crossfade(true)
                    .build(),
                contentDescription = "Scan thumbnail",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder when no image
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🌿",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

/**
 * Analysis type chip component
 */
@Composable
private fun AnalysisTypeChip(
    analysisType: AnalysisType,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = { },
        label = {
            Text(
                text = when (analysisType) {
                    AnalysisType.FRUIT -> "Fruit"
                    AnalysisType.LEAVES -> "Leaves"
                    AnalysisType.NON_LANSONES -> "General"
                },
                style = MaterialTheme.typography.labelSmall
            )
        },
        leadingIcon = {
            Icon(
                imageVector = when (analysisType) {
                    AnalysisType.FRUIT -> Icons.Default.Eco
                    AnalysisType.LEAVES -> Icons.Default.Eco
                    AnalysisType.NON_LANSONES -> Icons.Default.Info
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        modifier = modifier,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}

/**
 * Disease status indicator component
 */
@Composable
private fun DiseaseStatusIndicator(
    scanResult: ScanResult,
    modifier: Modifier = Modifier
) {
    val (icon, color, text) = when {
        scanResult.diseaseDetected -> Triple(
            Icons.Default.Warning,
            MaterialTheme.colorScheme.error,
            "Disease"
        )
        scanResult.analysisType == AnalysisType.NON_LANSONES -> Triple(
            Icons.Default.Info,
            MaterialTheme.colorScheme.outline,
            "Unknown"
        )
        scanResult.analysisType == AnalysisType.FRUIT && scanResult.variety != null && scanResult.variety != LansonesVariety.UNKNOWN -> Triple(
            Icons.Default.Info,
            MaterialTheme.colorScheme.primary,
            "Variety"
        )
        else -> Triple(
            Icons.Default.Eco,
            Color(0xFF4CAF50), // Green color for healthy
            "Healthy"
        )
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Format timestamp to readable string
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> {
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanHistoryItemPreview() {
    LansonesScanTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            // Healthy fruit scan
            ScanHistoryItem(
                scanResult = ScanResult(
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
                onClick = {},
                onDelete = {}
            )

            // Diseased leaves scan
            ScanHistoryItem(
                scanResult = ScanResult(
                    id = "2",
                    imagePath = "",
                    analysisType = AnalysisType.LEAVES,
                    diseaseDetected = true,
                    diseaseName = "Leaf Spot Disease",
                    confidenceLevel = 0.85f,
                    recommendations = listOf("Apply fungicide", "Remove affected leaves"),
                    timestamp = System.currentTimeMillis() - 172800000,
                    metadata = ScanMetadata(2048, "PNG", 3000, "1.0")
                ),
                onClick = {},
                onDelete = {}
            )

            // Non-lansones item scan
            ScanHistoryItem(
                scanResult = ScanResult(
                    id = "3",
                    imagePath = "",
                    analysisType = AnalysisType.NON_LANSONES,
                    diseaseDetected = false,
                    diseaseName = null,
                    confidenceLevel = 1.0f,
                    recommendations = listOf("Round red fruit observed", "Smooth surface texture"),
                    timestamp = System.currentTimeMillis() - 259200000,
                    metadata = ScanMetadata(1536, "JPEG", 1500, "1.0")
                ),
                onClick = {},
                onDelete = {}
            )
            
            // Fruit with variety detection
            ScanHistoryItem(
                scanResult = ScanResult(
                    id = "4",
                    imagePath = "",
                    analysisType = AnalysisType.FRUIT,
                    diseaseDetected = false,
                    diseaseName = null,
                    confidenceLevel = 0.95f,
                    recommendations = listOf("Plant appears healthy"),
                    timestamp = System.currentTimeMillis() - 345600000,
                    metadata = ScanMetadata(2048, "JPEG", 2500, "1.0"),
                    variety = LansonesVariety.LONGKONG,
                    varietyConfidence = 0.95f
                ),
                onClick = {},
                onDelete = {}
            )
        }
    }
}