package com.scan.core

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Complete scan result containing all findings and metadata */
data class ScanResult(
        val scanId: String,
        val timestamp: LocalDateTime,
        val configuration: ScanResultConfiguration,
        val summary: ScanSummary,
        val findings: List<Finding>,
        val errors: List<ScanError>,
        val performance: PerformanceMetrics
) {
    /** Check if scan found any security issues */
    fun hasFindings(): Boolean = findings.isNotEmpty()

    /** Check if scan completed successfully (no errors) */
    fun isSuccessful(): Boolean = errors.isEmpty()

    /** Get findings by severity level */
    fun getFindingsBySeverity(severity: Severity): List<Finding> {
        return findings.filter { it.severity == severity }
    }

    /** Get findings by detector type */
    fun getFindingsByDetector(detectorType: String): List<Finding> {
        return findings.filter { it.detectorType == detectorType }
    }

    /** Get findings for a specific file */
    fun getFindingsForFile(filePath: String): List<Finding> {
        return findings.filter { it.location.filePath == filePath }
    }

    /** Get unique files that contain findings */
    fun getAffectedFiles(): Set<String> {
        return findings.map { it.location.filePath }.toSet()
    }

    /** Format timestamp for display */
    fun getFormattedTimestamp(): String {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }

    /** Get scan duration in milliseconds */
    fun getScanDurationMs(): Long = performance.totalDurationMs

    /** Get scan duration formatted as human-readable string */
    fun getFormattedDuration(): String {
        val duration = performance.totalDurationMs
        return when {
            duration < 1000 -> "${duration}ms"
            duration < 60000 -> "${duration / 1000}s"
            else -> "${duration / 60000}m ${(duration % 60000) / 1000}s"
        }
    }
}

/** Summary statistics for the scan */
data class ScanSummary(
        val totalFilesScanned: Int,
        val totalFindingsCount: Int,
        val findingsBySeverity: Map<Severity, Int>,
        val findingsByDetector: Map<String, Int>,
        val totalLinesScanned: Long,
        val skippedFiles: Int,
        val skippedReason: Map<String, Int>
) {
    fun getCriticalCount(): Int = findingsBySeverity[Severity.CRITICAL] ?: 0
    fun getHighCount(): Int = findingsBySeverity[Severity.HIGH] ?: 0
    fun getMediumCount(): Int = findingsBySeverity[Severity.MEDIUM] ?: 0
    fun getLowCount(): Int = findingsBySeverity[Severity.LOW] ?: 0
    fun getInfoCount(): Int = findingsBySeverity[Severity.INFO] ?: 0

    fun getSkippedCount(): Int = skippedFiles
}

/** Configuration snapshot for the scan */
data class ScanResultConfiguration(
        val configurationSource: String,
        val enabledDetectors: List<String>,
        val excludedPaths: List<String>,
        val includedExtensions: List<String>,
        val severityThreshold: Severity,
        val maxFileSize: Long,
        val customPatterns: Int
)

/** Scan error information */
data class ScanError(
        val errorType: ErrorType,
        val message: String,
        val filePath: String? = null,
        val lineNumber: Int? = null,
        val exception: String? = null,
        val timestamp: LocalDateTime = LocalDateTime.now()
) {
    fun getFormattedMessage(): String {
        return buildString {
            append("[$errorType] ")
            append(message)
            filePath?.let { append(" in $it") }
            lineNumber?.let { append(" at line $it") }
        }
    }
}

/** Performance metrics for the scan */
data class PerformanceMetrics(
        val totalDurationMs: Long,
        val fileProcessingTimeMs: Long,
        val patternMatchingTimeMs: Long,
        val entropyCalculationTimeMs: Long,
        val reportGenerationTimeMs: Long,
        val memoryUsageBytes: Long,
        val peakMemoryUsageBytes: Long,
        val filesPerSecond: Double,
        val averageFileProcessingTimeMs: Double
) {
    fun getFormattedMetrics(): Map<String, String> {
        return mapOf(
                "Total Duration" to "${totalDurationMs}ms",
                "File Processing" to "${fileProcessingTimeMs}ms",
                "Pattern Matching" to "${patternMatchingTimeMs}ms",
                "Entropy Calculation" to "${entropyCalculationTimeMs}ms",
                "Report Generation" to "${reportGenerationTimeMs}ms",
                "Memory Usage" to "${memoryUsageBytes / 1024 / 1024}MB",
                "Peak Memory" to "${peakMemoryUsageBytes / 1024 / 1024}MB",
                "Files/Second" to "%.2f".format(filesPerSecond),
                "Avg File Time" to "%.2fms".format(averageFileProcessingTimeMs)
        )
    }
}

/** Builder class for creating ScanResult instances */
class ScanResultBuilder {
    private var scanId: String = ""
    private var timestamp: LocalDateTime = LocalDateTime.now()
    private var configuration: ScanResultConfiguration? = null
    private var summary: ScanSummary? = null
    private val findings: MutableList<Finding> = mutableListOf()
    private val errors: MutableList<ScanError> = mutableListOf()
    private var performance: PerformanceMetrics? = null

    fun scanId(id: String) = apply { this.scanId = id }
    fun timestamp(time: LocalDateTime) = apply { this.timestamp = time }
    fun configuration(config: ScanResultConfiguration) = apply { this.configuration = config }
    fun summary(summary: ScanSummary) = apply { this.summary = summary }
    fun addFinding(finding: Finding) = apply { this.findings.add(finding) }
    fun addFindings(findings: List<Finding>) = apply { this.findings.addAll(findings) }
    fun addError(error: ScanError) = apply { this.errors.add(error) }
    fun addErrors(errors: List<ScanError>) = apply { this.errors.addAll(errors) }
    fun performance(metrics: PerformanceMetrics) = apply { this.performance = metrics }

    fun build(): ScanResult {
        requireNotNull(configuration) { "Configuration is required" }
        requireNotNull(summary) { "Summary is required" }
        requireNotNull(performance) { "Performance metrics are required" }

        return ScanResult(
                scanId = scanId,
                timestamp = timestamp,
                configuration = configuration!!,
                summary = summary!!,
                findings = findings.toList(),
                errors = errors.toList(),
                performance = performance!!
        )
    }
}
