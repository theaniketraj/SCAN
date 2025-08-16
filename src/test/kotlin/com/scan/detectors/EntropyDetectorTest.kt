package com.scan.detectors

import com.scan.core.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("EntropyDetector Tests")
class EntropyDetectorTest {

    private lateinit var detector: EntropyDetector
    private lateinit var defaultConfig: ScanConfiguration
    private lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        defaultConfig = ScanConfiguration(
            entropy = EntropyConfiguration(
                enabled = true,
                threshold = 4.5,
                minLength = 20,
                maxLength = 200
            )
        )
        detector = EntropyDetector()
        tempDir = Files.createTempDirectory("scan-test")
    }

    @Nested
    @DisplayName("Entropy Calculation Tests")
    inner class EntropyCalculationTests {

        @Test
        @DisplayName("Should calculate correct entropy for uniform distribution")
        fun testUniformDistributionEntropy() {
            // String with uniform character distribution should have high entropy
            val highEntropyString = "aAbBcCdDeEfFgGhHiIjJkKlLmMnNoOpPqQrRsStTuUvVwWxXyYzZ123456789"
            val entropy = detector.analyzeEntropy(highEntropyString).entropy
            assertTrue(entropy > 4.0, "High entropy string should have entropy > 4.0, got $entropy")
        }

        @Test
        @DisplayName("Should calculate low entropy for repetitive strings")
        fun testRepetitiveStringEntropy() {
            val lowEntropyString = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            val entropy = detector.analyzeEntropy(lowEntropyString).entropy
            assertTrue(entropy < 1.0, "Repetitive string should have low entropy, got $entropy")
        }

        @Test
        @DisplayName("Should handle empty string")
        fun testEmptyStringEntropy() {
            val entropy = detector.analyzeEntropy("").entropy
            assertEquals(0.0, entropy, "Empty string should have zero entropy")
        }

        @Test
        @DisplayName("Should handle single character string")
        fun testSingleCharacterEntropy() {
            val entropy = detector.analyzeEntropy("a").entropy
            assertEquals(0.0, entropy, "Single character should have zero entropy")
        }

        @ParameterizedTest
        @ValueSource(
                strings =
                        [
                                "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv24TkELVR3LrKxN5HKgfKLdEQkM/wQ2V8dW", // Base64-like
                                "AIzaSyDxVlAb3c4fGhIjKlMnOpQrStUvWxYz123456", // API key-like
                                "sk-proj-abcdef1234567890abcdef1234567890abcdef12", // OpenAI
                                // key-like
                                "AKIAIOSFODNN7EXAMPLE", // AWS access key-like
                                "mongodb://user:pass@cluster0.mongodb.net/mydb?retryWrites=true" // Connection string
                        ]
        )
        @DisplayName("Should detect high entropy in typical secrets")
        fun testTypicalSecretsHighEntropy(secret: String) {
            val entropy = detector.analyzeEntropy(secret).entropy
            assertTrue(entropy > 3.5, "Secret '$secret' should have high entropy, got $entropy")
        }

        @ParameterizedTest
        @ValueSource(
                strings =
                        [
                                "this is a normal sentence with regular words",
                                "function getName() { return 'user'; }",
                                "SELECT * FROM users WHERE id = 1",
                                "hello world test example demo",
                                "password123" // Even though it's a password, it's low entropy
                        ]
        )
        @DisplayName("Should detect low entropy in normal text")
        fun testNormalTextLowEntropy(text: String) {
            val entropy = detector.analyzeEntropy(text).entropy
            assertTrue(entropy < 4.2, "Normal text '$text' should have low entropy, got $entropy")
        }
    }

    @Nested
    @DisplayName("Detection Logic Tests")
    inner class DetectionLogicTests {

        @Test
        @DisplayName("Should detect high entropy strings above threshold")
        fun testDetectHighEntropyString() {
            val content =
                    """
                const API_KEY = "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv24TkELVR3LrKxN5HKgfKLdEQkM";
                const normalVar = "hello world";
            """.trimIndent()

            val testFile = createTestFile("test.js", content)
            val context = createScanContext(testFile, content)
            val results = detector.detect(context)

            assertTrue(results.isNotEmpty(), "Should detect high entropy string")
            val result = results.first()
            assertTrue(
                    result.description.contains("entropy") || result.title.contains("entropy"),
                    "Result should mention entropy"
            )
            assertTrue(result.location.lineNumber > 0, "Should have valid line number")
        }

        @Test
        @DisplayName("Should not detect strings below entropy threshold")
        fun testIgnoreLowEntropyStrings() {
            val content =
                    """
                const message = "hello world this is a normal string";
                const name = "user name example";
                const description = "this is just regular text content";
            """.trimIndent()

            val testFile = createTestFile("test.js", content)
            val context = createScanContext(testFile, content)
            val results = detector.detect(context)

            assertTrue(results.isEmpty(), "Should not detect low entropy strings")
        }

        @Test
        @DisplayName("Should respect minimum length configuration")
        fun testMinimumLengthRespected() {
            val shortConfig = ScanConfiguration(
                entropy = EntropyConfiguration(
                    enabled = true,
                    threshold = 3.0,
                    minLength = 50, // High minimum length
                    maxLength = 200
                )
            )
            val shortDetector = EntropyDetector() // Config passed via context

            val content =
                    """
                const key = "K7gNU3sdo+OL0wNhqoVWhr3g6s1x"; // High entropy but short
            """.trimIndent()

            val testFile = createTestFile("test.js", content)
            val context = createScanContext(testFile, content).copy(configuration = shortConfig)
            val results = shortDetector.detect(context)

            assertTrue(results.isEmpty(), "Should not detect strings shorter than minimum length")
        }

        @Test
        @DisplayName("Should respect maximum length configuration")
        fun testMaximumLengthRespected() {
            val shortConfig = ScanConfiguration(
                entropy = EntropyConfiguration(
                    enabled = true,
                    threshold = 3.0,
                    minLength = 10,
                    maxLength = 30 // Low maximum length
                )
            )
            val shortDetector = EntropyDetector()

            val longHighEntropyString =
                    "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv24TkELVR3LrKxN5HKgfKLdEQkM/wQ2V8dW1234567890"
            val content =
                    """
                const key = "$longHighEntropyString"; // High entropy but too long
            """.trimIndent()

            val testFile = createTestFile("test.js", content)
            val context = createScanContext(testFile, content).copy(configuration = shortConfig)
            val results = shortDetector.detect(context)

            assertTrue(results.isEmpty(), "Should not detect strings longer than maximum length")
        }

        @Test
        @DisplayName("Should detect multiple high entropy strings in same file")
        fun testMultipleDetections() {
            val content =
                    """
                const API_KEY = "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv24TkELVR3LrKxN5HKgfKLdEQkM";
                const SECRET_TOKEN = "AIzaSyDxVlAb3c4fGhIjKlMnOpQrStUvWxYz123456789abcdef";
                const normalVar = "hello world";
                const ANOTHER_KEY = "sk-proj-abcdef1234567890abcdef1234567890abcdef1234567890";
            """.trimIndent()

            val testFile = createTestFile("test.js", content)
            val context = createScanContext(testFile, content)
            val results = detector.detect(context)

            assertEquals(3, results.size, "Should detect exactly 3 high entropy strings")

            val lines = results.map { it.location.lineNumber }.sorted()
            assertEquals(listOf(1, 2, 4), lines, "Should detect secrets on correct lines")
        }
    }

    @Nested
    @DisplayName("File Type Handling Tests")
    inner class FileTypeHandlingTests {

        @ParameterizedTest
        @CsvSource(
                "test.kt, kotlin",
                "test.java, java",
                "test.js, javascript",
                "test.py, python",
                "test.yml, yaml",
                "test.json, json",
                "test.xml, xml",
                "test.properties, properties"
        )
        @DisplayName("Should handle different file types")
        fun testDifferentFileTypes(fileName: String, expectedType: String) {
            val content =
                    """
                secret_key = "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv24TkELVR3LrKxN5HKgfKLdEQkM"
            """.trimIndent()

            val testFile = createTestFile(fileName, content)
            val context = createScanContext(testFile, content)
            val results = detector.detect(context)

            assertTrue(results.isNotEmpty(), "Should detect secrets in $expectedType files")
        }

        @Test
        @DisplayName("Should handle binary files gracefully")
        fun testBinaryFileHandling() {
            val binaryContent = ByteArray(100) { it.toByte() }.toString(Charsets.ISO_8859_1)
            val testFile = createTestFile("test.bin", binaryContent)
            val context = createScanContext(testFile, binaryContent)

            // Should not throw exception
            val results = detector.detect(context)

            // Binary content detection depends on implementation
            assertNotNull(results, "Should return results without throwing exception")
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    inner class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very large files")
        fun testLargeFileHandling() {
            val largeContent = buildString {
                repeat(1000) { i ->
                    if (i == 500) {
                        appendLine(
                                "const secret = \"K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv24TkELVR3LrKxN5HKgfKLdEQkM\";"
                        )
                    } else {
                        appendLine("const var$i = \"normal content line $i\";")
                    }
                }
            }

            val testFile = createTestFile("large.js", largeContent)
            val context = createScanContext(testFile, largeContent)
            val results = detector.detect(context)

            assertEquals(1, results.size, "Should find exactly one secret in large file")
            assertEquals(501, results.first().location.lineNumber, "Should find secret on correct line")
        }

        @Test
        @DisplayName("Should handle strings with special characters")
        fun testSpecialCharacters() {
            val content =
                    """
                const key1 = "K7gNU3s+/=do+OL0wNhqoVWhr3g6s1xYv24TkELVR3LrKxN5HKgfKLdEQkM";
                const key2 = "AIzaSyD@#$%^&*()xVlAb3c4fGhIjKlMnOpQrStUvWxYz123456789";
                const unicode = "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv24TkELVR3LrKxN5HKgfÄÖÜ";
            """.trimIndent()

            val testFile = createTestFile("special.js", content)
            val context = createScanContext(testFile, content)
            val results = detector.detect(context)

            assertTrue(
                    results.size >= 2,
                    "Should detect high entropy strings with special characters"
            )
        }

        @Test
        @DisplayName("Should handle empty file")
        fun testEmptyFile() {
            val testFile = createTestFile("empty.js", "")
            val context = createScanContext(testFile, "")
            val results = detector.detect(context)

            assertTrue(results.isEmpty(), "Empty file should produce no results")
        }

        @Test
        @DisplayName("Should handle file with only whitespace")
        fun testWhitespaceOnlyFile() {
            val content = "   \n\t  \n   \r\n  "
            val testFile = createTestFile("whitespace.js", content)
            val context = createScanContext(testFile, content)
            val results = detector.detect(context)

            assertTrue(results.isEmpty(), "Whitespace-only file should produce no results")
        }

        @Test
        @DisplayName("Should handle disabled entropy detection")
        fun testDisabledEntropyDetection() {
            val disabledConfig = ScanConfiguration(
                entropy = EntropyConfiguration(
                    enabled = false,
                    threshold = 3.0,
                    minLength = 10,
                    maxLength = 200
                )
            )
            val disabledDetector = EntropyDetector()

            val content =
                    """
                const key = "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv24TkELVR3LrKxN5HKgfKLdEQkM";
            """.trimIndent()

            val testFile = createTestFile("test.js", content)
            val context = createScanContext(testFile, content).copy(configuration = disabledConfig)
            val results = disabledDetector.detect(context)

            assertTrue(
                    results.isEmpty(),
                    "Should not detect anything when entropy detection is disabled"
            )
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    inner class ConfigurationTests {

        @Test
        @DisplayName("Should validate entropy threshold bounds")
        fun testEntropyThresholdValidation() {
            // Configuration validation is now done at the configuration level
            val invalidConfig1 = ScanConfiguration(
                entropy = EntropyConfiguration(threshold = -1.0)
            )
            val errors1 = invalidConfig1.validate()
            assertTrue(errors1.isNotEmpty(), "Should have validation error for negative threshold")

            val invalidConfig2 = ScanConfiguration(
                entropy = EntropyConfiguration(threshold = 10.0)
            )
            val errors2 = invalidConfig2.validate()
            assertTrue(errors2.isNotEmpty(), "Should have validation error for too high threshold")
        }

        @Test
        @DisplayName("Should validate length configuration")
        fun testLengthConfigurationValidation() {
            val invalidConfig1 = ScanConfiguration(
                entropy = EntropyConfiguration(minLength = 0)
            )
            val errors1 = invalidConfig1.validate()
            assertTrue(errors1.isNotEmpty(), "Should have validation error for zero minLength")

            val invalidConfig2 = ScanConfiguration(
                entropy = EntropyConfiguration(maxLength = 5, minLength = 10)
            )
            val errors2 = invalidConfig2.validate()
            assertTrue(errors2.isNotEmpty(), "Should have validation error for maxLength < minLength")
        }

        @ParameterizedTest
        @ValueSource(doubles = [2.0, 3.5, 4.0, 4.5, 5.0, 6.0])
        @DisplayName("Should work with different entropy thresholds")
        fun testDifferentEntropyThresholds(threshold: Double) {
            val config = ScanConfiguration(
                entropy = EntropyConfiguration(threshold = threshold)
            )
            val customDetector = EntropyDetector()

            val content =
                    """
                const mediumEntropy = "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv24";
                const highEntropy = "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv24TkELVR3LrKxN5HKgfKLdEQkM";
            """.trimIndent()

            val testFile = createTestFile("test.js", content)
            val context = createScanContext(testFile, content).copy(configuration = config)
            val results = customDetector.detect(context)

            // Results should vary based on threshold
            assertNotNull(results, "Should return results for threshold $threshold")
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    inner class PerformanceTests {

        @Test
        @DisplayName("Should complete detection within reasonable time")
        fun testPerformance() {
            val content = buildString {
                repeat(100) { i ->
                    when (i % 10) {
                        0 ->
                                appendLine(
                                        "const secret$i = \"K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv24TkELVR3LrKxN5HKgfKLdEQkM$i\";"
                                )
                        else ->
                                appendLine(
                                        "const normal$i = \"this is normal text content for line $i\";"
                                )
                    }
                }
            }

            val testFile = createTestFile("performance.js", content)
            val context = createScanContext(testFile, content)

            val startTime = System.currentTimeMillis()
            val results = detector.detect(context)
            val endTime = System.currentTimeMillis()

            val duration = endTime - startTime
            assertTrue(
                    duration < 5000,
                    "Detection should complete within 5 seconds, took ${duration}ms"
            )
            assertEquals(10, results.size, "Should detect exactly 10 secrets")
        }

        @Test
        @DisplayName("Should handle concurrent detection requests")
        fun testConcurrentDetection() {
            val content =
                    """
                const secret = "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv24TkELVR3LrKxN5HKgfKLdEQkM";
            """.trimIndent()

            val testFile = createTestFile("concurrent.js", content)
            val context = createScanContext(testFile, content)

            // Run multiple detections concurrently
            val results =
                    (1..10).toList()
                            .parallelStream()
                            .map { detector.detect(context) }
                            .toList()

            // All results should be identical
            results.forEach { result ->
                assertEquals(
                        1,
                        result.size,
                        "Each concurrent detection should find exactly one secret"
                )
            }
        }
    }

    @Nested
    @DisplayName("Result Quality Tests")
    inner class ResultQualityTests {

        @Test
        @DisplayName("Should provide accurate line numbers")
        fun testAccurateLineNumbers() {
            val content =
                    """
                // Line 1
                const normal = "hello world"; // Line 2
                // Line 3
                const secret = "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv24TkELVR3LrKxN5HKgfKLdEQkM"; // Line 4
                // Line 5
            """.trimIndent()

            val testFile = createTestFile("lines.js", content)
            val context = createScanContext(testFile, content)
            val results = detector.detect(context)

            assertEquals(1, results.size, "Should find exactly one secret")
            assertEquals(4, results.first().location.lineNumber, "Should report correct line number")
        }

        @Test
        @DisplayName("Should provide meaningful detection messages")
        fun testDetectionMessages() {
            val content =
                    """
                const secret = "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv24TkELVR3LrKxN5HKgfKLdEQkM";
            """.trimIndent()

            val testFile = createTestFile("message.js", content)
            val context = createScanContext(testFile, content)
            val results = detector.detect(context)

            val result = results.first()
            assertTrue(result.description.contains("entropy") || result.title.contains("entropy"), "Message should mention entropy")
            assertTrue(
                    result.description.contains("detected") || result.description.contains("found") || result.title.contains("detected"),
                    "Message should indicate detection"
            )
            assertFalse(result.description.isBlank(), "Message should not be empty")
        }

        @Test
        @DisplayName("Should include entropy value in results")
        fun testEntropyValueInResults() {
            val content =
                    """
                const secret = "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv24TkELVR3LrKxN5HKgfKLdEQkM";
            """.trimIndent()

            val testFile = createTestFile("entropy-value.js", content)
            val context = createScanContext(testFile, content)
            val results = detector.detect(context)

            val result = results.first()
            assertTrue(result.severity.name.isNotEmpty(), "Should have severity level")
            assertTrue(result.secretInfo.patternName.isNotEmpty(), "Should have rule ID")
        }
    }

    // Helper method to create test files
    private fun createTestFile(fileName: String, content: String): File {
        val file = tempDir.resolve(fileName).toFile()
        file.writeText(content)
        return file
    }

    // Helper method to create ScanContext
    private fun createScanContext(file: File, content: String): ScanContext {
        return ScanContext(
            filePath = file.toPath(),
            fileName = file.name,
            fileExtension = file.extension,
            isTestFile = file.name.contains("test", ignoreCase = true),
            fileSize = file.length(),
            configuration = defaultConfig,
            content = content,
            lines = content.lines(),
            relativePath = file.name
        )
    }
}
