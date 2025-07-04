package com.scan.reporting

import com.scan.core.ScanResult
import java.io.File
import java.time.format.DateTimeFormatter
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * Console reporter that provides rich, formatted output for scan results. Supports color-coded
 * output, detailed finding information, and summary statistics.
 */
class ConsoleReporter {
    private val logger: Logger = Logging.getLogger(ConsoleReporter::class.java)

    /** Generate console report with configurable verbosity and formatting */
    fun generateReport(
            scanResults: List<ScanResult>,
            summary: ReportSummary,
            consoleOutput: Boolean = true
    ) {
        if (!consoleOutput) return

        val findings = scanResults.flatMap { it.findings }

        when {
            findings.isEmpty() -> printCleanScanResults(summary)
            else -> printFindingsReport(scanResults, summary, findings)
        }
    }

    /** Print clean scan results (no findings) */
    private fun printCleanScanResults(summary: ReportSummary) {
        logger.lifecycle(
                buildString {
                    appendLine()
                    appendLine(colorize("✅ SECURITY SCAN PASSED", ConsoleColor.GREEN, bold = true))
                    appendLine(colorize("═".repeat(50), ConsoleColor.GREEN))
                    appendLine("No security issues detected in your codebase!")
                    appendLine()
                    appendLine("📊 Scan Statistics:")
                    appendLine("   • Project: ${File(summary.projectPath).name}")
                    appendLine("   • Completed: ${formatTimestamp(summary.timestamp)}")
                    appendLine("   • Duration: ${formatDuration(summary.scanDuration)}")
                    appendLine("   • Files scanned: ${getScannedFileCount(summary)}")
                    appendLine()
                    appendLine(
                            colorize(
                                    "Your code is ready for version control! 🚀",
                                    ConsoleColor.GREEN
                            )
                    )
                    appendLine()
                }
        )
    }

    /** Print detailed findings report */
    private fun printFindingsReport(
            scanResults: List<ScanResult>,
            summary: ReportSummary,
            findings: List<Finding>
    ) {
        logger.lifecycle(
                buildString {
                    appendLine()
                    appendLine(
                            colorize("🔍 SECURITY SCAN RESULTS", ConsoleColor.YELLOW, bold = true)
                    )
                    appendLine(colorize("═".repeat(60), ConsoleColor.YELLOW))
                    appendLine()
                }
        )

        // Print findings by severity
        printFindingsBySeverity(findings)

        // Print detailed findings
        printDetailedFindings(scanResults)

        // Print summary
        printScanSummary(summary, findings)

        // Print recommendations
        printRecommendations(findings)
    }

    /** Print findings grouped by severity */
    private fun printFindingsBySeverity(findings: List<Finding>) {
        val findingsBySeverity = findings.groupBy { it.severity }

        logger.lifecycle("📈 Findings by Severity:")
        logger.lifecycle("")

        FindingSeverity.values().reversed().forEach { severity ->
            val count = findingsBySeverity[severity]?.size ?: 0
            if (count > 0) {
                val icon = getSeverityIcon(severity)
                val color = getSeverityColor(severity)
                logger.lifecycle(
                        "   $icon ${colorize(severity.name, color, bold = true)}: $count ${if (count == 1) "finding" else "findings"}"
                )
            }
        }
        logger.lifecycle("")
    }

    /** Print detailed findings for each file */
    private fun printDetailedFindings(scanResults: List<ScanResult>) {
        logger.lifecycle(colorize("📋 Detailed Findings:", ConsoleColor.CYAN, bold = true))
        logger.lifecycle("")

        scanResults.forEach { result ->
            if (result.findings.isNotEmpty()) {
                printFileFindings(result)
            }
        }
    }

    /** Print findings for a specific file */
    private fun printFileFindings(scanResult: ScanResult) {
        logger.lifecycle(colorize("📁 ${scanResult.filePath}", ConsoleColor.BLUE, bold = true))
        logger.lifecycle(
                "   ${colorize("─".repeat(scanResult.filePath.length + 2), ConsoleColor.BLUE)}"
        )

        scanResult.findings.forEachIndexed { index, finding ->
            printFindingDetails(finding, index + 1)
        }
        logger.lifecycle("")
    }

    /** Print individual finding details */
    private fun printFindingDetails(finding: Finding, index: Int) {
        val severityIcon = getSeverityIcon(finding.severity)
        val severityColor = getSeverityColor(finding.severity)

        logger.lifecycle(
                "   ${colorize("$index.", ConsoleColor.WHITE)} $severityIcon ${colorize(finding.severity.name, severityColor)} - ${finding.description}"
        )

        // Print location information
        finding.lineNumber?.let { lineNumber ->
            logger.lifecycle("      ${colorize("📍 Line $lineNumber", ConsoleColor.GRAY)}")

            finding.columnStart?.let { col ->
                logger.lifecycle("      ${colorize("   Column $col", ConsoleColor.GRAY)}")
            }
        }

        // Print matched pattern/rule
        if (finding.ruleName.isNotEmpty()) {
            logger.lifecycle("      ${colorize("🎯 Rule: ${finding.ruleName}", ConsoleColor.GRAY)}")
        }

        // Print confidence score if available
        finding.confidence?.let { confidence ->
            val confidenceColor =
                    when {
                        confidence >= 0.8 -> ConsoleColor.GREEN
                        confidence >= 0.6 -> ConsoleColor.YELLOW
                        else -> ConsoleColor.RED
                    }
            logger.lifecycle(
                    "      ${colorize("🎲 Confidence: ${(confidence * 100).toInt()}%", confidenceColor)}"
            )
        }

        // Print code snippet if available
        finding.codeSnippet?.let { snippet -> printCodeSnippet(snippet, finding.lineNumber ?: 1) }

        // Print recommendations
        if (finding.recommendations.isNotEmpty()) {
            logger.lifecycle("      ${colorize("💡 Recommendations:", ConsoleColor.CYAN)}")
            finding.recommendations.forEach { recommendation ->
                logger.lifecycle("         • $recommendation")
            }
        }

        logger.lifecycle("")
    }

    /** Print syntax-highlighted code snippet */
    private fun printCodeSnippet(snippet: String, lineNumber: Int) {
        logger.lifecycle("      ${colorize("📝 Code:", ConsoleColor.CYAN)}")

        val lines = snippet.split("\n")
        val lineNumberWidth = (lineNumber + lines.size - 1).toString().length

        lines.forEachIndexed { index, line ->
            val currentLineNumber = lineNumber + index
            val lineNumStr = currentLineNumber.toString().padStart(lineNumberWidth)
            val prefix = "         ${colorize(lineNumStr, ConsoleColor.GRAY)}│ "

            // Highlight the problematic line
            val formattedLine =
                    if (index == 0 && lines.size == 1) {
                        colorize(line, ConsoleColor.RED, background = ConsoleColor.BG_RED)
                    } else {
                        line
                    }

            logger.lifecycle("$prefix$formattedLine")
        }
        logger.lifecycle("")
    }

    /** Print comprehensive scan summary */
    private fun printScanSummary(summary: ReportSummary, findings: List<Finding>) {
        logger.lifecycle(colorize("📊 Scan Summary:", ConsoleColor.CYAN, bold = true))
        logger.lifecycle(colorize("─".repeat(40), ConsoleColor.CYAN))

        // Basic statistics
        logger.lifecycle("   • Project: ${File(summary.projectPath).name}")
        logger.lifecycle("   • Scan completed: ${formatTimestamp(summary.timestamp)}")
        logger.lifecycle("   • Total findings: ${findings.size}")
        logger.lifecycle("   • Files scanned: ${getScannedFileCount(summary)}")
        logger.lifecycle("   • Scan duration: ${formatDuration(summary.scanDuration)}")
        logger.lifecycle("   • Report generation: ${formatDuration(summary.generationTime)}")

        // Severity breakdown
        val severityBreakdown = findings.groupBy { it.severity }
        if (severityBreakdown.isNotEmpty()) {
            logger.lifecycle("")
            logger.lifecycle("   Severity Breakdown:")
            FindingSeverity.values().reversed().forEach { severity ->
                val count = severityBreakdown[severity]?.size ?: 0
                if (count > 0) {
                    val percentage = (count * 100.0 / findings.size).toInt()
                    val icon = getSeverityIcon(severity)
                    val color = getSeverityColor(severity)
                    logger.lifecycle(
                            "   • $icon ${colorize(severity.name, color)}: $count ($percentage%)"
                    )
                }
            }
        }

        // File types analysis
        val fileTypeBreakdown = getFileTypeBreakdown(summary)
        if (fileTypeBreakdown.isNotEmpty()) {
            logger.lifecycle("")
            logger.lifecycle("   Files by Type:")
            fileTypeBreakdown.forEach { (extension, count) ->
                logger.lifecycle("   • ${extension.uppercase()}: $count files")
            }
        }

        logger.lifecycle("")
    }

    /** Print actionable recommendations */
    private fun printRecommendations(findings: List<Finding>) {
        val criticalCount = findings.count { it.severity == FindingSeverity.CRITICAL }
        val highCount = findings.count { it.severity == FindingSeverity.HIGH }

        logger.lifecycle(colorize("🚨 Recommendations:", ConsoleColor.YELLOW, bold = true))
        logger.lifecycle(colorize("─".repeat(40), ConsoleColor.YELLOW))

        when {
            criticalCount > 0 -> {
                logger.lifecycle(
                        colorize(
                                "   ⛔ CRITICAL: Immediate action required!",
                                ConsoleColor.RED,
                                bold = true
                        )
                )
                logger.lifecycle("   • Review and fix critical security issues before committing")
                logger.lifecycle("   • Consider these as blocking issues for production deployment")
            }
            highCount > 0 -> {
                logger.lifecycle(
                        colorize(
                                "   ⚠️  HIGH: Address these issues soon",
                                ConsoleColor.YELLOW,
                                bold = true
                        )
                )
                logger.lifecycle("   • Plan to fix high-severity issues in the next release cycle")
                logger.lifecycle("   • Consider implementing additional security measures")
            }
            else -> {
                logger.lifecycle(
                        colorize("   ✅ Good security posture!", ConsoleColor.GREEN, bold = true)
                )
                logger.lifecycle("   • Consider fixing remaining issues for enhanced security")
            }
        }

        logger.lifecycle("")
        logger.lifecycle("   General Recommendations:")
        logger.lifecycle("   • Review all findings in detail")
        logger.lifecycle("   • Use environment variables for sensitive data")
        logger.lifecycle("   • Implement proper secret management")
        logger.lifecycle("   • Add security scanning to your CI/CD pipeline")
        logger.lifecycle("   • Regular security audits and dependency updates")

        // Next steps
        logger.lifecycle("")
        logger.lifecycle(colorize("📋 Next Steps:", ConsoleColor.BLUE, bold = true))
        logger.lifecycle("   1. Review detailed HTML/JSON reports if generated")
        logger.lifecycle("   2. Fix critical and high-severity findings")
        logger.lifecycle("   3. Add exclusions for false positives if needed")
        logger.lifecycle("   4. Re-run scan to verify fixes")
        logger.lifecycle("   5. Configure CI/CD integration for continuous scanning")
        logger.lifecycle("")
    }

    /** Get icon for severity level */
    private fun getSeverityIcon(severity: FindingSeverity): String =
            when (severity) {
                FindingSeverity.CRITICAL -> "🔴"
                FindingSeverity.HIGH -> "🟠"
                FindingSeverity.MEDIUM -> "🟡"
                FindingSeverity.LOW -> "🔵"
            }

    /** Get color for severity level */
    private fun getSeverityColor(severity: FindingSeverity): ConsoleColor =
            when (severity) {
                FindingSeverity.CRITICAL -> ConsoleColor.RED
                FindingSeverity.HIGH -> ConsoleColor.YELLOW
                FindingSeverity.MEDIUM -> ConsoleColor.BLUE
                FindingSeverity.LOW -> ConsoleColor.GREEN
            }

    /** Format timestamp for display */
    private fun formatTimestamp(timestamp: java.time.LocalDateTime): String {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }

    /** Format duration in human-readable format */
    private fun formatDuration(durationMs: Long): String =
            when {
                durationMs < 1000 -> "${durationMs}ms"
                durationMs < 60000 -> "${durationMs / 1000}.${(durationMs % 1000) / 100}s"
                else -> "${durationMs / 60000}m ${(durationMs % 60000) / 1000}s"
            }

    /** Get scanned file count from summary */
    private fun getScannedFileCount(summary: ReportSummary): Int {
        // This would be implemented based on how ScanResult tracks file counts
        // Placeholder implementation
        return 0
    }

    /** Get file type breakdown */
    private fun getFileTypeBreakdown(summary: ReportSummary): Map<String, Int> {
        // This would be implemented based on scan results
        // Placeholder implementation
        return emptyMap()
    }

    /** Apply console colors and formatting */
    private fun colorize(
            text: String,
            color: ConsoleColor,
            bold: Boolean = false,
            background: ConsoleColor? = null
    ): String {
        if (!supportsColor()) return text

        val codes = mutableListOf<String>()
        if (bold) codes.add("1")
        codes.add(color.code)
        background?.let { codes.add(it.code) }

        return "\u001B[${codes.joinToString(";")}m$text\u001B[0m"
    }

    /** Check if terminal supports color output */
    private fun supportsColor(): Boolean {
        val term = System.getenv("TERM")
        val colorTerm = System.getenv("COLORTERM")
        val noColor = System.getenv("NO_COLOR")

        return when {
            noColor?.isNotEmpty() == true -> false
            colorTerm?.isNotEmpty() == true -> true
            term?.contains("color") == true -> true
            term?.contains("xterm") == true -> true
            System.getProperty("os.name").lowercase().contains("windows") -> {
                // Windows 10+ supports ANSI colors
                System.getProperty("os.version").let { version ->
                    version.split(".").firstOrNull()?.toIntOrNull()?.let { it >= 10 } ?: false
                }
            }
            else -> true // Default to true for most Unix-like systems
        }
    }
}

/** Console color codes */
enum class ConsoleColor(val code: String) {
    // Foreground colors
    BLACK("30"),
    RED("31"),
    GREEN("32"),
    YELLOW("33"),
    BLUE("34"),
    MAGENTA("35"),
    CYAN("36"),
    WHITE("37"),
    GRAY("90"),

    // Background colors
    BG_BLACK("40"),
    BG_RED("41"),
    BG_GREEN("42"),
    BG_YELLOW("43"),
    BG_BLUE("44"),
    BG_MAGENTA("45"),
    BG_CYAN("46"),
    BG_WHITE("47")
}

/** Represents a security finding for console reporting */
data class Finding(
        val description: String,
        val severity: FindingSeverity,
        val ruleName: String = "",
        val lineNumber: Int? = null,
        val columnStart: Int? = null,
        val columnEnd: Int? = null,
        val codeSnippet: String? = null,
        val confidence: Double? = null,
        val recommendations: List<String> = emptyList()
)
