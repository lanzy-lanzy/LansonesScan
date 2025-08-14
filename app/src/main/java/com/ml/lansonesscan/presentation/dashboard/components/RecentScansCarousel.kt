package com.ml.lansonesscan.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.ml.lansonesscan.domain.model.ScanMetadata
import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.ui.theme.LansonesScanTheme
import java.text.SimpleDateFormat
import java.util.*

/**
 * Carousel component displaying recent scan results with thumbnails
 */
@Composable
fun RecentScansCarousel(
    recentScans: List<ScanResult>,
    onScanClick: (ScanResult) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header with minimal styling
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Scans",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            if (recentScans.isNotEmpty()) {
                TextButton(onClick = onViewAllClick) {
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        // Scans carousel or empty state
        if (recentScans.isEmpty()) {
            EmptyScansState()
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(recentScans) { scanResult ->
                    RecentScanItem(
                        scanResult = scanResult,
                        onClick = { onScanClick(scanResult) }
                    )
                }
            }
        }
    }
}

/**
 * Individual recent scan item component
 */
@Composable
private fun RecentScanItem(
    scanResult: ScanResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(150.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Image thumbnail with minimal styling
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(scanResult.imagePath)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Scan thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    fallback = null,
                    error = null
                )

                // Fallback icon when image fails to load
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Image placeholder",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                // Minimal status indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(
                            color = if (scanResult.diseaseDetected) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (scanResult.diseaseDetected) {
                            Icons.Default.Error
                        } else {
                            Icons.Default.CheckCircle
                        },
                        contentDescription = if (scanResult.diseaseDetected) "Disease detected" else "Healthy",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
            
            // Minimal scan details
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Analysis type
                Text(
                    text = scanResult.analysisType.getDisplayName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                // Status
                Text(
                    text = when {
                        scanResult.analysisType == AnalysisType.NON_LANSONES -> "Non-Lansones"
                        scanResult.diseaseDetected -> scanResult.diseaseName ?: "Disease Detected"
                        else -> "Healthy"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Date
                Text(
                    text = formatDate(scanResult.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Empty state component when no scans are available
 */
@Composable
private fun EmptyScansState(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "No scans",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = "No scans yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Start scanning to see your results here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Formats timestamp to readable date string
 */
private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

@Preview(showBackground = true)
@Composable
private fun RecentScansCarouselPreview() {
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
                timestamp = System.currentTimeMillis() - 86400000, // 1 day ago
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
                timestamp = System.currentTimeMillis() - 172800000, // 2 days ago
                metadata = ScanMetadata(2048, "PNG", 3000, "1.0")
            )
        )
        
        RecentScansCarousel(
            recentScans = sampleScans,
            onScanClick = {},
            onViewAllClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecentScansCarouselEmptyPreview() {
    LansonesScanTheme {
        RecentScansCarousel(
            recentScans = emptyList(),
            onScanClick = {},
            onViewAllClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}