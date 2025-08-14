package com.ml.lansonesscan.data.remote.api

import com.ml.lansonesscan.data.remote.dto.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class GeminiRequestBuilderTest {
    
    private lateinit var requestBuilder: GeminiRequestBuilder
    
    @BeforeEach
    fun setup() {
        requestBuilder = GeminiRequestBuilder()
    }
    
    @Test
    fun `should create image analysis request with correct structure`() {
        // Given
        val imageBytes = "test image data".toByteArray()
        val mimeType = "image/jpeg"
        val prompt = "Analyze this lansones fruit for diseases"
        
        // When
        val request = requestBuilder.createImageAnalysisRequest(imageBytes, mimeType, prompt)
        
        // Then
        assertNotNull(request)
        assertEquals(1, request.contents.size)
        assertEquals(2, request.contents[0].parts.size)
        
        // Verify text part
        val textPart = request.contents[0].parts[0]
        assertTrue(textPart is RequestPart.TextPart)
        assertEquals(prompt, (textPart as RequestPart.TextPart).text)
        
        // Verify image part
        val imagePart = request.contents[0].parts[1]
        assertTrue(imagePart is RequestPart.InlineDataPart)
        val inlineData = (imagePart as RequestPart.InlineDataPart).inlineData
        assertEquals(mimeType, inlineData.mimeType)
        assertNotNull(inlineData.data)
        assertTrue(inlineData.data.isNotEmpty())
        
        // Verify generation config
        assertNotNull(request.generationConfig)
        assertEquals(0.1f, request.generationConfig!!.temperature)
        assertEquals(32, request.generationConfig.topK)
        assertEquals(1.0f, request.generationConfig.topP)
        assertEquals(2048, request.generationConfig.maxOutputTokens)
        
        // Verify safety settings
        assertNotNull(request.safetySettings)
        assertEquals(4, request.safetySettings!!.size)
        assertTrue(request.safetySettings.any { it.category == "HARM_CATEGORY_HARASSMENT" })
        assertTrue(request.safetySettings.any { it.category == "HARM_CATEGORY_HATE_SPEECH" })
        assertTrue(request.safetySettings.any { it.category == "HARM_CATEGORY_SEXUALLY_EXPLICIT" })
        assertTrue(request.safetySettings.any { it.category == "HARM_CATEGORY_DANGEROUS_CONTENT" })
        assertTrue(request.safetySettings.all { it.threshold == "BLOCK_NONE" })
    }
    
    @Test
    fun `should create text-only request with correct structure`() {
        // Given
        val prompt = "What are common diseases in lansones fruit?"
        
        // When
        val request = requestBuilder.createTextOnlyRequest(prompt)
        
        // Then
        assertNotNull(request)
        assertEquals(1, request.contents.size)
        assertEquals(1, request.contents[0].parts.size)
        
        // Verify text part
        val textPart = request.contents[0].parts[0]
        assertTrue(textPart is RequestPart.TextPart)
        assertEquals(prompt, (textPart as RequestPart.TextPart).text)
        
        // Verify generation config is present
        assertNotNull(request.generationConfig)
        
        // Verify safety settings are present
        assertNotNull(request.safetySettings)
        assertEquals(4, request.safetySettings!!.size)
    }
    
    @Test
    fun `should create custom request with provided config`() {
        // Given
        val imageBytes = "custom image data".toByteArray()
        val mimeType = "image/png"
        val prompt = "Custom analysis prompt"
        val customConfig = GenerationConfig(
            temperature = 0.8f,
            topK = 50,
            topP = 0.9f,
            maxOutputTokens = 1024
        )
        val customSafetySettings = listOf(
            SafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_MEDIUM_AND_ABOVE")
        )
        
        // When
        val request = requestBuilder.createCustomRequest(
            imageBytes, mimeType, prompt, customConfig, customSafetySettings
        )
        
        // Then
        assertNotNull(request)
        assertEquals(1, request.contents.size)
        assertEquals(2, request.contents[0].parts.size)
        
        // Verify custom generation config
        assertNotNull(request.generationConfig)
        assertEquals(0.8f, request.generationConfig!!.temperature)
        assertEquals(50, request.generationConfig.topK)
        assertEquals(0.9f, request.generationConfig.topP)
        assertEquals(1024, request.generationConfig.maxOutputTokens)
        
        // Verify custom safety settings
        assertNotNull(request.safetySettings)
        assertEquals(1, request.safetySettings!!.size)
        assertEquals("HARM_CATEGORY_HARASSMENT", request.safetySettings[0].category)
        assertEquals("BLOCK_MEDIUM_AND_ABOVE", request.safetySettings[0].threshold)
    }
    
    @Test
    fun `should create custom request with default safety settings when null provided`() {
        // Given
        val imageBytes = "test data".toByteArray()
        val mimeType = "image/jpeg"
        val prompt = "Test prompt"
        val customConfig = GenerationConfig(temperature = 0.5f)
        
        // When
        val request = requestBuilder.createCustomRequest(
            imageBytes, mimeType, prompt, customConfig, null
        )
        
        // Then
        assertNotNull(request)
        assertNotNull(request.safetySettings)
        assertEquals(4, request.safetySettings!!.size)
        assertTrue(request.safetySettings.all { it.threshold == "BLOCK_NONE" })
    }
    
    @Test
    fun `should handle different image mime types`() {
        // Given
        val imageBytes = "image data".toByteArray()
        val prompt = "Analyze image"
        val mimeTypes = listOf("image/jpeg", "image/png", "image/webp", "image/gif")
        
        // When & Then
        mimeTypes.forEach { mimeType ->
            val request = requestBuilder.createImageAnalysisRequest(imageBytes, mimeType, prompt)
            
            assertNotNull(request)
            val imagePart = request.contents[0].parts[1] as RequestPart.InlineDataPart
            assertEquals(mimeType, imagePart.inlineData.mimeType)
        }
    }
    
    @Test
    fun `should handle empty image bytes`() {
        // Given
        val imageBytes = ByteArray(0)
        val mimeType = "image/jpeg"
        val prompt = "Analyze empty image"
        
        // When
        val request = requestBuilder.createImageAnalysisRequest(imageBytes, mimeType, prompt)
        
        // Then
        assertNotNull(request)
        val imagePart = request.contents[0].parts[1] as RequestPart.InlineDataPart
        assertEquals("", imagePart.inlineData.data) // Empty base64 string
    }
    
    @Test
    fun `should handle large image data`() {
        // Given
        val imageBytes = ByteArray(1024 * 1024) { it.toByte() } // 1MB of test data
        val mimeType = "image/jpeg"
        val prompt = "Analyze large image"
        
        // When
        val request = requestBuilder.createImageAnalysisRequest(imageBytes, mimeType, prompt)
        
        // Then
        assertNotNull(request)
        val imagePart = request.contents[0].parts[1] as RequestPart.InlineDataPart
        assertTrue(imagePart.inlineData.data.isNotEmpty())
        assertEquals(mimeType, imagePart.inlineData.mimeType)
    }
}