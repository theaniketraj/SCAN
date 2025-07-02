# Contributing to SCAN Gradle Plugin

Thank you for your interest in contributing to SCAN! This document provides guidelines and information for contributors.

## 🤝 Code of Conduct

By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md). Please be respectful and constructive in all interactions.

## 🐛 Reporting Issues

### Before Submitting an Issue

- Check existing [issues](https://github.com/theaniketraj/SCAN/issues) to avoid duplicates
- Ensure you're using the latest version of the plugin
- Test with a minimal reproduction case

### Issue Template

When reporting bugs, please include:

- **Plugin version** and Gradle version
- **Operating system** and Java version
- **Clear description** of the expected vs actual behavior
- **Minimal reproduction steps** or sample project
- **Relevant configuration** (sanitized of any real secrets)
- **Log output** or error messages

## 💡 Suggesting Features

We welcome feature suggestions! Please:

- Check if the feature already exists or is planned
- Open an issue with the `enhancement` label
- Describe the use case and expected behavior
- Consider implementation complexity and maintenance impact

## 🛠️ Development Setup

### Prerequisites

- **Java 17+** (for development)
- **Gradle 8.0+**
- **Git**
- **IDE** with Kotlin support (IntelliJ IDEA recommended)

### Setting Up the Project

```bash
# Clone the repository
git clone https://github.com/theaniketraj/SCAN.git
cd scan-gradle-plugin

# Build the project
./gradlew build

# Run tests
./gradlew test

# Generate test coverage report
./gradlew jacocoTestReport
```

### Project Structure Overview

```pgsql
scan-gradle-plugin/
├── src/main/kotlin/com/scan/
│   ├── plugin/          # Gradle plugin integration
│   ├── core/            # Core scanning engine
│   ├── detectors/       # Detection strategies
│   ├── patterns/        # Secret pattern definitions
│   ├── filters/         # File filtering logic
│   ├── reporting/       # Report generation
│   └── utils/           # Utility classes
├── src/test/            # Test code structure mirrors main
└── docs/                # Documentation
```

- For complete project structure and file descriptions, refer to the [Project Structure](https://github.com/theaniketraj/SCAN/blob/main/PROJECT_STRUCTURE.md).

## 📝 Development Guidelines

### Code Style

We follow Kotlin coding conventions with these additions:

- **Line length**: 120 characters maximum
- **Indentation**: 4 spaces (no tabs)
- **Naming**: Use descriptive names, avoid abbreviations
- **Comments**: Document public APIs and complex logic

### Kotlin-Specific Guidelines

```kotlin
// ✅ Good: Clear, descriptive naming
class PatternDetector(
    private val patterns: List<SecretPattern>,
    private val configuration: DetectionConfiguration
) {
    fun detectSecrets(content: String): List<DetectionResult> {
        // Implementation
    }
}

// ❌ Avoid: Unclear naming and structure  
class PD(private val p: List<SP>) {
    fun detect(c: String): List<DR> { /* ... */ }
}
```

### Testing Standards

- **Unit tests**: Test individual components in isolation
- **Integration tests**: Test component interactions
- **End-to-end tests**: Test full plugin functionality
- **Test coverage**: Aim for 80%+ coverage on new code

```kotlin
@Test
fun `should detect AWS access key in kotlin file`() {
    // Given
    val content = """val awsKey = "AKIAIOSFODNN7EXAMPLE"""
    val detector = PatternDetector(awsPatterns)
    
    // When
    val results = detector.detectSecrets(content)
    
    // Then
    assertThat(results).hasSize(1)
    assertThat(results.first().type).isEqualTo(SecretType.AWS_ACCESS_KEY)
}
```

## 🔄 Pull Request Process

### Before Submitting

1. **Create an issue** first to discuss significant changes
2. **Fork the repository** and create a feature branch
3. **Write tests** for new functionality
4. **Update documentation** if needed
5. **Run the full test suite** locally

### Branch Naming

- **Feature**: `feature/description-of-feature`
- **Bug fix**: `fix/description-of-fix`
- **Documentation**: `docs/description-of-update`
- **Refactoring**: `refactor/description-of-change`

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```pgsql
feat: add entropy-based detection for base64 encoded secrets
fix: handle empty files without throwing exceptions
docs: update configuration examples in user guide
test: add integration tests for custom pattern loading
refactor: extract pattern matching logic into separate class
```

### Pull Request Template

- **Description**: Clear explanation of changes
- **Motivation**: Why this change is needed
- **Testing**: How the change was tested
- **Documentation**: Any docs that need updating
- **Breaking changes**: Note any API changes

## 🧪 Testing Guidelines

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "PatternDetectorTest"

# Run tests with coverage
./gradlew test jacocoTestReport

# Run integration tests
./gradlew integrationTest

# Run performance tests
./gradlew performanceTest
```

### Test Categories

- **Unit Tests** (`src/test/kotlin`): Fast, isolated component tests
- **Integration Tests** (`src/test/kotlin/integration`): Multi-component tests
- **Performance Tests**: Benchmark critical paths
- **End-to-End Tests**: Full plugin workflow tests

### Test Data

- Use realistic but **synthetic test data** only
- **Never commit real secrets** even in test files
- Place test files in `src/test/resources/test-files/`
- Document test scenarios clearly

## 📚 Documentation

### Types of Documentation

- **User documentation** (`docs/`): Guides for plugin users
- **API documentation**: KDoc comments in code
- **Architecture documentation**: High-level design decisions
- **Contributing documentation**: This file and related guides

### Documentation Standards

- **Write for your audience**: Users vs developers vs contributors
- **Keep it current**: Update docs with code changes
- **Include examples**: Show, don't just tell
- **Test documentation**: Ensure examples work

## 🚀 Release Process

### Versioning

We follow [Semantic Versioning](https://semver.org/):

- **Major** (1.0.0): Breaking changes
- **Minor** (1.1.0): New features, backward compatible
- **Patch** (1.1.1): Bug fixes, backward compatible

### Release Checklist

1. Update version in `build.gradle.kts`
2. Update `CHANGELOG.md`
3. Run full test suite
4. Create release PR
5. Tag release after merge
6. Publish to Gradle Plugin Portal

## 🛡️ Security

### Reporting Security Issues

**Do not open public issues for security vulnerabilities.**

Instead, email <security@scan-security.com> with:

- Description of the vulnerability
- Steps to reproduce
- Potential impact assessment
- Suggested fix (if any)

### Security Guidelines for Contributors

- **Never commit real secrets** in any form
- **Sanitize test data** of sensitive information
- **Review dependencies** for known vulnerabilities
- **Follow secure coding practices**

## 🏆 Recognition

Contributors are recognized in:

- **CONTRIBUTORS.md** file
- **Release notes** for significant contributions
- **GitHub releases** mentions

## ❓ Getting Help

- **General questions**: Open a [discussion](https://github.com/theaniketraj/SCAN/discussions)
- **Bug reports**: Create an [issue](https://github.com/theaniketraj/SCAN/issues)
- **Development help**: Ask in our development chat or open a discussion

## 📞 Contact

- **GitHub**: [@theaniketraj](https://github.com/theaniketraj)
- **Email**: <contribute@scan-security.com>
- **Discussions**: [GitHub Discussions](https://github.com/theaniketraj/SCAN/discussions)

---

Thank you for helping make SCAN better! Every contribution, whether it's code, documentation, testing, or feedback, helps improve security for the entire community.
