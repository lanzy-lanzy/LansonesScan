package com.ml.lansonesscan.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request model for Gemini API
 */
data class GeminiRequest(
    @SerializedName("contents")
    val contents: List<RequestContent>,
    @SerializedName("generationConfig")
    val generationConfig: GenerationConfig? = null,
    @SerializedName("safetySettings")
    val safetySettings: List<SafetySetting>? = null
)

data class RequestContent(
    @SerializedName("parts")
    val parts: List<RequestPart>
)

sealed class RequestPart {
    data class TextPart(
        @SerializedName("text")
        val text: String
    ) : RequestPart()
    
    data class InlineDataPart(
        @SerializedName("inlineData")
        val inlineData: InlineData
    ) : RequestPart()
}

data class InlineData(
    @SerializedName("mimeType")
    val mimeType: String,
    @SerializedName("data")
    val data: String // Base64 encoded image data
)

data class GenerationConfig(
    @SerializedName("temperature")
    val temperature: Float? = null,
    @SerializedName("topK")
    val topK: Int? = null,
    @SerializedName("topP")
    val topP: Float? = null,
    @SerializedName("maxOutputTokens")
    val maxOutputTokens: Int? = null,
    @SerializedName("stopSequences")
    val stopSequences: List<String>? = null
)

data class SafetySetting(
    @SerializedName("category")
    val category: String,
    @SerializedName("threshold")
    val threshold: String
)