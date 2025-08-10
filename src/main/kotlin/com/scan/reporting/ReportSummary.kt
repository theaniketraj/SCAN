package com.scan.reporting

import java.time.LocalDateTime

/**
 * Summary information for scan reporting
 */
data class ReportSummary(
    val totalFindings: Int,
    val timestamp: LocalDateTime,
    val projectPath: String,
    val scanDuration: Long,
    var generationTime: Long = 0,
    var generatedReports: List<GeneratedReport> = emptyList(),
    // Additional fields for console reporter compatibility
    val totalFiles: Int = 0,
    val scannedFiles: Int = 0,
    val skippedFiles: Int = 0,
    val errorFiles: Int = 0
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
