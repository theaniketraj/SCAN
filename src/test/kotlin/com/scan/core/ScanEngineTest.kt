package com.scan.core

import com.scan.detectors.*
import com.scan.filters.*
import com.scan.patterns.*
import com.scan.utils.*
import io.mockk.*
import java.io.File
import java.nio.file.Path
import kotlin.test.assertContains
import kotlin.test.assertTrue
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScanEngineTest {

    private lateinit var scanEngine: ScanEngine
    private lateinit var mockConfiguration: ScanConfiguration
    private lateinit var mockDetector: DetectorInterface
    private lateinit var mockFilter: FilterInterface
    private lateinit var mockFileScanner: FileScanner

    @TempDir lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)

        mockConfiguration =
                mockk(relaxed = true) {
                    every { scanPaths } returns listOf("src/main/kotlin")
                    every { excludePaths } returns listOf("**/*test*/**")
                    every { includeFileExtensions } returns
                            listOf("kt", "java", "properties", "yml", "yaml")
                    every { maxFileSize } returns 1024 * 1024 // 1MB
                    every { enableEntropyDetection } returns true
                    every { enablePatternDetection } returns true
                    every { enableContextAwareDetection } returns true
                    every { minEntropyThreshold } returns 4.5
                    every { reportFormat } returns "JSON"
                    every { outputFile } returns "scan-results.json"
                    every { failOnFound } returns true
                    every { verbose } returns false
                }

        mockDetector = mockk()
        mockFilter = mockk()
        mockFileScanner = mockk()

        scanEngine = ScanEngine(mockConfiguration)
    }

    @Test
    fun `should initialize with valid configuration`() {
        // Given
        val config =
                ScanConfiguration(
                        scanPaths = listOf("src/main/kotlin"),
                        excludePaths = listOf("**/test/**"),
                        includeFileExtensions = listOf("kt", "java")
                )

        // When
        val engine = ScanEngine(config)

        // Then
        assertNotNull(engine)
        assertEquals(config, engine.configuration)
    }

    @Test
    fun `should throw exception for invalid configuration`() {
        // Given
        val invalidConfig =
                ScanConfiguration(
                        scanPaths = emptyList(), // Invalid: empty scan paths
                        excludePaths = emptyList(),
                        includeFileExtensions = emptyList()
                )

        // When & Then
        assertThrows<IllegalArgumentException> { ScanEngine(invalidConfig) }
    }

    @Test
    fun `should scan directory and return results`() {
        // Given
        val testFile = createTestFile("TestFile.kt", "val apiKey = \"sk-1234567890abcdef\"")
        val expectedFindings =
                listOf(
                        ScanResult.Finding(
                                file = testFile.absolutePath,
                                line = 1,
                                column = 13,
                                pattern = "API Key Pattern",
                                match = "sk-1234567890abcdef",
                                severity = ScanResult.Severity.HIGH,
                                confidence = 0.95,
                                context = "val apiKey = \"sk-1234567890abcdef\""
                        )
                )

        every { mockFileScanner.scanFile(any()) } returns expectedFindings

        // When
        val result = scanEngine.scan(tempDir.toFile())

        // Then
        assertNotNull(result)
        assertEquals(1, result.totalFilesScanned)
        assertEquals(1, result.totalFindings)
        assertEquals(expectedFindings, result.findings)
        assertTrue(result.scanDuration > 0)
    }

    @Test
    fun `should scan multiple files and aggregate results`() {
        // Given
        val file1 = createTestFile("File1.kt", "val secret = \"password123\"")
        val file2 = createTestFile("File2.java", "String token = \"ghp_1234567890abcdef\";")

        val findings1 =
                listOf(
                        ScanResult.Finding(
                                file = file1.absolutePath,
                                line = 1,
                                column = 13,
                                pattern = "Password Pattern",
                                match = "password123",
                                severity = ScanResult.Severity.MEDIUM,
                                confidence = 0.8,
                                context = "val secret = \"password123\""
                        )
                )

        val findings2 =
                listOf(
                        ScanResult.Finding(
                                file = file2.absolutePath,
                                line = 1,
                                column = 15,
                                pattern = "GitHub Token Pattern",
                                match = "ghp_1234567890abcdef",
                                severity = ScanResult.Severity.HIGH,
                                confidence = 0.95,
                                context = "String token = \"ghp_1234567890abcdef\";"
                        )
                )

        every { mockFileScanner.scanFile(file1) } returns findings1
        every { mockFileScanner.scanFile(file2) } returns findings2

        // When
        val result = scanEngine.scan(tempDir.toFile())

        // Then
        assertEquals(2, result.totalFilesScanned)
        assertEquals(2, result.totalFindings)
        assertTrue(result.findings.containsAll(findings1 + findings2))
    }

    @Test
    fun `should apply file filters correctly`() {
        // Given
        val ktFile = createTestFile("Test.kt", "val secret = \"test\"")
        val txtFile = createTestFile("Test.txt", "secret = test")

        every { mockConfiguration.includeFileExtensions } returns listOf("kt")
        every { mockFileScanner.scanFile(ktFile) } returns emptyList()

        // When
        val result = scanEngine.scan(tempDir.toFile())

        // Then
        assertEquals(1, result.totalFilesScanned) // Only .kt file should be scanned
        verify(exactly = 1) { mockFileScanner.scanFile(ktFile) }
        verify(exactly = 0) { mockFileScanner.scanFile(txtFile) }
    }

    @Test
    fun `should respect exclude paths configuration`() {
        // Given
        val mainFile = createTestFile("main/Test.kt", "val secret = \"test\"")
        val testDir = tempDir.resolve("test").toFile()
        testDir.mkdirs()
        val testFile = File(testDir, "TestFile.kt").apply { writeText("val secret = \"test\"") }

        every { mockConfiguration.excludePaths } returns listOf("**/test/**")
        every { mockFileScanner.scanFile(mainFile) } returns emptyList()

        // When
        val result = scanEngine.scan(tempDir.toFile())

        // Then
        verify(exactly = 1) { mockFileScanner.scanFile(mainFile) }
        verify(exactly = 0) { mockFileScanner.scanFile(testFile) }
    }

    @Test
    fun `should handle file size limits`() {
        // Given
        val largeContent = "x".repeat(2 * 1024 * 1024) // 2MB content
        val largeFile = createTestFile("LargeFile.kt", largeContent)
        val smallFile = createTestFile("SmallFile.kt", "val x = 1")

        every { mockConfiguration.maxFileSize } returns 1024 * 1024 // 1MB limit
        every { mockFileScanner.scanFile(smallFile) } returns emptyList()

        // When
        val result = scanEngine.scan(tempDir.toFile())

        // Then
        verify(exactly = 1) { mockFileScanner.scanFile(smallFile) }
        verify(exactly = 0) { mockFileScanner.scanFile(largeFile) }
        assertTrue(result.skippedFiles.contains(largeFile.absolutePath))
    }

    @Test
    fun `should handle scan errors gracefully`() {
        // Given
        val testFile = createTestFile("Test.kt", "val secret = \"test\"")
        every { mockFileScanner.scanFile(testFile) } throws RuntimeException("Scan failed")

        // When
        val result = scanEngine.scan(tempDir.toFile())

        // Then
        assertEquals(0, result.totalFilesScanned)
        assertEquals(1, result.errors.size)
        assertContains(result.errors.first(), "Scan failed")
    }

    @Test
    fun `should generate scan summary with statistics`() {
        // Given
        val file1 = createTestFile("File1.kt", "content1")
        val file2 = createTestFile("File2.kt", "content2")

        val highSeverityFinding =
                ScanResult.Finding(
                        file = file1.absolutePath,
                        line = 1,
                        column = 1,
                        pattern = "High Pattern",
                        match = "high",
                        severity = ScanResult.Severity.HIGH,
                        confidence = 0.9,
                        context = "context"
                )

        val mediumSeverityFinding =
                ScanResult.Finding(
                        file = file2.absolutePath,
                        line = 1,
                        column = 1,
                        pattern = "Medium Pattern",
                        match = "medium",
                        severity = ScanResult.Severity.MEDIUM,
                        confidence = 0.8,
                        context = "context"
                )

        every { mockFileScanner.scanFile(file1) } returns listOf(highSeverityFinding)
        every { mockFileScanner.scanFile(file2) } returns listOf(mediumSeverityFinding)

        // When
        val result = scanEngine.scan(tempDir.toFile())

        // Then
        assertEquals(2, result.totalFilesScanned)
        assertEquals(2, result.totalFindings)

        val summary = result.summary
        assertEquals(1, summary.findingsBySeverity[ScanResult.Severity.HIGH])
        assertEquals(1, summary.findingsBySeverity[ScanResult.Severity.MEDIUM])
        assertEquals(0, summary.findingsBySeverity[ScanResult.Severity.LOW])
    }

    @Test
    fun `should handle empty directory scan`() {
        // Given
        val emptyDir = tempDir.resolve("empty").toFile()
        emptyDir.mkdirs()

        // When
        val result = scanEngine.scan(emptyDir)

        // Then
        assertEquals(0, result.totalFilesScanned)
        assertEquals(0, result.totalFindings)
        assertTrue(result.findings.isEmpty())
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `should scan with custom detector configuration`() {
        // Given
        val testFile = createTestFile("Test.kt", "val secret = \"test123\"")

        every { mockConfiguration.enablePatternDetection } returns true
        every { mockConfiguration.enableEntropyDetection } returns false
        every { mockConfiguration.enableContextAwareDetection } returns true

        val patternFinding =
                ScanResult.Finding(
                        file = testFile.absolutePath,
                        line = 1,
                        column = 13,
                        pattern = "Pattern Detection",
                        match = "test123",
                        severity = ScanResult.Severity.MEDIUM,
                        confidence = 0.7,
                        context = "val secret = \"test123\""
                )

        every { mockFileScanner.scanFile(testFile) } returns listOf(patternFinding)

        // When
        val result = scanEngine.scan(tempDir.toFile())

        // Then
        assertEquals(1, result.totalFindings)
        assertEquals("Pattern Detection", result.findings.first().pattern)
    }

    @Test
    fun `should track scan performance metrics`() {
        // Given
        val testFile = createTestFile("Test.kt", "val x = 1")
        every { mockFileScanner.scanFile(testFile) } returns emptyList()

        // When
        val startTime = System.currentTimeMillis()
        val result = scanEngine.scan(tempDir.toFile())
        val endTime = System.currentTimeMillis()

        // Then
        assertTrue(result.scanDuration > 0)
        assertTrue(result.scanDuration <= (endTime - startTime))
        assertNotNull(result.timestamp)
    }

    @Test
    fun `should validate scan paths exist`() {
        // Given
        val nonExistentPath = File("/non/existent/path")

        // When & Then
        assertThrows<IllegalArgumentException> { scanEngine.scan(nonExistentPath) }
    }

    @Test
    fun `should handle concurrent file scanning`() {
        // Given
        val files = (1..10).map { createTestFile("File$it.kt", "val secret$it = \"value$it\"") }

        every { mockFileScanner.scanFile(any()) } returns emptyList()

        // When
        val result = scanEngine.scan(tempDir.toFile())

        // Then
        assertEquals(10, result.totalFilesScanned)
        files.forEach { file -> verify(exactly = 1) { mockFileScanner.scanFile(file) } }
    }

    @Test
    fun `should group findings by file correctly`() {
        // Given
        val testFile = createTestFile("Test.kt", "multiple secrets here")

        val findings =
                listOf(
                        ScanResult.Finding(
                                file = testFile.absolutePath,
                                line = 1,
                                column = 1,
                                pattern = "Pattern1",
                                match = "secret1",
                                severity = ScanResult.Severity.HIGH,
                                confidence = 0.9,
                                context = "context1"
                        ),
                        ScanResult.Finding(
                                file = testFile.absolutePath,
                                line = 1,
                                column = 10,
                                pattern = "Pattern2",
                                match = "secret2",
                                severity = ScanResult.Severity.MEDIUM,
                                confidence = 0.8,
                                context = "context2"
                        )
                )

        every { mockFileScanner.scanFile(testFile) } returns findings

        // When
        val result = scanEngine.scan(tempDir.toFile())

        // Then
        assertEquals(1, result.totalFilesScanned)
        assertEquals(2, result.totalFindings)

        val fileFindings = result.findings.groupBy { it.file }
        assertEquals(1, fileFindings.keys.size)
        assertEquals(2, fileFindings[testFile.absolutePath]?.size)
    }

    @Nested
    inner class ConfigurationValidationTests {

        @Test
        fun `should validate minimum entropy threshold`() {
            // Given
            every { mockConfiguration.minEntropyThreshold } returns -1.0

            // When & Then
            assertThrows<IllegalArgumentException> { ScanEngine(mockConfiguration) }
        }

        @Test
        fun `should validate file extensions format`() {
            // Given
            every { mockConfiguration.includeFileExtensions } returns
                    listOf(".kt", "java") // Mixed formats

            // When
            val engine = ScanEngine(mockConfiguration)

            // Then
            // Should normalize extensions internally
            assertNotNull(engine)
        }

        @Test
        fun `should validate scan paths are not empty`() {
            // Given
            every { mockConfiguration.scanPaths } returns emptyList()

            // When & Then
            assertThrows<IllegalArgumentException> { ScanEngine(mockConfiguration) }
        }
    }

    @Nested
    inner class PerformanceTests {

        @Test
        fun `should complete scan within reasonable time for small project`() {
            // Given
            val files = (1..50).map { createTestFile("File$it.kt", "val value$it = \"test\"") }
            every { mockFileScanner.scanFile(any()) } returns emptyList()

            // When
            val startTime = System.currentTimeMillis()
            val result = scanEngine.scan(tempDir.toFile())
            val duration = System.currentTimeMillis() - startTime

            // Then
            assertTrue(duration < 5000) // Should complete within 5 seconds
            assertEquals(50, result.totalFilesScanned)
        }

        @Test
        fun `should handle large number of findings efficiently`() {
            // Given
            val testFile = createTestFile("Test.kt", "content")
            val manyFindings =
                    (1..1000).map { i ->
                        ScanResult.Finding(
                                file = testFile.absolutePath,
                                line = i,
                                column = 1,
                                pattern = "Pattern$i",
                                match = "match$i",
                                severity = ScanResult.Severity.LOW,
                                confidence = 0.5,
                                context = "context$i"
                        )
                    }

            every { mockFileScanner.scanFile(testFile) } returns manyFindings

            // When
            val result = scanEngine.scan(tempDir.toFile())

            // Then
            assertEquals(1000, result.totalFindings)
            assertEquals(1000, result.findings.size)
        }
    }

    private fun createTestFile(fileName: String, content: String): File {
        val file = tempDir.resolve(fileName).toFile()
        file.parentFile?.mkdirs()
        file.writeText(content)
        return file
    }
}
