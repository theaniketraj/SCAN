# Getting Started with SCAN Gradle Plugin

Welcome to the SCAN Gradle Plugin! This guide will help you quickly set up and start using the plugin to scan your projects for security vulnerabilities like exposed API keys, passwords, and other sensitive information.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Installation](#installation)
- [Basic Usage](#basic-usage)
- [Configuration](#configuration)
- [Understanding Results](#understanding-results)
- [Common Use Cases](#common-use-cases)
- [Troubleshooting](#troubleshooting)
- [Next Steps](#next-steps)

## Prerequisites

Before you begin, make sure you have:

- **Gradle 7.0+** (recommended: Gradle 8.0+)
- **Java 11+** or **Kotlin 1.8+**
- A Gradle project (Kotlin DSL or Groovy DSL)

Check your versions:

```bash
./gradlew --version
java --version
```

## Quick Start

### 1. Add the Plugin

**For Kotlin DSL (`build.gradle.kts`):**

```kotlin
plugins {
    id("io.github.theaniketraj.scan") version "1.0.0"
}
```

**For Groovy DSL (`build.gradle`):**

```groovy
plugins {
    id 'io.github.theaniketraj.scan' version '1.0.0'
}
```

### 2. Run Your First Scan

```bash
./gradlew scan
```

That's it! The plugin will scan your project and display results in the console.

## Installation

### Using the Plugins DSL (Recommended)

Add the plugin to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.theaniketraj.scan") version "1.0.0"
}

// Optional: Configure the plugin
scan {
    // Enable all detectors
    enablePatternDetection = true
    enableEntropyDetection = true
    enableContextAwareDetection = true
    
    // Set sensitivity level (LOW, MEDIUM, HIGH)
    sensitivityLevel = ScanSensitivity.MEDIUM
    
    // Specify output format
    outputFormat = listOf("console", "json")
}
```

### Using Legacy Plugin Application

If you're using an older Gradle version:

```kotlin
buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.scan:scan-gradle-plugin:1.0.0")
    }
}

apply(plugin = "io.github.theaniketraj.scan")
```

### Verifying Installation

After adding the plugin, verify it's available:

```bash
./gradlew tasks --group=scan
```

You should see available scan tasks:

```pgsql
Scan tasks
----------
scan - Scans the project for security vulnerabilities
scanReport - Generates detailed scan reports
```

## Basic Usage

### Running a Scan

**Basic scan:**

```bash
./gradlew scan
```

**Scan with specific configuration:**

```bash
./gradlew scan --sensitivity=HIGH --format=json
```

**Scan specific directories:**

```bash
./gradlew scan --include-paths=src/main/kotlin,src/main/resources
```

### Understanding Command Options

| Option | Description | Example |
|--------|-------------|---------|
| `--sensitivity` | Detection sensitivity (LOW, MEDIUM, HIGH) | `--sensitivity=HIGH` |
| `--format` | Output format (console, json, html) | `--format=json,html` |
| `--output-dir` | Directory for report files | `--output-dir=build/scan-reports` |
| `--include-paths` | Paths to include in scan | `--include-paths=src/main` |
| `--exclude-paths` | Paths to exclude from scan | `--exclude-paths=src/test` |
| `--config-file` | Custom configuration file | `--config-file=custom-scan.yml` |

## Configuration

### Basic Configuration

Create a configuration block in your `build.gradle.kts`:

```kotlin
scan {
    // Detection settings
    enablePatternDetection = true
    enableEntropyDetection = true
    enableContextAwareDetection = true
    
    // Sensitivity level
    sensitivityLevel = ScanSensitivity.MEDIUM
    
    // Output settings
    outputFormat = listOf("console", "json")
    outputDirectory = file("build/scan-reports")
    
    // File filtering
    includeExtensions = listOf("kt", "java", "properties", "yml", "json")
    excludePaths = listOf("build/", ".gradle/", "*.test.*")
    
    // Reporting
    failOnSecrets = true
    maxSecretsAllowed = 0
}
```

### Configuration File

For more complex configurations, create a `scan-config.yml` file:

```yaml
# scan-config.yml
detection:
  patterns:
    enabled: true
    sensitivity: MEDIUM
  entropy:
    enabled: true
    threshold: 4.5
  contextAware:
    enabled: true
    checkComments: false

filters:
  includeExtensions:
    - "kt"
    - "java"
    - "properties"
    - "yml"
    - "json"
    - "xml"
  
  excludePaths:
    - "build/"
    - ".gradle/"
    - "*.test.*"
    - "test/"
  
  whitelistPatterns:
    - "example_api_key"
    - "dummy_password"
    - "test_secret"

reporting:
  formats:
    - console
    - json
  outputDirectory: "build/scan-reports"
  failOnSecrets: true
  maxSecretsAllowed: 0

patterns:
  apiKeys:
    - pattern: "(?i)api[_-]?key['\"]?\\s*[:=]\\s*['\"]?([a-zA-Z0-9]{20,})"
      description: "Generic API Key"
      severity: HIGH
  
  passwords:
    - pattern: "(?i)password['\"]?\\s*[:=]\\s*['\"]?([^\\s'\"]{8,})"
      description: "Password field"
      severity: MEDIUM
```

Then reference it in your build file:

```kotlin
scan {
    configFile = file("scan-config.yml")
}
```

### Environment-Specific Configuration

```kotlin
scan {
    // Different settings for different environments
    if (project.hasProperty("ci")) {
        // CI environment - strict settings
        sensitivityLevel = ScanSensitivity.HIGH
        failOnSecrets = true
        outputFormat = listOf("json", "junit")
    } else {
        // Development environment - more lenient
        sensitivityLevel = ScanSensitivity.MEDIUM
        failOnSecrets = false
        outputFormat = listOf("console")
    }
}
```

## Understanding Results

### Console Output

```pgsql
> Task :scan

SCAN Results Summary
===================
Files Scanned: 127
Secrets Found: 3
High Severity: 1
Medium Severity: 2
Low Severity: 0

Detected Secrets:
-----------------
HIGH   | src/main/kotlin/Config.kt:15 | API Key detected
       | Pattern: api_key = "sk_live_abc123..."
       
MEDIUM | src/main/resources/application.properties:8 | Database password
       | Pattern: db.password=secretPassword123
       
MEDIUM | src/main/kotlin/Service.kt:42 | High entropy string
       | Pattern: val token = "hg8f7d6s5a4..."

Scan completed in 2.3 seconds
```

### JSON Report Structure

```json
{
  "scanResult": {
    "timestamp": "2024-01-15T10:30:00Z",
    "summary": {
      "filesScanned": 127,
      "secretsFound": 3,
      "severityBreakdown": {
        "HIGH": 1,
        "MEDIUM": 2,
        "LOW": 0
      }
    },
    "findings": [
      {
        "id": "finding-001",
        "file": "src/main/kotlin/Config.kt",
        "line": 15,
        "column": 20,
        "severity": "HIGH",
        "type": "API_KEY",
        "description": "API Key detected",
        "pattern": "api_key = \"sk_live_...\"",
        "detector": "PatternDetector",
        "confidence": 0.95
      }
    ],
    "configuration": {
      "sensitivityLevel": "MEDIUM",
      "detectorsUsed": ["PatternDetector", "EntropyDetector"]
    }
  }
}
```

### Severity Levels

| Severity | Description | Examples |
|----------|-------------|----------|
| **HIGH** | Confirmed secrets with high confidence | API keys, database URLs with credentials |
| **MEDIUM** | Likely secrets requiring review | High-entropy strings, password fields |
| **LOW** | Potential secrets with low confidence | Generic patterns, test data |

## Common Use Cases

### 1. Pre-commit Hook

Create a script `pre-commit-scan.sh`:

```pgsql
#!/bin/bash
echo "Running security scan..."
./gradlew scan --sensitivity=HIGH --format=console

if [ $? -eq 0 ]; then
    echo "✅ No secrets detected. Commit allowed."
    exit 0
else
    echo "❌ Secrets detected! Please review and remove them before committing."
    exit 1
fi
```

Make it executable and add to git hooks:

```bash
chmod +x pre-commit-scan.sh
cp pre-commit-scan.sh .git/hooks/pre-commit
```

### 2. CI/CD Integration

**GitHub Actions:**

```yaml
# .github/workflows/security-scan.yml
name: Security Scan
on: [push, pull_request]

jobs:
  security-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '11'
          distribution: 'temurin'
      
      - name: Run Security Scan
        run: ./gradlew scan --sensitivity=HIGH --format=json,junit
      
      - name: Upload Scan Results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: scan-results
          path: build/scan-reports/
```

**Jenkins Pipeline:**

```groovy
pipeline {
    agent any
    stages {
        stage('Security Scan') {
            steps {
                sh './gradlew scan --format=junit'
            }
            post {
                always {
                    publishTestResults testResultsPattern: 'build/scan-reports/scan-results.xml'
                    archiveArtifacts artifacts: 'build/scan-reports/**', allowEmptyArchive: true
                }
            }
        }
    }
}
```

### 3. Custom Patterns

Add project-specific patterns:

```kotlin
scan {
    customPatterns = mapOf(
        "company_api_key" to mapOf(
            "pattern" to "MYCOMPANY_[A-Z0-9]{32}",
            "description" to "Company-specific API key",
            "severity" to "HIGH"
        ),
        "internal_token" to mapOf(
            "pattern" to "tok_[a-z0-9]{20}",
            "description" to "Internal service token",
            "severity" to "MEDIUM"
        )
    )
}
```

### 4. Excluding False Positives

Create a `.scan-ignore` file:

```pgsql
# Ignore test files
src/test/**

# Ignore example configurations
examples/**
docs/**

# Ignore specific patterns
src/main/kotlin/Constants.kt:15  # Known safe constant
```

## Troubleshooting

### Common Issues

#### Issue: Plugin not found

```pgsql
Plugin [id: 'io.github.theaniketraj.scan', version: '1.0.0'] was not found
```

**Solution:** Ensure you're using Gradle 7.0+ and have internet access:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
```

#### Issue: Too many false positives

```pgsql
Found 50+ potential secrets, but most are test data
```

**Solution:** Adjust sensitivity and add exclusions:

```kotlin
scan {
    sensitivityLevel = ScanSensitivity.LOW
    excludePaths = listOf("src/test/", "*.test.*")
    whitelistPatterns = listOf("test_", "example_", "dummy_")
}
```

#### Issue: Scan is too slow

```pgsql
Scan takes several minutes on large project
```

**Solution:** Optimize scan scope:

```kotlin
scan {
    includeExtensions = listOf("kt", "java", "properties") // Only scan relevant files
    excludePaths = listOf("build/", "node_modules/", ".git/")
    enableEntropyDetection = false // Disable if not needed
}
```

### Getting Help

1. **Check the documentation**: See `docs/` directory for detailed guides
2. **Search existing issues**: Check GitHub issues for similar problems
3. **Enable debug logging**:

   ```bash
   ./gradlew scan --debug --stacktrace
   ```

4. **Create an issue**: If you can't find a solution, create a GitHub issue with:
   - Gradle version
   - Plugin version
   - Sample configuration
   - Error messages or unexpected behavior

## Next Steps

Now that you have the plugin running:

1. **Configure for your project**: Customize patterns and sensitivity
2. **Integrate with CI/CD**: Add automated scanning to your pipeline
3. **Set up pre-commit hooks**: Prevent secrets from being committed
4. **Train your team**: Share best practices for handling scan results
5. **Regular audits**: Schedule periodic comprehensive scans

### Advanced Topics

- [Configuration Reference](docs/configuration-reference.md) - Complete configuration options
- [Custom Patterns](docs/pattern-reference.md) - Writing custom detection patterns
- [CI/CD Integration](docs/examples/ci-cd-integration/) - Detailed CI/CD examples
- [Performance Tuning](docs/user-guide.md#performance) - Optimizing scan performance
- [Contributing](https://github.com/theaniketraj/SCAN/blob/main/CONTRIBUTING.md) - Contributing to the project

### Useful Commands

```bash
# Quick scan with high sensitivity
./gradlew scan --sensitivity=HIGH

# Generate HTML report
./gradlew scan --format=html --output-dir=reports

# Scan only specific files
./gradlew scan --include-paths=src/main/kotlin/sensitive/

# Dry run (show what would be scanned without scanning)
./gradlew scan --dry-run

# Show plugin version and available tasks
./gradlew tasks --group=scan
```

Welcome to secure development with the SCAN Gradle Plugin! 🔒
