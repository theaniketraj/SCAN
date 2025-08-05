package com.scan.plugin

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty

/**
 * Extension class for configuring the SCAN security scanning plugin
 * 
 * This extension allows users to configure security scanning behavior
 * for detecting secrets, API keys, and other sensitive information
 * in their build.gradle files.
 */
abstract class ScanExtension {
    
    // =====================================
    // CORE CONFIGURATION
    // =====================================
    
    /**
     * Enable or disable the SCAN plugin entirely
     * Default: true
     */
    abstract val enabled: Property<Boolean>
    
    /**
     * Enable strict mode for more thorough scanning
     * Default: false
     */
    abstract val strictMode: Property<Boolean>
    
    /**
     * Fail the build if secrets are detected
     * Default: true
     */
    abstract val failOnSecretsFound: Property<Boolean>
    
    /**
     * Fail on secrets (alternative naming)
     * Default: true
     */
    abstract val failOnSecrets: Property<Boolean>
    
    /**
     * Warn on secrets instead of failing
     * Default: true
     */
    abstract val warnOnSecrets: Property<Boolean>
    
    /**
     * Enable verbose logging for debugging
     * Default: false
     */
    abstract val verbose: Property<Boolean>
    
    /**
     * Enable quiet mode (minimal output)
     * Default: false
     */
    abstract val quiet: Property<Boolean>
    
    // =====================================
    // FILE SCANNING CONFIGURATION
    // =====================================
    
    /**
     * File patterns to include in scanning
     * Default: ["**\/*.kt", "**\/*.java", "**\/*.xml", "**\/*.properties", "**\/*.yml", "**\/*.yaml", "**\/*.json", "**\/*.gradle", "**\/*.gradle.kts"]
     */
    abstract val includePatterns: SetProperty<String>
    
    /**
     * File patterns to exclude from scanning
     * Default: ["**\/build\/**", "**\/.*\/**", "**\/.git\/**", "**\/node_modules\/**", "**\/target\/**"]
     */
    abstract val excludePatterns: SetProperty<String>
    
    /**
     * Maximum file size to scan in bytes (files larger than this will be skipped)
     * Default: 10MB (10 * 1024 * 1024)
     */
    abstract val maxFileSizeBytes: Property<Long>
    
    /**
     * Enable scanning of test files
     * Default: false (test files usually contain dummy secrets)
     */
    abstract val scanTestFiles: Property<Boolean>
    
    /**
     * Ignore test files (opposite of scanTestFiles)
     * Default: true
     */
    abstract val ignoreTestFiles: Property<Boolean>
    
    /**
     * Test file patterns to identify test files
     * Default: ["**\/test\/**", "**\/androidTest\/**", "**\/*Test.*", "**\/*Tests.*", "**\/*Spec.*"]
     */
    abstract val testFilePatterns: SetProperty<String>
    
    // =====================================
    // DETECTOR CONFIGURATION
    // =====================================
    
    /**
     * Enable pattern-based detection (regex patterns)
     * Default: true
     */
    abstract val enablePatternDetector: Property<Boolean>
    
    /**
     * Enable entropy-based detection (high-entropy strings)
     * Default: true
     */
    abstract val enableEntropyDetector: Property<Boolean>
    
    /**
     * Enable context-aware detection (considers code context)
     * Default: true
     */
    abstract val enableContextAwareDetector: Property<Boolean>
    
    /**
     * Minimum entropy threshold for entropy detector (0.0 - 8.0)
     * Higher values = fewer false positives, but might miss some secrets
     * Default: 4.5
     */
    abstract val entropyThreshold: Property<Double>
    
    /**
     * Minimum string length for entropy analysis
     * Default: 12
     */
    abstract val minEntropyLength: Property<Int>
    
    /**
     * Maximum string length for entropy analysis (performance optimization)
     * Default: 200
     */
    abstract val maxEntropyLength: Property<Int>
    
    // =====================================
    // PATTERN CONFIGURATION
    // =====================================
    
    /**
     * Enable built-in API key patterns
     * Default: true
     */
    abstract val enableApiKeyPatterns: Property<Boolean>
    
    /**
     * Enable built-in database connection patterns
     * Default: true
     */
    abstract val enableDatabasePatterns: Property<Boolean>
    
    /**
     * Enable built-in cryptographic key patterns
     * Default: true
     */
    abstract val enableCryptoPatterns: Property<Boolean>
    
    /**
     * Custom pattern configuration file path (YAML format)
     * Default: null (uses built-in patterns only)
     */
    abstract val customPatternsFile: Property<String>
    
    /**
     * Additional custom regex patterns to detect
     * Key: pattern name, Value: regex pattern
     */
    abstract val customPatterns: MapProperty<String, String>
    
    /**
     * Patterns to whitelist/ignore (won't be flagged as secrets)
     * These are regex patterns that match strings to ignore
     */
    abstract val whitelistPatterns: SetProperty<String>
    
    // =====================================
    // CONTEXT AWARENESS SETTINGS
    // =====================================
    
    /**
     * Skip secrets found in comments
     * Default: true (comments often contain example/dummy secrets)
     */
    abstract val skipComments: Property<Boolean>
    
    /**
     * Skip secrets found in string literals that appear to be examples
     * Default: true
     */
    abstract val skipExampleStrings: Property<Boolean>
    
    /**
     * Keywords that indicate example/dummy content (case-insensitive)
     * Default: ["example", "dummy", "test", "sample", "placeholder", "fake", "mock"]
     */
    abstract val exampleKeywords: SetProperty<String>
    
    /**
     * Variable names that typically contain non-sensitive values
     * Default: ["example", "sample", "test", "dummy", "placeholder", "default"]
     */
    abstract val safeVariableNames: SetProperty<String>
    
    // =====================================
    // REPORTING CONFIGURATION
    // =====================================
    
    /**
     * Output directory for scan reports
     * Default: build/reports/scan
     */
    abstract val reportOutputDir: Property<String>
    
    /**
     * Enable console reporting
     * Default: true
     */
    abstract val enableConsoleReport: Property<Boolean>
    
    /**
     * Enable JSON report generation
     * Default: true
     */
    abstract val enableJsonReport: Property<Boolean>
    
    /**
     * Generate JSON report (alternative naming)
     * Default: false
     */
    abstract val generateJsonReport: Property<Boolean>
    
    /**
     * Enable HTML report generation
     * Default: false
     */
    abstract val enableHtmlReport: Property<Boolean>
    
    /**
     * Generate HTML report (alternative naming)
     * Default: false
     */
    abstract val generateHtmlReport: Property<Boolean>
    
    /**
     * JSON report filename
     * Default: scan-results.json
     */
    abstract val jsonReportFileName: Property<String>
    
    /**
     * HTML report filename
     * Default: scan-results.html
     */
    abstract val htmlReportFileName: Property<String>
    
    /**
     * Include source code snippets in reports (security consideration)
     * Default: false (to avoid exposing secrets in reports)
     */
    abstract val includeSourceInReports: Property<Boolean>
    
    /**
     * Mask detected secrets in reports (show only partial content)
     * Default: true
     */
    abstract val maskSecretsInReports: Property<Boolean>
    
    // =====================================
    // PERFORMANCE CONFIGURATION
    // =====================================
    
    /**
     * Enable parallel scanning for better performance
     * Default: true
     */
    abstract val enableParallelScanning: Property<Boolean>
    
    /**
     * Enable parallel scanning (alternative naming)
     * Default: true
     */
    abstract val parallelScanning: Property<Boolean>
    
    /**
     * Number of threads for parallel scanning
     * Default: Runtime.getRuntime().availableProcessors()
     */
    abstract val scanThreadCount: Property<Int>
    
    /**
     * Enable result caching to speed up incremental builds
     * Default: true
     */
    abstract val enableCaching: Property<Boolean>
    
    /**
     * Cache directory for scan results
     * Default: build/tmp/scan-cache
     */
    abstract val cacheDirectory: Property<String>
    
    /**
     * Scan timeout in minutes (0 = no timeout)
     * Default: 10
     */
    abstract val timeoutMinutes: Property<Int>
    
    // =====================================
    // IGNORE/EXCLUSION CONFIGURATION
    // =====================================
    
    /**
     * Path to .scan-ignore file (similar to .gitignore)
     * Default: .scan-ignore
     */
    abstract val ignoreFile: Property<String>
    
    /**
     * Known false positive strings to ignore
     * These are exact string matches that will be ignored
     */
    abstract val ignoredSecrets: SetProperty<String>
    
    /**
     * File paths to completely ignore (exact matches)
     */
    abstract val ignoredFiles: SetProperty<String>
    
    /**
     * Enable automatic ignore suggestions for potential false positives
     * Default: true
     */
    abstract val suggestIgnores: Property<Boolean>
    
    // =====================================
    // BASELINE AND COMPARISON
    // =====================================
    
    /**
     * Generate baseline file for future comparisons
     * Default: false
     */
    abstract val generateBaseline: Property<Boolean>
    
    /**
     * Baseline file path for comparison
     * Default: null
     */
    abstract val baselineFile: Property<String>
    
    /**
     * Only report new secrets (compared to baseline)
     * Default: false
     */
    abstract val onlyReportNew: Property<Boolean>
    
    // =====================================
    // CI/CD INTEGRATION
    // =====================================
    
    /**
     * Enable CI/CD integration features
     * Default: true
     */
    abstract val enableCiIntegration: Property<Boolean>
    
    /**
     * Exit code when secrets are found (for CI/CD systems)
     * Default: 1
     */
    abstract val secretsFoundExitCode: Property<Int>
    
    /**
     * Enable GitHub/GitLab annotations in CI environments
     * Default: true
     */
    abstract val enableCiAnnotations: Property<Boolean>
    
    // =====================================
    // CONFIGURATION DSL BLOCKS
    // =====================================
    
    /**
     * Configure pattern detection settings
     */
    fun patterns(action: PatternConfig.() -> Unit) {
        val config = PatternConfig()
        action(config)
        // Apply pattern configuration
    }
    
    /**
     * Configure entropy detection settings
     */
    fun entropy(action: EntropyConfig.() -> Unit) {
        val config = EntropyConfig()
        action(config)
        // Apply entropy configuration
    }
    
    /**
     * Configure filtering settings
     */
    fun filters(action: FilterConfig.() -> Unit) {
        val config = FilterConfig()
        action(config)
        // Apply filter configuration
    }
    
    /**
     * Configure reporting settings
     */
    fun reporting(action: ReportingConfig.() -> Unit) {
        val config = ReportingConfig()
        action(config)
        // Apply reporting configuration
    }
}

// =====================================
// CONFIGURATION CLASSES
// =====================================

/**
 * Pattern detection configuration
 */
class PatternConfig {
    var enableApiKeys: Boolean = true
    var enableAwsKeys: Boolean = true
    var enableGoogleKeys: Boolean = true
    var enableGitHubTokens: Boolean = true
    var enableSlackTokens: Boolean = true
    var enableDatabaseUrls: Boolean = true
    var enableJwtTokens: Boolean = true
    var enableRsaKeys: Boolean = true
    var enableSshKeys: Boolean = true
    var customPatterns: MutableMap<String, String> = mutableMapOf()
    var patternSensitivity: String = "medium" // low, medium, high
}

/**
 * Entropy detection configuration
 */
class EntropyConfig {
    var threshold: Double = 4.5
    var minLength: Int = 12
    var maxLength: Int = 200
    var enableBase64Detection: Boolean = true
    var enableHexDetection: Boolean = true
    var skipCommonWords: Boolean = true
    var commonWordsFile: String? = null
}

/**
 * Filter configuration
 */
class FilterConfig {
    var includeExtensions: MutableSet<String> = mutableSetOf()
    var excludeExtensions: MutableSet<String> = mutableSetOf()
    var includePaths: MutableSet<String> = mutableSetOf()
    var excludePaths: MutableSet<String> = mutableSetOf()
    var maxFileSize: Long = 10 * 1024 * 1024 // 10MB
    var skipBinaryFiles: Boolean = true
    var skipLargeFiles: Boolean = true
}

}

/**
 * Reporting configuration
 */
class ReportingConfig {
    var outputDir: String = "build/reports/scan"
    var formats: MutableSet<String> = mutableSetOf("console", "json")
    var includeMetadata: Boolean = true
    var includeStatistics: Boolean = true
    var groupByFileType: Boolean = false
    var sortByRisk: Boolean = true
    var maskSecrets: Boolean = true
    var maxSecretLength: Int = 50
}