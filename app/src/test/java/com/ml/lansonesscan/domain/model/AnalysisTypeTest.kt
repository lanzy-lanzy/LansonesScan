package com.ml.lansonesscan.domain.model

import org.junit.Assert.*
import org.junit.Test

class AnalysisTypeTest {

    @Test
    fun `getDisplayName returns correct display names`() {
        assertEquals("Fruit Analysis", AnalysisType.FRUIT.getDisplayName())
        assertEquals("Leaf Analysis", AnalysisType.LEAVES.getDisplayName())
        assertEquals("General Analysis", AnalysisType.NON_LANSONES.getDisplayName())
    }

    @Test
    fun `getDescription returns correct descriptions`() {
        assertTrue(AnalysisType.FRUIT.getDescription().contains("fruit surface"))
        assertTrue(AnalysisType.LEAVES.getDescription().contains("leaves"))
    }

    @Test
    fun `fromString returns correct enum values`() {
        assertEquals(AnalysisType.FRUIT, AnalysisType.fromString("FRUIT"))
        assertEquals(AnalysisType.FRUIT, AnalysisType.fromString("fruit"))
        assertEquals(AnalysisType.LEAVES, AnalysisType.fromString("LEAVES"))
        assertEquals(AnalysisType.LEAVES, AnalysisType.fromString("leaves"))
    }

    @Test
    fun `fromString returns null for invalid values`() {
        assertNull(AnalysisType.fromString("INVALID"))
        assertNull(AnalysisType.fromString(""))
        assertNull(AnalysisType.fromString(null))
    }

    @Test
    fun `enum values are correct`() {
        val values = AnalysisType.values()
        assertEquals(3, values.size)
        assertTrue(values.contains(AnalysisType.FRUIT))
        assertTrue(values.contains(AnalysisType.LEAVES))
        assertTrue(values.contains(AnalysisType.NON_LANSONES))
    }
}