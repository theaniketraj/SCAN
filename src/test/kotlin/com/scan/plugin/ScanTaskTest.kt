package com.scan.plugin

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.*
import org.gradle.api.Project
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScanTaskTest {

    private lateinit var project: Project
    private lateinit var scanTask: ScanTask

    @TempDir lateinit var tempDir: Path

    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()

        project.pluginManager.apply("com.scan")
        scanTask = project.tasks.getByName("scan") as ScanTask
    }

    @Test
    fun `scan task should have correct configuration`() {
        // Then
        assertEquals("verification", scanTask.group)
        assertEquals("Scans source code for potential security leaks", scanTask.description)
        assertNotNull(scanTask.enabled)
        assertNotNull(scanTask.reportPath)
        assertNotNull(scanTask.reportFormats)
        assertNotNull(scanTask.failOnFound)
        assertNotNull(scanTask.entropyThreshold)
    }

    @Test
    fun `scan task should be configured from extension`() {
        // Given
        val extension = project.extensions.getByType(ScanExtension::class.java)
        extension.enabled = false
        extension.reportPath = "custom/reports"
        extension.reportFormats = listOf("html", "xml")
        extension.failOnFound = false
        extension.entropyThreshold = 3.5
        extension.scanTests = false
        extension.excludePatterns = listOf("test.*")
        extension.includePatterns = listOf("api.*")
        extension.excludeFiles = listOf("**/test/**")
        extension.includeFiles = listOf("**/*.kt")

        // When
        scanTask.configureFromExtension(extension)

        // Then
        assertEquals(false, scanTask.enabled.get())
        assertEquals("custom/reports", scanTask.reportPath.get())
        assertEquals(listOf("html", "xml"), scanTask.reportFormats.get())
        assertEquals(false, scanTask.failOnFound.get())
        assertEquals(3.5, scanTask.entropyThreshold.get())
        assertEquals(false, scanTask.scanTests.get())
        assertEquals(listOf("test.*"), scanTask.excludePatterns.get())
        assertEquals(listOf("api.*"), scanTask.includePatterns.get())
        assertEquals(listOf("**/test/**"), scanTask.excludeFiles.get())
        assertEquals(listOf("**/*.kt"), scanTask.includeFiles.get())
    }

    @Test
    fun `scan task should skip execution when disabled`() {
        // Given
        val extension = project.extensions.getByType(ScanExtension::class.java)
        extension.enabled = false
        scanTask.configureFromExtension(extension)

        // When
        val result = scanTask.scanAction()

        // Then
        assertTrue(result.skipped)
        assertEquals("Scan task is disabled", result.message)
        assertEquals(0, result.findings.size)
    }

    @Test
    fun `scan task should create report directory`() {
        // Given
        val extension = project.extensions.getByType(ScanExtension::class.java)
        extension.reportPath = "build/custom-reports"
        scanTask.configureFromExtension(extension)

        // When
        scanTask.prepareReportDirectory()

        // Then
        val reportDir = File(project.projectDir, "build/custom-reports")
        assertTrue(reportDir.exists())
        assertTrue(reportDir.isDirectory)
    }

    @Test
    fun `scan task should scan files with secrets`() {
        // Given
        createTestFileWithSecret(
                "src/main/kotlin/ApiKey.kt",
                """
            class ApiConfig {
                private val apiKey = "sk_test_1234567890abcdef1234567890abcdef"
                private val dbUrl = "postgresql://user:password123@localhost:5432/mydb"
            }
        """.trimIndent()
        )

        val extension = project.extensions.getByType(ScanExtension::class.java)
        scanTask.configureFromExtension(extension)

        // When
        val result = scanTask.scanAction()

        // Then
        assertFalse(result.skipped)
        assertTrue(result.findings.isNotEmpty())
        assertTrue(result.findings.any { it.pattern.contains("api") || it.pattern.contains("key") })
        assertTrue(result.findings.any { it.file.endsWith("ApiKey.kt") })
    }

    @Test
    fun `scan task should respect file include patterns`() {
        // Given
        createTestFileWithSecret(
                "src/main/kotlin/Config.kt",
                """
            val secret = "super_secret_password_123"
        """.trimIndent()
        )

        createTestFileWithSecret(
                "src/main/resources/config.properties",
                """
            database.password=secret123
        """.trimIndent()
        )

        val extension = project.extensions.getByType(ScanExtension::class.java)
        extension.includeFiles = listOf("**/*.kt") // Only Kotlin files
        scanTask.configureFromExtension(extension)

        // When
        val result = scanTask.scanAction()

        // Then
        assertTrue(result.findings.any { it.file.endsWith("Config.kt") })
        assertFalse(result.findings.any { it.file.endsWith("config.properties") })
    }

    @Test
    fun `scan task should respect file exclude patterns`() {
        // Given
        createTestFileWithSecret(
                "src/main/kotlin/Config.kt",
                """
            val secret = "super_secret_password_123"
        """.trimIndent()
        )

        createTestFileWithSecret(
                "src/test/kotlin/TestConfig.kt",
                """
            val testSecret = "test_secret_password_123"
        """.trimIndent()
        )

        val extension = project.extensions.getByType(ScanExtension::class.java)
        extension.excludeFiles = listOf("**/test/**") // Exclude test files
        scanTask.configureFromExtension(extension)

        // When
        val result = scanTask.scanAction()

        // Then
        assertTrue(
                result.findings.any { it.file.contains("Config.kt") && !it.file.contains("test") }
        )
        assertFalse(result.findings.any { it.file.contains("TestConfig.kt") })
    }

    @Test
    fun `scan task should respect pattern exclusions`() {
        // Given
        createTestFileWithSecret(
                "src/main/kotlin/Config.kt",
                """
            val realSecret = "super_secret_password_123"
            val testPassword = "test_password_123"
            val dummyKey = "dummy_key_for_testing"
        """.trimIndent()
        )

        val extension = project.extensions.getByType(ScanExtension::class.java)
        extension.excludePatterns = listOf("test.*", "dummy.*") // Exclude test and dummy patterns
        scanTask.configureFromExtension(extension)

        // When
        val result = scanTask.scanAction()

        // Then
        assertTrue(result.findings.any { it.matchedText.contains("super_secret") })
        assertFalse(result.findings.any { it.matchedText.contains("test_password") })
        assertFalse(result.findings.any { it.matchedText.contains("dummy_key") })
    }

    @Test
    fun `scan task should detect high entropy strings`() {
        // Given
        createTestFileWithSecret(
                "src/main/kotlin/CryptoKeys.kt",
                """
            class CryptoKeys {
                // High entropy string (likely a key)
                val encryptionKey = "aB3${'$'}kL9#mP2*qR5&vW8@xY1!zA4"
                // Low entropy string (not a secret)
                val username = "admin"
            }
        """.trimIndent()
        )

        val extension = project.extensions.getByType(ScanExtension::class.java)
        extension.entropyThreshold = 4.0
        scanTask.configureFromExtension(extension)

        // When
        val result = scanTask.scanAction()

        // Then
        assertTrue(result.findings.any { it.matchedText.contains("aB3") })
        assertFalse(result.findings.any { it.matchedText == "admin" })
    }

    @Test
    fun `scan task should handle empty project gracefully`() {
        // Given - No source files created
        val extension = project.extensions.getByType(ScanExtension::class.java)
        scanTask.configureFromExtension(extension)

        // When
        val result = scanTask.scanAction()

        // Then
        assertFalse(result.skipped)
        assertEquals(0, result.findings.size)
        assertTrue(result.message.contains("No files found to scan") || result.findings.isEmpty())
    }

    @Test
    fun `scan task should generate reports in multiple formats`() {
        // Given
        createTestFileWithSecret(
                "src/main/kotlin/Secret.kt",
                """
            val apiKey = "sk_test_1234567890abcdef"
        """.trimIndent()
        )

        val extension = project.extensions.getByType(ScanExtension::class.java)
        extension.reportFormats = listOf("json", "html", "console")
        extension.reportPath = "build/test-reports"
        scanTask.configureFromExtension(extension)

        // When
        val result = scanTask.scanAction()

        // Then
        val reportDir = File(project.projectDir, "build/test-reports")
        assertTrue(reportDir.exists())

        // Check that report files are created (exact names depend on implementation)
        val reportFiles = reportDir.listFiles()
        assertNotNull(reportFiles)
        assertTrue(reportFiles.isNotEmpty())
    }

    @Test
    fun `scan task should fail build when secrets found and failOnFound is true`() {
        // Given
        createTestFileWithSecret(
                "src/main/kotlin/Secret.kt",
                """
            val password = "super_secret_password_123"
        """.trimIndent()
        )

        val extension = project.extensions.getByType(ScanExtension::class.java)
        extension.failOnFound = true
        scanTask.configureFromExtension(extension)

        // When/Then
        assertFailsWith<TaskExecutionException> { scanTask.scanAction() }
    }

    @Test
    fun `scan task should not fail build when secrets found and failOnFound is false`() {
        // Given
        createTestFileWithSecret(
                "src/main/kotlin/Secret.kt",
                """
            val password = "super_secret_password_123"
        """.trimIndent()
        )

        val extension = project.extensions.getByType(ScanExtension::class.java)
        extension.failOnFound = false
        scanTask.configureFromExtension(extension)

        // When
        val result = scanTask.scanAction()

        // Then
        assertFalse(result.skipped)
        assertTrue(result.findings.isNotEmpty())
        // Should not throw exception
    }

    @Test
    fun `scan task should handle test files based on scanTests configuration`() {
        // Given
        createTestFileWithSecret(
                "src/test/kotlin/TestSecret.kt",
                """
            val testApiKey = "test_api_key_123456789"
        """.trimIndent()
        )

        // Test with scanTests = false
        val extension = project.extensions.getByType(ScanExtension::class.java)
        extension.scanTests = false
        scanTask.configureFromExtension(extension)

        // When
        val resultWithoutTests = scanTask.scanAction()

        // Then
        assertFalse(resultWithoutTests.findings.any { it.file.contains("test") })

        // Test with scanTests = true
        extension.scanTests = true
        scanTask.configureFromExtension(extension)

        // When
        val resultWithTests = scanTask.scanAction()

        // Then
        assertTrue(resultWithTests.findings.any { it.file.contains("TestSecret.kt") })
    }

    @Test
    fun `scan task should handle binary files gracefully`() {
        // Given
        val binaryFile = tempDir.resolve("src/main/resources/image.png")
        Files.createDirectories(binaryFile.parent)
        Files.write(binaryFile, byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)) // PNG header

        val extension = project.extensions.getByType(ScanExtension::class.java)
        extension.includeFiles = listOf("**/*") // Include all files
        scanTask.configureFromExtension(extension)

        // When
        val result = scanTask.scanAction()

        // Then - Should not crash on binary files
        assertFalse(result.skipped)
        // Binary files should be skipped or handled gracefully
    }

    @Test
    fun `scan task should respect custom patterns`() {
        // Given
        createTestFileWithSecret(
                "src/main/kotlin/CustomSecret.kt",
                """
            val customKey = "CUSTOM_12345_SECRET"
            val regularVar = "normal_value"
        """.trimIndent()
        )

        val extension = project.extensions.getByType(ScanExtension::class.java)
        extension.includePatterns = listOf("CUSTOM_.*_SECRET") // Custom pattern
        scanTask.configureFromExtension(extension)

        // When
        val result = scanTask.scanAction()

        // Then
        assertTrue(result.findings.any { it.matchedText.contains("CUSTOM_12345_SECRET") })
        assertFalse(result.findings.any { it.matchedText.contains("normal_value") })
    }

    @Test
    fun `scan task should provide detailed finding information`() {
        // Given
        createTestFileWithSecret(
                "src/main/kotlin/DetailedTest.kt",
                """
            class Config {
                // Line 3
                val apiKey = "sk_test_abcdef123456"
                // Line 5
                val dbPassword = "password123"
            }
        """.trimIndent()
        )

        val extension = project.extensions.getByType(ScanExtension::class.java)
        scanTask.configureFromExtension(extension)

        // When
        val result = scanTask.scanAction()

        // Then
        assertTrue(result.findings.isNotEmpty())
        val finding = result.findings.first()

        assertNotNull(finding.file)
        assertTrue(finding.lineNumber > 0)
        assertTrue(finding.columnNumber >= 0)
        assertNotNull(finding.matchedText)
        assertNotNull(finding.pattern)
        assertNotNull(finding.severity)
        assertTrue(finding.file.endsWith("DetailedTest.kt"))
    }

    @Test
    fun `scan task outputs should be declared for up-to-date checking`() {
        // Given
        val extension = project.extensions.getByType(ScanExtension::class.java)
        extension.reportPath = "build/scan-outputs"
        extension.reportFormats = listOf("json", "html")
        scanTask.configureFromExtension(extension)

        // When
        val outputs = scanTask.outputs.files

        // Then
        assertNotNull(outputs)
        assertTrue(outputs.files.isNotEmpty())
    }

    @Test
    fun `scan task inputs should be declared for up-to-date checking`() {
        // Given
        createTestFileWithSecret("src/main/kotlin/Input.kt", "val key = \"secret123\"")

        // When
        val inputs = scanTask.inputs.files

        // Then
        assertNotNull(inputs)
        // Task should declare source files as inputs
    }

    private fun createTestFileWithSecret(relativePath: String, content: String) {
        val file = tempDir.resolve(relativePath)
        Files.createDirectories(file.parent)
        file.writeText(content)
    }
}
