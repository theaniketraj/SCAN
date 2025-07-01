package com.scan.plugin.integration

import com.scan.core.ScanConfiguration
import com.scan.core.ScanEngine
import com.scan.detectors.*
import com.scan.filters.*
import com.scan.patterns.SecretPatterns
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir

@DisplayName("Performance Tests")
class PerformanceTest {

    @TempDir lateinit var tempDir: Path

    private lateinit var scanEngine: ScanEngine
    private lateinit var testFiles: List<File>
    private lateinit var largeTestFiles: List<File>

    @BeforeEach
    fun setUp() {
        // Initialize scan engine with performance-optimized configuration
        val configuration =
                ScanConfiguration(
                        targetPaths = listOf(tempDir.toString()),
                        excludePatterns = listOf("*.log", "*.tmp"),
                        includePatterns =
                                listOf("*.kt", "*.java", "*.properties", "*.json", "*.xml"),
                        maxFileSize = 10 * 1024 * 1024, // 10MB
                        parallelScanning = true,
                        threadPoolSize = Runtime.getRuntime().availableProcessors(),
                        enableEntropyDetection = true,
                        enablePatternDetection = true,
                        enableContextAwareDetection = true
                )

        val detectors =
                listOf(
                        PatternDetector(SecretPatterns.getAllPatterns()),
                        EntropyDetector(threshold = 4.5),
                        ContextAwareDetector()
                )

        val filters =
                listOf(
                        FileExtensionFilter(
                                configuration.includePatterns,
                                configuration.excludePatterns
                        ),
                        PathFilter(configuration.excludePatterns),
                        TestFileFilter()
                )

        scanEngine = ScanEngine(configuration, detectors, filters)

        // Create test files for performance testing
        createTestFiles()
        createLargeTestFiles()
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("Small Files Performance Test")
    fun testSmallFilesPerformance() {
        val executionTime = measureTimeMillis {
            val results = scanEngine.scanFiles(testFiles)
            assertNotNull(results)
        }

        println("Small files scan completed in: ${executionTime}ms")
        println("Files scanned: ${testFiles.size}")
        println("Average time per file: ${executionTime / testFiles.size}ms")

        // Performance assertion: should complete within reasonable time
        assertTrue(executionTime < 5000, "Small files scan took too long: ${executionTime}ms")
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @DisplayName("Large Files Performance Test")
    fun testLargeFilesPerformance() {
        val executionTime = measureTimeMillis {
            val results = scanEngine.scanFiles(largeTestFiles)
            assertNotNull(results)
        }

        println("Large files scan completed in: ${executionTime}ms")
        println("Files scanned: ${largeTestFiles.size}")
        println("Average time per file: ${executionTime / largeTestFiles.size}ms")

        // Performance assertion: should complete within reasonable time
        assertTrue(executionTime < 30000, "Large files scan took too long: ${executionTime}ms")
    }

    @Test
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    @DisplayName("Parallel vs Sequential Performance Comparison")
    fun testParallelVsSequentialPerformance() {
        // Test with parallel scanning enabled
        val parallelConfig =
                ScanConfiguration(
                        targetPaths = listOf(tempDir.toString()),
                        parallelScanning = true,
                        threadPoolSize = Runtime.getRuntime().availableProcessors()
                )
        val parallelEngine = createScanEngine(parallelConfig)

        val parallelTime = measureTimeMillis {
            parallelEngine.scanFiles(testFiles + largeTestFiles)
        }

        // Test with sequential scanning
        val sequentialConfig =
                ScanConfiguration(
                        targetPaths = listOf(tempDir.toString()),
                        parallelScanning = false,
                        threadPoolSize = 1
                )
        val sequentialEngine = createScanEngine(sequentialConfig)

        val sequentialTime = measureTimeMillis {
            sequentialEngine.scanFiles(testFiles + largeTestFiles)
        }

        println("Parallel scanning time: ${parallelTime}ms")
        println("Sequential scanning time: ${sequentialTime}ms")
        println(
                "Performance improvement: ${((sequentialTime - parallelTime).toDouble() / sequentialTime * 100).toInt()}%"
        )

        // Parallel should be faster (with some tolerance for small datasets)
        assertTrue(
                parallelTime <= sequentialTime * 1.1,
                "Parallel scanning should be faster or comparable: parallel=${parallelTime}ms, sequential=${sequentialTime}ms"
        )
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    @DisplayName("Memory Usage Test")
    fun testMemoryUsage() {
        val runtime = Runtime.getRuntime()

        // Force garbage collection and measure initial memory
        System.gc()
        Thread.sleep(100)
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // Perform scanning
        val results = scanEngine.scanFiles(testFiles + largeTestFiles)

        // Measure memory after scanning
        val peakMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsed = peakMemory - initialMemory

        println("Initial memory: ${initialMemory / 1024 / 1024}MB")
        println("Peak memory: ${peakMemory / 1024 / 1024}MB")
        println("Memory used for scanning: ${memoryUsed / 1024 / 1024}MB")
        println("Results found: ${results.violations.size}")

        // Memory usage should be reasonable (less than 500MB for test files)
        assertTrue(
                memoryUsed < 500 * 1024 * 1024,
                "Memory usage too high: ${memoryUsed / 1024 / 1024}MB"
        )
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    @DisplayName("Detector Performance Comparison")
    fun testDetectorPerformance() {
        val testFile = createFileWithContent("test-secrets.kt", generateTestContentWithSecrets())

        // Test individual detector performance
        val patternDetector = PatternDetector(SecretPatterns.getAllPatterns())
        val entropyDetector = EntropyDetector(threshold = 4.5)
        val contextAwareDetector = ContextAwareDetector()

        val patternTime = measureTimeMillis {
            repeat(100) { patternDetector.detect(testFile, testFile.readText()) }
        }

        val entropyTime = measureTimeMillis {
            repeat(100) { entropyDetector.detect(testFile, testFile.readText()) }
        }

        val contextTime = measureTimeMillis {
            repeat(100) { contextAwareDetector.detect(testFile, testFile.readText()) }
        }

        println("Pattern detector (100 runs): ${patternTime}ms")
        println("Entropy detector (100 runs): ${entropyTime}ms")
        println("Context-aware detector (100 runs): ${contextTime}ms")

        // All detectors should complete within reasonable time
        assertTrue(patternTime < 5000, "Pattern detector too slow: ${patternTime}ms")
        assertTrue(entropyTime < 3000, "Entropy detector too slow: ${entropyTime}ms")
        assertTrue(contextTime < 7000, "Context-aware detector too slow: ${contextTime}ms")
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("File Filter Performance Test")
    fun testFileFilterPerformance() {
        // Create many files to test filter performance
        val manyFiles = (1..1000).map { index -> createDummyFile("file_$index.kt") }

        val filters =
                listOf(
                        FileExtensionFilter(listOf("*.kt", "*.java"), listOf("*.tmp", "*.log")),
                        PathFilter(listOf("**/build/**", "**/target/**")),
                        TestFileFilter()
                )

        val filterTime = measureTimeMillis {
            val filteredFiles =
                    manyFiles.filter { file ->
                        filters.all { filter -> filter.shouldInclude(file) }
                    }
            println("Filtered ${manyFiles.size} files down to ${filteredFiles.size}")
        }

        println("File filtering time: ${filterTime}ms")
        println("Average time per file: ${filterTime.toDouble() / manyFiles.size}ms")

        // Filtering should be very fast
        assertTrue(filterTime < 1000, "File filtering too slow: ${filterTime}ms")
    }

    @Test
    @Timeout(value = 25, unit = TimeUnit.SECONDS)
    @DisplayName("Stress Test - Many Files")
    fun testStressTestManyFiles() {
        // Create a large number of files for stress testing
        val stressTestFiles =
                (1..500).map { index ->
                    val content =
                            if (index % 10 == 0) {
                                generateTestContentWithSecrets()
                            } else {
                                generateCleanContent()
                            }
                    createFileWithContent("stress_file_$index.kt", content)
                }

        val stressTime = measureTimeMillis {
            val results = scanEngine.scanFiles(stressTestFiles)
            println("Stress test found ${results.violations.size} violations")
        }

        println("Stress test completed in: ${stressTime}ms")
        println("Files processed: ${stressTestFiles.size}")
        println("Throughput: ${stressTestFiles.size * 1000 / stressTime} files/second")

        // Should handle stress test within reasonable time
        assertTrue(stressTime < 20000, "Stress test took too long: ${stressTime}ms")
    }

    private fun createTestFiles() {
        testFiles =
                listOf(
                        createFileWithContent("ApiKeys.kt", generateApiKeyContent()),
                        createFileWithContent(
                                "DatabaseConfig.properties",
                                generateDatabaseContent()
                        ),
                        createFileWithContent("CryptoKeys.json", generateCryptoContent()),
                        createFileWithContent("CleanCode.kt", generateCleanContent()),
                        createFileWithContent("MixedContent.java", generateMixedContent())
                )
    }

    private fun createLargeTestFiles() {
        largeTestFiles =
                listOf(
                        createLargeFileWithContent("LargeFile1.kt", 1024 * 100), // 100KB
                        createLargeFileWithContent("LargeFile2.java", 1024 * 200), // 200KB
                        createLargeFileWithContent("LargeFile3.json", 1024 * 150) // 150KB
                )
    }

    private fun createFileWithContent(fileName: String, content: String): File {
        val file = tempDir.resolve(fileName).toFile()
        file.writeText(content)
        return file
    }

    private fun createDummyFile(fileName: String): File {
        return createFileWithContent(fileName, "// Dummy file content\nclass DummyClass { }")
    }

    private fun createLargeFileWithContent(fileName: String, sizeInBytes: Int): File {
        val baseContent = generateTestContentWithSecrets()
        val repetitions = sizeInBytes / baseContent.length + 1
        val largeContent = (1..repetitions).joinToString("\n") { "// Section $it\n$baseContent" }
        return createFileWithContent(fileName, largeContent)
    }

    private fun generateApiKeyContent(): String =
            """
        class ApiConfiguration {
            companion object {
                const val API_KEY = "sk-1234567890abcdef1234567890abcdef"
                const val AWS_ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE"
                const val GITHUB_TOKEN = "ghp_1234567890abcdef1234567890abcdef12345678"
                const val STRIPE_KEY = "sk_test_1234567890abcdef1234567890abcdef"
            }
        }
    """.trimIndent()

    private fun generateDatabaseContent(): String =
            """
        database.url=jdbc:postgresql://localhost:5432/mydb
        database.username=admin
        database.password=super_secret_password_123
        redis.url=redis://user:password123@localhost:6379
        mongodb.connection=mongodb://admin:secretpass@localhost:27017/mydb
    """.trimIndent()

    private fun generateCryptoContent(): String =
            """
        {
            "private_key": "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7VJTUt9Us8cKB\n-----END PRIVATE KEY-----",
            "jwt_secret": "your-256-bit-secret-key-here-make-it-long-enough",
            "encryption_key": "aes256-encryption-key-32-characters-long-12345",
            "api_tokens": {
                "service_a": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ"
            }
        }
    """.trimIndent()

    private fun generateCleanContent(): String =
            """
        class CleanService {
            private val logger = LoggerFactory.getLogger(CleanService::class.java)
            
            fun processData(input: String): String {
                logger.info("Processing data of length: ${input.length}")
                return input.uppercase()
            }
            
            companion object {
                const val MAX_RETRIES = 3
                const val TIMEOUT_MS = 5000
            }
        }
    """.trimIndent()

    private fun generateMixedContent(): String =
            """
        public class MixedService {
            // This is a clean section
            private static final String SERVICE_NAME = "mixed-service";
            private static final int PORT = 8080;
            
            // This section contains secrets
            private static final String SECRET_KEY = "sk-abcdef1234567890abcdef1234567890ab";
            private static final String DB_PASSWORD = "admin123password";
            
            public void initialize() {
                System.out.println("Initializing " + SERVICE_NAME);
                // More clean code here
                connectToDatabase();
            }
        }
    """.trimIndent()

    private fun generateTestContentWithSecrets(): String =
            """
        class TestClass {
            val apiKey = "sk-1234567890abcdef1234567890abcdef"
            val password = "secret123password"
            val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
            
            fun normalMethod() {
                // Regular code
                println("Hello World")
            }
        }
    """.trimIndent()

    private fun createScanEngine(configuration: ScanConfiguration): ScanEngine {
        val detectors =
                listOf(
                        PatternDetector(SecretPatterns.getAllPatterns()),
                        EntropyDetector(threshold = 4.5),
                        ContextAwareDetector()
                )

        val filters =
                listOf(
                        FileExtensionFilter(
                                configuration.includePatterns,
                                configuration.excludePatterns
                        ),
                        PathFilter(configuration.excludePatterns),
                        TestFileFilter()
                )

        return ScanEngine(configuration, detectors, filters)
    }
}
