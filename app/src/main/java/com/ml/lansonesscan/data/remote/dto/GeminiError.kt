package com.ml.lansonesscan.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Error response model for Gemini API
 */
data class GeminiErrorResponse(
    @SerializedName("error")
    val error: GeminiError
)

data class GeminiError(
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("details")
    val details: List<ErrorDetail>? = null
)

data class ErrorDetail(
    @SerializedName("@type")
    val type: String,
    @SerializedName("reason")
    val reason: String? = null,
    @SerializedName("domain")
    val domain: String? = null,
    @SerializedName("metadata")
    val metadata: Map<String, String>? = null
)