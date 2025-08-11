package com.scan.integration

import com.scan.core.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Path
import kotlin.system.measureTimeMillis

@DisplayName("Performance Tests")
class PerformanceTest {

    @TempDir lateinit var tempDir: Path

    @Test
    @DisplayName("Should scan single file quickly")
    fun shouldScanSingleFileQuickly() {
        // Given
        createTestFile("App.kt", "val secret = \"test-secret\"")
        
        val configuration = ScanConfiguration(
            scanPath = tempDir.toString(),
            includedExtensions = setOf("kt")
        )

        val scanEngine = ScanEngine(configuration)

        // When
        val duration = measureTimeMillis {
            runBlocking { scanEngine.executeScan(tempDir.toString()) }
        }

        // Then
        assertTrue(duration < 5000, "Single file scan should complete in under 5 seconds")
    }

    @Test
    @DisplayName("Should handle multiple files efficiently")
    fun shouldHandleMultipleFilesEfficiently() {
        // Given
        repeat(10) { i ->
            createTestFile("File$i.kt", "val data$i = \"content-$i\"")
        }
        
        val configuration = ScanConfiguration(
            scanPath = tempDir.toString(),
            includedExtensions = setOf("kt")
        )

        val scanEngine = ScanEngine(configuration)

        // When
        val result = runBlocking { scanEngine.executeScan(tempDir.toString()) }

        // Then
        assertNotNull(result)
        assertTrue(result.summary.totalFilesScanned >= 10)
        assertTrue(result.getScanDurationMs() > 0)
    }

    @Test
    @DisplayName("Should provide performance metrics")
    fun shouldProvidePerformanceMetrics() {
        // Given
        createTestFile("Performance.kt", "val value = \"test\"")
        
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
    @DisplayName("Should handle large directories")
    fun shouldHandleLargeDirectories() {
        // Given
        val subDir = tempDir.resolve("subdir").toFile()
        subDir.mkdirs()
        
        // Create files in subdirectory
        repeat(5) { i ->
            File(subDir, "SubFile$i.kt").writeText("val subData$i = \"sub-content-$i\"")
        }
        
        val configuration = ScanConfiguration(
            scanPath = tempDir.toString(),
            includedExtensions = setOf("kt")
        )

        val scanEngine = ScanEngine(configuration)

        // When
        val result = runBlocking { scanEngine.executeScan(tempDir.toString()) }

        // Then
        assertNotNull(result)
        assertTrue(result.summary.totalFilesScanned >= 5)
    }

    @Test
    @DisplayName("Should scale with file count")
    fun shouldScaleWithFileCount() {
        // Given
        val fileCount = 20
        repeat(fileCount) { i ->
            createTestFile("Scale$i.kt", "val scale$i = \"value-$i\"")
        }
        
        val configuration = ScanConfiguration(
            scanPath = tempDir.toString(),
            includedExtensions = setOf("kt")
        )

        val scanEngine = ScanEngine(configuration)

        // When
        val duration = measureTimeMillis {
            runBlocking { scanEngine.executeScan(tempDir.toString()) }
        }

        // Then
        assertTrue(duration < 30000, "Scanning $fileCount files should complete in under 30 seconds")
    }

    @Test
    @DisplayName("Should measure scan duration accurately")
    fun shouldMeasureScanDurationAccurately() {
        // Given
        createTestFile("Duration.kt", "val timing = \"test\"")
        
        val configuration = ScanConfiguration(
            scanPath = tempDir.toString(),
            includedExtensions = setOf("kt")
        )

        val scanEngine = ScanEngine(configuration)

        // When
        val result = runBlocking { scanEngine.executeScan(tempDir.toString()) }

        // Then
        assertNotNull(result)
        assertTrue(result.getScanDurationMs() > 0)
    }

    @Test
    @DisplayName("Should report files per second metric")
    fun shouldReportFilesPerSecondMetric() {
        // Given
        repeat(5) { i ->
            createTestFile("Speed$i.kt", "val speed$i = \"fast-$i\"")
        }
        
        val configuration = ScanConfiguration(
            scanPath = tempDir.toString(),
            includedExtensions = setOf("kt")
        )

        val scanEngine = ScanEngine(configuration)

        // When
        val result = runBlocking { scanEngine.executeScan(tempDir.toString()) }

        // Then
        assertNotNull(result.performance)
        assertTrue(result.performance.filesPerSecond >= 0)
        // Should process at least 1 file per second for simple files
        if (result.summary.totalFilesScanned > 0) {
            assertTrue(result.performance.filesPerSecond > 0)
        }
    }

    @Test
    @DisplayName("Should handle empty scan efficiently")
    fun shouldHandleEmptyScanEfficiently() {
        // Given
        val emptyDir = tempDir.resolve("empty").toFile()
        emptyDir.mkdirs()
        
        val configuration = ScanConfiguration(
            scanPath = emptyDir.absolutePath,
            includedExtensions = setOf("kt")
        )

        val scanEngine = ScanEngine(configuration)

        // When
        val duration = measureTimeMillis {
            val result = runBlocking { scanEngine.executeScan(emptyDir.absolutePath) }
            
            // Then
            assertNotNull(result)
            assertEquals(0, result.summary.totalFilesScanned)
        }

        // Should complete very quickly for empty directories
        assertTrue(duration < 2000, "Empty directory scan should complete in under 2 seconds")
    }

    private fun createTestFile(fileName: String, content: String): File {
        val file = tempDir.resolve(fileName).toFile()
        file.parentFile?.mkdirs()
        file.writeText(content)
        return file
    }
}
