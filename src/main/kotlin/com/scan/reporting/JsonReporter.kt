package com.scan.reporting

import com.scan.core.ScanConfiguration
import com.scan.core.ScanResult
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.logging.Logger

/**
 * JSON reporter that generates structured JSON reports for scan results.
 *
 * This reporter creates machine-readable JSON output that can be easily consumed by CI/CD systems,
 * security dashboards, and other tools.
 */
class JsonReporter(private val logger: Logger, private val configuration: ScanConfiguration) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        private const val DEFAULT_OUTPUT_FILE = "scan-results.json"
        private val ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    }

    /**
     * Generates a JSON report from scan results.
     *
     * @param results List of scan results to report
     * @param outputFile Optional custom output file path
     * @return The generated JSON report as a string
     */
    fun generateReport(results: List<ScanResult>, outputFile: String? = null): String {
        val reportData = createReportData(results)
        val jsonReport = json.encodeToString(reportData)

        val targetFile =
                outputFile
                        ?: configuration.outputDir?.let {
                            File(it, DEFAULT_OUTPUT_FILE).absolutePath
                        }
                                ?: DEFAULT_OUTPUT_FILE

        try {
            writeReportToFile(jsonReport, targetFile)
            logger.lifecycle("JSON report generated: $targetFile")
        } catch (e: Exception) {
            logger.error("Failed to write JSON report to file: ${e.message}")
            throw ReportGenerationException("Failed to generate JSON report", e)
        }

        return jsonReport
    }

    /** Creates the structured report data from scan results. */
    private fun createReportData(results: List<ScanResult>): JsonScanReport {
        val timestamp = Instant.now().atOffset(ZoneOffset.UTC).format(ISO_FORMATTER)
        val totalFiles = results.map { it.filePath }.distinct().size
        val totalFindings = results.size
        val criticalFindings = results.count { it.severity == ScanResult.Severity.CRITICAL }
        val highFindings = results.count { it.severity == ScanResult.Severity.HIGH }
        val mediumFindings = results.count { it.severity == ScanResult.Severity.MEDIUM }
        val lowFindings = results.count { it.severity == ScanResult.Severity.LOW }

        val findingsByType =
                results.groupBy { it.ruleId }.mapValues { (_, findings) -> findings.size }

        val findingsByFile =
                results.groupBy { it.filePath }.map { (filePath, findings) ->
                    JsonFileResult(
                            filePath = filePath,
                            findingsCount = findings.size,
                            findings =
                                    findings.map { result ->
                                        JsonFinding(
                                                ruleId = result.ruleId,
                                                ruleName = result.ruleName,
                                                description = result.description,
                                                severity = result.severity.name.lowercase(),
                                                confidence = result.confidence.name.lowercase(),
                                                lineNumber = result.lineNumber,
                                                columnStart = result.columnStart,
                                                columnEnd = result.columnEnd,
                                                matchedText =
                                                        if (configuration.includeMatchedText)
                                                                result.matchedText
                                                        else "[REDACTED]",
                                                context =
                                                        result.context?.let { ctx ->
                                                            JsonContext(
                                                                    before = ctx.before,
                                                                    after = ctx.after,
                                                                    linesBefore = ctx.linesBefore,
                                                                    linesAfter = ctx.linesAfter
                                                            )
                                                        },
                                                metadata = result.metadata
                                        )
                                    }
                    )
                }

        return JsonScanReport(
                metadata =
                        JsonReportMetadata(
                                timestamp = timestamp,
                                version = getPluginVersion(),
                                scanConfiguration =
                                        JsonScanConfiguration(
                                                scanPaths = configuration.scanPaths,
                                                excludePaths = configuration.excludePaths,
                                                includePatterns = configuration.includePatterns,
                                                excludePatterns = configuration.excludePatterns,
                                                enabledDetectors = configuration.enabledDetectors,
                                                maxFileSize = configuration.maxFileSize,
                                                followSymlinks = configuration.followSymlinks,
                                                scanHiddenFiles = configuration.scanHiddenFiles
                                        )
                        ),
                summary =
                        JsonSummary(
                                totalFiles = totalFiles,
                                totalFindings = totalFindings,
                                findingsBySeverity =
                                        JsonSeverityBreakdown(
                                                critical = criticalFindings,
                                                high = highFindings,
                                                medium = mediumFindings,
                                                low = lowFindings
                                        ),
                                findingsByType = findingsByType,
                                scanDurationMs = results.firstOrNull()?.scanDuration ?: 0L
                        ),
                results = findingsByFile
        )
    }

    /** Writes the JSON report to the specified file. */
    private fun writeReportToFile(jsonContent: String, filePath: String) {
        val file = File(filePath)
        file.parentFile?.mkdirs()
        file.writeText(jsonContent)
    }

    /** Gets the plugin version for metadata. */
    private fun getPluginVersion(): String {
        return try {
            javaClass.`package`.implementationVersion ?: "development"
        } catch (e: Exception) {
            "unknown"
        }
    }

    /** Validates that the JSON report can be properly serialized. */
    fun validateReport(results: List<ScanResult>): Boolean {
        return try {
            val reportData = createReportData(results)
            json.encodeToString(reportData)
            true
        } catch (e: Exception) {
            logger.error("JSON report validation failed: ${e.message}")
            false
        }
    }

    /** Creates a compact JSON report without pretty printing. */
    fun generateCompactReport(results: List<ScanResult>): String {
        val compactJson = Json {
            prettyPrint = false
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

        val reportData = createReportData(results)
        return compactJson.encodeToString(reportData)
    }

    /** Generates a JSON report with only summary information (no detailed findings). */
    fun generateSummaryReport(results: List<ScanResult>): String {
        val reportData = createReportData(results)
        val summaryReport =
                JsonScanReport(
                        metadata = reportData.metadata,
                        summary = reportData.summary,
                        results = emptyList()
                )

        return json.encodeToString(summaryReport)
    }
}

/** Data classes for JSON serialization */
@Serializable
data class JsonScanReport(
        val metadata: JsonReportMetadata,
        val summary: JsonSummary,
        val results: List<JsonFileResult>
)

@Serializable
data class JsonReportMetadata(
        val timestamp: String,
        val version: String,
        val scanConfiguration: JsonScanConfiguration
)

@Serializable
data class JsonScanConfiguration(
        val scanPaths: List<String>,
        val excludePaths: List<String>,
        val includePatterns: List<String>,
        val excludePatterns: List<String>,
        val enabledDetectors: List<String>,
        val maxFileSize: Long,
        val followSymlinks: Boolean,
        val scanHiddenFiles: Boolean
)

@Serializable
data class JsonSummary(
        val totalFiles: Int,
        val totalFindings: Int,
        val findingsBySeverity: JsonSeverityBreakdown,
        val findingsByType: Map<String, Int>,
        val scanDurationMs: Long
)

@Serializable
data class JsonSeverityBreakdown(val critical: Int, val high: Int, val medium: Int, val low: Int)

@Serializable
data class JsonFileResult(
        val filePath: String,
        val findingsCount: Int,
        val findings: List<JsonFinding>
)

@Serializable
data class JsonFinding(
        val ruleId: String,
        val ruleName: String,
        val description: String,
        val severity: String,
        val confidence: String,
        val lineNumber: Int,
        val columnStart: Int?,
        val columnEnd: Int?,
        val matchedText: String,
        val context: JsonContext?,
        val metadata: Map<String, String>?
)

@Serializable
data class JsonContext(
        val before: String?,
        val after: String?,
        val linesBefore: List<String>?,
        val linesAfter: List<String>?
)

/** Custom exception for report generation errors. */
class ReportGenerationException(message: String, cause: Throwable? = null) :
        Exception(message, cause)
