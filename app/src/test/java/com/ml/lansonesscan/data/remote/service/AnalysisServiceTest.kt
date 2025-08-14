package com.ml.lansonesscan.data.remote.service

import android.net.Uri
import com.ml.lansonesscan.data.remote.api.GeminiApiClient
import com.ml.lansonesscan.data.remote.api.GeminiApiException
import com.ml.lansonesscan.data.remote.api.GeminiRequestBuilder
import com.ml.lansonesscan.data.remote.dto.*
import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.util.TestBase64Encoder
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach

class AnalysisServiceTest {
    
    private lateinit var analysisService: AnalysisService
    private lateinit var mockApiClient: GeminiApiClient
    private lateinit var mockRequestBuilder: GeminiRequestBuilder
    private lateinit var mockUri: Uri
    
    @BeforeEach
    fun setup() {
        mockApiClient = mockk()
        mockRequestBuilder = mockk()
        mockUri = mockk()
        
        analysisService = AnalysisService(mockApiClient, mockRequestBuilder)
    }
    
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `should successfully analyze fruit image with disease detected`() = runTest {
        // Given
        val imageBytes = "test image data".toByteArray()
        val mimeType = "image/jpeg"
        val analysisType = AnalysisType.FRUIT
        
        val mockRequest = createMockRequest()
        val mockResponse = createMockResponseWithDisease()
        
        every { mockRequestBuilder.createImageAnalysisRequest(imageBytes, mimeType, any()) } returns mockRequest
        coEvery { mockApiClient.analyzeImage(mockRequest) } returns Result.success(mockResponse)
        
        val getImageBytes: suspend (Uri) -> ByteArray = { imageBytes }
        val getMimeType: (Uri) -> String = { mimeType }
        
        // When
        val result = analysisService.analyzeImage(mockUri, analysisType, getImageBytes, getMimeType)
        
        // Then
        assertTrue(result.isSuccess)
        val analysisResult = result.getOrNull()!!
        
        assertTrue(analysisResult.diseaseDetected)
        assertEquals("fruit", analysisResult.affectedPart)
        assertTrue(analysisResult.confidenceLevel > 0.0f)
        assertTrue(analysisResult.symptoms.isNotEmpty())
        assertTrue(analysisResult.recommendations.isNotEmpty())
        assertNotNull(analysisResult.rawResponse)
        
        verify { mockRequestBuilder.createImageAnalysisRequest(imageBytes, mimeType, any()) }
        coVerify { mockApiClient.analyzeImage(mockRequest) }
    }
    
    @Test
    fun `should successfully analyze leaf image with no disease`() = runTest {
        // Given
        val imageBytes = "test leaf image".toByteArray()
        val mimeType = "image/png"
        val analysisType = AnalysisType.LEAVES
        
        val mockRequest = createMockRequest()
        val mockResponse = createMockResponseHealthy()
        
        every { mockRequestBuilder.createImageAnalysisRequest(imageBytes, mimeType, any()) } returns mockRequest
        coEvery { mockApiClient.analyzeImage(mockRequest) } returns Result.success(mockResponse)
        
        val getImageBytes: suspend (Uri) -> ByteArray = { imageBytes }
        val getMimeType: (Uri) -> String = { mimeType }
        
        // When
        val result = analysisService.analyzeImage(mockUri, analysisType, getImageBytes, getMimeType)
        
        // Then
        assertTrue(result.isSuccess)
        val analysisResult = result.getOrNull()!!
        
        assertFalse(analysisResult.diseaseDetected)
        assertEquals("leaves", analysisResult.affectedPart)
        assertEquals("none", analysisResult.severity)
        assertTrue(analysisResult.recommendations.isNotEmpty())
        
        // Verify the correct prompt was used for leaves
        verify { 
            mockRequestBuilder.createImageAnalysisRequest(
                imageBytes, 
                mimeType, 
                match { prompt -> prompt.contains("leaf") && prompt.contains("lansones") }
            ) 
        }
    }
    
    @Test
    fun `should handle empty image data`() = runTest {
        // Given
        val emptyImageBytes = ByteArray(0)
        val mimeType = "image/jpeg"
        val analysisType = AnalysisType.FRUIT
        
        val getImageBytes: suspend (Uri) -> ByteArray = { emptyImageBytes }
        val getMimeType: (Uri) -> String = { mimeType }
        
        // When
        val result = analysisService.analyzeImage(mockUri, analysisType, getImageBytes, getMimeType)
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is AnalysisException)
        assertTrue(exception!!.message!!.contains("Image data is empty"))
    }
    
    @Test
    fun `should handle API client failure`() = runTest {
        // Given
        val imageBytes = "test image data".toByteArray()
        val mimeType = "image/jpeg"
        val analysisType = AnalysisType.FRUIT
        
        val mockRequest = createMockRequest()
        val apiException = GeminiApiException(500, "Server error")
        
        every { mockRequestBuilder.createImageAnalysisRequest(imageBytes, mimeType, any()) } returns mockRequest
        coEvery { mockApiClient.analyzeImage(mockRequest) } returns Result.failure(apiException)
        
        val getImageBytes: suspend (Uri) -> ByteArray = { imageBytes }
        val getMimeType: (Uri) -> String = { mimeType }
        
        // When
        val result = analysisService.analyzeImage(mockUri, analysisType, getImageBytes, getMimeType)
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is AnalysisException)
        assertTrue(exception!!.message!!.contains("API call failed"))
        assertEquals(apiException, exception.cause)
    }
    
    @Test
    fun `should handle empty API response candidates`() = runTest {
        // Given
        val imageBytes = "test image data".toByteArray()
        val mimeType = "image/jpeg"
        val analysisType = AnalysisType.FRUIT
        
        val mockRequest = createMockRequest()
        val emptyResponse = GeminiResponse(candidates = emptyList())
        
        every { mockRequestBuilder.createImageAnalysisRequest(imageBytes, mimeType, any()) } returns mockRequest
        coEvery { mockApiClient.analyzeImage(mockRequest) } returns Result.success(emptyResponse)
        
        val getImageBytes: suspend (Uri) -> ByteArray = { imageBytes }
        val getMimeType: (Uri) -> String = { mimeType }
        
        // When
        val result = analysisService.analyzeImage(mockUri, analysisType, getImageBytes, getMimeType)
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is AnalysisException)
        assertTrue(exception!!.message!!.contains("No analysis candidates returned"))
    }
    
    @Test
    fun `should handle unexpected exception during analysis`() = runTest {
        // Given
        val imageBytes = "test image data".toByteArray()
        val mimeType = "image/jpeg"
        val analysisType = AnalysisType.FRUIT
        
        val getImageBytes: suspend (Uri) -> ByteArray = { throw RuntimeException("Unexpected error") }
        val getMimeType: (Uri) -> String = { mimeType }
        
        // When
        val result = analysisService.analyzeImage(mockUri, analysisType, getImageBytes, getMimeType)
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is AnalysisException)
        assertTrue(exception!!.message!!.contains("Unexpected error during analysis"))
    }
    
    @Test
    fun `should use different prompts for fruit and leaf analysis`() = runTest {
        // Given
        val imageBytes = "test image data".toByteArray()
        val mimeType = "image/jpeg"
        
        val mockRequest = createMockRequest()
        val mockResponse = createMockResponseHealthy()
        
        every { mockRequestBuilder.createImageAnalysisRequest(any(), any(), any()) } returns mockRequest
        coEvery { mockApiClient.analyzeImage(any()) } returns Result.success(mockResponse)
        
        val getImageBytes: suspend (Uri) -> ByteArray = { imageBytes }
        val getMimeType: (Uri) -> String = { mimeType }
        
        // When - Test fruit analysis
        analysisService.analyzeImage(mockUri, AnalysisType.FRUIT, getImageBytes, getMimeType)
        
        // Then - Verify fruit-specific prompt was used
        verify { 
            mockRequestBuilder.createImageAnalysisRequest(
                any(), 
                any(), 
                match { prompt -> 
                    prompt.contains("fruit") && 
                    prompt.contains("anthracnose") && 
                    prompt.contains("ripeness")
                }
            ) 
        }
        
        clearMocks(mockRequestBuilder)
        
        // When - Test leaf analysis
        analysisService.analyzeImage(mockUri, AnalysisType.LEAVES, getImageBytes, getMimeType)
        
        // Then - Verify leaf-specific prompt was used
        verify { 
            mockRequestBuilder.createImageAnalysisRequest(
                any(), 
                any(), 
                match { prompt -> 
                    prompt.contains("leaf") && 
                    prompt.contains("pest damage") && 
                    prompt.contains("nutrient deficiencies")
                }
            ) 
        }
    }
    
    @Test
    fun `should parse disease detection from text response`() = runTest {
        // Given
        val imageBytes = "test image data".toByteArray()
        val mimeType = "image/jpeg"
        val analysisType = AnalysisType.FRUIT
        
        val mockRequest = createMockRequest()
        val diseaseResponse = createMockResponseWithSpecificDisease("anthracnose")
        
        every { mockRequestBuilder.createImageAnalysisRequest(any(), any(), any()) } returns mockRequest
        coEvery { mockApiClient.analyzeImage(any()) } returns Result.success(diseaseResponse)
        
        val getImageBytes: suspend (Uri) -> ByteArray = { imageBytes }
        val getMimeType: (Uri) -> String = { mimeType }
        
        // When
        val result = analysisService.analyzeImage(mockUri, analysisType, getImageBytes, getMimeType)
        
        // Then
        assertTrue(result.isSuccess)
        val analysisResult = result.getOrNull()!!
        
        assertTrue(analysisResult.diseaseDetected)
        assertEquals("Anthracnose", analysisResult.diseaseName)
        assertTrue(analysisResult.symptoms.isNotEmpty())
    }
    
    @Test
    fun `should estimate confidence level from text indicators`() = runTest {
        // Given
        val imageBytes = "test image data".toByteArray()
        val mimeType = "image/jpeg"
        val analysisType = AnalysisType.FRUIT
        
        val mockRequest = createMockRequest()
        val highConfidenceResponse = createMockResponseWithConfidence("clearly diseased")
        
        every { mockRequestBuilder.createImageAnalysisRequest(any(), any(), any()) } returns mockRequest
        coEvery { mockApiClient.analyzeImage(any()) } returns Result.success(highConfidenceResponse)
        
        val getImageBytes: suspend (Uri) -> ByteArray = { imageBytes }
        val getMimeType: (Uri) -> String = { mimeType }
        
        // When
        val result = analysisService.analyzeImage(mockUri, analysisType, getImageBytes, getMimeType)
        
        // Then
        assertTrue(result.isSuccess)
        val analysisResult = result.getOrNull()!!
        
        // Should have high confidence due to "clearly" keyword
        assertTrue(analysisResult.confidenceLevel >= 0.8f)
    }
    
    private fun createMockRequest(): GeminiRequest {
        return GeminiRequest(
            contents = listOf(
                RequestContent(
                    parts = listOf(RequestPart.TextPart("test prompt"))
                )
            )
        )
    }
    
    private fun createMockResponseWithDisease(): GeminiResponse {
        return GeminiResponse(
            candidates = listOf(
                Candidate(
                    content = Content(
                        parts = listOf(
                            Part("The lansones fruit shows signs of disease with black spots and fungal growth. This appears to be a bacterial infection requiring immediate treatment.")
                        )
                    )
                )
            )
        )
    }
    
    private fun createMockResponseHealthy(): GeminiResponse {
        return GeminiResponse(
            candidates = listOf(
                Candidate(
                    content = Content(
                        parts = listOf(
                            Part("The lansones appears healthy with no visible disease symptoms. The fruit/leaves show normal coloration and structure.")
                        )
                    )
                )
            )
        )
    }
    
    private fun createMockResponseWithSpecificDisease(disease: String): GeminiResponse {
        return GeminiResponse(
            candidates = listOf(
                Candidate(
                    content = Content(
                        parts = listOf(
                            Part("The lansones shows clear signs of $disease disease with characteristic symptoms.")
                        )
                    )
                )
            )
        )
    }
    
    private fun createMockResponseWithConfidence(confidenceText: String): GeminiResponse {
        return GeminiResponse(
            candidates = listOf(
                Candidate(
                    content = Content(
                        parts = listOf(
                            Part("The lansones is $confidenceText with visible symptoms.")
                        )
                    )
                )
            )
        )
    }
}