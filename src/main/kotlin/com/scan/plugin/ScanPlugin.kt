package com.scan.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.kotlin.dsl.*

/**
 * SCAN (Sensitive Code Analyzer for Nerds) Gradle Plugin
 *
 * This plugin provides automated scanning for sensitive information like API keys, database
 * credentials, cryptographic keys, and other secrets that shouldn't be committed to version
 * control.
 *
 * The plugin integrates seamlessly with the Gradle build lifecycle, ensuring that security scanning
 * becomes a natural part of the development workflow without disrupting productivity.
 */
class ScanPlugin : Plugin<Project> {

    companion object {
        /**
         * The plugin ID that developers use to apply this plugin. This constant ensures consistency
         * across all plugin references.
         */
        const val PLUGIN_ID = "io.github.theaniketraj.scan"

        /**
         * The name of the main scanning task that gets registered with Gradle. This is the task
         * name developers can use to run scanning manually.
         */
        const val SCAN_TASK_NAME = "scanForSecrets"

        /**
         * The name of the configuration extension that allows users to customize the plugin
         * behavior in their build.gradle.kts files.
         */
        const val EXTENSION_NAME = "scan"

        /**
         * Task group name for better organization in Gradle task listings. This helps developers
         * find security-related tasks easily.
         */
        const val TASK_GROUP = "security"
    }

    /**
     * This is the main entry point for the plugin. Gradle calls this method when someone applies
     * the plugin to their project with: plugins { id("io.github.theaniketraj.scan") }
     *
     * Think of this method as setting up all the infrastructure that makes your plugin work - like
     * a construction foreman organizing all the tools and workers before starting to build a house.
     */
    override fun apply(project: Project) {
        // Enable detailed logging for plugin development and debugging
        // This helps both plugin developers and users understand what's happening
        project.logger.info("Applying SCAN plugin to project: ${project.name}")

        // Create the configuration extension that allows users to customize
        // plugin behavior in their build.gradle.kts files
        val scanExtension = createScanExtension(project)

        // Register the main scanning task with Gradle's task system
        val scanTaskProvider = registerScanTask(project, scanExtension)

        // Wire the scanning task into the build lifecycle so it runs automatically
        // at the appropriate time during builds
        configureBuildLifecycleIntegration(project, scanTaskProvider)

        // Set up configuration validation to catch user errors early
        configureValidation(project, scanExtension)

        // Configure the plugin to work well with other common plugins
        configurePluginIntegration(project, scanTaskProvider)

        project.logger.info("SCAN plugin configuration completed successfully")
    }

    /**
     * Creates and configures the extension object that allows users to customize the plugin
     * behavior. This is what enables syntax like:
     *
     * scan {
     * ```
     *     strictMode = true
     *     ignoreTestFiles = false
     *     customPatterns = listOf("CUSTOM_API_.*")
     * ```
     * }
     */
    private fun createScanExtension(project: Project): ScanExtension {
        project.logger.debug("Creating SCAN extension for project configuration")

        return project.extensions.create(EXTENSION_NAME, ScanExtension::class.java).apply {
            // Set sensible defaults that work for most projects out of the box
            // Users can override these in their build.gradle.kts if needed

            // Default to balanced mode - catches most secrets without too many false positives
            strictMode.convention(false)

            // Include common source directories that typically contain code
            includePatterns.convention(
                setOf(
                    "src/**/*.kt",
                    "src/**/*.java",
                    "src/**/*.scala",
                    "src/**/*.groovy",
                    "src/**/*.properties",
                    "src/**/*.yml",
                    "src/**/*.yaml",
                    "src/**/*.json",
                    "src/**/*.xml",
                    "*.gradle",
                    "*.gradle.kts",
                    "gradle.properties"
                )
            )
            
            // Aliases for include/exclude patterns (empty by default)
            includeFiles.convention(emptySet())
            excludeFiles.convention(emptySet())

            // Exclude common directories that rarely contain secrets but often have false positives
            excludePatterns.convention(
                setOf(
                    "**/build/**",
                    "**/target/**",
                    "**/.gradle/**",
                    "**/.git/**",
                    "**/node_modules/**",
                    "**/*.class",
                    "**/*.jar",
                    "**/*.war"
                )
            )

            // By default, be more lenient with test files since they often contain dummy data
            ignoreTestFiles.convention(true)

            // Default behavior for different build scenarios
            failOnSecrets.convention(true)
            failOnFound.convention(true) // Alias for failOnSecrets
            warnOnSecrets.convention(true)
            
            // Basic configuration defaults
            enabled.convention(true)
            scanTests.convention(true)
            entropyThreshold.convention(4.5)
            contextAwareScanning.convention(true)
            verbose.convention(false)
            quiet.convention(false)

            // Reporting configuration
            generateHtmlReport.convention(false)
            generateJsonReport.convention(false)
            reportOutputDir.convention(project.layout.buildDirectory.dir("reports/scan"))

            // Performance tuning defaults
            maxFileSizeBytes.convention(10 * 1024 * 1024) // 10MB default limit
            parallelScanning.convention(true)

            project.logger.debug("SCAN extension created with default configuration")
        }
    }

    /**
     * Registers the main scanning task with Gradle. This creates the task that actually performs
     * the security scanning work. We use a TaskProvider here instead of creating the task
     * immediately - this is a Gradle best practice that improves build performance by only creating
     * tasks when they're needed.
     */
    private fun registerScanTask(
        project: Project,
        extension: ScanExtension
    ): TaskProvider<ScanTask> {
        project.logger.debug("Registering main scan task: $SCAN_TASK_NAME")

        return project.tasks.register(SCAN_TASK_NAME, ScanTask::class.java) { task ->
            // Set up task metadata that appears in 'gradle tasks' output
            task.group = TASK_GROUP
            task.description =
                "Scans the codebase for sensitive information like API keys, passwords, and tokens"

            // Connect the task to the user's configuration
            // This is how the task knows what the user wants to scan and how to behave
            task.scanConfiguration.set(extension)

            // Configure task inputs and outputs for Gradle's up-to-date checking
            // This is crucial for build performance - Gradle can skip this task if
            // nothing has changed since the last run
            task.setupTaskInputsAndOutputs(project, extension)

            project.logger.debug("Scan task registered successfully")
        }
    }

    /**
     * Configures how the scanning task integrates with Gradle's build lifecycle. This is where we
     * decide when the scanning should happen automatically during builds, and how it should behave
     * in different scenarios.
     */
    private fun configureBuildLifecycleIntegration(
        project: Project,
        scanTaskProvider: TaskProvider<ScanTask>
    ) {
        project.logger.debug("Configuring build lifecycle integration")

        // Wait for other plugins to be applied before setting up dependencies
        // This ensures we can properly integrate with whatever build setup the user has
        project.afterEvaluate {
            // Hook into the compilation process - we want to scan before compilation
            // so developers get feedback about secrets before their code is compiled
            project.tasks.withType(AbstractCompile::class.java).configureEach { compileTask ->
                // Make compilation depend on our security scan
                // This means 'gradle build' will automatically run security scanning
                compileTask.dependsOn(scanTaskProvider)

                project.logger.debug("Configured ${compileTask.name} to depend on security scanning")
            }

            // Special integration with the 'check' task if it exists
            // The 'check' task is Gradle's standard place for verification tasks
            project.tasks.findByName("check")?.let { checkTask ->
                checkTask.dependsOn(scanTaskProvider)
                project.logger.debug("Integrated SCAN with existing 'check' task")
            }

            // Configure behavior based on build environment
            configureBuildEnvironmentBehavior(project, scanTaskProvider)
        }
    }

    /**
     * Sets up different behaviors for different build environments. For example, you might want
     * stricter checking in CI/CD pipelines but more lenient behavior during local development.
     */
    private fun configureBuildEnvironmentBehavior(
        project: Project,
        scanTaskProvider: TaskProvider<ScanTask>
    ) {
        // Detect if we're running in a CI environment
        val isCI =
            System.getenv("CI")?.toBoolean() == true ||
                System.getenv("CONTINUOUS_INTEGRATION")?.toBoolean() == true ||
                System.getenv("BUILD_NUMBER") != null ||
                System.getenv("JENKINS_URL") != null ||
                System.getenv("GITHUB_ACTIONS")?.toBoolean() == true

        if (isCI) {
            project.logger.info("CI environment detected - enabling strict security scanning")

            // In CI, we want to be more aggressive about failing builds
            scanTaskProvider.configure { task ->
                // Make sure secrets always fail the build in CI
                task.scanConfiguration.get().failOnSecrets.set(true)
                // Enable detailed reporting for CI logs
                task.scanConfiguration.get().generateJsonReport.set(true)
            }
        } else {
            project.logger.debug("Local development environment - using balanced security scanning")
        }
    }

    /**
     * Sets up validation to catch configuration errors early and provide helpful error messages to
     * users who misconfigure the plugin.
     */
    private fun configureValidation(project: Project, extension: ScanExtension) {
        project.logger.debug("Setting up configuration validation")

        // Validate configuration after the build script has been evaluated
        project.afterEvaluate { validateConfiguration(extension, project) }
    }

    /**
     * Performs validation of the user's configuration and throws helpful error messages if they've
     * configured something incorrectly.
     */
    private fun validateConfiguration(extension: ScanExtension, project: Project) {
        // Validate file size limits
        val maxFileSize = extension.maxFileSizeBytes.get()
        if (maxFileSize <= 0) {
            throw IllegalArgumentException(
                "SCAN plugin configuration error: maxFileSizeBytes must be positive, got $maxFileSize"
            )
        }

        // Validate include patterns aren't empty
        val includePatterns = extension.includePatterns.get()
        if (includePatterns.isEmpty()) {
            project.logger.warn(
                "SCAN plugin warning: No include patterns specified. " +
                    "This means no files will be scanned. Consider adding patterns like 'src/**/*.kt'"
            )
        }

        // Validate output directory is reasonable
        val outputDir = extension.reportOutputDir.get().asFile
        if (!outputDir.parentFile.exists() && !outputDir.parentFile.mkdirs()) {
            project.logger.warn(
                "SCAN plugin warning: Cannot create report output directory: ${outputDir.absolutePath}"
            )
        }

        project.logger.debug("Configuration validation completed successfully")
    }

    /**
     * Configures integration with other common Gradle plugins to ensure our security scanning works
     * well in typical project setups.
     */
    private fun configurePluginIntegration(
        project: Project,
        scanTaskProvider: TaskProvider<ScanTask>
    ) {
        project.logger.debug("Configuring integration with other plugins")

        // Integration with Java plugin if present
        project.plugins.withType(JavaPlugin::class.java) {
            project.logger.debug("Java plugin detected - configuring Java-specific integration")

            // Make sure we scan before the 'classes' task compiles anything
            project.tasks.findByName("classes")?.dependsOn(scanTaskProvider)

            // Include Java-specific file patterns if user hasn't customized
            project.afterEvaluate {
                val extension = project.extensions.getByType(ScanExtension::class.java)
                val currentPatterns = extension.includePatterns.get().toMutableList()

                // Add Java-specific patterns if they're not already there
                val javaPatterns = listOf("src/**/*.java", "src/**/*.properties")
                javaPatterns.forEach { pattern ->
                    if (!currentPatterns.contains(pattern)) {
                        currentPatterns.add(pattern)
                    }
                }

                extension.includePatterns.set(currentPatterns)
            }
        }

        // Integration with Kotlin plugin if present
        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            project.logger.debug(
                "Kotlin JVM plugin detected - configuring Kotlin-specific integration"
            )

            project.afterEvaluate {
                // Hook into Kotlin compilation tasks
                project.tasks.findByName("compileKotlin")?.dependsOn(scanTaskProvider)
                project.tasks.findByName("compileTestKotlin")?.dependsOn(scanTaskProvider)
            }
        }

        // Integration with Android plugin if present
        project.plugins.withId("com.android.application") {
            project.logger.debug("Android application plugin detected")
            // Android-specific integration would go here
        }
    }

    /**
     * Configures Gradle's input/output tracking for the scan task. This is crucial for build
     * performance - it tells Gradle what files the task reads and what files it produces, so Gradle
     * can skip the task if nothing has changed.
     */
    private fun ScanTask.setupTaskInputsAndOutputs(project: Project, extension: ScanExtension) {
        // Configure inputs - these are the things that, if changed, require re-running the task
        inputs.property("strictMode", extension.strictMode)
        inputs.property("includePatterns", extension.includePatterns)
        inputs.property("excludePatterns", extension.excludePatterns)
        inputs.property("ignoreTestFiles", extension.ignoreTestFiles)
        inputs.property("customPatterns", extension.customPatterns)
        inputs.property("maxFileSizeBytes", extension.maxFileSizeBytes)

        // The actual source files are inputs too - if they change, we need to re-scan
        inputs.files(project.fileTree(project.projectDir))
            .withPropertyName("sourceFiles")

        // Configure outputs - these are the files the task produces
        if (extension.generateHtmlReport.get()) {
            outputs.file(extension.reportOutputDir.get().file("scan-report.html"))
        }
        if (extension.generateJsonReport.get()) {
            outputs.file(extension.reportOutputDir.get().file("scan-report.json"))
        }

        // Even if we don't generate file reports, we should have some output
        // to make Gradle's up-to-date checking work properly
        outputs.file(extension.reportOutputDir.get().file("scan-results.txt"))
            .withPropertyName("scanResults")
    }
}
