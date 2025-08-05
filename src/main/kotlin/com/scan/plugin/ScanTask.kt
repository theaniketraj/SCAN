package com.scan.plugin

import com.scan.core.*
import com.scan.core.ScanEngine
import com.scan.core.ScanResult
import com.scan.reporting.ConsoleReporter
import com.scan.reporting.HtmlReporter
import com.scan.reporting.JsonReporter
import java.io.File
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault

/**
 * The main task that performs security scanning for sensitive information.
 *
 * This task represents the core execution unit of the SCAN plugin. When developers run `./gradlew
 * build` or `./gradlew scanForSecrets`, this is the class that actually does the work of analyzing
 * files for potential security vulnerabilities.
 *
 * The task is designed to integrate seamlessly with Gradle's task execution engine, including
 * support for incremental builds, build caching, and parallel execution. It follows Gradle's best
 * practices for task implementation to ensure optimal performance and reliability.
 */
@DisableCachingByDefault(because = "Security scanning results should be fresh for each execution")
abstract class ScanTask @Inject constructor() : DefaultTask() {

    /**
     * The configuration object that contains all user preferences and settings. This property
     * connects the task to the extension configuration that users define in their build.gradle.kts
     * files.
     *
     * We use Gradle's Property API here, which provides lazy evaluation and proper integration with
     * Gradle's configuration cache and task isolation features.
     */
    @get:Nested abstract val scanConfiguration: Property<ScanExtension>

    /**
     * Lazily computed file tree that represents all files to be scanned. This property is marked as
     * an input so Gradle knows to re-run the task when the set of files changes, even if individual
     * file contents haven't changed.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val sourceFiles: FileTree
        get() = project.fileTree(project.projectDir)

    /**
     * The main output file that always gets generated, even if no secrets are found. This ensures
     * Gradle's up-to-date checking works correctly. Without an output, Gradle wouldn't know if the
     * task had run successfully or not.
     */
    @get:OutputFile
    val resultFile: File
        get() = scanConfiguration.get().reportOutputDir.get().asFile.resolve("scan-results.txt")

    /**
     * Optional HTML report output. This is conditionally created based on user configuration. Using
     * @Optional tells Gradle that this output might not exist, which is perfectly fine.
     */
    @get:OutputFile
    @get:Optional
    val htmlReportFile: File?
        get() =
                if (scanConfiguration.get().generateHtmlReport.get()) {
                    scanConfiguration.get().reportOutputDir.get().asFile.resolve("scan-report.html")
                } else null

    /** Optional JSON report output for CI/CD integration and programmatic processing. */
    @get:OutputFile
    @get:Optional
    val jsonReportFile: File?
        get() =
                if (scanConfiguration.get().generateJsonReport.get()) {
                    scanConfiguration.get().reportOutputDir.get().asFile.resolve("scan-report.json")
                } else null

    /**
     * The main execution method that Gradle calls when this task needs to run. This is annotated
     * with @TaskAction to tell Gradle that this method contains the actual work the task performs.
     *
     * The method is designed to be idempotent - running it multiple times with the same inputs
     * should produce the same outputs. This is crucial for Gradle's build caching and incremental
     * execution to work correctly.
     */
    @TaskAction
    fun scanForSecrets() {
        val startTime = Instant.now()

        logger.lifecycle("🔍 Starting SCAN security analysis...")

        try {
            // Validate task configuration before starting expensive operations
            validateTaskConfiguration()

            // Prepare the output directory structure
            prepareOutputDirectories()

            // Create and configure the scanning engine
            val scanEngine = createScanEngine()

            // Collect all files to be scanned with progress reporting
            val filesToScan = collectFilesToScan()

            if (filesToScan.isEmpty()) {
                handleNoFilesToScan()
                return
            }

            logger.lifecycle("📁 Scanning ${filesToScan.size} files for sensitive information...")

            // Perform the actual security scanning
            val scanResults = performScanning(scanEngine, filesToScan)

            // Generate reports based on user configuration
            generateReports(scanResults)

            // Handle the results based on configuration and build environment
            handleScanResults(scanResults, startTime)
        } catch (exception: Exception) {
            // Provide detailed error information to help users diagnose problems
            handleScanException(exception)
        }
    }

    /**
     * Validates that the task configuration is valid and consistent. This catches configuration
     * errors early with helpful messages rather than failing mysteriously during scanning.
     */
    private fun validateTaskConfiguration() {
        val config = scanConfiguration.get()

        // Validate that we have patterns to work with
        val includePatterns = config.includePatterns.get()
        if (includePatterns.isEmpty()) {
            throw GradleException(
                    "SCAN configuration error: No include patterns specified. " +
                            "Add patterns like 'src/**/*.kt' to scan your source files."
            )
        }

        // Validate file size limits are reasonable
        val maxFileSize = config.maxFileSizeBytes.get()
        if (maxFileSize <= 0) {
            throw GradleException(
                    "SCAN configuration error: maxFileSizeBytes must be positive, got $maxFileSize"
            )
        }

        // Warn about potentially problematic configurations
        if (maxFileSize < 1024) {
            logger.warn(
                    "SCAN warning: Very small maxFileSizeBytes (${maxFileSize}). This might skip legitimate source files."
            )
        }

        logger.debug("Task configuration validation completed successfully")
    }

    /**
     * Ensures all necessary output directories exist and are writable. This prevents cryptic IO
     * errors later in the process.
     */
    private fun prepareOutputDirectories() {
        val outputDir = scanConfiguration.get().reportOutputDir.get().asFile

        if (!outputDir.exists()) {
            val created = outputDir.mkdirs()
            if (!created) {
                throw GradleException(
                        "SCAN error: Cannot create output directory: ${outputDir.absolutePath}. " +
                                "Please check permissions and disk space."
                )
            }
            logger.debug("Created output directory: ${outputDir.absolutePath}")
        }

        if (!outputDir.canWrite()) {
            throw GradleException(
                    "SCAN error: Cannot write to output directory: ${outputDir.absolutePath}. " +
                            "Please check directory permissions."
            )
        }
    }

    /**
     * Creates and configures the scanning engine based on user preferences. This is where we
     * transform the user's configuration into the internal configuration objects that the scanning
     * engine understands.
     */
    private fun createScanEngine(): ScanEngine {
        val config = scanConfiguration.get()

        // Transform the Gradle extension configuration into the internal configuration format
        val internalConfig =
                ScanConfiguration(
                        includePatterns = config.includePatterns.getOrElse(emptySet()).toList(),
                        excludePatterns = config.excludePatterns.getOrElse(emptySet()).toList(),
                        maxFileSize = config.maxFileSizeBytes.getOrElse(10 * 1024 * 1024),
                        performance = PerformanceConfiguration(
                                maxConcurrency = if (config.parallelScanning.getOrElse(true)) 
                                    Runtime.getRuntime().availableProcessors() else 1
                        ),
                        entropy = EntropyConfiguration(
                                threshold = if (config.strictMode.getOrElse(false)) 3.5 else 4.0
                        ),
                        reporting = ReportingConfiguration(
                                console = ConsoleReportConfiguration(),
                                json = if (config.generateJsonReport.getOrElse(false))
                                    JsonReportConfiguration()
                                else JsonReportConfiguration(),
                                html = if (config.generateHtmlReport.getOrElse(false))
                                    HtmlReportConfiguration()
                                else HtmlReportConfiguration()
                        )
                )

        logger.debug(
                "Created scan engine with configuration: strictMode=${internalConfig.strictMode}"
        )

        return ScanEngine(internalConfig)
    }

    /**
     * Collects all files that should be scanned, applying filters and size limits. This method
     * provides progress feedback for large projects where file collection might take some time.
     */
    private fun collectFilesToScan(): List<File> {
        logger.info("Collecting files to scan...")

        val config = scanConfiguration.get()
        val maxFileSize = config.maxFileSizeBytes.get()

        val allFiles = sourceFiles.files.toList()
        logger.debug("Found ${allFiles.size} files matching include/exclude patterns")

        // Apply additional filtering based on configuration
        val filteredFiles =
                allFiles.filter { file ->
                    when {
                        !file.isFile -> {
                            logger.debug("Skipping non-file: ${file.name}")
                            false
                        }
                        file.length() == 0L -> {
                            logger.debug("Skipping empty file: ${file.name}")
                            false
                        }
                        file.length() > maxFileSize -> {
                            logger.info(
                                    "Skipping large file (${file.length()} bytes): ${file.name}"
                            )
                            false
                        }
                        !file.canRead() -> {
                            logger.warn("Skipping unreadable file: ${file.name}")
                            false
                        }
                        else -> true
                    }
                }

        // Apply test file filtering if configured
        val finalFiles =
                if (config.ignoreTestFiles.get()) {
                    filteredFiles.filter { file ->
                        val isTestFile = isTestFile(file)
                        if (isTestFile) {
                            logger.debug("Skipping test file: ${file.name}")
                        }
                        !isTestFile
                    }
                } else {
                    filteredFiles
                }

        logger.info("Selected ${finalFiles.size} files for security scanning")
        return finalFiles
    }

    /**
     * Determines if a file is likely a test file based on common patterns. This heuristic helps
     * reduce false positives from test data and mock objects.
     */
    private fun isTestFile(file: File): Boolean {
        val relativePath = project.rootDir.toPath().relativize(file.toPath()).toString()

        return relativePath.contains("/test/") ||
                relativePath.contains("\\test\\") ||
                relativePath.contains("/tests/") ||
                relativePath.contains("\\tests\\") ||
                file.name.endsWith("Test.kt") ||
                file.name.endsWith("Test.java") ||
                file.name.endsWith("Tests.kt") ||
                file.name.endsWith("Tests.java") ||
                file.name.startsWith("Test") ||
                file.name.contains("Mock") ||
                file.name.contains("Fake")
    }

    /**
     * Handles the case where no files are found to scan. This provides clear feedback to users
     * about why scanning didn't happen.
     */
    private fun handleNoFilesToScan() {
        val message = "No files found to scan. Check your include/exclude patterns."
        logger.lifecycle("ℹ️  $message")

        // Still create the output file to satisfy Gradle's up-to-date checking
        resultFile.writeText("No files scanned - no files matched the configured patterns.\n")

        logger.lifecycle("✅ SCAN completed: No files to scan")
    }

    /**
     * Performs the actual scanning using the configured engine. This method handles progress
     * reporting and error recovery for individual file failures.
     */
    private fun performScanning(scanEngine: ScanEngine, filesToScan: List<File>): ScanResult {
        val config = scanConfiguration.get()

        // Provide progress updates for large scans
        val progressReportingThreshold = 100
        val shouldReportProgress = filesToScan.size >= progressReportingThreshold

        if (shouldReportProgress) {
            logger.lifecycle(
                    "Scanning progress will be reported every ${progressReportingThreshold} files..."
            )
        }

        try {
            // Delegate the actual scanning to the engine
            return scanEngine.scanFiles(filesToScan) { scannedCount, totalCount ->
                // Progress callback for large operations
                if (shouldReportProgress && scannedCount % progressReportingThreshold == 0) {
                    val percentage = (scannedCount * 100) / totalCount
                    logger.lifecycle(
                            "📊 Scanning progress: $scannedCount/$totalCount files ($percentage%)"
                    )
                }
            }
        } catch (exception: Exception) {
            throw GradleException("Scanning failed: ${exception.message}", exception)
        }
    }

    /**
     * Generates all requested report formats based on user configuration. Each reporter is
     * independent, so failure in one format doesn't affect others.
     */
    private fun generateReports(scanResults: ScanResult) {
        val config = scanConfiguration.get()
        val outputDir = config.reportOutputDir.get().asFile

        logger.debug("Generating reports in directory: ${outputDir.absolutePath}")

        try {
            // Always generate the basic text result file
            generateTextReport(scanResults)

            // Generate optional format reports
            if (config.generateHtmlReport.get()) {
                generateHtmlReport(scanResults, outputDir)
            }

            if (config.generateJsonReport.get()) {
                generateJsonReport(scanResults, outputDir)
            }
        } catch (exception: Exception) {
            // Report generation failures shouldn't stop the build, but users should know
            logger.error("Failed to generate some reports: ${exception.message}", exception)
        }
    }

    /**
     * Generates the basic text report that's always created. This satisfies Gradle's requirement
     * for task outputs.
     */
    private fun generateTextReport(scanResults: ScanResult) {
        val report = buildString {
            appendLine("SCAN Security Analysis Results")
            appendLine("Generated: ${Instant.now()}")
            appendLine("Project: ${project.name}")
            appendLine()

            if (scanResults.findings.isEmpty()) {
                appendLine("✅ No sensitive information found")
            } else {
                appendLine("⚠️  Found ${scanResults.findings.size} potential security issues:")
                scanResults.findings.forEach { finding ->
                    appendLine("- ${finding.file.name}: ${finding.description}")
                }
            }

            appendLine()
            appendLine("Scan Statistics:")
            appendLine("- Files scanned: ${scanResults.filesScanned}")
            appendLine("- Scan duration: ${scanResults.scanDuration}")
            appendLine("- Issues found: ${scanResults.findings.size}")
        }

        resultFile.writeText(report)
        logger.debug("Generated text report: ${resultFile.absolutePath}")
    }

    /** Generates an HTML report for easy viewing and sharing. */
    private fun generateHtmlReport(scanResults: ScanResult, outputDir: File) {
        try {
            val htmlReporter = HtmlReporter()
            val htmlFile = outputDir.resolve("scan-report.html")
            htmlReporter.generateReport(scanResults, htmlFile)
            logger.info("Generated HTML report: ${htmlFile.absolutePath}")
        } catch (exception: Exception) {
            logger.warn("Failed to generate HTML report: ${exception.message}")
        }
    }

    /** Generates a JSON report for programmatic processing and CI/CD integration. */
    private fun generateJsonReport(scanResults: ScanResult, outputDir: File) {
        try {
            val jsonReporter = JsonReporter()
            val jsonFile = outputDir.resolve("scan-report.json")
            jsonReporter.generateReport(scanResults, jsonFile)
            logger.info("Generated JSON report: ${jsonFile.absolutePath}")
        } catch (exception: Exception) {
            logger.warn("Failed to generate JSON report: ${exception.message}")
        }
    }

    /**
     * Handles scan results based on configuration and determines build success/failure. This is
     * where we decide whether finding secrets should fail the build or just warn.
     */
    private fun handleScanResults(scanResults: ScanResult, startTime: Instant) {
        val config = scanConfiguration.get()
        val duration = Duration.between(startTime, Instant.now())

        // Always show console output using the console reporter
        val consoleReporter = ConsoleReporter()
        consoleReporter.reportResults(scanResults)

        // Determine the appropriate response based on findings and configuration
        when {
            scanResults.findings.isEmpty() -> {
                logger.lifecycle("✅ SCAN completed successfully: No sensitive information found")
                logger.lifecycle(
                        "📊 Scanned ${scanResults.filesScanned} files in ${duration.toMillis()}ms"
                )
            }
            config.failOnSecrets.get() -> {
                val message =
                        "❌ SCAN failed: Found ${scanResults.findings.size} potential security issues. " +
                                "Review the findings and remove sensitive information before continuing."
                logger.error(message)
                throw GradleException(message)
            }
            config.warnOnSecrets.get() -> {
                logger.lifecycle(
                        "⚠️  SCAN completed with warnings: Found ${scanResults.findings.size} potential security issues"
                )
                logger.lifecycle(
                        "📊 Scanned ${scanResults.filesScanned} files in ${duration.toMillis()}ms"
                )
                logger.lifecycle(
                        "💡 Consider reviewing and addressing these findings to improve security"
                )
            }
            else -> {
                // Silent mode - just complete without fanfare
                logger.info(
                        "SCAN completed: ${scanResults.findings.size} findings, ${scanResults.filesScanned} files scanned"
                )
            }
        }
    }

    /**
     * Handles exceptions that occur during scanning with helpful error messages. This transforms
     * technical exceptions into user-friendly guidance.
     */
    private fun handleScanException(exception: Exception) {
        when (exception) {
            is GradleException -> {
                // GradleExceptions are already user-friendly, just re-throw
                throw exception
            }
            is SecurityException -> {
                throw GradleException(
                        "SCAN failed due to security restrictions: ${exception.message}. " +
                                "Check file permissions and security policies.",
                        exception
                )
            }
            is OutOfMemoryError -> {
                throw GradleException(
                        "SCAN failed due to insufficient memory. " +
                                "Try reducing maxFileSizeBytes or enabling parallel scanning with fewer threads.",
                        exception
                )
            }
            else -> {
                logger.error("Unexpected error during security scanning", exception)
                throw GradleException(
                        "SCAN failed with unexpected error: ${exception.message}. " +
                                "Please report this issue with your project details.",
                        exception
                )
            }
        }
    }
}
