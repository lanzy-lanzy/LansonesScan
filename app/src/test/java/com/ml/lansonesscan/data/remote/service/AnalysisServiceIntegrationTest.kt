package com.ml.lansonesscan.data.remote.service

import android.net.Uri
import com.ml.lansonesscan.data.remote.api.GeminiApiClient
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

class AnalysisServiceIntegrationTest {
    
    private lateinit var analysisService: AnalysisService
    private lateinit var mockApiClient: GeminiApiClient
    private lateinit var requestBuilder: GeminiRequestBuilder // Real instance
    private lateinit var mockUri: Uri
    
    @BeforeEach
    fun setup() {
        mockApiClient = mockk()
        requestBuilder = GeminiRequestBuilder(TestBase64Encoder()) // Use real request builder with test encoder
        mockUri = mockk()
        
        analysisService = AnalysisService(mockApiClient, requestBuilder)
    }
    
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `should create proper request structure for fruit analysis`() = runTest {
        // Given
        val imageBytes = "test fruit image data".toByteArray()
        val mimeType = "image/jpeg"
        val analysisType = AnalysisType.FRUIT
        
        val mockResponse = createRealisticFruitDiseaseResponse()
        
        // Capture the request that gets sent to the API
        val requestSlot = slot<GeminiRequest>()
        coEvery { mockApiClient.analyzeImage(capture(requestSlot)) } returns Result.success(mockResponse)
        
        val getImageBytes: suspend (Uri) -> ByteArray = { imageBytes }
        val getMimeType: (Uri) -> String = { mimeType }
        
        // When
        val result = analysisService.analyzeImage(mockUri, analysisType, getImageBytes, getMimeType)
        
        // Then
        assertTrue(result.isSuccess)
        
        // Verify request structure
        val capturedRequest = requestSlot.captured
        assertNotNull(capturedRequest)
        assertEquals(1, capturedRequest.contents.size)
        assertEquals(2, capturedRequest.contents[0].parts.size) // Text prompt + image
        
        // Verify text part contains fruit-specific prompt
        val textPart = capturedRequest.contents[0].parts[0] as RequestPart.TextPart
        assertTrue(textPart.text.contains("fruit"))
        assertTrue(textPart.text.contains("anthracnose"))
        assertTrue(textPart.text.contains("ripeness"))
        
        // Verify image part
        val imagePart = capturedRequest.contents[0].parts[1] as RequestPart.InlineDataPart
        assertEquals(mimeType, imagePart.inlineData.mimeType)
        assertNotNull(imagePart.inlineData.data)
        
        // Verify generation config
        assertNotNull(capturedRequest.generationConfig)
        assertEquals(0.1f, capturedRequest.generationConfig!!.temperature)
        
        // Verify safety settings
        assertNotNull(capturedRequest.safetySettings)
        assertEquals(4, capturedRequest.safetySettings!!.size)
    }
    
    @Test
    fun `should create proper request structure for leaf analysis`() = runTest {
        // Given
        val imageBytes = "test leaf image data".toByteArray()
        val mimeType = "image/png"
        val analysisType = AnalysisType.LEAVES
        
        val mockResponse = createRealisticLeafHealthyResponse()
        
        val requestSlot = slot<GeminiRequest>()
        coEvery { mockApiClient.analyzeImage(capture(requestSlot)) } returns Result.success(mockResponse)
        
        val getImageBytes: suspend (Uri) -> ByteArray = { imageBytes }
        val getMimeType: (Uri) -> String = { mimeType }
        
        // When
        val result = analysisService.analyzeImage(mockUri, analysisType, getImageBytes, getMimeType)
        
        // Then
        assertTrue(result.isSuccess)
        
        // Verify leaf-specific prompt
        val capturedRequest = requestSlot.captured
        val textPart = capturedRequest.contents[0].parts[0] as RequestPart.TextPart
        assertTrue(textPart.text.contains("leaf"))
        assertTrue(textPart.text.contains("pest damage"))
        assertTrue(textPart.text.contains("nutrient deficiencies"))
        assertTrue(textPart.text.contains("scale insect"))
    }
    
    @Test
    fun `should handle realistic fruit disease response`() = runTest {
        // Given
        val imageBytes = "diseased fruit image".toByteArray()
        val mimeType = "image/jpeg"
        val analysisType = AnalysisType.FRUIT
        
        val mockResponse = createRealisticFruitDiseaseResponse()
        coEvery { mockApiClient.analyzeImage(any()) } returns Result.success(mockResponse)
        
        val getImageBytes: suspend (Uri) -> ByteArray = { imageBytes }
        val getMimeType: (Uri) -> String = { mimeType }
        
        // When
        val result = analysisService.analyzeImage(mockUri, analysisType, getImageBytes, getMimeType)
        
        // Then
        assertTrue(result.isSuccess)
        val analysisResult = result.getOrNull()!!
        
        assertTrue(analysisResult.diseaseDetected)
        assertEquals("Anthracnose", analysisResult.diseaseName)
        assertEquals("fruit", analysisResult.affectedPart)
        assertEquals("high", analysisResult.severity)
        assertTrue(analysisResult.confidenceLevel >= 0.8f) // "clearly" indicates high confidence
        
        // Verify symptoms extraction
        assertTrue(analysisResult.symptoms.contains("black spots"))
        assertTrue(analysisResult.symptoms.contains("fungal growth"))
        
        // Verify recommendations extraction
        assertTrue(analysisResult.recommendations.any { it.contains("fungicide") })
        assertTrue(analysisResult.recommendations.any { it.contains("Remove") })
    }
    
    @Test
    fun `should handle realistic leaf healthy response`() = runTest {
        // Given
        val imageBytes = "healthy leaf image".toByteArray()
        val mimeType = "image/jpeg"
        val analysisType = AnalysisType.LEAVES
        
        val mockResponse = createRealisticLeafHealthyResponse()
        coEvery { mockApiClient.analyzeImage(any()) } returns Result.success(mockResponse)
        
        val getImageBytes: suspend (Uri) -> ByteArray = { imageBytes }
        val getMimeType: (Uri) -> String = { mimeType }
        
        // When
        val result = analysisService.analyzeImage(mockUri, analysisType, getImageBytes, getMimeType)
        
        // Then
        assertTrue(result.isSuccess)
        val analysisResult = result.getOrNull()!!
        
        assertFalse(analysisResult.diseaseDetected)
        assertNull(analysisResult.diseaseName)
        assertEquals("leaves", analysisResult.affectedPart)
        assertEquals("none", analysisResult.severity)
        
        // Should still provide recommendations for healthy plants
        assertTrue(analysisResult.recommendations.isNotEmpty())
        assertTrue(analysisResult.recommendations.any { it.contains("Monitor") })
    }
    
    @Test
    fun `should handle realistic leaf disease response with nutrient deficiency`() = runTest {
        // Given
        val imageBytes = "nutrient deficient leaf".toByteArray()
        val mimeType = "image/jpeg"
        val analysisType = AnalysisType.LEAVES
        
        val mockResponse = createRealisticLeafNutrientDeficiencyResponse()
        coEvery { mockApiClient.analyzeImage(any()) } returns Result.success(mockResponse)
        
        val getImageBytes: suspend (Uri) -> ByteArray = { imageBytes }
        val getMimeType: (Uri) -> String = { mimeType }
        
        // When
        val result = analysisService.analyzeImage(mockUri, analysisType, getImageBytes, getMimeType)
        
        // Then
        assertTrue(result.isSuccess)
        val analysisResult = result.getOrNull()!!
        
        assertTrue(analysisResult.diseaseDetected)
        assertEquals("leaves", analysisResult.affectedPart)
        assertEquals("medium", analysisResult.severity)
        
        // Should detect yellowing symptoms
        assertTrue(analysisResult.symptoms.contains("yellowing"))
        
        // Should provide nutrient-related recommendations
        assertTrue(analysisResult.recommendations.isNotEmpty())
    }
    
    @Test
    fun `should handle response with mixed confidence indicators`() = runTest {
        // Given
        val imageBytes = "uncertain case image".toByteArray()
        val mimeType = "image/jpeg"
        val analysisType = AnalysisType.FRUIT
        
        val mockResponse = createUncertainResponse()
        coEvery { mockApiClient.analyzeImage(any()) } returns Result.success(mockResponse)
        
        val getImageBytes: suspend (Uri) -> ByteArray = { imageBytes }
        val getMimeType: (Uri) -> String = { mimeType }
        
        // When
        val result = analysisService.analyzeImage(mockUri, analysisType, getImageBytes, getMimeType)
        
        // Then
        assertTrue(result.isSuccess)
        val analysisResult = result.getOrNull()!!
        
        // Should have lower confidence due to "possibly" keyword
        assertTrue(analysisResult.confidenceLevel <= 0.6f)
        assertEquals("medium", analysisResult.severity)
    }
    
    private fun createRealisticFruitDiseaseResponse(): GeminiResponse {
        val responseText = """
            Based on the analysis of this lansones fruit image, I can clearly identify signs of anthracnose disease. 
            The fruit shows characteristic black spots and dark patches on the surface, along with some fungal growth 
            in the affected areas. This is a severe case that requires immediate attention.
            
            Symptoms observed:
            - Multiple black spots across the fruit surface
            - Fungal growth in affected areas
            - Brown patches indicating tissue damage
            - Signs of bacterial infection
            
            Recommendations:
            - Apply appropriate fungicide treatment immediately
            - Remove affected fruits to prevent spread to healthy ones
            - Improve ventilation around the plants
            - Ensure proper drainage to reduce moisture
        """.trimIndent()
        
        return GeminiResponse(
            candidates = listOf(
                Candidate(
                    content = Content(
                        parts = listOf(Part(responseText)),
                        role = "model"
                    ),
                    finishReason = "STOP"
                )
            )
        )
    }
    
    private fun createRealisticLeafHealthyResponse(): GeminiResponse {
        val responseText = """
            The lansones leaves in this image appear healthy with no visible signs of disease or pest damage. 
            The leaves show normal green coloration and proper structure. No symptoms of nutrient deficiencies 
            are observed.
            
            The leaves display:
            - Normal green coloration
            - Proper leaf structure and shape
            - No signs of pest damage
            - No disease symptoms
            
            Recommendations for maintaining health:
            - Monitor plant health regularly
            - Maintain proper growing conditions
            - Ensure adequate nutrition and watering
            - Continue current care practices
        """.trimIndent()
        
        return GeminiResponse(
            candidates = listOf(
                Candidate(
                    content = Content(
                        parts = listOf(Part(responseText)),
                        role = "model"
                    ),
                    finishReason = "STOP"
                )
            )
        )
    }
    
    private fun createRealisticLeafNutrientDeficiencyResponse(): GeminiResponse {
        val responseText = """
            The lansones leaves show signs of nutrient deficiency, likely nitrogen deficiency based on the 
            yellowing patterns observed. This is a moderate case that can be addressed with proper fertilization.
            
            Symptoms observed:
            - Yellowing of older leaves (chlorosis)
            - Some browning at leaf edges
            - Reduced leaf vigor
            
            Recommendations:
            - Apply balanced fertilizer with adequate nitrogen
            - Monitor soil pH and adjust if necessary
            - Ensure proper watering schedule
            - Consider soil testing for comprehensive nutrient analysis
        """.trimIndent()
        
        return GeminiResponse(
            candidates = listOf(
                Candidate(
                    content = Content(
                        parts = listOf(Part(responseText)),
                        role = "model"
                    ),
                    finishReason = "STOP"
                )
            )
        )
    }
    
    private fun createUncertainResponse(): GeminiResponse {
        val responseText = """
            The lansones fruit possibly shows some signs of disease, but it's difficult to determine with certainty. 
            There might be some minor discoloration that could indicate early stage disease development.
            
            Uncertain symptoms:
            - Possible slight discoloration
            - Might be early disease symptoms
            - Could be normal variation
            
            Recommendations:
            - Monitor closely for development of clearer symptoms
            - Maintain good growing conditions
            - Consider consulting with agricultural extension services
        """.trimIndent()
        
        return GeminiResponse(
            candidates = listOf(
                Candidate(
                    content = Content(
                        parts = listOf(Part(responseText)),
                        role = "model"
                    ),
                    finishReason = "STOP"
                )
            )
        )
    }
}