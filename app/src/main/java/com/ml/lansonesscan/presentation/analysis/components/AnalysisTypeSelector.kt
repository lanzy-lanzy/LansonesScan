package com.ml.lansonesscan.presentation.analysis.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.ui.theme.LansonesScanTheme

/**
 * Composable for selecting analysis type (Fruit or Leaves)
 * Provides visual indicators and proper accessibility support
 */
@Composable
fun AnalysisTypeSelector(
    selectedType: AnalysisType?,
    onTypeSelected: (AnalysisType) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup()
            .semantics {
                contentDescription = "Analysis type selection"
            },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Select Analysis Type",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnalysisTypeCard(
                analysisType = AnalysisType.FRUIT,
                icon = Icons.Default.LocalFlorist,
                isSelected = selectedType == AnalysisType.FRUIT,
                onSelected = { onTypeSelected(AnalysisType.FRUIT) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
            
            AnalysisTypeCard(
                analysisType = AnalysisType.LEAVES,
                icon = Icons.Default.Eco,
                isSelected = selectedType == AnalysisType.LEAVES,
                onSelected = { onTypeSelected(AnalysisType.LEAVES) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Individual card for each analysis type option
 */
@Composable
private fun AnalysisTypeCard(
    analysisType: AnalysisType,
    icon: ImageVector,
    isSelected: Boolean,
    onSelected: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val cardColors = if (isSelected) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    }
    
    val borderStroke = if (isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    }
    
    Card(
        modifier = modifier
            .selectable(
                selected = isSelected,
                onClick = onSelected,
                enabled = enabled,
                role = Role.RadioButton
            )
            .semantics {
                contentDescription = "${analysisType.getDisplayName()}: ${analysisType.getDescription()}"
            },
        colors = cardColors,
        border = borderStroke,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Text(
                text = analysisType.getDisplayName(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = analysisType.getDescription(),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight
            )
            
            // Add leaf-specific note
            if (analysisType == AnalysisType.LEAVES) {
                Text(
                    text = "Detects leaf spots, pests, and nutrient issues",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Selection indicator
            if (isSelected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFlorist, // Using as checkmark placeholder
                        contentDescription = "Selected",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Selected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AnalysisTypeSelectorPreview() {
    LansonesScanTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // No selection
                AnalysisTypeSelector(
                    selectedType = null,
                    onTypeSelected = { }
                )
                
                Divider()
                
                // Fruit selected
                AnalysisTypeSelector(
                    selectedType = AnalysisType.FRUIT,
                    onTypeSelected = { }
                )
                
                Divider()
                
                // Leaves selected
                AnalysisTypeSelector(
                    selectedType = AnalysisType.LEAVES,
                    onTypeSelected = { }
                )
                
                Divider()
                
                // Disabled state
                AnalysisTypeSelector(
                    selectedType = AnalysisType.FRUIT,
                    onTypeSelected = { },
                    enabled = false
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Dark Theme")
@Composable
private fun AnalysisTypeSelectorDarkPreview() {
    LansonesScanTheme(darkTheme = true) {
        Surface {
            AnalysisTypeSelector(
                selectedType = AnalysisType.FRUIT,
                onTypeSelected = { },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}