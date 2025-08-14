package com.ml.lansonesscan.util

/**
 * Interface for Base64 encoding operations
 */
interface Base64Encoder {
    fun encodeToString(input: ByteArray): String
}

/**
 * Android implementation using android.util.Base64
 */
class AndroidBase64Encoder : Base64Encoder {
    override fun encodeToString(input: ByteArray): String {
        return android.util.Base64.encodeToString(input, android.util.Base64.NO_WRAP)
    }
}