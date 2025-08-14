package com.ml.lansonesscan.domain.model

import org.junit.Assert.*
import org.junit.Test

class ScanMetadataTest {

    @Test
    fun `constructor creates valid metadata`() {
        val metadata = ScanMetadata(
            imageSize = 1024L,
            imageFormat = "JPEG",
            analysisTime = 5000L,
            apiVersion = "1.0"
        )

        assertEquals(1024L, metadata.imageSize)
        assertEquals("JPEG", metadata.imageFormat)
        assertEquals(5000L, metadata.analysisTime)
        assertEquals("1.0", metadata.apiVersion)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor throws exception for negative image size`() {
        ScanMetadata(
            imageSize = -1L,
            imageFormat = "JPEG",
            analysisTime = 5000L,
            apiVersion = "1.0"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor throws exception for blank image format`() {
        ScanMetadata(
            imageSize = 1024L,
            imageFormat = "",
            analysisTime = 5000L,
            apiVersion = "1.0"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor throws exception for negative analysis time`() {
        ScanMetadata(
            imageSize = 1024L,
            imageFormat = "JPEG",
            analysisTime = -1L,
            apiVersion = "1.0"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor throws exception for blank api version`() {
        ScanMetadata(
            imageSize = 1024L,
            imageFormat = "JPEG",
            analysisTime = 5000L,
            apiVersion = ""
        )
    }

    @Test
    fun `getFormattedImageSize returns correct format`() {
        val bytesMetadata = ScanMetadata(512L, "JPEG", 1000L, "1.0")
        assertEquals("512 B", bytesMetadata.getFormattedImageSize())

        val kbMetadata = ScanMetadata(2048L, "JPEG", 1000L, "1.0")
        assertEquals("2 KB", kbMetadata.getFormattedImageSize())

        val mbMetadata = ScanMetadata(2097152L, "JPEG", 1000L, "1.0")
        assertEquals("2 MB", mbMetadata.getFormattedImageSize())
    }

    @Test
    fun `getFormattedAnalysisTime returns correct format`() {
        val msMetadata = ScanMetadata(1024L, "JPEG", 500L, "1.0")
        assertEquals("500ms", msMetadata.getFormattedAnalysisTime())

        val secMetadata = ScanMetadata(1024L, "JPEG", 5000L, "1.0")
        assertEquals("5s", secMetadata.getFormattedAnalysisTime())

        val minMetadata = ScanMetadata(1024L, "JPEG", 125000L, "1.0")
        assertEquals("2m 5s", minMetadata.getFormattedAnalysisTime())
    }

    @Test
    fun `isValidImageFormat returns correct validation`() {
        val jpegMetadata = ScanMetadata(1024L, "JPEG", 1000L, "1.0")
        assertTrue(jpegMetadata.isValidImageFormat())

        val jpgMetadata = ScanMetadata(1024L, "JPG", 1000L, "1.0")
        assertTrue(jpgMetadata.isValidImageFormat())

        val pngMetadata = ScanMetadata(1024L, "PNG", 1000L, "1.0")
        assertTrue(pngMetadata.isValidImageFormat())

        val webpMetadata = ScanMetadata(1024L, "WEBP", 1000L, "1.0")
        assertTrue(webpMetadata.isValidImageFormat())

        val invalidMetadata = ScanMetadata(1024L, "BMP", 1000L, "1.0")
        assertFalse(invalidMetadata.isValidImageFormat())
    }

    @Test
    fun `create factory method normalizes image format`() {
        val metadata = ScanMetadata.create(
            imageSize = 1024L,
            imageFormat = "jpeg",
            analysisTime = 1000L,
            apiVersion = "1.0"
        )

        assertEquals("JPEG", metadata.imageFormat)
    }

    @Test
    fun `equals and hashCode work correctly`() {
        val metadata1 = ScanMetadata(1024L, "JPEG", 1000L, "1.0")
        val metadata2 = ScanMetadata(1024L, "JPEG", 1000L, "1.0")
        val metadata3 = ScanMetadata(2048L, "PNG", 2000L, "2.0")

        assertEquals(metadata1, metadata2)
        assertEquals(metadata1.hashCode(), metadata2.hashCode())
        assertNotEquals(metadata1, metadata3)
    }

    @Test
    fun `toString contains all fields`() {
        val metadata = ScanMetadata(1024L, "JPEG", 1000L, "1.0")
        val toString = metadata.toString()

        assertTrue(toString.contains("1024"))
        assertTrue(toString.contains("JPEG"))
        assertTrue(toString.contains("1000"))
        assertTrue(toString.contains("1.0"))
    }
}