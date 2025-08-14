package com.ml.lansonesscan.data.remote.service

import com.ml.lansonesscan.domain.model.AnalysisType
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for neutral analysis functionality
 */
class NeutralAnalysisTest {

    @Test
    fun `AnalysisResult with NON_LANSONES type should have correct properties`() {
        val result = AnalysisResult(
            diseaseDetected = false,
            diseaseName = null,
            confidenceLevel = 1.0f,
            affectedPart = "general",
            symptoms = listOf("Observable characteristic 1", "Observable characteristic 2"),
            recommendations = emptyList(),
            severity = "none",
            rawResponse = "Neutral analysis: Test item description",
            detectedAnalysisType = AnalysisType.NON_LANSONES
        )

        // Verify neutral analysis properties
        assertFalse("Disease should not be detected for neutral analysis", result.diseaseDetected)
        assertNull("Disease name should be null for neutral analysis", result.diseaseName)
        assertEquals("Confidence should be 1.0 for neutral analysis", 1.0f, result.confidenceLevel)
        assertEquals("Affected part should be general", "general", result.affectedPart)
        assertTrue("Recommendations should be empty for neutral analysis", result.recommendations.isEmpty())
        assertEquals("Severity should be none", "none", result.severity)
        assertEquals("Analysis type should be NON_LANSONES", AnalysisType.NON_LANSONES, result.detectedAnalysisType)
        assertTrue("Raw response should contain description", result.rawResponse.contains("Test item description"))
    }

    @Test
    fun `AnalysisType NON_LANSONES should have correct display properties`() {
        val analysisType = AnalysisType.NON_LANSONES
        
        assertEquals("Display name should be correct", "General Analysis", analysisType.getDisplayName())
        assertEquals("Description should be neutral", "Provides factual analysis of non-lansones items", analysisType.getDescription())
    }

    @Test
    fun `DetectionResult should properly identify non-lansones items`() {
        val detectionResult = DetectionResult(
            isLansones = false,
            itemType = "other",
            confidence = 0.9f,
            description = "Apple fruit detected"
        )
        
        assertFalse("Should not be identified as lansones", detectionResult.isLansones)
        assertEquals("Item type should be other", "other", detectionResult.itemType)
        assertEquals("Confidence should match", 0.9f, detectionResult.confidence)
        assertEquals("Description should match", "Apple fruit detected", detectionResult.description)
    }

    @Test
    fun `NeutralAnalysisResponse should have correct default values`() {
        val neutralResponse = NeutralAnalysisResponse()
        
        assertFalse("Disease detected should be false by default", neutralResponse.diseaseDetected)
        assertNull("Disease name should be null by default", neutralResponse.diseaseName)
        assertEquals("Confidence should be 1.0 by default", 1.0f, neutralResponse.confidenceLevel)
        assertEquals("Affected part should be general by default", "general", neutralResponse.affectedPart)
        assertEquals("Severity should be none by default", "none", neutralResponse.severity)
    }
}
