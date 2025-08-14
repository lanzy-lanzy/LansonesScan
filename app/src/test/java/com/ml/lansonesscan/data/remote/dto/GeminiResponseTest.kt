package com.ml.lansonesscan.data.remote.dto

import com.google.gson.Gson
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class GeminiResponseTest {
    
    private val gson = Gson()
    
    @Test
    fun `should deserialize valid GeminiResponse JSON`() {
        // Given
        val json = """
            {
                "candidates": [
                    {
                        "content": {
                            "parts": [
                                {
                                    "text": "This is a healthy lansones fruit with no visible diseases."
                                }
                            ],
                            "role": "model"
                        },
                        "finishReason": "STOP",
                        "index": 0,
                        "safetyRatings": [
                            {
                                "category": "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                                "probability": "NEGLIGIBLE"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        
        // When
        val response = gson.fromJson(json, GeminiResponse::class.java)
        
        // Then
        assertNotNull(response)
        assertEquals(1, response.candidates.size)
        
        val candidate = response.candidates[0]
        assertEquals("STOP", candidate.finishReason)
        assertEquals(0, candidate.index)
        assertEquals(1, candidate.content.parts.size)
        assertEquals("This is a healthy lansones fruit with no visible diseases.", 
                    candidate.content.parts[0].text)
        assertEquals("model", candidate.content.role)
        
        assertNotNull(candidate.safetyRatings)
        assertEquals(1, candidate.safetyRatings!!.size)
        assertEquals("HARM_CATEGORY_SEXUALLY_EXPLICIT", candidate.safetyRatings[0].category)
        assertEquals("NEGLIGIBLE", candidate.safetyRatings[0].probability)
    }
    
    @Test
    fun `should deserialize minimal GeminiResponse JSON`() {
        // Given
        val json = """
            {
                "candidates": [
                    {
                        "content": {
                            "parts": [
                                {
                                    "text": "Analysis result"
                                }
                            ]
                        }
                    }
                ]
            }
        """.trimIndent()
        
        // When
        val response = gson.fromJson(json, GeminiResponse::class.java)
        
        // Then
        assertNotNull(response)
        assertEquals(1, response.candidates.size)
        
        val candidate = response.candidates[0]
        assertNull(candidate.finishReason)
        assertNull(candidate.index)
        assertNull(candidate.safetyRatings)
        assertEquals(1, candidate.content.parts.size)
        assertEquals("Analysis result", candidate.content.parts[0].text)
    }
    
    @Test
    fun `should serialize GeminiResponse to JSON`() {
        // Given
        val response = GeminiResponse(
            candidates = listOf(
                Candidate(
                    content = Content(
                        parts = listOf(Part("Test response")),
                        role = "model"
                    ),
                    finishReason = "STOP",
                    index = 0
                )
            )
        )
        
        // When
        val json = gson.toJson(response)
        
        // Then
        assertNotNull(json)
        assertTrue(json.contains("\"candidates\""))
        assertTrue(json.contains("\"content\""))
        assertTrue(json.contains("\"parts\""))
        assertTrue(json.contains("\"text\":\"Test response\""))
        assertTrue(json.contains("\"role\":\"model\""))
        assertTrue(json.contains("\"finishReason\":\"STOP\""))
        assertTrue(json.contains("\"index\":0"))
    }
    
    @Test
    fun `should handle empty candidates list`() {
        // Given
        val json = """
            {
                "candidates": []
            }
        """.trimIndent()
        
        // When
        val response = gson.fromJson(json, GeminiResponse::class.java)
        
        // Then
        assertNotNull(response)
        assertTrue(response.candidates.isEmpty())
    }
    
    @Test
    fun `should handle multiple candidates`() {
        // Given
        val json = """
            {
                "candidates": [
                    {
                        "content": {
                            "parts": [
                                {
                                    "text": "First candidate response"
                                }
                            ]
                        },
                        "index": 0
                    },
                    {
                        "content": {
                            "parts": [
                                {
                                    "text": "Second candidate response"
                                }
                            ]
                        },
                        "index": 1
                    }
                ]
            }
        """.trimIndent()
        
        // When
        val response = gson.fromJson(json, GeminiResponse::class.java)
        
        // Then
        assertNotNull(response)
        assertEquals(2, response.candidates.size)
        assertEquals("First candidate response", response.candidates[0].content.parts[0].text)
        assertEquals("Second candidate response", response.candidates[1].content.parts[0].text)
        assertEquals(0, response.candidates[0].index)
        assertEquals(1, response.candidates[1].index)
    }
}