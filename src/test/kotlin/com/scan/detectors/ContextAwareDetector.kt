package com.scan.detectors

import com.scan.patterns.SecretPatterns
import io.mockk.every
import io.mockk.mockk
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
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("ContextAwareDetector Tests")
class ContextAwareDetectorTest {

    private lateinit var detector: ContextAwareDetector
    private lateinit var mockSecretPatterns: SecretPatterns

    @BeforeEach
    fun setUp() {
        mockSecretPatterns = mockk()
        every { mockSecretPatterns.getAllPatterns() } returns
                mapOf(
                        "api_key" to
                                listOf(
                                        "(?i)api[_-]?key['\"]?\\s*[:=]\\s*['\"]?([a-zA-Z0-9_-]{20,})",
                                        "(?i)secret[_-]?key['\"]?\\s*[:=]\\s*['\"]?([a-zA-Z0-9_-]{20,})"
                                ),
                        "aws_access_key" to listOf("AKIA[0-9A-Z]{16}"),
                        "database_url" to
                                listOf(
                                        "(?i)(jdbc|mysql|postgres)://[^\\s'\"]+:[^\\s'\"]+@[^\\s'\"]+/[^\\s'\"]+",
                                        "(?i)database[_-]?url['\"]?\\s*[:=]\\s*['\"]?([^\\s'\"]+)"
                                )
                )
        detector = ContextAwareDetector(mockSecretPatterns)
    }

    @Nested
    @DisplayName("Code Context Detection")
    inner class CodeContextDetection {

        @Test
        @DisplayName("Should detect secrets in actual code")
        fun shouldDetectSecretsInActualCode() {
            val content =
                    """
                package com.example
                
                class DatabaseConfig {
                    private val apiKey = "sk_live_abcdef123456789012345678"
                    private val dbUrl = "jdbc:mysql://user:password123@localhost:3306/mydb"
                }
            """.trimIndent()

            val results = detector.detect(content, "DatabaseConfig.kt")

            assertEquals(2, results.size)
            assertTrue(
                    results.any {
                        it.type == "api_key" &&
                                it.value.contains("sk_live_abcdef123456789012345678")
                    }
            )
            assertTrue(
                    results.any {
                        it.type == "database_url" &&
                                it.value.contains(
                                        "jdbc:mysql://user:password123@localhost:3306/mydb"
                                )
                    }
            )
        }

        @Test
        @DisplayName("Should ignore secrets in single-line comments")
        fun shouldIgnoreSecretsInSingleLineComments() {
            val content =
                    """
                package com.example
                
                class Config {
                    // This is a comment with API key: sk_live_abcdef123456789012345678
                    // TODO: Remove this database URL: jdbc:mysql://user:password123@localhost:3306/mydb
                    private val realApiKey = "sk_live_realkey123456789012345678"
                }
            """.trimIndent()

            val results = detector.detect(content, "Config.kt")

            assertEquals(1, results.size)
            assertTrue(results.any { it.value.contains("sk_live_realkey123456789012345678") })
            assertFalse(results.any { it.value.contains("sk_live_abcdef123456789012345678") })
        }

        @Test
        @DisplayName("Should ignore secrets in multi-line comments")
        fun shouldIgnoreSecretsInMultiLineComments() {
            val content =
                    """
                package com.example
                
                /*
                 * This is a multi-line comment
                 * API Key: sk_live_abcdef123456789012345678
                 * Database URL: jdbc:mysql://user:password123@localhost:3306/mydb
                 */
                class Config {
                    private val apiKey = "sk_live_realkey123456789012345678"
                }
            """.trimIndent()

            val results = detector.detect(content, "Config.kt")

            assertEquals(1, results.size)
            assertTrue(results.any { it.value.contains("sk_live_realkey123456789012345678") })
            assertFalse(results.any { it.value.contains("sk_live_abcdef123456789012345678") })
        }

        @Test
        @DisplayName("Should ignore secrets in KDoc comments")
        fun shouldIgnoreSecretsInKDocComments() {
            val content =
                    """
                package com.example
                
                /**
                 * This class handles database configuration
                 * Example usage:
                 * ```
                 * val config = DatabaseConfig()
                 * config.apiKey = "sk_live_abcdef123456789012345678"
                 * ```
                 */
                class DatabaseConfig {
                    private val apiKey = "sk_live_realkey123456789012345678"
                }
            """.trimIndent()

            val results = detector.detect(content, "DatabaseConfig.kt")

            assertEquals(1, results.size)
            assertTrue(results.any { it.value.contains("sk_live_realkey123456789012345678") })
            assertFalse(results.any { it.value.contains("sk_live_abcdef123456789012345678") })
        }

        @Test
        @DisplayName("Should detect secrets in string literals")
        fun shouldDetectSecretsInStringLiterals() {
            val content =
                    """
                package com.example
                
                class Config {
                    private val apiKey = "sk_live_abcdef123456789012345678"
                    private val dbUrl = "jdbc:mysql://user:password123@localhost:3306/mydb"
                    private val multilineSecret = ${"\"\"\""}
                        secret_key=sk_live_multiline123456789012345678
                    ${"\"\"\""}
                }
            """.trimIndent()

            val results = detector.detect(content, "Config.kt")

            assertEquals(3, results.size)
            assertTrue(results.any { it.value.contains("sk_live_abcdef123456789012345678") })
            assertTrue(
                    results.any {
                        it.value.contains("jdbc:mysql://user:password123@localhost:3306/mydb")
                    }
            )
            assertTrue(results.any { it.value.contains("sk_live_multiline123456789012345678") })
        }
    }

    @Nested
    @DisplayName("Context Sensitivity")
    inner class ContextSensitivity {

        @Test
        @DisplayName("Should handle nested comments correctly")
        fun shouldHandleNestedCommentsCorrectly() {
            val content =
                    """
                package com.example
                
                class Config {
                    /*
                     * This is a comment with API key: sk_live_comment123456789012345678
                     * // Inner comment: sk_live_inner123456789012345678
                     */
                    private val apiKey = "sk_live_actual123456789012345678" // End comment: sk_live_end123456789012345678
                }
            """.trimIndent()

            val results = detector.detect(content, "Config.kt")

            assertEquals(1, results.size)
            assertTrue(results.any { it.value.contains("sk_live_actual123456789012345678") })
            assertFalse(results.any { it.value.contains("sk_live_comment123456789012345678") })
            assertFalse(results.any { it.value.contains("sk_live_inner123456789012345678") })
            assertFalse(results.any { it.value.contains("sk_live_end123456789012345678") })
        }

        @Test
        @DisplayName("Should handle commented-out code")
        fun shouldHandleCommentedOutCode() {
            val content =
                    """
                package com.example
                
                class Config {
                    // private val oldApiKey = "sk_live_old123456789012345678"
                    /* 
                    private val deprecatedKey = "sk_live_deprecated123456789012345678"
                    */
                    private val currentApiKey = "sk_live_current123456789012345678"
                }
            """.trimIndent()

            val results = detector.detect(content, "Config.kt")

            assertEquals(1, results.size)
            assertTrue(results.any { it.value.contains("sk_live_current123456789012345678") })
            assertFalse(results.any { it.value.contains("sk_live_old123456789012345678") })
            assertFalse(results.any { it.value.contains("sk_live_deprecated123456789012345678") })
        }

        @Test
        @DisplayName("Should detect secrets in configuration files")
        fun shouldDetectSecretsInConfigurationFiles() {
            val content =
                    """
                # Configuration file
                # This is a comment with fake key: sk_live_fake123456789012345678
                api_key=sk_live_real123456789012345678
                database_url=jdbc:mysql://user:password123@localhost:3306/mydb
                # Another comment
            """.trimIndent()

            val results = detector.detect(content, "application.properties")

            assertEquals(2, results.size)
            assertTrue(results.any { it.value.contains("sk_live_real123456789012345678") })
            assertTrue(
                    results.any {
                        it.value.contains("jdbc:mysql://user:password123@localhost:3306/mydb")
                    }
            )
            assertFalse(results.any { it.value.contains("sk_live_fake123456789012345678") })
        }
    }

    @Nested
    @DisplayName("File Type Handling")
    inner class FileTypeHandling {

        @ParameterizedTest
        @ValueSource(strings = ["Config.kt", "Config.java", "Config.scala", "Config.groovy"])
        @DisplayName("Should handle JVM language files")
        fun shouldHandleJvmLanguageFiles(fileName: String) {
            val content =
                    """
                class Config {
                    // Comment with API key: sk_live_comment123456789012345678
                    private val apiKey = "sk_live_actual123456789012345678"
                }
            """.trimIndent()

            val results = detector.detect(content, fileName)

            assertEquals(1, results.size)
            assertTrue(results.any { it.value.contains("sk_live_actual123456789012345678") })
        }

        @ParameterizedTest
        @ValueSource(strings = ["config.yml", "config.yaml", "config.json", "config.xml"])
        @DisplayName("Should handle configuration files")
        fun shouldHandleConfigurationFiles(fileName: String) {
            val content =
                    when {
                        fileName.endsWith(".yml") || fileName.endsWith(".yaml") ->
                                """
                    # YAML configuration
                    api:
                      key: sk_live_yaml123456789012345678
                    database:
                      url: jdbc:mysql://user:password123@localhost:3306/mydb
                """.trimIndent()
                        fileName.endsWith(".json") ->
                                """
                    {
                      "api_key": "sk_live_json123456789012345678",
                      "database_url": "jdbc:mysql://user:password123@localhost:3306/mydb"
                    }
                """.trimIndent()
                        else ->
                                """
                    api_key=sk_live_config123456789012345678
                    database_url=jdbc:mysql://user:password123@localhost:3306/mydb
                """.trimIndent()
                    }

            val results = detector.detect(content, fileName)

            assertTrue(results.size >= 2)
            assertTrue(results.any { it.type == "api_key" })
            assertTrue(results.any { it.type == "database_url" })
        }

        @Test
        @DisplayName("Should handle SQL files with comments")
        fun shouldHandleSqlFilesWithComments() {
            val content =
                    """
                -- This is a SQL comment with fake key: sk_live_fake123456789012345678
                INSERT INTO config (key, value) VALUES ('api_key', 'sk_live_sql123456789012345678');
                /* Multi-line comment
                   with fake database URL: jdbc:mysql://user:fake@localhost:3306/db */
                UPDATE config SET value = 'jdbc:mysql://user:real123@localhost:3306/mydb' WHERE key = 'database_url';
            """.trimIndent()

            val results = detector.detect(content, "setup.sql")

            assertEquals(2, results.size)
            assertTrue(results.any { it.value.contains("sk_live_sql123456789012345678") })
            assertTrue(
                    results.any {
                        it.value.contains("jdbc:mysql://user:real123@localhost:3306/mydb")
                    }
            )
            assertFalse(results.any { it.value.contains("sk_live_fake123456789012345678") })
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        @DisplayName("Should handle empty content")
        fun shouldHandleEmptyContent() {
            val results = detector.detect("", "Empty.kt")
            assertTrue(results.isEmpty())
        }

        @Test
        @DisplayName("Should handle content with only comments")
        fun shouldHandleContentWithOnlyComments() {
            val content =
                    """
                // This is a comment with API key: sk_live_comment123456789012345678
                /* 
                 * Multi-line comment with database URL: jdbc:mysql://user:password123@localhost:3306/mydb
                 */
            """.trimIndent()

            val results = detector.detect(content, "CommentsOnly.kt")
            assertTrue(results.isEmpty())
        }

        @Test
        @DisplayName("Should handle malformed comments")
        fun shouldHandleMalformedComments() {
            val content =
                    """
                package com.example
                
                class Config {
                    /* Unclosed comment with API key: sk_live_unclosed123456789012345678
                    private val apiKey = "sk_live_actual123456789012345678"
                    */
                }
            """.trimIndent()

            val results = detector.detect(content, "Config.kt")

            assertEquals(1, results.size)
            assertTrue(results.any { it.value.contains("sk_live_actual123456789012345678") })
            assertFalse(results.any { it.value.contains("sk_live_unclosed123456789012345678") })
        }

        @Test
        @DisplayName("Should handle strings with escaped quotes")
        fun shouldHandleStringsWithEscapedQuotes() {
            val content =
                    """
                package com.example
                
                class Config {
                    private val message = "This is a \"quoted\" message with API key: sk_live_quoted123456789012345678"
                    private val apiKey = "sk_live_actual123456789012345678"
                }
            """.trimIndent()

            val results = detector.detect(content, "Config.kt")

            assertEquals(2, results.size)
            assertTrue(results.any { it.value.contains("sk_live_quoted123456789012345678") })
            assertTrue(results.any { it.value.contains("sk_live_actual123456789012345678") })
        }

        @Test
        @DisplayName("Should handle very long lines")
        fun shouldHandleVeryLongLines() {
            val longApiKey = "sk_live_" + "a".repeat(1000)
            val content =
                    """
                package com.example
                
                class Config {
                    // This is a very long comment: ${"x".repeat(1000)}
                    private val apiKey = "$longApiKey"
                }
            """.trimIndent()

            val results = detector.detect(content, "Config.kt")

            assertEquals(1, results.size)
            assertTrue(results.any { it.value.contains(longApiKey) })
        }
    }

    @Nested
    @DisplayName("Result Validation")
    inner class ResultValidation {

        @Test
        @DisplayName("Should provide accurate line numbers")
        fun shouldProvideAccurateLineNumbers() {
            val content =
                    """
                package com.example
                
                class Config {
                    private val apiKey = "sk_live_line4_123456789012345678"
                    
                    private val dbUrl = "jdbc:mysql://user:password123@localhost:3306/mydb"
                }
            """.trimIndent()

            val results = detector.detect(content, "Config.kt")

            assertEquals(2, results.size)
            val apiKeyResult =
                    results.find { it.value.contains("sk_live_line4_123456789012345678") }
            val dbUrlResult =
                    results.find {
                        it.value.contains("jdbc:mysql://user:password123@localhost:3306/mydb")
                    }

            assertNotNull(apiKeyResult)
            assertNotNull(dbUrlResult)
            assertEquals(4, apiKeyResult.lineNumber)
            assertEquals(6, dbUrlResult.lineNumber)
        }

        @Test
        @DisplayName("Should provide accurate column numbers")
        fun shouldProvideAccurateColumnNumbers() {
            val content = """val apiKey = "sk_live_col14_123456789012345678""""

            val results = detector.detect(content, "Config.kt")

            assertEquals(1, results.size)
            val result = results.first()
            assertEquals(14, result.columnNumber)
        }

        @Test
        @DisplayName("Should include file name in results")
        fun shouldIncludeFileNameInResults() {
            val content = """val apiKey = "sk_live_test123456789012345678""""
            val fileName = "TestConfig.kt"

            val results = detector.detect(content, fileName)

            assertEquals(1, results.size)
            assertEquals(fileName, results.first().fileName)
        }

        @Test
        @DisplayName("Should categorize secret types correctly")
        fun shouldCategorizeSecretTypesCorrectly() {
            val content =
                    """
                val apiKey = "sk_live_api123456789012345678"
                val awsKey = "AKIA1234567890123456"
                val dbUrl = "jdbc:mysql://user:password123@localhost:3306/mydb"
            """.trimIndent()

            val results = detector.detect(content, "Config.kt")

            assertEquals(3, results.size)
            assertTrue(results.any { it.type == "api_key" })
            assertTrue(results.any { it.type == "aws_access_key" })
            assertTrue(results.any { it.type == "database_url" })
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    inner class PerformanceTests {

        @Test
        @DisplayName("Should handle large files efficiently")
        fun shouldHandleLargeFilesEfficiently() {
            val lines = mutableListOf<String>()
            repeat(10000) { i ->
                if (i % 1000 == 0) {
                    lines.add(
                            """    private val apiKey$i = "sk_live_large${i}123456789012345678""""
                    )
                } else {
                    lines.add("""    // Comment line $i with some text""")
                }
            }
            val content = lines.joinToString("\n")

            val startTime = System.currentTimeMillis()
            val results = detector.detect(content, "LargeFile.kt")
            val endTime = System.currentTimeMillis()

            assertEquals(10, results.size)
            assertTrue(endTime - startTime < 5000) // Should complete within 5 seconds
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {

        @Test
        @DisplayName("Should handle null file name gracefully")
        fun shouldHandleNullFileNameGracefully() {
            val content = """val apiKey = "sk_live_test123456789012345678""""

            assertThrows<IllegalArgumentException> { detector.detect(content, null) }
        }

        @Test
        @DisplayName("Should handle null content gracefully")
        fun shouldHandleNullContentGracefully() {
            assertThrows<IllegalArgumentException> { detector.detect(null, "Config.kt") }
        }

        @Test
        @DisplayName("Should handle invalid regex patterns gracefully")
        fun shouldHandleInvalidRegexPatternsGracefully() {
            val invalidPatterns = mockk<SecretPatterns>()
            every { invalidPatterns.getAllPatterns() } returns
                    mapOf("invalid" to listOf("[unclosed"))

            val detectorWithInvalidPatterns = ContextAwareDetector(invalidPatterns)
            val content = """val test = "some content""""

            val results = detectorWithInvalidPatterns.detect(content, "Config.kt")
            assertTrue(results.isEmpty()) // Should not crash, just return empty results
        }
    }
}
