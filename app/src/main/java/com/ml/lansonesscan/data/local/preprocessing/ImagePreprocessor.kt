package com.ml.lansonesscan.data.local.preprocessing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Handles image preprocessing for API optimization
 */
class ImagePreprocessor(
    private val context: Context
) {
    
    companion object {
        // API optimization constants
        private const val API_MAX_DIMENSION = 1024
        private const val API_COMPRESSION_QUALITY = 85
        private const val API_MAX_FILE_SIZE_KB = 500
        
        // Processing constants
        private const val JPEG_QUALITY_HIGH = 90
        private const val JPEG_QUALITY_MEDIUM = 75
        private const val JPEG_QUALITY_LOW = 60
    }

    /**
     * Preprocesses an image for API submission
     * @param imageUri The source image URI
     * @return PreprocessingResult containing the processed image data
     */
    suspend fun preprocessForApi(imageUri: Uri): PreprocessingResult = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext PreprocessingResult.failure(PreprocessingError.INVALID_URI)
            
            inputStream.use { stream ->
                // Decode the original bitmap
                val originalBitmap = BitmapFactory.decodeStream(stream)
                    ?: return@withContext PreprocessingResult.failure(PreprocessingError.DECODE_FAILED)
                
                try {
                    // Apply rotation correction based on EXIF data
                    val correctedBitmap = correctOrientation(imageUri, originalBitmap)
                    
                    // Resize if necessary
                    val resizedBitmap = resizeForApi(correctedBitmap)
                    
                    // Compress to bytes with quality optimization
                    val compressedBytes = compressForApi(resizedBitmap)
                    
                    // Clean up bitmaps
                    if (correctedBitmap != originalBitmap) {
                        correctedBitmap.recycle()
                    }
                    if (resizedBitmap != correctedBitmap) {
                        resizedBitmap.recycle()
                    }
                    originalBitmap.recycle()
                    
                    PreprocessingResult.success(
                        ProcessedImageData(
                            imageBytes = compressedBytes,
                            width = resizedBitmap.width,
                            height = resizedBitmap.height,
                            fileSizeKB = compressedBytes.size / 1024,
                            compressionQuality = getUsedCompressionQuality(compressedBytes.size)
                        )
                    )
                    
                } catch (e: Exception) {
                    originalBitmap.recycle()
                    PreprocessingResult.failure(PreprocessingError.PROCESSING_FAILED)
                }
            }
            
        } catch (e: IOException) {
            PreprocessingResult.failure(PreprocessingError.IO_ERROR)
        } catch (e: OutOfMemoryError) {
            PreprocessingResult.failure(PreprocessingError.OUT_OF_MEMORY)
        } catch (e: Exception) {
            PreprocessingResult.failure(PreprocessingError.UNKNOWN_ERROR)
        }
    }

    /**
     * Creates a compressed version of a bitmap for storage
     * @param bitmap The source bitmap
     * @param quality The compression quality (0-100)
     * @return Compressed image bytes or null if failed
     */
    suspend fun compressForStorage(
        bitmap: Bitmap, 
        quality: Int = JPEG_QUALITY_HIGH
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val outputStream = ByteArrayOutputStream()
            val success = bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            if (success) outputStream.toByteArray() else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Resizes a bitmap to fit within API constraints while maintaining aspect ratio
     * @param bitmap The source bitmap
     * @return Resized bitmap
     */
    private fun resizeForApi(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Check if resizing is needed
        if (width <= API_MAX_DIMENSION && height <= API_MAX_DIMENSION) {
            return bitmap
        }
        
        // Calculate new dimensions maintaining aspect ratio
        val aspectRatio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        
        if (width > height) {
            newWidth = API_MAX_DIMENSION
            newHeight = (API_MAX_DIMENSION / aspectRatio).toInt()
        } else {
            newHeight = API_MAX_DIMENSION
            newWidth = (API_MAX_DIMENSION * aspectRatio).toInt()
        }
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Compresses a bitmap to bytes with size optimization for API
     * @param bitmap The bitmap to compress
     * @return Compressed image bytes
     */
    private fun compressForApi(bitmap: Bitmap): ByteArray {
        var quality = API_COMPRESSION_QUALITY
        var compressedBytes: ByteArray
        
        do {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            compressedBytes = outputStream.toByteArray()
            
            // If still too large, reduce quality
            if (compressedBytes.size > API_MAX_FILE_SIZE_KB * 1024 && quality > 30) {
                quality -= 10
            } else {
                break
            }
        } while (quality > 30)
        
        return compressedBytes
    }

    /**
     * Corrects image orientation based on EXIF data
     * @param imageUri The source image URI
     * @param bitmap The bitmap to correct
     * @return Orientation-corrected bitmap
     */
    private suspend fun correctOrientation(imageUri: Uri, bitmap: Bitmap): Bitmap = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext bitmap
            
            inputStream.use { stream ->
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                    else -> return@withContext bitmap
                }
                
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
        } catch (e: Exception) {
            // If orientation correction fails, return original bitmap
            bitmap
        }
    }

    /**
     * Determines the compression quality that was likely used based on file size
     * @param fileSizeBytes The compressed file size in bytes
     * @return Estimated compression quality
     */
    private fun getUsedCompressionQuality(fileSizeBytes: Int): Int {
        val fileSizeKB = fileSizeBytes / 1024
        return when {
            fileSizeKB > 400 -> JPEG_QUALITY_HIGH
            fileSizeKB > 200 -> JPEG_QUALITY_MEDIUM
            else -> JPEG_QUALITY_LOW
        }
    }

    /**
     * Creates a preview bitmap with reduced size for UI display
     * @param imageUri The source image URI
     * @param maxDimension Maximum dimension for the preview
     * @return Preview bitmap or null if failed
     */
    suspend fun createPreview(
        imageUri: Uri, 
        maxDimension: Int = 400
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext null
            
            inputStream.use { stream ->
                // First, decode with inJustDecodeBounds to get dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(stream, null, options)
                
                // Calculate sample size
                options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
                options.inJustDecodeBounds = false
                
                // Decode the sampled bitmap
                context.contentResolver.openInputStream(imageUri)?.use { resetStream ->
                    BitmapFactory.decodeStream(resetStream, null, options)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculates the appropriate sample size for bitmap decoding
     * @param options BitmapFactory options with original dimensions
     * @param reqWidth Required width
     * @param reqHeight Required height
     * @return Sample size
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
}

/**
 * Result of image preprocessing
 */
sealed class PreprocessingResult {
    data class Success(override val processedData: ProcessedImageData) : PreprocessingResult() {
        override val error: PreprocessingError? = null
    }
    data class Failure(override val error: PreprocessingError, val message: String) : PreprocessingResult() {
        override val processedData: ProcessedImageData? = null
    }
    
    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
    
    abstract val processedData: ProcessedImageData?
    abstract val error: PreprocessingError?
    val errorMessage: String? get() = (this as? Failure)?.message
    
    companion object {
        fun success(processedData: ProcessedImageData) = Success(processedData)
        fun failure(error: PreprocessingError) = Failure(error, error.message)
    }
}

/**
 * Data class containing processed image information
 */
data class ProcessedImageData(
    val imageBytes: ByteArray,
    val width: Int,
    val height: Int,
    val fileSizeKB: Int,
    val compressionQuality: Int
) {
    fun getFileSizeMB(): Float = fileSizeKB / 1024f
    fun getDimensionsString(): String = "${width}x${height}"
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as ProcessedImageData
        
        if (!imageBytes.contentEquals(other.imageBytes)) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (fileSizeKB != other.fileSizeKB) return false
        if (compressionQuality != other.compressionQuality) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = imageBytes.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + fileSizeKB
        result = 31 * result + compressionQuality
        return result
    }
}

/**
 * Types of preprocessing errors
 */
enum class PreprocessingError(val message: String) {
    INVALID_URI("Invalid image URI or file not accessible"),
    DECODE_FAILED("Failed to decode image"),
    PROCESSING_FAILED("Image processing failed"),
    IO_ERROR("Error reading image file"),
    OUT_OF_MEMORY("Not enough memory to process image"),
    UNKNOWN_ERROR("Unknown error occurred during preprocessing")
}