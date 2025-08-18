import React from "react"
import Link from "next/link"
import DocsLayout from "../../../components/DocsLayout"

export default function ContributingPage() {
    const sections = [
        { id: "overview", title: "Overview" },
        { id: "getting-started", title: "Getting Started" },
        { id: "development-setup", title: "Development Setup" },
        { id: "contribution-types", title: "Types of Contributions" },
        { id: "code-standards", title: "Code Standards" },
        { id: "testing", title: "Testing Guidelines" },
        { id: "documentation", title: "Documentation" },
        { id: "pull-requests", title: "Pull Request Process" },
        { id: "issue-reporting", title: "Issue Reporting" },
        { id: "community", title: "Community" }
    ]

    return (
        <DocsLayout sections={sections} title="Contributing">
            <div className="prose prose-lg dark:prose-invert max-w-none">
                <h1>Contributing to SCAN</h1>
                
                <p className="text-xl text-gray-600 dark:text-gray-300">
                    Thank you for your interest in contributing to SCAN! This guide will help you get started 
                    with contributing to the project, from setting up your development environment to submitting your first pull request.
                </p>

                <div className="bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-700 rounded-lg p-6 my-8">
                    <h3 className="text-green-800 dark:text-green-200 text-lg font-semibold mb-3">🎯 Quick Start for Contributors</h3>
                    <div className="space-y-2 text-green-700 dark:text-green-300 text-sm">
                        <p>1. Fork the repository and clone it locally</p>
                        <p>2. Set up your development environment</p>
                        <p>3. Create a feature branch for your contribution</p>
                        <p>4. Make your changes and add tests</p>
                        <p>5. Submit a pull request with a clear description</p>
                    </div>
                </div>

                <section id="overview">
                    <h2>Overview</h2>
                    
                    <p>
                        SCAN (Sensitive Code Analyzer for Nerds) is an open-source Gradle plugin that helps developers 
                        detect secrets and sensitive information in their codebase. We welcome contributions from the 
                        community to make this tool even better.
                    </p>

                    <h3>Code of Conduct</h3>
                    <p>
                        This project adheres to a <a href="https://github.com/theaniketraj/SCAN/blob/main/CODE_OF_CONDUCT.md" target="_blank" 
                        className="text-primary-600 dark:text-primary-400 hover:underline">Code of Conduct ↗</a>. 
                        By participating, you are expected to uphold this code.
                    </p>
                </section>

                <section id="getting-started">
                    <h2>Getting Started</h2>

                    <h3>Prerequisites</h3>
                    <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-4 my-4">
                        <ul className="space-y-2 text-sm mb-0">
                            <li><strong>Java 21</strong> or higher</li>
                            <li><strong>Gradle 8.14+</strong> (wrapper included)</li>
                            <li><strong>Git</strong> for version control</li>
                            <li><strong>IDE</strong> (IntelliJ IDEA recommended)</li>
                        </ul>
                    </div>

                    <h3>Fork and Clone</h3>
                    <p>1. Fork the repository on GitHub</p>
                    <p>2. Clone your fork locally:</p>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`git clone https://github.com/theaniketraj/SCAN.git
cd SCAN`}</code></pre>

                    <p>3. Add the upstream remote:</p>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`git remote add upstream https://github.com/theaniketraj/SCAN.git`}</code></pre>
                </section>

                <section id="development-setup">
                    <h2>Development Setup</h2>

                    <h3>Project Setup</h3>
                    <p>1. Import the project into your IDE</p>
                    <p>2. Install dependencies:</p>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`./gradlew build`}</code></pre>

                    <p>3. Run tests to ensure everything works:</p>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`./gradlew test`}</code></pre>

                    <h3>Project Structure</h3>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto text-sm"><code>{`scan-gradle-plugin/
├── src/
│   ├── main/kotlin/com/scan/
│   │   ├── core/           # Core scanning logic
│   │   ├── detectors/      # Pattern and entropy detectors
│   │   ├── filters/        # File filtering logic
│   │   ├── patterns/       # Built-in pattern definitions
│   │   ├── plugin/         # Gradle plugin implementation
│   │   ├── reporting/      # Report generation
│   │   └── utils/          # Utility classes
│   ├── test/kotlin/        # Unit tests
│   └── functionalTest/kotlin/ # Integration tests
├── docs/                   # Documentation
├── config/                 # Configuration files
└── examples/               # Usage examples`}</code></pre>

                    <h3>Key Development Commands</h3>
                    <div className="grid gap-4 md:grid-cols-2 my-6">
                        <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-4">
                            <h4 className="font-semibold mb-2">Build & Test</h4>
                            <div className="space-y-1 text-sm text-gray-600 dark:text-gray-400">
                                <p><code>./gradlew build</code> - Build project</p>
                                <p><code>./gradlew test</code> - Run tests</p>
                                <p><code>./gradlew functionalTest</code> - Integration tests</p>
                            </div>
                        </div>
                        <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-4">
                            <h4 className="font-semibold mb-2">Code Quality</h4>
                            <div className="space-y-1 text-sm text-gray-600 dark:text-gray-400">
                                <p><code>./gradlew spotlessCheck</code> - Check code style</p>
                                <p><code>./gradlew spotlessApply</code> - Fix code style</p>
                                <p><code>./gradlew detekt</code> - Static analysis</p>
                            </div>
                        </div>
                    </div>
                </section>

                <section id="contribution-types">
                    <h2>Types of Contributions</h2>

                    <p>We welcome various types of contributions:</p>

                    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3 my-6">
                        <div className="bg-blue-50 dark:bg-blue-900/30 border border-blue-200 dark:border-blue-700 rounded-lg p-4">
                            <h4 className="text-blue-800 dark:text-blue-200 font-semibold mb-2">🐛 Bug Fixes</h4>
                            <p className="text-blue-700 dark:text-blue-300 text-sm">
                                Fix issues in existing functionality
                            </p>
                        </div>
                        <div className="bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-700 rounded-lg p-4">
                            <h4 className="text-green-800 dark:text-green-200 font-semibold mb-2">✨ Features</h4>
                            <p className="text-green-700 dark:text-green-300 text-sm">
                                Add new capabilities and enhancements
                            </p>
                        </div>
                        <div className="bg-purple-50 dark:bg-purple-900/30 border border-purple-200 dark:border-purple-700 rounded-lg p-4">
                            <h4 className="text-purple-800 dark:text-purple-200 font-semibold mb-2">🔍 Patterns</h4>
                            <p className="text-purple-700 dark:text-purple-300 text-sm">
                                Add new secret detection patterns
                            </p>
                        </div>
                        <div className="bg-yellow-50 dark:bg-yellow-900/30 border border-yellow-200 dark:border-yellow-700 rounded-lg p-4">
                            <h4 className="text-yellow-800 dark:text-yellow-200 font-semibold mb-2">📚 Documentation</h4>
                            <p className="text-yellow-700 dark:text-yellow-300 text-sm">
                                Improve or expand documentation
                            </p>
                        </div>
                        <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-700 rounded-lg p-4">
                            <h4 className="text-red-800 dark:text-red-200 font-semibold mb-2">⚡ Performance</h4>
                            <p className="text-red-700 dark:text-red-300 text-sm">
                                Optimize scanning performance
                            </p>
                        </div>
                        <div className="bg-indigo-50 dark:bg-indigo-900/30 border border-indigo-200 dark:border-indigo-700 rounded-lg p-4">
                            <h4 className="text-indigo-800 dark:text-indigo-200 font-semibold mb-2">🧪 Testing</h4>
                            <p className="text-indigo-700 dark:text-indigo-300 text-sm">
                                Add or improve test coverage
                            </p>
                        </div>
                    </div>

                    <h3>Before You Start</h3>
                    <ol>
                        <li>Check existing issues to see if your contribution is already being worked on</li>
                        <li>Create an issue to discuss major changes before implementing</li>
                        <li>Follow the coding standards outlined below</li>
                    </ol>
                </section>

                <section id="code-standards">
                    <h2>Code Standards</h2>

                    <h3>Kotlin Style Guidelines</h3>
                    <p>
                        We follow the <a href="https://kotlinlang.org/docs/coding-conventions.html" 
                        className="text-primary-600 dark:text-primary-400 hover:underline">Kotlin Coding Conventions</a> 
                        with project-specific guidelines:
                    </p>

                    <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-4 my-4">
                        <h4 className="font-semibold mb-3">Code Formatting</h4>
                        <ul className="space-y-1 text-sm">
                            <li><strong>Line length:</strong> 120 characters maximum</li>
                            <li><strong>Indentation:</strong> 4 spaces (no tabs)</li>
                            <li><strong>Import order:</strong> Standard Kotlin import ordering</li>
                            <li><strong>Trailing commas:</strong> Enabled for multiline constructs</li>
                        </ul>
                    </div>

                    <h3>Naming Conventions</h3>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`// Classes: PascalCase
class SecretDetector

// Functions and properties: camelCase
fun detectSecrets()
val patternMatches: List<Match>

// Constants: SCREAMING_SNAKE_CASE
const val DEFAULT_ENTROPY_THRESHOLD = 4.5`}</code></pre>

                    <h3>Documentation</h3>
                    <p>Use KDoc for public APIs:</p>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`/**
 * Detects potential secrets in the given text using pattern matching.
 *
 * @param text The text to analyze for secrets
 * @param patterns List of regex patterns to apply
 * @return List of detected secrets with metadata
 */
fun detectSecrets(
    text: String,
    patterns: List<Pattern>
): List<SecretMatch>`}</code></pre>
                </section>

                <section id="testing">
                    <h2>Testing Guidelines</h2>

                    <h3>Test Structure</h3>
                    <p>We maintain high test coverage with different types of tests:</p>

                    <h4>Unit Tests</h4>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`class SecretDetectorTest {
    private val detector = SecretDetector()
    
    @Test
    fun \`should detect AWS access key\`() {
        // Given
        val text = "aws_access_key_id=AKIAIOSFODNN7EXAMPLE"
        
        // When
        val matches = detector.detectSecrets(text)
        
        // Then
        assertThat(matches).hasSize(1)
        assertThat(matches[0].type).isEqualTo(SecretType.AWS_ACCESS_KEY)
    }
}`}</code></pre>

                    <h4>Integration Tests</h4>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`@Test
fun \`should integrate with Gradle build lifecycle\`() {
    // Given
    val projectDir = createTestProject()
    addSecretToFile(projectDir, "Config.kt", "API_KEY=secret123")
    
    // When
    val result = runGradleTask(projectDir, "build")
    
    // Then
    assertThat(result.task(":scanForSecrets")?.outcome)
        .isEqualTo(TaskOutcome.FAILED)
}`}</code></pre>

                    <div className="bg-yellow-50 dark:bg-yellow-900/30 border border-yellow-200 dark:border-yellow-700 rounded-lg p-4 my-6">
                        <h4 className="text-yellow-800 dark:text-yellow-200 font-semibold mb-2">Test Coverage Goals</h4>
                        <ul className="text-yellow-700 dark:text-yellow-300 space-y-1 text-sm">
                            <li><strong>Target:</strong> 80% line coverage minimum</li>
                            <li><strong>Critical paths:</strong> 100% coverage for security-critical code</li>
                            <li><strong>Edge cases:</strong> Test boundary conditions and error scenarios</li>
                        </ul>
                    </div>
                </section>

                <section id="documentation">
                    <h2>Documentation</h2>

                    <h3>Types of Documentation</h3>
                    <div className="grid gap-4 md:grid-cols-2 my-6">
                        <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-4">
                            <h4 className="font-semibold mb-2">📝 Code Documentation</h4>
                            <p className="text-sm text-gray-600 dark:text-gray-400">
                                KDoc comments for public APIs and complex logic
                            </p>
                        </div>
                        <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-4">
                            <h4 className="font-semibold mb-2">📖 User Documentation</h4>
                            <p className="text-sm text-gray-600 dark:text-gray-400">
                                Guides, tutorials, and reference documentation
                            </p>
                        </div>
                        <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-4">
                            <h4 className="font-semibold mb-2">💡 Examples</h4>
                            <p className="text-sm text-gray-600 dark:text-gray-400">
                                Practical usage examples and code samples
                            </p>
                        </div>
                        <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-4">
                            <h4 className="font-semibold mb-2">📋 Changelog</h4>
                            <p className="text-sm text-gray-600 dark:text-gray-400">
                                Record of changes and version history
                            </p>
                        </div>
                    </div>

                    <h3>Documentation Standards</h3>
                    <ul>
                        <li><strong>Clear examples:</strong> Provide working code examples</li>
                        <li><strong>Step-by-step guides:</strong> Break down complex procedures</li>
                        <li><strong>Common use cases:</strong> Cover typical scenarios</li>
                        <li><strong>Troubleshooting:</strong> Address common issues</li>
                    </ul>
                </section>

                <section id="pull-requests">
                    <h2>Pull Request Process</h2>

                    <h3>Before Submitting</h3>
                    <p>1. Update your branch with the latest changes:</p>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`git checkout develop
git pull upstream develop
git checkout your-feature-branch
git rebase develop`}</code></pre>

                    <p>2. Run all checks:</p>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`./gradlew build
./gradlew test
./gradlew functionalTest
./gradlew spotlessCheck
./gradlew detekt`}</code></pre>

                    <h3>PR Guidelines</h3>
                    
                    <h4>Title Format</h4>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`type(scope): Brief description

Examples:
feat(patterns): Add support for Slack API tokens
fix(entropy): Correct entropy calculation for Unicode strings
docs(user-guide): Add CI/CD integration examples`}</code></pre>

                    <h4>Description Template</h4>
                    <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-4 my-4">
                        <p className="text-sm font-medium mb-2">Your PR should include:</p>
                        <ul className="text-sm space-y-1">
                            <li>• Clear description of the change and why it&apos;s needed</li>
                            <li>• Type of change (bug fix, feature, breaking change, docs)</li>
                            <li>• Testing information</li>
                            <li>• Checklist confirmation</li>
                        </ul>
                    </div>

                    <h3>Review Process</h3>
                    <ol>
                        <li>Automated checks must pass</li>
                        <li>Code review by maintainers</li>
                        <li>Testing on different environments</li>
                        <li>Documentation review if applicable</li>
                        <li>Approval by project maintainers</li>
                    </ol>
                </section>

                <section id="issue-reporting">
                    <h2>Issue Reporting</h2>

                    <h3>Bug Reports</h3>
                    <p>When reporting bugs, please include:</p>
                    <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-700 rounded-lg p-4 my-4">
                        <ul className="text-red-700 dark:text-red-300 space-y-1 text-sm">
                            <li>• Clear description of the bug</li>
                            <li>• Steps to reproduce the behavior</li>
                            <li>• Expected vs actual behavior</li>
                            <li>• Environment details (OS, Java version, Gradle version)</li>
                            <li>• Additional context or error messages</li>
                        </ul>
                    </div>

                    <h3>Feature Requests</h3>
                    <p>For feature requests, please describe:</p>
                    <div className="bg-blue-50 dark:bg-blue-900/30 border border-blue-200 dark:border-blue-700 rounded-lg p-4 my-4">
                        <ul className="text-blue-700 dark:text-blue-300 space-y-1 text-sm">
                            <li>• The problem you&apos;re trying to solve</li>
                            <li>• Your proposed solution</li>
                            <li>• Alternative solutions you&apos;ve considered</li>
                            <li>• Additional context or use cases</li>
                        </ul>
                    </div>
                </section>

                <section id="community">
                    <h2>Community</h2>

                    <h3>Getting Help</h3>
                    <div className="grid gap-4 md:grid-cols-3 my-6">
                        <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-4">
                            <h4 className="font-semibold mb-2">💬 GitHub Discussions</h4>
                            <p className="text-sm text-gray-600 dark:text-gray-400">
                                For questions and general discussion
                            </p>
                        </div>
                        <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-4">
                            <h4 className="font-semibold mb-2">🐛 Issues</h4>
                            <p className="text-sm text-gray-600 dark:text-gray-400">
                                For bug reports and feature requests
                            </p>
                        </div>
                        <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-4">
                            <h4 className="font-semibold mb-2">📖 Documentation</h4>
                            <p className="text-sm text-gray-600 dark:text-gray-400">
                                Check the docs for answers first
                            </p>
                        </div>
                    </div>

                    <h3>Recognition</h3>
                    <p>Contributors are recognized in:</p>
                    <ul>
                        <li><strong>Release notes:</strong> Major contributions acknowledged</li>
                        <li><strong>Contributors file:</strong> All contributors listed</li>
                        <li><strong>Special thanks:</strong> Outstanding contributions highlighted</li>
                    </ul>

                    <div className="bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-700 rounded-lg p-6 my-8">
                        <h3 className="text-green-800 dark:text-green-200 text-lg font-semibold mb-3">🎉 Ready to Contribute?</h3>
                        <div className="text-green-700 dark:text-green-300 space-y-2">
                            <p>Start by exploring the <a href="https://github.com/theaniketraj/SCAN/issues" target="_blank" className="underline">open issues</a> or proposing a new feature.</p>
                            <p>Join our community and help make codebases more secure for everyone! 🔒✨</p>
                        </div>
                    </div>
                </section>
            </div>
        </DocsLayout>
    )
}
