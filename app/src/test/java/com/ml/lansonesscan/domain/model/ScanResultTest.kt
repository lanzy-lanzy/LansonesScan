package com.ml.lansonesscan.domain.model

import org.junit.Assert.*
import org.junit.Test

class ScanResultTest {

    private val validMetadata = ScanMetadata(
        imageSize = 1024L,
        imageFormat = "JPEG",
        analysisTime = 1000L,
        apiVersion = "1.0"
    )

    @Test
    fun `constructor creates valid scan result`() {
        val scanResult = ScanResult(
            id = "test-id",
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseDetected = true,
            diseaseName = "Brown Spot",
            confidenceLevel = 0.85f,
            recommendations = listOf("Apply fungicide", "Remove affected fruits"),
            timestamp = System.currentTimeMillis(),
            metadata = validMetadata
        )

        assertEquals("test-id", scanResult.id)
        assertEquals("/path/to/image.jpg", scanResult.imagePath)
        assertEquals(AnalysisType.FRUIT, scanResult.analysisType)
        assertTrue(scanResult.diseaseDetected)
        assertEquals("Brown Spot", scanResult.diseaseName)
        assertEquals(0.85f, scanResult.confidenceLevel, 0.001f)
        assertEquals(2, scanResult.recommendations.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor throws exception for blank id`() {
        ScanResult(
            id = "",
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseDetected = false,
            diseaseName = null,
            confidenceLevel = 0.85f,
            recommendations = emptyList(),
            timestamp = System.currentTimeMillis(),
            metadata = validMetadata
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor throws exception for blank image path`() {
        ScanResult(
            id = "test-id",
            imagePath = "",
            analysisType = AnalysisType.FRUIT,
            diseaseDetected = false,
            diseaseName = null,
            confidenceLevel = 0.85f,
            recommendations = emptyList(),
            timestamp = System.currentTimeMillis(),
            metadata = validMetadata
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor throws exception for invalid confidence level`() {
        ScanResult(
            id = "test-id",
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseDetected = false,
            diseaseName = null,
            confidenceLevel = 1.5f,
            recommendations = emptyList(),
            timestamp = System.currentTimeMillis(),
            metadata = validMetadata
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor throws exception for negative timestamp`() {
        ScanResult(
            id = "test-id",
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseDetected = false,
            diseaseName = null,
            confidenceLevel = 0.85f,
            recommendations = emptyList(),
            timestamp = -1L,
            metadata = validMetadata
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor throws exception when disease detected but name is null`() {
        ScanResult(
            id = "test-id",
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseDetected = true,
            diseaseName = null,
            confidenceLevel = 0.85f,
            recommendations = emptyList(),
            timestamp = System.currentTimeMillis(),
            metadata = validMetadata
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor throws exception when disease detected but name is blank`() {
        ScanResult(
            id = "test-id",
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseDetected = true,
            diseaseName = "",
            confidenceLevel = 0.85f,
            recommendations = emptyList(),
            timestamp = System.currentTimeMillis(),
            metadata = validMetadata
        )
    }

    @Test
    fun `getConfidencePercentage returns correct percentage`() {
        val scanResult = createValidScanResult(confidenceLevel = 0.85f)
        assertEquals(85, scanResult.getConfidencePercentage())
    }

    @Test
    fun `getStatusText returns correct status for healthy`() {
        val scanResult = createValidScanResult(diseaseDetected = false, diseaseName = null)
        assertEquals("Healthy", scanResult.getStatusText())
    }

    @Test
    fun `getStatusText returns correct status for diseased`() {
        val scanResult = createValidScanResult(diseaseDetected = true, diseaseName = "Brown Spot")
        assertEquals("Disease Detected: Brown Spot", scanResult.getStatusText())
    }

    @Test
    fun `getSeverityLevel returns correct levels`() {
        val healthyScan = createValidScanResult(diseaseDetected = false)
        assertEquals(SeverityLevel.HEALTHY, healthyScan.getSeverityLevel())

        val highSeverity = createValidScanResult(diseaseDetected = true, diseaseName = "High Disease", confidenceLevel = 0.9f)
        assertEquals(SeverityLevel.HIGH, highSeverity.getSeverityLevel())

        val mediumSeverity = createValidScanResult(diseaseDetected = true, diseaseName = "Medium Disease", confidenceLevel = 0.7f)
        assertEquals(SeverityLevel.MEDIUM, mediumSeverity.getSeverityLevel())

        val lowSeverity = createValidScanResult(diseaseDetected = true, diseaseName = "Low Disease", confidenceLevel = 0.5f)
        assertEquals(SeverityLevel.LOW, lowSeverity.getSeverityLevel())
    }

    @Test
    fun `isValid returns true for valid scan result`() {
        val scanResult = createValidScanResult()
        assertTrue(scanResult.isValid())
    }

    @Test
    fun `withRecommendations creates copy with new recommendations`() {
        val original = createValidScanResult(recommendations = listOf("Original"))
        val updated = original.withRecommendations(listOf("New", "Recommendations"))

        assertEquals(listOf("Original"), original.recommendations)
        assertEquals(listOf("New", "Recommendations"), updated.recommendations)
        assertEquals(original.id, updated.id)
    }

    @Test
    fun `create factory method generates id and timestamp`() {
        val scanResult = ScanResult.create(
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseDetected = false,
            diseaseName = null,
            confidenceLevel = 0.85f,
            recommendations = emptyList(),
            metadata = validMetadata
        )

        assertNotNull(scanResult.id)
        assertTrue(scanResult.id.isNotBlank())
        assertTrue(scanResult.timestamp > 0)
    }

    @Test
    fun `createHealthy factory method creates healthy scan`() {
        val scanResult = ScanResult.createHealthy(
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.LEAVES,
            confidenceLevel = 0.9f,
            metadata = validMetadata
        )

        assertFalse(scanResult.diseaseDetected)
        assertNull(scanResult.diseaseName)
        assertTrue(scanResult.recommendations.isNotEmpty())
        assertTrue(scanResult.recommendations[0].contains("healthy"))
    }

    @Test
    fun `createDiseased factory method creates diseased scan`() {
        val scanResult = ScanResult.createDiseased(
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseName = "Brown Spot",
            confidenceLevel = 0.8f,
            recommendations = listOf("Apply treatment"),
            metadata = validMetadata
        )

        assertTrue(scanResult.diseaseDetected)
        assertEquals("Brown Spot", scanResult.diseaseName)
        assertEquals(listOf("Apply treatment"), scanResult.recommendations)
    }

    @Test
    fun `equals and hashCode work correctly`() {
        val timestamp = System.currentTimeMillis()
        val scanResult1 = createValidScanResult(id = "same-id", timestamp = timestamp)
        val scanResult2 = createValidScanResult(id = "same-id", timestamp = timestamp)
        val scanResult3 = createValidScanResult(id = "different-id", timestamp = timestamp)

        assertEquals(scanResult1, scanResult2)
        assertEquals(scanResult1.hashCode(), scanResult2.hashCode())
        assertNotEquals(scanResult1, scanResult3)
    }

    private fun createValidScanResult(
        id: String = "test-id",
        imagePath: String = "/path/to/image.jpg",
        analysisType: AnalysisType = AnalysisType.FRUIT,
        diseaseDetected: Boolean = false,
        diseaseName: String? = null,
        confidenceLevel: Float = 0.85f,
        recommendations: List<String> = emptyList(),
        timestamp: Long = System.currentTimeMillis(),
        metadata: ScanMetadata = validMetadata
    ): ScanResult {
        return ScanResult(
            id = id,
            imagePath = imagePath,
            analysisType = analysisType,
            diseaseDetected = diseaseDetected,
            diseaseName = diseaseName,
            confidenceLevel = confidenceLevel,
            recommendations = recommendations,
            timestamp = timestamp,
            metadata = metadata
        )
    }
}

class SeverityLevelTest {

    @Test
    fun `getDisplayName returns correct display names`() {
        assertEquals("Healthy", SeverityLevel.HEALTHY.getDisplayName())
        assertEquals("Low Risk", SeverityLevel.LOW.getDisplayName())
        assertEquals("Medium Risk", SeverityLevel.MEDIUM.getDisplayName())
        assertEquals("High Risk", SeverityLevel.HIGH.getDisplayName())
    }

    @Test
    fun `getColor returns valid hex colors`() {
        assertTrue(SeverityLevel.HEALTHY.getColor().startsWith("#"))
        assertTrue(SeverityLevel.LOW.getColor().startsWith("#"))
        assertTrue(SeverityLevel.MEDIUM.getColor().startsWith("#"))
        assertTrue(SeverityLevel.HIGH.getColor().startsWith("#"))
    }

    @Test
    fun `enum values are correct`() {
        val values = SeverityLevel.values()
        assertEquals(4, values.size)
        assertTrue(values.contains(SeverityLevel.HEALTHY))
        assertTrue(values.contains(SeverityLevel.LOW))
        assertTrue(values.contains(SeverityLevel.MEDIUM))
        assertTrue(values.contains(SeverityLevel.HIGH))
    }
}