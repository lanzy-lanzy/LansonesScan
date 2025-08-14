package com.ml.lansonesscan.data.local.database

import com.ml.lansonesscan.domain.model.AnalysisType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ConvertersTest {
    
    private lateinit var converters: Converters
    
    @Before
    fun setUp() {
        converters = Converters()
    }
    
    @Test
    fun `fromStringList converts list to JSON string correctly`() {
        // Given
        val stringList = listOf("recommendation 1", "recommendation 2", "recommendation 3")
        
        // When
        val result = converters.fromStringList(stringList)
        
        // Then
        assertNotNull(result)
        assertTrue(result.contains("recommendation 1"))
        assertTrue(result.contains("recommendation 2"))
        assertTrue(result.contains("recommendation 3"))
    }
    
    @Test
    fun `toStringList converts JSON string back to list correctly`() {
        // Given
        val originalList = listOf("recommendation 1", "recommendation 2", "recommendation 3")
        val jsonString = converters.fromStringList(originalList)
        
        // When
        val result = converters.toStringList(jsonString)
        
        // Then
        assertEquals(originalList, result)
    }
    
    @Test
    fun `toStringList handles empty JSON string`() {
        // Given
        val emptyJsonString = "[]"
        
        // When
        val result = converters.toStringList(emptyJsonString)
        
        // Then
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `toStringList handles blank string gracefully`() {
        // Given
        val blankString = ""
        
        // When
        val result = converters.toStringList(blankString)
        
        // Then
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `fromStringList handles empty list`() {
        // Given
        val emptyList = emptyList<String>()
        
        // When
        val result = converters.fromStringList(emptyList)
        
        // Then
        assertEquals("[]", result)
    }
    
    @Test
    fun `fromStringList handles null list`() {
        // Given
        val nullList: List<String>? = null
        
        // When
        val result = converters.fromStringList(nullList)
        
        // Then
        assertEquals("", result)
    }
    
    @Test
    fun `fromAnalysisType converts FRUIT to string correctly`() {
        // Given
        val analysisType = AnalysisType.FRUIT
        
        // When
        val result = converters.fromAnalysisType(analysisType)
        
        // Then
        assertEquals("FRUIT", result)
    }
    
    @Test
    fun `fromAnalysisType converts LEAVES to string correctly`() {
        // Given
        val analysisType = AnalysisType.LEAVES
        
        // When
        val result = converters.fromAnalysisType(analysisType)
        
        // Then
        assertEquals("LEAVES", result)
    }
    
    @Test
    fun `toAnalysisType converts string to FRUIT correctly`() {
        // Given
        val analysisTypeString = "FRUIT"
        
        // When
        val result = converters.toAnalysisType(analysisTypeString)
        
        // Then
        assertEquals(AnalysisType.FRUIT, result)
    }
    
    @Test
    fun `toAnalysisType converts string to LEAVES correctly`() {
        // Given
        val analysisTypeString = "LEAVES"
        
        // When
        val result = converters.toAnalysisType(analysisTypeString)
        
        // Then
        assertEquals(AnalysisType.LEAVES, result)
    }
    
    @Test
    fun `toAnalysisType returns FRUIT for invalid string`() {
        // Given
        val invalidString = "INVALID"
        
        // When
        val result = converters.toAnalysisType(invalidString)
        
        // Then
        assertEquals(AnalysisType.FRUIT, result) // Should fallback to FRUIT
    }
    
    @Test
    fun `round trip conversion for string list works correctly`() {
        // Given
        val originalList = listOf("item1", "item2", "item3", "special chars: !@#$%")
        
        // When
        val jsonString = converters.fromStringList(originalList)
        val convertedBack = converters.toStringList(jsonString)
        
        // Then
        assertEquals(originalList, convertedBack)
    }
    
    @Test
    fun `round trip conversion for analysis type works correctly`() {
        // Given
        val originalType = AnalysisType.FRUIT
        
        // When
        val stringValue = converters.fromAnalysisType(originalType)
        val convertedBack = converters.toAnalysisType(stringValue)
        
        // Then
        assertEquals(originalType, convertedBack)
    }
    
    @Test
    fun `fromTimestamp handles null value`() {
        // Given
        val nullTimestamp: Long? = null
        
        // When
        val result = converters.fromTimestamp(nullTimestamp)
        
        // Then
        assertEquals(0L, result)
    }
    
    @Test
    fun `fromTimestamp handles valid value`() {
        // Given
        val timestamp = 1234567890L
        
        // When
        val result = converters.fromTimestamp(timestamp)
        
        // Then
        assertEquals(1234567890L, result)
    }
    
    @Test
    fun `toTimestamp returns same value`() {
        // Given
        val timestamp = 1234567890L
        
        // When
        val result = converters.toTimestamp(timestamp)
        
        // Then
        assertEquals(timestamp, result)
    }
}