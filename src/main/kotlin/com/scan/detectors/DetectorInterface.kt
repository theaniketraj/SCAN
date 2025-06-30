package com.scan.detectors

import com.scan.core.Finding
import com.scan.core.ScanContext
import com.scan.core.Severity
import java.util.regex.Pattern

/**
 * Common interface for all secret detection strategies.
 *
 * Implementations should be stateless and thread-safe to allow concurrent scanning. Each detector
 * focuses on a specific detection approach (patterns, entropy, context, etc.).
 */
interface DetectorInterface {

    /**
     * Unique identifier for this detector type. Used for configuration, reporting, and debugging.
     */
    val detectorId: String

    /** Human-readable name for this detector. */
    val detectorName: String

    /** Version of the detector implementation. Useful for tracking detection rule updates. */
    val version: String

    /**
     * Set of file extensions this detector is optimized for. Empty set means it works on all file
     * types.
     */
    val supportedFileTypes: Set<String>

    /**
     * Whether this detector should run on test files. Some detectors might want to skip test files
     * to reduce false positives.
     */
    val scanTestFiles: Boolean
        get() = true

    /**
     * Priority level for this detector (higher number = higher priority). Used for ordering
     * detector execution and conflict resolution.
     */
    val priority: Int
        get() = 100

    /**
     * Detects secrets and sensitive information in the provided scan context.
     *
     * @param context Contains file information, content, and configuration
     * @return List of findings, empty if no secrets detected
     * @throws DetectorException if detection fails critically
     */
    fun detect(context: ScanContext): List<Finding>

    /**
     * Validates detector configuration and readiness. Called once during detector initialization.
     *
     * @return ValidationResult indicating if detector is ready to use
     */
    fun validate(): ValidationResult = ValidationResult.success()

    /**
     * Gets detector-specific configuration schema. Useful for dynamic configuration validation and
     * UI generation.
     */
    fun getConfigurationSchema(): DetectorConfigSchema = DetectorConfigSchema.empty()

    /**
     * Provides statistics about detector performance and effectiveness. Called after scanning for
     * reporting and optimization.
     */
    fun getStatistics(): DetectorStatistics = DetectorStatistics()

    /** Resets internal state and statistics. Called before starting a new scan session. */
    fun reset() {
        // Default implementation - override if detector maintains state
    }

    /**
     * Checks if this detector supports the given file context. Allows detectors to skip files they
     * can't process effectively.
     */
    fun supports(context: ScanContext): Boolean {
        // Skip test files if detector doesn't support them
        if (context.isTestFile && !scanTestFiles) {
            return false
        }

        // Check file extension support
        if (supportedFileTypes.isNotEmpty() &&
                        context.fileExtension.lowercase() !in
                                supportedFileTypes.map { it.lowercase() }
        ) {
            return false
        }

        return true
    }
}

/**
 * Abstract base class providing common detector functionality. Implements shared patterns and
 * utilities that most detectors need.
 */
abstract class AbstractDetector : DetectorInterface {

    protected var scanCount = 0
    protected var findingCount = 0
    protected var totalScanTime = 0L
    protected val findingsByType = mutableMapOf<String, Int>()

    override fun detect(context: ScanContext): List<Finding> {
        if (!supports(context)) {
            return emptyList()
        }

        val startTime = System.currentTimeMillis()
        scanCount++

        return try {
            val findings = performDetection(context)

            // Update statistics
            findingCount += findings.size
            findings.groupBy { it.type }.forEach { (type, typeFindings) ->
                findingsByType[type] = findingsByType.getOrDefault(type, 0) + typeFindings.size
            }

            findings
        } catch (e: Exception) {
            throw DetectorException("Detection failed in ${detectorId}: ${e.message}", e)
        } finally {
            totalScanTime += System.currentTimeMillis() - startTime
        }
    }

    /**
     * Performs the actual detection logic. Subclasses implement their specific detection strategy
     * here.
     */
    protected abstract fun performDetection(context: ScanContext): List<Finding>

    override fun getStatistics(): DetectorStatistics {
        return DetectorStatistics(
                detectorId = detectorId,
                scanCount = scanCount,
                findingCount = findingCount,
                averageScanTime = if (scanCount > 0) totalScanTime.toDouble() / scanCount else 0.0,
                findingsByType = findingsByType.toMap()
        )
    }

    override fun reset() {
        scanCount = 0
        findingCount = 0
        totalScanTime = 0L
        findingsByType.clear()
    }

    /** Helper method to create findings with consistent metadata. */
    protected fun createFinding(
            type: String,
            value: String,
            lineNumber: Int,
            columnStart: Int,
            columnEnd: Int,
            confidence: Double,
            rule: String,
            context: ScanContext,
            severity: Severity = Severity.MEDIUM,
            contextText: String = ""
    ): Finding {
        return Finding(
                type = type,
                value = value,
                lineNumber = lineNumber,
                columnStart = columnStart,
                columnEnd = columnEnd,
                confidence = confidence.coerceIn(0.0, 1.0),
                rule = "${detectorId}:${rule}",
                context =
                        contextText.ifEmpty {
                            extractContext(context, lineNumber, columnStart, columnEnd)
                        },
                severity = severity
        )
    }

    /** Extracts surrounding context for a finding. */
    protected fun extractContext(
            scanContext: ScanContext,
            lineNumber: Int,
            columnStart: Int,
            columnEnd: Int,
            contextLines: Int = 1
    ): String {
        if (lineNumber <= 0 || lineNumber > scanContext.lines.size) {
            return ""
        }

        val startLine = maxOf(1, lineNumber - contextLines)
        val endLine = minOf(scanContext.lines.size, lineNumber + contextLines)

        return (startLine..endLine)
                .mapNotNull { lineNum ->
                    val line = scanContext.lines.getOrNull(lineNum - 1)
                    if (line != null) {
                        val marker = if (lineNum == lineNumber) ">>> " else "    "
                        "${marker}${lineNum}: ${line}"
                    } else null
                }
                .joinToString("\n")
    }

    /** Validates that a potential secret value meets minimum criteria. */
    protected fun isValidSecretValue(value: String, minLength: Int = 8): Boolean {
        return value.length >= minLength && value.isNotBlank() && !isCommonPlaceholder(value)
    }

    /** Checks if a value appears to be a placeholder or example. */
    protected fun isCommonPlaceholder(value: String): Boolean {
        val normalized = value.lowercase()
        val placeholders =
                setOf(
                        "example",
                        "test",
                        "demo",
                        "sample",
                        "placeholder",
                        "dummy",
                        "fake",
                        "mock",
                        "your_key",
                        "your_secret",
                        "insert_here",
                        "put_your_key_here",
                        "xxxxxxxx",
                        "12345678",
                        "abcdefgh",
                        "password",
                        "secret",
                        "token",
                        "key"
                )

        return placeholders.any { normalized.contains(it) } ||
                normalized.matches(Regex("^[x]+$")) ||
                normalized.matches(Regex("^[0-9]+$")) ||
                normalized.matches(Regex("^[a-z]+$"))
    }
}

/**
 * Specialized interface for pattern-based detectors. Provides additional methods for managing
 * detection patterns.
 */
interface PatternBasedDetector : DetectorInterface {

    /** Gets all active patterns used by this detector. */
    fun getPatterns(): List<DetectionPattern>

    /** Adds a custom pattern to the detector. */
    fun addPattern(pattern: DetectionPattern): Boolean

    /** Removes a pattern by its ID. */
    fun removePattern(patternId: String): Boolean

    /** Updates an existing pattern. */
    fun updatePattern(pattern: DetectionPattern): Boolean
}

/**
 * Specialized interface for entropy-based detectors. Provides methods for configuring entropy
 * thresholds and analysis.
 */
interface EntropyBasedDetector : DetectorInterface {

    /** Minimum entropy threshold for detection. */
    var entropyThreshold: Double

    /** Minimum string length to analyze for entropy. */
    var minStringLength: Int

    /** Maximum string length to analyze for entropy. */
    var maxStringLength: Int

    /** Calculates entropy for a given string. */
    fun calculateEntropy(text: String): Double
}

/**
 * Specialized interface for context-aware detectors. Understands code structure and can make
 * decisions based on context.
 */
interface ContextAwareDetector : DetectorInterface {

    /** Analyzes the code context around a potential finding. */
    fun analyzeContext(context: ScanContext, lineNumber: Int, columnStart: Int): ContextAnalysis

    /** Determines if a finding should be reported based on its context. */
    fun shouldReport(finding: Finding, contextAnalysis: ContextAnalysis): Boolean
}

/** Represents a detection pattern with metadata. */
data class DetectionPattern(
        val id: String,
        val name: String,
        val pattern: Pattern,
        val secretType: String,
        val confidence: Double,
        val severity: Severity = Severity.MEDIUM,
        val description: String = "",
        val examples: List<String> = emptyList(),
        val falsePositives: List<String> = emptyList(),
        val tags: Set<String> = emptySet(),
        val enabled: Boolean = true
) {
    /** Tests if the pattern matches the given text. */
    fun matches(text: String): Boolean = pattern.matcher(text).find()

    /** Finds all matches in the given text. */
    fun findMatches(text: String): List<PatternMatch> {
        val matcher = pattern.matcher(text)
        val matches = mutableListOf<PatternMatch>()

        while (matcher.find()) {
            matches.add(
                    PatternMatch(
                            value = matcher.group(),
                            start = matcher.start(),
                            end = matcher.end(),
                            groups = (1..matcher.groupCount()).map { matcher.group(it) }
                    )
            )
        }

        return matches
    }
}

/** Represents a pattern match result. */
data class PatternMatch(
        val value: String,
        val start: Int,
        val end: Int,
        val groups: List<String> = emptyList()
)

/** Result of context analysis for a potential finding. */
data class ContextAnalysis(
        val isInComment: Boolean = false,
        val isInString: Boolean = false,
        val isInTestCode: Boolean = false,
        val isInDocumentation: Boolean = false,
        val isVariableAssignment: Boolean = false,
        val variableName: String? = null,
        val functionContext: String? = null,
        val confidence: Double = 1.0
)

/** Validation result for detector configuration. */
data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList()
) {
    companion object {
        fun success() = ValidationResult(true)
        fun failure(errors: List<String>) = ValidationResult(false, errors)
        fun failure(error: String) = ValidationResult(false, listOf(error))
    }
}

/** Configuration schema for a detector. */
data class DetectorConfigSchema(
        val properties: Map<String, PropertySchema> = emptyMap(),
        val required: Set<String> = emptySet()
) {
    companion object {
        fun empty() = DetectorConfigSchema()
    }
}

/** Schema for a configuration property. */
data class PropertySchema(
        val type: PropertyType,
        val description: String,
        val defaultValue: Any? = null,
        val allowedValues: List<Any>? = null,
        val minValue: Number? = null,
        val maxValue: Number? = null
)

/** Types of configuration properties. */
enum class PropertyType {
    STRING,
    INTEGER,
    DOUBLE,
    BOOLEAN,
    LIST,
    MAP
}

/** Statistics about detector performance. */
data class DetectorStatistics(
        val detectorId: String = "",
        val scanCount: Int = 0,
        val findingCount: Int = 0,
        val averageScanTime: Double = 0.0,
        val findingsByType: Map<String, Int> = emptyMap(),
        val falsePositiveRate: Double = 0.0,
        val performanceMetrics: Map<String, Any> = emptyMap()
)

/** Exception thrown when detector encounters a critical error. */
class DetectorException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Factory interface for creating detectors with configuration. */
interface DetectorFactory {

    /** Creates a detector instance with the given configuration. */
    fun createDetector(config: Map<String, Any>): DetectorInterface

    /** Gets the detector type this factory creates. */
    fun getDetectorType(): String

    /** Gets the configuration schema for this detector type. */
    fun getConfigurationSchema(): DetectorConfigSchema
}

/** Registry for managing detector factories and instances. */
object DetectorRegistry {

    private val factories = mutableMapOf<String, DetectorFactory>()

    /** Registers a detector factory. */
    fun registerFactory(factory: DetectorFactory) {
        factories[factory.getDetectorType()] = factory
    }

    /** Creates a detector of the specified type with configuration. */
    fun createDetector(type: String, config: Map<String, Any> = emptyMap()): DetectorInterface? {
        return factories[type]?.createDetector(config)
    }

    /** Gets all registered detector types. */
    fun getAvailableTypes(): Set<String> = factories.keys.toSet()

    /** Gets configuration schema for a detector type. */
    fun getConfigurationSchema(type: String): DetectorConfigSchema? {
        return factories[type]?.getConfigurationSchema()
    }
}
