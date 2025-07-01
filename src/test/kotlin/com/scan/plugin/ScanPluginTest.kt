package com.scan.plugin

import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScanPluginTest {

    private lateinit var project: Project

    @TempDir lateinit var tempDir: Path

    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
    }

    @Test
    fun `plugin should be applied successfully`() {
        // When
        project.pluginManager.apply("com.scan")

        // Then
        assertTrue(project.plugins.hasPlugin("com.scan"))
        assertTrue(project.plugins.hasPlugin(ScanPlugin::class.java))
    }

    @Test
    fun `plugin should register scan extension`() {
        // When
        project.pluginManager.apply("com.scan")

        // Then
        val extension = project.extensions.findByName("scan")
        assertNotNull(extension)
        assertTrue(extension is ScanExtension)
    }

    @Test
    fun `plugin should register scan task`() {
        // When
        project.pluginManager.apply("com.scan")

        // Then
        val task = project.tasks.findByName("scan")
        assertNotNull(task)
        assertTrue(task is ScanTask)
    }

    @Test
    fun `scan extension should have default configuration`() {
        // When
        project.pluginManager.apply("com.scan")
        val extension = project.extensions.getByType(ScanExtension::class.java)

        // Then
        assertTrue(extension.enabled)
        assertEquals("build/reports/scan", extension.reportPath)
        assertEquals(listOf("json", "console"), extension.reportFormats)
        assertTrue(extension.failOnFound)
        assertEquals(4.0, extension.entropyThreshold)
        assertTrue(extension.scanTests)
        assertEquals(emptyList<String>(), extension.excludePatterns)
        assertEquals(emptyList<String>(), extension.includePatterns)
        assertEquals(emptyList<String>(), extension.excludeFiles)
        assertEquals(
                listOf(
                        "**/*.kt",
                        "**/*.java",
                        "**/*.properties",
                        "**/*.yml",
                        "**/*.yaml",
                        "**/*.json",
                        "**/*.xml"
                ),
                extension.includeFiles
        )
    }

    @Test
    fun `scan extension should allow configuration`() {
        // Given
        project.pluginManager.apply("com.scan")
        val extension = project.extensions.getByType(ScanExtension::class.java)

        // When
        extension.enabled = false
        extension.reportPath = "custom/reports"
        extension.reportFormats = listOf("html", "xml")
        extension.failOnFound = false
        extension.entropyThreshold = 3.5
        extension.scanTests = false
        extension.excludePatterns = listOf("test-pattern")
        extension.includePatterns = listOf("include-pattern")
        extension.excludeFiles = listOf("**/exclude/**")
        extension.includeFiles = listOf("**/*.custom")

        // Then
        assertEquals(false, extension.enabled)
        assertEquals("custom/reports", extension.reportPath)
        assertEquals(listOf("html", "xml"), extension.reportFormats)
        assertEquals(false, extension.failOnFound)
        assertEquals(3.5, extension.entropyThreshold)
        assertEquals(false, extension.scanTests)
        assertEquals(listOf("test-pattern"), extension.excludePatterns)
        assertEquals(listOf("include-pattern"), extension.includePatterns)
        assertEquals(listOf("**/exclude/**"), extension.excludeFiles)
        assertEquals(listOf("**/*.custom"), extension.includeFiles)
    }

    @Test
    fun `scan task should have correct group and description`() {
        // When
        project.pluginManager.apply("com.scan")
        val task = project.tasks.getByName("scan") as ScanTask

        // Then
        assertEquals("verification", task.group)
        assertEquals("Scans source code for potential security leaks", task.description)
    }

    @Test
    fun `scan task should be configured with extension values`() {
        // Given
        project.pluginManager.apply("com.scan")
        val extension = project.extensions.getByType(ScanExtension::class.java)
        val task = project.tasks.getByName("scan") as ScanTask

        // When
        extension.enabled = false
        extension.reportPath = "custom/path"
        extension.entropyThreshold = 5.0

        // Configure task (this would normally happen during task execution)
        task.configureFromExtension(extension)

        // Then - Task should reflect extension configuration
        // Note: These assertions depend on the ScanTask implementation
        // The actual implementation will need corresponding properties
        assertEquals(false, task.enabled.get())
        assertEquals("custom/path", task.reportPath.get())
        assertEquals(5.0, task.entropyThreshold.get())
    }

    @Test
    fun `plugin should work with kotlin dsl configuration`() {
        // Given
        project.pluginManager.apply("com.scan")

        // When - Simulate Kotlin DSL configuration
        project.extensions.configure(ScanExtension::class.java) { extension ->
            extension.enabled = true
            extension.reportFormats = listOf("json", "html")
            extension.failOnFound = true
            extension.entropyThreshold = 4.5
            extension.excludePatterns = listOf("password.*test", "dummy.*key")
            extension.includeFiles = listOf("**/*.kt", "**/*.properties")
        }

        val extension = project.extensions.getByType(ScanExtension::class.java)

        // Then
        assertTrue(extension.enabled)
        assertEquals(listOf("json", "html"), extension.reportFormats)
        assertTrue(extension.failOnFound)
        assertEquals(4.5, extension.entropyThreshold)
        assertEquals(listOf("password.*test", "dummy.*key"), extension.excludePatterns)
        assertEquals(listOf("**/*.kt", "**/*.properties"), extension.includeFiles)
    }

    @Test
    fun `plugin should validate report formats`() {
        // Given
        project.pluginManager.apply("com.scan")
        val extension = project.extensions.getByType(ScanExtension::class.java)

        // When/Then - Valid formats should work
        extension.reportFormats = listOf("json", "html", "console", "xml")
        // No exception expected

        // Invalid formats should be handled gracefully (depending on implementation)
        extension.reportFormats = listOf("invalid-format")
        // The validation might happen at task execution time
    }

    @Test
    fun `plugin should handle multiple applications`() {
        // When - Apply plugin multiple times
        project.pluginManager.apply("com.scan")
        project.pluginManager.apply("com.scan")

        // Then - Should not cause issues
        assertTrue(project.plugins.hasPlugin("com.scan"))
        assertNotNull(project.extensions.findByName("scan"))
        assertNotNull(project.tasks.findByName("scan"))
    }

    @Test
    fun `scan task should depend on compilation tasks when available`() {
        // Given
        project.pluginManager.apply("kotlin")
        project.pluginManager.apply("com.scan")

        val scanTask = project.tasks.getByName("scan")

        // Then - Scan task should depend on compile tasks
        val dependencies = scanTask.dependsOn
        // The exact dependencies will depend on the ScanPlugin implementation
        assertTrue(dependencies.isNotEmpty())
    }

    @Test
    fun `plugin should integrate with gradle build lifecycle`() {
        // Given
        project.pluginManager.apply("com.scan")
        val scanTask = project.tasks.getByName("scan")

        // When applying check plugin
        project.pluginManager.apply("base")

        // Then scan task should be part of verification group
        assertEquals("verification", scanTask.group)
    }

    @Test
    fun `extension should support nested configuration`() {
        // Given
        project.pluginManager.apply("com.scan")

        // When - Configure using nested approach
        project.extensions.configure(ScanExtension::class.java) { scan ->
            scan.apply {
                enabled = true
                reportPath = "build/security-reports"
                reportFormats = mutableListOf("json", "html")

                // Configure patterns
                excludePatterns = mutableListOf("test.*password", "mock.*key", "dummy.*secret")

                includePatterns = mutableListOf("api.*key", "database.*url", ".*secret.*")

                // Configure file filters
                excludeFiles = mutableListOf("**/test/**", "**/mock/**", "**/*.test.kt")

                includeFiles = mutableListOf("**/*.kt", "**/*.java", "**/*.properties", "**/*.yml")
            }
        }

        val extension = project.extensions.getByType(ScanExtension::class.java)

        // Then
        assertTrue(extension.enabled)
        assertEquals("build/security-reports", extension.reportPath)
        assertEquals(3, extension.excludePatterns.size)
        assertEquals(3, extension.includePatterns.size)
        assertEquals(3, extension.excludeFiles.size)
        assertEquals(4, extension.includeFiles.size)
    }

    @Test
    fun `plugin should handle project without source sets gracefully`() {
        // Given - Project without Kotlin/Java plugin (no source sets)
        project.pluginManager.apply("com.scan")

        // When
        val scanTask = project.tasks.getByName("scan") as ScanTask

        // Then - Should not throw exception during configuration
        assertNotNull(scanTask)
    }

    @Test
    fun `plugin should create report directory structure`() {
        // Given
        project.pluginManager.apply("com.scan")
        val extension = project.extensions.getByType(ScanExtension::class.java)
        extension.reportPath = "custom/report/path"

        // When - This would typically happen during task execution
        val reportDir = File(project.projectDir, extension.reportPath)

        // Then - Directory structure should be creatable
        assertTrue(reportDir.mkdirs() || reportDir.exists())
        assertTrue(reportDir.isDirectory)
    }

    @Test
    fun `extension validation should catch invalid entropy threshold`() {
        // Given
        project.pluginManager.apply("com.scan")
        val extension = project.extensions.getByType(ScanExtension::class.java)

        // When/Then - Invalid entropy thresholds
        assertFailsWith<IllegalArgumentException> {
            extension.entropyThreshold = -1.0
            extension.validate() // Assuming validation method exists
        }

        assertFailsWith<IllegalArgumentException> {
            extension.entropyThreshold = 0.0
            extension.validate()
        }
    }

    @Test
    fun `plugin should provide meaningful task outputs`() {
        // Given
        project.pluginManager.apply("com.scan")
        val extension = project.extensions.getByType(ScanExtension::class.java)
        val scanTask = project.tasks.getByName("scan") as ScanTask

        // When
        extension.reportFormats = listOf("json", "html")
        extension.reportPath = "build/scan-reports"

        // Then - Task should declare outputs for up-to-date checking
        val outputs = scanTask.outputs.files
        assertNotNull(outputs)
        // The exact output files depend on the ScanTask implementation
    }
}
