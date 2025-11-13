package com.ml.lansonesscan.data.remote.api

import com.ml.lansonesscan.data.remote.dto.*
import com.ml.lansonesscan.util.Base64Encoder
import com.ml.lansonesscan.util.AndroidBase64Encoder

/**
 * Builder class for creating Gemini API requests
 */
class GeminiRequestBuilder(
    private val base64Encoder: Base64Encoder = AndroidBase64Encoder()
) {
    
    companion object {
        // Minimal safety settings - matching your working tomato app approach
        private val DEFAULT_SAFETY_SETTINGS = listOf(
            SafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_ONLY_HIGH"),
            SafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_ONLY_HIGH"),
            SafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_ONLY_HIGH"),
            SafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_ONLY_HIGH")
        )
        
        // Optimized for speed and consistency - similar to your tomato app
        private val DEFAULT_GENERATION_CONFIG = GenerationConfig(
            temperature = 0.15f,  // Low for deterministic, consistent results
            topK = 1,             // Most focused sampling
            topP = 0.8f,          // Balanced probability
            maxOutputTokens = 2048  // Enough for comprehensive analysis
        )
    }
    
    /**
     * Creates a request for image analysis with text prompt
     */
    fun createImageAnalysisRequest(
        imageBytes: ByteArray,
        mimeType: String,
        prompt: String
    ): GeminiRequest {
        val base64Image = base64Encoder.encodeToString(imageBytes)
        
        val parts = listOf(
            RequestPart.TextPart(prompt),
            RequestPart.InlineDataPart(
                InlineData(
                    mimeType = mimeType,
                    data = base64Image
                )
            )
        )
        
        return GeminiRequest(
            contents = listOf(RequestContent(parts)),
            generationConfig = DEFAULT_GENERATION_CONFIG,
            safetySettings = DEFAULT_SAFETY_SETTINGS
        )
    }
    
    /**
     * Creates a request with only text prompt (for testing)
     */
    fun createTextOnlyRequest(prompt: String): GeminiRequest {
        val parts = listOf(RequestPart.TextPart(prompt))
        
        return GeminiRequest(
            contents = listOf(RequestContent(parts)),
            generationConfig = DEFAULT_GENERATION_CONFIG,
            safetySettings = DEFAULT_SAFETY_SETTINGS
        )
    }
    
    /**
     * Creates a request with custom generation config
     */
    fun createCustomRequest(
        imageBytes: ByteArray,
        mimeType: String,
        prompt: String,
        generationConfig: GenerationConfig,
        safetySettings: List<SafetySetting>? = null
    ): GeminiRequest {
        val base64Image = base64Encoder.encodeToString(imageBytes)
        
        val parts = listOf(
            RequestPart.TextPart(prompt),
            RequestPart.InlineDataPart(
                InlineData(
                    mimeType = mimeType,
                    data = base64Image
                )
            )
        )
        
        return GeminiRequest(
            contents = listOf(RequestContent(parts)),
            generationConfig = generationConfig,
            safetySettings = safetySettings ?: DEFAULT_SAFETY_SETTINGS
        )
    }
}