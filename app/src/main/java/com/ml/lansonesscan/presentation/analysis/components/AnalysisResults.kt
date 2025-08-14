package com.ml.lansonesscan.presentation.analysis.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.ScanMetadata
import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.ui.theme.LansonesScanTheme

/**
 * Composable that displays the results of a lansones disease analysis
 * 
 * @param scanResult The scan result to display
 * @param onViewDetails Callback when user wants to view detailed results
 * @param onStartNewScan Callback when user wants to start a new scan
 * @param modifier Modifier for styling
 */
@Composable
fun AnalysisResults(
    scanResult: ScanResult,
    onViewDetails: () -> Unit,
    onStartNewScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("analysis_results"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DiseaseStatusCard(scanResult = scanResult)
        ConfidenceLevelCard(scanResult = scanResult)

        // Only show recommendations for lansones analysis
        if (scanResult.analysisType != AnalysisType.NON_LANSONES) {
            RecommendationsCard(recommendations = scanResult.recommendations)
        } else {
            // Show neutral observations for non-lansones items
            NeutralObservationsCard(scanResult = scanResult)
        }

        ActionButtonsCard(
            onViewDetails = onViewDetails,
            onStartNewScan = onStartNewScan
        )
    }
}

/**
 * Card displaying the disease detection status with appropriate visual indicators
 */
@Composable
private fun DiseaseStatusCard(
    scanResult: ScanResult,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusIcon, statusText) = when {
        scanResult.analysisType == AnalysisType.NON_LANSONES -> Triple(
            MaterialTheme.colorScheme.primary,
            Icons.Default.Info,
            "Non-Lansones Item Detected"
        )
        scanResult.diseaseDetected -> Triple(
            MaterialTheme.colorScheme.error,
            Icons.Default.Warning,
            scanResult.diseaseName ?: "Disease Detected"
        )
        else -> Triple(
            Color(0xFF4CAF50), // Green
            Icons.Default.CheckCircle,
            "Healthy"
        )
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("disease_status_card"),
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = "Disease status indicator",
                tint = statusColor,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("status_icon")
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = statusText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("status_text")
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = scanResult.analysisType.getDisplayName(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("analysis_type_text")
            )
        }
    }
}

/**
 * Card displaying the confidence level with animated progress bar
 */
@Composable
private fun ConfidenceLevelCard(
    scanResult: ScanResult,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = scanResult.confidenceLevel,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "confidence_animation"
    )
    
    val confidenceColor = when {
        scanResult.confidenceLevel >= 0.8f -> Color(0xFF4CAF50) // Green
        scanResult.confidenceLevel >= 0.6f -> Color(0xFFFF9800) // Orange
        else -> MaterialTheme.colorScheme.error // Red
    }
    
    val confidencePercentage = (scanResult.confidenceLevel * 100).toInt()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("confidence_level_card"),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (scanResult.analysisType == AnalysisType.NON_LANSONES) {
                        "Detection Confidence"
                    } else {
                        "Confidence Level"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "$confidencePercentage%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = confidenceColor,
                    modifier = Modifier.testTag("confidence_percentage")
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .testTag("confidence_progress_bar"),
                color = confidenceColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when {
                    scanResult.confidenceLevel >= 0.9f -> "Very high confidence in the analysis result"
                    scanResult.confidenceLevel >= 0.8f -> "High confidence in the analysis result"
                    scanResult.confidenceLevel >= 0.7f -> "Good confidence in the analysis result"
                    scanResult.confidenceLevel >= 0.6f -> "Moderate confidence in the analysis result"
                    else -> "Low confidence - consider retaking the image"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("confidence_description")
            )
        }
    }
}

/**
 * Card displaying the list of recommendations with proper formatting
 */
@Composable
private fun RecommendationsCard(
    recommendations: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recommendations_card"),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Recommendations",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Recommendations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (recommendations.isEmpty()) {
                Text(
                    text = "No specific recommendations available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("no_recommendations_text")
                )
            } else {
                recommendations.forEachIndexed { index, recommendation ->
                    RecommendationItem(
                        recommendation = recommendation,
                        index = index + 1,
                        modifier = Modifier.testTag("recommendation_item_$index")
                    )
                    
                    if (index < recommendations.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/**
 * Individual recommendation item with bullet point styling
 */
@Composable
private fun RecommendationItem(
    recommendation: String,
    index: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$index.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        
        Text(
            text = recommendation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Card with action buttons for viewing details and starting new scan
 */
@Composable
private fun ActionButtonsCard(
    onViewDetails: () -> Unit,
    onStartNewScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("action_buttons_card"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Details")
            }
            
            OutlinedButton(
                onClick = onStartNewScan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start New Scan")
            }
        }
    }
}

/**
 * Card displaying neutral observations for non-lansones items
 */
@Composable
private fun NeutralObservationsCard(
    scanResult: ScanResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("neutral_observations_card"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Observations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (scanResult.recommendations.isEmpty()) {
                Text(
                    text = "No specific observations available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("no_observations_text")
                )
            } else {
                scanResult.recommendations.forEachIndexed { index, observation ->
                    ObservationItem(
                        observation = observation,
                        index = index + 1,
                        modifier = Modifier.testTag("observation_item_$index")
                    )

                    if (index < scanResult.recommendations.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/**
 * Individual observation item for neutral analysis
 */
@Composable
private fun ObservationItem(
    observation: String,
    index: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$index.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = observation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AnalysisResultsPreview() {
    LansonesScanTheme {
        val sampleResult = ScanResult(
            id = "1",
            imagePath = "/path/to/image.jpg",
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
        
        AnalysisResults(
            scanResult = sampleResult,
            onViewDetails = {},
            onStartNewScan = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HealthyAnalysisResultsPreview() {
    LansonesScanTheme {
        val sampleResult = ScanResult(
            id = "2",
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
        
        AnalysisResults(
            scanResult = sampleResult,
            onViewDetails = {},
            onStartNewScan = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NeutralAnalysisResultsPreview() {
    LansonesScanTheme {
        val sampleResult = ScanResult(
            id = "3",
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.NON_LANSONES,
            diseaseDetected = false,
            diseaseName = null,
            confidenceLevel = 1.0f,
            recommendations = listOf(
                "Item appears to be an apple",
                "Round shape with red coloration",
                "Smooth surface texture observed",
                "Approximately 8cm in diameter"
            ),
            timestamp = System.currentTimeMillis(),
            metadata = ScanMetadata(
                imageSize = 1536000,
                imageFormat = "png",
                analysisTime = 980,
                apiVersion = "1.0"
            )
        )

        AnalysisResults(
            scanResult = sampleResult,
            onViewDetails = {},
            onStartNewScan = {}
        )
    }
}