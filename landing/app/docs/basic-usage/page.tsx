import React from "react"
import Link from "next/link"

export default function BasicUsagePage() {
    return (
        <div className="container mx-auto px-4 sm:px-6 py-8 sm:py-12 max-w-4xl">
            <nav className="mb-8">
                <Link href="/docs" className="text-primary-500 hover:text-primary-400 text-sm">← Back to Documentation</Link>
            </nav>

            <div className="prose prose-lg dark:prose-invert max-w-none">
                <h1>Basic Usage Examples</h1>
                
                <p className="text-xl text-gray-600 dark:text-gray-300">
                    This guide contains simple examples demonstrating basic SCAN plugin usage and common patterns to fix.
                </p>

                <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-6 my-8">
                    <h3 className="text-blue-800 dark:text-blue-200 mt-0">Table of Contents</h3>
                    <ol className="text-blue-700 dark:text-blue-300 space-y-1">
                        <li><a href="#quick-start" className="hover:text-blue-900 dark:hover:text-blue-100">Quick Start Example</a></li>
                        <li><a href="#configurations" className="hover:text-blue-900 dark:hover:text-blue-100">Configuration Examples</a></li>
                        <li><a href="#common-patterns" className="hover:text-blue-900 dark:hover:text-blue-100">Common Patterns to Fix</a></li>
                        <li><a href="#gradle-integration" className="hover:text-blue-900 dark:hover:text-blue-100">Gradle Integration</a></li>
                        <li><a href="#testing" className="hover:text-blue-900 dark:hover:text-blue-100">Testing the Setup</a></li>
                    </ol>
                </div>

                <h2 id="quick-start">Quick Start Example</h2>

                <h3>build.gradle.kts</h3>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`plugins {
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
}`}</code></pre>

                <h3>Example Source File (src/main/kotlin/Config.kt)</h3>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`package com.example

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
        val API_KEY = System.getenv("AWS_ACCESS_KEY_ID") 
            ?: throw IllegalStateException("Missing AWS_ACCESS_KEY_ID")
        val DATABASE_URL = System.getenv("DATABASE_URL") ?: "jdbc:h2:mem:testdb"
        val JWT_SECRET = System.getenv("JWT_SECRET") ?: generateRandomSecret()
        
        private fun generateRandomSecret(): String {
            // Generate secure random secret at runtime
            return "generated-at-runtime"
        }
    }
}`}</code></pre>

                <h3>Run the Scan</h3>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`# Run security scan
./gradlew scanForSecrets

# Run as part of build (scan runs automatically)
./gradlew build

# View detailed output
./gradlew scanForSecrets --info`}</code></pre>

                <h3>Expected Output</h3>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto text-sm"><code>{`> Task :scanForSecrets FAILED

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
> Security scan failed: 3 potential secrets detected`}</code></pre>

                <h2 id="configurations">Configuration Examples</h2>

                <h3>Lenient Configuration (Development)</h3>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`scan {
    // Don't fail build during development
    failOnSecrets = false
    warnOnSecrets = true
    
    // Be more permissive with test files
    ignoreTestFiles = true
    
    // Generate reports for review
    generateHtmlReport = true
    verbose = true
}`}</code></pre>

                <h3>Strict Configuration (Production)</h3>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`scan {
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
}`}</code></pre>

                <h3>Environment-Based Configuration</h3>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`scan {
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
}`}</code></pre>

                <h2 id="common-patterns">Common Patterns to Fix</h2>

                <h3>1. Hardcoded API Keys</h3>

                <div className="grid md:grid-cols-2 gap-6">
                    <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
                        <h4 className="text-red-800 dark:text-red-200 mt-0">❌ Bad:</h4>
                        <pre className="bg-gray-900 text-white p-3 rounded text-sm"><code>{`class ApiClient {
    private val apiKey = "sk_live_abcd1234567890abcd1234567890"
}`}</code></pre>
                    </div>

                    <div className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-4">
                        <h4 className="text-green-800 dark:text-green-200 mt-0">✅ Good:</h4>
                        <pre className="bg-gray-900 text-white p-3 rounded text-sm"><code>{`class ApiClient {
    private val apiKey = System.getenv("STRIPE_API_KEY") 
        ?: throw IllegalStateException("STRIPE_API_KEY environment variable not set")
}`}</code></pre>
                    </div>
                </div>

                <h3>2. Database Credentials</h3>

                <div className="grid md:grid-cols-2 gap-6">
                    <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
                        <h4 className="text-red-800 dark:text-red-200 mt-0">❌ Bad:</h4>
                        <pre className="bg-gray-900 text-white p-3 rounded text-sm"><code>{`val dataSource = HikariDataSource().apply {
    jdbcUrl = "jdbc:postgresql://localhost:5432/mydb"
    username = "admin"
    password = "supersecret123"
}`}</code></pre>
                    </div>

                    <div className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-4">
                        <h4 className="text-green-800 dark:text-green-200 mt-0">✅ Good:</h4>
                        <pre className="bg-gray-900 text-white p-3 rounded text-sm"><code>{`val dataSource = HikariDataSource().apply {
    jdbcUrl = System.getenv("DATABASE_URL") ?: "jdbc:h2:mem:testdb"
    username = System.getenv("DB_USERNAME") ?: "test"
    password = System.getenv("DB_PASSWORD") ?: "test"
}`}</code></pre>
                    </div>
                </div>

                <h3>3. Configuration Files</h3>

                <div className="grid md:grid-cols-2 gap-6">
                    <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
                        <h4 className="text-red-800 dark:text-red-200 mt-0">❌ Bad application.yml:</h4>
                        <pre className="bg-gray-900 text-white p-3 rounded text-sm"><code>{`aws:
  accessKeyId: AKIAIOSFODNN7EXAMPLE
  secretAccessKey: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

database:
  url: mysql://root:password@localhost:3306/prod_db`}</code></pre>
                    </div>

                    <div className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-4">
                        <h4 className="text-green-800 dark:text-green-200 mt-0">✅ Good application.yml:</h4>
                        <pre className="bg-gray-900 text-white p-3 rounded text-sm"><code>{`aws:
  accessKeyId: \${AWS_ACCESS_KEY_ID}
  secretAccessKey: \${AWS_SECRET_ACCESS_KEY}

database:
  url: \${DATABASE_URL:jdbc:h2:mem:testdb}`}</code></pre>
                    </div>
                </div>

                <h2 id="gradle-integration">Gradle Integration</h2>

                <h3>Task Dependencies</h3>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`// Make deployment depend on security scan
tasks.named("deploy") {
    dependsOn("scanForSecrets")
}

// Custom task that requires clean security scan
tasks.register("secureRelease") {
    dependsOn("scanForSecrets", "build")
    doLast {
        println("Release build completed with security verification")
    }
}`}</code></pre>

                <h3>Conditional Execution</h3>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`// Only run scan for certain tasks
tasks.named("scanForSecrets") {
    onlyIf {
        project.gradle.startParameter.taskNames.any { 
            it.contains("release") || it.contains("deploy") 
        }
    }
}`}</code></pre>

                <h2 id="testing">Testing the Setup</h2>

                <p>Create a test file to verify SCAN is working:</p>

                <h3>test-secrets.kt</h3>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`// This file is for testing SCAN detection - DO NOT COMMIT REAL SECRETS

val testSecrets = mapOf(
    "aws_key" to "AKIAIOSFODNN7EXAMPLE",
    "github_token" to "ghp_1234567890abcdef1234567890abcdef12345678",
    "slack_token" to "xoxb-1234567890-1234567890-abcdefghijklmnopqrstuvwx",
    "jwt" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
)`}</code></pre>

                <p>Run scan:</p>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>./gradlew scanForSecrets</code></pre>

                <p><strong>Expected:</strong> SCAN should detect multiple secrets in this file.</p>

                <div className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-6 my-8">
                    <h3 className="text-green-800 dark:text-green-200 mt-0">Next Steps</h3>
                    <ul className="text-green-700 dark:text-green-300 space-y-2">
                        <li><Link href="/docs/configuration" className="hover:text-green-900 dark:hover:text-green-100">Configuration Reference</Link>: Advanced configuration options</li>
                        <li><Link href="/docs/ci" className="hover:text-green-900 dark:hover:text-green-100">CI/CD Integration Examples</Link>: Automated scanning in pipelines</li>
                        <li><Link href="/docs/patterns" className="hover:text-green-900 dark:hover:text-green-100">Custom Patterns Examples</Link>: Organization-specific secrets</li>
                    </ul>
                </div>
            </div>
        </div>
    )
}
