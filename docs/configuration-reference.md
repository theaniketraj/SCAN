# SCAN Plugin Configuration Reference

This document provides a comprehensive reference for all configuration options available in the SCAN Gradle Plugin.

## Table of Contents

1. [Configuration Overview](#configuration-overview)
2. [Basic Settings](#basic-settings)
3. [File Pattern Configuration](#file-pattern-configuration)
4. [Detection Settings](#detection-settings)
5. [Reporting Options](#reporting-options)
6. [Performance Settings](#performance-settings)
7. [Environment Configuration](#environment-configuration)
8. [Advanced Options](#advanced-options)
9. [Configuration Examples](#configuration-examples)

## Configuration Overview

All SCAN configuration is done through the `scan` extension block in your `build.gradle.kts`:

```kotlin
scan {
    // Configuration options go here
}
```

### Convention Properties

SCAN uses Gradle's Provider API with convention properties. This means:

- All properties have sensible defaults
- You only need to configure what you want to change
- Properties are lazily evaluated for better performance

## Basic Settings

### `enabled`

- **Type**: `Property<Boolean>`
- **Default**: `true`
- **Description**: Enable or disable the SCAN plugin entirely

```kotlin
scan {
    enabled = false  // Disable SCAN completely
}
```

### `failOnSecrets`

- **Type**: `Property<Boolean>`
- **Default**: `true`
- **Description**: Fail the build when secrets are detected

```kotlin
scan {
    failOnSecrets = false  // Only warn, don't fail build
}
```

### `failOnFound` (Alias)

- **Type**: `Property<Boolean>`
- **Default**: `true`
- **Description**: Alias for `failOnSecrets`

### `warnOnSecrets`

- **Type**: `Property<Boolean>`
- **Default**: `true`
- **Description**: Show warnings even when not failing the build

```kotlin
scan {
    warnOnSecrets = false  // Suppress warning messages
}
```

### `verbose`

- **Type**: `Property<Boolean>`
- **Default**: `false`
- **Description**: Enable detailed console output

```kotlin
scan {
    verbose = true  // Show detailed scanning progress
}
```

### `quiet`

- **Type**: `Property<Boolean>`
- **Default**: `false`
- **Description**: Suppress non-essential output

```kotlin
scan {
    quiet = true  // Minimal console output
}
```

## File Pattern Configuration

### `includePatterns`

- **Type**: `Property<Set<String>>`
- **Default**:

  ```kotlin
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
  ```

- **Description**: Ant-style patterns for files to include in scanning

```kotlin
scan {
    includePatterns = setOf(
        "src/**/*.kt",
        "config/**/*.yml",
        "*.properties"
    )
}
```

### `excludePatterns`

- **Type**: `Property<Set<String>>`
- **Default**:

  ```kotlin
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
  ```

- **Description**: Ant-style patterns for files to exclude from scanning

```kotlin
scan {
    excludePatterns = setOf(
        "**/build/**",
        "**/test-data/**",
        "**/*.generated.*"
    )
}
```

### `includeFiles`

- **Type**: `Property<Set<String>>`
- **Default**: `emptySet()`
- **Description**: Specific file names to include (alternative to patterns)

```kotlin
scan {
    includeFiles = setOf(
        "application.yml",
        "config.properties"
    )
}
```

### `excludeFiles`

- **Type**: `Property<Set<String>>`
- **Default**: `emptySet()`
- **Description**: Specific file names to exclude (alternative to patterns)

```kotlin
scan {
    excludeFiles = setOf(
        "test-secrets.properties",
        "mock-data.json"
    )
}
```

### `ignoreTestFiles`

- **Type**: `Property<Boolean>`
- **Default**: `true`
- **Description**: Apply more lenient scanning to test files

```kotlin
scan {
    ignoreTestFiles = false  // Apply strict scanning to test files
}
```

### `scanTests`

- **Type**: `Property<Boolean>`
- **Default**: `true`
- **Description**: Whether to scan test directories at all

```kotlin
scan {
    scanTests = false  // Skip test directories entirely
}
```

## Detection Settings

### `strictMode`

- **Type**: `Property<Boolean>`
- **Default**: `false`
- **Description**: Enable all detectors with maximum sensitivity

```kotlin
scan {
    strictMode = true  // Maximum security, may increase false positives
}
```

### `entropyThreshold`

- **Type**: `Property<Double>`
- **Default**: `4.5`
- **Description**: Minimum entropy threshold for random string detection (0.0-8.0)

```kotlin
scan {
    entropyThreshold = 5.0  // Higher = fewer false positives, may miss some secrets
}
```

### `contextAwareScanning`

- **Type**: `Property<Boolean>`
- **Default**: `true`
- **Description**: Use intelligent context analysis to reduce false positives

```kotlin
scan {
    contextAwareScanning = false  // Disable context analysis for faster scanning
}
```

### `customPatterns`

- **Type**: `Property<List<String>>`
- **Default**: `emptyList()`
- **Description**: Custom regex patterns for organization-specific secrets

```kotlin
scan {
    customPatterns = listOf(
        "MYCOMPANY_API_[A-Z0-9]{32}",
        "INTERNAL_SECRET_.*",
        "[sS]ecret[kK]ey\\s*[=:]\\s*['\"]([^'\"]+)['\"]"
    )
}
```

## Reporting Options

### `generateHtmlReport`

- **Type**: `Property<Boolean>`
- **Default**: `false`
- **Description**: Generate an HTML report with detailed findings

```kotlin
scan {
    generateHtmlReport = true
}
```

### `generateJsonReport`

- **Type**: `Property<Boolean>`
- **Default**: `false`
- **Description**: Generate a JSON report for CI/CD integration

```kotlin
scan {
    generateJsonReport = true
}
```

### `reportOutputDir`

- **Type**: `Property<Directory>`
- **Default**: `project.layout.buildDirectory.dir("reports/scan")`
- **Description**: Directory where reports are generated

```kotlin
scan {
    reportOutputDir = layout.buildDirectory.dir("reports/security")
}
```

## Performance Settings

### `maxFileSizeBytes`

- **Type**: `Property<Long>`
- **Default**: `10 * 1024 * 1024` (10MB)
- **Description**: Maximum file size to scan (in bytes)

```kotlin
scan {
    maxFileSizeBytes = 5 * 1024 * 1024  // 5MB limit
}
```

### `parallelScanning`

- **Type**: `Property<Boolean>`
- **Default**: `true`
- **Description**: Enable parallel processing of files

```kotlin
scan {
    parallelScanning = false  // Disable for debugging or resource constraints
}
```

## Environment Configuration

SCAN automatically adapts to different environments. You can customize this behavior:

```kotlin
scan {
    val isCI = System.getenv("CI")?.toBoolean() ?: false
    val isDevelopment = project.hasProperty("dev")
    
    when {
        isCI -> {
            // Strict CI configuration
            failOnSecrets = true
            generateJsonReport = true
            verbose = true
            strictMode = true
        }
        isDevelopment -> {
            // Lenient development configuration
            failOnSecrets = false
            warnOnSecrets = true
            generateHtmlReport = true
        }
        else -> {
            // Default production configuration
            failOnSecrets = true
            generateHtmlReport = true
        }
    }
}
```

## Advanced Options

### Working with Property Providers

SCAN uses Gradle's Provider API. You can use providers for dynamic configuration:

```kotlin
scan {
    // Use project properties
    verbose = project.providers.gradleProperty("scan.verbose")
        .map { it.toBoolean() }
        .orElse(false)
    
    // Use environment variables
    failOnSecrets = project.providers.environmentVariable("SCAN_FAIL_ON_SECRETS")
        .map { it.toBoolean() }
        .orElse(true)
    
    // Conditional configuration
    strictMode = project.providers.provider {
        project.gradle.startParameter.taskNames.contains("release")
    }
}
```

### Gradle Configuration Cache Compatibility

SCAN is designed to work with Gradle's configuration cache. However, for publishing tasks, you may need to disable it:

```kotlin
// In gradle.properties for publishing
org.gradle.configuration-cache=false

// Or run specific tasks without cache
// ./gradlew publishPlugins --no-configuration-cache
```

## Configuration Examples

### Minimal Security Setup

```kotlin
scan {
    failOnSecrets = true
    generateJsonReport = true
}
```

### Development-Friendly Setup

```kotlin
scan {
    failOnSecrets = false
    warnOnSecrets = true
    verbose = true
    generateHtmlReport = true
    ignoreTestFiles = true
}
```

### High-Security Production Setup

```kotlin
scan {
    strictMode = true
    failOnSecrets = true
    entropyThreshold = 4.0
    contextAwareScanning = true
    generateHtmlReport = true
    generateJsonReport = true
    
    customPatterns = listOf(
        "COMPANY_SECRET_[A-Z0-9]{32}",
        "INTERNAL_API_KEY_.*"
    )
    
    includePatterns = setOf(
        "src/**/*.kt",
        "src/**/*.java",
        "src/**/*.properties",
        "src/**/*.yml",
        "config/**/*",
        "*.gradle.kts",
        "gradle.properties"
    )
}
```

### CI/CD Optimized Setup

```kotlin
scan {
    val isCI = System.getenv("CI")?.toBoolean() ?: false
    
    failOnSecrets = isCI
    warnOnSecrets = true
    generateJsonReport = isCI
    generateHtmlReport = !isCI
    verbose = isCI
    
    if (isCI) {
        // More aggressive scanning in CI
        strictMode = true
        entropyThreshold = 4.0
    } else {
        // More lenient for local development
        strictMode = false
        entropyThreshold = 5.0
    }
}
```

### Large Codebase Optimization

```kotlin
scan {
    // Performance optimizations for large codebases
    maxFileSizeBytes = 1024 * 1024  // 1MB limit
    parallelScanning = true
    
    // Focus on critical files only
    includePatterns = setOf(
        "src/main/**/*.kt",
        "src/main/**/*.java",
        "src/main/resources/**/*.properties",
        "src/main/resources/**/*.yml"
    )
    
    // Exclude large directories
    excludePatterns = setOf(
        "**/build/**",
        "**/node_modules/**",
        "**/test-data/**",
        "**/*.generated.*",
        "**/vendor/**"
    )
}
```

### Multi-Module Project Setup

```kotlin
// In root build.gradle.kts
allprojects {
    apply(plugin = "io.github.theaniketraj.scan")
    
    scan {
        // Common configuration for all modules
        failOnSecrets = true
        generateJsonReport = true
        
        // Module-specific customization
        if (project.name.contains("test")) {
            failOnSecrets = false
            scanTests = true
        }
        
        if (project.name.contains("api")) {
            strictMode = true
            customPatterns = listOf("API_SECRET_.*")
        }
    }
}
```

---

## Property Reference Summary

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | `Boolean` | `true` | Enable/disable plugin |
| `failOnSecrets` | `Boolean` | `true` | Fail build on detection |
| `warnOnSecrets` | `Boolean` | `true` | Show warnings |
| `verbose` | `Boolean` | `false` | Detailed output |
| `quiet` | `Boolean` | `false` | Minimal output |
| `strictMode` | `Boolean` | `false` | Maximum sensitivity |
| `entropyThreshold` | `Double` | `4.5` | Entropy detection threshold |
| `contextAwareScanning` | `Boolean` | `true` | Intelligent context analysis |
| `ignoreTestFiles` | `Boolean` | `true` | Lenient test file scanning |
| `scanTests` | `Boolean` | `true` | Scan test directories |
| `generateHtmlReport` | `Boolean` | `false` | Generate HTML report |
| `generateJsonReport` | `Boolean` | `false` | Generate JSON report |
| `maxFileSizeBytes` | `Long` | `10MB` | Maximum file size |
| `parallelScanning` | `Boolean` | `true` | Parallel processing |
| `includePatterns` | `Set<String>` | See docs | Files to include |
| `excludePatterns` | `Set<String>` | See docs | Files to exclude |
| `customPatterns` | `List<String>` | `empty` | Custom regex patterns |

For more examples and use cases, see the [examples directory](examples/) and [user guide](user-guide.md).
