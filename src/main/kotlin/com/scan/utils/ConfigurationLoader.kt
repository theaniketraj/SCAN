package com.scan.utils

import com.scan.core.ScanConfiguration
import com.scan.core.DetectorConfiguration
import com.scan.core.PatternConfiguration
import com.scan.core.ReportingConfiguration
import com.scan.core.PerformanceConfiguration
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.error.YAMLException

/**
 * Handles loading and merging of scan configurations from various sources.
 *
 * Configuration loading priority (highest to lowest):
 * 1. Task-specific configuration (programmatic)
 * 2. Project-level configuration file (scan-config.yml in project root)
 * 3. User-level configuration file (~/.scan/config.yml)
 * 4. Default configuration (embedded in plugin)
 */
class ConfigurationLoader(private val project: Project) {

    private val logger: Logger = Logging.getLogger(ConfigurationLoader::class.java)
    private val yaml = Yaml()

    companion object {
        private const val PROJECT_CONFIG_FILE = "scan-config.yml"
        private const val USER_CONFIG_DIR = ".scan"
        private const val USER_CONFIG_FILE = "config.yml"
        private const val DEFAULT_CONFIG_RESOURCE = "/patterns/default-patterns.yml"
        private const val IGNORE_FILE = ".scan-ignore"
    }

    /** Loads the complete configuration by merging all available sources. */
    fun loadConfiguration(): ScanConfiguration {
        logger.info("Loading scan configuration...")

        // Start with default configuration
        val defaultConfig = loadDefaultConfiguration()
        logger.debug("Loaded default configuration")

        // Load and merge user-level configuration
        val userConfig = loadUserConfiguration()
        val mergedUserConfig =
                if (userConfig != null) {
                    logger.debug("Merging user-level configuration")
                    mergeConfigurations(defaultConfig, userConfig)
                } else {
                    defaultConfig
                }

        // Load and merge project-level configuration
        val projectConfig = loadProjectConfiguration()
        val finalConfig =
                if (projectConfig != null) {
                    logger.debug("Merging project-level configuration")
                    mergeConfigurations(mergedUserConfig, projectConfig)
                } else {
                    mergedUserConfig
                }

        // Load ignore patterns
        val ignorePatterns = loadIgnorePatterns()

        // Convert to ScanConfiguration
        return convertToScanConfiguration(finalConfig, ignorePatterns)
    }

    /** Loads configuration from a specific file path. */
    fun loadConfigurationFromFile(configFile: File): ScanConfiguration {
        if (!configFile.exists()) {
            throw GradleException("Configuration file not found: ${configFile.absolutePath}")
        }

        return try {
            logger.info("Loading configuration from: ${configFile.absolutePath}")
            val configData = loadConfigurationData(FileInputStream(configFile))
            val ignorePatterns = loadIgnorePatterns()
            convertToScanConfiguration(configData, ignorePatterns)
        } catch (e: Exception) {
            throw GradleException(
                    "Failed to load configuration from ${configFile.absolutePath}: ${e.message}",
                    e
            )
        }
    }

    /** Loads the default configuration from embedded resources. */
    private fun loadDefaultConfiguration(): ConfigurationData {
        return try {
            val resourceStream =
                    javaClass.getResourceAsStream(DEFAULT_CONFIG_RESOURCE)
                            ?: throw GradleException(
                                    "Default configuration resource not found: $DEFAULT_CONFIG_RESOURCE"
                            )

            loadConfigurationData(resourceStream)
        } catch (e: Exception) {
            logger.warn("Failed to load default configuration, using fallback: ${e.message}")
            createFallbackConfiguration()
        }
    }

    /** Loads user-level configuration from ~/.scan/config.yml */
    private fun loadUserConfiguration(): ConfigurationData? {
        val userHome = System.getProperty("user.home")
        val configPath = Paths.get(userHome, USER_CONFIG_DIR, USER_CONFIG_FILE)

        return if (configPath.exists() && configPath.isRegularFile()) {
            try {
                logger.debug("Loading user configuration from: $configPath")
                loadConfigurationData(Files.newInputStream(configPath))
            } catch (e: Exception) {
                logger.warn("Failed to load user configuration from $configPath: ${e.message}")
                null
            }
        } else {
            logger.debug("No user configuration found at: $configPath")
            null
        }
    }

    /** Loads project-level configuration from scan-config.yml in project root. */
    private fun loadProjectConfiguration(): ConfigurationData? {
        val configFile = project.file(PROJECT_CONFIG_FILE)

        return if (configFile.exists()) {
            try {
                logger.debug("Loading project configuration from: ${configFile.absolutePath}")
                loadConfigurationData(FileInputStream(configFile))
            } catch (e: Exception) {
                logger.warn(
                        "Failed to load project configuration from ${configFile.absolutePath}: ${e.message}"
                )
                null
            }
        } else {
            logger.debug("No project configuration found at: ${configFile.absolutePath}")
            null
        }
    }

    /** Loads configuration data from an input stream. */
    private fun loadConfigurationData(inputStream: InputStream): ConfigurationData {
        return try {
            inputStream.use { stream ->
                yaml.load(stream) as? ConfigurationData
                        ?: throw GradleException("Invalid configuration format")
            }
        } catch (e: YAMLException) {
            throw GradleException("Invalid YAML configuration: ${e.message}", e)
        }
    }

    /** Loads ignore patterns from .scan-ignore file. */
    private fun loadIgnorePatterns(): List<String> {
        val ignoreFile = project.file(IGNORE_FILE)

        return if (ignoreFile.exists()) {
            try {
                logger.debug("Loading ignore patterns from: ${ignoreFile.absolutePath}")
                ignoreFile.readLines().map { it.trim() }.filter {
                    it.isNotEmpty() && !it.startsWith("#")
                }
            } catch (e: Exception) {
                logger.warn(
                        "Failed to load ignore patterns from ${ignoreFile.absolutePath}: ${e.message}"
                )
                emptyList()
            }
        } else {
            logger.debug("No ignore file found at: ${ignoreFile.absolutePath}")
            emptyList()
        }
    }

    /** Merges two configuration objects, with the override configuration taking precedence. */
    private fun mergeConfigurations(
            base: ConfigurationData,
            override: ConfigurationData
    ): ConfigurationData {
        return ConfigurationData(
                enabled = override.enabled ?: base.enabled,
                scanPaths = mergeLists(base.scanPaths, override.scanPaths),
                excludePaths = mergeLists(base.excludePaths, override.excludePaths),
                includeExtensions = mergeLists(base.includeExtensions, override.includeExtensions),
                excludeExtensions = mergeLists(base.excludeExtensions, override.excludeExtensions),
                patterns = mergePatterns(base.patterns, override.patterns),
                detectors = mergeDetectors(base.detectors, override.detectors),
                reporting = mergeReporting(base.reporting, override.reporting),
                performance = mergePerformance(base.performance, override.performance)
        )
    }

    private fun mergeLists(base: List<String>?, override: List<String>?): List<String>? {
        return when {
            override != null -> override
            base != null -> base
            else -> null
        }
    }

    private fun mergePatterns(base: PatternsConfig?, override: PatternsConfig?): PatternsConfig? {
        if (override == null) return base
        if (base == null) return override

        return PatternsConfig(
                secretPatterns = mergeLists(base.secretPatterns, override.secretPatterns),
                apiKeyPatterns = mergeLists(base.apiKeyPatterns, override.apiKeyPatterns),
                cryptoPatterns = mergeLists(base.cryptoPatterns, override.cryptoPatterns),
                databasePatterns = mergeLists(base.databasePatterns, override.databasePatterns),
                customPatterns = mergeLists(base.customPatterns, override.customPatterns)
        )
    }

    private fun mergeDetectors(
            base: DetectorsConfig?,
            override: DetectorsConfig?
    ): DetectorsConfig? {
        if (override == null) return base
        if (base == null) return override

        return DetectorsConfig(
                patternDetector =
                        mergeDetectorConfig(base.patternDetector, override.patternDetector),
                entropyDetector =
                        mergeDetectorConfig(base.entropyDetector, override.entropyDetector),
                contextAwareDetector =
                        mergeDetectorConfig(
                                base.contextAwareDetector,
                                override.contextAwareDetector
                        )
        )
    }

    private fun mergeDetectorConfig(
            base: DetectorConfig?,
            override: DetectorConfig?
    ): DetectorConfig? {
        if (override == null) return base
        if (base == null) return override

        return DetectorConfig(
                enabled = override.enabled ?: base.enabled,
                sensitivity = override.sensitivity ?: base.sensitivity,
                options = mergeOptions(base.options, override.options)
        )
    }

    private fun mergeOptions(
            base: Map<String, Any>?,
            override: Map<String, Any>?
    ): Map<String, Any>? {
        return when {
            override != null && base != null -> base + override
            override != null -> override
            base != null -> base
            else -> null
        }
    }

    private fun mergeReporting(
            base: ReportingConfig?,
            override: ReportingConfig?
    ): ReportingConfig? {
        if (override == null) return base
        if (base == null) return override

        return ReportingConfig(
                formats = mergeLists(base.formats, override.formats),
                outputPath = override.outputPath ?: base.outputPath,
                failOnSecrets = override.failOnSecrets ?: base.failOnSecrets,
                includeContext = override.includeContext ?: base.includeContext
        )
    }

    private fun mergePerformance(
            base: PerformanceConfig?,
            override: PerformanceConfig?
    ): PerformanceConfig? {
        if (override == null) return base
        if (base == null) return override

        return PerformanceConfig(
                maxFileSize = override.maxFileSize ?: base.maxFileSize,
                parallelScanning = override.parallelScanning ?: base.parallelScanning,
                threadCount = override.threadCount ?: base.threadCount
        )
    }

    /** Converts ConfigurationData to ScanConfiguration. */
    private fun convertToScanConfiguration(
            data: ConfigurationData,
            ignorePatterns: List<String>
    ): ScanConfiguration {
        return ScanConfiguration(
                enabled = data.enabled ?: true,
                scanPath = (data.scanPaths ?: listOf("src/main", "src/test")).firstOrNull() ?: ".",
                includePatterns = (data.excludePaths ?: emptyList()) + ignorePatterns,
                includedExtensions = (data.includeExtensions
                                ?: listOf("kt", "java", "properties", "yml", "yaml", "json", "xml")).toSet(),
                excludedExtensions = (data.excludeExtensions ?: listOf("class", "jar", "war", "ear")).toSet(),
                patterns = convertPatterns(data.patterns),
                detectors = convertDetectors(data.detectors),
                reporting = convertReporting(data.reporting),
                performance = convertPerformance(data.performance)
        )
    }

    private fun convertPatterns(patterns: Any?): PatternConfiguration {
        return PatternConfiguration()
    }
    }

    private fun convertDetectors(detectors: Any?): DetectorConfiguration {
        return DetectorConfiguration()
    }

    private fun convertDetectorConfig(config: Any?): DetectorConfiguration {
        return DetectorConfiguration()
    }

    private fun convertReporting(reporting: Any?): ReportingConfiguration {
        return ReportingConfiguration()
    }

    private fun convertPerformance(performance: Any?): PerformanceConfiguration {
        return PerformanceConfiguration()
    }

    /** Creates a fallback configuration when default resources are not available. */
    private fun createFallbackConfiguration(): ConfigurationData {
        return ConfigurationData(
                enabled = true,
                scanPaths = listOf("src/main", "src/test"),
                excludePaths = listOf("build", ".gradle", ".git"),
                includeExtensions =
                        listOf("kt", "java", "properties", "yml", "yaml", "json", "xml"),
                excludeExtensions = listOf("class", "jar", "war", "ear"),
                patterns =
                        PatternsConfig(
                                secretPatterns = getDefaultSecretPatterns(),
                                apiKeyPatterns = getDefaultApiKeyPatterns(),
                                cryptoPatterns = getDefaultCryptoPatterns(),
                                databasePatterns = getDefaultDatabasePatterns(),
                                customPatterns = emptyList()
                        ),
                detectors =
                        DetectorsConfig(
                                patternDetector =
                                        DetectorConfig(enabled = true, sensitivity = "medium"),
                                entropyDetector =
                                        DetectorConfig(enabled = true, sensitivity = "medium"),
                                contextAwareDetector =
                                        DetectorConfig(enabled = false, sensitivity = "low")
                        ),
                reporting =
                        ReportingConfig(
                                formats = listOf("console"),
                                outputPath = "build/reports/scan",
                                failOnSecrets = true,
                                includeContext = true
                        ),
                performance =
                        PerformanceConfig(
                                maxFileSize = 10 * 1024 * 1024,
                                parallelScanning = true,
                                threadCount = Runtime.getRuntime().availableProcessors()
                        )
        )
    }

    // Default pattern collections
    private fun getDefaultSecretPatterns(): List<String> =
            listOf(
                    "(?i)password\\s*[:=]\\s*[\"']?([^\\s\"']{8,})[\"']?",
                    "(?i)secret\\s*[:=]\\s*[\"']?([^\\s\"']{8,})[\"']?",
                    "(?i)token\\s*[:=]\\s*[\"']?([a-zA-Z0-9]{20,})[\"']?",
                    "(?i)private[_-]?key\\s*[:=]\\s*[\"']?([^\\s\"']{20,})[\"']?"
            )

    private fun getDefaultApiKeyPatterns(): List<String> =
            listOf(
                    "(?i)api[_-]?key\\s*[:=]\\s*[\"']?([a-zA-Z0-9]{20,})[\"']?",
                    "(?i)access[_-]?key\\s*[:=]\\s*[\"']?([a-zA-Z0-9]{20,})[\"']?",
                    "sk-[a-zA-Z0-9]{32,}",
                    "pk_[a-z]{4}_[a-zA-Z0-9]{24}"
            )

    private fun getDefaultCryptoPatterns(): List<String> =
            listOf(
                    "-----BEGIN (DSA|EC|OPENSSH|PGP|RSA) PRIVATE KEY-----",
                    "-----BEGIN PRIVATE KEY-----",
                    "-----BEGIN ENCRYPTED PRIVATE KEY-----"
            )

    private fun getDefaultDatabasePatterns(): List<String> =
            listOf(
                    "(?i)jdbc:[a-zA-Z0-9]+://[^\\s]+",
                    "(?i)mongodb://[^\\s]+",
                    "(?i)redis://[^\\s]+",
                    "(?i)postgresql://[^\\s]+"
            )
}

/** Data classes for YAML configuration parsing */
data class ConfigurationData(
        val enabled: Boolean? = null,
        val scanPaths: List<String>? = null,
        val excludePaths: List<String>? = null,
        val includeExtensions: List<String>? = null,
        val excludeExtensions: List<String>? = null,
        val patterns: PatternsConfig? = null,
        val detectors: DetectorsConfig? = null,
        val reporting: ReportingConfig? = null,
        val performance: PerformanceConfig? = null
)

data class PatternsConfig(
        val secretPatterns: List<String>? = null,
        val apiKeyPatterns: List<String>? = null,
        val cryptoPatterns: List<String>? = null,
        val databasePatterns: List<String>? = null,
        val customPatterns: List<String>? = null
)

data class DetectorsConfig(
        val patternDetector: DetectorConfig? = null,
        val entropyDetector: DetectorConfig? = null,
        val contextAwareDetector: DetectorConfig? = null
)

data class DetectorConfig(
        val enabled: Boolean? = null,
        val sensitivity: String? = null,
        val options: Map<String, Any>? = null
)

data class ReportingConfig(
        val formats: List<String>? = null,
        val outputPath: String? = null,
        val failOnSecrets: Boolean? = null,
        val includeContext: Boolean? = null
)

data class PerformanceConfig(
        val maxFileSize: Long? = null,
        val parallelScanning: Boolean? = null,
        val threadCount: Int? = null
)
