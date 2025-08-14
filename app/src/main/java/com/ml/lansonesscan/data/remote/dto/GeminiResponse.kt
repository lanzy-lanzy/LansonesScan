package com.ml.lansonesscan.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response model for Gemini API
 */
data class GeminiResponse(
    @SerializedName("candidates")
    val candidates: List<Candidate>
)

data class Candidate(
    @SerializedName("content")
    val content: Content,
    @SerializedName("finishReason")
    val finishReason: String? = null,
    @SerializedName("index")
    val index: Int? = null,
    @SerializedName("safetyRatings")
    val safetyRatings: List<SafetyRating>? = null
)

data class Content(
    @SerializedName("parts")
    val parts: List<Part>,
    @SerializedName("role")
    val role: String? = null
)

data class Part(
    @SerializedName("text")
    val text: String
)

data class SafetyRating(
    @SerializedName("category")
    val category: String,
    @SerializedName("probability")
    val probability: String
)