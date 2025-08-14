package com.ml.lansonesscan.presentation.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ml.lansonesscan.domain.usecase.GetScanHistoryUseCase
import com.ml.lansonesscan.ui.theme.*
import com.ml.lansonesscan.ui.components.GradientCard
import com.ml.lansonesscan.ui.theme.LansonesScanTheme

/**
 * Card component displaying scan statistics on the dashboard
 */
@Composable
fun StatisticsCard(
    statistics: GetScanHistoryUseCase.ScanStatistics,
    modifier: Modifier = Modifier
) {
    GradientCard(
        modifier = modifier.fillMaxWidth(),
        gradient = cardGradient(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with minimal styling
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Statistics",
                    tint = BrandGreen,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Scan Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Statistics Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // First row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatisticItem(
                        icon = Icons.Default.Analytics,
                        value = statistics.totalScans.toString(),
                        label = "Total Scans",
                        modifier = Modifier.weight(1f)
                    )

                    StatisticItem(
                        icon = Icons.Default.CheckCircle,
                        value = statistics.healthyScansCount.toString(),
                        label = "Healthy",
                        modifier = Modifier.weight(1f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                }

                // Second row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatisticItem(
                        icon = Icons.Default.Warning,
                        value = statistics.diseaseDetectedCount.toString(),
                        label = "Disease Found",
                        modifier = Modifier.weight(1f),
                        iconTint = MaterialTheme.colorScheme.error
                    )

                    StatisticItem(
                        icon = Icons.Default.Info,
                        value = statistics.nonLansonesCount.toString(),
                        label = "Non-Lansones",
                        modifier = Modifier.weight(1f),
                        iconTint = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            // Additional info with minimal styling
            if (statistics.totalScans > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Disease Detection Rate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = statistics.getFormattedDetectionRate(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (statistics.diseaseDetectionRate > 50f) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }

                // Storage Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Storage Used",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = statistics.getFormattedStorageSize(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Individual statistic item component
 */
@Composable
private fun StatisticItem(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatisticsCardPreview() {
    LansonesScanTheme {
        StatisticsCard(
            statistics = GetScanHistoryUseCase.ScanStatistics(
                totalScans = 25,
                diseaseDetectedCount = 8,
                healthyScansCount = 15,
                nonLansonesCount = 2,
                totalStorageSize = 1024 * 1024 * 15, // 15 MB
                diseaseDetectionRate = 32.0f
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatisticsCardEmptyPreview() {
    LansonesScanTheme {
        StatisticsCard(
            statistics = GetScanHistoryUseCase.ScanStatistics(
                totalScans = 0,
                diseaseDetectedCount = 0,
                healthyScansCount = 0,
                nonLansonesCount = 0,
                totalStorageSize = 0,
                diseaseDetectionRate = 0f
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}