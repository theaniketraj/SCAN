package com.scan.reporting

import com.scan.core.ScanConfiguration
import com.scan.core.ScanResult
import com.scan.core.Severity
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Central report generator that coordinates different report formats and manages output
 * destinations. Supports multiple concurrent report formats and provides extensible architecture
 * for new formats.
 */
class ReportGenerator(private val configuration: ScanConfiguration) {
    companion object {
        private const val KB: Long = 1024
        private const val MB: Long = 1024 * KB
        private const val GB: Long = 1024 * MB
        private const val HEADER_WIDTH = 60
        private val TIMESTAMP_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    // Simple logger replacement
    private fun log(message: String) {
        println("[ReportGenerator] $message")
    }

    private fun logError(message: String) {
        System.err.println("[ReportGenerator ERROR] $message")
    }

    private val consoleReporter = ConsoleReporter()
    private val jsonReporter = JsonReporter()
    private val htmlReporter = HtmlReporter()

    /** Generate reports in all configured formats */
    fun generateReports(scanResults: List<ScanResult>, projectPath: File): ReportSummary {
        val startTime = System.currentTimeMillis()
        val timestamp = LocalDateTime.now()

        log("Generating scan reports for ${scanResults.size} findings...")

        val reportSummary =
            ReportSummary(
                totalFindings = scanResults.size,
                timestamp = timestamp,
                projectPath = projectPath.absolutePath,
                scanDuration = calculateScanDuration(scanResults)
            )

        val generatedReports = mutableListOf<GeneratedReport>()

        // Always generate console output
        generateConsoleReport(scanResults, reportSummary)
        generatedReports.add(GeneratedReport(ReportFormat.CONSOLE, "console", 0))

        // Generate JSON report if configured
        if (configuration.reporting.formats.contains(com.scan.core.ReportFormat.JSON)) {
            val jsonReport = generateJsonReport(scanResults, reportSummary, projectPath)
            generatedReports.add(jsonReport)
        }

        // Generate HTML report if configured
        if (configuration.reporting.formats.contains(com.scan.core.ReportFormat.HTML)) {
            val htmlReport = generateHtmlReport(scanResults, reportSummary, projectPath)
            generatedReports.add(htmlReport)
        }

        // Skip custom reporters for now since they're not properly configured

        val totalTime = System.currentTimeMillis() - startTime
        reportSummary.generationTime = totalTime
        reportSummary.generatedReports = generatedReports

        log("Report generation completed in ${totalTime}ms")
        logReportSummary(reportSummary)

        return reportSummary
    }

    /** Generate console report */
    private fun generateConsoleReport(scanResults: List<ScanResult>, @Suppress("UNUSED_PARAMETER") summary: ReportSummary) {
        log("Generating console report...")
        consoleReporter.generateReport(scanResults, summary, true)
    }

    /** Generate JSON report */
    private fun generateJsonReport(
        scanResults: List<ScanResult>,
        @Suppress("UNUSED_PARAMETER") summary: ReportSummary,
        projectPath: File
    ): GeneratedReport {
        log("Generating JSON report...")
        val startTime = System.currentTimeMillis()

        val outputFile =
            resolveOutputFile(
                projectPath,
                null, // Use default path since json config doesn't exist
                "scan-report",
                "json"
            )

        // Create a consolidated ScanResult from all results
        val consolidatedResult = if (scanResults.isNotEmpty()) {
            scanResults.first().copy(findings = scanResults.flatMap { it.findings })
        } else {
            // Create a default empty result - simplified approach
            throw IllegalStateException("No scan results provided")
        }

        jsonReporter.generateReport(listOf(consolidatedResult), outputFile.absolutePath)

        val generationTime = System.currentTimeMillis() - startTime
        log("JSON report generated: ${outputFile.absolutePath}")

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
        @Suppress("UNUSED_PARAMETER") summary: ReportSummary,
        projectPath: File
    ): GeneratedReport {
        log("Generating HTML report...")
        val startTime = System.currentTimeMillis()

        val outputFile =
            resolveOutputFile(
                projectPath,
                null, // Use default path since html config doesn't exist
                "scan-report",
                "html"
            )

        // Create a consolidated ScanResult from all results
        val consolidatedResult = if (scanResults.isNotEmpty()) {
            scanResults.first().copy(findings = scanResults.flatMap { it.findings })
        } else {
            // Create a default empty result - simplified approach
            throw IllegalStateException("No scan results provided")
        }

        // For now, we'll use a synchronous approach until coroutines are properly configured
        htmlReporter.generateReportSync(consolidatedResult, outputFile)

        val generationTime = System.currentTimeMillis() - startTime
        log("HTML report generated: ${outputFile.absolutePath}")

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
        @Suppress("UNUSED_PARAMETER") summary: ReportSummary,
        reporterName: String,
        projectPath: File
    ): GeneratedReport {
        log("Generating custom report: $reporterName")
        val startTime = System.currentTimeMillis()

        val outputFile =
            resolveOutputFile(
                projectPath,
                null,
                "scan-report-${reporterName.lowercase()}",
                "txt"
            )

        // For now, just create a basic text report
        val report = buildString {
            appendLine("Custom Report: $reporterName")
            appendLine("Generated: ${java.time.LocalDateTime.now()}")
            appendLine("Total findings: ${scanResults.flatMap { it.findings }.size}")
        }

        outputFile.writeText(report)

        val generationTime = System.currentTimeMillis() - startTime
        log("Custom report '$reporterName' generated: ${outputFile.absolutePath}")

        return GeneratedReport(
            format = ReportFormat.CUSTOM,
            path = outputFile.absolutePath,
            sizeBytes = outputFile.length(),
            generationTimeMs = generationTime,
            customName = reporterName
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
                "$defaultName-$timestamp.$extension"
            }

        return File(outputDir, fileName)
    }

    /** Calculate total scan duration from individual scan results */
    private fun calculateScanDuration(scanResults: List<ScanResult>): Long {
        return scanResults.sumOf { it.performance.totalDurationMs }
    }

    /** Log comprehensive report summary */
    private fun logReportSummary(summary: ReportSummary) {
        log("\n" + "=".repeat(HEADER_WIDTH))
        log("SECURITY SCAN REPORT SUMMARY")
        log("=".repeat(HEADER_WIDTH))
        log("Project: ${summary.projectPath}")
        log("Scan completed: ${summary.timestamp.format(TIMESTAMP_FORMATTER)}")
        log("Total findings: ${summary.totalFindings}")
        log("Scan duration: ${summary.scanDuration}ms")
        log("Report generation: ${summary.generationTime}ms")

        if (summary.generatedReports.isNotEmpty()) {
            log("\nGenerated Reports:")
            summary.generatedReports.forEach { report ->
                val name = report.customName ?: report.format.name.lowercase()
                log("  • $name: ${report.path} (${formatFileSize(report.sizeBytes)})")
            }
        }

        // Summary by severity if available
        val severityCounts = getSeverityCounts(summary)
        if (severityCounts.isNotEmpty()) {
            log("\nFindings by Severity:")
            severityCounts.forEach { (severity, count) ->
                log("  • ${severity.name}: $count")
            }
        }

        log("=".repeat(HEADER_WIDTH))

        // Exit with error code if high severity findings exist
        if (configuration.buildIntegration.failOnFindings && summary.totalFindings > 0) {
            val highSeverityCount = severityCounts[Severity.HIGH] ?: 0
            val criticalSeverityCount = severityCounts[Severity.CRITICAL] ?: 0

            if (highSeverityCount > 0 || criticalSeverityCount > 0) {
                logError(
                    "Build will fail due to high/critical severity findings (failOnFindings=true)"
                )
            }
        }
    }

    /** Get severity counts from scan results */
    private fun getSeverityCounts(@Suppress("UNUSED_PARAMETER") summary: ReportSummary): Map<Severity, Int> {
        // This would need to be implemented based on how ScanResult stores severity
        // For now, return empty map as placeholder
        return emptyMap()
    }

    /** Format file size in human-readable format */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < KB -> "${bytes}B"
            bytes < MB -> "${bytes / KB}KB"
            bytes < GB -> "${bytes / MB}MB"
            else -> "${bytes / GB}GB"
        }
    }

    /** Validate report configuration and log warnings for potential issues */
    fun validateConfiguration(): List<String> {
        val warnings = mutableListOf<String>()

        // Check if at least one output format is available
        // Since the configuration structure doesn't have enabled flags, assume they're enabled if formats are specified
        if (configuration.reporting.formats.isEmpty()) {
            warnings.add("No report formats are configured - no output will be generated")
        }

        warnings.forEach { warning -> log("Report configuration warning: $warning") }

        return warnings
    }

    /** Check if output path is valid */
    private fun isValidOutputPath(path: String): Boolean {
        return try {
            File(path).parentFile?.let { parent -> parent.exists() || parent.mkdirs() } ?: true
        } catch (_: SecurityException) {
            false
        }
    }
}
