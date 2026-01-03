package com.scan.reporting

import com.scan.core.Finding
import com.scan.core.ScanResult
import com.scan.core.Severity
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * SARIF (Static Analysis Results Interchange Format) reporter for GitHub Code Scanning integration.
 *
 * This reporter generates SARIF v2.1.0 format JSON files that can be uploaded to GitHub's
 * Code Scanning API to create security alerts in repositories.
 *
 * @see https://docs.github.com/en/code-security/code-scanning/integrating-with-code-scanning/sarif-support-for-code-scanning
 * @see https://sarifweb.azurewebsites.net/
 */
class SarifReporter {

    companion object {
        private const val DEFAULT_OUTPUT_FILE = "scan-results.sarif"
        private const val SARIF_VERSION = "2.1.0"
        private const val SARIF_SCHEMA = "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json"
        private val ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    }

    /**
     * Generates a SARIF report from scan results.
     *
     * @param results List of scan results to report
     * @param outputDir Output directory for the report
     * @param repositoryUri Optional repository URI for GitHub integration
     * @return The generated SARIF report as a string
     */
    fun generateReport(
        results: List<ScanResult>,
        outputDir: String,
        repositoryUri: String? = null
    ): String {
        val sarifReport = createSarifReport(results, repositoryUri)
        val targetFile = File(outputDir, DEFAULT_OUTPUT_FILE)

        try {
            writeReportToFile(sarifReport, targetFile.absolutePath)
        } catch (e: Exception) {
            throw ReportGenerationException("Failed to generate SARIF report", e)
        }

        return sarifReport
    }

    /** Creates the SARIF report from scan results. */
    private fun createSarifReport(results: List<ScanResult>, repositoryUri: String?): String {
        val timestamp = Instant.now().atOffset(ZoneOffset.UTC).format(ISO_FORMATTER)
        val allFindings = results.flatMap { it.findings }

        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"\$schema\": \"$SARIF_SCHEMA\",\n")
        sb.append("  \"version\": \"$SARIF_VERSION\",\n")
        sb.append("  \"runs\": [\n")
        sb.append("    {\n")
        
        // Tool information
        sb.append("      \"tool\": {\n")
        sb.append("        \"driver\": {\n")
        sb.append("          \"name\": \"SCAN\",\n")
        sb.append("          \"informationUri\": \"https://github.com/theaniketraj/SCAN\",\n")
        sb.append("          \"version\": \"2.0.0\",\n")
        sb.append("          \"semanticVersion\": \"2.0.0\",\n")
        sb.append("          \"organization\": \"SCAN Security\",\n")
        sb.append("          \"shortDescription\": {\n")
        sb.append("            \"text\": \"Security scanner for detecting secrets and credentials in source code\"\n")
        sb.append("          },\n")
        sb.append("          \"rules\": [\n")
        
        // Generate unique rules from findings
        val rules = generateRules(allFindings)
        sb.append(rules.joinToString(",\n") { "            $it" })
        
        sb.append("\n          ]\n")
        sb.append("        }\n")
        sb.append("      },\n")
        
        // Results
        sb.append("      \"results\": [\n")
        val resultEntries = allFindings.mapIndexed { index, finding ->
            createSarifResult(finding, index, repositoryUri)
        }
        sb.append(resultEntries.joinToString(",\n") { "        $it" })
        sb.append("\n      ],\n")
        
        // Automation details
        sb.append("      \"automationDetails\": {\n")
        sb.append("        \"id\": \"scan/${timestamp}\"\n")
        sb.append("      },\n")
        
        // Invocations
        sb.append("      \"invocations\": [\n")
        sb.append("        {\n")
        sb.append("          \"executionSuccessful\": true,\n")
        sb.append("          \"endTimeUtc\": \"$timestamp\"\n")
        sb.append("        }\n")
        sb.append("      ]\n")
        
        sb.append("    }\n")
        sb.append("  ]\n")
        sb.append("}\n")

        return sb.toString()
    }

    /** Generates SARIF rules from findings. */
    private fun generateRules(findings: List<Finding>): List<String> {
        val uniqueRules = findings
            .groupBy { it.type }
            .map { (type, findingsOfType) ->
                val firstFinding = findingsOfType.first()
                createSarifRule(type, firstFinding)
            }
        return uniqueRules
    }

    /** Creates a SARIF rule definition. */
    private fun createSarifRule(ruleId: String, finding: Finding): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("              \"id\": \"${escapeJson(ruleId)}\",\n")
        sb.append("              \"name\": \"${escapeJson(ruleId)}\",\n")
        sb.append("              \"shortDescription\": {\n")
        sb.append("                \"text\": \"${escapeJson(finding.description)}\"\n")
        sb.append("              },\n")
        sb.append("              \"fullDescription\": {\n")
        sb.append("                \"text\": \"${escapeJson(finding.description)}\"\n")
        sb.append("              },\n")
        sb.append("              \"help\": {\n")
        sb.append("                \"text\": \"${escapeJson(generateHelp(finding))}\",\n")
        sb.append("                \"markdown\": \"${escapeJson(generateHelpMarkdown(finding))}\"\n")
        sb.append("              },\n")
        sb.append("              \"defaultConfiguration\": {\n")
        sb.append("                \"level\": \"${mapSeverityToSarifLevel(finding.severity)}\"\n")
        sb.append("              },\n")
        sb.append("              \"properties\": {\n")
        sb.append("                \"tags\": ${generateTags(finding)},\n")
        sb.append("                \"precision\": \"${determinePrecision(finding)}\",\n")
        sb.append("                \"security-severity\": \"${mapSeverityToScore(finding.severity)}\"\n")
        sb.append("              }\n")
        sb.append("            }")
        return sb.toString()
    }

    /** Creates a SARIF result entry for a finding. */
    private fun createSarifResult(finding: Finding, index: Int, repositoryUri: String?): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("          \"ruleId\": \"${escapeJson(finding.type)}\",\n")
        sb.append("          \"ruleIndex\": $index,\n")
        sb.append("          \"level\": \"${mapSeverityToSarifLevel(finding.severity)}\",\n")
        sb.append("          \"message\": {\n")
        sb.append("            \"text\": \"${escapeJson(finding.message)}\"\n")
        sb.append("          },\n")
        sb.append("          \"locations\": [\n")
        sb.append("            {\n")
        sb.append("              \"physicalLocation\": {\n")
        sb.append("                \"artifactLocation\": {\n")
        sb.append("                  \"uri\": \"${escapeJson(finding.file)}\",\n")
        if (repositoryUri != null) {
            sb.append("                  \"uriBaseId\": \"%SRCROOT%\",\n")
        }
        sb.append("                  \"index\": 0\n")
        sb.append("                },\n")
        sb.append("                \"region\": {\n")
        sb.append("                  \"startLine\": ${finding.lineNumber},\n")
        sb.append("                  \"startColumn\": ${finding.column},\n")
        sb.append("                  \"endLine\": ${finding.lineNumber},\n")
        sb.append("                  \"endColumn\": ${finding.column + finding.match.length},\n")
        sb.append("                  \"snippet\": {\n")
        sb.append("                    \"text\": \"${escapeJson(finding.lineContent.trim())}\"\n")
        sb.append("                  }\n")
        sb.append("                }\n")
        sb.append("              }\n")
        sb.append("            }\n")
        sb.append("          ],\n")
        sb.append("          \"partialFingerprints\": {\n")
        sb.append("            \"primaryLocationLineHash\": \"${generateFingerprint(finding)}\"\n")
        sb.append("          },\n")
        sb.append("          \"properties\": {\n")
        sb.append("            \"detectorType\": \"${escapeJson(finding.detectorType)}\",\n")
        sb.append("            \"confidence\": ${finding.confidence}\n")
        sb.append("          }\n")
        sb.append("        }")
        return sb.toString()
    }

    /** Maps SCAN severity to SARIF level. */
    private fun mapSeverityToSarifLevel(severity: Severity): String {
        return when (severity) {
            Severity.CRITICAL, Severity.HIGH -> "error"
            Severity.MEDIUM -> "warning"
            Severity.LOW, Severity.INFO -> "note"
        }
    }

    /** Maps SCAN severity to SARIF security-severity score (0.0-10.0). */
    private fun mapSeverityToScore(severity: Severity): String {
        return when (severity) {
            Severity.CRITICAL -> "9.0"
            Severity.HIGH -> "7.0"
            Severity.MEDIUM -> "5.0"
            Severity.LOW -> "3.0"
            Severity.INFO -> "1.0"
        }
    }

    /** Generates tags for a finding. */
    private fun generateTags(finding: Finding): String {
        val tags = mutableListOf("security", "secrets")
        
        when (finding.detectorType.lowercase()) {
            "pattern" -> tags.add("pattern-matching")
            "entropy" -> tags.add("entropy-analysis")
            "contextaware", "context-aware" -> tags.add("context-aware")
        }
        
        // Add severity tag
        tags.add("severity-${finding.severity.toString().lowercase()}")
        
        return "[${tags.joinToString(", ") { "\"$it\"" }}]"
    }

    /** Determines precision level for a finding. */
    private fun determinePrecision(finding: Finding): String {
        return when {
            finding.confidence >= 0.9 -> "very-high"
            finding.confidence >= 0.7 -> "high"
            finding.confidence >= 0.5 -> "medium"
            else -> "low"
        }
    }

    /** Generates help text for a finding. */
    private fun generateHelp(finding: Finding): String {
        return buildString {
            append(finding.description)
            append(" ")
            append("This type of secret should be stored securely and never committed to source code. ")
            append("Consider using environment variables or a secrets management service.")
        }
    }

    /** Generates help text in Markdown format. */
    private fun generateHelpMarkdown(finding: Finding): String {
        return buildString {
            append("## ${finding.type}\\n\\n")
            append("**Description:** ${finding.description}\\n\\n")
            append("**Severity:** ${finding.severity}\\n\\n")
            append("### Recommendations\\n\\n")
            append("- Remove this secret from source code immediately\\n")
            append("- Rotate the compromised credential\\n")
            append("- Use environment variables for sensitive data\\n")
            append("- Implement a secrets management solution (e.g., Azure Key Vault, AWS Secrets Manager, HashiCorp Vault)\\n")
            append("- Add pre-commit hooks to prevent future leaks\\n")
        }
    }

    /** Generates a fingerprint for deduplication. */
    private fun generateFingerprint(finding: Finding): String {
        val content = "${finding.file}:${finding.lineNumber}:${finding.type}"
        return content.hashCode().toString(16)
    }

    /** Escapes special characters for JSON. */
    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")
    }

    /** Writes the report to a file. */
    private fun writeReportToFile(content: String, filePath: String) {
        File(filePath).apply {
            parentFile?.mkdirs()
            writeText(content)
        }
    }
}
