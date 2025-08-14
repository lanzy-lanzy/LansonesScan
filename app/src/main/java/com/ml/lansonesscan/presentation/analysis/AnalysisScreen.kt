package com.ml.lansonesscan.presentation.analysis

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ml.lansonesscan.ui.theme.*
import com.ml.lansonesscan.ui.components.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.presentation.analysis.components.*
import com.ml.lansonesscan.ui.theme.LansonesScanTheme

/**
 * Main Analysis screen composable with complete workflow integration
 * Handles image capture, upload, analysis type selection, results display,
 * loading states, error handling, and navigation
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel,
    onNavigateToScanDetail: (String) -> Unit,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Permission states
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    ) { granted ->
        viewModel.onCameraPermissionResult(granted)
    }

    // Use READ_MEDIA_IMAGES for Android 13+ (API 33+), fallback to READ_EXTERNAL_STORAGE for older versions
    val storagePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_IMAGES
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    val storagePermissionState = rememberPermissionState(
        storagePermission
    ) { granted ->
        viewModel.onStoragePermissionResult(granted)
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setImageUri(it) }
    }

    // Create temporary file for camera capture
    val createImageFile = remember {
        {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "LANSONES_${timeStamp}_"
            val storageDir = File(context.cacheDir, "images")
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            File.createTempFile(imageFileName, ".jpg", storageDir)
        }
    }

    // Remember the current photo URI
    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Camera launcher with proper file handling
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            // Photo was taken successfully, set the URI
            viewModel.setImageUri(currentPhotoUri!!)
        }
    }

    // Settings launcher for opening app settings
    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { 
        // User returned from settings, permissions might have changed
        // The permission states will automatically update
    }

    // Function to open app settings
    val openAppSettings = {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        settingsLauncher.launch(intent)
    }

    // Handle navigation events
    LaunchedEffect(uiState.navigationEvent) {
        when (val event = uiState.navigationEvent) {
            is AnalysisNavigationEvent.NavigateToScanDetail -> {
                onNavigateToScanDetail(event.scanId)
                viewModel.onNavigationEventHandled()
            }
            is AnalysisNavigationEvent.NavigateToDashboard -> {
                onNavigateToDashboard()
                viewModel.onNavigationEventHandled()
            }
            null -> { /* No navigation event */ }
        }
    }

    // Auto-scroll to results when analysis completes
    LaunchedEffect(uiState.analysisResult) {
        if (uiState.analysisResult != null) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .primaryGradientBackground()
            .testTag("analysis_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            AnalysisHeader()

            // Analysis Type Selection
            AnimatedVisibility(
                visible = !uiState.isAnalyzing,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                AnalysisTypeSelector(
                    selectedType = uiState.selectedAnalysisType,
                    onTypeSelected = viewModel::setAnalysisType,
                    enabled = !uiState.isAnalyzing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("analysis_type_selector")
                )
            }

            // Image Selection Section
            AnimatedVisibility(
                visible = !uiState.isAnalyzing,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                ImageSelectionSection(
                    selectedImageUri = uiState.selectedImageUri,
                    onCameraClick = {
                        when {
                            cameraPermissionState.status.isGranted -> {
                                try {
                                    // Create temporary file for camera capture
                                    val photoFile = createImageFile()
                                    val photoUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        photoFile
                                    )
                                    currentPhotoUri = photoUri
                                    cameraLauncher.launch(photoUri)
                                } catch (e: Exception) {
                                    viewModel.clearError()
                                    // Fallback to gallery if camera fails
                                    if (storagePermissionState.status.isGranted) {
                                        imagePickerLauncher.launch("image/*")
                                    } else {
                                        storagePermissionState.launchPermissionRequest()
                                    }
                                }
                            }
                            cameraPermissionState.status.shouldShowRationale -> {
                                // Permission rationale will be shown below
                            }
                            else -> {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        }
                    },
                    onGalleryClick = {
                        when {
                            storagePermissionState.status.isGranted -> {
                                imagePickerLauncher.launch("image/*")
                            }
                            storagePermissionState.status.shouldShowRationale -> {
                                // Permission rationale will be shown below
                            }
                            else -> {
                                storagePermissionState.launchPermissionRequest()
                            }
                        }
                    },
                    enabled = !uiState.isAnalyzing,
                    cameraPermissionGranted = cameraPermissionState.status.isGranted,
                    storagePermissionGranted = storagePermissionState.status.isGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("image_selection_section")
                )
            }

            // Image Preview
            AnimatedVisibility(
                visible = uiState.selectedImageUri != null && !uiState.isAnalyzing,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                uiState.selectedImageUri?.let { uri ->
                    CompactImagePreview(
                        imageUri = uri,
                        onClear = { viewModel.setImageUri(Uri.EMPTY) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .testTag("image_preview")
                    )
                }
            }

            // Analysis Button
            AnimatedVisibility(
                visible = !uiState.isAnalyzing,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                AnalysisButton(
                    canStartAnalysis = uiState.canStartAnalysis,
                    isAnalyzing = uiState.isAnalyzing,
                    onStartAnalysis = viewModel::startAnalysis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("analysis_button")
                )
            }

            // Analysis Progress
            AnimatedVisibility(
                visible = uiState.isAnalyzing,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                AnalysisProgress(
                    progress = uiState.analysisProgress,
                    status = uiState.displayStatus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("analysis_progress")
                )
            }

            // Analysis Results
            AnimatedVisibility(
                visible = uiState.analysisResult != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                uiState.analysisResult?.let { result ->
                    AnalysisResults(
                        scanResult = result,
                        onViewDetails = viewModel::onViewResultDetails,
                        onStartNewScan = viewModel::resetAnalysis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analysis_results")
                    )
                }
            }

            // Error Display
            AnimatedVisibility(
                visible = uiState.hasError,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                uiState.error?.let { error ->
                    ErrorCard(
                        error = error,
                        onRetry = if (uiState.canStartAnalysis) {
                            { viewModel.retryAnalysis() }
                        } else null,
                        onDismiss = viewModel::clearError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("error_card")
                    )
                }
            }

            // Permission handling section
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Camera Permission Cards
                when {
                    !cameraPermissionState.status.isGranted && cameraPermissionState.status.shouldShowRationale -> {
                        PermissionRationaleCard(
                            title = "Camera Permission Required",
                            description = "Camera access is needed to capture images of your lansones for analysis.",
                            onGrantPermission = { cameraPermissionState.launchPermissionRequest() },
                            onOpenSettings = openAppSettings,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("camera_permission_rationale")
                        )
                    }
                    !cameraPermissionState.status.isGranted -> {
                        // Show permission denied card only if user has interacted with camera button
                        // This prevents showing the card immediately on first app launch
                        if (uiState.error?.contains("Camera permission", ignoreCase = true) == true) {
                            PermissionDeniedCard(
                                title = "Camera Permission Denied",
                                description = "Camera permission was denied. To use camera features, please enable camera permission in app settings.",
                                onOpenSettings = openAppSettings,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("camera_permission_denied")
                            )
                        }
                    }
                }

                // Storage Permission Cards
                when {
                    !storagePermissionState.status.isGranted && storagePermissionState.status.shouldShowRationale -> {
                        PermissionRationaleCard(
                            title = "Storage Permission Required",
                            description = "Storage access is needed to select images from your gallery for analysis.",
                            onGrantPermission = { storagePermissionState.launchPermissionRequest() },
                            onOpenSettings = openAppSettings,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("storage_permission_rationale")
                        )
                    }
                    !storagePermissionState.status.isGranted -> {
                        // Show permission denied card only if user has interacted with gallery button
                        if (uiState.error?.contains("Storage permission", ignoreCase = true) == true) {
                            PermissionDeniedCard(
                                title = "Storage Permission Denied",
                                description = "Storage permission was denied. To select images from gallery, please enable storage permission in app settings.",
                                onOpenSettings = openAppSettings,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("storage_permission_denied")
                            )
                        }
                    }
                }
            }

            // Bottom padding for scroll
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Note: Removed loading overlay as it was blocking navigation
        // The analysis progress is already shown inline in the content
    }
}

/**
 * Analysis screen header with improved accessibility and styling
 */
@Composable
private fun AnalysisHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.testTag("analysis_header")
    ) {
        Text(
            text = "Disease Analysis",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("analysis_title")
        )
        Text(
            text = "Capture or upload an image to analyze your lansones for diseases",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("analysis_subtitle")
        )
    }
}

/**
 * Image selection section with camera and gallery options
 * Enhanced with better accessibility and visual feedback
 */
@Composable
private fun ImageSelectionSection(
    selectedImageUri: Uri?,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    enabled: Boolean,
    cameraPermissionGranted: Boolean,
    storagePermissionGranted: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Select Image Source",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.testTag("image_source_title")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Camera Button
            OutlinedCard(
                onClick = onCameraClick,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .testTag("camera_button"),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (cameraPermissionGranted) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    }
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (cameraPermissionGranted) {
                            MaterialTheme.colorScheme.outline
                        } else {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        }
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Take photo with camera",
                        modifier = Modifier.size(32.dp),
                        tint = when {
                            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            cameraPermissionGranted -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                    Text(
                        text = if (cameraPermissionGranted) "Take Photo" else "Camera Permission Required",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = when {
                            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            cameraPermissionGranted -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
            }

            // Gallery Button
            OutlinedCard(
                onClick = onGalleryClick,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .testTag("gallery_button"),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (storagePermissionGranted) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    }
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (storagePermissionGranted) {
                            MaterialTheme.colorScheme.outline
                        } else {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        }
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Select image from gallery",
                        modifier = Modifier.size(32.dp),
                        tint = when {
                            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            storagePermissionGranted -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                    Text(
                        text = if (storagePermissionGranted) "Upload Image" else "Storage Permission Required",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = when {
                            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            storagePermissionGranted -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }
    }
}

/**
 * Analysis button component with enhanced states and accessibility
 */
@Composable
private fun AnalysisButton(
    canStartAnalysis: Boolean,
    isAnalyzing: Boolean,
    onStartAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    PrimaryGradientButton(
        text = "Start Analysis",
        onClick = onStartAnalysis,
        enabled = canStartAnalysis,
        isLoading = isAnalyzing,
        modifier = modifier.testTag("start_analysis_button")
    )
}

/**
 * Analysis progress component with enhanced visual feedback
 */
@Composable
private fun AnalysisProgress(
    progress: Float,
    status: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("analysis_progress_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Analysis in Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("progress_indicator"),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("progress_status")
            )
        }
    }
}

/**
 * Error card component with enhanced error handling and accessibility
 */
@Composable
private fun ErrorCard(
    error: String,
    onRetry: (() -> Unit)?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("error_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = "Analysis Failed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.testTag("error_message")
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onRetry != null) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("retry_button")
                    ) {
                        Text("Retry Analysis")
                    }
                }
                
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dismiss_error_button")
                ) {
                    Text("Dismiss")
                }
            }
        }
    }
}

/**
 * Permission rationale card with enhanced accessibility and visual design
 */
@Composable
private fun PermissionRationaleCard(
    title: String,
    description: String,
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("permission_rationale_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Permission required",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onGrantPermission,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("grant_permission_button")
                ) {
                    Text("Grant Permission")
                }
                
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_settings_button")
                ) {
                    Text("Open Settings")
                }
            }
        }
    }
}

/**
 * Permission denied card for when permission is permanently denied
 */
@Composable
private fun PermissionDeniedCard(
    title: String,
    description: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("permission_denied_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Permission denied",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Button(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_settings_from_denied_button")
            ) {
                Text("Open App Settings")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AnalysisScreenPreview() {
    LansonesScanTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            AnalysisHeader()
            
            AnalysisTypeSelector(
                selectedType = AnalysisType.FRUIT,
                onTypeSelected = {},
                enabled = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            ImageSelectionSection(
                selectedImageUri = null,
                onCameraClick = {},
                onGalleryClick = {},
                enabled = true,
                cameraPermissionGranted = true,
                storagePermissionGranted = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}