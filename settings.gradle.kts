plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "scan-gradle-plugin"

// Configure Gradle Enterprise if available
gradle.settingsEvaluated {
    if (settings.plugins.hasPlugin("com.gradle.enterprise")) {
        extensions.configure<com.gradle.enterprise.gradlebuild.GradleEnterpriseExtension> {
            buildScan {
                termsOfServiceUrl = "https://gradle.com/terms-of-service"
                termsOfServiceAgree = "yes"
                
                // Publish build scans for CI builds
                publishAlways()
                
                // Add custom tags
                tag("security-plugin")
                tag("kotlin")
                
                // Add custom values
                value("CI", System.getenv("CI") ?: "false")
                value("Build Number", System.getenv("BUILD_NUMBER") ?: "local")
                
                // Capture task input files for better caching
                capture.taskInputFiles = true
            }
        }
    }
}

// Configure dependency resolution strategy
dependencyResolutionManagement {
    // Use repository declarations from project build files
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    
    repositories {
        // Gradle Plugin Portal for plugin dependencies
        gradlePluginPortal()
        
        // Maven Central for library dependencies
        mavenCentral()
        
        // JCenter (legacy support, if needed)
        // gradlePluginPortal() already includes most plugins
        
        // Add custom repositories if needed
        // maven {
        //     name = "CustomRepo"
        //     url = uri("https://custom.maven.repo/releases")
        // }
    }
    
    // Configure version catalogs for dependency management
    versionCatalogs {
        create("libs") {
            // Kotlin versions
            version("kotlin", "1.9.20")
            version("kotlinx-coroutines", "1.7.3")
            
            // Jackson versions for JSON/YAML processing
            version("jackson", "2.15.3")
            version("snakeyaml", "2.2")
            
            // Testing versions
            version("junit", "5.10.1")
            version("mockk", "1.13.8")
            version("assertj", "3.24.2")
            version("truth", "1.1.5")
            
            // Utility library versions
            version("commons-lang", "3.13.0")
            version("commons-io", "2.14.0")
            version("commons-text", "1.11.0")
            version("jsoup", "1.16.2")
            
            // Logging versions
            version("slf4j", "2.0.9")
            
            // Plugin versions
            version("gradle-plugin-publish", "1.2.1")
            version("dokka", "1.9.10")
            
            // Define library aliases
            library("kotlin-stdlib", "org.jetbrains.kotlin", "kotlin-stdlib-jdk8").versionRef("kotlin")
            library("kotlin-reflect", "org.jetbrains.kotlin", "kotlin-reflect").versionRef("kotlin")
            library("kotlinx-coroutines-core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef("kotlinx-coroutines")
            
            // Jackson libraries
            library("jackson-databind", "com.fasterxml.jackson.core", "jackson-databind").versionRef("jackson")
            library("jackson-kotlin", "com.fasterxml.jackson.module", "jackson-module-kotlin").versionRef("jackson")
            library("jackson-yaml", "com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml").versionRef("jackson")
            library("snakeyaml", "org.yaml", "snakeyaml").versionRef("snakeyaml")
            
            // Testing libraries
            library("junit-api", "org.junit.jupiter", "junit-jupiter-api").versionRef("junit")
            library("junit-engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef("junit")
            library("junit-params", "org.junit.jupiter", "junit-jupiter-params").versionRef("junit")
            library("mockk", "io.mockk", "mockk").versionRef("mockk")
            library("assertj", "org.assertj", "assertj-core").versionRef("assertj")
            library("truth", "com.google.truth", "truth").versionRef("truth")
            
            // Utility libraries
            library("commons-lang", "org.apache.commons", "commons-lang3").versionRef("commons-lang")
            library("commons-io", "commons-io", "commons-io").versionRef("commons-io")
            library("commons-text", "org.apache.commons", "commons-text").versionRef("commons-text")
            library("jsoup", "org.jsoup", "jsoup").versionRef("jsoup")
            
            // Logging
            library("slf4j-api", "org.slf4j", "slf4j-api").versionRef("slf4j")
            
            // Define bundles for commonly used together dependencies
            bundle("jackson", listOf("jackson-databind", "jackson-kotlin", "jackson-yaml"))
            bundle("testing", listOf("junit-api", "junit-engine", "junit-params", "mockk", "assertj", "truth"))
            bundle("commons", listOf("commons-lang", "commons-io", "commons-text"))
        }
    }
}

// Configure build cache
buildCache {
    local {
        isEnabled = true
        directory = File(rootDir, "build-cache")
        removeUnusedEntriesAfterDays = 30
    }
    
    // Configure remote build cache if available
    val buildCacheUrl = System.getenv("GRADLE_BUILD_CACHE_URL")
    if (!buildCacheUrl.isNullOrEmpty()) {
        remote<HttpBuildCache> {
            url = uri(buildCacheUrl)
            isPush = System.getenv("CI")?.toBoolean() == true
            
            // Configure authentication if needed
            val buildCacheUser = System.getenv("GRADLE_BUILD_CACHE_USER")
            val buildCachePassword = System.getenv("GRADLE_BUILD_CACHE_PASSWORD")
            if (!buildCacheUser.isNullOrEmpty() && !buildCachePassword.isNullOrEmpty()) {
                credentials {
                    username = buildCacheUser
                    password = buildCachePassword
                }
            }
        }
    }
}

// Configure feature previews
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

// Include additional build logic if present
val additionalBuildDir = File(rootDir, "gradle/build-logic")
if (additionalBuildDir.exists()) {
    includeBuild(additionalBuildDir)
}

// Configure toolchain resolution
toolchainManagement {
    jvm {
        javaRepositories {
            repository("foojay") {
                resolverClass.set(org.gradle.toolchains.foojay.FoojayToolchainResolver::class.java)
            }
        }
    }
}

// Development mode configuration
val isDevelopmentMode = providers.gradleProperty("scan.development.mode")
    .orElse(providers.environmentVariable("SCAN_DEVELOPMENT_MODE"))
    .orElse("false")
    .get()
    .toBoolean()

if (isDevelopmentMode) {
    println("🔧 Development mode enabled")
    
    // Enable additional logging in development
    gradle.startParameter.logLevel = LogLevel.INFO
    
    // Enable build scan in development
    gradle.settingsEvaluated {
        if (settings.plugins.hasPlugin("com.gradle.enterprise")) {
            extensions.configure<com.gradle.enterprise.gradlebuild.GradleEnterpriseExtension> {
                buildScan {
                    tag("development")
                    publishAlwaysIf(true)
                }
            }
        }
    }
}

// Configure plugin management
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    
    // Configure plugin resolution strategy
    resolutionStrategy {
        eachPlugin {
            when (requested.id.namespace) {
                "org.jetbrains.kotlin" -> {
                    useVersion(libs.versions.kotlin.get())
                }
                "org.jetbrains.dokka" -> {
                    useVersion(libs.versions.dokka.get())
                }
            }
        }
    }
}

// Validate Gradle version
val minimumGradleVersion = "8.0"
val currentGradleVersion = gradle.gradleVersion

if (currentGradleVersion < minimumGradleVersion) {
    throw GradleException(
        """
        |This project requires Gradle $minimumGradleVersion or higher.
        |Current version: $currentGradleVersion
        |
        |Please update your Gradle wrapper:
        |  ./gradlew wrapper --gradle-version 8.4
        """.trimMargin()
    )
}

// Print build information
gradle.settingsEvaluated {
    println("""
        |
        |🔍 Scan Gradle Plugin Build Configuration
        |├── Root Project: ${rootProject.name}
        |├── Gradle Version: ${gradle.gradleVersion}
        |├── Java Version: ${System.getProperty("java.version")}
        |├── Kotlin Version: ${libs.versions.kotlin.get()}
        |├── Development Mode: $isDevelopmentMode
        |├── Build Cache: ${buildCache.local.isEnabled}
        |└── CI Environment: ${System.getenv("CI") ?: "false"}
        |
    """.trimMargin())
}