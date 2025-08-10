package com.scan.detectors

import com.scan.core.*
import com.scan.patterns.ApiKeyPatterns
import com.scan.patterns.CryptoPatterns
import com.scan.patterns.DatabasePatterns
import com.scan.patterns.SecretPatterns
import java.io.File
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PatternDetectorTest {

    @TempDir lateinit var tempDir: Path

    private lateinit var patternDetector: PatternDetector
    private lateinit var testFile: File

    @BeforeEach
    fun setUp() {
        val patterns =
                mapOf(
                        "api-key" to
                                listOf(Regex("sk-[a-zA-Z0-9]{32}"), Regex("pk-[a-zA-Z0-9]{32}")),
                        "aws-secret" to listOf(Regex("[A-Za-z0-9/+=]{40}")),
                        "github-token" to
                                listOf(
                                        Regex("ghp_[A-Za-z0-9]{36}"),
                                        Regex("gho_[A-Za-z0-9]{36}"),
                                        Regex("ghu_[A-Za-z0-9]{36}")
                                ),
                        "jwt-token" to
                                listOf(
                                        Regex(
                                                "eyJ[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*"
                                        )
                                ),
                        "private-key" to
                                listOf(
                                        Regex("-----BEGIN.*PRIVATE KEY-----"),
                                        Regex("-----BEGIN RSA PRIVATE KEY-----")
                                ),
                        "database-url" to
                                listOf(
                                        Regex("jdbc:[a-zA-Z0-9]+://[^\\s]+"),
                                        Regex("mongodb://[^\\s]+"),
                                        Regex("postgres://[^\\s]+")
                                ),
                        "password" to
                                listOf(
                                        Regex(
                                                "password\\s*[=:]\\s*[\"']([^\"'\\s]{8,})[\"']",
                                                RegexOption.IGNORE_CASE
                                        ),
                                        Regex(
                                                "pass\\s*[=:]\\s*[\"']([^\"'\\s]{8,})[\"']",
                                                RegexOption.IGNORE_CASE
                                        )
                                ),
                        "email" to listOf(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")),
                        "ip-address" to listOf(Regex("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b")),
                        "slack-token" to listOf(Regex("xox[baprs]-[0-9a-zA-Z]{10,48}"))
                )

        patternDetector = PatternDetector()
        testFile = tempDir.resolve("test-file.kt").toFile()
    }

    @Nested
    @DisplayName("API Key Detection Tests")
    inner class ApiKeyDetectionTests {

        @Test
        @DisplayName("Should detect Stripe API keys")
        fun testDetectStripeApiKeys() {
            // Arrange
            val content =
                    """
                class PaymentService {
                    private val secretKey = "sk-test_1234567890abcdefghijklmnopqr"
                    private val publishableKey = "pk-test_1234567890abcdefghijklmnopqr"
                }
            """.trimIndent()

            // Act
            val tempFile = File.createTempFile("test", ".kt")
            tempFile.writeText(content)
            val scanContext = ScanContext(
                filePath = tempFile.toPath(),
                fileName = "PaymentService.kt",
                fileExtension = "kt",
                isTestFile = false,
                fileSize = content.length.toLong(),
                configuration = ScanConfiguration(),
                content = content,
                lines = content.lines()
            )
            val results = patternDetector.detect(scanContext)

            // Assert
            assertEquals(2, results.size)

            val secretKeyResult = results.find { it.secretInfo.detectedValue.startsWith("sk-") }
            assertNotNull(secretKeyResult)
            assertEquals("sk-test_1234567890abcdefghijklmnopqr", secretKeyResult?.secretInfo?.detectedValue)
            assertEquals("api-key", secretKeyResult?.secretInfo?.patternName)
            assertEquals(Severity.HIGH, secretKeyResult?.severity)

            val pubKeyResult = results.find { it.secretInfo.detectedValue.startsWith("pk-") }
            assertNotNull(pubKeyResult)
            assertEquals("pk-test_1234567890abcdefghijklmnopqr", pubKeyResult?.secretInfo?.detectedValue)
        }

        @Test
        @DisplayName("Should detect GitHub tokens")
        fun testDetectGitHubTokens() {
            // Arrange
            val content =
                    """
                class GitHubService {
                    val personalToken = "ghp_1234567890abcdefghijklmnopqrstuvwxyz"
                    val oauthToken = "gho_1234567890abcdefghijklmnopqrstuvwxyz"
                    val userToken = "ghu_1234567890abcdefghijklmnopqrstuvwxyz"
                }
            """.trimIndent()

            // Act
            val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(content); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = content.length.toLong(), configuration = ScanConfiguration(), content = content, lines = content.lines()); val results = patternDetector.detect(scanContext)

            // Assert
            assertEquals(3, results.size)
            assertTrue(results.any { it.secretInfo.detectedValue.startsWith("ghp_") })
            assertTrue(results.any { it.secretInfo.detectedValue.startsWith("gho_") })
            assertTrue(results.any { it.secretInfo.detectedValue.startsWith("ghu_") })
            results.forEach { result ->
                assertEquals("github-token", result.secretInfo.patternName)
                assertEquals(Severity.HIGH, result.severity)
            }
        }

        @Test
        @DisplayName("Should detect Slack tokens")
        fun testDetectSlackTokens() {
            // Arrange
            val content =
                    """
                val botToken = "xoxb-1234567890-1234567890-abcdefghijklmnopqrstuvwx"
                val appToken = "xoxa-1234567890-1234567890-abcdefghijklmnopqrstuvwx"
                val userToken = "xoxp-1234567890-1234567890-abcdefghijklmnopqrstuvwx"
            """.trimIndent()

            // Act
            val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(content); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = content.length.toLong(), configuration = ScanConfiguration(), content = content, lines = content.lines()); val results = patternDetector.detect(scanContext)

            // Assert
            assertEquals(3, results.size)
            results.forEach { result ->
                assertTrue(result.secretInfo.detectedValue.startsWith("xox"))
                assertEquals("slack-token", result.secretInfo.patternName)
                assertEquals(Severity.HIGH, result.severity)
            }
        }
    }

    @Nested
    @DisplayName("Cryptographic Key Detection Tests")
    inner class CryptoKeyDetectionTests {

        @Test
        @DisplayName("Should detect private key headers")
        fun testDetectPrivateKeyHeaders() {
            // Arrange
            val content =
                    """
                val rsaKey = '''
                -----BEGIN RSA PRIVATE KEY-----
                MIIEpAIBAAKCAQEA1234567890abcdef...
                -----END RSA PRIVATE KEY-----
                '''
                
                val genericKey = '''
                -----BEGIN PRIVATE KEY-----
                MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC...
                -----END PRIVATE KEY-----
                '''
            """.trimIndent()

            // Act
            val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(content); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = content.length.toLong(), configuration = ScanConfiguration(), content = content, lines = content.lines()); val results = patternDetector.detect(scanContext)

            // Assert
            assertEquals(2, results.size)
            assertTrue(results.any { it.secretInfo.detectedValue.contains("RSA PRIVATE KEY") })
            assertTrue(results.any { it.secretInfo.detectedValue.contains("-----BEGIN PRIVATE KEY-----") })
            results.forEach { result ->
                assertEquals("private-key", result.secretInfo.patternName)
                assertEquals(Severity.CRITICAL, result.severity)
            }
        }

        @Test
        @DisplayName("Should detect AWS secret access keys")
        fun testDetectAwsSecretKeys() {
            // Arrange
            val content =
                    """
                class AwsConfig {
                    val accessKey = "AKIAIOSFODNN7EXAMPLE"
                    val secretKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
                }
            """.trimIndent()

            // Act
            val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(content); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = content.length.toLong(), configuration = ScanConfiguration(), content = content, lines = content.lines()); val results = patternDetector.detect(scanContext)

            // Assert
            val secretResult =
                    results.find { it.secretInfo.detectedValue == "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY" }
            assertNotNull(secretResult)
            assertEquals("aws-secret", secretResult?.secretInfo?.patternName)
            assertEquals(Severity.CRITICAL, secretResult?.severity)
        }
    }

    @Nested
    @DisplayName("Database Connection Detection Tests")
    inner class DatabaseDetectionTests {

        @Test
        @DisplayName("Should detect JDBC URLs")
        fun testDetectJdbcUrls() {
            // Arrange
            val content =
                    """
                class DatabaseConfig {
                    val mysqlUrl = "jdbc:mysql://localhost:3306/mydb"
                    val postgresUrl = "jdbc:postgresql://localhost:5432/mydb"
                    val oracleUrl = "jdbc:oracle:thin:@localhost:1521:xe"
                }
            """.trimIndent()

            // Act
            val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(content); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = content.length.toLong(), configuration = ScanConfiguration(), content = content, lines = content.lines()); val results = patternDetector.detect(scanContext)

            // Assert
            assertEquals(3, results.size)
            assertTrue(results.any { it.secretInfo.detectedValue.contains("jdbc:mysql") })
            assertTrue(results.any { it.secretInfo.detectedValue.contains("jdbc:postgresql") })
            assertTrue(results.any { it.secretInfo.detectedValue.contains("jdbc:oracle") })
            results.forEach { result ->
                assertEquals("database-url", result.secretInfo.patternName)
                assertEquals(Severity.MEDIUM, result.severity)
            }
        }

        @Test
        @DisplayName("Should detect MongoDB URLs")
        fun testDetectMongoUrls() {
            // Arrange
            val content =
                    """
                val mongoUrl = "mongodb://user:pass@localhost:27017/mydb"
                val mongoAtlas = "mongodb+srv://user:pass@cluster.mongodb.net/mydb"
            """.trimIndent()

            // Act
            val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(content); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = content.length.toLong(), configuration = ScanConfiguration(), content = content, lines = content.lines()); val results = patternDetector.detect(scanContext)

            // Assert
            assertTrue(results.size >= 1)
            assertTrue(results.any { it.secretInfo.detectedValue.startsWith("mongodb://") })
            results.forEach { result -> assertEquals("database-url", result.secretInfo.patternName) }
        }

        @Test
        @DisplayName("Should detect PostgreSQL URLs")
        fun testDetectPostgresUrls() {
            // Arrange
            val content =
                    """
                val postgresUrl = "postgres://user:password@localhost/dbname"
                val postgresqlUrl = "postgresql://user:password@localhost/dbname"
            """.trimIndent()

            // Act
            val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(content); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = content.length.toLong(), configuration = ScanConfiguration(), content = content, lines = content.lines()); val results = patternDetector.detect(scanContext)

            // Assert
            assertTrue(results.size >= 1)
            assertTrue(results.any { it.secretInfo.detectedValue.startsWith("postgres://") })
        }
    }

    @Nested
    @DisplayName("JWT Token Detection Tests")
    inner class JwtDetectionTests {

        @Test
        @DisplayName("Should detect JWT tokens")
        fun testDetectJwtTokens() {
            // Arrange
            val content =
                    """
                class AuthService {
                    val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
                    val shortToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIn0.hash"
                }
            """.trimIndent()

            // Act
            val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(content); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = content.length.toLong(), configuration = ScanConfiguration(), content = content, lines = content.lines()); val results = patternDetector.detect(scanContext)

            // Assert
            assertTrue(results.size >= 1)
            assertTrue(results.any { it.secretInfo.detectedValue.startsWith("eyJ") })
            results.forEach { result ->
                assertEquals("jwt-token", result.secretInfo.patternName)
                assertEquals(Severity.MEDIUM, result.severity)
            }
        }

        @Test
        @DisplayName("Should handle malformed JWT tokens")
        fun testHandleMalformedJwt() {
            // Arrange
            val content =
                    """
                val notJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI" // Missing parts
                val validJwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
            """.trimIndent()

            // Act
            val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(content); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = content.length.toLong(), configuration = ScanConfiguration(), content = content, lines = content.lines()); val results = patternDetector.detect(scanContext)

            // Assert
            // Should only detect the valid JWT
            assertEquals(1, results.size)
            assertTrue(results[0].secretInfo.detectedValue.contains("eyJhbGciOiJIUzI1NiJ9"))
        }
    }

    @Nested
    @DisplayName("Password Detection Tests")
    inner class PasswordDetectionTests {

        @Test
        @DisplayName("Should detect password assignments")
        fun testDetectPasswordAssignments() {
            // Arrange
            val content =
                    """
                class Config {
                    val password = "mySecretPassword123"
                    val pass = "anotherSecret456"
                    val PASSWORD: String = "UPPERCASEPASS789"
                }
            """.trimIndent()

            // Act
            val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(content); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = content.length.toLong(), configuration = ScanConfiguration(), content = content, lines = content.lines()); val results = patternDetector.detect(scanContext)

            // Assert
            assertTrue(results.size >= 2)
            assertTrue(results.any { it.secretInfo.detectedValue == "mySecretPassword123" })
            assertTrue(results.any { it.secretInfo.detectedValue == "anotherSecret456" })
            results.forEach { result ->
                assertEquals("password", result.secretInfo.patternName)
                assertEquals(Severity.MEDIUM, result.severity)
            }
        }

        @Test
        @DisplayName("Should not detect short passwords")
        fun testIgnoreShortPasswords() {
            // Arrange
            val content =
                    """
                val password = "short" // Too short, should be ignored
                val pass = "12345" // Also too short
                val validPassword = "thisIsLongEnough123"
            """.trimIndent()

            // Act
            val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(content); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = content.length.toLong(), configuration = ScanConfiguration(), content = content, lines = content.lines()); val results = patternDetector.detect(scanContext)

            // Assert
            assertEquals(1, results.size)
            assertEquals("thisIsLongEnough123", results[0].secretInfo.detectedValue)
        }
    }

    @Nested
    @DisplayName("Line and Column Detection Tests")
    inner class PositionDetectionTests {

        @Test
        @DisplayName("Should correctly identify line numbers")
        fun testLineNumberDetection() {
            // Arrange
            val content =
                    """
                class TestClass {
                    val normalVar = "normal"
                    val secretKey = "sk-1234567890abcdefghijklmnopqr"
                    val anotherVar = "another"
                    val apiToken = "ghp_1234567890abcdefghijklmnopqrstuvwxyz"
                }
            """.trimIndent()

            // Act
            val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(content); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = content.length.toLong(), configuration = ScanConfiguration(), content = content, lines = content.lines()); val results = patternDetector.detect(scanContext)

            // Assert
            assertEquals(2, results.size)

            val secretResult = results.find { it.secretInfo.detectedValue.startsWith("sk-") }
            assertEquals(3, secretResult?.location?.lineNumber)

            val tokenResult = results.find { it.secretInfo.detectedValue.startsWith("ghp_") }
            assertEquals(5, tokenResult?.location?.lineNumber)
        }

        @Test
        @DisplayName("Should correctly identify column positions")
        fun testColumnPositionDetection() {
            // Arrange
            val content = "val key = \"sk-1234567890abcdefghijklmnopqr\""

            // Act
            val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(content); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = content.length.toLong(), configuration = ScanConfiguration(), content = content, lines = content.lines()); val results = patternDetector.detect(scanContext)

            // Assert
            assertEquals(1, results.size)
            val result = results[0]
            assertTrue(result.location.columnStart > 0)
            assertTrue(result.location.columnEnd > result.location.columnStart)
            assertEquals("sk-1234567890abcdefghijklmnopqr", result.secretInfo.detectedValue)
        }

        @Test
        @DisplayName("Should handle multiple matches on same line")
        fun testMultipleMatchesSameLine() {
            // Arrange
            val content =
                    "val keys = \"sk-1234567890abcdefghijklmnopqr and pk-1234567890abcdefghijklmnopqr\""

            // Act
            val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(content); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = content.length.toLong(), configuration = ScanConfiguration(), content = content, lines = content.lines()); val results = patternDetector.detect(scanContext)

            // Assert
            assertEquals(2, results.size)
            results.forEach { result -> assertEquals(1, result.location.lineNumber) }

            val firstResult = results.minByOrNull { it.location.columnStart }
            val secondResult = results.maxByOrNull { it.location.columnStart }

            assertNotNull(firstResult)
            assertNotNull(secondResult)
            assertTrue(firstResult!!.location.columnStart < secondResult!!.location.columnStart)
        }
    }

    @Nested
    @DisplayName("Pattern Matching Edge Cases")
    inner class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty content")
        fun testEmptyContent() {
            // Act
            val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(""); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = 0L, configuration = ScanConfiguration(), content = "", lines = emptyList()); val results = patternDetector.detect(scanContext)

            // Assert
            assertTrue(results.isEmpty())
        }

        @Test
        @DisplayName("Should handle content with no matches")
        fun testNoMatches() {
            // Arrange
            val content =
                    """
                class CleanClass {
                    val normalVariable = "normalValue"
                    fun doSomething() = "result"
                }
            """.trimIndent()

            // Act
            val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(content); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = content.length.toLong(), configuration = ScanConfiguration(), content = content, lines = content.lines()); val results = patternDetector.detect(scanContext)

            // Assert
            assertTrue(results.isEmpty())
        }

        @Test
        @DisplayName("Should handle special characters in content")
        fun testSpecialCharacters() {
            // Arrange
            val content =
                    """
                val unicode = "Hello 世界 🌍"
                val secret = "sk-1234567890abcdefghijklmnopqr"
                val symbols = "¡¢£¤¥¦§¨©ª«¬®¯°±²³´µ¶·¸¹º»¼½¾¿"
            """.trimIndent()

            // Act
            val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(content); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = content.length.toLong(), configuration = ScanConfiguration(), content = content, lines = content.lines()); val results = patternDetector.detect(scanContext)

            // Assert
            assertEquals(1, results.size)
            assertEquals("sk-1234567890abcdefghijklmnopqr", results[0].secretInfo.detectedValue)
        }

        @Test
        @DisplayName("Should handle very long lines")
        fun testVeryLongLines() {
            // Arrange
            val longString = "x".repeat(10000)
            val content =
                    "val longVar = \"$longString\" val secret = \"sk-1234567890abcdefghijklmnopqr\""

            // Act
            val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(content); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = content.length.toLong(), configuration = ScanConfiguration(), content = content, lines = content.lines()); val results = patternDetector.detect(scanContext)

            // Assert
            assertEquals(1, results.size)
            assertEquals("sk-1234567890abcdefghijklmnopqr", results[0].secretInfo.detectedValue)
        }

        @Test
        @DisplayName("Should handle patterns at line boundaries")
        fun testPatternAtLineBoundaries() {
            // Arrange
            val content =
                    """sk-1234567890abcdefghijklmnopqr
                |normal content
                |ghp_1234567890abcdefghijklmnopqrstuvwxyz""".trimMargin()

            // Act
            val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(content); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = content.length.toLong(), configuration = ScanConfiguration(), content = content, lines = content.lines()); val results = patternDetector.detect(scanContext)

            // Assert
            assertEquals(2, results.size)
            assertTrue(results.any { it.location.lineNumber == 1 })
            assertTrue(results.any { it.location.lineNumber == 3 })
        }
    }

    @Nested
    @DisplayName("Pattern Configuration Tests")
    inner class PatternConfigurationTests {

        @Test
        @DisplayName("Should work with empty pattern map")
        fun testEmptyPatternMap() {
            // Arrange
            val emptyDetector = PatternDetector()
            val content = "val secret = \"sk-1234567890abcdefghijklmnopqr\""

            // Act
            val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(content); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = content.length.toLong(), configuration = ScanConfiguration(), content = content, lines = content.lines()); val results = emptyDetector.detect(scanContext)

            // Assert
            assertTrue(results.isEmpty())
        }

        @Test
        @DisplayName("Should work with single pattern")
        fun testSinglePattern() {
            // Arrange
            val singlePatternMap = mapOf("test-pattern" to listOf(Regex("test-\\d+")))
            val singleDetector = PatternDetector()
            val content = "val value = \"test-123\""

            // Act
            val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(content); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = content.length.toLong(), configuration = ScanConfiguration(), content = content, lines = content.lines()); val results = singleDetector.detect(scanContext)

            // Assert
            assertEquals(1, results.size)
            assertEquals("test-123", results[0].secretInfo.detectedValue)
            assertEquals("test-pattern", results[0].secretInfo.patternName)
        }

        @Test
        @DisplayName("Should handle multiple patterns for same rule")
        fun testMultiplePatternsPerRule() {
            // Arrange
            val multiPatternMap =
                    mapOf(
                            "multi-test" to
                                    listOf(
                                            Regex("pattern1-\\d+"),
                                            Regex("pattern2-[a-z]+"),
                                            Regex("pattern3-[A-Z]+")
                                    )
                    )
            val multiDetector = PatternDetector()
            val content =
                    """
                val val1 = "pattern1-123"
                val val2 = "pattern2-abc"
                val val3 = "pattern3-XYZ"
            """.trimIndent()

            // Act
            val results = multiDetector.detect(testFile, content)

            // Assert
            assertEquals(3, results.size)
            results.forEach { result -> assertEquals("multi-test", result.secretInfo.patternName) }
        }

        @Test
        @DisplayName("Should handle regex with flags")
        fun testRegexWithFlags() {
            // Arrange
            val flagPatternMap =
                    mapOf(
                            "case-insensitive" to
                                    listOf(
                                            Regex(
                                                    "SECRET\\s*=\\s*\"([^\"]+)\"",
                                                    RegexOption.IGNORE_CASE
                                            )
                                    )
                    )
            val flagDetector = PatternDetector()
            val content =
                    """
                val secret = "mySecret"
                val SECRET = "myOtherSecret"
                val Secret = "anotherSecret"
            """.trimIndent()

            // Act
            val results = flagDetector.detect(testFile, content)

            // Assert
            assertTrue(results.size >= 2) // Should match different cases
        }
    }

    @Nested
    @DisplayName("Integration with Real Patterns")
    inner class RealPatternIntegrationTests {

        @Test
        @DisplayName("Should work with SecretPatterns")
        fun testWithSecretPatterns() {
            // Arrange
            val realDetector = PatternDetector())
            val content =
                    """
                class RealSecrets {
                    val apiKey = "sk-1234567890abcdefghijklmnopqr"
                    val awsSecret = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
                    val githubToken = "ghp_1234567890abcdefghijklmnopqrstuvwxyz"
                }
            """.trimIndent()

            // Act
            val results = realDetector.detect(testFile, content)

            // Assert
            assertFalse(results.isEmpty())
            assertTrue(results.any { it.secretInfo.detectedValue.contains("sk-") })
            assertTrue(results.any { it.secretInfo.detectedValue.contains("wJalrXUtnFEMI") })
            assertTrue(results.any { it.secretInfo.detectedValue.contains("ghp_") })
        }

        @Test
        @DisplayName("Should work with ApiKeyPatterns")
        fun testWithApiKeyPatterns() {
            // Arrange
            val realDetector = PatternDetector())
            val content =
                    """
                val stripeKey = "sk-test_1234567890abcdefghijklmnopqr"
                val twilioSid = "AC1234567890abcdefghijklmnopqrstuvwx"
            """.trimIndent()

            // Act
            val results = realDetector.detect(testFile, content)

            // Assert
            assertFalse(results.isEmpty())
        }

        @Test
        @DisplayName("Should work with CryptoPatterns")
        fun testWithCryptoPatterns() {
            // Arrange
            val realDetector = PatternDetector())
            val content =
                    """
                val privateKey = '''
                -----BEGIN RSA PRIVATE KEY-----
                MIIEpAIBAAKCAQEA1234567890abcdef...
                -----END RSA PRIVATE KEY-----
                '''
            """.trimIndent()

            // Act
            val results = realDetector.detect(testFile, content)

            // Assert
            assertFalse(results.isEmpty())
            assertTrue(results.any { it.secretInfo.detectedValue.contains("RSA PRIVATE KEY") })
        }

        @Test
        @DisplayName("Should work with DatabasePatterns")
        fun testWithDatabasePatterns() {
            // Arrange
            val realDetector = PatternDetector())
            val content =
                    """
                val dbUrl = "jdbc:postgresql://localhost:5432/mydb"
                val mongoUrl = "mongodb://user:pass@localhost:27017/mydb"
            """.trimIndent()

            // Act
            val results = realDetector.detect(testFile, content)

            // Assert
            assertFalse(results.isEmpty())
            assertTrue(results.any { it.secretInfo.detectedValue.contains("jdbc:") })
            assertTrue(results.any { it.secretInfo.detectedValue.contains("mongodb://") })
        }
    }

    @ParameterizedTest
    @ValueSource(
            strings =
                    [
                            "sk-1234567890abcdefghijklmnopqr",
                            "pk-1234567890abcdefghijklmnopqr",
                            "ghp_1234567890abcdefghijklmnopqrstuvwxyz",
                            "gho_1234567890abcdefghijklmnopqrstuvwxyz",
                            "xoxb-1234567890-1234567890-abcdefghijklmnopqrstuvwx"]
    )
    @DisplayName("Should detect various API key formats")
    fun testDetectVariousApiKeys(secretValue: String) {
        // Arrange
        val content = "val secret = \"$secretValue\""

        // Act
        val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(content); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = content.length.toLong(), configuration = ScanConfiguration(), content = content, lines = content.lines()); val results = patternDetector.detect(scanContext)

        // Assert
        assertFalse(results.isEmpty())
        assertTrue(results.any { it.secretInfo.detectedValue == secretValue })
    }

    @ParameterizedTest
    @CsvSource(
            "sk-1234567890abcdefghijklmnopqr, api-key, HIGH",
            "ghp_1234567890abcdefghijklmnopqrstuvwxyz, github-token, HIGH",
            "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY, aws-secret, CRITICAL",
            "jdbc:mysql://localhost:3306/mydb, database-url, MEDIUM"
    )
    @DisplayName("Should assign correct severity levels")
    fun testSeverityLevels(secretValue: String, expectedRuleId: String, expectedSeverity: String) {
        // Arrange
        val content = "val secret = \"$secretValue\""

        // Act
        val tempFile = File.createTempFile("test", ".kt"); tempFile.writeText(content); val scanContext = ScanContext(filePath = tempFile.toPath(), fileName = "test.kt", fileExtension = "kt", isTestFile = false, fileSize = content.length.toLong(), configuration = ScanConfiguration(), content = content, lines = content.lines()); val results = patternDetector.detect(scanContext)

        // Assert
        assertFalse(results.isEmpty())
        val result = results.find { it.secretInfo.detectedValue == secretValue }
        assertNotNull(result)
        assertEquals(expectedRuleId, result?.secretInfo.patternName)
        assertEquals(Severity.valueOf(expectedSeverity), result?.severity)
    }
}
