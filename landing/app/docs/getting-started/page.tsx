import React from "react"
import Link from "next/link"
import DocsLayout from "../../../components/DocsLayout"

export default function GettingStartedPage() {
    const sections = [
        { id: "introduction", title: "Introduction" },
        { id: "requirements", title: "Requirements" },
        { id: "installation", title: "Installation" },
        { id: "first-scan", title: "First Scan" },
        { id: "configuration", title: "Basic Configuration" },
        { id: "understanding-results", title: "Understanding Results" },
        { id: "next-steps", title: "Next Steps" }
    ]

    return (
        <DocsLayout sections={sections} title="Getting Started">
            <div className="prose prose-lg dark:prose-invert max-w-none">
                <h1>Getting Started</h1>
                <p className="text-xl text-gray-600 dark:text-gray-300">
                    Get up and running with SCAN in minutes. This guide will help you install, configure, and run your first security scan.
                </p>

                <section id="introduction">
                    <h2>Introduction</h2>
                    <p>
                        SCAN (Sensitive Code Analyzer for Nerds) is a powerful Gradle plugin designed to automatically detect 
                        secrets, API keys, and other sensitive information in your codebase. It integrates seamlessly into your 
                        build process and helps prevent accidental exposure of sensitive data.
                    </p>
                    
                    <div className="bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-700 rounded-lg p-4 my-6">
                        <h3 className="text-green-800 dark:text-green-200 text-lg font-semibold mb-2">Key Features</h3>
                        <ul className="text-green-700 dark:text-green-300 mb-0">
                            <li>🔍 Comprehensive pattern detection for various secret types</li>
                            <li>⚡ Fast scanning with minimal performance impact</li>
                            <li>🛠️ Highly configurable with custom patterns</li>
                            <li>📊 Multiple output formats (console, JSON, HTML)</li>
                            <li>🔧 CI/CD integration ready</li>
                        </ul>
                    </div>
                </section>

                <section id="requirements">
                    <h2>Requirements</h2>
                    <p>Before installing SCAN, ensure your environment meets these requirements:</p>
                    
                    <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-4 my-4">
                        <h3 className="font-semibold mb-3">System Requirements</h3>
                        <ul className="space-y-2 text-sm">
                            <li><strong>Java:</strong> JDK 17 or higher</li>
                            <li><strong>Gradle:</strong> 7.0 or higher</li>
                            <li><strong>Operating System:</strong> Windows, macOS, or Linux</li>
                            <li><strong>Memory:</strong> Minimum 1GB RAM (2GB+ recommended for large projects)</li>
                        </ul>
                    </div>

                    <p>You can check your current versions:</p>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`# Check Java version
java -version

# Check Gradle version
gradle --version

# Or using Gradle wrapper
./gradlew --version`}</code></pre>
                </section>

                <section id="installation">
                    <h2>Installation</h2>
                    <p>
                        Add the SCAN plugin to your <code>build.gradle.kts</code> or <code>build.gradle</code> file:
                    </p>

                    <h3>Kotlin DSL (build.gradle.kts)</h3>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`plugins {
    id("io.github.theaniketraj.scan") version "2.0.0"
}`}</code></pre>

                    <h3>Groovy DSL (build.gradle)</h3>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`plugins {
    id 'io.github.theaniketraj.scan' version '2.0.0'
}`}</code></pre>

                    <h3>Legacy Plugin Application</h3>
                    <p>If you&apos;re using an older Gradle version, you can use the legacy plugin syntax:</p>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath "io.github.theaniketraj:scan-gradle-plugin:2.0.0"
    }
}

apply plugin: "io.github.theaniketraj.scan"`}</code></pre>

                    <div className="bg-blue-50 dark:bg-blue-900/30 border border-blue-200 dark:border-blue-700 rounded-lg p-4 my-6">
                        <h4 className="text-blue-800 dark:text-blue-200 font-semibold mb-2">💡 Pro Tip</h4>
                        <p className="text-blue-700 dark:text-blue-300 mb-0">
                            Always use the latest version for the best security patterns and performance improvements. 
                            Check the <a href="https://github.com/theaniketraj/SCAN/releases" className="underline">releases page</a> for updates.
                        </p>
                    </div>
                </section>

                <section id="first-scan">
                    <h2>First Scan</h2>
                    <p>
                        Once the plugin is installed, you can run your first security scan:
                    </p>

                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`# Run a basic scan
./gradlew scanForSecrets

# Run with verbose output
./gradlew scanForSecrets --info

# Run with debug information
./gradlew scanForSecrets --debug`}</code></pre>

                    <p>The scan will:</p>
                    <ul>
                        <li>🔍 Analyze all source files in your project</li>
                        <li>🎯 Apply built-in patterns to detect common secrets</li>
                        <li>📝 Generate a detailed report</li>
                        <li>⚠️ Display findings in the console</li>
                    </ul>

                    <h3>Understanding the Output</h3>
                    <p>A typical scan output looks like this:</p>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`> Task :scanForSecrets
Starting security scan...
Scanning 127 files...

🔍 SCAN Results:
┌─────────────────────────────────────────────────┐
│ Found 2 potential secrets in 1 file(s)          │
├─────────────────────────────────────────────────┤
│ File: src/main/java/Config.java                 │
│ Line: 15                                        │
│ Type: API Key                                   │
│ Match: sk_test_51H...                           │
├─────────────────────────────────────────────────┤
│ File: src/main/java/Config.java                 │
│ Line: 23                                        │
│ Type: Database Password                         │
│ Match: password=secret123                       │
└─────────────────────────────────────────────────┘

BUILD FAILED`}</code></pre>
                </section>

                <section id="configuration">
                    <h2>Basic Configuration</h2>
                    <p>
                        Customize SCAN&apos;s behavior by adding a configuration block to your build file:
                    </p>

                    <h3>Essential Configuration</h3>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`scan {
    // Fail the build when secrets are detected
    failOnDetection = true
    
    // Choose output format: "console", "json", "html"
    reportFormat = "console"
    
    // Specify output directory
    outputDir = "scan-results"
    
    // Set scan sensitivity: "low", "medium", "high"
    sensitivity = "medium"
}`}</code></pre>

                    <h3>Advanced Configuration</h3>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`scan {
    failOnDetection = true
    reportFormat = "json"
    outputDir = "security-reports"
    
    // Exclude specific files or directories
    excludePaths = [
        "**/test/**",
        "**/build/**",
        "**/*.min.js"
    ]
    
    // Include only specific file types
    includePaths = [
        "**/*.java",
        "**/*.kt",
        "**/*.properties"
    ]
    
    // Custom patterns file
    customPatternsFile = "config/custom-patterns.yml"
    
    // Enable parallel processing
    parallel = true
    
    // Set maximum file size (in MB)
    maxFileSize = 10
}`}</code></pre>

                    <div className="bg-yellow-50 dark:bg-yellow-900/30 border border-yellow-200 dark:border-yellow-700 rounded-lg p-4 my-6">
                        <h4 className="text-yellow-800 dark:text-yellow-200 font-semibold mb-2">⚠️ Important</h4>
                        <p className="text-yellow-700 dark:text-yellow-300 mb-0">
                            Setting <code>failOnDetection = true</code> will cause your build to fail when secrets are detected. 
                            This is recommended for CI/CD pipelines to prevent deployment of code with secrets.
                        </p>
                    </div>
                </section>

                <section id="understanding-results">
                    <h2>Understanding Results</h2>
                    <p>
                        SCAN provides detailed information about detected secrets to help you understand and address security issues.
                    </p>

                    <h3>Result Components</h3>
                    <div className="grid gap-4 md:grid-cols-2 my-6">
                        <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-4">
                            <h4 className="font-semibold mb-2">📁 File Path</h4>
                            <p className="text-sm text-gray-600 dark:text-gray-400">
                                Exact location of the file containing the potential secret
                            </p>
                        </div>
                        <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-4">
                            <h4 className="font-semibold mb-2">📍 Line Number</h4>
                            <p className="text-sm text-gray-600 dark:text-gray-400">
                                Specific line where the secret was detected
                            </p>
                        </div>
                        <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-4">
                            <h4 className="font-semibold mb-2">🏷️ Secret Type</h4>
                            <p className="text-sm text-gray-600 dark:text-gray-400">
                                Classification of the detected secret (API Key, Password, etc.)
                            </p>
                        </div>
                        <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-4">
                            <h4 className="font-semibold mb-2">🎯 Match Pattern</h4>
                            <p className="text-sm text-gray-600 dark:text-gray-400">
                                Partial match showing the detected pattern (sensitive parts redacted)
                            </p>
                        </div>
                    </div>

                    <h3>Report Formats</h3>
                    <p>Choose the format that best suits your workflow:</p>
                    <ul>
                        <li><strong>Console:</strong> Human-readable output for development</li>
                        <li><strong>JSON:</strong> Machine-readable for CI/CD integration</li>
                        <li><strong>HTML:</strong> Detailed web-based report with navigation</li>
                    </ul>
                </section>

                <section id="next-steps">
                    <h2>Next Steps</h2>
                    <p>
                        Now that you have SCAN running, explore these topics to get the most out of the plugin:
                    </p>

                    <div className="grid gap-4 md:grid-cols-2 my-8">
                        <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-6">
                            <h3 className="text-lg font-semibold mb-3">📖 Comprehensive Guide</h3>
                            <p className="text-gray-600 dark:text-gray-300 mb-4 text-sm">
                                Learn advanced features, best practices, and troubleshooting tips.
                            </p>
                            <Link href="/docs/user-guide" className="text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300 font-medium text-sm">
                                Read User Guide →
                            </Link>
                        </div>

                        <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-6">
                            <h3 className="text-lg font-semibold mb-3">⚙️ Advanced Configuration</h3>
                            <p className="text-gray-600 dark:text-gray-300 mb-4 text-sm">
                                Customize patterns, exclusions, and output formats for your needs.
                            </p>
                            <Link href="/docs/configuration" className="text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300 font-medium text-sm">
                                View Configuration →
                            </Link>
                        </div>

                        <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-6">
                            <h3 className="text-lg font-semibold mb-3">🔍 Pattern Reference</h3>
                            <p className="text-gray-600 dark:text-gray-300 mb-4 text-sm">
                                Understand built-in patterns and create custom detection rules.
                            </p>
                            <Link href="/docs/patterns" className="text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300 font-medium text-sm">
                                Explore Patterns →
                            </Link>
                        </div>

                        <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-6">
                            <h3 className="text-lg font-semibold mb-3">🔧 CI/CD Integration</h3>
                            <p className="text-gray-600 dark:text-gray-300 mb-4 text-sm">
                                Integrate SCAN into your automated build and deployment pipelines.
                            </p>
                            <Link href="/docs/ci" className="text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300 font-medium text-sm">
                                Setup CI/CD →
                            </Link>
                        </div>
                    </div>

                    <div className="bg-blue-50 dark:bg-blue-900/30 border border-blue-200 dark:border-blue-700 rounded-lg p-6 my-8">
                        <h3 className="text-blue-800 dark:text-blue-200 text-lg font-semibold mb-3">🚀 Quick Start Checklist</h3>
                        <div className="space-y-2 text-blue-700 dark:text-blue-300">
                            <label className="flex items-center">
                                <input type="checkbox" className="mr-2" /> Plugin installed and configured
                            </label>
                            <label className="flex items-center">
                                <input type="checkbox" className="mr-2" /> First scan completed successfully
                            </label>
                            <label className="flex items-center">
                                <input type="checkbox" className="mr-2" /> Build failure configured for CI/CD
                            </label>
                            <label className="flex items-center">
                                <input type="checkbox" className="mr-2" /> Exclusions configured for test files
                            </label>
                            <label className="flex items-center">
                                <input type="checkbox" className="mr-2" /> Team notified of new security checks
                            </label>
                        </div>
                    </div>
                </section>
            </div>
        </DocsLayout>
    )
}