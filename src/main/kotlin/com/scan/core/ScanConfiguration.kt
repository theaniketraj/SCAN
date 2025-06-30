package com.scan.core

import java.io.File
import java.util.regex.Pattern

/**
 * Main configuration class for the SCAN plugin
 * Supports hierarchical configuration loading and validation
 */
data class ScanConfiguration(
    // Core scanning settings
    val enabled: Boolean = true,
    val scanPath: String = ".",
    val configSource: String = "default",
    
    // File filtering
    val includePatterns: List<String> = DEFAULT_INCLUDE_PATTERNS,
    val excludePatterns: List<String> = DEFAULT_EXCLUDE_PATTERNS,
    val includedExtensions: Set<String> = DEFAULT_INCLUDED_EXTENSIONS,
    val excludedExtensions: Set<String> = DEFAULT_EXCLUDED_EXTENSIONS,
    val maxFileSize: Long = DEFAULT_MAX_FILE_SIZE,
    val followSymlinks: Boolean = false,
    
    // Detection settings
    val detectors: DetectorConfiguration = DetectorConfiguration(),
    val patterns: PatternConfiguration = PatternConfiguration(),
    val entropy: EntropyConfiguration = EntropyConfiguration(),
    val contextAnalysis: ContextConfiguration = ContextConfiguration(),
    
    // Filtering and exclusions
    val filters: FilterConfiguration = FilterConfiguration(),
    val whitelist: WhitelistConfiguration = WhitelistConfiguration(),
    
    // Reporting settings
    val reporting: ReportingConfiguration = ReportingConfiguration(),
    
    // Performance settings
    val performance: PerformanceConfiguration = PerformanceConfiguration(),
    
    // Validation settings
    val validation: ValidationConfiguration = ValidationConfiguration(),
    
    // Build integration
    val buildIntegration: BuildIntegrationConfiguration = BuildIntegrationConfiguration()
) {
    
    companion object {
        // Default file patterns
        val DEFAULT_INCLUDE_PATTERNS = listOf("**/*")
        val DEFAULT_EXCLUDE_PATTERNS = listOf(
            "**/.*",
            "**/node_modules/**",
            "**/build/**",
            "**/target/**",
            "**/dist/**",
            "**/out/**",
            "**/.git/**",
            "**/.gradle/**",
            "**/.idea/**",
            "**/.vscode/**",
            "**/bin/**",
            "**/obj/**"
        )
        
        // Default file extensions
        val DEFAULT_INCLUDED_EXTENSIONS = setOf(
            // Source code
            "kt", "kts", "java", "scala", "groovy",
            "js", "ts", "jsx", "tsx", "vue", "svelte",
            "py", "rb", "php", "go", "rs", "cpp", "c", "h", "hpp",
            "cs", "vb", "fs", "swift", "m", "mm",
            // Configuration files
            "yml", "yaml", "json", "xml", "toml", "ini", "cfg", "conf",
            "properties", "env", "config",
            // Web files
            "html", "htm", "css", "scss", "sass", "less",
            // Scripts
            "sh", "bash", "zsh", "fish", "ps1", "bat", "cmd",
            // Documentation
            "md", "txt", "rst", "adoc",
            // Data files
            "csv", "tsv", "sql"
        )
        
        val DEFAULT_EXCLUDED_EXTENSIONS = setOf(
            // Binary files
            "exe", "dll", "so", "dylib", "lib", "a", "o", "obj",
            "jar", "war", "ear", "class", "pyc", "pyo",
            // Images
            "png", "jpg", "jpeg", "gif", "bmp", "tiff", "svg", "ico",
            "webp", "heic", "raw",
            // Videos
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v",
            // Audio
            "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a",
            // Archives
            "zip", "tar", "gz", "bz2", "xz", "7z", "rar", "deb", "rpm",
            // Fonts
            "ttf", "otf", "woff", "woff2", "eot",
            // Other binary
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx"
        )
        
        const val DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024L // 10MB
        const val DEFAULT_MAX_LINE_LENGTH = 10000
        const val DEFAULT_CONTEXT_LINES = 3
        const val DEFAULT_ENTROPY_THRESHOLD = 4.0
        const val DEFAULT_MIN_SECRET_LENGTH = 8
        const val DEFAULT_MAX_SECRET_LENGTH = 256
    }
    
    /**
     * Validate the configuration and return any validation errors
     */
    fun validate(): List<ConfigurationError> {
        val errors = mutableListOf<ConfigurationError>()
        
        // Validate scan path
        if (scanPath.isBlank()) {
            errors.add(ConfigurationError("scanPath cannot be empty"))
        }
        
        // Validate file size limits
        if (maxFileSize <= 0) {
            errors.add(ConfigurationError("maxFileSize must be positive"))
        }
        
        // Validate detector configuration
        errors.addAll(detectors.validate())
        errors.addAll(patterns.validate())
        errors.addAll(entropy.validate())
        errors.addAll(reporting.validate())
        errors.addAll(performance.validate())
        errors.addAll(validation.validate())
        
        return errors
    }
    
    /**
     * Check if configuration is valid
     */
    fun isValid(): Boolean = validate().isEmpty()
    
    /**
     * Get effective include patterns (combines global and specific patterns)
     */
    fun getEffectiveIncludePatterns(): List<Pattern> {
        return includePatterns.map { Pattern.compile(convertGlobToRegex(it)) }
    }
    
    /**
     * Get effective exclude patterns
     */
    fun getEffectiveExcludePatterns(): List<Pattern> {
        return excludePatterns.map { Pattern.compile(convertGlobToRegex(it)) }
    }
    
    /**
     * Check if a file should be scanned based on configuration
     */
    fun shouldScanFile(file: File): Boolean {
        val path = file.path
        val extension = file.extension.lowercase()
        
        // Check file size
        if (file.length() > maxFileSize) return false
        
        // Check excluded extensions
        if (extension in excludedExtensions) return false
        
        // Check included extensions (if specified)
        if (includedExtensions.isNotEmpty() && extension !in includedExtensions) return false
        
        // Check exclude patterns
        if (getEffectiveExcludePatterns().any { it.matcher(path).matches() }) return false
        
        // Check include patterns
        if (includePatterns.isNotEmpty()) {
            return getEffectiveIncludePatterns().any { it.matcher(path).matches() }
        }
        
        return true
    }
    
    /**
     * Merge this configuration with another, with the other taking precedence
     */
    fun mergeWith(other: ScanConfiguration): ScanConfiguration {
        return copy(
            enabled = other.enabled,
            scanPath = if (other.scanPath != ".") other.scanPath else this.scanPath,
            configSource = "${this.configSource} + ${other.configSource}",
            includePatterns = if (other.includePatterns != DEFAULT_INCLUDE_PATTERNS) other.includePatterns else this.includePatterns,
            excludePatterns = this.excludePatterns + other.excludePatterns,
            includedExtensions = if (other.includedExtensions != DEFAULT_INCLUDED_EXTENSIONS) other.includedExtensions else this.includedExtensions,
            excludedExtensions = this.excludedExtensions + other.excludedExtensions,
            maxFileSize = if (other.maxFileSize != DEFAULT_MAX_FILE_SIZE) other.maxFileSize else this.maxFileSize,
            followSymlinks = other.followSymlinks,
            detectors = this.detectors.mergeWith(other.detectors),
            patterns = this.patterns.mergeWith(other.patterns),
            entropy = this.entropy.mergeWith(other.entropy),
            contextAnalysis = this.contextAnalysis.mergeWith(other.contextAnalysis),
            filters = this.filters.mergeWith(other.filters),
            whitelist = this.whitelist.mergeWith(other.whitelist),
            reporting = this.reporting.mergeWith(other.reporting),
            performance = this.performance.mergeWith(other.performance),
            validation = this.validation.mergeWith(other.validation),
            buildIntegration = this.buildIntegration.mergeWith(other.buildIntegration)
        )
    }
    
    private fun convertGlobToRegex(glob: String): String {
        val regex = StringBuilder()
        var i = 0
        while (i < glob.length) {
            when (val c = glob[i]) {
                '*' -> {
                    if (i + 1 < glob.length && glob[i + 1] == '*') {
                        regex.append(".*")
                        i++ // Skip next *
                    } else {
                        regex.append("[^/]*")
                    }
                }
                '?' -> regex.append("[^/]")
                '[' -> {
                    val j = glob.indexOf(']', i)
                    if (j != -1) {
                        regex.append(glob.substring(i, j + 1))
                        i = j
                    } else {
                        regex.append("\\[")
                    }
                }
                '\\' -> {
                    regex.append("\\\\")
                }
                else -> {
                    if (c in ".^$+{}()|") {
                        regex.append("\\")
                    }
                    regex.append(c)
                }
            }
            i++
        }
        return regex.toString()
    }
}

/**
 * Detector-specific configuration
 */
data class DetectorConfiguration(
    val enabledDetectors: Set<String> = setOf("pattern", "entropy", "context"),
    val disabledDetectors: Set<String> = emptySet(),
    val detectorSettings: Map<String, Map<String, Any>> = emptyMap(),
    val customDetectors: List<CustomDetectorConfiguration> = emptyList()
) {
    fun validate(): List<ConfigurationError> {
        val errors = mutableListOf<ConfigurationError>()
        
        if (enabledDetectors.isEmpty()) {
            errors.add(ConfigurationError("At least one detector must be enabled"))
        }
        
        // Validate custom detectors
        customDetectors.forEach { detector ->
            errors.addAll(detector.validate())
        }
        
        return errors
    }
    
    fun mergeWith(other: DetectorConfiguration): DetectorConfiguration {
        return copy(
            enabledDetectors = if (other.enabledDetectors != setOf("pattern", "entropy", "context")) other.enabledDetectors else this.enabledDetectors,
            disabledDetectors = this.disabledDetectors + other.disabledDetectors,
            detectorSettings = this.detectorSettings + other.detectorSettings,
            customDetectors = this.customDetectors + other.customDetectors
        )
    }
}

/**
 * Pattern detection configuration
 */
data class PatternConfiguration(
    val enableDefaultPatterns: Boolean = true,
    val customPatternFiles: List<String> = emptyList(),
    val customPatterns: List<CustomPattern> = emptyList(),
    val patternSets: Set<String> = setOf("default", "api-keys", "crypto", "database"),
    val caseSensitive: Boolean = false,
    val multiline: Boolean = true,
    val maxMatches: Int = 1000
) {
    fun validate(): List<ConfigurationError> {
        val errors = mutableListOf<ConfigurationError>()
        
        if (maxMatches <= 0) {
            errors.add(ConfigurationError("maxMatches must be positive"))
        }
        
        // Validate custom patterns
        customPatterns.forEach { pattern ->
            errors.addAll(pattern.validate())
        }
        
        return errors
    }
    
    fun mergeWith(other: PatternConfiguration): PatternConfiguration {
        return copy(
            enableDefaultPatterns = other.enableDefaultPatterns,
            customPatternFiles = this.customPatternFiles + other.customPatternFiles,
            customPatterns = this.customPatterns + other.customPatterns,
            patternSets = if (other.patternSets != setOf("default", "api-keys", "crypto", "database")) other.patternSets else this.patternSets,
            caseSensitive = other.caseSensitive,
            multiline = other.multiline,
            maxMatches = if (other.maxMatches != 1000) other.maxMatches else this.maxMatches
        )
    }
}

/**
 * Entropy detection configuration
 */
data class EntropyConfiguration(
    val enabled: Boolean = true,
    val threshold: Double = DEFAULT_ENTROPY_THRESHOLD,
    val minLength: Int = DEFAULT_MIN_SECRET_LENGTH,
    val maxLength: Int = DEFAULT_MAX_SECRET_LENGTH,
    val charset: String = "base64", // base64, hex, alphanumeric, ascii
    val contextRequired: Boolean = true,
    val skipCommonWords: Boolean = true,
    val skipKnownHashes: Boolean = true
) {
    fun validate(): List<ConfigurationError> {
        val errors = mutableListOf<ConfigurationError>()
        
        if (threshold <= 0.0 || threshold > 8.0) {
            errors.add(ConfigurationError("entropy threshold must be between 0.0 and 8.0"))
        }
        
        if (minLength <= 0) {
            errors.add(ConfigurationError("minLength must be positive"))
        }
        
        if (maxLength <= minLength) {
            errors.add(ConfigurationError("maxLength must be greater than minLength"))
        }
        
        if (charset !in setOf("base64", "hex", "alphanumeric", "ascii")) {
            errors.add(ConfigurationError("charset must be one of: base64, hex, alphanumeric, ascii"))
        }
        
        return errors
    }
    
    fun mergeWith(other: EntropyConfiguration): EntropyConfiguration {
        return copy(
            enabled = other.enabled,
            threshold = if (other.threshold != DEFAULT_ENTROPY_THRESHOLD) other.threshold else this.threshold,
            minLength = if (other.minLength != DEFAULT_MIN_SECRET_LENGTH) other.minLength else this.minLength,
            maxLength = if (other.maxLength != DEFAULT_MAX_SECRET_LENGTH) other.maxLength else this.maxLength,
            charset = if (other.charset != "base64") other.charset else this.charset,
            contextRequired = other.contextRequired,
            skipCommonWords = other.skipCommonWords,
            skipKnownHashes = other.skipKnownHashes
        )
    }
}

/**
 * Context analysis configuration
 */
data class ContextConfiguration(
    val enabled: Boolean = true,
    val analyzeComments: Boolean = true,
    val analyzeStrings: Boolean = true,
    val analyzeVariableNames: Boolean = true,
    val analyzeFunctionNames: Boolean = true,
    val contextLines: Int = DEFAULT_CONTEXT_LINES,
    val weightByContext: Boolean = true,
    val testFileHandling: TestFileHandling = TestFileHandling.REDUCED_SEVERITY
) {
    fun mergeWith(other: ContextConfiguration): ContextConfiguration {
        return copy(
            enabled = other.enabled,
            analyzeComments = other.analyzeComments,
            analyzeStrings = other.analyzeStrings,
            analyzeVariableNames = other.analyzeVariableNames,
            analyzeFunctionNames = other.analyzeFunctionNames,
            contextLines = if (other.contextLines != DEFAULT_CONTEXT_LINES) other.contextLines else this.contextLines,
            weightByContext = other.weightByContext,
            testFileHandling = other.testFileHandling
        )
    }
}

/**
 * Filter configuration
 */
data class FilterConfiguration(
    val enabledFilters: Set<String> = setOf("extension", "path", "whitelist", "test"),
    val maxLineLength: Int = DEFAULT_MAX_LINE_LENGTH,
    val skipBinaryFiles: Boolean = true,
    val skipEmptyFiles: Boolean = true,
    val skipGeneratedFiles: Boolean = true,
    val generatedFileMarkers: List<String> = listOf(
        "// Generated by",
        "/* Generated by",
        "# Generated by",
        "# This file was automatically generated",
        "@generated"
    )
) {
    fun mergeWith(other: FilterConfiguration): FilterConfiguration {
        return copy(
            enabledFilters = if (other.enabledFilters != setOf("extension", "path", "whitelist", "test")) other.enabledFilters else this.enabledFilters,
            maxLineLength = if (other.maxLineLength != DEFAULT_MAX_LINE_LENGTH) other.maxLineLength else this.maxLineLength,
            skipBinaryFiles = other.skipBinaryFiles,
            skipEmptyFiles = other.skipEmptyFiles,
            skipGeneratedFiles = other.skipGeneratedFiles,
            generatedFileMarkers = if (other.generatedFileMarkers != this.generatedFileMarkers) other.generatedFileMarkers else this.generatedFileMarkers
        )
    }
}

/**
 * Whitelist configuration for known false positives
 */
data class WhitelistConfiguration(
    val whitelistFiles: List<String> = listOf(".scanignore", ".scan-whitelist.yml"),
    val globalExclusions: List<String> = DEFAULT_GLOBAL_EXCLUSIONS,
    val hashExclusions: Set<String> = emptySet(),
    val pathExclusions: List<String> = emptyList(),
    val patternExclusions: List<String> = emptyList(),
    val contextExclusions: List<ContextExclusion> = emptyList()
) {
    companion object {
        val DEFAULT_GLOBAL_EXCLUSIONS = listOf(
            "example",
            "test",
            "demo",
            "placeholder",
            "dummy",
            "fake",
            "mock",
            "sample",
            "XXXXXXXX",
            "YOUR_API_KEY",
            "INSERT_KEY_HERE",
            "REPLACE_ME",
            "TODO",
            "FIXME"
        )
    }
    
    fun mergeWith(other: WhitelistConfiguration): WhitelistConfiguration {
        return copy(
            whitelistFiles = this.whitelistFiles + other.whitelistFiles,
            globalExclusions = this.globalExclusions + other.globalExclusions,
            hashExclusions = this.hashExclusions + other.hashExclusions,
            pathExclusions = this.pathExclusions + other.pathExclusions,
            patternExclusions = this.patternExclusions + other.patternExclusions,
            contextExclusions = this.contextExclusions + other.contextExclusions
        )
    }
}

/**
 * Reporting configuration
 */
data class ReportingConfiguration(
    val enabled: Boolean = true,
    val formats: Set<ReportFormat> = setOf(ReportFormat.CONSOLE),
    val outputPath: String = "scan-results",
    val verbosity: Verbosity = Verbosity.NORMAL,
    val showContext: Boolean = true,
    val showRemediation: Boolean = true,
    val groupByFile: Boolean = false,
    val includeMetrics: Boolean = true,
    val console: ConsoleReportConfiguration = ConsoleReportConfiguration(),
    val json: JsonReportConfiguration = JsonReportConfiguration(),
    val html: HtmlReportConfiguration = HtmlReportConfiguration()
) {
    fun validate(): List<ConfigurationError> {
        val errors = mutableListOf<ConfigurationError>()
        
        if (formats.isEmpty()) {
            errors.add(ConfigurationError("At least one report format must be enabled"))
        }
        
        if (outputPath.isBlank()) {
            errors.add(ConfigurationError("outputPath cannot be empty"))
        }
        
        return errors
    }
    
    fun mergeWith(other: ReportingConfiguration): ReportingConfiguration {
        return copy(
            enabled = other.enabled,
            formats = if (other.formats != setOf(ReportFormat.CONSOLE)) other.formats else this.formats,
            outputPath = if (other.outputPath != "scan-results") other.outputPath else this.outputPath,
            verbosity = other.verbosity,
            showContext = other.showContext,
            showRemediation = other.showRemediation,
            groupByFile = other.groupByFile,
            includeMetrics = other.includeMetrics,
            console = this.console.mergeWith(other.console),
            json = this.json.mergeWith(other.json),
            html = this.html.mergeWith(other.html)
        )
    }
}

/**
 * Performance configuration
 */
data class PerformanceConfiguration(
    val maxConcurrency: Int = Runtime.getRuntime().availableProcessors(),
    val timeoutSeconds: Int = 300,
    val memoryLimitMB: Int = 1024,
    val enableProgressReporting: Boolean = true,
    val batchSize: Int = 50,
    val enableCaching: Boolean = true,
    val cacheDirectory: String = ".scan-cache"
) {
    fun validate(): List<ConfigurationError> {
        val errors = mutableListOf<ConfigurationError>()
        
        if (maxConcurrency <= 0) {
            errors.add(ConfigurationError("maxConcurrency must be positive"))
        }
        
        if (timeoutSeconds <= 0) {
            errors.add(ConfigurationError("timeoutSeconds must be positive"))
        }
        
        if (memoryLimitMB <= 0) {
            errors.add(ConfigurationError("memoryLimitMB must be positive"))
        }
        
        if (batchSize <= 0) {
            errors.add(ConfigurationError("batchSize must be positive"))
        }
        
        return errors
    }
    
    fun mergeWith(other: PerformanceConfiguration): PerformanceConfiguration {
        return copy(
            maxConcurrency = if (other.maxConcurrency != Runtime.getRuntime().availableProcessors()) other.maxConcurrency else this.maxConcurrency,
            timeoutSeconds = if (other.timeoutSeconds != 300) other.timeoutSeconds else this.timeoutSeconds,
            memoryLimitMB = if (other.memoryLimitMB != 1024) other.memoryLimitMB else this.memoryLimitMB,
            enableProgressReporting = other.enableProgressReporting,
            batchSize = if (other.batchSize != 50) other.batchSize else this.batchSize,
            enableCaching = other.enableCaching,
            cacheDirectory = if (other.cacheDirectory != ".scan-cache") other.cacheDirectory else this.cacheDirectory
        )
    }
}

/**
 * Secret validation configuration
 */
data class ValidationConfiguration(
    val enabled: Boolean = false,
    val validateApiKeys: Boolean = false,
    val validateTokens: Boolean = false,
    val timeoutSeconds: Int = 10,
    val maxConcurrentValidations: Int = 5,
    val cacheValidationResults: Boolean = true,
    val cacheTtlHours: Int = 24,
    val providers: Map<String, ValidationProviderConfiguration> = emptyMap()
) {
    fun validate(): List<ConfigurationError> {
        val errors = mutableListOf<ConfigurationError>()
        
        if (timeoutSeconds <= 0) {
            errors.add(ConfigurationError("validation timeoutSeconds must be positive"))
        }
        
        if (maxConcurrentValidations <= 0) {
            errors.add(ConfigurationError("maxConcurrentValidations must be positive"))
        }
        
        if (cacheTtlHours <= 0) {
            errors.add(ConfigurationError("cacheTtlHours must be positive"))
        }
        
        return errors
    }
    
    fun mergeWith(other: ValidationConfiguration): ValidationConfiguration {
        return copy(
            enabled = other.enabled,
            validateApiKeys = other.validateApiKeys,
            validateTokens = other.validateTokens,
            timeoutSeconds = if (other.timeoutSeconds != 10) other.timeoutSeconds else this.timeoutSeconds,
            maxConcurrentValidations = if (other.maxConcurrentValidations != 5) other.maxConcurrentValidations else this.maxConcurrentValidations,
            cacheValidationResults = other.cacheValidationResults,
            cacheTtlHours = if (other.cacheTtlHours != 24) other.cacheTtlHours else this.cacheTtlHours,
            providers = this.providers + other.providers
        )
    }
}

/**
 * Build integration configuration
 */
data class BuildIntegrationConfiguration(
    val failOnFindings: Boolean = true,
    val failureThreshold: Severity = Severity.HIGH,
    val maxFindings: Int = 0, // 0 = unlimited
    val skipOnBranches: List<String> = emptyList(),
    val requireApprovalForFindings: Boolean = false,
    val generateReports: Boolean = true,
    val uploadReports: Boolean = false,
    val reportUploadUrl: String? = null
) {
    fun mergeWith(other: BuildIntegrationConfiguration): BuildIntegrationConfiguration {
        return copy(
            failOnFindings = other.failOnFindings,
            failureThreshold = other.failureThreshold,
            maxFindings = if (other.maxFindings != 0) other.maxFindings else this.maxFindings,
            skipOnBranches = this.skipOnBranches + other.skipOnBranches,
            requireApprovalForFindings = other.requireApprovalForFindings,
            generateReports = other.generateReports,
            uploadReports = other.uploadReports,
            reportUploadUrl = other.reportUploadUrl ?: this.reportUploadUrl
        )
    }
}

// Supporting data classes and enums

data class CustomDetectorConfiguration(
    val name: String,
    val className: String,
    val enabled: Boolean = true,
    val settings: Map<String, Any> = emptyMap()
) {
    fun validate(): List<ConfigurationError> {
        val errors = mutableListOf<ConfigurationError>()
        
        if (name.isBlank()) {
            errors.add(ConfigurationError("Custom detector name cannot be empty"))
        }
        
        if (className.isBlank()) {
            errors.add(ConfigurationError("Custom detector className cannot be empty"))
        }
        
        return errors
    }
}

data class CustomPattern(
    val name: String,
    val pattern: String,
    val description: String = "",
    val severity: Severity = Severity.MEDIUM,
    val confidence: Confidence = Confidence.MEDIUM,
    val secretType: SecretType = SecretType.UNKNOWN,
    val enabled: Boolean = true
) {
    fun validate(): List<ConfigurationError> {
        val errors = mutableListOf<ConfigurationError>()
        
        if (name.isBlank()) {
            errors.add(ConfigurationError("Custom pattern name cannot be empty"))
        }
        
        if (pattern.isBlank()) {
            errors.add(ConfigurationError("Custom pattern regex cannot be empty"))
        }
        
        try {
            Pattern.compile(pattern)
        } catch (e: Exception) {
            errors.add(ConfigurationError("Invalid regex pattern '$pattern': ${e.message}"))
        }
        
        return errors
    }
}

data class ContextExclusion(
    val path: String? = null,
    val function: String? = null,
    val variable: String? = null,
    val pattern: String? = null,
    val reason: String = ""
)

data class ConsoleReportConfiguration(
    val colors: Boolean = true,
    val showProgress: Boolean = true,
    val showSummary: Boolean = true,
    val maxWidth: Int = 120
) {
    fun mergeWith(other: ConsoleReportConfiguration): ConsoleReportConfiguration {
        return copy(
            colors = other.colors,
            showProgress = other.showProgress,
            showSummary = other.showSummary,
            maxWidth = if (other.maxWidth != 120) other.maxWidth else this.maxWidth
        )
    }
}

data class JsonReportConfiguration(
    val prettyPrint: Boolean = true,
    val includeRawData: Boolean = false,
    val includeContext: Boolean = true
) {
    fun mergeWith(other: JsonReportConfiguration): JsonReportConfiguration {
        return copy(
            prettyPrint = other.prettyPrint,
            includeRawData = other.includeRawData,
            includeContext = other.includeContext
        )
    }
}

data class HtmlReportConfiguration(
    val template: String = "default",
    val includeCharts: Boolean = true,
    val includeDetails: Boolean = true,
    val theme: String = "light"
) {
    fun mergeWith(other: HtmlReportConfiguration): HtmlReportConfiguration {
        return copy(
            template = if (other.template != "default") other.template else this.template,
            includeCharts = other.includeCharts,
            includeDetails = other.includeDetails,
            theme = if (other.theme != "light") other.theme else this.theme
        )
    }
}

data class ValidationProviderConfiguration(
    val enabled: Boolean = true,
    val apiUrl: String,
    val apiKey: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val timeout: Int = 10
)

// Enums

enum class TestFileHandling {
    NORMAL,           // Treat test files normally
    REDUCED_SEVERITY, // Reduce severity of findings in test files
    SKIP              // Skip test files entirely
}

enum class ReportFormat {
    CONSOLE, JSON, HTML, XML, SARIF
}

enum class Verbosity {
    QUIET, NORMAL, VERBOSE, DEBUG
}

/**
 * Configuration error
 */
data class ConfigurationError(
    val message: String,
    val field: String? = null,
    val value: Any? = null
) {
    override fun toString(): String {
        return buildString {
            append("Configuration Error: ")
            append(message)
            field?.let { append(" (field: $it)") }
            value?.let { append(" (value: $it)") }
        }
    }
}

/**
 * Configuration builder for easy construction
 */
class ScanConfigurationBuilder {
    private var config = ScanConfiguration()
    
    fun enabled(enabled: Boolean) = apply { config = config.copy(enabled = enabled) }
    fun scanPath(path: String) = apply { config = config.copy(scanPath = path) }
    fun includePatterns(patterns: List<String>) = apply { config = config.copy(includePatterns = patterns) }
    fun excludePatterns(patterns: List<String>) = apply { config = config.copy(excludePatterns = patterns) }
    fun includedExtensions(extensions: Set<String>) = apply { config = config.copy(includedExtensions = extensions) }
    fun excludedExtensions(extensions: Set<String>) = apply { config = config.copy(excludedExtensions = extensions) }
    fun maxFileSize(size: Long) = apply { config = config.copy(maxFileSize = size) }
    fun followSymlinks(follow: Boolean) = apply { config = config.copy(followSymlinks = follow) }
    fun detectors(detectors: DetectorConfiguration) = apply { config = config.copy(detectors = detectors) }
    fun patterns(patterns: PatternConfiguration) = apply { config = config.copy(patterns = patterns) }
    fun entropy(entropy: EntropyConfiguration) = apply { config = config.copy(entropy = entropy) }
    fun contextAnalysis(context: ContextConfiguration) = apply { config = config.copy(contextAnalysis = context) }
    fun filters(filters: FilterConfiguration) = apply { config = config.copy(filters = filters) }
    fun whitelist(whitelist: WhitelistConfiguration) = apply { config = config.copy(whitelist = whitelist) }
    fun reporting(reporting: ReportingConfiguration) = apply { config = config.copy(reporting = reporting) }
    fun performance(performance: PerformanceConfiguration) = apply { config = config.copy(performance = performance) }
    fun validation(validation: ValidationConfiguration) = apply { config = config.copy(validation = validation) }
    fun buildIntegration(buildIntegration: BuildIntegrationConfiguration) = apply { config = config.copy(buildIntegration = buildIntegration) }
    fun build(): ScanConfiguration {
        return config
    }
    fun reset() {
        config = ScanConfiguration()
    }
    fun from(existing: ScanConfiguration) = apply {
        config = existing
    }
    fun from(other: ScanConfigurationBuilder) = apply {
        config = other.build()
    }
    fun mergeWith(other: ScanConfigurationBuilder): ScanConfigurationBuilder {
        return from(config.mergeWith(other.build()))
    }
    fun mergeWith(other: ScanConfiguration): ScanConfigurationBuilder {
        return from(config.mergeWith(other))
    }
    fun validate(): List<ConfigurationError> {
        return config.validate()
    }
    fun isValid(): Boolean {
        return config.isValid()
    }
    fun getEffectiveIncludePatterns(): List<Pattern> {
        return config.getEffectiveIncludePatterns()
    }
    fun getEffectiveExcludePatterns(): List<Pattern> {
        return config.getEffectiveExcludePatterns()
    }
    