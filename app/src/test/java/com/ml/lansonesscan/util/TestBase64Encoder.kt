package com.ml.lansonesscan.util

import java.util.Base64

/**
 * Test implementation using java.util.Base64
 */
class TestBase64Encoder : Base64Encoder {
    override fun encodeToString(input: ByteArray): String {
        return Base64.getEncoder().encodeToString(input)
    }
}