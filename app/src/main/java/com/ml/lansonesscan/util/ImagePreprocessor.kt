package com.ml.lansonesscan.util

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log

/**
 * Image preprocessing utilities for optimal AI analysis
 * Based on TomatoScan implementation for consistent results
 */
object ImagePreprocessor {
    
    private const val TAG = "ImagePreprocessor"
    
    // Optimal dimensions for Gemini API (balance between quality and speed)
    private const val MAX_WIDTH = 1024
    private const val MAX_HEIGHT = 1024
    private const val MIN_WIDTH = 512
    private const val MIN_HEIGHT = 512
    
    /**
     * Preprocesses image for analysis - resizes and optimizes
     * Based on TomatoScan's proven approach
     */
    fun preprocessForAnalysis(bitmap: Bitmap): Bitmap {
        val startTime = System.currentTimeMillis()
        
        // Check if image needs preprocessing
        if (bitmap.width <= MAX_WIDTH && bitmap.height <= MAX_HEIGHT &&
            bitmap.width >= MIN_WIDTH && bitmap.height >= MIN_HEIGHT) {
            Log.d(TAG, "Image already optimal size: ${bitmap.width}x${bitmap.height}")
            return bitmap
        }
        
        // Calculate optimal dimensions maintaining aspect ratio
        val (newWidth, newHeight) = calculateOptimalDimensions(
            bitmap.width,
            bitmap.height
        )
        
        // Resize image
        val resized = resizeBitmap(bitmap, newWidth, newHeight)
        
        val processingTime = System.currentTimeMillis() - startTime
        Log.d(TAG, "Image preprocessed: ${bitmap.width}x${bitmap.height} → ${newWidth}x${newHeight} (${processingTime}ms)")
        
        return resized
    }
    
    /**
     * Calculates optimal dimensions maintaining aspect ratio
     */
    private fun calculateOptimalDimensions(width: Int, height: Int): Pair<Int, Int> {
        val aspectRatio = width.toFloat() / height.toFloat()
        
        return when {
            // Image is too large - scale down
            width > MAX_WIDTH || height > MAX_HEIGHT -> {
                if (aspectRatio > 1) {
                    // Landscape
                    val newWidth = MAX_WIDTH
                    val newHeight = (MAX_WIDTH / aspectRatio).toInt()
                    newWidth to newHeight
                } else {
                    // Portrait or square
                    val newHeight = MAX_HEIGHT
                    val newWidth = (MAX_HEIGHT * aspectRatio).toInt()
                    newWidth to newHeight
                }
            }
            // Image is too small - scale up slightly
            width < MIN_WIDTH || height < MIN_HEIGHT -> {
                if (aspectRatio > 1) {
                    val newWidth = MIN_WIDTH
                    val newHeight = (MIN_WIDTH / aspectRatio).toInt()
                    newWidth to newHeight
                } else {
                    val newHeight = MIN_HEIGHT
                    val newWidth = (MIN_HEIGHT * aspectRatio).toInt()
                    newWidth to newHeight
                }
            }
            // Image is already optimal
            else -> width to height
        }
    }
    
    /**
     * Resizes bitmap to specified dimensions using high-quality filtering
     */
    private fun resizeBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        return try {
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resize bitmap, using original", e)
            bitmap
        }
    }
    
    /**
     * Rotates bitmap if needed (for camera images)
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        
        return try {
            val matrix = Matrix().apply {
                postRotate(degrees)
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate bitmap", e)
            bitmap
        }
    }
    
    /**
     * Compresses bitmap to JPEG with optimal quality
     */
    fun compressBitmap(bitmap: Bitmap, quality: Int = 90): ByteArray {
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }
}
