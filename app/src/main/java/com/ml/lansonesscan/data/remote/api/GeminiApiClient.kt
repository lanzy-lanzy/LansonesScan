package com.ml.lansonesscan.data.remote.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ml.lansonesscan.BuildConfig
import com.ml.lansonesscan.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * API client for Google Gemini API integration
 */
class GeminiApiClient {
    
    companion object {
        private const val TAG = "GeminiApiClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com"
        private const val API_VERSION = "v1beta"
        private const val MODEL_NAME = "gemini-1.5-flash"
        private const val TIMEOUT_SECONDS = 60L
        
        // Content types
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
    
    private val apiKey: String = BuildConfig.GEMINI_API_KEY.also { key ->
        Log.d(TAG, "API Key loaded: ${if (key.isNotEmpty()) "✓ (${key.take(10)}...)" else "✗ EMPTY"}")
    }
    
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(RequestPart::class.java, RequestPartSerializer())
        .registerTypeAdapter(RequestPart::class.java, RequestPartDeserializer())
        .create()
    
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(createLoggingInterceptor())
        .addInterceptor(createApiKeyInterceptor())
        .build()
    
    /**
     * Analyzes an image using Gemini API
     */
    suspend fun analyzeImage(request: GeminiRequest): Result<GeminiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/$API_VERSION/models/$MODEL_NAME:generateContent"
                val requestBody = gson.toJson(request).toRequestBody(JSON_MEDIA_TYPE)
                
                val httpRequest = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()
                
                Log.d(TAG, "Making API request to: $url")
                
                val response = okHttpClient.newCall(httpRequest).execute()
                
                response.use { resp ->
                    val responseBody = resp.body?.string()
                    
                    if (resp.isSuccessful && responseBody != null) {
                        Log.d(TAG, "API request successful")
                        val geminiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)
                        Result.success(geminiResponse)
                    } else {
                        Log.e(TAG, "API request failed with code: ${resp.code}")
                        Log.e(TAG, "Response body: $responseBody")
                        Log.e(TAG, "Request URL: ${httpRequest.url}")
                        
                        val errorResponse = responseBody?.let { body ->
                            try {
                                gson.fromJson(body, GeminiErrorResponse::class.java)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to parse error response", e)
                                null
                            }
                        }
                        
                        val exception = GeminiApiException(
                            code = resp.code,
                            message = errorResponse?.error?.message ?: "API request failed: ${resp.message}",
                            geminiError = errorResponse?.error
                        )
                        Result.failure(exception)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error during API request", e)
                Result.failure(GeminiApiException(
                    code = -1,
                    message = "Network error: ${e.message}",
                    cause = e
                ))
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during API request", e)
                Result.failure(GeminiApiException(
                    code = -1,
                    message = "Unexpected error: ${e.message}",
                    cause = e
                ))
            }
        }
    }
    
    /**
     * Creates logging interceptor for debugging
     */
    private fun createLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            Log.d("$TAG-HTTP", message)
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }
    
    /**
     * Creates API key interceptor
     */
    private fun createApiKeyInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val urlWithApiKey = originalRequest.url.newBuilder()
                .addQueryParameter("key", apiKey)
                .build()
            
            val newRequest = originalRequest.newBuilder()
                .url(urlWithApiKey)
                .build()
            
            chain.proceed(newRequest)
        }
    }
}

/**
 * Custom exception for Gemini API errors
 */
class GeminiApiException(
    val code: Int,
    message: String,
    val geminiError: GeminiError? = null,
    cause: Throwable? = null
) : Exception(message, cause) {
    
    fun isRateLimitError(): Boolean = code == 429 || geminiError?.code == 429
    
    fun isQuotaExceededError(): Boolean = 
        geminiError?.status == "RESOURCE_EXHAUSTED" || 
        geminiError?.message?.contains("quota", ignoreCase = true) == true
    
    fun isInvalidApiKeyError(): Boolean = 
        code == 401 || code == 403 ||
        geminiError?.status == "UNAUTHENTICATED" ||
        geminiError?.status == "PERMISSION_DENIED"
}