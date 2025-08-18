import React from "react"
import Link from "next/link"

export default function DocsIndex() {
    return (
        <div className="container mx-auto px-4 sm:px-6 py-8 sm:py-12 max-w-4xl">
            <div className="prose prose-lg dark:prose-invert max-w-none">
                <h1>Documentation</h1>
                <p className="text-xl text-gray-600 dark:text-gray-300">
                    Find quickstart, configuration reference and CI examples for the SCAN Gradle Plugin.
                </p>

                <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3 mt-8 not-prose">
                    {/* Getting Started */}
                    <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-6 hover:border-primary-300 dark:hover:border-primary-600 transition-colors">
                        <div className="flex items-center mb-3">
                            <div className="bg-green-100 dark:bg-green-900 p-2 rounded-lg mr-3">
                                <svg className="w-6 h-6 text-green-600 dark:text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                                </svg>
                            </div>
                            <h3 className="text-lg font-semibold">Getting Started</h3>
                        </div>
                        <p className="text-gray-600 dark:text-gray-300 mb-4">Quick setup and installation guide to get SCAN running in your project.</p>
                        <Link href="/docs/getting-started" className="text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300 font-medium">
                            Get Started →
                        </Link>
                    </div>

                    {/* User Guide */}
                    <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-6 hover:border-primary-300 dark:hover:border-primary-600 transition-colors">
                        <div className="flex items-center mb-3">
                            <div className="bg-blue-100 dark:bg-blue-900 p-2 rounded-lg mr-3">
                                <svg className="w-6 h-6 text-blue-600 dark:text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.746 0 3.332.477 4.5 1.253v13C19.832 18.477 18.246 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                                </svg>
                            </div>
                            <h3 className="text-lg font-semibold">User Guide</h3>
                        </div>
                        <p className="text-gray-600 dark:text-gray-300 mb-4">Comprehensive guide with examples and best practices for using SCAN effectively.</p>
                        <Link href="/docs/user-guide" className="text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300 font-medium">
                            Read Guide →
                        </Link>
                    </div>

                    {/* Configuration */}
                    <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-6 hover:border-primary-300 dark:hover:border-primary-600 transition-colors">
                        <div className="flex items-center mb-3">
                            <div className="bg-purple-100 dark:bg-purple-900 p-2 rounded-lg mr-3">
                                <svg className="w-6 h-6 text-purple-600 dark:text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                </svg>
                            </div>
                            <h3 className="text-lg font-semibold">Configuration</h3>
                        </div>
                        <p className="text-gray-600 dark:text-gray-300 mb-4">Complete reference for all configuration options and settings.</p>
                        <Link href="/docs/configuration" className="text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300 font-medium">
                            Configure →
                        </Link>
                    </div>

                    {/* Pattern Reference */}
                    <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-6 hover:border-primary-300 dark:hover:border-primary-600 transition-colors">
                        <div className="flex items-center mb-3">
                            <div className="bg-yellow-100 dark:bg-yellow-900 p-2 rounded-lg mr-3">
                                <svg className="w-6 h-6 text-yellow-600 dark:text-yellow-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                                </svg>
                            </div>
                            <h3 className="text-lg font-semibold">Pattern Reference</h3>
                        </div>
                        <p className="text-gray-600 dark:text-gray-300 mb-4">Built-in patterns and guide for creating custom detection patterns.</p>
                        <Link href="/docs/patterns" className="text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300 font-medium">
                            View Patterns →
                        </Link>
                    </div>

                    {/* Basic Usage */}
                    <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-6 hover:border-primary-300 dark:hover:border-primary-600 transition-colors">
                        <div className="flex items-center mb-3">
                            <div className="bg-indigo-100 dark:bg-indigo-900 p-2 rounded-lg mr-3">
                                <svg className="w-6 h-6 text-indigo-600 dark:text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zM21 5a2 2 0 00-2-2h-4a2 2 0 00-2 2v12a4 4 0 004 4h4a2 2 0 002-2V5z" />
                                </svg>
                            </div>
                            <h3 className="text-lg font-semibold">Basic Usage</h3>
                        </div>
                        <p className="text-gray-600 dark:text-gray-300 mb-4">Simple examples and common patterns to get you started quickly.</p>
                        <Link href="/docs/basic-usage" className="text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300 font-medium">
                            See Examples →
                        </Link>
                    </div>

                    {/* CI/CD Examples */}
                    <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-6 hover:border-primary-300 dark:hover:border-primary-600 transition-colors">
                        <div className="flex items-center mb-3">
                            <div className="bg-red-100 dark:bg-red-900 p-2 rounded-lg mr-3">
                                <svg className="w-6 h-6 text-red-600 dark:text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" />
                                </svg>
                            </div>
                            <h3 className="text-lg font-semibold">CI/CD Examples</h3>
                        </div>
                        <p className="text-gray-600 dark:text-gray-300 mb-4">Integration examples for GitHub Actions, Jenkins, GitLab CI and more.</p>
                        <Link href="/docs/ci" className="text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300 font-medium">
                            Integrate →
                        </Link>
                    </div>
                </div>

                <div className="mt-12 bg-gray-50 dark:bg-gray-800 rounded-lg p-8">
                    <h2 className="text-2xl font-bold mb-4">Quick Reference</h2>
                    <div className="grid gap-6 md:grid-cols-2">
                        <div>
                            <h3 className="text-lg font-semibold mb-3">Installation</h3>
                            <pre className="bg-gray-900 text-white p-3 rounded text-sm overflow-x-auto"><code>{`plugins {
    id("io.github.theaniketraj.scan") version "1.0.0"
}`}</code></pre>
                        </div>
                        <div>
                            <h3 className="text-lg font-semibold mb-3">Basic Usage</h3>
                            <pre className="bg-gray-900 text-white p-3 rounded text-sm overflow-x-auto"><code>{`# Run security scan
./gradlew scanForSecrets

# With verbose output
./gradlew scanForSecrets --info`}</code></pre>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}