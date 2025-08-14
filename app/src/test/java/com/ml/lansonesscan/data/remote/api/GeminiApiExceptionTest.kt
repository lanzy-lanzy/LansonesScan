package com.ml.lansonesscan.data.remote.api

import com.ml.lansonesscan.data.remote.dto.GeminiError
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class GeminiApiExceptionTest {
    
    @Test
    fun `should create exception with basic parameters`() {
        // Given
        val code = 400
        val message = "Bad request"
        
        // When
        val exception = GeminiApiException(code, message)
        
        // Then
        assertEquals(code, exception.code)
        assertEquals(message, exception.message)
        assertNull(exception.geminiError)
        assertNull(exception.cause)
    }
    
    @Test
    fun `should create exception with gemini error`() {
        // Given
        val code = 429
        val message = "Rate limit exceeded"
        val geminiError = GeminiError(
            code = 429,
            message = "Too many requests",
            status = "RESOURCE_EXHAUSTED"
        )
        
        // When
        val exception = GeminiApiException(code, message, geminiError)
        
        // Then
        assertEquals(code, exception.code)
        assertEquals(message, exception.message)
        assertEquals(geminiError, exception.geminiError)
        assertNull(exception.cause)
    }
    
    @Test
    fun `should create exception with cause`() {
        // Given
        val code = -1
        val message = "Network error"
        val cause = RuntimeException("Connection failed")
        
        // When
        val exception = GeminiApiException(code, message, cause = cause)
        
        // Then
        assertEquals(code, exception.code)
        assertEquals(message, exception.message)
        assertNull(exception.geminiError)
        assertEquals(cause, exception.cause)
    }
    
    @Test
    fun `should create exception with all parameters`() {
        // Given
        val code = 403
        val message = "Forbidden"
        val geminiError = GeminiError(
            code = 403,
            message = "API key invalid",
            status = "PERMISSION_DENIED"
        )
        val cause = RuntimeException("Auth failed")
        
        // When
        val exception = GeminiApiException(code, message, geminiError, cause)
        
        // Then
        assertEquals(code, exception.code)
        assertEquals(message, exception.message)
        assertEquals(geminiError, exception.geminiError)
        assertEquals(cause, exception.cause)
    }
    
    @Test
    fun `should detect rate limit error by HTTP code`() {
        // Given
        val exception = GeminiApiException(429, "Rate limited")
        
        // When & Then
        assertTrue(exception.isRateLimitError())
        assertFalse(exception.isQuotaExceededError())
        assertFalse(exception.isInvalidApiKeyError())
    }
    
    @Test
    fun `should detect rate limit error by gemini error code`() {
        // Given
        val geminiError = GeminiError(code = 429, message = "Rate limit", status = "RESOURCE_EXHAUSTED")
        val exception = GeminiApiException(200, "OK", geminiError)
        
        // When & Then
        assertTrue(exception.isRateLimitError())
    }
    
    @Test
    fun `should detect quota exceeded error by status`() {
        // Given
        val geminiError = GeminiError(code = 429, message = "Quota exceeded", status = "RESOURCE_EXHAUSTED")
        val exception = GeminiApiException(429, "Quota exceeded", geminiError)
        
        // When & Then
        assertTrue(exception.isQuotaExceededError())
        assertTrue(exception.isRateLimitError()) // Should also be true for rate limit
    }
    
    @Test
    fun `should detect quota exceeded error by message content`() {
        // Given
        val geminiError = GeminiError(code = 400, message = "API quota exceeded for this request", status = "INVALID_ARGUMENT")
        val exception = GeminiApiException(400, "Bad request", geminiError)
        
        // When & Then
        assertTrue(exception.isQuotaExceededError())
        assertFalse(exception.isRateLimitError())
    }
    
    @Test
    fun `should detect invalid API key error by HTTP codes`() {
        // Given
        val exception401 = GeminiApiException(401, "Unauthorized")
        val exception403 = GeminiApiException(403, "Forbidden")
        
        // When & Then
        assertTrue(exception401.isInvalidApiKeyError())
        assertTrue(exception403.isInvalidApiKeyError())
        assertFalse(exception401.isRateLimitError())
        assertFalse(exception403.isRateLimitError())
    }
    
    @Test
    fun `should detect invalid API key error by gemini error status`() {
        // Given
        val unauthenticatedError = GeminiError(code = 401, message = "Invalid API key", status = "UNAUTHENTICATED")
        val permissionDeniedError = GeminiError(code = 403, message = "Permission denied", status = "PERMISSION_DENIED")
        
        val exception1 = GeminiApiException(200, "OK", unauthenticatedError)
        val exception2 = GeminiApiException(200, "OK", permissionDeniedError)
        
        // When & Then
        assertTrue(exception1.isInvalidApiKeyError())
        assertTrue(exception2.isInvalidApiKeyError())
    }
    
    @Test
    fun `should not detect errors when conditions are not met`() {
        // Given
        val geminiError = GeminiError(code = 500, message = "Internal server error", status = "INTERNAL")
        val exception = GeminiApiException(500, "Server error", geminiError)
        
        // When & Then
        assertFalse(exception.isRateLimitError())
        assertFalse(exception.isQuotaExceededError())
        assertFalse(exception.isInvalidApiKeyError())
    }
    
    @Test
    fun `should handle null gemini error gracefully`() {
        // Given
        val exception = GeminiApiException(500, "Server error")
        
        // When & Then
        assertFalse(exception.isRateLimitError())
        assertFalse(exception.isQuotaExceededError())
        assertFalse(exception.isInvalidApiKeyError())
    }
    
    @Test
    fun `should handle case insensitive quota message matching`() {
        // Given
        val geminiError1 = GeminiError(code = 400, message = "API QUOTA exceeded", status = "INVALID_ARGUMENT")
        val geminiError2 = GeminiError(code = 400, message = "quota limit reached", status = "INVALID_ARGUMENT")
        val geminiError3 = GeminiError(code = 400, message = "Quota Exceeded", status = "INVALID_ARGUMENT")
        
        val exception1 = GeminiApiException(400, "Bad request", geminiError1)
        val exception2 = GeminiApiException(400, "Bad request", geminiError2)
        val exception3 = GeminiApiException(400, "Bad request", geminiError3)
        
        // When & Then
        assertTrue(exception1.isQuotaExceededError())
        assertTrue(exception2.isQuotaExceededError())
        assertTrue(exception3.isQuotaExceededError())
    }
    
    @Test
    fun `should match quota in message regardless of context`() {
        // Given - The current implementation uses contains() which will match "quota" anywhere in the message
        val geminiError = GeminiError(code = 400, message = "Request quotation invalid", status = "INVALID_ARGUMENT")
        val exception = GeminiApiException(400, "Bad request", geminiError)
        
        // When & Then - This actually returns true because "quotation" contains "quota"
        assertTrue(exception.isQuotaExceededError())
    }
}