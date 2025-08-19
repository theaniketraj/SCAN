package com.scan.reporting

import com.scan.core.ScanResult
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * JSON reporter that generates structured JSON reports for scan results.
 *
 * This reporter creates machine-readable JSON output that can be easily consumed by CI/CD systems,
 * security dashboards, and other tools.
 */
class JsonReporter {

    companion object {
        private const val DEFAULT_OUTPUT_FILE = "scan-results.json"
        private val ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    }

    /**
     * Generates a JSON report from scan results.
     *
     * @param results List of scan results to report
     * @param outputDir Output directory for the report
     * @return The generated JSON report as a string
     */
    fun generateReport(results: List<ScanResult>, outputDir: String): String {
        val jsonReport = createJsonReport(results)
        val targetFile = File(outputDir, DEFAULT_OUTPUT_FILE)

        try {
            writeReportToFile(jsonReport, targetFile.absolutePath)
        } catch (e: Exception) {
            throw ReportGenerationException("Failed to generate JSON report", e)
        }

        return jsonReport
    }

    /** Creates the JSON report from scan results. */
    private fun createJsonReport(results: List<ScanResult>): String {
        val timestamp = Instant.now().atOffset(ZoneOffset.UTC).format(ISO_FORMATTER)
        val allFindings = results.flatMap { it.findings }

        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"scanReport\": {\n")
        sb.append("    \"metadata\": {\n")
        sb.append("      \"timestamp\": \"$timestamp\",\n")
        sb.append("      \"version\": \"2.0.0\",\n")
        sb.append("      \"tool\": \"SCAN Security Scanner\"\n")
        sb.append("    },\n")
        sb.append("    \"summary\": {\n")
        sb.append("      \"totalFiles\": ${results.size},\n")
        sb.append("      \"totalFindings\": ${allFindings.size},\n")
        sb.append("      \"criticalFindings\": ${allFindings.count { it.severity == com.scan.core.Severity.CRITICAL }},\n")
        sb.append("      \"highFindings\": ${allFindings.count { it.severity == com.scan.core.Severity.HIGH }},\n")
        sb.append("      \"mediumFindings\": ${allFindings.count { it.severity == com.scan.core.Severity.MEDIUM }},\n")
        sb.append("      \"lowFindings\": ${allFindings.count { it.severity == com.scan.core.Severity.LOW }}\n")
        sb.append("    },\n")
        sb.append("    \"findings\": [\n")

        allFindings.forEachIndexed { index, finding ->
            sb.append("      {\n")
            sb.append("        \"id\": \"${finding.id}\",\n")
            sb.append("        \"title\": \"${escapeJson(finding.title)}\",\n")
            sb.append("        \"description\": \"${escapeJson(finding.description)}\",\n")
            sb.append("        \"severity\": \"${finding.severity.name}\",\n")
            sb.append("        \"confidence\": \"${finding.confidence.name}\",\n")
            sb.append("        \"location\": {\n")
            sb.append("          \"filePath\": \"${escapeJson(finding.location.filePath)}\",\n")
            sb.append("          \"lineNumber\": ${finding.location.lineNumber},\n")
            sb.append("          \"columnStart\": ${finding.location.columnStart},\n")
            sb.append("          \"columnEnd\": ${finding.location.columnEnd}\n")
            sb.append("        },\n")
            sb.append("        \"secretInfo\": {\n")
            sb.append("          \"type\": \"${finding.secretInfo.secretType.name}\",\n")
            sb.append("          \"patternName\": \"${escapeJson(finding.secretInfo.patternName)}\",\n")
            sb.append("          \"entropy\": ${finding.secretInfo.entropy}\n")
            sb.append("        }\n")
            sb.append("      }")
            if (index < allFindings.size - 1) sb.append(",")
            sb.append("\n")
        }

        sb.append("    ]\n")
        sb.append("  }\n")
        sb.append("}")

        return sb.toString()
    }

    private fun escapeJson(text: String): String {
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun writeReportToFile(content: String, filePath: String) {
        File(filePath).writeText(content)
    }
}

/** Exception thrown when report generation fails */
class ReportGenerationException(message: String, cause: Throwable? = null) : Exception(message, cause)
