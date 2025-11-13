package com.ml.lansonesscan.data.cache

import androidx.collection.LruCache
import com.ml.lansonesscan.data.remote.service.AnalysisResult
import java.security.MessageDigest

/**
 * Cache for storing analysis results to avoid redundant API calls for identical images
 */
class AnalysisCache {
    
    private val cache = LruCache<String, CachedAnalysisResult>(MAX_CACHE_SIZE)
    
    companion object {
        private const val MAX_CACHE_SIZE = 50
        private const val CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000L // 24 hours
    }
    
    /**
     * Generates a hash for the image bytes to use as cache key
     */
    fun generateImageHash(imageBytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(imageBytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Retrieves cached analysis result if available and not expired
     */
    @Synchronized
    fun get(imageHash: String): AnalysisResult? {
        val cached = cache.get(imageHash) ?: return null
        
        // Check if cache entry has expired
        if (System.currentTimeMillis() - cached.timestamp > CACHE_EXPIRY_MS) {
            cache.remove(imageHash)
            return null
        }
        
        return cached.result
    }
    
    /**
     * Stores analysis result in cache
     */
    @Synchronized
    fun put(imageHash: String, result: AnalysisResult) {
        cache.put(imageHash, CachedAnalysisResult(
            result = result,
            timestamp = System.currentTimeMillis()
        ))
    }
    
    /**
     * Checks if a result exists in cache for the given image hash
     */
    @Synchronized
    fun contains(imageHash: String): Boolean {
        return get(imageHash) != null
    }
    
    /**
     * Clears all cached results
     */
    @Synchronized
    fun clear() {
        cache.evictAll()
    }
    
    /**
     * Gets cache statistics
     */
    @Synchronized
    fun getStats(): CacheStats {
        return CacheStats(
            size = cache.size(),
            maxSize = cache.maxSize()
        )
    }
}

/**
 * Wrapper for cached analysis result with timestamp
 */
data class CachedAnalysisResult(
    val result: AnalysisResult,
    val timestamp: Long
)

/**
 * Cache statistics
 */
data class CacheStats(
    val size: Int,
    val maxSize: Int
)