package com.scan.core

import com.scan.detectors.CompositeDetector
import com.scan.detectors.DetectorInterface
import com.scan.detectors.EntropyDetector
import com.scan.detectors.PatternDetector
import com.scan.filters.FileExtensionFilter
import com.scan.filters.FilterInterface
import com.scan.filters.PathFilter
import com.scan.filters.WhitelistFilter
import com.scan.patterns.SecretPatterns
import io.mockk.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileScannerTest {

    @TempDir lateinit var tempDir: Path

    private lateinit var fileScanner: FileScanner
    private lateinit var mockDetector: DetectorInterface
    private lateinit var mockFilter: FilterInterface
    private lateinit var configuration: ScanConfiguration

    @BeforeEach
    fun setUp() {
        mockDetector = mockk()
        mockFilter = mockk()

        configuration =
                ScanConfiguration(
                        detectors = listOf(mockDetector),
                        filters = listOf(mockFilter),
                        maxFileSize = 1024 * 1024, // 1MB
                        includeHidden = false,
                        followSymlinks = false,
                        parallelProcessing = false,
                        maxThreads = 1
                )

        fileScanner = FileScanner(configuration)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Nested
    @DisplayName("File Scanning Tests")
    inner class FileScanningTests {

        @Test
        @DisplayName("Should scan file with secrets and return results")
        fun testScanFileWithSecrets() {
            // Arrange
            val testFile = tempDir.resolve("test-file.kt")
            val fileContent =
                    """
                class TestClass {
                    private val apiKey = "sk-1234567890abcdef"
                    private val password = "my-secret-password"
                }
            """.trimIndent()

            testFile.writeText(fileContent)

            val expectedResults =
                    listOf(
                            ScanResult(
                                    file = testFile.toFile(),
                                    lineNumber = 2,
                                    columnStart = 25,
                                    columnEnd = 45,
                                    content = "sk-1234567890abcdef",
                                    ruleId = "api-key-pattern",
                                    severity = ScanResult.Severity.HIGH,
                                    message = "Potential API key detected"
                            ),
                            ScanResult(
                                    file = testFile.toFile(),
                                    lineNumber = 3,
                                    columnStart = 25,
                                    columnEnd = 45,
                                    content = "my-secret-password",
                                    ruleId = "password-pattern",
                                    severity = ScanResult.Severity.MEDIUM,
                                    message = "Potential password detected"
                            )
                    )

            every { mockFilter.shouldInclude(any()) } returns true
            every { mockDetector.detect(any(), any()) } returns expectedResults

            // Act
            val results = fileScanner.scanFile(testFile.toFile())

            // Assert
            assertEquals(2, results.size)
            assertEquals(expectedResults[0].content, results[0].content)
            assertEquals(expectedResults[1].content, results[1].content)

            verify { mockFilter.shouldInclude(testFile.toFile()) }
            verify { mockDetector.detect(testFile.toFile(), fileContent) }
        }

        @Test
        @DisplayName("Should return empty results for clean file")
        fun testScanCleanFile() {
            // Arrange
            val testFile = tempDir.resolve("clean-file.kt")
            val fileContent =
                    """
                class CleanClass {
                    private val normalVariable = "normal-value"
                    fun doSomething() = println("Hello World")
                }
            """.trimIndent()

            testFile.writeText(fileContent)

            every { mockFilter.shouldInclude(any()) } returns true
            every { mockDetector.detect(any(), any()) } returns emptyList()

            // Act
            val results = fileScanner.scanFile(testFile.toFile())

            // Assert
            assertTrue(results.isEmpty())
            verify { mockDetector.detect(testFile.toFile(), fileContent) }
        }

        @Test
        @DisplayName("Should skip file if filter excludes it")
        fun testSkipFilteredFile() {
            // Arrange
            val testFile = tempDir.resolve("filtered-file.kt")
            testFile.writeText("some content")

            every { mockFilter.shouldInclude(any()) } returns false

            // Act
            val results = fileScanner.scanFile(testFile.toFile())

            // Assert
            assertTrue(results.isEmpty())
            verify { mockFilter.shouldInclude(testFile.toFile()) }
            verify(exactly = 0) { mockDetector.detect(any(), any()) }
        }

        @Test
        @DisplayName("Should handle non-existent file gracefully")
        fun testScanNonExistentFile() {
            // Arrange
            val nonExistentFile = File(tempDir.toFile(), "non-existent.kt")

            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                fileScanner.scanFile(nonExistentFile)
            }
        }

        @Test
        @DisplayName("Should handle empty file")
        fun testScanEmptyFile() {
            // Arrange
            val emptyFile = tempDir.resolve("empty-file.kt")
            emptyFile.writeText("")

            every { mockFilter.shouldInclude(any()) } returns true
            every { mockDetector.detect(any(), any()) } returns emptyList()

            // Act
            val results = fileScanner.scanFile(emptyFile.toFile())

            // Assert
            assertTrue(results.isEmpty())
            verify { mockDetector.detect(emptyFile.toFile(), "") }
        }
    }

    @Nested
    @DisplayName("File Processing Tests")
    inner class FileProcessingTests {

        @Test
        @DisplayName("Should handle large files within size limit")
        fun testScanLargeFileWithinLimit() {
            // Arrange
            val largeFile = tempDir.resolve("large-file.kt")
            val content = "x".repeat(1024 * 512) // 512KB - within 1MB limit
            largeFile.writeText(content)

            every { mockFilter.shouldInclude(any()) } returns true
            every { mockDetector.detect(any(), any()) } returns emptyList()

            // Act
            val results = fileScanner.scanFile(largeFile.toFile())

            // Assert
            assertTrue(results.isEmpty())
            verify { mockDetector.detect(largeFile.toFile(), content) }
        }

        @Test
        @DisplayName("Should skip files exceeding size limit")
        fun testSkipFilesExceedingSizeLimit() {
            // Arrange
            val oversizedFile = tempDir.resolve("oversized-file.kt")
            val content = "x".repeat(1024 * 1024 + 1) // 1MB + 1 byte
            oversizedFile.writeText(content)

            every { mockFilter.shouldInclude(any()) } returns true

            // Act
            val results = fileScanner.scanFile(oversizedFile.toFile())

            // Assert
            assertTrue(results.isEmpty())
            verify { mockFilter.shouldInclude(oversizedFile.toFile()) }
            verify(exactly = 0) { mockDetector.detect(any(), any()) }
        }

        @Test
        @DisplayName("Should handle files with special characters")
        fun testScanFileWithSpecialCharacters() {
            // Arrange
            val specialFile = tempDir.resolve("special-chars.kt")
            val content =
                    """
                val unicode = "Hello 世界 🌍"
                val special = "Café naïve résumé"
                val symbols = "¡¢£¤¥¦§¨©ª«¬®¯°±²³´µ¶·¸¹º»¼½¾¿"
            """.trimIndent()

            specialFile.writeText(content)

            every { mockFilter.shouldInclude(any()) } returns true
            every { mockDetector.detect(any(), any()) } returns emptyList()

            // Act
            val results = fileScanner.scanFile(specialFile.toFile())

            // Assert
            assertTrue(results.isEmpty())
            verify { mockDetector.detect(specialFile.toFile(), content) }
        }

        @Test
        @DisplayName("Should handle binary files gracefully")
        fun testScanBinaryFile() {
            // Arrange
            val binaryFile = tempDir.resolve("binary-file.jar")
            val binaryContent = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0x00) // ZIP header
            Files.write(binaryFile, binaryContent)

            every { mockFilter.shouldInclude(any()) } returns true
            every { mockDetector.detect(any(), any()) } returns emptyList()

            // Act
            val results = fileScanner.scanFile(binaryFile.toFile())

            // Assert
            assertTrue(results.isEmpty())
            verify { mockDetector.detect(eq(binaryFile.toFile()), any()) }
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    inner class IntegrationTests {

        @Test
        @DisplayName("Should work with real pattern detector")
        fun testIntegrationWithPatternDetector() {
            // Arrange
            val realDetector = PatternDetector(SecretPatterns.getAllPatterns())
            val realFilter = FileExtensionFilter(listOf(".kt", ".java"))
            val realConfig =
                    ScanConfiguration(
                            detectors = listOf(realDetector),
                            filters = listOf(realFilter),
                            maxFileSize = 1024 * 1024,
                            includeHidden = false,
                            followSymlinks = false,
                            parallelProcessing = false,
                            maxThreads = 1
                    )

            val realFileScanner = FileScanner(realConfig)

            val testFile = tempDir.resolve("integration-test.kt")
            val fileContent =
                    """
                class ApiService {
                    private val apiKey = "sk-1234567890abcdefghijklmnopqrstuvwxyz"
                    private val awsSecret = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
                    private val githubToken = "ghp_1234567890abcdefghijklmnopqrstuvwxyz"
                }
            """.trimIndent()

            testFile.writeText(fileContent)

            // Act
            val results = realFileScanner.scanFile(testFile.toFile())

            // Assert
            assertFalse(results.isEmpty())
            assertTrue(results.any { it.content.contains("sk-") })
            assertTrue(results.any { it.content.contains("wJalrXUtnFEMI") })
            assertTrue(results.any { it.content.contains("ghp_") })
        }

        @Test
        @DisplayName("Should work with entropy detector")
        fun testIntegrationWithEntropyDetector() {
            // Arrange
            val entropyDetector = EntropyDetector(threshold = 4.5)
            val realConfig =
                    ScanConfiguration(
                            detectors = listOf(entropyDetector),
                            filters = emptyList(),
                            maxFileSize = 1024 * 1024,
                            includeHidden = false,
                            followSymlinks = false,
                            parallelProcessing = false,
                            maxThreads = 1
                    )

            val realFileScanner = FileScanner(realConfig)

            val testFile = tempDir.resolve("entropy-test.kt")
            val fileContent =
                    """
                class Config {
                    val lowEntropy = "password123"
                    val highEntropy = "X9k2mN8qP4wR7tY5uI3oL6aS1dF0gH"
                }
            """.trimIndent()

            testFile.writeText(fileContent)

            // Act
            val results = realFileScanner.scanFile(testFile.toFile())

            // Assert
            assertFalse(results.isEmpty())
            assertTrue(results.any { it.content.contains("X9k2mN8qP4wR7tY5uI3oL6aS1dF0gH") })
        }

        @Test
        @DisplayName("Should work with composite detector")
        fun testIntegrationWithCompositeDetector() {
            // Arrange
            val patternDetector = PatternDetector(SecretPatterns.getAllPatterns())
            val entropyDetector = EntropyDetector(threshold = 4.0)
            val compositeDetector = CompositeDetector(listOf(patternDetector, entropyDetector))

            val realConfig =
                    ScanConfiguration(
                            detectors = listOf(compositeDetector),
                            filters = emptyList(),
                            maxFileSize = 1024 * 1024,
                            includeHidden = false,
                            followSymlinks = false,
                            parallelProcessing = false,
                            maxThreads = 1
                    )

            val realFileScanner = FileScanner(realConfig)

            val testFile = tempDir.resolve("composite-test.kt")
            val fileContent =
                    """
                class SecurityTest {
                    val apiKey = "sk-1234567890abcdef" // Pattern match
                    val randomSecret = "A7k9mN2qP8wR3tY6uI5oL1aS4dF0gH" // High entropy
                    val normal = "normalValue" // Should not match
                }
            """.trimIndent()

            testFile.writeText(fileContent)

            // Act
            val results = realFileScanner.scanFile(testFile.toFile())

            // Assert
            assertTrue(results.size >= 2) // At least pattern and entropy matches
            assertTrue(results.any { it.content.contains("sk-") })
            assertTrue(results.any { it.content.contains("A7k9mN2qP8wR3tY6uI5oL1aS4dF0gH") })
            assertFalse(results.any { it.content.contains("normalValue") })
        }

        @Test
        @DisplayName("Should work with multiple filters")
        fun testIntegrationWithMultipleFilters() {
            // Arrange
            val detector = PatternDetector(SecretPatterns.getAllPatterns())
            val extensionFilter = FileExtensionFilter(listOf(".kt"))
            val pathFilter = PathFilter(excludePatterns = listOf("**/test/**"))
            val whitelistFilter = WhitelistFilter(listOf("normalValue"))

            val realConfig =
                    ScanConfiguration(
                            detectors = listOf(detector),
                            filters = listOf(extensionFilter, pathFilter, whitelistFilter),
                            maxFileSize = 1024 * 1024,
                            includeHidden = false,
                            followSymlinks = false,
                            parallelProcessing = false,
                            maxThreads = 1
                    )

            val realFileScanner = FileScanner(realConfig)

            // Create test file in main directory
            val mainFile = tempDir.resolve("main-file.kt")
            mainFile.writeText("val secret = \"sk-1234567890abcdef\"")

            // Create test file in test directory
            val testDir = tempDir.resolve("test")
            Files.createDirectory(testDir)
            val testFile = testDir.resolve("test-file.kt")
            testFile.writeText("val secret = \"sk-1234567890abcdef\"")

            // Act
            val mainResults = realFileScanner.scanFile(mainFile.toFile())
            val testResults = realFileScanner.scanFile(testFile.toFile())

            // Assert
            assertFalse(mainResults.isEmpty()) // Main file should be scanned
            assertTrue(testResults.isEmpty()) // Test file should be filtered out
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle detector exceptions gracefully")
        fun testHandleDetectorException() {
            // Arrange
            val testFile = tempDir.resolve("error-test.kt")
            testFile.writeText("some content")

            every { mockFilter.shouldInclude(any()) } returns true
            every { mockDetector.detect(any(), any()) } throws RuntimeException("Detector error")

            // Act & Assert
            assertThrows(RuntimeException::class.java) { fileScanner.scanFile(testFile.toFile()) }
        }

        @Test
        @DisplayName("Should handle filter exceptions gracefully")
        fun testHandleFilterException() {
            // Arrange
            val testFile = tempDir.resolve("filter-error-test.kt")
            testFile.writeText("some content")

            every { mockFilter.shouldInclude(any()) } throws RuntimeException("Filter error")

            // Act & Assert
            assertThrows(RuntimeException::class.java) { fileScanner.scanFile(testFile.toFile()) }
        }

        @Test
        @DisplayName("Should handle file read permission errors")
        fun testHandleFileReadPermissionError() {
            // Arrange
            val testFile = tempDir.resolve("permission-test.kt")
            testFile.writeText("some content")

            // Make file unreadable (Unix-like systems)
            testFile.toFile().setReadable(false)

            every { mockFilter.shouldInclude(any()) } returns true

            // Act & Assert
            if (System.getProperty("os.name").lowercase().contains("windows")) {
                // Windows doesn't support file permissions the same way
                // Skip this test or handle differently
                return
            }

            assertThrows(Exception::class.java) { fileScanner.scanFile(testFile.toFile()) }

            // Cleanup
            testFile.toFile().setReadable(true)
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    inner class PerformanceTests {

        @Test
        @DisplayName("Should complete scan within reasonable time")
        fun testScanPerformance() {
            // Arrange
            val testFile = tempDir.resolve("performance-test.kt")
            val content =
                    """
                class LargeClass {
                    ${(1..1000).joinToString("\n    ") { "val field$it = \"value$it\"" }}
                }
            """.trimIndent()

            testFile.writeText(content)

            every { mockFilter.shouldInclude(any()) } returns true
            every { mockDetector.detect(any(), any()) } returns emptyList()

            // Act
            val startTime = System.currentTimeMillis()
            val results = fileScanner.scanFile(testFile.toFile())
            val endTime = System.currentTimeMillis()

            // Assert
            assertTrue(results.isEmpty())
            assertTrue(endTime - startTime < 5000) // Should complete within 5 seconds
        }

        @Test
        @DisplayName("Should handle multiple scans efficiently")
        fun testMultipleScanPerformance() {
            // Arrange
            val testFiles =
                    (1..10).map { index ->
                        val file = tempDir.resolve("multi-test-$index.kt")
                        file.writeText("class Test$index { val value = \"test$index\" }")
                        file.toFile()
                    }

            every { mockFilter.shouldInclude(any()) } returns true
            every { mockDetector.detect(any(), any()) } returns emptyList()

            // Act
            val startTime = System.currentTimeMillis()
            testFiles.forEach { file -> fileScanner.scanFile(file) }
            val endTime = System.currentTimeMillis()

            // Assert
            assertTrue(endTime - startTime < 10000) // Should complete within 10 seconds
            verify(exactly = 10) { mockDetector.detect(any(), any()) }
        }
    }
}
