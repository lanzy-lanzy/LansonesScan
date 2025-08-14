package com.ml.lansonesscan.data.remote.dto

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class GeminiRequestTest {
    
    private lateinit var gson: Gson
    
    @BeforeEach
    fun setup() {
        gson = GsonBuilder()
            .registerTypeAdapter(RequestPart::class.java, RequestPartSerializer())
            .registerTypeAdapter(RequestPart::class.java, RequestPartDeserializer())
            .create()
    }
    
    @Test
    fun `should serialize text-only request`() {
        // Given
        val request = GeminiRequest(
            contents = listOf(
                RequestContent(
                    parts = listOf(
                        RequestPart.TextPart("Analyze this image for diseases")
                    )
                )
            )
        )
        
        // When
        val json = gson.toJson(request)
        
        // Then
        assertNotNull(json)
        assertTrue(json.contains("\"contents\""))
        assertTrue(json.contains("\"parts\""))
        assertTrue(json.contains("\"text\":\"Analyze this image for diseases\""))
        assertFalse(json.contains("\"inlineData\""))
    }
    
    @Test
    fun `should serialize image request with inline data`() {
        // Given
        val request = GeminiRequest(
            contents = listOf(
                RequestContent(
                    parts = listOf(
                        RequestPart.TextPart("Analyze this lansones fruit"),
                        RequestPart.InlineDataPart(
                            InlineData(
                                mimeType = "image/jpeg",
                                data = "base64encodedimagedata"
                            )
                        )
                    )
                )
            )
        )
        
        // When
        val json = gson.toJson(request)
        
        // Then
        assertNotNull(json)
        assertTrue(json.contains("\"text\":\"Analyze this lansones fruit\""))
        assertTrue(json.contains("\"inlineData\""))
        assertTrue(json.contains("\"mimeType\":\"image/jpeg\""))
        assertTrue(json.contains("\"data\":\"base64encodedimagedata\""))
    }
    
    @Test
    fun `should serialize request with generation config`() {
        // Given
        val request = GeminiRequest(
            contents = listOf(
                RequestContent(
                    parts = listOf(RequestPart.TextPart("Test prompt"))
                )
            ),
            generationConfig = GenerationConfig(
                temperature = 0.5f,
                topK = 40,
                topP = 0.95f,
                maxOutputTokens = 1024
            )
        )
        
        // When
        val json = gson.toJson(request)
        
        // Then
        assertNotNull(json)
        assertTrue(json.contains("\"generationConfig\""))
        assertTrue(json.contains("\"temperature\":0.5"))
        assertTrue(json.contains("\"topK\":40"))
        assertTrue(json.contains("\"topP\":0.95"))
        assertTrue(json.contains("\"maxOutputTokens\":1024"))
    }
    
    @Test
    fun `should serialize request with safety settings`() {
        // Given
        val request = GeminiRequest(
            contents = listOf(
                RequestContent(
                    parts = listOf(RequestPart.TextPart("Test prompt"))
                )
            ),
            safetySettings = listOf(
                SafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_MEDIUM_AND_ABOVE"),
                SafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_NONE")
            )
        )
        
        // When
        val json = gson.toJson(request)
        
        // Then
        assertNotNull(json)
        assertTrue(json.contains("\"safetySettings\""))
        assertTrue(json.contains("\"category\":\"HARM_CATEGORY_HARASSMENT\""))
        assertTrue(json.contains("\"threshold\":\"BLOCK_MEDIUM_AND_ABOVE\""))
        assertTrue(json.contains("\"category\":\"HARM_CATEGORY_DANGEROUS_CONTENT\""))
        assertTrue(json.contains("\"threshold\":\"BLOCK_NONE\""))
    }
    
    @Test
    fun `should deserialize text-only request`() {
        // Given
        val json = """
            {
                "contents": [
                    {
                        "parts": [
                            {
                                "text": "Analyze this image"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        
        // When
        val request = gson.fromJson(json, GeminiRequest::class.java)
        
        // Then
        assertNotNull(request)
        assertEquals(1, request.contents.size)
        assertEquals(1, request.contents[0].parts.size)
        
        val part = request.contents[0].parts[0]
        assertTrue(part is RequestPart.TextPart)
        assertEquals("Analyze this image", (part as RequestPart.TextPart).text)
    }
    
    @Test
    fun `should deserialize image request with inline data`() {
        // Given
        val json = """
            {
                "contents": [
                    {
                        "parts": [
                            {
                                "text": "Analyze this fruit"
                            },
                            {
                                "inlineData": {
                                    "mimeType": "image/png",
                                    "data": "testbase64data"
                                }
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        
        // When
        val request = gson.fromJson(json, GeminiRequest::class.java)
        
        // Then
        assertNotNull(request)
        assertEquals(1, request.contents.size)
        assertEquals(2, request.contents[0].parts.size)
        
        val textPart = request.contents[0].parts[0]
        assertTrue(textPart is RequestPart.TextPart)
        assertEquals("Analyze this fruit", (textPart as RequestPart.TextPart).text)
        
        val imagePart = request.contents[0].parts[1]
        assertTrue(imagePart is RequestPart.InlineDataPart)
        val inlineData = (imagePart as RequestPart.InlineDataPart).inlineData
        assertEquals("image/png", inlineData.mimeType)
        assertEquals("testbase64data", inlineData.data)
    }
    
    @Test
    fun `should handle mixed content types in single request`() {
        // Given
        val request = GeminiRequest(
            contents = listOf(
                RequestContent(
                    parts = listOf(
                        RequestPart.TextPart("First text part"),
                        RequestPart.InlineDataPart(
                            InlineData("image/jpeg", "imagedata1")
                        ),
                        RequestPart.TextPart("Second text part"),
                        RequestPart.InlineDataPart(
                            InlineData("image/png", "imagedata2")
                        )
                    )
                )
            )
        )
        
        // When
        val json = gson.toJson(request)
        val deserializedRequest = gson.fromJson(json, GeminiRequest::class.java)
        
        // Then
        assertNotNull(deserializedRequest)
        assertEquals(1, deserializedRequest.contents.size)
        assertEquals(4, deserializedRequest.contents[0].parts.size)
        
        // Verify order and types are preserved
        assertTrue(deserializedRequest.contents[0].parts[0] is RequestPart.TextPart)
        assertTrue(deserializedRequest.contents[0].parts[1] is RequestPart.InlineDataPart)
        assertTrue(deserializedRequest.contents[0].parts[2] is RequestPart.TextPart)
        assertTrue(deserializedRequest.contents[0].parts[3] is RequestPart.InlineDataPart)
    }
}