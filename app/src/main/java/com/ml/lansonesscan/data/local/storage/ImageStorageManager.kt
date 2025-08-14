package com.ml.lansonesscan.data.local.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * Manages image storage operations including saving, retrieving, compression, and cleanup
 */
class ImageStorageManager(
    private val context: Context
) {
    
    companion object {
        private const val IMAGES_DIR = "images"
        private const val ORIGINALS_DIR = "originals"
        private const val THUMBNAILS_DIR = "thumbnails"
        private const val CACHE_DIR = "cache"
        
        private const val THUMBNAIL_SIZE = 200
        private const val COMPRESSION_QUALITY = 85
        private const val THUMBNAIL_QUALITY = 70
    }
    
    private val imagesDir: File by lazy {
        File(context.getExternalFilesDir(null), IMAGES_DIR).apply {
            mkdirs()
        }
    }
    
    private val originalsDir: File by lazy {
        File(imagesDir, ORIGINALS_DIR).apply {
            mkdirs()
        }
    }
    
    private val thumbnailsDir: File by lazy {
        File(imagesDir, THUMBNAILS_DIR).apply {
            mkdirs()
        }
    }
    
    private val cacheDir: File by lazy {
        File(imagesDir, CACHE_DIR).apply {
            mkdirs()
        }
    }

    /**
     * Saves an image from URI to internal storage with compression
     * @param imageUri The source image URI
     * @param generateThumbnail Whether to generate a thumbnail
     * @return The saved image file path or null if failed
     */
    suspend fun saveImage(
        imageUri: Uri,
        generateThumbnail: Boolean = true
    ): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext null
            
            val bitmap = BitmapFactory.decodeStream(inputStream)
                ?: return@withContext null
            
            val fileName = "${UUID.randomUUID()}.jpg"
            val originalFile = File(originalsDir, fileName)
            
            // Save compressed original
            val success = FileOutputStream(originalFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)
            }
            
            if (!success) {
                return@withContext null
            }
            
            // Generate thumbnail if requested
            if (generateThumbnail) {
                generateThumbnail(bitmap, fileName)
            }
            
            bitmap.recycle()
            originalFile.absolutePath
            
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Saves image bytes directly to storage
     * @param imageBytes The image bytes to save
     * @param extension The file extension (without dot)
     * @param generateThumbnail Whether to generate a thumbnail
     * @return The saved image file path or null if failed
     */
    suspend fun saveImage(
        imageBytes: ByteArray,
        extension: String,
        generateThumbnail: Boolean = true
    ): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = "${UUID.randomUUID()}.$extension"
            val originalFile = File(originalsDir, fileName)
            
            // Save image bytes directly
            originalFile.writeBytes(imageBytes)
            
            // Generate thumbnail if requested
            if (generateThumbnail) {
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (bitmap != null) {
                    generateThumbnail(bitmap, fileName)
                    bitmap.recycle()
                }
            }
            
            originalFile.absolutePath
            
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Saves a bitmap directly to storage
     * @param bitmap The bitmap to save
     * @param generateThumbnail Whether to generate a thumbnail
     * @return The saved image file path or null if failed
     */
    suspend fun saveBitmap(
        bitmap: Bitmap,
        generateThumbnail: Boolean = true
    ): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = "${UUID.randomUUID()}.jpg"
            val originalFile = File(originalsDir, fileName)
            
            // Save compressed original
            val success = FileOutputStream(originalFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)
            }
            
            if (!success) {
                return@withContext null
            }
            
            // Generate thumbnail if requested
            if (generateThumbnail) {
                generateThumbnail(bitmap, fileName)
            }
            
            originalFile.absolutePath
            
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Retrieves an image as bitmap from storage
     * @param imagePath The full path to the image file
     * @return The bitmap or null if not found/failed
     */
    suspend fun getImage(imagePath: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(imagePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(imagePath)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Retrieves a thumbnail bitmap
     * @param originalImagePath The path to the original image
     * @return The thumbnail bitmap or null if not found
     */
    suspend fun getThumbnail(originalImagePath: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val fileName = File(originalImagePath).name
            val thumbnailFile = File(thumbnailsDir, fileName)
            
            if (thumbnailFile.exists()) {
                BitmapFactory.decodeFile(thumbnailFile.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets the thumbnail file path for an original image
     * @param originalImagePath The path to the original image
     * @return The thumbnail file path
     */
    fun getThumbnailPath(originalImagePath: String): String {
        val fileName = File(originalImagePath).name
        return File(thumbnailsDir, fileName).absolutePath
    }

    /**
     * Deletes an image and its thumbnail from storage
     * @param imagePath The path to the image to delete
     * @return True if deletion was successful
     */
    suspend fun deleteImage(imagePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val originalFile = File(imagePath)
            val fileName = originalFile.name
            val thumbnailFile = File(thumbnailsDir, fileName)
            
            var success = true
            
            if (originalFile.exists()) {
                success = originalFile.delete()
            }
            
            if (thumbnailFile.exists()) {
                success = thumbnailFile.delete() && success
            }
            
            success
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Cleans up orphaned images that are not referenced in the database
     * @param referencedPaths List of image paths that should be kept
     * @return Number of files cleaned up
     */
    suspend fun cleanupOrphanedImages(referencedPaths: List<String>): Int = withContext(Dispatchers.IO) {
        try {
            val referencedFileNames = referencedPaths.map { File(it).name }.toSet()
            var cleanedCount = 0
            
            // Clean originals
            originalsDir.listFiles()?.forEach { file ->
                if (file.name !in referencedFileNames) {
                    if (file.delete()) {
                        cleanedCount++
                    }
                }
            }
            
            // Clean thumbnails
            thumbnailsDir.listFiles()?.forEach { file ->
                if (file.name !in referencedFileNames) {
                    if (file.delete()) {
                        cleanedCount++
                    }
                }
            }
            
            cleanedCount
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Gets storage information
     * @return StorageInfo containing size and file count details
     */
    suspend fun getStorageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        try {
            val originalFiles = originalsDir.listFiles() ?: emptyArray()
            val thumbnailFiles = thumbnailsDir.listFiles() ?: emptyArray()
            val cacheFiles = cacheDir.listFiles() ?: emptyArray()
            
            val originalSize = originalFiles.sumOf { it.length() }
            val thumbnailSize = thumbnailFiles.sumOf { it.length() }
            val cacheSize = cacheFiles.sumOf { it.length() }
            
            StorageInfo(
                originalImagesCount = originalFiles.size,
                thumbnailsCount = thumbnailFiles.size,
                cacheFilesCount = cacheFiles.size,
                originalImagesSize = originalSize,
                thumbnailsSize = thumbnailSize,
                cacheSize = cacheSize,
                totalSize = originalSize + thumbnailSize + cacheSize
            )
        } catch (e: Exception) {
            StorageInfo()
        }
    }

    /**
     * Clears all cached files
     * @return True if successful
     */
    suspend fun clearCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets all image paths currently stored
     * @return Set of all image file paths
     */
    suspend fun getAllImagePaths(): Set<String> = withContext(Dispatchers.IO) {
        try {
            val originalFiles = originalsDir.listFiles() ?: emptyArray()
            originalFiles.map { it.absolutePath }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * Clears all stored images and thumbnails
     * @return True if successful
     */
    suspend fun clearAllImages(): Boolean = withContext(Dispatchers.IO) {
        try {
            var success = true
            
            originalsDir.listFiles()?.forEach { 
                success = it.delete() && success
            }
            
            thumbnailsDir.listFiles()?.forEach { 
                success = it.delete() && success
            }
            
            cacheDir.listFiles()?.forEach { 
                success = it.delete() && success
            }
            
            success
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Generates a thumbnail for the given bitmap
     * @param bitmap The source bitmap
     * @param fileName The filename to use for the thumbnail
     */
    private suspend fun generateThumbnail(bitmap: Bitmap, fileName: String) = withContext(Dispatchers.IO) {
        try {
            val thumbnailFile = File(thumbnailsDir, fileName)
            
            // Calculate thumbnail dimensions maintaining aspect ratio
            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val thumbnailWidth: Int
            val thumbnailHeight: Int
            
            if (aspectRatio > 1) {
                thumbnailWidth = THUMBNAIL_SIZE
                thumbnailHeight = (THUMBNAIL_SIZE / aspectRatio).toInt()
            } else {
                thumbnailWidth = (THUMBNAIL_SIZE * aspectRatio).toInt()
                thumbnailHeight = THUMBNAIL_SIZE
            }
            
            val thumbnailBitmap = Bitmap.createScaledBitmap(
                bitmap, 
                thumbnailWidth, 
                thumbnailHeight, 
                true
            )
            
            FileOutputStream(thumbnailFile).use { outputStream ->
                thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, outputStream)
            }
            
            thumbnailBitmap.recycle()
        } catch (e: IOException) {
            // Thumbnail generation failed, but this shouldn't fail the main operation
        }
    }
}

/**
 * Data class containing storage information
 */
data class StorageInfo(
    val originalImagesCount: Int = 0,
    val thumbnailsCount: Int = 0,
    val cacheFilesCount: Int = 0,
    val originalImagesSize: Long = 0L,
    val thumbnailsSize: Long = 0L,
    val cacheSize: Long = 0L,
    val totalSize: Long = 0L
) {
    fun getTotalSizeInMB(): Float = totalSize / (1024f * 1024f)
}