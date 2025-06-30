package com.scan.reporting

import com.scan.core.ScanConfiguration
import com.scan.core.ScanResult
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * Central report generator that coordinates different report formats and manages output
 * destinations. Supports multiple concurrent report formats and provides extensible architecture
 * for new formats.
 */
class ReportGenerator(private val configuration: ScanConfiguration) {
    private val logger: Logger = Logging.getLogger(ReportGenerator::class.java)

    private val consoleReporter = ConsoleReporter()
    private val jsonReporter = JsonReporter()
    private val htmlReporter = HtmlReporter()

    /** Generate reports in all configured formats */
    fun generateReports(scanResults: List<ScanResult>, projectPath: File): ReportSummary {
        val startTime = System.currentTimeMillis()
        val timestamp = LocalDateTime.now()

        logger.info("Generating scan reports for ${scanResults.size} findings...")

        val reportSummary =
                ReportSummary(
                        totalFindings = scanResults.size,
                        timestamp = timestamp,
                        projectPath = projectPath.absolutePath,
                        scanDuration = calculateScanDuration(scanResults)
                )

        val generatedReports = mutableListOf<GeneratedReport>()

        // Always generate console output
        if (configuration.reporting.consoleOutput) {
            generateConsoleReport(scanResults, reportSummary)
            generatedReports.add(GeneratedReport(ReportFormat.CONSOLE, "console", 0))
        }

        // Generate JSON report if configured
        if (configuration.reporting.jsonOutput.enabled) {
            val jsonReport = generateJsonReport(scanResults, reportSummary, projectPath)
            generatedReports.add(jsonReport)
        }

        // Generate HTML report if configured
        if (configuration.reporting.htmlOutput.enabled) {
            val htmlReport = generateHtmlReport(scanResults, reportSummary, projectPath)
            generatedReports.add(htmlReport)
        }

        // Generate custom reports if any are configured
        configuration.reporting.customReporters.forEach { customReporter ->
            try {
                val customReport =
                        generateCustomReport(
                                scanResults,
                                reportSummary,
                                customReporter,
                                projectPath
                        )
                generatedReports.add(customReport)
            } catch (e: Exception) {
                logger.error("Failed to generate custom report: ${customReporter.name}", e)
            }
        }

        val totalTime = System.currentTimeMillis() - startTime
        reportSummary.generationTime = totalTime
        reportSummary.generatedReports = generatedReports

        logger.info("Report generation completed in ${totalTime}ms")
        logReportSummary(reportSummary)

        return reportSummary
    }

    /** Generate console report */
    private fun generateConsoleReport(scanResults: List<ScanResult>, summary: ReportSummary) {
        logger.info("Generating console report...")
        consoleReporter.generateReport(scanResults, summary, configuration.reporting.consoleOutput)
    }

    /** Generate JSON report */
    private fun generateJsonReport(
            scanResults: List<ScanResult>,
            summary: ReportSummary,
            projectPath: File
    ): GeneratedReport {
        logger.info("Generating JSON report...")
        val startTime = System.currentTimeMillis()

        val outputFile =
                resolveOutputFile(
                        projectPath,
                        configuration.reporting.jsonOutput.outputPath,
                        "scan-report",
                        "json"
                )

        jsonReporter.generateReport(
                scanResults,
                summary,
                configuration.reporting.jsonOutput,
                outputFile
        )

        val generationTime = System.currentTimeMillis() - startTime
        logger.info("JSON report generated: ${outputFile.absolutePath}")

        return GeneratedReport(
                format = ReportFormat.JSON,
                path = outputFile.absolutePath,
                sizeBytes = outputFile.length(),
                generationTimeMs = generationTime
        )
    }

    /** Generate HTML report */
    private fun generateHtmlReport(
            scanResults: List<ScanResult>,
            summary: ReportSummary,
            projectPath: File
    ): GeneratedReport {
        logger.info("Generating HTML report...")
        val startTime = System.currentTimeMillis()

        val outputFile =
                resolveOutputFile(
                        projectPath,
                        configuration.reporting.htmlOutput.outputPath,
                        "scan-report",
                        "html"
                )

        htmlReporter.generateReport(
                scanResults,
                summary,
                configuration.reporting.htmlOutput,
                outputFile
        )

        val generationTime = System.currentTimeMillis() - startTime
        logger.info("HTML report generated: ${outputFile.absolutePath}")

        return GeneratedReport(
                format = ReportFormat.HTML,
                path = outputFile.absolutePath,
                sizeBytes = outputFile.length(),
                generationTimeMs = generationTime
        )
    }

    /** Generate custom report using configured reporter */
    private fun generateCustomReport(
            scanResults: List<ScanResult>,
            summary: ReportSummary,
            customReporter: CustomReporterConfig,
            projectPath: File
    ): GeneratedReport {
        logger.info("Generating custom report: ${customReporter.name}")
        val startTime = System.currentTimeMillis()

        // Load custom reporter class dynamically
        val reporterClass = Class.forName(customReporter.className)
        val reporter = reporterClass.getDeclaredConstructor().newInstance() as CustomReporter

        val outputFile =
                resolveOutputFile(
                        projectPath,
                        customReporter.outputPath,
                        "scan-report-${customReporter.name.toLowerCase()}",
                        customReporter.fileExtension
                )

        reporter.generateReport(scanResults, summary, customReporter.configuration, outputFile)

        val generationTime = System.currentTimeMillis() - startTime
        logger.info("Custom report '${customReporter.name}' generated: ${outputFile.absolutePath}")

        return GeneratedReport(
                format = ReportFormat.CUSTOM,
                path = outputFile.absolutePath,
                sizeBytes = outputFile.length(),
                generationTimeMs = generationTime,
                customName = customReporter.name
        )
    }

    /** Resolve output file path with timestamp and ensure directory exists */
    private fun resolveOutputFile(
            projectPath: File,
            configuredPath: String?,
            defaultName: String,
            extension: String
    ): File {
        val outputDir =
                if (configuredPath != null) {
                    File(projectPath, configuredPath).parentFile ?: projectPath
                } else {
                    File(projectPath, "build/reports/scan")
                }

        outputDir.mkdirs()

        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())
        val fileName =
                if (configuredPath?.contains("/") == true) {
                    File(configuredPath).name
                } else {
                    "${defaultName}-${timestamp}.${extension}"
                }

        return File(outputDir, fileName)
    }

    /** Calculate total scan duration from individual scan results */
    private fun calculateScanDuration(scanResults: List<ScanResult>): Long {
        return scanResults.mapNotNull { it.scanDurationMs }.sum()
    }

    /** Log comprehensive report summary */
    private fun logReportSummary(summary: ReportSummary) {
        logger.lifecycle("\n" + "=".repeat(60))
        logger.lifecycle("SECURITY SCAN REPORT SUMMARY")
        logger.lifecycle("=".repeat(60))
        logger.lifecycle("Project: ${summary.projectPath}")
        logger.lifecycle(
                "Scan completed: ${summary.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}"
        )
        logger.lifecycle("Total findings: ${summary.totalFindings}")
        logger.lifecycle("Scan duration: ${summary.scanDuration}ms")
        logger.lifecycle("Report generation: ${summary.generationTime}ms")

        if (summary.generatedReports.isNotEmpty()) {
            logger.lifecycle("\nGenerated Reports:")
            summary.generatedReports.forEach { report ->
                val name = report.customName ?: report.format.name.toLowerCase()
                logger.lifecycle("  • $name: ${report.path} (${formatFileSize(report.sizeBytes)})")
            }
        }

        // Summary by severity if available
        val severityCounts = getSeverityCounts(summary)
        if (severityCounts.isNotEmpty()) {
            logger.lifecycle("\nFindings by Severity:")
            severityCounts.forEach { (severity, count) ->
                logger.lifecycle("  • ${severity.name}: $count")
            }
        }

        logger.lifecycle("=".repeat(60))

        // Exit with error code if high severity findings exist
        if (configuration.failOnFindings && summary.totalFindings > 0) {
            val highSeverityCount = severityCounts[FindingSeverity.HIGH] ?: 0
            val criticalSeverityCount = severityCounts[FindingSeverity.CRITICAL] ?: 0

            if (highSeverityCount > 0 || criticalSeverityCount > 0) {
                logger.error(
                        "Build will fail due to high/critical severity findings (failOnFindings=true)"
                )
            }
        }
    }

    /** Get severity counts from scan results */
    private fun getSeverityCounts(summary: ReportSummary): Map<FindingSeverity, Int> {
        // This would need to be implemented based on how ScanResult stores severity
        // For now, return empty map as placeholder
        return emptyMap()
    }

    /** Format file size in human-readable format */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
            else -> "${bytes / (1024 * 1024 * 1024)}GB"
        }
    }

    /** Validate report configuration and log warnings for potential issues */
    fun validateConfiguration(): List<String> {
        val warnings = mutableListOf<String>()

        if (!configuration.reporting.consoleOutput &&
                        !configuration.reporting.jsonOutput.enabled &&
                        !configuration.reporting.htmlOutput.enabled &&
                        configuration.reporting.customReporters.isEmpty()
        ) {
            warnings.add("No report formats are enabled - no output will be generated")
        }

        // Validate output paths
        configuration.reporting.jsonOutput.outputPath?.let { path ->
            if (!isValidOutputPath(path)) {
                warnings.add("Invalid JSON output path: $path")
            }
        }

        configuration.reporting.htmlOutput.outputPath?.let { path ->
            if (!isValidOutputPath(path)) {
                warnings.add("Invalid HTML output path: $path")
            }
        }

        // Validate custom reporters
        configuration.reporting.customReporters.forEach { customReporter ->
            try {
                Class.forName(customReporter.className)
            } catch (e: ClassNotFoundException) {
                warnings.add("Custom reporter class not found: ${customReporter.className}")
            }
        }

        warnings.forEach { warning -> logger.warn("Report configuration warning: $warning") }

        return warnings
    }

    /** Check if output path is valid */
    private fun isValidOutputPath(path: String): Boolean {
        return try {
            File(path).parentFile?.let { parent -> parent.exists() || parent.mkdirs() } ?: true
        } catch (e: Exception) {
            false
        }
    }
}

/** Summary of report generation process */
data class ReportSummary(
        val totalFindings: Int,
        val timestamp: LocalDateTime,
        val projectPath: String,
        val scanDuration: Long,
        var generationTime: Long = 0,
        var generatedReports: List<GeneratedReport> = emptyList()
)

/** Information about a generated report */
data class GeneratedReport(
        val format: ReportFormat,
        val path: String,
        val sizeBytes: Long,
        val generationTimeMs: Long = 0,
        val customName: String? = null
)

/** Supported report formats */
enum class ReportFormat {
    CONSOLE,
    JSON,
    HTML,
    CUSTOM
}

/** Finding severity levels */
enum class FindingSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/** Interface for custom reporters */
interface CustomReporter {
    fun generateReport(
            scanResults: List<ScanResult>,
            summary: ReportSummary,
            configuration: Map<String, Any>,
            outputFile: File
    )
}

/** Configuration for custom reporters */
data class CustomReporterConfig(
        val name: String,
        val className: String,
        val outputPath: String?,
        val fileExtension: String,
        val configuration: Map<String, Any> = emptyMap()
)
