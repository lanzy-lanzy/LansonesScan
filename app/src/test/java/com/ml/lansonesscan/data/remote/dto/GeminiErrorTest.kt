package com.ml.lansonesscan.data.remote.dto

import com.google.gson.Gson
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class GeminiErrorTest {
    
    private val gson = Gson()
    
    @Test
    fun `should deserialize error response with full details`() {
        // Given
        val json = """
            {
                "error": {
                    "code": 400,
                    "message": "Invalid request format",
                    "status": "INVALID_ARGUMENT",
                    "details": [
                        {
                            "@type": "type.googleapis.com/google.rpc.BadRequest",
                            "reason": "INVALID_FORMAT",
                            "domain": "googleapis.com",
                            "metadata": {
                                "field": "contents"
                            }
                        }
                    ]
                }
            }
        """.trimIndent()
        
        // When
        val errorResponse = gson.fromJson(json, GeminiErrorResponse::class.java)
        
        // Then
        assertNotNull(errorResponse)
        assertNotNull(errorResponse.error)
        
        val error = errorResponse.error
        assertEquals(400, error.code)
        assertEquals("Invalid request format", error.message)
        assertEquals("INVALID_ARGUMENT", error.status)
        
        assertNotNull(error.details)
        assertEquals(1, error.details!!.size)
        
        val detail = error.details[0]
        assertEquals("type.googleapis.com/google.rpc.BadRequest", detail.type)
        assertEquals("INVALID_FORMAT", detail.reason)
        assertEquals("googleapis.com", detail.domain)
        assertNotNull(detail.metadata)
        assertEquals("contents", detail.metadata!!["field"])
    }
    
    @Test
    fun `should deserialize minimal error response`() {
        // Given
        val json = """
            {
                "error": {
                    "code": 401,
                    "message": "API key not valid"
                }
            }
        """.trimIndent()
        
        // When
        val errorResponse = gson.fromJson(json, GeminiErrorResponse::class.java)
        
        // Then
        assertNotNull(errorResponse)
        assertNotNull(errorResponse.error)
        
        val error = errorResponse.error
        assertEquals(401, error.code)
        assertEquals("API key not valid", error.message)
        assertNull(error.status)
        assertNull(error.details)
    }
    
    @Test
    fun `should deserialize rate limit error`() {
        // Given
        val json = """
            {
                "error": {
                    "code": 429,
                    "message": "Quota exceeded for requests per minute per project",
                    "status": "RESOURCE_EXHAUSTED",
                    "details": [
                        {
                            "@type": "type.googleapis.com/google.rpc.QuotaFailure",
                            "reason": "RATE_LIMIT_EXCEEDED",
                            "domain": "googleapis.com"
                        }
                    ]
                }
            }
        """.trimIndent()
        
        // When
        val errorResponse = gson.fromJson(json, GeminiErrorResponse::class.java)
        
        // Then
        assertNotNull(errorResponse)
        val error = errorResponse.error
        assertEquals(429, error.code)
        assertEquals("RESOURCE_EXHAUSTED", error.status)
        assertTrue(error.message.contains("Quota exceeded"))
        
        assertNotNull(error.details)
        assertEquals(1, error.details!!.size)
        assertEquals("RATE_LIMIT_EXCEEDED", error.details[0].reason)
    }
    
    @Test
    fun `should serialize error response`() {
        // Given
        val errorResponse = GeminiErrorResponse(
            error = GeminiError(
                code = 403,
                message = "Permission denied",
                status = "PERMISSION_DENIED"
            )
        )
        
        // When
        val json = gson.toJson(errorResponse)
        
        // Then
        assertNotNull(json)
        assertTrue(json.contains("\"error\""))
        assertTrue(json.contains("\"code\":403"))
        assertTrue(json.contains("\"message\":\"Permission denied\""))
        assertTrue(json.contains("\"status\":\"PERMISSION_DENIED\""))
    }
    
    @Test
    fun `should handle error with empty details list`() {
        // Given
        val json = """
            {
                "error": {
                    "code": 500,
                    "message": "Internal server error",
                    "status": "INTERNAL",
                    "details": []
                }
            }
        """.trimIndent()
        
        // When
        val errorResponse = gson.fromJson(json, GeminiErrorResponse::class.java)
        
        // Then
        assertNotNull(errorResponse)
        val error = errorResponse.error
        assertEquals(500, error.code)
        assertEquals("Internal server error", error.message)
        assertEquals("INTERNAL", error.status)
        assertNotNull(error.details)
        assertTrue(error.details!!.isEmpty())
    }
}