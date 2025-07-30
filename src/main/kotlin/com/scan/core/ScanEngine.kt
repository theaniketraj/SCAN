package com.scan.core

import com.scan.detectors.*
import com.scan.filters.*
import com.scan.reporting.*
import com.scan.utils.*
import java.io.File
import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.*

/**
 * Core scanning engine that orchestrates the entire security scanning process
 *
 * This engine coordinates file discovery, filtering, detection, and reporting while managing
 * performance optimizations like parallel processing and caching.
 */
class ScanEngine(private val configuration: ScanConfiguration) {

    private val logger = createLogger("ScanEngine")

    // Detector instances
    private val patternDetector: DetectorInterface by lazy {
        PatternDetector()
    }
    private val entropyDetector: DetectorInterface by lazy {
        EntropyDetector()
    }
    private val contextAwareDetector: DetectorInterface by lazy {
        // TODO: ContextAwareDetector is an interface, need to find the implementation class
        // ContextAwareDetector(configuration.contextConfig)
        PatternDetector() // temporary fallback
    }
    private val compositeDetector: DetectorInterface by lazy {
        CompositeDetector(getEnabledDetectors())
    }

    // Filter instances
    private val fileFilters: List<FilterInterface> by lazy { createFileFilters() }

    // Cache for performance optimization
    private val scanCache = ConcurrentHashMap<String, CachedScanResult>()
    private val fileHashCache = ConcurrentHashMap<String, String>()

    // Statistics tracking
    private val stats = ScanStatistics()

    /**
     * Execute the complete scanning process
     *
     * @param rootPath The root directory to scan
     * @return Complete scan results with all detected secrets and metadata
     */
    suspend fun executeScan(rootPath: String): ScanResult {
        logger.info("Starting security scan of: $rootPath")

        val totalTime = measureTimeMillis {
            try {
                // Initialize scan process
                initializeScan()

                // Discover files to scan
                val filesToScan = discoverFiles(rootPath)
                logger.info("Found ${filesToScan.size} files to scan")

                // Load cache if enabled
                if (configuration.performance.enableCaching) {
                    loadCache()
                }

                // Execute scanning with parallel processing
                val scanResults =
                        if (configuration.performance.enableParallelScanning) {
                            scanFilesParallel(filesToScan)
                        } else {
                            scanFilesSequential(filesToScan)
                        }

                // Process and aggregate results
                val aggregatedResults = aggregateResults(scanResults)

                // Apply baseline comparison if configured
                val finalResults =
                        if (configuration.baselineFile != null) {
                            compareWithBaseline(aggregatedResults)
                        } else {
                            aggregatedResults
                        }

                // Save cache if enabled
                if (configuration.enableCaching) {
                    saveCache()
                }

                // Generate final scan result
                createFinalResult(finalResults, rootPath)
            } catch (e: Exception) {
                logger.error("Scan execution failed", e)
                throw ScanException("Scan execution failed: ${e.message}", e)
            }
        }

        logger.info("Scan completed in ${totalTime}ms")
        return createFinalResult(emptyList(), rootPath)
    }

    /** Initialize the scanning process */
    private fun initializeScan() {
        stats.startTime = System.currentTimeMillis()

        // Validate configuration
        validateConfiguration()

        // Initialize detectors
        initializeDetectors()

        // Setup output directories
        setupOutputDirectories()

        logger.debug("Scan initialization completed")
    }

    /** Discover all files that should be scanned based on configuration */
    private suspend fun discoverFiles(rootPath: String): List<File> {
        val root = File(rootPath)
        if (!root.exists() || !root.isDirectory) {
            throw ScanException("Root path does not exist or is not a directory: $rootPath")
        }

        val allFiles = mutableListOf<File>()

        // Walk the file tree
        Files.walk(root.toPath()).filter { Files.isRegularFile(it) }.forEach { path ->
            val file = path.toFile()
            if (shouldScanFile(file)) {
                allFiles.add(file)
            }
        }

        stats.totalFilesDiscovered = allFiles.size
        logger.debug("Discovered ${allFiles.size} files for scanning")

        return allFiles
    }

    /** Check if a file should be scanned based on filters */
    private fun shouldScanFile(file: File): Boolean {
        return fileFilters.all { filter -> filter.shouldInclude(file) }
    }

    /** Scan files in parallel using coroutines */
    private suspend fun scanFilesParallel(files: List<File>): List<FileScanResult> {
        val results = ConcurrentHashMap<String, FileScanResult>()
        val processedCount = AtomicInteger(0)
        val totalFiles = files.size

        // Create coroutine scope with limited parallelism
        val dispatcher = Dispatchers.IO.limitedParallelism(configuration.scanThreadCount)

        withContext(dispatcher) {
            files
                    .map { file ->
                        async {
                            try {
                                val result = scanSingleFile(file)
                                results[file.absolutePath] = result

                                val processed = processedCount.incrementAndGet()
                                if (processed % 100 == 0 || processed == totalFiles) {
                                    logger.info("Scanned $processed/$totalFiles files")
                                }

                                result
                            } catch (e: Exception) {
                                logger.warn("Failed to scan file: ${file.absolutePath}", e)
                                FileScanResult.error(
                                        file.absolutePath,
                                        e.message ?: "Unknown error"
                                )
                            }
                        }
                    }
                    .awaitAll()
        }

        return results.values.toList()
    }

    /** Scan files sequentially (fallback for low-memory environments) */
    private suspend fun scanFilesSequential(files: List<File>): List<FileScanResult> {
        val results = mutableListOf<FileScanResult>()

        files.forEachIndexed { index, file ->
            try {
                val result = scanSingleFile(file)
                results.add(result)

                if ((index + 1) % 50 == 0 || index == files.size - 1) {
                    logger.info("Scanned ${index + 1}/${files.size} files")
                }
            } catch (e: Exception) {
                logger.warn("Failed to scan file: ${file.absolutePath}", e)
                results.add(FileScanResult.error(file.absolutePath, e.message ?: "Unknown error"))
            }
        }

        return results
    }

    /** Scan a single file for secrets */
    private suspend fun scanSingleFile(file: File): FileScanResult {
        val filePath = file.absolutePath
        val fileHash = calculateFileHash(file)

        // Check cache first
        if (configuration.enableCaching) {
            val cachedResult = scanCache[filePath]
            if (cachedResult != null && cachedResult.fileHash == fileHash) {
                stats.cacheHits++
                return cachedResult.result
            }
        }

        // Check file size limits
        if (file.length() > configuration.maxFileSizeBytes) {
            logger.debug("Skipping large file: $filePath (${file.length()} bytes)")
            stats.skippedFiles++
            return FileScanResult.skipped(filePath, "File too large")
        }

        // Read file content
        val content =
                try {
                    file.readText(Charsets.UTF_8)
                } catch (e: Exception) {
                    logger.debug("Failed to read file as UTF-8: $filePath", e)
                    stats.errorFiles++
                    return FileScanResult.error(filePath, "Failed to read file: ${e.message}")
                }

        // Skip empty files
        if (content.isBlank()) {
            stats.emptyFiles++
            return FileScanResult.empty(filePath)
        }

        // Create file scanner and scan
        val fileScanner = FileScanner(file, content, configuration)
        val detections = compositeDetector.detect(fileScanner)

        // Create scan result
        val result =
                FileScanResult(
                        filePath = filePath,
                        detections = detections,
                        scanTimeMs = 0L, // Will be set by caller
                        fileSize = file.length(),
                        isError = false,
                        errorMessage = null
                )

        // Cache result if enabled
        if (configuration.enableCaching) {
            scanCache[filePath] = CachedScanResult(fileHash, result)
        }

        // Update statistics
        updateStatistics(result)

        return result
    }

    /** Aggregate results from all scanned files */
    private fun aggregateResults(results: List<FileScanResult>): List<Detection> {
        val allDetections = mutableListOf<Detection>()

        results.forEach { result ->
            if (!result.isError) {
                allDetections.addAll(result.detections)
            }
        }

        // Sort by severity and file path
        return allDetections.sortedWith(
                compareByDescending<Detection> { it.severity }.thenBy { it.filePath }.thenBy {
                    it.lineNumber
                }
        )
    }

    /** Compare current results with baseline if configured */
    private fun compareWithBaseline(detections: List<Detection>): List<Detection> {
        val baselineFile = File(configuration.baselineFile!!)
        if (!baselineFile.exists()) {
            logger.warn("Baseline file not found: ${configuration.baselineFile}")
            return detections
        }

        return try {
            val baseline = loadBaseline(baselineFile)
            if (configuration.onlyReportNew) {
                filterNewDetections(detections, baseline)
            } else {
                detections
            }
        } catch (e: Exception) {
            logger.error("Failed to load baseline file", e)
            detections
        }
    }

    /** Create the final scan result with all metadata */
    private fun createFinalResult(detections: List<Detection>, rootPath: String): ScanResult {
        stats.endTime = System.currentTimeMillis()
        stats.totalDetections = detections.size
        stats.criticalIssues = detections.count { it.severity == DetectionSeverity.CRITICAL }
        stats.highIssues = detections.count { it.severity == DetectionSeverity.HIGH }
        stats.mediumIssues = detections.count { it.severity == DetectionSeverity.MEDIUM }
        stats.lowIssues = detections.count { it.severity == DetectionSeverity.LOW }

        return ScanResult(
                rootPath = rootPath,
                detections = detections,
                statistics = stats,
                configuration = configuration,
                scanTimestamp = System.currentTimeMillis(),
                success = detections.isEmpty() || !configuration.failOnSecretsFound
        )
    }

    // =====================================
    // HELPER METHODS
    // =====================================

    private fun validateConfiguration() {
        if (configuration.scanThreadCount <= 0) {
            throw ScanException("Scan thread count must be positive")
        }
        if (configuration.entropyThreshold < 0.0 || configuration.entropyThreshold > 8.0) {
            throw ScanException("Entropy threshold must be between 0.0 and 8.0")
        }
        if (configuration.maxFileSizeBytes <= 0) {
            throw ScanException("Max file size must be positive")
        }
    }

    private fun getEnabledDetectors(): List<com.scan.detectors.CompositeDetector.DetectorConfig> {
        val detectorConfigs = mutableListOf<com.scan.detectors.CompositeDetector.DetectorConfig>()

        if (configuration.patterns.enablePatternDetector) {
            detectorConfigs.add(com.scan.detectors.CompositeDetector.DetectorConfig(patternDetector))
        }
        if (configuration.entropy.enableEntropyDetector) {
            detectorConfigs.add(com.scan.detectors.CompositeDetector.DetectorConfig(entropyDetector))
        }
        if (configuration.context.enableContextAwareDetector) {
            detectorConfigs.add(com.scan.detectors.CompositeDetector.DetectorConfig(contextAwareDetector))
        }

        return detectorConfigs
    }

    private fun createFileFilters(): List<FilterInterface> {
        val filters = mutableListOf<FilterInterface>()

        // File extension filter
        filters.add(
                FileExtensionFilter(
                        includeExtensions = configuration.includePatterns.toSet(),
                        excludeExtensions = configuration.excludePatterns.toSet()
                )
        )

        // Path filter
        filters.add(
                PathFilter(
                        includePaths = configuration.includePatterns.toSet(),
                        excludePaths = configuration.excludePatterns.toSet()
                )
        )

        // Test file filter
        if (!configuration.scanTestFiles) {
            filters.add(TestFileFilter(configuration.testFilePatterns.toSet()))
        }

        // Whitelist filter
        if (configuration.ignoredFiles.isNotEmpty()) {
            filters.add(WhitelistFilter(configuration.ignoredFiles.toSet()))
        }

        return filters
    }

    private fun initializeDetectors() {
        // Initialize each detector with its configuration
        if (configuration.enablePatternDetector) {
            patternDetector.initialize()
        }
        if (configuration.enableEntropyDetector) {
            entropyDetector.initialize()
        }
        if (configuration.enableContextAwareDetector) {
            contextAwareDetector.initialize()
        }
    }

    private fun setupOutputDirectories() {
        val outputDir = File(configuration.reportOutputDir)
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        if (configuration.enableCaching) {
            val cacheDir = File(configuration.cacheDirectory)
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
        }
    }

    private fun calculateFileHash(file: File): String {
        return fileHashCache.computeIfAbsent(file.absolutePath) { FileUtils.calculateSHA256(file) }
    }

    private fun loadCache() {
        try {
            val cacheFile = File(configuration.cacheDirectory, "scan-cache.json")
            if (cacheFile.exists()) {
                // Load cached results (implementation depends on serialization choice)
                logger.debug("Loaded scan cache with ${scanCache.size} entries")
            }
        } catch (e: Exception) {
            logger.warn("Failed to load scan cache", e)
        }
    }

    private fun saveCache() {
        try {
            val cacheFile = File(configuration.cacheDirectory, "scan-cache.json")
            // Save cache to file (implementation depends on serialization choice)
            logger.debug("Saved scan cache with ${scanCache.size} entries")
        } catch (e: Exception) {
            logger.warn("Failed to save scan cache", e)
        }
    }

    private fun loadBaseline(baselineFile: File): List<Detection> {
        // Load baseline detections from file
        return emptyList() // TODO: Implement baseline loading
    }

    private fun filterNewDetections(
            current: List<Detection>,
            baseline: List<Detection>
    ): List<Detection> {
        // Compare current detections with baseline to find new ones
        return current.filter { detection ->
            !baseline.any { baselineDetection -> baselineDetection.isSameAs(detection) }
        }
    }

    private fun updateStatistics(result: FileScanResult) {
        stats.scannedFiles++
        if (result.detections.isNotEmpty()) {
            stats.filesWithSecrets++
        }
    }

    private fun createLogger(name: String): Logger {
        return LoggerFactory.getLogger(name)
    }
}

// =====================================
// DATA CLASSES
// =====================================

/** Cached scan result for performance optimization */
private data class CachedScanResult(val fileHash: String, val result: FileScanResult)

/** Statistics tracking for scan execution */
data class ScanStatistics(
        var startTime: Long = 0L,
        var endTime: Long = 0L,
        var totalFilesDiscovered: Int = 0,
        var scannedFiles: Int = 0,
        var skippedFiles: Int = 0,
        var errorFiles: Int = 0,
        var emptyFiles: Int = 0,
        var filesWithSecrets: Int = 0,
        var totalDetections: Int = 0,
        var criticalIssues: Int = 0,
        var highIssues: Int = 0,
        var mediumIssues: Int = 0,
        var lowIssues: Int = 0,
        var cacheHits: Int = 0
) {
    val scanDurationMs: Long
        get() = endTime - startTime
    val scanDurationSeconds: Double
        get() = scanDurationMs / 1000.0
}

/** Exception thrown during scan execution */
class ScanException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Logger interface for abstraction */
interface Logger {
    fun debug(message: String)
    fun info(message: String)
    fun warn(message: String, throwable: Throwable? = null)
    fun error(message: String, throwable: Throwable? = null)
}

/** Logger factory for creating logger instances */
object LoggerFactory {
    fun getLogger(name: String): Logger = ConsoleLogger(name)
}

/** Simple console logger implementation */
private class ConsoleLogger(private val name: String) : Logger {
    override fun debug(message: String) = println("[DEBUG] $name: $message")
    override fun info(message: String) = println("[INFO] $name: $message")
    override fun warn(message: String, throwable: Throwable?) =
            println("[WARN] $name: $message ${throwable?.message ?: ""}")
    override fun error(message: String, throwable: Throwable?) =
            println("[ERROR] $name: $message ${throwable?.message ?: ""}")
}
