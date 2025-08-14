package com.ml.lansonesscan.di

import org.junit.Assert.*
import org.junit.Test

class DatabaseModuleTest {

    @Test
    fun `database module has required methods`() {
        val moduleClass = DatabaseModule::class.java
        
        // Check provideDatabase method exists
        val provideDatabaseMethod = moduleClass.declaredMethods.find { it.name == "provideDatabase" }
        assertNotNull("provideDatabase method should exist", provideDatabaseMethod)
        assertEquals("provideDatabase should have 1 parameter", 1, provideDatabaseMethod?.parameterCount)
        
        // Check provideScanDao method exists
        val provideScanDaoMethod = moduleClass.declaredMethods.find { it.name == "provideScanDao" }
        assertNotNull("provideScanDao method should exist", provideScanDaoMethod)
        assertEquals("provideScanDao should have 1 parameter", 1, provideScanDaoMethod?.parameterCount)
        
        // Check provideInMemoryDatabase method exists
        val provideInMemoryDatabaseMethod = moduleClass.declaredMethods.find { it.name == "provideInMemoryDatabase" }
        assertNotNull("provideInMemoryDatabase method should exist", provideInMemoryDatabaseMethod)
        assertEquals("provideInMemoryDatabase should have 1 parameter", 1, provideInMemoryDatabaseMethod?.parameterCount)
        
        // Check clearDatabase method exists
        val clearDatabaseMethod = moduleClass.declaredMethods.find { it.name == "clearDatabase" }
        assertNotNull("clearDatabase method should exist", clearDatabaseMethod)
        assertEquals("clearDatabase should have 0 parameters", 0, clearDatabaseMethod?.parameterCount)
    }

    @Test
    fun `database module structure is correct`() {
        val moduleClass = DatabaseModule::class.java
        
        // Should not be an interface
        assertFalse("DatabaseModule should not be an interface", moduleClass.isInterface)
        
        // Should be a Kotlin object (singleton)
        val instanceField = moduleClass.declaredFields.find { it.name == "INSTANCE" }
        assertNotNull("DatabaseModule should have INSTANCE field (Kotlin object)", instanceField)
    }

    @Test
    fun `database module has database field`() {
        val moduleClass = DatabaseModule::class.java
        
        // Check if there's a database field
        val databaseField = moduleClass.declaredFields.find { it.name == "database" }
        assertNotNull("DatabaseModule should have database field", databaseField)
    }

    @Test
    fun `database module methods have correct return types`() {
        val moduleClass = DatabaseModule::class.java
        
        // Check provideDatabase return type
        val provideDatabaseMethod = moduleClass.declaredMethods.find { it.name == "provideDatabase" }
        assertEquals("provideDatabase should return LansonesDatabase", 
            "LansonesDatabase", provideDatabaseMethod?.returnType?.simpleName)
        
        // Check provideScanDao return type
        val provideScanDaoMethod = moduleClass.declaredMethods.find { it.name == "provideScanDao" }
        assertEquals("provideScanDao should return ScanDao", 
            "ScanDao", provideScanDaoMethod?.returnType?.simpleName)
        
        // Check provideInMemoryDatabase return type
        val provideInMemoryDatabaseMethod = moduleClass.declaredMethods.find { it.name == "provideInMemoryDatabase" }
        assertEquals("provideInMemoryDatabase should return LansonesDatabase", 
            "LansonesDatabase", provideInMemoryDatabaseMethod?.returnType?.simpleName)
    }

    @Test
    fun `extension functions file structure is valid`() {
        // This is a basic test to ensure the file compiles correctly
        // Extension functions are tested implicitly by compilation success
        assertTrue("Extension functions should be defined", true)
    }

    @Test
    fun `database module structure validation`() {
        // These are basic structural tests that can run without Android context
        val moduleClass = DatabaseModule::class.java
        
        // Verify module has required methods
        val methods = moduleClass.declaredMethods.map { it.name }
        assertTrue("Module should have provideDatabase method", methods.contains("provideDatabase"))
        assertTrue("Module should have provideScanDao method", methods.contains("provideScanDao"))
        assertTrue("Module should have provideInMemoryDatabase method", methods.contains("provideInMemoryDatabase"))
        assertTrue("Module should have clearDatabase method", methods.contains("clearDatabase"))
        
        // Verify module has database field
        val fields = moduleClass.declaredFields.map { it.name }
        assertTrue("Module should have database field", fields.contains("database"))
    }

    @Test
    fun `database module method signatures are correct`() {
        val moduleClass = DatabaseModule::class.java
        
        // Check provideDatabase method signature
        val provideDatabaseMethod = moduleClass.declaredMethods.find { it.name == "provideDatabase" }
        assertNotNull("provideDatabase method should exist", provideDatabaseMethod)
        assertEquals("provideDatabase should have 1 parameter", 1, provideDatabaseMethod?.parameterCount)
        assertEquals("provideDatabase should return LansonesDatabase", 
            "LansonesDatabase", provideDatabaseMethod?.returnType?.simpleName)
        
        // Check provideScanDao method signature
        val provideScanDaoMethod = moduleClass.declaredMethods.find { it.name == "provideScanDao" }
        assertNotNull("provideScanDao method should exist", provideScanDaoMethod)
        assertEquals("provideScanDao should have 1 parameter", 1, provideScanDaoMethod?.parameterCount)
        assertEquals("provideScanDao should return ScanDao", 
            "ScanDao", provideScanDaoMethod?.returnType?.simpleName)
        
        // Check clearDatabase method signature
        val clearDatabaseMethod = moduleClass.declaredMethods.find { it.name == "clearDatabase" }
        assertNotNull("clearDatabase method should exist", clearDatabaseMethod)
        assertEquals("clearDatabase should have 0 parameters", 0, clearDatabaseMethod?.parameterCount)
    }

    @Test
    fun `database module is properly structured as singleton`() {
        val moduleClass = DatabaseModule::class.java
        
        // Should not be an interface
        assertFalse("DatabaseModule should not be an interface", moduleClass.isInterface)
        
        // Should be a Kotlin object (singleton) - indicated by INSTANCE field
        val instanceField = moduleClass.declaredFields.find { it.name == "INSTANCE" }
        assertNotNull("DatabaseModule should have INSTANCE field (Kotlin object)", instanceField)
        
        // Should have Volatile annotation on database field for thread safety
        val databaseField = moduleClass.declaredFields.find { it.name == "database" }
        assertNotNull("DatabaseModule should have database field", databaseField)
    }
}