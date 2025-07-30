package com.scan.core

import com.scan.detectors.DetectorInterface
import com.scan.filters.FilterInterface
import com.scan.utils.EntropyCalculator
import com.scan.utils.FileUtils
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Core file scanner responsible for processing individual files and detecting secrets. Handles file
 * reading, content analysis, and coordinates with detectors and filters.
 */
class FileScanner(
        private val configuration: ScanConfiguration,
        private val detectors: List<DetectorInterface>,
        private val filters: List<FilterInterface>
) {

    companion object {
        private const val MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024 // 50MB
        private const val MAX_LINE_LENGTH = 10000
        private const val BINARY_THRESHOLD = 0.3 // 30% non-printable characters

        // Common file extensions that should be scanned
        private val SCANNABLE_EXTENSIONS =
                setOf(
                        "kt",
                        "java",
                        "js",
                        "ts",
                        "py",
                        "rb",
                        "go",
                        "rs",
                        "cpp",
                        "c",
                        "h",
                        "cs",
                        "php",
                        "scala",
                        "clj",
                        "swift",
                        "dart",
                        "sh",
                        "bash",
                        "zsh",
                        "yml",
                        "yaml",
                        "json",
                        "xml",
                        "properties",
                        "conf",
                        "config",
                        "ini",
                        "env",
                        "dockerfile",
                        "sql",
                        "md",
                        "txt",
                        "gradle",
                        "kts"
                )

        // Binary file extensions to skip
        private val BINARY_EXTENSIONS =
                setOf(
                        "jar",
                        "zip",
                        "tar",
                        "gz",
                        "7z",
                        "rar",
                        "exe",
                        "dll",
                        "so",
                        "dylib",
                        "png",
                        "jpg",
                        "jpeg",
                        "gif",
                        "bmp",
                        "ico",
                        "svg",
                        "pdf",
                        "doc",
                        "docx",
                        "xls",
                        "xlsx",
                        "ppt",
                        "pptx",
                        "mp3",
                        "mp4",
                        "avi",
                        "mov",
                        "wav"
                )
    }

    // Cache for file hashes to avoid rescanning unchanged files
    private val fileHashCache = ConcurrentHashMap<String, String>()

    /**
     * Scans a single file for secrets and sensitive information.
     *
     * @param filePath The path to the file to scan
     * @return ScanResult containing any findings or null if file should be skipped
     */
    fun scanFile(filePath: Path): ScanResult? {
        val file = filePath.toFile()

        // Pre-scanning validation
        if (!shouldScanFile(file)) {
            return null
        }

        return try {
            val scanContext = createScanContext(filePath)
            val findings = mutableListOf<Finding>()

            // Apply filters before scanning
            if (shouldSkipFile(filePath, scanContext)) {
                return null
            }

            // Read file content
            val content = readFileContent(file) ?: return null
            scanContext.content = content
            scanContext.lines = content.lines()

            // Check if content has changed (for incremental scans)
            if (configuration.incrementalScan && !hasFileChanged(filePath, content)) {
                return null // Skip unchanged files
            }

            // Process each detector
            detectors.forEach { detector ->
                try {
                    val detectorFindings = detector.detect(scanContext)
                    findings.addAll(detectorFindings)
                } catch (e: Exception) {
                    // Log detector error but continue with other detectors
                    System.err.println(
                            "Detector ${detector.javaClass.simpleName} failed for ${filePath}: ${e.message}"
                    )
                }
            }

            // Post-process findings
            val processedFindings = postProcessFindings(findings, scanContext)

            ScanResult(
                    filePath = filePath.toString(),
                    findings = processedFindings,
                    scanTime = System.currentTimeMillis(),
                    fileSize = file.length(),
                    linesScanned = scanContext.lines.size
            )
        } catch (e: Exception) {
            System.err.println("Failed to scan file ${filePath}: ${e.message}")
            ScanResult(
                    filePath = filePath.toString(),
                    findings = emptyList(),
                    scanTime = System.currentTimeMillis(),
                    fileSize = file.length(),
                    linesScanned = 0,
                    error = e.message
            )
        }
    }

    /** Creates scan context with file metadata and initial setup. */
    private fun createScanContext(filePath: Path): ScanContext {
        val file = filePath.toFile()
        val extension = FileUtils.getFileExtension(file)
        val isTestFile = isTestFile(filePath)

        return ScanContext(
                filePath = filePath,
                fileName = file.name,
                fileExtension = extension,
                isTestFile = isTestFile,
                fileSize = file.length(),
                configuration = configuration
        )
    }

    /** Determines if a file should be scanned based on basic criteria. */
    private fun shouldScanFile(file: File): Boolean {
        // Check if file exists and is readable
        if (!file.exists() || !file.canRead()) {
            return false
        }

        // Check file size limits
        if (file.length() > MAX_FILE_SIZE_BYTES) {
            if (configuration.verbose) {
                println("Skipping large file: ${file.path} (${file.length()} bytes)")
            }
            return false
        }

        // Check if it's a directory (shouldn't happen, but safety check)
        if (file.isDirectory) {
            return false
        }

        // Check file extension
        val extension = FileUtils.getFileExtension(file).lowercase()

        // Skip known binary files
        if (extension in BINARY_EXTENSIONS) {
            return false
        }

        // Only scan known text file extensions if strict mode is enabled
        if (configuration.strictMode && extension !in SCANNABLE_EXTENSIONS) {
            return false
        }

        return true
    }

    /** Applies filters to determine if file should be skipped. */
    private fun shouldSkipFile(filePath: Path, scanContext: ScanContext): Boolean {
        return filters.any { filter ->
            try {
                !filter.shouldInclude(scanContext)
            } catch (e: Exception) {
                System.err.println(
                        "Filter ${filter.javaClass.simpleName} failed for ${filePath}: ${e.message}"
                )
                false // Don't skip on filter error
            }
        }
    }

    /** Reads file content with proper encoding detection and validation. */
    private fun readFileContent(file: File): String? {
        return try {
            // First, check if file is binary by sampling
            val sampleSize = minOf(1024, file.length().toInt())
            val sample = Files.readAllBytes(file.toPath()).take(sampleSize).toByteArray()

            if (isBinaryContent(sample)) {
                if (configuration.verbose) {
                    println("Skipping binary file: ${file.path}")
                }
                return null
            }

            // Read full content
            val content = Files.readString(file.toPath(), StandardCharsets.UTF_8)

            // Validate content length and line lengths
            if (content.length > configuration.maxContentLength) {
                if (configuration.verbose) {
                    println("Skipping large content file: ${file.path} (${content.length} chars)")
                }
                return null
            }

            // Check for extremely long lines that might indicate binary or generated content
            val lines = content.lines()
            val hasLongLines = lines.any { it.length > MAX_LINE_LENGTH }
            if (hasLongLines && !configuration.scanLongLines) {
                if (configuration.verbose) {
                    println("Skipping file with long lines: ${file.path}")
                }
                return null
            }

            content
        } catch (e: IOException) {
            System.err.println("Failed to read file ${file.path}: ${e.message}")
            null
        } catch (e: OutOfMemoryError) {
            System.err.println("File too large to read into memory: ${file.path}")
            null
        }
    }

    /** Determines if content is binary based on character analysis. */
    private fun isBinaryContent(content: ByteArray): Boolean {
        if (content.isEmpty()) return false

        var nonPrintableCount = 0
        for (byte in content) {
            val char = byte.toInt() and 0xFF
            // Check for null bytes or non-printable characters (excluding common whitespace)
            if (char == 0 || (char < 32 && char != 9 && char != 10 && char != 13)) {
                nonPrintableCount++
            }
        }

        return nonPrintableCount.toDouble() / content.size > BINARY_THRESHOLD
    }

    /** Checks if file has changed since last scan (for incremental scanning). */
    private fun hasFileChanged(filePath: Path, content: String): Boolean {
        val fileKey = filePath.toString()
        val currentHash = calculateContentHash(content)
        val previousHash = fileHashCache[fileKey]

        fileHashCache[fileKey] = currentHash
        return previousHash != currentHash
    }

    /** Calculates a hash of the file content for change detection. */
    private fun calculateContentHash(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toByteArray(StandardCharsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /** Determines if a file is a test file based on path and naming conventions. */
    private fun isTestFile(filePath: Path): Boolean {
        val pathStr = filePath.toString().lowercase()
        val fileName = filePath.fileName.toString().lowercase()

        return pathStr.contains("/test/") ||
                pathStr.contains("\\test\\") ||
                pathStr.contains("/tests/") ||
                pathStr.contains("\\tests\\") ||
                fileName.contains("test") ||
                fileName.contains("spec") ||
                pathStr.contains("src/test") ||
                pathStr.contains("src\\test")
    }

    /**
     * Post-processes findings to remove duplicates, apply confidence scoring, and filter false
     * positives.
     */
    private fun postProcessFindings(findings: List<Finding>, context: ScanContext): List<Finding> {
        if (findings.isEmpty()) return findings

        val processed = mutableListOf<Finding>()
        val seenFindings = mutableSetOf<String>()

        findings.forEach { finding ->
            // Create a unique key for deduplication
            val uniqueKey =
                    "${finding.lineNumber}:${finding.columnStart}:${finding.type}:${finding.value}"

            if (uniqueKey !in seenFindings) {
                seenFindings.add(uniqueKey)

                // Apply confidence scoring
                val adjustedFinding = adjustConfidenceScore(finding, context)

                // Filter based on minimum confidence threshold
                if (adjustedFinding.confidence >= configuration.minConfidence) {
                    processed.add(adjustedFinding)
                }
            }
        }

        // Sort by confidence (highest first) and then by line number
        return processed.sortedWith(
                compareByDescending<Finding> { it.confidence }.thenBy { it.lineNumber }.thenBy {
                    it.columnStart
                }
        )
    }

    /** Adjusts confidence score based on context and heuristics. */
    private fun adjustConfidenceScore(finding: Finding, context: ScanContext): Finding {
        var adjustedConfidence = finding.confidence

        // Reduce confidence for test files
        if (context.isTestFile) {
            adjustedConfidence *= 0.7
        }

        // Reduce confidence for findings in comments
        if (isInComment(finding, context)) {
            adjustedConfidence *= 0.6
        }

        // Reduce confidence for common false positive patterns
        if (isFalsePositivePattern(finding)) {
            adjustedConfidence *= 0.5
        }

        // Increase confidence for high-entropy strings
        if (finding.value.isNotEmpty()) {
            val entropy = EntropyCalculator.calculateShannonEntropy(finding.value)
            if (entropy > 4.5) {
                adjustedConfidence *= 1.2
            }
        }

        // Ensure confidence stays within bounds
        adjustedConfidence = adjustedConfidence.coerceIn(0.0, 1.0)

        return finding.copy(confidence = adjustedConfidence)
    }

    /** Checks if a finding is within a comment block or line. */
    private fun isInComment(finding: Finding, context: ScanContext): Boolean {
        if (finding.lineNumber <= 0 || finding.lineNumber > context.lines.size) {
            return false
        }

        val line = context.lines[finding.lineNumber - 1]
        val beforeFinding = line.substring(0, minOf(finding.columnStart, line.length))

        // Check for line comments
        if (beforeFinding.contains("//") || beforeFinding.contains("#")) {
            return true
        }

        // Check for block comments (simple heuristic)
        val trimmedLine = line.trim()
        if (trimmedLine.startsWith("*") ||
                        trimmedLine.startsWith("/*") ||
                        trimmedLine.endsWith("*/")
        ) {
            return true
        }

        return false
    }

    /** Identifies common false positive patterns. */
    private fun isFalsePositivePattern(finding: Finding): Boolean {
        val value = finding.value.lowercase()

        // Common placeholder patterns
        val placeholders =
                listOf(
                        "example",
                        "test",
                        "demo",
                        "sample",
                        "placeholder",
                        "dummy",
                        "fake",
                        "mock",
                        "stub",
                        "your_",
                        "my_",
                        "insert_",
                        "put_your_",
                        "xxxxxxxx",
                        "12345",
                        "abcdef",
                        "foobar",
                        "lorem",
                        "ipsum"
                )

        return placeholders.any { value.contains(it) }
    }

    /** Clears the file hash cache (useful for fresh scans). */
    fun clearCache() {
        fileHashCache.clear()
    }

    /** Gets cache statistics for monitoring. */
    fun getCacheStats(): Map<String, Any> {
        return mapOf(
                "cacheSize" to fileHashCache.size,
                "memoryUsage" to (fileHashCache.size * 100) // Rough estimate
        )
    }
}

/** Context object passed to detectors containing file information and content. */
data class ScanContext(
        val filePath: Path,
        val fileName: String,
        val fileExtension: String,
        val isTestFile: Boolean,
        val fileSize: Long,
        val configuration: ScanConfiguration,
        var content: String = "",
        var lines: List<String> = emptyList()
)
        get() = findings.filter { it.confidence >= 0.8 }
}
