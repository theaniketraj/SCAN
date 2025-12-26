package com.scan.plugin

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ScanPluginFunctionalTest {

    @TempDir
    lateinit var testProjectDir: File

    private lateinit var settingsFile: File
    private lateinit var buildFile: File

    @BeforeEach
    fun setup() {
        settingsFile = File(testProjectDir, "settings.gradle.kts")
        settingsFile.writeText("rootProject.name = \"scan-test-project\"")

        buildFile = File(testProjectDir, "build.gradle.kts")
        buildFile.writeText("""
            plugins {
                id("io.github.theaniketraj.scan")
            }
            
            scan {
                enabled.set(true)
                failOnFound.set(true)
                entropyThreshold.set(4.5)
                reportFormats.set(setOf("console", "json"))
            }
        """.trimIndent())
    }

    @Test
    fun `scanForSecrets should detect AWS Access Key`() {
        // Create a file with an AWS Access Key
        val srcDir = File(testProjectDir, "src/main/kotlin")
        srcDir.mkdirs()
        
        val secretFile = File(srcDir, "Secrets.kt")
        // "AKIA" followed by 16 alphanumeric chars is a standard pattern for AWS Access Key IDs
        secretFile.writeText("""
            object Secrets {
                const val AWS_KEY = "AKIATXQ73D0192847528" 
            }
        """.trimIndent())

        val gradleHome = File(testProjectDir.parentFile, "gradle-home-${System.currentTimeMillis()}")
        gradleHome.mkdirs()
        
        val runner = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("scanForSecrets", "--stacktrace", "--gradle-user-home", gradleHome.absolutePath)
        
        val result = runner.buildAndFail()
        
        println("DEBUG: Test 1 FAILED as expected. Output:\n" + result.output)
        
        assertTrue(result.output.contains("secrets found") || result.output.contains("Found 1 potential security issues") || result.output.contains("Detected Secrets"), "Output should mention secrets found. Output was: \n${result.output}")
        assertTrue(result.output.contains("AKIATXQ73D0192847528"), "Output should contain the secret or its reference")
        assertEquals(TaskOutcome.FAILED, result.task(":scanForSecrets")?.outcome)
    }
    
    @Test
    fun `scanForSecrets should pass when no secrets present`() {
        // Create a clean file
        val srcDir = File(testProjectDir, "src/main/kotlin")
        srcDir.mkdirs()
        
        val safeFile = File(srcDir, "Safe.kt")
        safeFile.writeText("""
            object Safe {
                const val GREETING = "Hello World"
            }
        """.trimIndent())

        val gradleHome = File(testProjectDir.parentFile, "gradle-home-safe-${System.currentTimeMillis()}")
        gradleHome.mkdirs()

        val runner = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("scanForSecrets", "--stacktrace", "--gradle-user-home", gradleHome.absolutePath)
            
        val result = runner.build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":scanForSecrets")?.outcome)
    }
}
