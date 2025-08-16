# Basic Usage Examples

This directory contains simple examples demonstrating basic SCAN plugin usage.

## Quick Start Example

### build.gradle.kts

```kotlin
plugins {
    kotlin("jvm") version "2.0.20"
    id("io.github.theaniketraj.scan") version "1.0.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}

// Minimal SCAN configuration (uses defaults)
scan {
    failOnSecrets = true
    generateHtmlReport = true
}
```

### Example Source File (src/main/kotlin/Config.kt)

```kotlin
package com.example

// ❌ This will be detected as a secret
class DatabaseConfig {
    companion object {
        const val API_KEY = "AKIAIOSFODNN7EXAMPLE"  // AWS Access Key
        const val DATABASE_URL = "mysql://user:password123@localhost:3306/mydb"
        const val JWT_SECRET = "dGhpc19pc19hX3Rlc3Rfc2VjcmV0XzEyMzQ1"  // Base64 encoded
    }
}

// ✅ This is better - using environment variables
class SecureConfig {
    companion object {
        val API_KEY = System.getenv("AWS_ACCESS_KEY_ID") ?: throw IllegalStateException("Missing AWS_ACCESS_KEY_ID")
        val DATABASE_URL = System.getenv("DATABASE_URL") ?: "jdbc:h2:mem:testdb"
        val JWT_SECRET = System.getenv("JWT_SECRET") ?: generateRandomSecret()
        
        private fun generateRandomSecret(): String {
            // Generate secure random secret at runtime
            return "generated-at-runtime"
        }
    }
}
```

### Run the Scan

```bash
# Run security scan
./gradlew scanForSecrets

# Run as part of build (scan runs automatically)
./gradlew build

# View detailed output
./gradlew scanForSecrets --info
```

### Expected Output

```bash
> Task :scanForSecrets FAILED

🔍 SCAN: Analyzing 3 files for sensitive information...

❌ CRITICAL: AWS Access Key detected
   File: src/main/kotlin/Config.kt:6
   Pattern: AWS Access Key
   Content: AKIAIOSFODNN7EXAMPLE
   
❌ CRITICAL: Database credentials detected
   File: src/main/kotlin/Config.kt:7
   Pattern: Database URL with credentials
   Content: mysql://user:password123@localhost:3306/mydb
   
⚠️  WARNING: High entropy string detected
   File: src/main/kotlin/Config.kt:8
   Entropy: 4.8/5.0
   Content: dGhpc19pc19hX3Rlc3Rfc2VjcmV0XzEyMzQ1
   
📊 Scan Results:
   - Files scanned: 3
   - Secrets found: 2 critical, 1 warning
   - HTML report: build/reports/scan/scan-report.html

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':scanForSecrets'.
> Security scan failed: 3 potential secrets detected
```

## Configuration Examples

### Lenient Configuration (Development)

```kotlin
scan {
    // Don't fail build during development
    failOnSecrets = false
    warnOnSecrets = true
    
    // Be more permissive with test files
    ignoreTestFiles = true
    
    // Generate reports for review
    generateHtmlReport = true
    verbose = true
}
```

### Strict Configuration (Production)

```kotlin
scan {
    // Fail build on any detection
    failOnSecrets = true
    strictMode = true
    
    // Lower entropy threshold (catch more potential secrets)
    entropyThreshold = 4.0
    
    // Comprehensive reporting
    generateHtmlReport = true
    generateJsonReport = true
    
    // Custom patterns for organization
    customPatterns = listOf(
        "COMPANY_API_[A-Z0-9]{32}",
        "INTERNAL_SECRET_.*"
    )
}
```

### Environment-Based Configuration

```kotlin
scan {
    val isCI = System.getenv("CI")?.toBoolean() ?: false
    val isDev = project.hasProperty("dev")
    
    when {
        isCI -> {
            failOnSecrets = true
            generateJsonReport = true
            verbose = true
        }
        isDev -> {
            failOnSecrets = false
            generateHtmlReport = true
        }
        else -> {
            failOnSecrets = true
            generateHtmlReport = true
        }
    }
}
```

## Common Patterns to Fix

### 1. Hardcoded API Keys

❌ **Bad:**

```kotlin
class ApiClient {
    private val apiKey = "sk_live_abcd1234567890abcd1234567890"
}
```

✅ **Good:**

```kotlin
class ApiClient {
    private val apiKey = System.getenv("STRIPE_API_KEY") 
        ?: throw IllegalStateException("STRIPE_API_KEY environment variable not set")
}
```

### 2. Database Credentials

❌ **Bad:**

```kotlin
val dataSource = HikariDataSource().apply {
    jdbcUrl = "jdbc:postgresql://localhost:5432/mydb"
    username = "admin"
    password = "supersecret123"
}
```

✅ **Good:**

```kotlin
val dataSource = HikariDataSource().apply {
    jdbcUrl = System.getenv("DATABASE_URL") ?: "jdbc:h2:mem:testdb"
    username = System.getenv("DB_USERNAME") ?: "test"
    password = System.getenv("DB_PASSWORD") ?: "test"
}
```

### 3. Configuration Files

❌ **Bad application.yml:**

```yaml
aws:
  accessKeyId: AKIAIOSFODNN7EXAMPLE
  secretAccessKey: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

database:
  url: mysql://root:password@localhost:3306/prod_db
```

✅ **Good application.yml:**

```yaml
aws:
  accessKeyId: ${AWS_ACCESS_KEY_ID}
  secretAccessKey: ${AWS_SECRET_ACCESS_KEY}

database:
  url: ${DATABASE_URL:jdbc:h2:mem:testdb}
```

## Gradle Integration

### Task Dependencies

```kotlin
// Make deployment depend on security scan
tasks.named("deploy") {
    dependsOn("scanForSecrets")
}

// Custom task that requires clean security scan
tasks.register("secureRelease") {
    dependsOn("scanForSecrets", "build")
    doLast {
        println("Release build completed with security verification")
    }
}
```

### Conditional Execution

```kotlin
// Only run scan for certain tasks
tasks.named("scanForSecrets") {
    onlyIf {
        project.gradle.startParameter.taskNames.any { 
            it.contains("release") || it.contains("deploy") 
        }
    }
}
```

## Testing the Setup

Create a test file to verify SCAN is working:

### test-secrets.kt

```kotlin
// This file is for testing SCAN detection - DO NOT COMMIT REAL SECRETS

val testSecrets = mapOf(
    "aws_key" to "AKIAIOSFODNN7EXAMPLE",
    "github_token" to "ghp_1234567890abcdef1234567890abcdef12345678",
    "slack_token" to "xoxb-1234567890-1234567890-abcdefghijklmnopqrstuvwx",
    "jwt" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
)
```

Run scan:

```bash
./gradlew scanForSecrets
```

Expected: SCAN should detect multiple secrets in this file.

## Next Steps

- Review the [Configuration Reference](../configuration-reference.md) for advanced options
- See [CI/CD Integration Examples](../ci-cd-integration/) for automated scanning
- Check [Custom Patterns Examples](../custom-patterns/) for organization-specific secrets
