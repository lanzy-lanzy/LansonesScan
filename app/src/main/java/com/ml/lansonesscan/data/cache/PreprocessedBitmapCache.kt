package com.ml.lansonesscan.data.cache

import android.graphics.Bitmap
import androidx.collection.LruCache

/**
 * A simple in-memory cache for preprocessed bitmaps.
 */
class PreprocessedBitmapCache {

    private val cache = LruCache<String, Bitmap>(10)

    fun get(key: String): Bitmap? {
        return cache.get(key)
    }

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }
}