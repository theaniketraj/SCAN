package com.scan.plugin

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.file.DirectoryProperty

/**
 * Extension class for configuring the SCAN security scanning plugin
 */
abstract class ScanExtension {
    abstract val enabled: Property<Boolean>
    abstract val strictMode: Property<Boolean>
    abstract val failOnSecretsFound: Property<Boolean>
    abstract val failOnSecrets: Property<Boolean>
    abstract val failOnFound: Property<Boolean>  // Alias for failOnSecrets
    abstract val warnOnSecrets: Property<Boolean>
    abstract val verbose: Property<Boolean>
    abstract val quiet: Property<Boolean>
    abstract val includePatterns: SetProperty<String>
    abstract val excludePatterns: SetProperty<String>
    abstract val includeFiles: SetProperty<String>  // Alias for includePatterns
    abstract val excludeFiles: SetProperty<String>   // Alias for excludePatterns
    abstract val maxFileSizeBytes: Property<Long>
    abstract val ignoreTestFiles: Property<Boolean>
    abstract val scanTests: Property<Boolean>
    abstract val generateHtmlReport: Property<Boolean>
    abstract val generateJsonReport: Property<Boolean>
    abstract val reportOutputDir: DirectoryProperty
    abstract val reportPath: Property<String>
    abstract val reportFormats: SetProperty<String>
    abstract val outputFormat: Property<String>
    abstract val outputFile: Property<String>
    abstract val parallelScanning: Property<Boolean>
    abstract val entropyThreshold: Property<Double>
    abstract val customPatterns: MapProperty<String, String>
    abstract val contextAwareScanning: Property<Boolean>
    
    fun patterns(action: PatternConfig.() -> Unit) {
        val config = PatternConfig()
        action(config)
    }
    
    fun entropy(action: EntropyConfig.() -> Unit) {
        val config = EntropyConfig()
        action(config)
    }
    
    fun filter(action: FilterConfig.() -> Unit) {
        val config = FilterConfig()
        action(config)
    }
    
    fun reporting(action: ReportingConfig.() -> Unit) {
        val config = ReportingConfig()
        action(config)
    }

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
        var patternSensitivity: String = "medium"
    }

    class EntropyConfig {
        var threshold: Double = 4.5
        var minLength: Int = 12
        var maxLength: Int = 200
        var enableBase64Detection: Boolean = true
        var enableHexDetection: Boolean = true
        var skipCommonWords: Boolean = true
        var commonWordsFile: String? = null
    }

    class FilterConfig {
        var includeExtensions: MutableSet<String> = mutableSetOf()
        var excludeExtensions: MutableSet<String> = mutableSetOf()
        var includePaths: MutableSet<String> = mutableSetOf()
        var excludePaths: MutableSet<String> = mutableSetOf()
        var maxFileSize: Long = 10 * 1024 * 1024
        var skipBinaryFiles: Boolean = true
        var skipLargeFiles: Boolean = true
    }

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

    /**
     * Validates the extension configuration
     */
    fun validate() {
        // Entropy threshold validation
        val threshold = entropyThreshold.getOrElse(4.5)
        if (threshold < 0.0 || threshold > 10.0) {
            throw IllegalArgumentException("entropyThreshold must be between 0.0 and 10.0, got $threshold")
        }
    }
}
