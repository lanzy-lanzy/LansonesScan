package com.ml.lansonesscan.data.local.entities

import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.ScanMetadata
import com.ml.lansonesscan.domain.model.ScanResult
import org.junit.Assert.*
import org.junit.Test

class ScanResultEntityTest {
    
    @Test
    fun `toDomainModel converts entity to domain model correctly`() {
        // Given
        val entity = ScanResultEntity(
            id = "test-id",
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseDetected = true,
            diseaseName = "Brown Spot",
            confidenceLevel = 0.85f,
            recommendations = listOf("Apply fungicide", "Remove affected fruits"),
            timestamp = 1234567890L,
            imageSize = 1024L,
            imageFormat = "JPEG",
            analysisTime = 2500L,
            apiVersion = "1.0"
        )
        
        // When
        val domainModel = entity.toDomainModel()
        
        // Then
        assertEquals("test-id", domainModel.id)
        assertEquals("/path/to/image.jpg", domainModel.imagePath)
        assertEquals(AnalysisType.FRUIT, domainModel.analysisType)
        assertTrue(domainModel.diseaseDetected)
        assertEquals("Brown Spot", domainModel.diseaseName)
        assertEquals(0.85f, domainModel.confidenceLevel, 0.001f)
        assertEquals(listOf("Apply fungicide", "Remove affected fruits"), domainModel.recommendations)
        assertEquals(1234567890L, domainModel.timestamp)
        
        // Check metadata
        assertEquals(1024L, domainModel.metadata.imageSize)
        assertEquals("JPEG", domainModel.metadata.imageFormat)
        assertEquals(2500L, domainModel.metadata.analysisTime)
        assertEquals("1.0", domainModel.metadata.apiVersion)
    }
    
    @Test
    fun `fromDomainModel converts domain model to entity correctly`() {
        // Given
        val metadata = ScanMetadata(
            imageSize = 2048L,
            imageFormat = "PNG",
            analysisTime = 3000L,
            apiVersion = "1.1"
        )
        
        val domainModel = ScanResult(
            id = "domain-id",
            imagePath = "/path/to/domain/image.png",
            analysisType = AnalysisType.LEAVES,
            diseaseDetected = false,
            diseaseName = null,
            confidenceLevel = 0.95f,
            recommendations = listOf("Plant appears healthy"),
            timestamp = 9876543210L,
            metadata = metadata
        )
        
        // When
        val entity = ScanResultEntity.fromDomainModel(domainModel)
        
        // Then
        assertEquals("domain-id", entity.id)
        assertEquals("/path/to/domain/image.png", entity.imagePath)
        assertEquals(AnalysisType.LEAVES, entity.analysisType)
        assertFalse(entity.diseaseDetected)
        assertNull(entity.diseaseName)
        assertEquals(0.95f, entity.confidenceLevel, 0.001f)
        assertEquals(listOf("Plant appears healthy"), entity.recommendations)
        assertEquals(9876543210L, entity.timestamp)
        
        // Check metadata fields
        assertEquals(2048L, entity.imageSize)
        assertEquals("PNG", entity.imageFormat)
        assertEquals(3000L, entity.analysisTime)
        assertEquals("1.1", entity.apiVersion)
    }
    
    @Test
    fun `round trip conversion preserves all data`() {
        // Given
        val originalMetadata = ScanMetadata(
            imageSize = 4096L,
            imageFormat = "WEBP",
            analysisTime = 1500L,
            apiVersion = "2.0"
        )
        
        val originalDomainModel = ScanResult(
            id = "round-trip-id",
            imagePath = "/path/to/round/trip/image.webp",
            analysisType = AnalysisType.FRUIT,
            diseaseDetected = true,
            diseaseName = "Anthracnose",
            confidenceLevel = 0.72f,
            recommendations = listOf("Prune affected areas", "Improve air circulation", "Apply copper fungicide"),
            timestamp = 1640995200000L,
            metadata = originalMetadata
        )
        
        // When
        val entity = ScanResultEntity.fromDomainModel(originalDomainModel)
        val convertedBack = entity.toDomainModel()
        
        // Then
        assertEquals(originalDomainModel.id, convertedBack.id)
        assertEquals(originalDomainModel.imagePath, convertedBack.imagePath)
        assertEquals(originalDomainModel.analysisType, convertedBack.analysisType)
        assertEquals(originalDomainModel.diseaseDetected, convertedBack.diseaseDetected)
        assertEquals(originalDomainModel.diseaseName, convertedBack.diseaseName)
        assertEquals(originalDomainModel.confidenceLevel, convertedBack.confidenceLevel, 0.001f)
        assertEquals(originalDomainModel.recommendations, convertedBack.recommendations)
        assertEquals(originalDomainModel.timestamp, convertedBack.timestamp)
        
        // Check metadata
        assertEquals(originalMetadata.imageSize, convertedBack.metadata.imageSize)
        assertEquals(originalMetadata.imageFormat, convertedBack.metadata.imageFormat)
        assertEquals(originalMetadata.analysisTime, convertedBack.metadata.analysisTime)
        assertEquals(originalMetadata.apiVersion, convertedBack.metadata.apiVersion)
    }
    
    @Test
    fun `entity handles empty recommendations list`() {
        // Given
        val entity = ScanResultEntity(
            id = "empty-recommendations",
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.LEAVES,
            diseaseDetected = false,
            diseaseName = null,
            confidenceLevel = 0.9f,
            recommendations = emptyList(),
            timestamp = 1234567890L,
            imageSize = 512L,
            imageFormat = "JPEG",
            analysisTime = 1000L,
            apiVersion = "1.0"
        )
        
        // When
        val domainModel = entity.toDomainModel()
        
        // Then
        assertTrue(domainModel.recommendations.isEmpty())
    }
    
    @Test
    fun `entity handles null disease name correctly`() {
        // Given
        val entity = ScanResultEntity(
            id = "null-disease",
            imagePath = "/path/to/image.jpg",
            analysisType = AnalysisType.FRUIT,
            diseaseDetected = false,
            diseaseName = null,
            confidenceLevel = 0.88f,
            recommendations = listOf("Continue monitoring"),
            timestamp = 1234567890L,
            imageSize = 1536L,
            imageFormat = "PNG",
            analysisTime = 2000L,
            apiVersion = "1.2"
        )
        
        // When
        val domainModel = entity.toDomainModel()
        
        // Then
        assertFalse(domainModel.diseaseDetected)
        assertNull(domainModel.diseaseName)
    }
    
    @Test
    fun `entity handles disease detected with name correctly`() {
        // Given
        val entity = ScanResultEntity(
            id = "with-disease",
            imagePath = "/path/to/diseased/image.jpg",
            analysisType = AnalysisType.LEAVES,
            diseaseDetected = true,
            diseaseName = "Leaf Blight",
            confidenceLevel = 0.78f,
            recommendations = listOf("Remove infected leaves", "Apply appropriate treatment"),
            timestamp = 1234567890L,
            imageSize = 2048L,
            imageFormat = "JPEG",
            analysisTime = 3500L,
            apiVersion = "1.3"
        )
        
        // When
        val domainModel = entity.toDomainModel()
        
        // Then
        assertTrue(domainModel.diseaseDetected)
        assertEquals("Leaf Blight", domainModel.diseaseName)
    }
}