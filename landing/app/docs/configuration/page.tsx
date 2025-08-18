import React from "react"
import Link from "next/link"
import DocsLayout from "../../../components/DocsLayout"

export default function ConfigurationPage() {
    const sections = [
        { id: "overview", title: "Overview" },
        { id: "basic-config", title: "Basic Configuration" },
        { id: "scan-settings", title: "Scan Settings" },
        { id: "output-config", title: "Output Configuration" },
        { id: "pattern-config", title: "Pattern Configuration" },
        { id: "performance", title: "Performance Settings" },
        { id: "exclusions", title: "File Exclusions" },
        { id: "environment", title: "Environment Variables" },
        { id: "examples", title: "Complete Examples" }
    ]

    return (
        <DocsLayout sections={sections} title="Configuration Reference">
            <div className="prose prose-lg dark:prose-invert max-w-none">
                <h1>SCAN Plugin Configuration Reference</h1>
                
                <p className="text-xl text-gray-600 dark:text-gray-300">
                    This document provides a comprehensive reference for all configuration options available in the SCAN Gradle Plugin.
                </p>

                <h2 id="overview">Configuration Overview</h2>
                
                <p>All SCAN configuration is done through the <code>scan</code> extension block in your <code>build.gradle.kts</code>:</p>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`scan {
    // Configuration options go here
}`}</code></pre>

                <h2 id="basic-config">Basic Configuration</h2>

                <div className="space-y-6">
                    <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-6">
                        <h3 className="mt-0"><code>failOnSecrets</code></h3>
                        <ul className="space-y-1">
                            <li><strong>Type:</strong> <code>Boolean</code></li>
                            <li><strong>Default:</strong> <code>true</code></li>
                            <li><strong>Description:</strong> Fail the build when secrets are detected</li>
                        </ul>
                        <pre className="bg-gray-900 text-white p-3 rounded mt-3 text-sm"><code>{`scan {
    failOnSecrets = false  // Only warn, don't fail build
}`}</code></pre>
                    </div>

                    <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-6">
                        <h3 className="mt-0"><code>verbose</code></h3>
                        <ul className="space-y-1">
                            <li><strong>Type:</strong> <code>Boolean</code></li>
                            <li><strong>Default:</strong> <code>false</code></li>
                            <li><strong>Description:</strong> Enable detailed console output</li>
                        </ul>
                        <pre className="bg-gray-900 text-white p-3 rounded mt-3 text-sm"><code>{`scan {
    verbose = true  // Show detailed scanning progress
}`}</code></pre>
                    </div>

                    <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-6">
                        <h3 className="mt-0"><code>warnOnSecrets</code></h3>
                        <ul className="space-y-1">
                            <li><strong>Type:</strong> <code>Boolean</code></li>
                            <li><strong>Default:</strong> <code>true</code></li>
                            <li><strong>Description:</strong> Show warnings even when not failing the build</li>
                        </ul>
                        <pre className="bg-gray-900 text-white p-3 rounded mt-3 text-sm"><code>{`scan {
    warnOnSecrets = false  // Suppress warning messages
}`}</code></pre>
                    </div>
                </div>

                <h2 id="pattern-config">Pattern Configuration</h2>

                <div className="space-y-6">
                    <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-6">
                        <h3 className="mt-0"><code>includePatterns</code></h3>
                        <ul className="space-y-1">
                            <li><strong>Type:</strong> <code>Set&lt;String&gt;</code></li>
                            <li><strong>Default:</strong> Common source file patterns</li>
                            <li><strong>Description:</strong> Ant-style patterns for files to include in scanning</li>
                        </ul>
                        <pre className="bg-gray-900 text-white p-3 rounded mt-3 text-sm"><code>{`scan {
    includePatterns = setOf(
        "src/**/*.kt",
        "config/**/*.yml",
        "*.properties"
    )
}`}</code></pre>
                    </div>

                    <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-6">
                        <h3 className="mt-0"><code>excludePatterns</code></h3>
                        <ul className="space-y-1">
                            <li><strong>Type:</strong> <code>Set&lt;String&gt;</code></li>
                            <li><strong>Default:</strong> Build directories and generated files</li>
                            <li><strong>Description:</strong> Ant-style patterns for files to exclude from scanning</li>
                        </ul>
                        <pre className="bg-gray-900 text-white p-3 rounded mt-3 text-sm"><code>{`scan {
    excludePatterns = setOf(
        "**/build/**",
        "**/test-data/**",
        "**/*.generated.*"
    )
}`}</code></pre>
                    </div>

                    <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-6">
                        <h3 className="mt-0"><code>scanTests</code></h3>
                        <ul className="space-y-1">
                            <li><strong>Type:</strong> <code>Boolean</code></li>
                            <li><strong>Default:</strong> <code>true</code></li>
                            <li><strong>Description:</strong> Whether to scan test directories</li>
                        </ul>
                        <pre className="bg-gray-900 text-white p-3 rounded mt-3 text-sm"><code>{`scan {
    scanTests = false  // Skip test directories entirely
}`}</code></pre>
                    </div>
                </div>

                <h2 id="scan-settings">Scan Settings</h2>

                <div className="space-y-6">
                    <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-6">
                        <h3 className="mt-0"><code>strictMode</code></h3>
                        <ul className="space-y-1">
                            <li><strong>Type:</strong> <code>Boolean</code></li>
                            <li><strong>Default:</strong> <code>false</code></li>
                            <li><strong>Description:</strong> Enable all detectors with maximum sensitivity</li>
                        </ul>
                        <pre className="bg-gray-900 text-white p-3 rounded mt-3 text-sm"><code>{`scan {
    strictMode = true  // Maximum security, may increase false positives
}`}</code></pre>
                    </div>

                    <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-6">
                        <h3 className="mt-0"><code>entropyThreshold</code></h3>
                        <ul className="space-y-1">
                            <li><strong>Type:</strong> <code>Double</code></li>
                            <li><strong>Default:</strong> <code>4.5</code></li>
                            <li><strong>Description:</strong> Minimum entropy threshold for random string detection (0.0-8.0)</li>
                        </ul>
                        <pre className="bg-gray-900 text-white p-3 rounded mt-3 text-sm"><code>{`scan {
    entropyThreshold = 5.0  // Higher = fewer false positives
}`}</code></pre>
                    </div>

                    <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-6">
                        <h3 className="mt-0"><code>customPatterns</code></h3>
                        <ul className="space-y-1">
                            <li><strong>Type:</strong> <code>List&lt;String&gt;</code></li>
                            <li><strong>Default:</strong> <code>emptyList()</code></li>
                            <li><strong>Description:</strong> Custom regex patterns for organization-specific secrets</li>
                        </ul>
                        <pre className="bg-gray-900 text-white p-3 rounded mt-3 text-sm"><code>{`scan {
    customPatterns = listOf(
        "MYCOMPANY_API_[A-Z0-9]{32}",
        "INTERNAL_SECRET_.*"
    )
}`}</code></pre>
                    </div>
                </div>

                <h2 id="output-config">Output Configuration</h2>

                <div className="space-y-6">
                    <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-6">
                        <h3 className="mt-0"><code>generateHtmlReport</code></h3>
                        <ul className="space-y-1">
                            <li><strong>Type:</strong> <code>Boolean</code></li>
                            <li><strong>Default:</strong> <code>false</code></li>
                            <li><strong>Description:</strong> Generate an HTML report with detailed findings</li>
                        </ul>
                        <pre className="bg-gray-900 text-white p-3 rounded mt-3 text-sm"><code>{`scan {
    generateHtmlReport = true
}`}</code></pre>
                    </div>

                    <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-6">
                        <h3 className="mt-0"><code>generateJsonReport</code></h3>
                        <ul className="space-y-1">
                            <li><strong>Type:</strong> <code>Boolean</code></li>
                            <li><strong>Default:</strong> <code>false</code></li>
                            <li><strong>Description:</strong> Generate a JSON report for CI/CD integration</li>
                        </ul>
                        <pre className="bg-gray-900 text-white p-3 rounded mt-3 text-sm"><code>{`scan {
    generateJsonReport = true
}`}</code></pre>
                    </div>
                </div>

                <h2 id="performance">Performance Settings</h2>
                
                <div className="space-y-6">
                    <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-6">
                        <h3 className="mt-0"><code>maxThreads</code></h3>
                        <ul className="space-y-1">
                            <li><strong>Type:</strong> <code>Integer</code></li>
                            <li><strong>Default:</strong> <code>4</code></li>
                            <li><strong>Description:</strong> Maximum number of threads for parallel scanning</li>
                        </ul>
                        <pre className="bg-gray-900 text-white p-3 rounded mt-3 text-sm"><code>{`scan {
    maxThreads = 8  // Use more threads for faster scanning
}`}</code></pre>
                    </div>
                </div>

                <h2 id="exclusions">File Exclusions</h2>
                
                <div className="space-y-6">
                    <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-6">
                        <h3 className="mt-0"><code>ignoreTestFiles</code></h3>
                        <ul className="space-y-1">
                            <li><strong>Type:</strong> <code>Boolean</code></li>
                            <li><strong>Default:</strong> <code>true</code></li>
                            <li><strong>Description:</strong> Skip scanning test files and directories</li>
                        </ul>
                        <pre className="bg-gray-900 text-white p-3 rounded mt-3 text-sm"><code>{`scan {
    ignoreTestFiles = false  // Include test files in scanning
}`}</code></pre>
                    </div>
                </div>

                <h2 id="environment">Environment Variables</h2>
                
                <div className="space-y-6">
                    <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-6">
                        <p>SCAN supports various environment variables for configuration:</p>
                        <ul className="space-y-2">
                            <li><code>SCAN_FAIL_ON_SECRETS</code> - Override failOnSecrets setting</li>
                            <li><code>SCAN_VERBOSE</code> - Enable verbose output</li>
                            <li><code>SCAN_OUTPUT_DIR</code> - Set custom output directory</li>
                        </ul>
                    </div>
                </div>

                <h2 id="examples">Complete Examples</h2>

                <h3>Development-Friendly Setup</h3>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`scan {
    failOnSecrets = false
    warnOnSecrets = true
    verbose = true
    generateHtmlReport = true
    ignoreTestFiles = true
}`}</code></pre>

                <h3>High-Security Production Setup</h3>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`scan {
    strictMode = true
    failOnSecrets = true
    entropyThreshold = 4.0
    generateHtmlReport = true
    generateJsonReport = true
    
    customPatterns = listOf(
        "COMPANY_SECRET_[A-Z0-9]{32}",
        "INTERNAL_API_KEY_.*"
    )
}`}</code></pre>

                <h3>CI/CD Optimized Setup</h3>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`scan {
    val isCI = System.getenv("CI")?.toBoolean() ?: false
    
    failOnSecrets = isCI
    warnOnSecrets = true
    generateJsonReport = isCI
    generateHtmlReport = !isCI
    verbose = isCI
}`}</code></pre>

                <div className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-6 my-8">
                    <h3 className="text-green-800 dark:text-green-200 mt-0">Related Documentation</h3>
                    <ul className="text-green-700 dark:text-green-300 space-y-2">
                        <li><Link href="/docs/user-guide" className="hover:text-green-900 dark:hover:text-green-100">User Guide</Link>: Complete walkthrough with examples</li>
                        <li><Link href="/docs/patterns" className="hover:text-green-900 dark:hover:text-green-100">Pattern Reference</Link>: Built-in patterns and custom pattern creation</li>
                        <li><Link href="/docs/ci" className="hover:text-green-900 dark:hover:text-green-100">CI/CD Examples</Link>: Integration with popular CI/CD platforms</li>
                    </ul>
                </div>
            </div>
        </DocsLayout>
    )
}