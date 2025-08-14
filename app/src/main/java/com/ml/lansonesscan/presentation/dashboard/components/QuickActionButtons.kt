package com.ml.lansonesscan.presentation.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ml.lansonesscan.ui.theme.*
import com.ml.lansonesscan.ui.components.*
import com.ml.lansonesscan.ui.theme.LansonesScanTheme

/**
 * Quick action buttons component for starting new analysis
 */
@Composable
fun QuickActionButtons(
    onQuickScanClick: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        // Primary action - Quick Scan with minimal styling
        PrimaryGradientButton(
            text = "Start Quick Scan",
            onClick = onQuickScanClick,
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.Scanner
        )

        // Secondary actions row with reduced spacing
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionCard(
                icon = Icons.Default.CameraAlt,
                title = "Camera",
                subtitle = "Take Photo",
                onClick = onCameraClick,
                modifier = Modifier.weight(1f)
            )

            QuickActionCard(
                icon = Icons.Default.PhotoLibrary,
                title = "Gallery",
                subtitle = "Upload Image",
                onClick = onGalleryClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Individual quick action card component
 */
@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ActionGradientCard(
        title = title,
        subtitle = subtitle,
        icon = icon,
        onClick = onClick,
        modifier = modifier.height(72.dp)
    )
}

/**
 * Alternative layout with floating action button style
 */
@Composable
fun QuickActionFab(
    onQuickScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExtendedFloatingActionButton(
        onClick = onQuickScanClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Icon(
            imageVector = Icons.Default.Scanner,
            contentDescription = "Quick scan"
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Quick Scan",
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickActionButtonsPreview() {
    LansonesScanTheme {
        QuickActionButtons(
            onQuickScanClick = {},
            onCameraClick = {},
            onGalleryClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickActionFabPreview() {
    LansonesScanTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            QuickActionFab(
                onQuickScanClick = {}
            )
        }
    }
}