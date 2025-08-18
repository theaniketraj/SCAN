import React from "react"
import Link from "next/link"
import DocsLayout from "../../../components/DocsLayout"

export default function UserGuidePage() {
    const sections = [
        { id: "overview", title: "Overview" },
        { id: "installation", title: "Installation" },
        { id: "basic-usage", title: "Basic Usage" },
        { id: "configuration", title: "Configuration" },
        { id: "patterns", title: "Understanding Patterns" },
        { id: "custom-patterns", title: "Custom Patterns" },
        { id: "output-formats", title: "Output Formats" },
        { id: "exclusions", title: "File Exclusions" },
        { id: "ci-integration", title: "CI Integration" },
        { id: "troubleshooting", title: "Troubleshooting" },
        { id: "best-practices", title: "Best Practices" }
    ]

    return (
        <DocsLayout sections={sections} title="User Guide">
            <div className="prose prose-lg dark:prose-invert max-w-none">
                <h1>SCAN Plugin User Guide</h1>
                
                <p className="text-xl text-gray-600 dark:text-gray-300">
                    Welcome to the comprehensive user guide for the SCAN (Sensitive Code Analyzer for Nerds) Gradle Plugin. 
                    This guide will walk you through everything you need to know to secure your codebase effectively.
                </p>

                <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-6 my-8">
                    <h3 className="text-blue-800 dark:text-blue-200 mt-0">Table of Contents</h3>
                    <ol className="text-blue-700 dark:text-blue-300 space-y-1">
                        <li><a href="#quick-start" className="hover:text-blue-900 dark:hover:text-blue-100">Quick Start</a></li>
                        <li><a href="#installation" className="hover:text-blue-900 dark:hover:text-blue-100">Installation</a></li>
                        <li><a href="#basic-configuration" className="hover:text-blue-900 dark:hover:text-blue-100">Basic Configuration</a></li>
                        <li><a href="#advanced-configuration" className="hover:text-blue-900 dark:hover:text-blue-100">Advanced Configuration</a></li>
                        <li><a href="#understanding-results" className="hover:text-blue-900 dark:hover:text-blue-100">Understanding Results</a></li>
                        <li><a href="#build-integration" className="hover:text-blue-900 dark:hover:text-blue-100">Build Lifecycle Integration</a></li>
                        <li><a href="#cicd-integration" className="hover:text-blue-900 dark:hover:text-blue-100">CI/CD Integration</a></li>
                        <li><a href="#troubleshooting" className="hover:text-blue-900 dark:hover:text-blue-100">Troubleshooting</a></li>
                        <li><a href="#best-practices" className="hover:text-blue-900 dark:hover:text-blue-100">Best Practices</a></li>
                    </ol>
                </div>

                <h2 id="quick-start">Quick Start</h2>
                
                <p>The fastest way to get started with SCAN is to add it to your <code>build.gradle.kts</code> and run a scan:</p>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`plugins {
    id("io.github.theaniketraj.scan") version "1.0.0"
}`}</code></pre>

                <p>Run your first scan:</p>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>./gradlew scanForSecrets</code></pre>

                <p>That&apos;s it! SCAN will analyze your codebase with sensible defaults and report any potential security issues.</p>

                <h2 id="installation">Installation</h2>

                <h3>Gradle Kotlin DSL (build.gradle.kts)</h3>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`plugins {
    id("io.github.theaniketraj.scan") version "1.0.0"
}`}</code></pre>

                <h3>Gradle Groovy DSL (build.gradle)</h3>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`plugins {
    id 'io.github.theaniketraj.scan' version '1.0.0'
}`}</code></pre>

                <h3>Legacy Plugin Application</h3>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath("io.github.theaniketraj:scan-gradle-plugin:1.0.0")
    }
}

apply(plugin = "io.github.theaniketraj.scan")`}</code></pre>

                <h2 id="basic-configuration">Basic Configuration</h2>

                <h3>Minimal Configuration</h3>

                <p>SCAN works out of the box with zero configuration:</p>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`// No configuration needed - SCAN uses intelligent defaults`}</code></pre>

                <h3>Common Configurations</h3>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`scan {
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
}`}</code></pre>

                <h2 id="understanding-results">Understanding Results</h2>

                <h3>Console Output</h3>

                <p>SCAN provides clear, actionable output:</p>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto text-sm"><code>{`> Task :scanForSecrets
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
   - Scan duration: 2.3s`}</code></pre>

                <h2 id="cicd-integration">CI/CD Integration</h2>

                <h3>GitHub Actions</h3>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`name: Security Scan
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
        path: build/reports/scan/`}</code></pre>

                <h2 id="best-practices">Best Practices</h2>

                <h3>Development Workflow</h3>

                <ol>
                    <li><strong>Run locally first</strong>: Always test SCAN locally before pushing</li>
                    <li><strong>Start permissive</strong>: Begin with <code>failOnSecrets = false</code> to understand findings</li>
                    <li><strong>Iterate gradually</strong>: Slowly tighten security as you clean up existing issues</li>
                    <li><strong>Document exceptions</strong>: Use comments to explain why certain patterns are safe</li>
                </ol>

                <h3>Security Hygiene</h3>

                <ol>
                    <li><strong>Regular scans</strong>: Run SCAN on every commit</li>
                    <li><strong>Review findings</strong>: Don&apos;t just ignore warnings</li>
                    <li><strong>Update patterns</strong>: Keep custom patterns current</li>
                    <li><strong>Document decisions</strong>: Record why certain patterns are excluded</li>
                </ol>

                <div className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-6 my-8">
                    <h3 className="text-green-800 dark:text-green-200 mt-0">Next Steps</h3>
                    <ul className="text-green-700 dark:text-green-300 space-y-2">
                        <li><Link href="/docs/configuration" className="hover:text-green-900 dark:hover:text-green-100">Configuration Reference</Link>: Detailed explanation of all configuration options</li>
                        <li><Link href="/docs/patterns" className="hover:text-green-900 dark:hover:text-green-100">Pattern Reference</Link>: Complete list of built-in patterns and how to create custom ones</li>
                        <li><Link href="/docs/ci" className="hover:text-green-900 dark:hover:text-green-100">CI/CD Examples</Link>: Real-world usage examples and integrations</li>
                    </ul>
                </div>
            </div>
        </DocsLayout>
    )
}
