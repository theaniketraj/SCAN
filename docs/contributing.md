# Contributing to SCAN

Thank you for your interest in contributing to the SCAN (Sensitive Code Analyzer for Nerds) Gradle Plugin! This document provides guidelines for contributing to the project.

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [Getting Started](#getting-started)
3. [Development Environment](#development-environment)
4. [Making Contributions](#making-contributions)
5. [Code Standards](#code-standards)
6. [Testing Guidelines](#testing-guidelines)
7. [Documentation](#documentation)
8. [Pull Request Process](#pull-request-process)
9. [Issue Reporting](#issue-reporting)
10. [Community](#community)

## Code of Conduct

This project adheres to a [Code of Conduct](../CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to the project maintainers.

## Getting Started

### Prerequisites

- **Java 21** or higher
- **Gradle 8.14+** (wrapper included)
- **Git** for version control
- **IDE** (IntelliJ IDEA recommended)

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:

```bash
git clone https://github.com/theaniketraj/SCAN.git
cd SCAN
```

3. Add the upstream remote:

```bash
git remote add upstream https://github.com/theaniketraj/SCAN.git
```

## Development Environment

### Setup

1. **Import the project** into your IDE
2. **Install dependencies**:

```bash
./gradlew build
```

3. **Run tests** to ensure everything works:

```bash
./gradlew test
```

### Project Structure

```pgsql
scan-gradle-plugin/
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
└── examples/               # Usage examples
```

### Key Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run functional tests
./gradlew functionalTest

# Check code style
./gradlew spotlessCheck

# Fix code style
./gradlew spotlessApply

# Run static analysis
./gradlew detekt

# Generate documentation
./gradlew dokkaHtml

# Publish to local repository (for testing)
./gradlew publishToMavenLocal
```

## Making Contributions

### Types of Contributions

We welcome various types of contributions:

- **Bug Fixes**: Fix issues in existing functionality
- **Feature Enhancements**: Add new capabilities
- **Pattern Additions**: Add new secret detection patterns
- **Documentation**: Improve or expand documentation
- **Performance Improvements**: Optimize scanning performance
- **Test Coverage**: Add or improve tests

### Before You Start

1. **Check existing issues** to see if your contribution is already being worked on
2. **Create an issue** to discuss major changes before implementing
3. **Follow the coding standards** outlined below

### Branch Strategy

- **main**: Stable branch, releases are cut from here
- **develop**: Integration branch for new features
- **feature/**: Feature branches (e.g., `feature/new-aws-patterns`)
- **bugfix/**: Bug fix branches (e.g., `bugfix/entropy-calculation`)
- **docs/**: Documentation improvements

Create feature branches from `develop`:

```bash
git checkout develop
git pull upstream develop
git checkout -b feature/your-feature-name
```

## Code Standards

### Kotlin Style

We follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) with some project-specific guidelines:

#### Code Formatting

- **Line length**: 120 characters maximum
- **Indentation**: 4 spaces (no tabs)
- **Import order**: Standard Kotlin import ordering
- **Trailing commas**: Enabled for multiline constructs

#### Naming Conventions

```kotlin
// Classes: PascalCase
class SecretDetector

// Functions and properties: camelCase
fun detectSecrets()
val patternMatches: List<Match>

// Constants: SCREAMING_SNAKE_CASE
const val DEFAULT_ENTROPY_THRESHOLD = 4.5

// File names: PascalCase matching primary class
// SecretDetector.kt
```

#### Documentation

Use KDoc for public APIs:

```kotlin
/**
 * Detects potential secrets in the given text using pattern matching and entropy analysis.
 *
 * @param text The text to analyze for secrets
 * @param patterns List of regex patterns to apply
 * @param entropyThreshold Minimum entropy threshold for random string detection
 * @return List of detected secrets with metadata
 * @throws IllegalArgumentException if entropy threshold is invalid
 */
fun detectSecrets(
    text: String,
    patterns: List<Pattern>,
    entropyThreshold: Double = DEFAULT_ENTROPY_THRESHOLD
): List<SecretMatch>
```

### Code Organization

#### Package Structure

```kotlin
// Core functionality
package com.scan.core

// Specific detector implementations
package com.scan.detectors.patterns
package com.scan.detectors.entropy

// Gradle plugin components
package com.scan.plugin

// Utilities
package com.scan.utils
```

#### Dependency Injection

Use constructor injection for dependencies:

```kotlin
class SecretScanner(
    private val patternDetector: PatternDetector,
    private val entropyDetector: EntropyDetector,
    private val fileFilter: FileFilter
) {
    // Implementation
}
```

### Error Handling

#### Exception Handling

```kotlin
// Use specific exception types
class ScanConfigurationException(message: String, cause: Throwable? = null) : Exception(message, cause)

// Handle exceptions appropriately
try {
    scanFile(file)
} catch (e: IOException) {
    logger.warn("Failed to read file: ${file.absolutePath}", e)
    // Continue with other files
} catch (e: ScanConfigurationException) {
    logger.error("Configuration error", e)
    throw e // Re-throw configuration errors
}
```

#### Logging

Use SLF4J for logging:

```kotlin
class SecretDetector {
    companion object {
        private val logger = LoggerFactory.getLogger(SecretDetector::class.java)
    }
    
    fun detectSecrets(text: String) {
        logger.debug("Scanning text of length: ${text.length}")
        // Implementation
        logger.info("Found ${matches.size} potential secrets")
    }
}
```

## Testing Guidelines

### Test Structure

#### Unit Tests

```kotlin
class SecretDetectorTest {
    private val detector = SecretDetector()
    
    @Test
    fun `should detect AWS access key`() {
        // Given
        val text = "aws_access_key_id=AKIAIOSFODNN7EXAMPLE"
        
        // When
        val matches = detector.detectSecrets(text)
        
        // Then
        assertThat(matches).hasSize(1)
        assertThat(matches[0].type).isEqualTo(SecretType.AWS_ACCESS_KEY)
    }
}
```

#### Integration Tests

```kotlin
class ScanPluginFunctionalTest {
    @Test
    fun `should integrate with Gradle build lifecycle`() {
        // Given
        val projectDir = createTestProject()
        addSecretToFile(projectDir, "src/main/kotlin/Config.kt", "API_KEY=secret123")
        
        // When
        val result = runGradleTask(projectDir, "build")
        
        // Then
        assertThat(result.task(":scanForSecrets")?.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("Secret detected in Config.kt")
    }
}
```

### Test Coverage

- **Target**: 80% line coverage minimum
- **Critical paths**: 100% coverage for security-critical code
- **Edge cases**: Test boundary conditions and error scenarios

### Performance Tests

```kotlin
class PerformanceTest {
    @Test
    fun `should scan large files efficiently`() {
        // Given
        val largeFile = generateFile(sizeInMB = 10)
        
        // When
        val startTime = System.currentTimeMillis()
        val results = scanner.scan(largeFile)
        val duration = System.currentTimeMillis() - startTime
        
        // Then
        assertThat(duration).isLessThan(5000) // 5 seconds max
    }
}
```

## Documentation

### Types of Documentation

1. **Code Documentation**: KDoc comments for public APIs
2. **User Documentation**: Guides and references in `/docs`
3. **Examples**: Practical usage examples
4. **Changelog**: Record of changes in releases

### Documentation Standards

#### API Documentation

```kotlin
/**
 * Represents a detected secret with metadata about the finding.
 *
 * @property type The type of secret detected (e.g., AWS_ACCESS_KEY)
 * @property content The actual secret content (may be redacted)
 * @property file The file where the secret was found
 * @property line The line number of the detection
 * @property confidence Confidence level of the detection (0.0-1.0)
 */
data class SecretMatch(
    val type: SecretType,
    val content: String,
    val file: File,
    val line: Int,
    val confidence: Double
)
```

#### User Documentation

- **Clear examples**: Provide working code examples
- **Step-by-step guides**: Break down complex procedures
- **Common use cases**: Cover typical scenarios
- **Troubleshooting**: Address common issues

### Documentation Review

All documentation changes should be reviewed for:

- **Accuracy**: Information is correct and up-to-date
- **Clarity**: Content is easy to understand
- **Completeness**: All necessary information is included
- **Examples**: Code examples are working and relevant

## Pull Request Process

### Before Submitting

1. **Update your branch** with the latest changes:

```bash
git checkout develop
git pull upstream develop
git checkout your-feature-branch
git rebase develop
```

2. **Run all checks**:

```bash
./gradlew build
./gradlew test
./gradlew functionalTest
./gradlew spotlessCheck
./gradlew detekt
```

3. **Update documentation** if needed

### PR Guidelines

#### Title Format

```
type(scope): Brief description

Examples:
feat(patterns): Add support for Slack API tokens
fix(entropy): Correct entropy calculation for Unicode strings
docs(user-guide): Add CI/CD integration examples
```

#### Description Template

```markdown
## Description
Brief description of the change and why it's needed.

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing performed

## Checklist
- [ ] Code follows the style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] Tests added/updated
```

### Review Process

1. **Automated checks** must pass
2. **Code review** by maintainers
3. **Testing** on different environments
4. **Documentation review** if applicable
5. **Approval** by project maintainers

## Issue Reporting

### Bug Reports

Use the bug report template:

```markdown
**Describe the bug**
A clear description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior:
1. Configure plugin with '...'
2. Run gradle task '...'
3. See error

**Expected behavior**
What you expected to happen.

**Environment:**
- OS: [e.g. Windows 10, macOS 12, Ubuntu 20.04]
- Java version: [e.g. OpenJDK 21]
- Gradle version: [e.g. 8.14]
- Plugin version: [e.g. 1.0.0]

**Additional context**
Any other context about the problem.
```

### Feature Requests

Use the feature request template:

```markdown
**Is your feature request related to a problem?**
A clear description of what the problem is.

**Describe the solution you'd like**
A clear description of what you want to happen.

**Describe alternatives you've considered**
Alternative solutions or features you've considered.

**Additional context**
Any other context or screenshots about the feature request.
```

## Community

### Getting Help

- **GitHub Discussions**: For questions and general discussion
- **Issues**: For bug reports and feature requests
- **Documentation**: Check the docs first

### Recognition

Contributors will be recognized in:

- **Release notes**: Major contributions acknowledged
- **Contributors file**: All contributors listed
- **Special thanks**: Outstanding contributions highlighted

### Maintainer Responsibilities

Maintainers will:

- **Review PRs** in a timely manner
- **Provide feedback** and guidance
- **Maintain code quality** standards
- **Release management** and versioning
- **Community engagement** and support

---

Thank you for contributing to SCAN! Your efforts help make codebases more secure for everyone. 🔒✨
