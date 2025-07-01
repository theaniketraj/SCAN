package com.scan.integration

import com.scan.plugin.ScanExtension
import com.scan.plugin.ScanPlugin
import com.scan.plugin.ScanTask
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Path
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("End-to-End Integration Tests")
class EndToEndTest {

    @TempDir lateinit var tempDir: Path

    private lateinit var project: Project
    private lateinit var scanTask: ScanTask
    private lateinit var originalOut: PrintStream
    private lateinit var testOut: ByteArrayOutputStream

    @BeforeEach
    fun setUp() {
        // Create a Gradle project for testing
        project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()

        // Apply the scan plugin
        project.pluginManager.apply(ScanPlugin::class.java)

        // Get the scan task
        scanTask = project.tasks.getByName("scan") as ScanTask

        // Capture console output
        originalOut = System.out
        testOut = ByteArrayOutputStream()
        System.setOut(PrintStream(testOut))
    }

    @AfterEach
    fun tearDown() {
        // Restore console output
        System.setOut(originalOut)
    }

    @Nested
    @DisplayName("Basic Plugin Integration")
    inner class BasicPluginIntegration {

        @Test
        @DisplayName("Should apply plugin successfully")
        fun shouldApplyPluginSuccessfully() {
            assertTrue(project.plugins.hasPlugin(ScanPlugin::class.java))
            assertNotNull(project.tasks.findByName("scan"))
            assertNotNull(project.extensions.findByType(ScanExtension::class.java))
        }

        @Test
        @DisplayName("Should register scan task with correct properties")
        fun shouldRegisterScanTaskWithCorrectProperties() {
            val task = project.tasks.getByName("scan")

            assertEquals("scan", task.name)
            assertEquals("verification", task.group)
            assertNotNull(task.description)
            assertTrue(task.description!!.contains("security"))
        }

        @Test
        @DisplayName("Should configure extension with default values")
        fun shouldConfigureExtensionWithDefaultValues() {
            val extension = project.extensions.getByType(ScanExtension::class.java)

            assertNotNull(extension)
            // Test default configuration values
            assertTrue(extension.enabled)
            assertFalse(extension.failOnFound)
            assertEquals("console", extension.outputFormat)
        }
    }

    @Nested
    @DisplayName("File Scanning Workflow")
    inner class FileScanningWorkflow {

        @Test
        @DisplayName("Should scan and detect secrets in Kotlin files")
        fun shouldScanAndDetectSecretsInKotlinFiles() {
            // Create test files with secrets
            createTestFile(
                    "src/main/kotlin/Config.kt",
                    """
                package com.example
                
                class DatabaseConfig {
                    private val apiKey = "sk_live_abcdef123456789012345678901234567890"
                    private val dbUrl = "jdbc:mysql://admin:secretpass123@localhost:3306/myapp"
                    private val awsAccessKey = "AKIA1234567890123456"
                    
                    fun getConfig(): Map<String, String> {
                        return mapOf(
                            "api_key" to apiKey,
                            "database_url" to dbUrl,
                            "aws_key" to awsAccessKey
                        )
                    }
                }
            """.trimIndent()
            )

            // Execute scan task
            scanTask.scan()

            // Verify console output contains detected secrets
            val output = testOut.toString()
            assertContains(output, "sk_live_abcdef123456789012345678901234567890")
            assertContains(output, "jdbc:mysql://admin:secretpass123@localhost:3306/myapp")
            assertContains(output, "AKIA1234567890123456")
            assertContains(output, "Config.kt")
        }

        @Test
        @DisplayName("Should scan and detect secrets in properties files")
        fun shouldScanAndDetectSecretsInPropertiesFiles() {
            createTestFile(
                    "src/main/resources/application.properties",
                    """
                # Database configuration
                spring.datasource.url=jdbc:postgresql://user:password123@localhost:5432/mydb
                spring.datasource.username=admin
                spring.datasource.password=supersecret123
                
                # API Configuration
                api.key=sk_live_properties123456789012345678901234567890
                secret.token=ghp_abcdefghijklmnopqrstuvwxyz1234567890
                
                # AWS Configuration
                aws.access.key=AKIA9876543210987654
                aws.secret.key=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
            """.trimIndent()
            )

            scanTask.scan()

            val output = testOut.toString()
            assertContains(output, "sk_live_properties123456789012345678901234567890")
            assertContains(output, "ghp_abcdefghijklmnopqrstuvwxyz1234567890")
            assertContains(output, "AKIA9876543210987654")
            assertContains(output, "application.properties")
        }

        @Test
        @DisplayName("Should scan and detect secrets in YAML files")
        fun shouldScanAndDetectSecretsInYamlFiles() {
            createTestFile(
                    "src/main/resources/application.yml",
                    """
                server:
                  port: 8080
                
                database:
                  url: jdbc:mysql://root:rootpass123@localhost:3306/testdb
                  username: admin
                  password: adminpass123
                
                api:
                  key: sk_live_yaml123456789012345678901234567890
                  secret: api_secret_token_abcdefghijklmnopqrstuvwxyz
                
                github:
                  token: ghp_yamltoken123456789012345678901234567890
                
                aws:
                  access_key: AKIAYAML1234567890AB
                  secret_key: secretkey123456789012345678901234567890abcdef
            """.trimIndent()
            )

            scanTask.scan()

            val output = testOut.toString()
            assertContains(output, "sk_live_yaml123456789012345678901234567890")
            assertContains(output, "ghp_yamltoken123456789012345678901234567890")
            assertContains(output, "AKIAYAML1234567890AB")
            assertContains(output, "application.yml")
        }

        @Test
        @DisplayName("Should scan and detect secrets in JSON files")
        fun shouldScanAndDetectSecretsInJsonFiles() {
            createTestFile(
                    "src/main/resources/config.json",
                    """
                {
                  "database": {
                    "url": "jdbc:postgresql://dbuser:dbpass123@localhost:5432/jsondb",
                    "username": "admin",
                    "password": "jsonpass123"
                  },
                  "api": {
                    "key": "sk_live_json123456789012345678901234567890",
                    "secret": "json_secret_abcdefghijklmnopqrstuvwxyz123456"
                  },
                  "aws": {
                    "access_key": "AKIAJSON1234567890CD",
                    "secret_access_key": "jsonawssecret123456789012345678901234567890"
                  },
                  "github": {
                    "personal_access_token": "ghp_jsontoken123456789012345678901234567890"
                  }
                }
            """.trimIndent()
            )

            scanTask.scan()

            val output = testOut.toString()
            assertContains(output, "sk_live_json123456789012345678901234567890")
            assertContains(output, "ghp_jsontoken123456789012345678901234567890")
            assertContains(output, "AKIAJSON1234567890CD")
            assertContains(output, "config.json")
        }

        @Test
        @DisplayName("Should ignore secrets in comments")
        fun shouldIgnoreSecretsInComments() {
            createTestFile(
                    "src/main/kotlin/CommentTest.kt",
                    """
                package com.example
                
                /**
                 * This is documentation with example API key: sk_live_doc123456789012345678901234567890
                 * Example database URL: jdbc:mysql://user:pass@localhost:3306/db
                 */
                class Config {
                    // This comment has fake API key: sk_live_comment123456789012345678901234567890
                    private val realApiKey = "sk_live_real123456789012345678901234567890"
                    
                    /*
                     * Multi-line comment with fake AWS key: AKIACOMMENT1234567890
                     * And fake database URL: jdbc:postgresql://fake:fake@localhost:5432/fake
                     */
                    private val realAwsKey = "AKIAREAL1234567890EF"
                }
            """.trimIndent()
            )

            scanTask.scan()

            val output = testOut.toString()
            // Should detect real secrets
            assertContains(output, "sk_live_real123456789012345678901234567890")
            assertContains(output, "AKIAREAL1234567890EF")

            // Should NOT detect commented secrets
            assertFalse(output.contains("sk_live_doc123456789012345678901234567890"))
            assertFalse(output.contains("sk_live_comment123456789012345678901234567890"))
            assertFalse(output.contains("AKIACOMMENT1234567890"))
        }

        @Test
        @DisplayName("Should handle empty and clean files")
        fun shouldHandleEmptyAndCleanFiles() {
            createTestFile("src/main/kotlin/EmptyFile.kt", "")
            createTestFile(
                    "src/main/kotlin/CleanFile.kt",
                    """
                package com.example
                
                class CleanConfig {
                    private val normalProperty = "normal_value"
                    private val anotherProperty = 12345
                    
                    fun doSomething() {
                        println("This is a clean file with no secrets")
                    }
                }
            """.trimIndent()
            )

            scanTask.scan()

            val output = testOut.toString()
            // Should complete without errors
            assertTrue(
                    output.contains("Scan completed") ||
                            output.contains("No secrets found") ||
                            output.isEmpty()
            )
        }
    }

    @Nested
    @DisplayName("Configuration Testing")
    inner class ConfigurationTesting {

        @Test
        @DisplayName("Should respect include patterns configuration")
        fun shouldRespectIncludePatternsConfiguration() {
            val extension = project.extensions.getByType(ScanExtension::class.java)
            extension.includePatterns = listOf("**/*.kt")

            createTestFile(
                    "src/main/kotlin/Config.kt",
                    """
                val apiKey = "sk_live_kotlin123456789012345678901234567890"
            """.trimIndent()
            )

            createTestFile(
                    "src/main/java/Config.java",
                    """
                public class Config {
                    private String apiKey = "sk_live_java123456789012345678901234567890";
                }
            """.trimIndent()
            )

            scanTask.scan()

            val output = testOut.toString()
            assertContains(output, "sk_live_kotlin123456789012345678901234567890")
            assertFalse(output.contains("sk_live_java123456789012345678901234567890"))
        }

        @Test
        @DisplayName("Should respect exclude patterns configuration")
        fun shouldRespectExcludePatternsConfiguration() {
            val extension = project.extensions.getByType(ScanExtension::class.java)
            extension.excludePatterns = listOf("**/test/**", "**/*Test.kt")

            createTestFile(
                    "src/main/kotlin/Config.kt",
                    """
                val apiKey = "sk_live_main123456789012345678901234567890"
            """.trimIndent()
            )

            createTestFile(
                    "src/test/kotlin/ConfigTest.kt",
                    """
                val testApiKey = "sk_live_test123456789012345678901234567890"
            """.trimIndent()
            )

            scanTask.scan()

            val output = testOut.toString()
            assertContains(output, "sk_live_main123456789012345678901234567890")
            assertFalse(output.contains("sk_live_test123456789012345678901234567890"))
        }

        @Test
        @DisplayName("Should handle custom configuration file")
        fun shouldHandleCustomConfigurationFile() {
            createTestFile(
                    "scan-config.yml",
                    """
                enabled: true
                failOnFound: true
                outputFormat: json
                includePatterns:
                  - "**/*.kt"
                  - "**/*.properties"
                excludePatterns:
                  - "**/test/**"
                customPatterns:
                  custom_token:
                    - "custom_[a-zA-Z0-9]{32}"
            """.trimIndent()
            )

            val extension = project.extensions.getByType(ScanExtension::class.java)
            extension.configFile = "scan-config.yml"

            createTestFile(
                    "src/main/kotlin/Config.kt",
                    """
                val customToken = "custom_abcdef123456789012345678901234"
            """.trimIndent()
            )

            scanTask.scan()

            val output = testOut.toString()
            assertContains(output, "custom_abcdef123456789012345678901234")
        }
    }

    @Nested
    @DisplayName("Report Generation")
    inner class ReportGeneration {

        @Test
        @DisplayName("Should generate console report by default")
        fun shouldGenerateConsoleReportByDefault() {
            createTestFile(
                    "src/main/kotlin/Config.kt",
                    """
                val apiKey = "sk_live_console123456789012345678901234567890"
            """.trimIndent()
            )

            scanTask.scan()

            val output = testOut.toString()
            assertContains(output, "sk_live_console123456789012345678901234567890")
            assertContains(output, "Config.kt")
            assertTrue(output.contains("API Key") || output.contains("api_key"))
        }

        @Test
        @DisplayName("Should generate JSON report when configured")
        fun shouldGenerateJsonReportWhenConfigured() {
            val extension = project.extensions.getByType(ScanExtension::class.java)
            extension.outputFormat = "json"
            extension.outputFile = "scan-results.json"

            createTestFile(
                    "src/main/kotlin/Config.kt",
                    """
                val apiKey = "sk_live_json123456789012345678901234567890"
                val dbUrl = "jdbc:mysql://user:pass@localhost:3306/db"
            """.trimIndent()
            )

            scanTask.scan()

            val reportFile = File(project.projectDir, "scan-results.json")
            assertTrue(reportFile.exists())

            val reportContent = reportFile.readText()
            assertContains(reportContent, "sk_live_json123456789012345678901234567890")
            assertContains(reportContent, "jdbc:mysql://user:pass@localhost:3306/db")
            assertContains(reportContent, "Config.kt")

            // Verify JSON structure
            assertTrue(reportContent.contains("\"results\""))
            assertTrue(reportContent.contains("\"type\""))
            assertTrue(reportContent.contains("\"lineNumber\""))
        }

        @Test
        @DisplayName("Should generate HTML report when configured")
        fun shouldGenerateHtmlReportWhenConfigured() {
            val extension = project.extensions.getByType(ScanExtension::class.java)
            extension.outputFormat = "html"
            extension.outputFile = "scan-results.html"

            createTestFile(
                    "src/main/kotlin/Config.kt",
                    """
                val apiKey = "sk_live_html123456789012345678901234567890"
                val githubToken = "ghp_htmltoken123456789012345678901234567890"
            """.trimIndent()
            )

            scanTask.scan()

            val reportFile = File(project.projectDir, "scan-results.html")
            assertTrue(reportFile.exists())

            val reportContent = reportFile.readText()
            assertContains(reportContent, "sk_live_html123456789012345678901234567890")
            assertContains(reportContent, "ghp_htmltoken123456789012345678901234567890")
            assertContains(reportContent, "Config.kt")

            // Verify HTML structure
            assertContains(reportContent, "<html")
            assertContains(reportContent, "<body")
            assertContains(reportContent, "</html>")
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases")
    inner class ErrorHandlingAndEdgeCases {

        @Test
        @DisplayName("Should handle missing source directories gracefully")
        fun shouldHandleMissingSourceDirectoriesGracefully() {
            // Don't create any source files
            scanTask.scan()

            val output = testOut.toString()
            // Should complete without throwing exceptions
            assertTrue(
                    output.contains("No files to scan") ||
                            output.contains("Scan completed") ||
                            output.isEmpty()
            )
        }

        @Test
        @DisplayName("Should handle binary files gracefully")
        fun shouldHandleBinaryFilesGracefully() {
            // Create a binary file
            val binaryFile = File(tempDir.toFile(), "src/main/resources/image.png")
            binaryFile.parentFile.mkdirs()
            binaryFile.writeBytes(
                    byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
            )

            scanTask.scan()

            val output = testOut.toString()
            // Should complete without throwing exceptions
            assertTrue(
                    output.contains("Scan completed") ||
                            output.contains("No secrets found") ||
                            output.isEmpty()
            )
        }

        @Test
        @DisplayName("Should handle very large files")
        fun shouldHandleVeryLargeFiles() {
            val largeContent = buildString {
                repeat(10000) { i ->
                    appendLine("// Comment line $i")
                    if (i % 1000 == 0) {
                        appendLine(
                                """val secret$i = "sk_live_large${i}123456789012345678901234567890""""
                        )
                    }
                }
            }

            createTestFile("src/main/kotlin/LargeFile.kt", largeContent)

            val startTime = System.currentTimeMillis()
            scanTask.scan()
            val endTime = System.currentTimeMillis()

            val output = testOut.toString()
            // Should complete within reasonable time
            assertTrue(endTime - startTime < 30000) // 30 seconds max
            assertTrue(output.contains("sk_live_large0123456789012345678901234567890"))
        }

        @Test
        @DisplayName("Should handle invalid configuration gracefully")
        fun shouldHandleInvalidConfigurationGracefully() {
            createTestFile(
                    "invalid-config.yml",
                    """
                this is not valid yaml content
                enabled: true
                invalid_structure
            """.trimIndent()
            )

            val extension = project.extensions.getByType(ScanExtension::class.java)
            extension.configFile = "invalid-config.yml"

            createTestFile(
                    "src/main/kotlin/Config.kt",
                    """
                val apiKey = "sk_live_invalid123456789012345678901234567890"
            """.trimIndent()
            )

            // Should not throw exception, fall back to defaults
            scanTask.scan()

            val output = testOut.toString()
            assertContains(output, "sk_live_invalid123456789012345678901234567890")
        }
    }

    @Nested
    @DisplayName("Performance and Scalability")
    inner class PerformanceAndScalability {

        @Test
        @DisplayName("Should handle multiple files efficiently")
        fun shouldHandleMultipleFilesEfficiently() {
            // Create multiple test files
            repeat(50) { i ->
                createTestFile(
                        "src/main/kotlin/Config$i.kt",
                        """
                    package com.example.config$i
                    
                    class Config$i {
                        private val apiKey = "sk_live_config${i}_123456789012345678901234567890"
                        private val dbUrl = "jdbc:mysql://user$i:pass$i@localhost:3306/db$i"
                    }
                """.trimIndent()
                )
            }

            val startTime = System.currentTimeMillis()
            scanTask.scan()
            val endTime = System.currentTimeMillis()

            val output = testOut.toString()
            // Should complete within reasonable time
            assertTrue(endTime - startTime < 60000) // 1 minute max

            // Should detect secrets in all files
            assertTrue(output.contains("sk_live_config0_123456789012345678901234567890"))
            assertTrue(output.contains("sk_live_config49_123456789012345678901234567890"))
        }

        @Test
        @DisplayName("Should handle deep directory structures")
        fun shouldHandleDeepDirectoryStructures() {
            // Create deeply nested structure
            createTestFile(
                    "src/main/kotlin/com/example/deep/nested/structure/Config.kt",
                    """
                package com.example.deep.nested.structure
                
                class DeepConfig {
                    private val apiKey = "sk_live_deep123456789012345678901234567890"
                }
            """.trimIndent()
            )

            scanTask.scan()

            val output = testOut.toString()
            assertContains(output, "sk_live_deep123456789012345678901234567890")
            assertContains(output, "structure/Config.kt")
        }
    }

    @Nested
    @DisplayName("Integration with Build Lifecycle")
    inner class IntegrationWithBuildLifecycle {

        @Test
        @DisplayName("Should fail build when failOnFound is enabled")
        fun shouldFailBuildWhenFailOnFoundIsEnabled() {
            val extension = project.extensions.getByType(ScanExtension::class.java)
            extension.failOnFound = true

            createTestFile(
                    "src/main/kotlin/Config.kt",
                    """
                val apiKey = "sk_live_fail123456789012345678901234567890"
            """.trimIndent()
            )

            try {
                scanTask.scan()
                // Should throw exception when secrets are found
                assertTrue(false) // Should not reach here
            } catch (e: Exception) {
                assertContains(e.message ?: "", "found")
            }
        }

        @Test
        @DisplayName("Should integrate with other verification tasks")
        fun shouldIntegrateWithOtherVerificationTasks() {
            // Create a dummy test task
            val testTask = project.tasks.create("test") { it.group = "verification" }

            // Scan task should be part of verification group
            assertEquals("verification", scanTask.group)

            // Should be able to run alongside other verification tasks
            val verificationTasks =
                    project.tasks.withType(Task::class.java).filter { it.group == "verification" }

            assertTrue(verificationTasks.contains(scanTask))
        }
    }

    private fun createTestFile(path: String, content: String) {
        val file = File(tempDir.toFile(), path)
        file.parentFile.mkdirs()
        file.writeText(content)
    }
}
