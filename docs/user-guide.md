# SCAN Plugin User Guide

Welcome to the comprehensive user guide for the SCAN (Sensitive Code Analyzer for Nerds) Gradle Plugin. This guide will walk you through everything you need to know to secure your codebase effectively.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Installation](#installation)
3. [Basic Configuration](#basic-configuration)
4. [Advanced Configuration](#advanced-configuration)
5. [Understanding Results](#understanding-results)
6. [Integration with Build Lifecycle](#integration-with-build-lifecycle)
7. [CI/CD Integration](#cicd-integration)
8. [Troubleshooting](#troubleshooting)
9. [Best Practices](#best-practices)

## Quick Start

The fastest way to get started with SCAN is to add it to your `build.gradle.kts` and run a scan:

```kotlin
plugins {
    id("io.github.theaniketraj.scan") version "2.2.0"
}
```

Run your first scan:

```bash
./gradlew scanForSecrets
```

That's it! SCAN will analyze your codebase with sensible defaults and report any potential security issues.

## Installation

### Gradle Kotlin DSL (build.gradle.kts)

```kotlin
plugins {
    id("io.github.theaniketraj.scan") version "2.2.0"
}
```

### Gradle Groovy DSL (build.gradle)

```groovy
plugins {
    id 'io.github.theaniketraj.scan' version '2.2.0'
}
```

### Legacy Plugin Application

```kotlin
buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath("io.github.theaniketraj:scan-gradle-plugin:2.2.0")
    }
}

apply(plugin = "io.github.theaniketraj.scan")
```

## Basic Configuration

### Minimal Configuration

SCAN works out of the box with zero configuration:

```kotlin
// No configuration needed - SCAN uses intelligent defaults
```

### Common Configurations

```kotlin
scan {
    // Fail the build if secrets are found (default: true)
    failOnSecrets = true
    
    // Include test files in scanning (default: true)
    scanTests = false
    
    // Generate HTML report (default: false)
    generateHtmlReport = true
    
    // Set entropy threshold for random string detection (default: 4.5)
    entropyThreshold = 5.0
    
    // Enable verbose output (default: false)
    verbose = true
}
```

### File Pattern Configuration

```kotlin
scan {
    // Customize which files to scan
    includePatterns = setOf(
        "src/**/*.kt",
        "src/**/*.java",
        "src/**/*.properties",
        "config/**/*.yml"
    )
    
    // Exclude specific patterns
    excludePatterns = setOf(
        "**/build/**",
        "**/test/resources/test-data/**",
        "**/*.class"
    )
    
    // Alternative: use individual file patterns
    includeFiles = setOf("gradle.properties", "application.yml")
    excludeFiles = setOf("test-secrets.properties")
}
```

## Advanced Configuration

### Detector Configuration

```kotlin
scan {
    // Configure different detection methods
    strictMode = true                    // Enable all detectors with high sensitivity
    contextAwareScanning = true          // Use intelligent context analysis
    entropyThreshold = 4.5              // Threshold for entropy-based detection
    maxFileSizeBytes = 10 * 1024 * 1024 // Skip files larger than 10MB
    
    // Custom secret patterns (regex)
    customPatterns = listOf(
        "MY_COMPANY_API_.*",
        "CUSTOM_SECRET_[A-Z0-9]{32}"
    )
}
```

### Reporting Configuration

```kotlin
scan {
    // Console output
    verbose = true                       // Detailed console output
    quiet = false                       // Suppress non-essential output
    
    // File reports
    generateHtmlReport = true           // Generate HTML report
    generateJsonReport = true           // Generate JSON report for CI/CD
    reportOutputDir = layout.buildDirectory.dir("reports/security")
    
    // Build behavior
    failOnSecrets = true               // Fail build on detection
    failOnFound = true                 // Alias for failOnSecrets
    warnOnSecrets = true              // Show warnings even if not failing
}
```

### Performance Configuration

```kotlin
scan {
    // Performance tuning
    parallelScanning = true             // Enable parallel file processing
    maxFileSizeBytes = 5 * 1024 * 1024 // Limit file size to 5MB
    
    // Memory management
    // SCAN automatically manages memory for large codebases
}
```

### Environment-Specific Configuration

```kotlin
scan {
    // Configure differently for different environments
    if (System.getenv("CI") == "true") {
        failOnSecrets = true
        generateJsonReport = true
        verbose = true
    } else {
        // More lenient for local development
        warnOnSecrets = true
        failOnSecrets = false
    }
}
```

## Understanding Results

### Console Output

SCAN provides clear, actionable output:

```pgsql
> Task :scanForSecrets
🔍 SCAN: Analyzing 147 files for sensitive information...

❌ CRITICAL: AWS Access Key detected
   File: src/main/resources/application.yml:12
   Pattern: AWS Access Key
   Content: AKIAIOSFODNN7EXAMPLE
   
⚠️  WARNING: High entropy string detected
   File: src/main/kotlin/Config.kt:25
   Entropy: 4.8/5.0
   Content: dGhpc19pc19hX3Rlc3Rfc2VjcmV0XzEyMzQ1
   
✅ SAFE: Test API key (whitelisted)
   File: src/test/resources/test.properties:5
   Content: test_api_key_12345

📊 Scan Results:
   - Files scanned: 147
   - Secrets found: 1 critical, 1 warning
   - Scan duration: 2.3s
```

### HTML Report

When `generateHtmlReport = true`, SCAN generates a comprehensive HTML report with:

- **Summary Dashboard**: Overview of findings by severity
- **File-by-File Analysis**: Detailed breakdown per file
- **Pattern Details**: Which patterns triggered each detection
- **Context Information**: Surrounding code for each finding
- **Recommendations**: Specific remediation advice

### JSON Report

When `generateJsonReport = true`, SCAN outputs machine-readable JSON perfect for CI/CD integration:

```json
{
  "scanTimestamp": "2025-08-17T10:30:00Z",
  "scanDuration": "2.3s",
  "summary": {
    "filesScanned": 147,
    "secretsFound": 2,
    "criticalFindings": 1,
    "warningFindings": 1
  },
  "findings": [
    {
      "severity": "CRITICAL",
      "type": "AWS_ACCESS_KEY",
      "file": "src/main/resources/application.yml",
      "line": 12,
      "content": "AKIAIOSFODNN7EXAMPLE",
      "pattern": "AKIA[0-9A-Z]{16}",
      "recommendation": "Remove AWS credentials and use IAM roles or environment variables"
    }
  ]
}
```

## Integration with Build Lifecycle

### Automatic Integration

SCAN automatically integrates with your build lifecycle:

```kotlin
// SCAN runs automatically before compilation
./gradlew build        // Includes security scanning
./gradlew compileKotlin // Runs scan first
./gradlew check        // Includes security verification
```

### Manual Task Execution

```bash
# Run security scan only
./gradlew scanForSecrets

# Run with verbose output
./gradlew scanForSecrets --info

# Force re-run even if up-to-date
./gradlew scanForSecrets --rerun-tasks
```

### Task Dependencies

```kotlin
// Make other tasks depend on security scanning
tasks.named("deployToProduction") {
    dependsOn("scanForSecrets")
}

// Or run scan before custom tasks
tasks.register("customDeploy") {
    dependsOn("scanForSecrets")
    doLast {
        // Your deployment logic
    }
}
```

## CI/CD Integration

### GitHub Actions

```yaml
name: Security Scan
on: [push, pull_request]

jobs:
  security-scan:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Run Security Scan
      run: ./gradlew scanForSecrets
    
    - name: Upload Security Report
      if: always()
      uses: actions/upload-artifact@v3
      with:
        name: security-report
        path: build/reports/scan/
```

### Jenkins Pipeline

```groovy
pipeline {
    agent any
    stages {
        stage('Security Scan') {
            steps {
                sh './gradlew scanForSecrets'
            }
            post {
                always {
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'build/reports/scan',
                        reportFiles: 'scan-report.html',
                        reportName: 'Security Scan Report'
                    ])
                }
            }
        }
    }
}
```

### GitLab CI

```yaml
security-scan:
  stage: test
  script:
    - ./gradlew scanForSecrets
  artifacts:
    when: always
    reports:
      junit: build/reports/scan/scan-report.xml
    paths:
      - build/reports/scan/
```

## Troubleshooting

### Common Issues

#### False Positives

```kotlin
scan {
    // Reduce false positives
    strictMode = false
    entropyThreshold = 5.0  // Higher threshold = fewer false positives
    ignoreTestFiles = true
    
    // Exclude known safe patterns
    excludePatterns = setOf(
        "**/test-data/**",
        "**/mock-responses/**"
    )
}
```

#### Performance Issues

```kotlin
scan {
    // Optimize for large codebases
    maxFileSizeBytes = 1024 * 1024  // 1MB limit
    parallelScanning = true
    
    // Focus on important files only
    includePatterns = setOf(
        "src/main/**/*.kt",
        "src/main/**/*.properties"
    )
}
```

#### Build Failures

If SCAN is failing your build unexpectedly:

```kotlin
scan {
    // Temporary: warn instead of fail
    failOnSecrets = false
    warnOnSecrets = true
    verbose = true  // Get detailed output to debug
}
```

### Debug Mode

Enable verbose logging to understand what SCAN is doing:

```bash
./gradlew scanForSecrets --info --stacktrace
```

### Getting Help

1. **Check the logs**: Run with `--info` for detailed output
2. **Review configuration**: Ensure your patterns are correct
3. **Test incrementally**: Start with default config, then customize
4. **Check file permissions**: Ensure SCAN can read your files

## Best Practices

### Development Workflow

1. **Run locally first**: Always test SCAN locally before pushing
2. **Start permissive**: Begin with `failOnSecrets = false` to understand findings
3. **Iterate gradually**: Slowly tighten security as you clean up existing issues
4. **Document exceptions**: Use comments to explain why certain patterns are safe

### Configuration Management

```kotlin
scan {
    // Use environment-specific configuration
    val isCI = System.getenv("CI")?.toBoolean() ?: false
    val isDevelopment = project.hasProperty("dev")
    
    when {
        isCI -> {
            failOnSecrets = true
            generateJsonReport = true
            verbose = true
        }
        isDevelopment -> {
            failOnSecrets = false
            warnOnSecrets = true
        }
        else -> {
            // Production defaults
            failOnSecrets = true
            generateHtmlReport = true
        }
    }
}
```

### Team Adoption

1. **Start with warnings**: Don't break builds immediately
2. **Educate the team**: Share examples of what SCAN catches
3. **Create whitelist process**: Establish how to handle false positives
4. **Regular reviews**: Periodically review and update patterns

### Security Hygiene

1. **Regular scans**: Run SCAN on every commit
2. **Review findings**: Don't just ignore warnings
3. **Update patterns**: Keep custom patterns current
4. **Document decisions**: Record why certain patterns are excluded

---

## Next Steps

- **[Configuration Reference](configuration-reference.md)**: Detailed explanation of all configuration options
- **[Pattern Reference](pattern-reference.md)**: Complete list of built-in patterns and how to create custom ones
- **[Examples](examples/)**: Real-world usage examples and integrations
- **[Contributing](contributing.md)**: Help improve SCAN for everyone
