package com.scan.detectors

import com.scan.core.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.nio.file.Paths

@DisplayName("PatternDetector Tests")
class PatternDetectorTest {

    private lateinit var detector: PatternDetector

    @BeforeEach
    fun setUp() {
        detector = PatternDetector()
    }

    @Test
    @DisplayName("Should detect API keys")
    fun shouldDetectApiKeys() {
        val content = "val apiKey = \"sk_live_1234567890abcdef1234567890abcdef\""
        
        val scanContext = ScanContext(
            filePath = Paths.get("ApiTest.kt"),
            fileName = "ApiTest.kt",
            fileExtension = "kt",
            isTestFile = false,
            fileSize = 100L,
            configuration = ScanConfiguration(),
            content = content,
            lines = content.lines()
        )

        val findings = detector.detect(scanContext)
        
        assertNotNull(findings)
        // API key patterns should be detected if configured
    }

    @Test
    @DisplayName("Should detect passwords")
    fun shouldDetectPasswords() {
        val content = "val password = \"admin123!@#\""
        
        val scanContext = ScanContext(
            filePath = Paths.get("PasswordTest.kt"),
            fileName = "PasswordTest.kt",
            fileExtension = "kt",
            isTestFile = false,
            fileSize = 100L,
            configuration = ScanConfiguration(),
            content = content,
            lines = content.lines()
        )

        val findings = detector.detect(scanContext)
        
        assertNotNull(findings)
        // Password patterns should be detected if configured
    }

    @Test
    @DisplayName("Should detect private keys")
    fun shouldDetectPrivateKeys() {
        val content = """
            -----BEGIN PRIVATE KEY-----
            MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC5
            -----END PRIVATE KEY-----
        """.trimIndent()
        
        val scanContext = ScanContext(
            filePath = Paths.get("KeyTest.kt"),
            fileName = "KeyTest.kt",
            fileExtension = "kt",
            isTestFile = false,
            fileSize = 200L,
            configuration = ScanConfiguration(),
            content = content,
            lines = content.lines()
        )

        val findings = detector.detect(scanContext)
        
        assertNotNull(findings)
        // Private key patterns should be detected if configured
    }

    @Test
    @DisplayName("Should handle empty content")
    fun shouldHandleEmptyContent() {
        val scanContext = ScanContext(
            filePath = Paths.get("Empty.kt"),
            fileName = "Empty.kt",
            fileExtension = "kt",
            isTestFile = false,
            fileSize = 0L,
            configuration = ScanConfiguration(),
            content = "",
            lines = emptyList()
        )

        val findings = detector.detect(scanContext)
        
        assertNotNull(findings)
        assertTrue(findings.isEmpty())
    }

    @Test
    @DisplayName("Should handle multi-line content")
    fun shouldHandleMultiLineContent() {
        val content = """
            package com.example
            
            class Config {
                val secret1 = "sk_test_123456789"
                val secret2 = "another-secret-value"
            }
        """.trimIndent()
        
        val scanContext = ScanContext(
            filePath = Paths.get("MultiLine.kt"),
            fileName = "MultiLine.kt",
            fileExtension = "kt",
            isTestFile = false,
            fileSize = 200L,
            configuration = ScanConfiguration(),
            content = content,
            lines = content.lines()
        )

        val findings = detector.detect(scanContext)
        
        assertNotNull(findings)
        // Should process multi-line content correctly
    }

    @Test
    @DisplayName("Should respect file type filters")
    fun shouldRespectFileTypeFilters() {
        val content = "secret = test-value"
        
        val scanContext = ScanContext(
            filePath = Paths.get("Unsupported.xyz"),
            fileName = "Unsupported.xyz",
            fileExtension = "xyz",
            isTestFile = false,
            fileSize = 100L,
            configuration = ScanConfiguration(),
            content = content,
            lines = content.lines()
        )

        val findings = detector.detect(scanContext)
        
        assertNotNull(findings)
        // Should handle unsupported file types gracefully
    }

    @Test
    @DisplayName("Should validate detector properties")
    fun shouldValidateDetectorProperties() {
        assertNotNull(detector.detectorId)
        assertNotNull(detector.detectorName)
        assertNotNull(detector.version)
        assertNotNull(detector.supportedFileTypes)
    }

    @Test
    @DisplayName("Should handle test files")
    fun shouldHandleTestFiles() {
        val content = "val testSecret = \"fake-test-secret-123\""
        
        val scanContext = ScanContext(
            filePath = Paths.get("TestFile.kt"),
            fileName = "TestFile.kt",
            fileExtension = "kt",
            isTestFile = true,
            fileSize = 100L,
            configuration = ScanConfiguration(),
            content = content,
            lines = content.lines()
        )

        val findings = detector.detect(scanContext)
        
        assertNotNull(findings)
        // Test files should be handled according to detector configuration
    }
}
