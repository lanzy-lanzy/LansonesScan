package com.ml.lansonesscan.data.local.validation

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Validates images for format, size, and integrity
 */
class ImageValidator(
    private val context: Context
) {
    
    companion object {
        // Supported image formats
        private val SUPPORTED_FORMATS = setOf("image/jpeg", "image/jpg", "image/png")
        
        // Size constraints
        private const val MIN_WIDTH = 100
        private const val MIN_HEIGHT = 100
        private const val MAX_WIDTH = 4096
        private const val MAX_HEIGHT = 4096
        private const val MAX_FILE_SIZE_MB = 10
        private const val MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024
        
        // Aspect ratio constraints
        private const val MIN_ASPECT_RATIO = 0.25f // 1:4
        private const val MAX_ASPECT_RATIO = 4.0f  // 4:1
    }

    /**
     * Validates an image from URI
     * @param imageUri The image URI to validate
     * @return ValidationResult containing validation status and details
     */
    suspend fun validateImage(imageUri: Uri): ValidationResult = withContext(Dispatchers.IO) {
        try {
            // Check if URI is accessible
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext ValidationResult.failure(ValidationError.INVALID_URI)
            
            inputStream.use { stream ->
                // Get file size
                val fileSize = stream.available().toLong()
                if (fileSize > MAX_FILE_SIZE_BYTES) {
                    return@withContext ValidationResult.failure(ValidationError.FILE_TOO_LARGE)
                }
                
                if (fileSize == 0L) {
                    return@withContext ValidationResult.failure(ValidationError.EMPTY_FILE)
                }
                
                // Check MIME type
                val mimeType = context.contentResolver.getType(imageUri)
                if (mimeType == null || mimeType !in SUPPORTED_FORMATS) {
                    return@withContext ValidationResult.failure(ValidationError.UNSUPPORTED_FORMAT)
                }
                
                // Decode image to check integrity and dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                
                // Reset stream for bitmap decoding
                context.contentResolver.openInputStream(imageUri)?.use { resetStream ->
                    BitmapFactory.decodeStream(resetStream, null, options)
                }
                
                // Check if image could be decoded
                if (options.outWidth <= 0 || options.outHeight <= 0) {
                    return@withContext ValidationResult.failure(ValidationError.CORRUPTED_IMAGE)
                }
                
                // Validate dimensions
                val width = options.outWidth
                val height = options.outHeight
                
                if (width < MIN_WIDTH || height < MIN_HEIGHT) {
                    return@withContext ValidationResult.failure(ValidationError.IMAGE_TOO_SMALL)
                }
                
                if (width > MAX_WIDTH || height > MAX_HEIGHT) {
                    return@withContext ValidationResult.failure(ValidationError.IMAGE_TOO_LARGE)
                }
                
                // Validate aspect ratio
                val aspectRatio = width.toFloat() / height.toFloat()
                if (aspectRatio < MIN_ASPECT_RATIO || aspectRatio > MAX_ASPECT_RATIO) {
                    return@withContext ValidationResult.failure(ValidationError.INVALID_ASPECT_RATIO)
                }
                
                // All validations passed
                ValidationResult.success(
                    ImageInfo(
                        width = width,
                        height = height,
                        fileSize = fileSize,
                        mimeType = mimeType,
                        aspectRatio = aspectRatio
                    )
                )
            }
            
        } catch (e: IOException) {
            ValidationResult.failure(ValidationError.IO_ERROR)
        } catch (e: SecurityException) {
            ValidationResult.failure(ValidationError.PERMISSION_DENIED)
        } catch (e: Exception) {
            ValidationResult.failure(ValidationError.UNKNOWN_ERROR)
        }
    }

    /**
     * Quick validation that only checks basic properties without full decoding
     * @param imageUri The image URI to validate
     * @return ValidationResult with basic validation
     */
    suspend fun quickValidateImage(imageUri: Uri): ValidationResult = withContext(Dispatchers.IO) {
        try {
            // Check MIME type
            val mimeType = context.contentResolver.getType(imageUri)
            if (mimeType == null || mimeType !in SUPPORTED_FORMATS) {
                return@withContext ValidationResult.failure(ValidationError.UNSUPPORTED_FORMAT)
            }
            
            // Check file size
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext ValidationResult.failure(ValidationError.INVALID_URI)
            
            inputStream.use { stream ->
                val fileSize = stream.available().toLong()
                if (fileSize > MAX_FILE_SIZE_BYTES) {
                    return@withContext ValidationResult.failure(ValidationError.FILE_TOO_LARGE)
                }
                
                if (fileSize == 0L) {
                    return@withContext ValidationResult.failure(ValidationError.EMPTY_FILE)
                }
                
                ValidationResult.success(
                    ImageInfo(
                        width = -1, // Not determined in quick validation
                        height = -1,
                        fileSize = fileSize,
                        mimeType = mimeType,
                        aspectRatio = -1f
                    )
                )
            }
            
        } catch (e: IOException) {
            ValidationResult.failure(ValidationError.IO_ERROR)
        } catch (e: SecurityException) {
            ValidationResult.failure(ValidationError.PERMISSION_DENIED)
        } catch (e: Exception) {
            ValidationResult.failure(ValidationError.UNKNOWN_ERROR)
        }
    }

    /**
     * Validates if an image is suitable for analysis
     * @param imageUri The image URI to validate
     * @return True if image is suitable for analysis
     */
    suspend fun isImageSuitableForAnalysis(imageUri: Uri): Boolean {
        val result = validateImage(imageUri)
        return result.isSuccess && result.imageInfo?.let { info ->
            // Additional checks for analysis suitability
            info.width >= 224 && info.height >= 224 && // Minimum for most ML models
            info.fileSize < MAX_FILE_SIZE_BYTES / 2 // Prefer smaller files for faster processing
        } ?: false
    }
}

/**
 * Result of image validation
 */
sealed class ValidationResult {
    data class Success(override val imageInfo: ImageInfo) : ValidationResult() {
        override val error: ValidationError? = null
    }
    data class Failure(override val error: ValidationError, val message: String) : ValidationResult() {
        override val imageInfo: ImageInfo? = null
    }
    
    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
    
    abstract val imageInfo: ImageInfo?
    abstract val error: ValidationError?
    val errorMessage: String? get() = (this as? Failure)?.message
    
    companion object {
        fun success(imageInfo: ImageInfo) = Success(imageInfo)
        fun failure(error: ValidationError) = Failure(error, error.message)
    }
}

/**
 * Information about a validated image
 */
data class ImageInfo(
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val mimeType: String,
    val aspectRatio: Float
) {
    fun getFileSizeInMB(): Float = fileSize / (1024f * 1024f)
    fun getDimensionsString(): String = "${width}x${height}"
}

/**
 * Types of validation errors
 */
enum class ValidationError(val message: String) {
    INVALID_URI("Invalid image URI or file not accessible"),
    UNSUPPORTED_FORMAT("Image format not supported. Please use JPEG or PNG"),
    FILE_TOO_LARGE("Image file is too large. Maximum size is 10MB"),
    EMPTY_FILE("Image file is empty or corrupted"),
    IMAGE_TOO_SMALL("Image is too small. Minimum size is 100x100"),
    IMAGE_TOO_LARGE("Image is too large. Maximum size is 4096x4096"),
    INVALID_ASPECT_RATIO("Image aspect ratio is not suitable for analysis"),
    CORRUPTED_IMAGE("Image file is corrupted or cannot be decoded"),
    IO_ERROR("Error reading image file"),
    PERMISSION_DENIED("Permission denied to access image file"),
    UNKNOWN_ERROR("Unknown error occurred during validation")
}