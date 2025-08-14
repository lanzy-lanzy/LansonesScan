package com.ml.lansonesscan.data.remote.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ml.lansonesscan.data.remote.dto.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class GeminiApiClientTest {
    
    private lateinit var gson: Gson
    
    @BeforeEach
    fun setup() {
        // Setup Gson with custom serializers
        gson = GsonBuilder()
            .registerTypeAdapter(RequestPart::class.java, RequestPartSerializer())
            .registerTypeAdapter(RequestPart::class.java, RequestPartDeserializer())
            .create()
    }
    
    @Test
    fun `should create GeminiApiClient instance successfully`() {
        // When
        val apiClient = GeminiApiClient()
        
        // Then
        assertNotNull(apiClient)
    }
    
    @Test
    fun `should create GeminiApiException with correct properties`() {
        // Given
        val code = 400
        val message = "Bad request"
        val geminiError = GeminiError(code = 400, message = "Invalid input", status = "INVALID_ARGUMENT")
        val cause = RuntimeException("Test cause")
        
        // When
        val exception = GeminiApiException(code, message, geminiError, cause)
        
        // Then
        assertEquals(code, exception.code)
        assertEquals(message, exception.message)
        assertEquals(geminiError, exception.geminiError)
        assertEquals(cause, exception.cause)
    }
    
    @Test
    fun `should detect rate limit errors correctly`() {
        // Given
        val rateLimitException = GeminiApiException(429, "Rate limited")
        val geminiRateLimitError = GeminiError(code = 429, message = "Too many requests", status = "RESOURCE_EXHAUSTED")
        val geminiRateLimitException = GeminiApiException(200, "OK", geminiRateLimitError)
        val normalException = GeminiApiException(400, "Bad request")
        
        // When & Then
        assertTrue(rateLimitException.isRateLimitError())
        assertTrue(geminiRateLimitException.isRateLimitError())
        assertFalse(normalException.isRateLimitError())
    }
    
    @Test
    fun `should detect quota exceeded errors correctly`() {
        // Given
        val quotaError = GeminiError(code = 429, message = "Quota exceeded", status = "RESOURCE_EXHAUSTED")
        val quotaException = GeminiApiException(429, "Quota exceeded", quotaError)
        val messageQuotaError = GeminiError(code = 400, message = "API quota limit reached", status = "INVALID_ARGUMENT")
        val messageQuotaException = GeminiApiException(400, "Bad request", messageQuotaError)
        val normalException = GeminiApiException(400, "Bad request")
        
        // When & Then
        assertTrue(quotaException.isQuotaExceededError())
        assertTrue(messageQuotaException.isQuotaExceededError())
        assertFalse(normalException.isQuotaExceededError())
    }
    
    @Test
    fun `should detect invalid API key errors correctly`() {
        // Given
        val unauthorizedException = GeminiApiException(401, "Unauthorized")
        val forbiddenException = GeminiApiException(403, "Forbidden")
        val unauthenticatedError = GeminiError(code = 401, message = "Invalid API key", status = "UNAUTHENTICATED")
        val unauthenticatedException = GeminiApiException(200, "OK", unauthenticatedError)
        val permissionError = GeminiError(code = 403, message = "Permission denied", status = "PERMISSION_DENIED")
        val permissionException = GeminiApiException(200, "OK", permissionError)
        val normalException = GeminiApiException(400, "Bad request")
        
        // When & Then
        assertTrue(unauthorizedException.isInvalidApiKeyError())
        assertTrue(forbiddenException.isInvalidApiKeyError())
        assertTrue(unauthenticatedException.isInvalidApiKeyError())
        assertTrue(permissionException.isInvalidApiKeyError())
        assertFalse(normalException.isInvalidApiKeyError())
    }
    
    @Test
    fun `should serialize and deserialize GeminiRequest correctly`() {
        // Given
        val request = GeminiRequest(
            contents = listOf(
                RequestContent(
                    parts = listOf(
                        RequestPart.TextPart("Analyze this lansones fruit for diseases"),
                        RequestPart.InlineDataPart(
                            InlineData(
                                mimeType = "image/jpeg",
                                data = "base64encodedimagedata"
                            )
                        )
                    )
                )
            ),
            generationConfig = GenerationConfig(
                temperature = 0.1f,
                topK = 32,
                topP = 1.0f,
                maxOutputTokens = 2048
            )
        )
        
        // When
        val json = gson.toJson(request)
        val deserializedRequest = gson.fromJson(json, GeminiRequest::class.java)
        
        // Then
        assertNotNull(json)
        assertNotNull(deserializedRequest)
        assertEquals(request.contents.size, deserializedRequest.contents.size)
        assertEquals(request.generationConfig?.temperature, deserializedRequest.generationConfig?.temperature)
        assertEquals(request.generationConfig?.topK, deserializedRequest.generationConfig?.topK)
    }
    
    @Test
    fun `should serialize and deserialize GeminiResponse correctly`() {
        // Given
        val response = GeminiResponse(
            candidates = listOf(
                Candidate(
                    content = Content(
                        parts = listOf(
                            Part("This is a healthy lansones fruit.")
                        ),
                        role = "model"
                    ),
                    finishReason = "STOP",
                    index = 0
                )
            )
        )
        
        // When
        val json = gson.toJson(response)
        val deserializedResponse = gson.fromJson(json, GeminiResponse::class.java)
        
        // Then
        assertNotNull(json)
        assertNotNull(deserializedResponse)
        assertEquals(response.candidates.size, deserializedResponse.candidates.size)
        assertEquals(response.candidates[0].content.parts[0].text, 
                    deserializedResponse.candidates[0].content.parts[0].text)
        assertEquals(response.candidates[0].finishReason, deserializedResponse.candidates[0].finishReason)
    }
}