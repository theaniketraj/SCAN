# SCAN Gradle Plugin

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/com.scan)](https://plugins.gradle.org/plugin/com.scan)
[![Build Status](https://github.com/scan-security/scan-gradle-plugin/workflows/CI/badge.svg)](https://github.com/theaniketraj/SCAN/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.0-blue.svg?logo=kotlin)](http://kotlinlang.org)

> **Secure your code before it reaches version control**

SCAN is an intelligent Gradle plugin that automatically detects secrets, API keys, credentials, and other sensitive information in your codebase. It acts as your first line of defense against accidental security leaks by scanning files during your build process.

## 🎯 What Does SCAN Do?

SCAN prevents security incidents by catching sensitive data before it gets committed to your repository. Think of it as a security guard that:

- **Detects Secrets**: Finds API keys, passwords, tokens, and cryptographic keys
- **Prevents Leaks**: Stops builds when sensitive data is detected
- **Provides Context**: Shows exactly where and what was found
- **Integrates Seamlessly**: Works naturally within your existing Gradle workflow

## 🔬 How It Works Under the Hood

### Multi-Layered Detection Engine

SCAN employs three sophisticated detection strategies that work together:

#### 1. **Pattern Recognition** 🎭

- Uses carefully crafted regex patterns to identify known secret formats
- Recognizes AWS keys, GitHub tokens, database URLs, and 50+ other secret types
- Maintains high accuracy with minimal false positives through pattern refinement

#### 2. **Entropy Analysis** 🧮

- Calculates mathematical entropy of strings to find random-looking data
- Identifies base64-encoded secrets, random tokens, and obfuscated credentials
- Uses configurable thresholds to balance sensitivity vs. noise

#### 3. **Context-Aware Intelligence** 🧠

- Understands code structure to differentiate between real secrets and test data
- Analyzes variable names, comments, and file types for additional context
- Reduces false positives by understanding when something *looks* like a secret but isn't

### Smart Filtering System

The plugin includes an intelligent filtering pipeline:

- **File Type Filtering**: Focuses on code files, ignores binaries and generated content
- **Path-Based Exclusions**: Skips test directories, build artifacts, and dependencies
- **Whitelist Support**: Allows known-safe patterns to be explicitly permitted
- **Custom Rules**: Supports project-specific filtering requirements

### Performance Architecture

SCAN is built for speed and efficiency:

- **Lazy Evaluation**: Only processes files that match inclusion criteria
- **Parallel Processing**: Scans multiple files concurrently when possible
- **Memory Efficient**: Streams large files without loading them entirely into memory
- **Incremental Scanning**: Can focus on changed files in CI environments

## 🚀 Quick Start

### Installation

```kotlin
plugins {
    id("io.github.theaniketraj.scan") version "1.0.0"
}
```

### Basic Usage

```bash
./gradlew scan
```

### Simple Configuration

```kotlin
scan {
    failOnDetection = true
    reportFormat = "console"
}
```

That's it! SCAN will now protect your builds with sensible defaults.

## 📖 Documentation

For comprehensive guides, configuration options, and examples:

- **[User Guide](docs/user-guide.md)** - Complete setup and usage instructions
- **[Configuration Reference](docs/configuration-reference.md)** - All available options explained
- **[Pattern Reference](docs/pattern-reference.md)** - Built-in patterns and custom pattern creation
- **[CI/CD Examples](docs/examples/ci-cd-integration/)** - Integration with popular CI platforms

## 🛡️ What Gets Detected?

### Built-in Secret Types

- **Cloud Providers**: AWS, GCP, Azure credentials and keys
- **Version Control**: GitHub, GitLab, Bitbucket tokens
- **Databases**: Connection strings, passwords, authentication URLs
- **APIs**: REST API keys, webhook secrets, service tokens
- **Cryptographic**: Private keys, certificates, encryption keys
- **Generic**: High-entropy strings, encoded secrets, custom patterns

### Detection Examples

```pgsql
❌ AWS Access Key found in Config.kt:15
   AKIAIOSFODNN7EXAMPLE

⚠️  High entropy string in application.yml:8  
   Entropy: 4.8 (random-looking password detected)

✅ Test key in TestConfig.kt:5 (whitelisted)
   test_key_12345
```

## 🎛️ Key Features

- **Zero Configuration**: Works out-of-the-box with intelligent defaults
- **Flexible Reporting**: Console, JSON, and HTML output formats
- **Gradle Integration**: Native task lifecycle integration
- **CI/CD Ready**: Designed for automated pipeline integration
- **Extensible**: Custom patterns and detection rules
- **Performance Focused**: Optimized for large codebases

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](Contributing-guide.md) for details on:

- Setting up the development environment
- Running tests and benchmarks
- Submitting pull requests
- Reporting issues

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](https://github.com/theaniketraj/SCAN/blob/main/LICENSE) file for details.

## 🙏 Acknowledgments

SCAN is inspired by tools like TruffleHog, GitLeaks, and Detect-Secrets, but built specifically for the Gradle ecosystem with Kotlin-first design principles.

---

**Ready to secure your code?** Install SCAN today and never worry about accidental credential leaks again.
