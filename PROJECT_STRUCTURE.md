# SCAN Plugin Project Structure

```pgsql
scan-gradle-plugin/
├── README.md
├── build.gradle.kts                    # Main build configuration
├── settings.gradle.kts                 # Project settings
├── gradle.properties                   # Gradle properties
├── .gitignore
│
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/
│   │   │       └── scan/
│   │   │           ├── plugin/
│   │   │           │   ├── ScanPlugin.kt                    # Main plugin class
│   │   │           │   ├── ScanTask.kt                      # Main scanning task
│   │   │           │   └── ScanExtension.kt                 # Plugin configuration extension
│   │   │           │
│   │   │           ├── core/
│   │   │           │   ├── ScanEngine.kt                    # Core scanning logic
│   │   │           │   ├── ScanResult.kt                    # Result models
│   │   │           │   ├── ScanConfiguration.kt             # Configuration models
│   │   │           │   └── FileScanner.kt                   # File processing logic
│   │   │           │
│   │   │           ├── detectors/
│   │   │           │   ├── DetectorInterface.kt             # Common interface for all detectors
│   │   │           │   ├── PatternDetector.kt               # Pattern-based detection
│   │   │           │   ├── EntropyDetector.kt               # High-entropy string detection
│   │   │           │   ├── ContextAwareDetector.kt          # Context-aware scanning
│   │   │           │   └── CompositeDetector.kt             # Combines multiple detectors
│   │   │           │
│   │   │           ├── patterns/
│   │   │           │   ├── SecretPatterns.kt                # Predefined secret patterns
│   │   │           │   ├── ApiKeyPatterns.kt                # API key specific patterns
│   │   │           │   ├── CryptoPatterns.kt                # Cryptographic key patterns
│   │   │           │   └── DatabasePatterns.kt              # Database connection patterns
│   │   │           │
│   │   │           ├── filters/
│   │   │           │   ├── FilterInterface.kt               # Common interface for filters
│   │   │           │   ├── FileExtensionFilter.kt           # Filter by file extensions
│   │   │           │   ├── PathFilter.kt                    # Filter by file paths
│   │   │           │   ├── WhitelistFilter.kt               # Whitelist/exclusion filter
│   │   │           │   └── TestFileFilter.kt                # Handle test files differently
│   │   │           │
│   │   │           ├── reporting/
│   │   │           │   ├── ReportGenerator.kt               # Generate scan reports
│   │   │           │   ├── ConsoleReporter.kt               # Console output formatting
│   │   │           │   ├── JsonReporter.kt                  # JSON report format
│   │   │           │   └── HtmlReporter.kt                  # HTML report format
│   │   │           │
│   │   │           └── utils/
│   │   │               ├── FileUtils.kt                     # File handling utilities
│   │   │               ├── EntropyCalculator.kt             # Entropy calculation logic
│   │   │               ├── PatternMatcher.kt                # Pattern matching utilities
│   │   │               └── ConfigurationLoader.kt           # Load configuration files
│   │   │
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   └── gradle-plugins/
│   │       │       └── com.scan.properties                  # Plugin descriptor
│   │       │
│   │       └── patterns/
│   │           ├── default-patterns.yml                     # Default secret patterns
│   │           ├── api-patterns.yml                         # API-specific patterns
│   │           └── crypto-patterns.yml                      # Cryptographic patterns
│   │
│   └── test/
│       ├── kotlin/
│       │   └── com/
│       │       └── scan/
│       │           ├── plugin/
│       │           │   ├── ScanPluginTest.kt                # Plugin integration tests
│       │           │   └── ScanTaskTest.kt                  # Task behavior tests
│       │           │
│       │           ├── core/
│       │           │   ├── ScanEngineTest.kt                # Core engine tests
│       │           │   └── FileScannerTest.kt               # File scanning tests
│       │           │
│       │           ├── detectors/
│       │           │   ├── PatternDetectorTest.kt           # Pattern detection tests
│       │           │   ├── EntropyDetectorTest.kt           # Entropy detection tests
│       │           │   └── ContextAwareDetectorTest.kt      # Context-aware tests
│       │           │
│       │           └── integration/
│       │               ├── EndToEndTest.kt                  # Full integration tests
│       │               └── PerformanceTest.kt               # Performance benchmarks
│       │
│       └── resources/
│           ├── test-files/
│           │   ├── with-secrets/                            # Test files containing secrets
│           │   │   ├── api-keys.kt
│           │   │   ├── database-urls.properties
│           │   │   └── crypto-keys.json
│           │   │
│           │   ├── clean-files/                             # Test files without secrets
│           │   │   ├── normal-code.kt
│           │   │   └── configuration.properties
│           │   │
│           │   └── edge-cases/                              # Edge case test files
│           │       ├── false-positives.kt
│           │       └── encoded-secrets.txt
│           │
│           └── configurations/
│               ├── strict-config.yml                        # Strict scanning configuration
│               ├── balanced-config.yml                      # Balanced scanning configuration
│               └── custom-patterns.yml                      # Custom pattern examples
│
├── config/
│   └── detekt/
│       ├── detekt.yml
│       └── baseline.xml
│
│
├── docs/
│   ├── user-guide.md                                        # User documentation
│   ├── configuration-reference.md                           # Configuration options
│   ├── pattern-reference.md                                 # Pattern documentation
│   ├── contributing.md                                      # Contribution guidelines
│   └── examples/
│       ├── basic-usage/
│       ├── custom-patterns/
│       └── ci-cd-integration/
│
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
│
├── gradlew
├── gradlew.bat
│
└── config-examples/
    ├── .scan-ignore                                         # Example ignore file
    ├── scan-config.yml                                      # Example configuration
    └── custom-patterns.yml                                  # Custom pattern examples
```
