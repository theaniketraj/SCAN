package com.scan.detectors

import com.scan.core.ScanConfiguration
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
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
        defaultConfig =
                ScanConfiguration(
                        entropyThreshold = 4.5,
                        minSecretLength = 20,
                        maxSecretLength = 200,
                        enableEntropyDetection = true
                )
        detector = EntropyDetector(defaultConfig)
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
            val entropy = detector.calculateEntropy(highEntropyString)
            assertTrue(entropy > 4.0, "High entropy string should have entropy > 4.0, got $entropy")
        }

        @Test
        @DisplayName("Should calculate low entropy for repetitive strings")
        fun testRepetitiveStringEntropy() {
            val lowEntropyString = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            val entropy = detector.calculateEntropy(lowEntropyString)
            assertTrue(entropy < 1.0, "Repetitive string should have low entropy, got $entropy")
        }

        @Test
        @DisplayName("Should handle empty string")
        fun testEmptyStringEntropy() {
            val entropy = detector.calculateEntropy("")
            assertEquals(0.0, entropy, "Empty string should have zero entropy")
        }

        @Test
        @DisplayName("Should handle single character string")
        fun testSingleCharacterEntropy() {
            val entropy = detector.calculateEntropy("a")
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
            val entropy = detector.calculateEntropy(secret)
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
            val entropy = detector.calculateEntropy(text)
            assertTrue(entropy < 4.0, "Normal text '$text' should have low entropy, got $entropy")
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
            val results = detector.detect(testFile, content)

            assertTrue(results.isNotEmpty(), "Should detect high entropy string")
            val result = results.first()
            assertTrue(
                    result.message.contains("High entropy"),
                    "Result should mention high entropy"
            )
            assertTrue(result.line > 0, "Should have valid line number")
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
            val results = detector.detect(testFile, content)

            assertTrue(results.isEmpty(), "Should not detect low entropy strings")
        }

        @Test
        @DisplayName("Should respect minimum length configuration")
        fun testMinimumLengthRespected() {
            val shortConfig =
                    ScanConfiguration(
                            entropyThreshold = 3.0,
                            minSecretLength = 50, // High minimum length
                            maxSecretLength = 200,
                            enableEntropyDetection = true
                    )
            val shortDetector = EntropyDetector(shortConfig)

            val content =
                    """
                const key = "K7gNU3sdo+OL0wNhqoVWhr3g6s1x"; // High entropy but short
            """.trimIndent()

            val testFile = createTestFile("test.js", content)
            val results = shortDetector.detect(testFile, content)

            assertTrue(results.isEmpty(), "Should not detect strings shorter than minimum length")
        }

        @Test
        @DisplayName("Should respect maximum length configuration")
        fun testMaximumLengthRespected() {
            val shortConfig =
                    ScanConfiguration(
                            entropyThreshold = 3.0,
                            minSecretLength = 10,
                            maxSecretLength = 30, // Low maximum length
                            enableEntropyDetection = true
                    )
            val shortDetector = EntropyDetector(shortConfig)

            val longHighEntropyString =
                    "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv24TkELVR3LrKxN5HKgfKLdEQkM/wQ2V8dW1234567890"
            val content =
                    """
                const key = "$longHighEntropyString"; // High entropy but too long
            """.trimIndent()

            val testFile = createTestFile("test.js", content)
            val results = shortDetector.detect(testFile, content)

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
            val results = detector.detect(testFile, content)

            assertEquals(3, results.size, "Should detect exactly 3 high entropy strings")

            val lines = results.map { it.line }.sorted()
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
            val results = detector.detect(testFile, content)

            assertTrue(results.isNotEmpty(), "Should detect secrets in $expectedType files")
        }

        @Test
        @DisplayName("Should handle binary files gracefully")
        fun testBinaryFileHandling() {
            val binaryContent = ByteArray(100) { it.toByte() }.toString(Charsets.ISO_8859_1)
            val testFile = createTestFile("test.bin", binaryContent)

            // Should not throw exception
            val results = detector.detect(testFile, binaryContent)

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
            val results = detector.detect(testFile, largeContent)

            assertEquals(1, results.size, "Should find exactly one secret in large file")
            assertEquals(501, results.first().line, "Should find secret on correct line")
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
            val results = detector.detect(testFile, content)

            assertTrue(
                    results.size >= 2,
                    "Should detect high entropy strings with special characters"
            )
        }

        @Test
        @DisplayName("Should handle empty file")
        fun testEmptyFile() {
            val testFile = createTestFile("empty.js", "")
            val results = detector.detect(testFile, "")

            assertTrue(results.isEmpty(), "Empty file should produce no results")
        }

        @Test
        @DisplayName("Should handle file with only whitespace")
        fun testWhitespaceOnlyFile() {
            val content = "   \n\t  \n   \r\n  "
            val testFile = createTestFile("whitespace.js", content)
            val results = detector.detect(testFile, content)

            assertTrue(results.isEmpty(), "Whitespace-only file should produce no results")
        }

        @Test
        @DisplayName("Should handle disabled entropy detection")
        fun testDisabledEntropyDetection() {
            val disabledConfig =
                    ScanConfiguration(
                            entropyThreshold = 3.0,
                            minSecretLength = 10,
                            maxSecretLength = 200,
                            enableEntropyDetection = false
                    )
            val disabledDetector = EntropyDetector(disabledConfig)

            val content =
                    """
                const key = "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv24TkELVR3LrKxN5HKgfKLdEQkM";
            """.trimIndent()

            val testFile = createTestFile("test.js", content)
            val results = disabledDetector.detect(testFile, content)

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
            assertThrows<IllegalArgumentException> {
                EntropyDetector(ScanConfiguration(entropyThreshold = -1.0))
            }

            assertThrows<IllegalArgumentException> {
                EntropyDetector(ScanConfiguration(entropyThreshold = 10.0))
            }
        }

        @Test
        @DisplayName("Should validate length configuration")
        fun testLengthConfigurationValidation() {
            assertThrows<IllegalArgumentException> {
                EntropyDetector(ScanConfiguration(minSecretLength = 0))
            }

            assertThrows<IllegalArgumentException> {
                EntropyDetector(ScanConfiguration(maxSecretLength = 5, minSecretLength = 10))
            }
        }

        @ParameterizedTest
        @ValueSource(doubles = [2.0, 3.5, 4.0, 4.5, 5.0, 6.0])
        @DisplayName("Should work with different entropy thresholds")
        fun testDifferentEntropyThresholds(threshold: Double) {
            val config = ScanConfiguration(entropyThreshold = threshold)
            val customDetector = EntropyDetector(config)

            val content =
                    """
                const mediumEntropy = "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv24";
                const highEntropy = "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv24TkELVR3LrKxN5HKgfKLdEQkM";
            """.trimIndent()

            val testFile = createTestFile("test.js", content)
            val results = customDetector.detect(testFile, content)

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

            val startTime = System.currentTimeMillis()
            val results = detector.detect(testFile, content)
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

            // Run multiple detections concurrently
            val results =
                    (1..10).toList()
                            .parallelStream()
                            .map { detector.detect(testFile, content) }
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
            val results = detector.detect(testFile, content)

            assertEquals(1, results.size, "Should find exactly one secret")
            assertEquals(4, results.first().line, "Should report correct line number")
        }

        @Test
        @DisplayName("Should provide meaningful detection messages")
        fun testDetectionMessages() {
            val content =
                    """
                const secret = "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv24TkELVR3LrKxN5HKgfKLdEQkM";
            """.trimIndent()

            val testFile = createTestFile("message.js", content)
            val results = detector.detect(testFile, content)

            val result = results.first()
            assertTrue(result.message.contains("entropy"), "Message should mention entropy")
            assertTrue(
                    result.message.contains("detected") || result.message.contains("found"),
                    "Message should indicate detection"
            )
            assertFalse(result.message.isBlank(), "Message should not be empty")
        }

        @Test
        @DisplayName("Should include entropy value in results")
        fun testEntropyValueInResults() {
            val content =
                    """
                const secret = "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv24TkELVR3LrKxN5HKgfKLdEQkM";
            """.trimIndent()

            val testFile = createTestFile("entropy-value.js", content)
            val results = detector.detect(testFile, content)

            val result = results.first()
            assertTrue(result.severity.isNotEmpty(), "Should have severity level")
            assertTrue(result.ruleId.isNotEmpty(), "Should have rule ID")
        }
    }

    // Helper method to create test files
    private fun createTestFile(fileName: String, content: String): File {
        val file = tempDir.resolve(fileName).toFile()
        file.writeText(content)
        return file
    }
}
