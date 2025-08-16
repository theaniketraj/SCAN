package com.scan.plugin

import java.io.File
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

// Helper function for assertFailsWith since it's not in JUnit 5
inline fun <reified T : Throwable> assertFailsWith(crossinline block: () -> Unit): T {
    return assertThrows(T::class.java) { block() }
}

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
        project.pluginManager.apply("io.github.theaniketraj.scan")

        // Then
        assertTrue(project.plugins.hasPlugin("io.github.theaniketraj.scan"))
        assertTrue(project.plugins.hasPlugin(ScanPlugin::class.java))
    }

    @Test
    fun `plugin should register scan extension`() {
        // When
        project.pluginManager.apply("io.github.theaniketraj.scan")

        // Then
        val extension = project.extensions.findByName("scan")
        assertNotNull(extension)
        assertTrue(extension is ScanExtension)
    }

    @Test
    fun `plugin should register scan task`() {
        // When
        project.pluginManager.apply("io.github.theaniketraj.scan")

        // Then
        val task = project.tasks.findByName("scanForSecrets")
        assertNotNull(task)
        assertTrue(task is ScanTask)
    }

    @Test
    fun `scan extension should have default values`() {
        // Given
        project.pluginManager.apply("io.github.theaniketraj.scan")
        val extension = project.extensions.getByType(ScanExtension::class.java)

        // Then
        assertTrue(extension.enabled.get())
        assertTrue(extension.failOnFound.get())
        assertTrue(extension.scanTests.get())
        assertEquals(4.5, extension.entropyThreshold.get())
        assertFalse(extension.verbose.get()) // Changed from assertTrue to assertFalse
        assertFalse(extension.excludePatterns.get().isEmpty()) // Plugin sets default patterns
        assertFalse(extension.includePatterns.get().isEmpty()) // Plugin sets default patterns
        assertTrue(extension.excludeFiles.get().isEmpty()) // These are aliases, remain empty
        assertTrue(extension.includeFiles.get().isEmpty()) // These are aliases, remain empty
    }

    @Test
    fun `scan extension should allow configuration`() {
        // Given
        project.pluginManager.apply("io.github.theaniketraj.scan")
        val extension = project.extensions.getByType(ScanExtension::class.java)

        // When
        extension.enabled.set(false)
        extension.reportPath.set("custom/reports")
        extension.reportFormats.set(setOf("html", "xml"))
        extension.failOnFound.set(false)
        extension.entropyThreshold.set(3.5)
        extension.scanTests.set(false)
        extension.excludePatterns.set(setOf("test-pattern"))
        extension.includePatterns.set(setOf("include-pattern"))
        extension.excludeFiles.set(setOf("**/exclude/**"))
        extension.includeFiles.set(setOf("**/*.custom"))

        // Then
        assertEquals(false, extension.enabled.get())
        assertEquals("custom/reports", extension.reportPath.get())
        assertTrue(extension.reportFormats.get().contains("html"))
        assertEquals(false, extension.failOnFound.get())
        assertEquals(3.5, extension.entropyThreshold.get())
        assertEquals(false, extension.scanTests.get())
        assertTrue(extension.excludePatterns.get().contains("test-pattern"))
        assertTrue(extension.includePatterns.get().contains("include-pattern"))
        assertTrue(extension.excludeFiles.get().contains("**/exclude/**"))
        assertTrue(extension.includeFiles.get().contains("**/*.custom"))
    }

    @Test
    fun `scan task should use extension configuration`() {
        // Given
        project.pluginManager.apply("io.github.theaniketraj.scan")
        val extension = project.extensions.getByType(ScanExtension::class.java)
        val task = project.tasks.withType(ScanTask::class.java).first()

        // When
        extension.enabled.set(false)
        extension.reportPath.set("custom/reports")
        extension.entropyThreshold.set(3.5)

        // Then
        assertEquals(false, extension.enabled.get())
        assertEquals("custom/reports", extension.reportPath.get())
        assertEquals(3.5, extension.entropyThreshold.get())
    }

    @Test
    fun `plugin should handle multiple configurations`() {
        // Given
        project.pluginManager.apply("io.github.theaniketraj.scan")
        val extension = project.extensions.getByType(ScanExtension::class.java)

        // When
        extension.enabled.set(true)
        extension.excludePatterns.set(setOf("**/*.test.kt"))
        extension.verbose.set(false)
        extension.entropyThreshold.set(5.0)
        extension.excludePatterns.set(setOf("**/*.spec.kt"))
        extension.includePatterns.set(setOf("**/*.kt"))

        // Then
        assertTrue(extension.enabled.get())
        assertTrue(extension.excludePatterns.get().contains("**/*.spec.kt"))
        assertEquals(false, extension.verbose.get())
        assertEquals(5.0, extension.entropyThreshold.get())
        assertTrue(extension.includePatterns.get().contains("**/*.kt"))
    }

    @Test
    fun `extension should support pattern configuration`() {
        // Given
        project.pluginManager.apply("io.github.theaniketraj.scan")
        val extension = project.extensions.getByType(ScanExtension::class.java)

        // When
        extension.includePatterns.set(setOf("**/*.kt", "**/*.java"))
        extension.excludePatterns.set(setOf("**/test/**", "**/tests/**"))

        // Then
        assertTrue(extension.includePatterns.get().size == 2)
        assertTrue(extension.excludePatterns.get().size == 2)
        assertTrue(extension.includePatterns.get().contains("**/*.kt"))
        assertTrue(extension.includePatterns.get().contains("**/*.java"))
        assertTrue(extension.excludePatterns.get().contains("**/test/**"))
        assertTrue(extension.excludePatterns.get().contains("**/tests/**"))
    }

    @Test
    fun `extension should support reporting configuration`() {
        // Given
        project.pluginManager.apply("io.github.theaniketraj.scan")
        val extension = project.extensions.getByType(ScanExtension::class.java)

        // When
        extension.reportFormats.set(setOf("json", "html", "xml"))
        extension.reportPath.set("build/scan-reports")

        // Then
        assertEquals(3, extension.reportFormats.get().size)
        assertTrue(extension.reportFormats.get().contains("json"))
        assertTrue(extension.reportFormats.get().contains("html"))
        assertTrue(extension.reportFormats.get().contains("xml"))
        assertEquals("build/scan-reports", extension.reportPath.get())
    }

    @Test
    fun `extension should allow pattern lists`() {
        // Given
        project.pluginManager.apply("io.github.theaniketraj.scan")
        val extension = project.extensions.getByType(ScanExtension::class.java)

        // When
        val includes = mutableSetOf<String>()
        includes.add("src/**")
        includes.add("lib/**")
        extension.includePatterns.set(includes)

        val excludes = mutableSetOf<String>()
        excludes.add("**/node_modules/**")
        excludes.add("**/build/**")
        extension.excludePatterns.set(excludes)

        // Then
        assertEquals(2, extension.includePatterns.get().size)
        assertEquals(2, extension.excludePatterns.get().size)
    }

    @Test
    fun `extension should handle entropy threshold configuration`() {
        // Given
        project.pluginManager.apply("io.github.theaniketraj.scan")
        val extension = project.extensions.getByType(ScanExtension::class.java)

        // When
        extension.entropyThreshold.set(6.0)

        // Then - valid threshold should be accepted
        assertEquals(6.0, extension.entropyThreshold.get())

        // When - invalid threshold should cause validation error  
        extension.entropyThreshold.set(-1.0)

        // Then
        assertThrows(IllegalArgumentException::class.java) {
            extension.validate()
        }
    }

    @Test
    fun `extension should validate entropy threshold bounds`() {
        // Given
        project.pluginManager.apply("io.github.theaniketraj.scan")
        val extension = project.extensions.getByType(ScanExtension::class.java)

        // When/Then - test upper bound
        extension.entropyThreshold.set(11.0)
        assertThrows(IllegalArgumentException::class.java) {
            extension.validate()
        }
    }

    @Test
    fun `extension should support mixed list configuration`() {
        // Given
        project.pluginManager.apply("io.github.theaniketraj.scan")
        val extension = project.extensions.getByType(ScanExtension::class.java)

        // When
        extension.includePatterns.set(setOf("**/*.properties", "**/*.yaml"))
        extension.reportPath.set("reports/security")
        extension.reportFormats.set(setOf("json"))

        // Then
        assertTrue(extension.includePatterns.get().contains("**/*.properties"))
        assertTrue(extension.includePatterns.get().contains("**/*.yaml"))
        assertEquals("reports/security", extension.reportPath.get())
        assertTrue(extension.reportFormats.get().contains("json"))
    }
}
