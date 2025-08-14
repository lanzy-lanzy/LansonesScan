package com.ml.lansonesscan.data.repository

import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.AccessControlException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for RepositoryError
 */
class RepositoryErrorTest {
    
    @Test
    fun `fromException should create NetworkError for network exceptions`() {
        // Test UnknownHostException
        val unknownHostException = UnknownHostException("Host not found")
        val networkError1 = RepositoryError.fromException(unknownHostException)
        
        assertTrue(networkError1 is RepositoryError.NetworkError)
        assertTrue(networkError1.message.contains("Network error"))
        assertEquals(unknownHostException, networkError1.cause)
        
        // Test SocketTimeoutException
        val timeoutException = SocketTimeoutException("Connection timeout")
        val networkError2 = RepositoryError.fromException(timeoutException)
        
        assertTrue(networkError2 is RepositoryError.NetworkError)
        assertTrue(networkError2.message.contains("Network error"))
        assertEquals(timeoutException, networkError2.cause)
        
        // Test IOException
        val ioException = IOException("IO error")
        val networkError3 = RepositoryError.fromException(ioException)
        
        assertTrue(networkError3 is RepositoryError.NetworkError)
        assertTrue(networkError3.message.contains("Network error"))
        assertEquals(ioException, networkError3.cause)
    }
    
    @Test
    fun `fromException should create DatabaseError for SQL exceptions`() {
        val sqlException = android.database.SQLException("SQL error")
        val databaseError = RepositoryError.fromException(sqlException)
        
        assertTrue(databaseError is RepositoryError.DatabaseError)
        assertTrue(databaseError.message.contains("Database error"))
        assertEquals(sqlException, databaseError.cause)
    }
    
    @Test
    fun `fromException should create StorageError for file exceptions`() {
        // Test FileNotFoundException
        val fileNotFoundException = FileNotFoundException("File not found")
        val storageError1 = RepositoryError.fromException(fileNotFoundException)
        
        assertTrue(storageError1 is RepositoryError.StorageError)
        assertTrue(storageError1.message.contains("Storage error"))
        assertEquals(fileNotFoundException, storageError1.cause)
        
        // Test AccessControlException
        val accessException = AccessControlException("Access denied")
        val storageError2 = RepositoryError.fromException(accessException)
        
        assertTrue(storageError2 is RepositoryError.StorageError)
        assertTrue(storageError2.message.contains("Storage error"))
        assertEquals(accessException, storageError2.cause)
    }
    
    @Test
    fun `fromException should create ValidationError for argument exceptions`() {
        // Test IllegalArgumentException
        val illegalArgException = IllegalArgumentException("Invalid argument")
        val validationError1 = RepositoryError.fromException(illegalArgException)
        
        assertTrue(validationError1 is RepositoryError.ValidationError)
        assertTrue(validationError1.message.contains("Validation error"))
        assertEquals(illegalArgException, validationError1.cause)
        
        // Test IllegalStateException
        val illegalStateException = IllegalStateException("Invalid state")
        val validationError2 = RepositoryError.fromException(illegalStateException)
        
        assertTrue(validationError2 is RepositoryError.ValidationError)
        assertTrue(validationError2.message.contains("Validation error"))
        assertEquals(illegalStateException, validationError2.cause)
    }
    
    @Test
    fun `fromException should create UnknownError for other exceptions`() {
        val runtimeException = RuntimeException("Unknown error")
        val unknownError = RepositoryError.fromException(runtimeException)
        
        assertTrue(unknownError is RepositoryError.UnknownError)
        assertTrue(unknownError.message.contains("Unexpected error"))
        assertEquals(runtimeException, unknownError.cause)
    }
    
    @Test
    fun `error types should have correct message format`() {
        val networkError = RepositoryError.NetworkError("Network issue", null)
        assertEquals("Network issue", networkError.message)
        
        val databaseError = RepositoryError.DatabaseError("Database issue", null)
        assertEquals("Database issue", databaseError.message)
        
        val storageError = RepositoryError.StorageError("Storage issue", null)
        assertEquals("Storage issue", storageError.message)
        
        val validationError = RepositoryError.ValidationError("Validation issue", null)
        assertEquals("Validation issue", validationError.message)
        
        val analysisError = RepositoryError.AnalysisError("Analysis issue", null)
        assertEquals("Analysis issue", analysisError.message)
        
        val unknownError = RepositoryError.UnknownError("Unknown issue", null)
        assertEquals("Unknown issue", unknownError.message)
    }
}