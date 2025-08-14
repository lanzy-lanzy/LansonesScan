package com.ml.lansonesscan.presentation.analysis.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.ml.lansonesscan.ui.theme.LansonesScanTheme

/**
 * Composable for selecting images from gallery with proper permissions handling
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GalleryPicker(
    onImageSelected: (Uri) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // Storage permission state (for Android 13+, we use READ_MEDIA_IMAGES)
    val storagePermissionState = rememberPermissionState(
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )
    
    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onImageSelected(uri)
        } else {
            onError("No image selected")
        }
    }
    
    // Check permission and launch gallery
    val launchGallery = {
        when {
            storagePermissionState.status.isGranted -> {
                try {
                    galleryLauncher.launch("image/*")
                } catch (e: Exception) {
                    onError("Failed to open gallery: ${e.message}")
                }
            }
            storagePermissionState.status.shouldShowRationale -> {
                onError("Storage permission is required to access images")
            }
            else -> {
                storagePermissionState.launchPermissionRequest()
            }
        }
    }
    
    // Handle permission result
    LaunchedEffect(storagePermissionState.status) {
        if (storagePermissionState.status.isGranted) {
            // Permission granted, but don't auto-launch gallery
            // User needs to click the button
        } else if (!storagePermissionState.status.shouldShowRationale && 
                   !storagePermissionState.status.isGranted) {
            // Permission permanently denied
            onError("Storage permission denied. Please enable it in app settings.")
        }
    }
    
    GalleryPickerButton(
        onClick = launchGallery,
        enabled = enabled,
        modifier = modifier
    )
}

/**
 * Gallery picker button component
 */
@Composable
private fun GalleryPickerButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Select image from gallery" },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Choose from Gallery",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = "Select an existing image",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/**
 * Standalone gallery picker with permission handling UI
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GalleryPickerWithPermissions(
    onImageSelected: (Uri) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val storagePermissionState = rememberPermissionState(
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onImageSelected(uri)
        }
    }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when {
            storagePermissionState.status.isGranted -> {
                GalleryPickerButton(
                    onClick = { 
                        try {
                            galleryLauncher.launch("image/*")
                        } catch (e: Exception) {
                            onError("Failed to open gallery: ${e.message}")
                        }
                    },
                    enabled = enabled
                )
            }
            
            storagePermissionState.status.shouldShowRationale -> {
                PermissionRationaleCard(
                    title = "Storage Permission Required",
                    description = "This app needs storage access to select images from your gallery.",
                    onGrantPermission = { storagePermissionState.launchPermissionRequest() },
                    onCancel = { onError("Storage permission is required to select images") }
                )
            }
            
            else -> {
                PermissionDeniedCard(
                    title = "Storage Access Denied",
                    description = "Please enable storage permission in app settings to select images from gallery.",
                    onRetry = { storagePermissionState.launchPermissionRequest() }
                )
            }
        }
    }
}

/**
 * Permission rationale card
 */
@Composable
private fun PermissionRationaleCard(
    title: String,
    description: String,
    onGrantPermission: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterHorizontally),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = onGrantPermission,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

/**
 * Permission denied card
 */
@Composable
private fun PermissionDeniedCard(
    title: String,
    description: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterHorizontally),
                tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f)
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.fillMaxWidth()
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onErrorContainer,
                    contentColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text("Try Again")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GalleryPickerPreview() {
    LansonesScanTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GalleryPickerButton(
                    onClick = { },
                    enabled = true
                )
                
                GalleryPickerButton(
                    onClick = { },
                    enabled = false
                )
                
                PermissionRationaleCard(
                    title = "Storage Permission Required",
                    description = "This app needs storage access to select images from your gallery.",
                    onGrantPermission = { },
                    onCancel = { }
                )
                
                PermissionDeniedCard(
                    title = "Storage Access Denied",
                    description = "Please enable storage permission in app settings.",
                    onRetry = { }
                )
            }
        }
    }
}