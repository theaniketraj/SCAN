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

/** Individual security finding */
data class Finding(
        val id: String,
        val title: String,
        val description: String,
        val severity: Severity,
        val confidence: Confidence,
        val detectorType: String,
        val detectorName: String,
        val location: FindingLocation,
        val secretInfo: SecretInfo,
        val context: FindingContext,
        val remediation: RemediationInfo,
        val metadata: Map<String, Any> = emptyMap()
) {
    /** Check if this finding should block the build */
    fun shouldBlockBuild(blockingThreshold: Severity): Boolean {
        return severity.ordinal <= blockingThreshold.ordinal
    }

    /** Get a short description for display */
    fun getShortDescription(): String {
        return if (description.length > 100) {
            "${description.take(97)}..."
        } else {
            description
        }
    }

    /** Get the detected secret value (masked for security) */
    fun getMaskedSecret(): String {
        val secret = secretInfo.detectedValue
        return when {
            secret.length <= 4 -> "*".repeat(secret.length)
            secret.length <= 8 ->
                    "${secret.take(2)}${"*".repeat(secret.length - 4)}${secret.takeLast(2)}"
            else -> "${secret.take(4)}${"*".repeat(secret.length - 8)}${secret.takeLast(4)}"
        }
    }
}

/** Location information for a finding */
data class FindingLocation(
        val filePath: String,
        val relativePath: String,
        val lineNumber: Int,
        val columnStart: Int,
        val columnEnd: Int,
        val lineContent: String
) {
    /** Get the file name without path */
    fun getFileName(): String = File(filePath).name

    /** Get directory path */
    fun getDirectoryPath(): String = File(filePath).parent ?: ""

    /** Get position as string (line:column) */
    fun getPosition(): String = "$lineNumber:$columnStart"

    /** Get the highlighted line content with the secret marked */
    fun getHighlightedContent(): String {
        val before = lineContent.substring(0, columnStart.coerceAtMost(lineContent.length))
        val secret =
                lineContent.substring(
                        columnStart.coerceAtMost(lineContent.length),
                        columnEnd.coerceAtMost(lineContent.length)
                )
        val after = lineContent.substring(columnEnd.coerceAtMost(lineContent.length))
        return "$before>>>$secret<<<$after"
    }
}

/** Information about the detected secret */
data class SecretInfo(
        val detectedValue: String,
        val patternName: String,
        val patternDescription: String,
        val secretType: SecretType,
        val entropyScore: Double? = null,
        val possibleSource: String? = null,
        val validationStatus: ValidationStatus = ValidationStatus.NOT_VALIDATED
) {
    /** Check if this secret has high entropy */
    fun hasHighEntropy(): Boolean = entropyScore?.let { it > 4.0 } ?: false

    /** Get the length of the detected secret */
    fun getSecretLength(): Int = detectedValue.length
}

/** Context information around the finding */
data class FindingContext(
        val surroundingLines: List<String>,
        val isInComment: Boolean,
        val isInString: Boolean,
        val isInTestFile: Boolean,
        val isInConfigFile: Boolean,
        val functionName: String? = null,
        val className: String? = null,
        val variableName: String? = null
) {
    /** Check if finding is in a risky context */
    fun isRiskyContext(): Boolean {
        return !isInComment && !isInTestFile && (isInString || isInConfigFile)
    }

    /** Get context description */
    fun getContextDescription(): String {
        val contexts = mutableListOf<String>()
        if (isInComment) contexts.add("comment")
        if (isInString) contexts.add("string literal")
        if (isInTestFile) contexts.add("test file")
        if (isInConfigFile) contexts.add("configuration file")
        functionName?.let { contexts.add("function '$it'") }
        className?.let { contexts.add("class '$it'") }
        variableName?.let { contexts.add("variable '$it'") }

        return when {
            contexts.isEmpty() -> "code"
            contexts.size == 1 -> contexts.first()
            else -> contexts.dropLast(1).joinToString(", ") + " and " + contexts.last()
        }
    }
}

/** Remediation guidance for the finding */
data class RemediationInfo(
        val recommendation: String,
        val actionItems: List<String>,
        val references: List<String> = emptyList(),
        val automatedFix: AutomatedFix? = null
) {
    /** Check if automated fix is available */
    fun hasAutomatedFix(): Boolean = automatedFix != null
}

/** Automated fix information */
data class AutomatedFix(
        val description: String,
        val replacement: String,
        val requiresManualReview: Boolean = true
)

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

/** Severity levels for findings */
enum class Severity(val displayName: String, val numericValue: Int) {
    CRITICAL("Critical", 5),
    HIGH("High", 4),
    MEDIUM("Medium", 3),
    LOW("Low", 2),
    INFO("Info", 1);

    companion object {
        fun fromString(value: String): Severity? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
    }
}

/** Confidence levels for findings */
enum class Confidence(val displayName: String, val description: String) {
    HIGH("High", "Very likely to be a real secret"),
    MEDIUM("Medium", "Probably a secret, but may be false positive"),
    LOW("Low", "Possibly a secret, likely needs manual review");

    companion object {
        fun fromString(value: String): Confidence? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
    }
}

/** Types of secrets that can be detected */
enum class SecretType(val displayName: String, val description: String) {
    API_KEY("API Key", "Application Programming Interface authentication key"),
    ACCESS_TOKEN("Access Token", "OAuth or similar access token"),
    SECRET_KEY("Secret Key", "Generic secret key or password"),
    PRIVATE_KEY("Private Key", "Cryptographic private key"),
    DATABASE_URL("Database URL", "Database connection string"),
    DATABASE_PASSWORD("Database Password", "Database authentication password"),
    CERTIFICATE("Certificate", "Digital certificate or key material"),
    ENCRYPTION_KEY("Encryption Key", "Symmetric encryption key"),
    WEBHOOK_URL("Webhook URL", "Webhook endpoint with embedded secrets"),
    CLOUD_CREDENTIALS("Cloud Credentials", "Cloud service credentials"),
    UNKNOWN("Unknown", "Unclassified secret type");

    companion object {
        fun fromString(value: String): SecretType? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
    }
}

/** Validation status for secrets */
enum class ValidationStatus(val displayName: String) {
    VALID("Valid - Secret is active"),
    INVALID("Invalid - Secret is not active"),
    REVOKED("Revoked - Secret has been revoked"),
    EXPIRED("Expired - Secret has expired"),
    NOT_VALIDATED("Not Validated - Validation not performed"),
    VALIDATION_FAILED("Validation Failed - Could not validate");

    companion object {
        fun fromString(value: String): ValidationStatus? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
    }
}

/** Types of scan errors */
enum class ErrorType(val displayName: String) {
    FILE_READ_ERROR("File Read Error"),
    PATTERN_COMPILATION_ERROR("Pattern Compilation Error"),
    CONFIGURATION_ERROR("Configuration Error"),
    PERMISSION_ERROR("Permission Error"),
    MEMORY_ERROR("Memory Error"),
    TIMEOUT_ERROR("Timeout Error"),
    UNKNOWN_ERROR("Unknown Error");

    companion object {
        fun fromString(value: String): ErrorType? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
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
