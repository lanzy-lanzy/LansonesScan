package com.ml.lansonesscan.data.remote.api

import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.ml.lansonesscan.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/**
 * Simplified Gemini API client using official Google AI SDK
 * Based on TomatoScan implementation - fast and reliable
 * Much faster and more reliable than raw OkHttp approach
 */
class GeminiSdkClient {

    companion object {
        private const val TAG = "GeminiSdkClient"
        // Use gemini-2.0-flash-exp for best balance of speed and accuracy
        private const val MODEL_NAME = "gemini-2.0-flash-exp"
        // 512px is a good balance for quality and performance. 10px is too small for meaningful analysis.
        private const val MAX_IMAGE_SIZE = 1024
    }

    private val generativeModel by lazy {
        try {
            GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = BuildConfig.GEMINI_API_KEY,
                generationConfig = generationConfig {
                    // Optimized settings for speed and consistency
                    temperature = 0.2f
                    topK = 5
                    topP = 0.85f
                    // Reduced for faster response, assuming JSON output is not excessively long
                    maxOutputTokens = 1024
                }
            ).also {
                Log.d(TAG, "✓ Gemini SDK initialized successfully for model: $MODEL_NAME")
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to initialize Gemini model", e)
            null
        }
    }

    /**
     * Analyzes an image with a text prompt using the official SDK.
     * Ensures optimal performance by resizing large images and validating responses.
     *
     * @param bitmap The input image.
     * @param prompt The text prompt for analysis.
     * @return A Result containing the raw text response or an exception.
     */
    suspend fun analyzeImage(bitmap: Bitmap, prompt: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (generativeModel == null) {
                    return@withContext Result.failure(
                        IllegalStateException("Gemini API not configured. Check API key.")
                    )
                }

                Log.d(TAG, "Starting image analysis...")
                val processedBitmap = resizeImage(bitmap)
                
                val inputContent = content {
                    image(processedBitmap)
                    text(prompt)
                }

                Log.d(TAG, "Sending request to Gemini API ($MODEL_NAME)...")
                val response = generativeModel!!.generateContent(inputContent)
                val responseText = response.text?.trim()

                if (responseText.isNullOrBlank() || !isValidResponse(responseText)) {
                    Log.e(TAG, "Received invalid or empty response from API: '$responseText'")
                    return@withContext Result.failure(
                        Exception("Invalid or empty response from Gemini API. Response: ${responseText?.take(100)}...")
                    )
                }

                Log.d(TAG, "✓ Analysis successful. Response length: ${responseText.length}")
                Result.success(responseText)

            } catch (e: Exception) {
                Log.e(TAG, "✗ Error during Gemini API request", e)
                // Create a user-friendly error message
                val userFriendlyMessage = convertExceptionToUserMessage(e)
                Result.failure(Exception(userFriendlyMessage, e))
            }
        }
    }

    /**
     * Converts API exceptions to user-friendly messages
     */
    private fun convertExceptionToUserMessage(e: Exception): String {
        val errorMessage = e.message ?: "Unknown error"
        
        // Handle specific error patterns
        return when {
            // API overload errors
            errorMessage.contains("503", ignoreCase = true) || 
            errorMessage.contains("overload", ignoreCase = true) ||
            errorMessage.contains("UNAVAILABLE", ignoreCase = true) -> {
                "The AI model is currently overloaded. Please try again in a few moments."
            }
            // Network errors
            errorMessage.contains("network", ignoreCase = true) ||
            errorMessage.contains("timeout", ignoreCase = true) ||
            errorMessage.contains("unreachable", ignoreCase = true) -> {
                "Network connection error. Please check your internet connection and try again."
            }
            // Authentication errors
            errorMessage.contains("401", ignoreCase = true) ||
            errorMessage.contains("403", ignoreCase = true) ||
            errorMessage.contains("unauthenticated", ignoreCase = true) ||
            errorMessage.contains("permission denied", ignoreCase = true) -> {
                "API authentication failed. Please check your API key configuration."
            }
            // Quota/rate limit errors
            errorMessage.contains("429", ignoreCase = true) ||
            errorMessage.contains("quota", ignoreCase = true) ||
            errorMessage.contains("rate limit", ignoreCase = true) -> {
                "API rate limit exceeded. Please wait a moment and try again."
            }
            // Invalid API key
            errorMessage.contains("invalid", ignoreCase = true) &&
            errorMessage.contains("key", ignoreCase = true) -> {
                "Invalid API key. Please verify your configuration."
            }
            // Serialization/deserialization errors (suppress internal details)
            errorMessage.contains("MissingField", ignoreCase = true) ||
            errorMessage.contains("serialization", ignoreCase = true) ||
            errorMessage.contains("deserialization", ignoreCase = true) -> {
                "The AI service encountered an error processing the response. Please try again."
            }
            // Generic fallback
            else -> {
                "Analysis failed: Please try again. If the problem persists, contact support."
            }
        }
    }

    /**
     * Resizes the bitmap if its dimensions exceed MAX_IMAGE_SIZE to optimize performance.
     */
    private fun resizeImage(bitmap: Bitmap): Bitmap {
        val (width, height) = bitmap.width to bitmap.height
        if (width <= MAX_IMAGE_SIZE && height <= MAX_IMAGE_SIZE) {
            Log.d(TAG, "Image size ($width x $height) is within limits. No resize needed.")
            return bitmap
        }

        val aspectRatio = width.toFloat() / height.toFloat()
        val (newWidth, newHeight) = if (width > height) {
            MAX_IMAGE_SIZE to (MAX_IMAGE_SIZE / aspectRatio).toInt()
        } else {
            (MAX_IMAGE_SIZE * aspectRatio).toInt() to MAX_IMAGE_SIZE
        }

        Log.d(TAG, "Resizing image from $width x $height to $newWidth x $newHeight")
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * Validates that the response contains valid content.
     */
    private fun isValidResponse(response: String): Boolean {
        // A more flexible validation that checks if the response contains meaningful content
        return try {
            if (response.isNullOrBlank()) {
                Log.w(TAG, "Response is null or blank")
                return false
            }
            
            // Check if response contains JSON-like content or meaningful text
            val trimmedResponse = response.trim()
            
            // If it's a JSON object, validate it
            if (trimmedResponse.startsWith("{") && trimmedResponse.endsWith("}")) {
                JSONObject(trimmedResponse)
                return true
            }
            
            // If it's wrapped in code blocks, try to extract and validate JSON
            if (trimmedResponse.contains("```")) {
                // This is a valid case - the extractJsonFromText function in AnalysisService will handle it
                return true
            }
            
            // If it contains JSON-like content anywhere in the text, consider it valid
            if (trimmedResponse.contains("\"isLansones\"") || 
                trimmedResponse.contains("\"diseaseDetected\"") || 
                trimmedResponse.contains("\"observations\"")) {
                return true
            }
            
            // If it's just text content, consider it valid as long as it's not empty
            return trimmedResponse.length > 10 // Arbitrary minimum length for meaningful content
        } catch (e: Exception) {
            Log.e(TAG, "Response validation failed.", e)
            false
        }
    }
}
