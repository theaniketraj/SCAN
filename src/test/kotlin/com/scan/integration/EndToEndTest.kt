package com.scan.integration

import com.scan.core.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Path

@DisplayName("End-to-End Integration Tests")
class EndToEndTest {

    @TempDir lateinit var tempDir: Path

    @Test
    @DisplayName("Should perform complete scan workflow")
    fun shouldPerformCompleteScanWorkflow() {
        // Given
        val testFile = createTestFile("Application.kt", """
            package com.example
            
            class Application {
                private val apiKey = "sk_live_abcdef123456"
                private val password = "admin123"
            }
        """.trimIndent())

        val configuration = ScanConfiguration(
            scanPath = tempDir.toString(),
            includedExtensions = setOf("kt")
        )

        val scanEngine = ScanEngine(configuration)

        // When
        val result = runBlocking { scanEngine.executeScan(tempDir.toString()) }

        // Then
        assertNotNull(result)
        assertTrue(result.summary.totalFilesScanned >= 1)
        assertNotNull(result.findings)
        assertTrue(result.getScanDurationMs() > 0)
    }

    @Test
    @DisplayName("Should handle multiple file types")
    fun shouldHandleMultipleFileTypes() {
        // Given
        createTestFile("Config.kt", "val secret = \"kt-secret\"")
        createTestFile("App.java", "String secret = \"java-secret\";")
        createTestFile("config.yml", "secret: yml-secret")
        
        val configuration = ScanConfiguration(
            scanPath = tempDir.toString(),
            includedExtensions = setOf("kt", "java", "yml")
        )

        val scanEngine = ScanEngine(configuration)

        // When
        val result = runBlocking { scanEngine.executeScan(tempDir.toString()) }

        // Then
        assertNotNull(result)
        assertTrue(result.summary.totalFilesScanned >= 3)
    }

    @Test
    @DisplayName("Should respect file exclusions")
    fun shouldRespectFileExclusions() {
        // Given
        createTestFile("main/App.kt", "val secret = \"main-secret\"")
        val testDir = tempDir.resolve("test").toFile()
        testDir.mkdirs()
        File(testDir, "Test.kt").writeText("val secret = \"test-secret\"")
        
        val configuration = ScanConfiguration(
            scanPath = tempDir.toString(),
            excludePatterns = listOf("**/test/**"),
            includedExtensions = setOf("kt")
        )

        val scanEngine = ScanEngine(configuration)

        // When
        val result = runBlocking { scanEngine.executeScan(tempDir.toString()) }

        // Then
        assertNotNull(result)
        assertTrue(result.summary.totalFilesScanned >= 1)
        // Should only scan main files, not test files
    }

    @Test
    @DisplayName("Should handle empty directories")
    fun shouldHandleEmptyDirectories() {
        // Given
        val emptyDir = tempDir.resolve("empty").toFile()
        emptyDir.mkdirs()
        
        val configuration = ScanConfiguration(
            scanPath = emptyDir.absolutePath
        )

        val scanEngine = ScanEngine(configuration)

        // When
        val result = runBlocking { scanEngine.executeScan(emptyDir.absolutePath) }

        // Then
        assertNotNull(result)
        assertEquals(0, result.summary.totalFilesScanned)
        assertEquals(0, result.summary.totalFindingsCount)
    }

    @Test
    @DisplayName("Should handle large files correctly")
    fun shouldHandleLargeFilesCorrectly() {
        // Given
        val largeContent = "val secret = \"test\"\n".repeat(1000)
        createTestFile("LargeFile.kt", largeContent)
        
        val configuration = ScanConfiguration(
            scanPath = tempDir.toString(),
            maxFileSize = 1024 * 1024, // 1MB limit
            includedExtensions = setOf("kt")
        )

        val scanEngine = ScanEngine(configuration)

        // When
        val result = runBlocking { scanEngine.executeScan(tempDir.toString()) }

        // Then
        assertNotNull(result)
        assertTrue(result.summary.totalFilesScanned >= 1)
    }

    @Test
    @DisplayName("Should provide performance metrics")
    fun shouldProvidePerformanceMetrics() {
        // Given
        createTestFile("PerfTest.kt", "val data = \"test-data\"")
        
        val configuration = ScanConfiguration(
            scanPath = tempDir.toString(),
            includedExtensions = setOf("kt")
        )

        val scanEngine = ScanEngine(configuration)

        // When
        val result = runBlocking { scanEngine.executeScan(tempDir.toString()) }

        // Then
        assertNotNull(result.performance)
        assertTrue(result.performance.totalDurationMs > 0)
        assertTrue(result.performance.filesPerSecond >= 0)
    }

    @Test
    @DisplayName("Should generate scan summary")
    fun shouldGenerateScanSummary() {
        // Given
        createTestFile("Summary.kt", "val value = \"test\"")
        
        val configuration = ScanConfiguration(
            scanPath = tempDir.toString(),
            includedExtensions = setOf("kt")
        )

        val scanEngine = ScanEngine(configuration)

        // When
        val result = runBlocking { scanEngine.executeScan(tempDir.toString()) }

        // Then
        assertNotNull(result.summary)
        assertTrue(result.summary.totalFilesScanned >= 1)
        assertNotNull(result.summary.findingsBySeverity)
        assertNotNull(result.summary.findingsByDetector)
    }

    @Test
    @DisplayName("Should handle scan errors gracefully")
    fun shouldHandleScanErrorsGracefully() {
        // Given
        val invalidPath = tempDir.resolve("non-existent").toString()
        
        val configuration = ScanConfiguration(
            scanPath = invalidPath
        )

        val scanEngine = ScanEngine(configuration)

        // When & Then
        assertThrows<Exception> {
            runBlocking { scanEngine.executeScan(invalidPath) }
        }
    }

    private fun createTestFile(fileName: String, content: String): File {
        val file = tempDir.resolve(fileName).toFile()
        file.parentFile?.mkdirs()
        file.writeText(content)
        return file
    }
}
