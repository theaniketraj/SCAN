package com.scan.filters

import com.scan.core.Finding
import java.io.File
import java.util.regex.Pattern

/**
 * Filter implementation that handles test files with configurable policies. Test files often
 * contain mock data, sample credentials, and test fixtures that shouldn't be treated as real
 * security vulnerabilities.
 */
class TestFileFilter(private val config: TestFileConfig = TestFileConfig()) : FilterInterface {

    /** Configuration for test file handling policies */
    data class TestFileConfig(
        val policy: TestFilePolicy = TestFilePolicy.EXCLUDE_ALL,
        val allowRealSecrets: Boolean = false,
        val strictnessLevel: StrictnessLevel = StrictnessLevel.BALANCED,
        val customTestPatterns: Set<Pattern> = emptySet(),
        val customTestDirectories: Set<String> = emptySet(),
        val customTestFileExtensions: Set<String> = emptySet(),
        val excludeTestDataFiles: Boolean = true,
        val excludeMockFiles: Boolean = true,
        val excludeFixtureFiles: Boolean = true,
        val treatIntegrationTestsAsProduction: Boolean = false,
        val caseSensitive: Boolean = false
    ) {
        companion object {
            fun builder() = TestFileConfigBuilder()
        }
    }

    /** Policies for handling test files */
    enum class TestFilePolicy {
        EXCLUDE_ALL, // Exclude all test files from scanning
        INCLUDE_ALL, // Include all test files with normal scanning
        RELAXED_SCANNING, // Scan test files but with relaxed rules
        INTEGRATION_ONLY // Only scan integration test files
    }

    /** Strictness levels for test file detection */
    enum class StrictnessLevel {
        STRICT, // Only obvious test files (test/, *Test.kt, etc.)
        BALANCED, // Common test patterns and conventions
        LOOSE // Aggressive test file detection
    }

    /** Builder for TestFileConfig */
    class TestFileConfigBuilder {
        private var policy = TestFilePolicy.EXCLUDE_ALL
        private var allowRealSecrets = false
        private var strictnessLevel = StrictnessLevel.BALANCED
        private val customTestPatterns = mutableSetOf<Pattern>()
        private val customTestDirectories = mutableSetOf<String>()
        private val customTestFileExtensions = mutableSetOf<String>()
        private var excludeTestDataFiles = true
        private var excludeMockFiles = true
        private var excludeFixtureFiles = true
        private var treatIntegrationTestsAsProduction = false
        private var caseSensitive = false

        fun policy(policy: TestFilePolicy) = apply { this.policy = policy }
        fun allowRealSecrets(allow: Boolean) = apply { this.allowRealSecrets = allow }
        fun strictnessLevel(level: StrictnessLevel) = apply { this.strictnessLevel = level }
        fun excludeTestDataFiles(exclude: Boolean) = apply { this.excludeTestDataFiles = exclude }
        fun excludeMockFiles(exclude: Boolean) = apply { this.excludeMockFiles = exclude }
        fun excludeFixtureFiles(exclude: Boolean) = apply { this.excludeFixtureFiles = exclude }
        fun treatIntegrationTestsAsProduction(treat: Boolean) = apply {
            this.treatIntegrationTestsAsProduction = treat
        }
        fun caseSensitive(sensitive: Boolean) = apply { this.caseSensitive = sensitive }

        fun addCustomTestPattern(pattern: String) = apply {
            val flags = if (caseSensitive) 0 else Pattern.CASE_INSENSITIVE
            customTestPatterns.add(Pattern.compile(pattern, flags))
        }

        fun addCustomTestDirectory(directory: String) = apply {
            customTestDirectories.add(directory)
        }

        fun addCustomTestFileExtension(extension: String) = apply {
            customTestFileExtensions.add(extension.removePrefix("."))
        }

        fun build() =
            TestFileConfig(
                policy = policy,
                allowRealSecrets = allowRealSecrets,
                strictnessLevel = strictnessLevel,
                customTestPatterns = customTestPatterns.toSet(),
                customTestDirectories = customTestDirectories.toSet(),
                customTestFileExtensions = customTestFileExtensions.toSet(),
                excludeTestDataFiles = excludeTestDataFiles,
                excludeMockFiles = excludeMockFiles,
                excludeFixtureFiles = excludeFixtureFiles,
                treatIntegrationTestsAsProduction = treatIntegrationTestsAsProduction,
                caseSensitive = caseSensitive
            )
    }

    override fun shouldIncludeFile(file: File, relativePath: String): Boolean {
        val isTestFile = isTestFile(file)

        return when (config.policy) {
            TestFilePolicy.EXCLUDE_ALL -> !isTestFile
            TestFilePolicy.INCLUDE_ALL -> true
            TestFilePolicy.RELAXED_SCANNING -> true
            TestFilePolicy.INTEGRATION_ONLY -> !isTestFile || isIntegrationTest(file)
        }
    }

    override fun shouldIncludeLine(line: String, lineNumber: Int, file: File): Boolean {
        return shouldIncludeFile(file, file.path)
    }

    override fun getDescription(): String {
        return "Filter that handles test files with policy: ${config.policy}"
    }

    fun shouldIncludeFinding(finding: Finding): Boolean {
        val file = File(finding.location.filePath)
        val isTestFile = isTestFile(file)

        if (!isTestFile) {
            return true // Not a test file, apply normal rules
        }

        return when (config.policy) {
            TestFilePolicy.EXCLUDE_ALL -> false
            TestFilePolicy.INCLUDE_ALL -> true
            TestFilePolicy.RELAXED_SCANNING -> shouldIncludeTestFinding(finding, file)
            TestFilePolicy.INTEGRATION_ONLY -> isIntegrationTest(file)
        }
    }

    private fun shouldIncludeTestFinding(finding: Finding, file: File): Boolean {
        // If configured to allow real secrets in tests, don't filter
        if (config.allowRealSecrets) {
            return true
        }

        // Check if this looks like a real secret vs test data
        if (looksLikeRealSecret(finding)) {
            return true
        }

        // Check if it's in test data/mock/fixture files
        if (config.excludeTestDataFiles && isTestDataFile(file)) {
            return false
        }

        if (config.excludeMockFiles && isMockFile(file)) {
            return false
        }

        if (config.excludeFixtureFiles && isFixtureFile(file)) {
            return false
        }

        // Filter out obvious test patterns
        return !isObviousTestSecret(finding)
    }

    private fun isTestFile(file: File): Boolean {
        val fileName = if (config.caseSensitive) file.name else file.name.lowercase()
        val filePath =
            if (config.caseSensitive) {
                file.absolutePath.replace("\\", "/")
            } else {
                file.absolutePath.replace("\\", "/").lowercase()
            }

        // Check custom patterns first
        if (matchesCustomTestPatterns(filePath)) {
            return true
        }

        return when (config.strictnessLevel) {
            StrictnessLevel.STRICT -> isTestFileStrict(fileName, filePath)
            StrictnessLevel.BALANCED -> isTestFileBalanced(fileName, filePath)
            StrictnessLevel.LOOSE -> isTestFileLoose(fileName, filePath)
        }
    }

    private fun matchesCustomTestPatterns(filePath: String): Boolean {
        return config.customTestPatterns.any { pattern -> pattern.matcher(filePath).find() }
    }

    private fun isTestFileStrict(fileName: String, filePath: String): Boolean {
        val testDirs = setOf("test", "tests") + config.customTestDirectories
        val testExtensions =
            setOf("test.kt", "test.java", "spec.kt", "spec.java") +
                config.customTestFileExtensions.map { "$it.test" }.toSet() +
                config.customTestFileExtensions.map { "$it.spec" }.toSet()

        return testDirs.any { dir -> filePath.contains("/$dir/") } ||
            testExtensions.any { ext -> fileName.endsWith(ext) } ||
            fileName.matches(Regex(".*Test\\.(kt|java|js|ts|py|rb|go|rs|scala)$")) ||
            fileName.matches(Regex(".*Spec\\.(kt|java|js|ts|py|rb|go|rs|scala)$"))
    }

    private fun isTestFileBalanced(fileName: String, filePath: String): Boolean {
        if (isTestFileStrict(fileName, filePath)) return true

        val testDirs =
            setOf(
                "test",
                "tests",
                "spec",
                "specs",
                "__tests__",
                "__test__",
                "testing",
                "unittest",
                "unit-test",
                "integration-test"
            ) + config.customTestDirectories

        val testPatterns = listOf("test", "spec", "mock", "fixture", "stub", "fake")

        // Check directories
        if (testDirs.any { dir -> filePath.contains("/$dir/") }) {
            return true
        }

        // Check file name patterns
        if (testPatterns.any { pattern ->
                fileName.contains(pattern) ||
                    fileName.startsWith("${pattern}_") ||
                    fileName.endsWith("_$pattern")
            }
        ) {
            return true
        }

        // Check for test-specific file extensions
        val testExtensions =
            config.customTestFileExtensions +
                setOf(
                    "test.js",
                    "test.ts",
                    "spec.js",
                    "spec.ts",
                    "test.py",
                    "spec.py",
                    "test.rb",
                    "spec.rb"
                )

        return testExtensions.any { ext -> fileName.endsWith(ext) }
    }

    private fun isTestFileLoose(fileName: String, filePath: String): Boolean {
        if (isTestFileBalanced(fileName, filePath)) return true

        val loosePatterns =
            listOf(
                "example",
                "sample",
                "demo",
                "playground",
                "sandbox",
                "dummy",
                "temp",
                "tmp",
                "dev",
                "debug"
            )

        return loosePatterns.any { pattern ->
            fileName.contains(pattern) || filePath.contains("/$pattern/")
        }
    }

    private fun isIntegrationTest(file: File): Boolean {
        val filePath =
            if (config.caseSensitive) {
                file.absolutePath.replace("\\", "/")
            } else {
                file.absolutePath.replace("\\", "/").lowercase()
            }

        val integrationPatterns =
            listOf(
                "integration",
                "integration-test",
                "integrationtest",
                "e2e",
                "end-to-end",
                "endtoend",
                "system-test",
                "systemtest"
            )

        return integrationPatterns.any { pattern ->
            filePath.contains("/$pattern/") ||
                file.name.contains(pattern, ignoreCase = !config.caseSensitive)
        }
    }

    private fun isTestDataFile(file: File): Boolean {
        val fileName = if (config.caseSensitive) file.name else file.name.lowercase()
        val filePath =
            if (config.caseSensitive) {
                file.absolutePath.replace("\\", "/")
            } else {
                file.absolutePath.replace("\\", "/").lowercase()
            }

        val testDataPatterns =
            listOf("testdata", "test-data", "test_data", "data", "resources", "assets")

        return testDataPatterns.any { pattern ->
            fileName.contains(pattern) || filePath.contains("/$pattern/")
        } ||
            file.extension.let { ext ->
                listOf("json", "xml", "yaml", "yml", "csv", "txt", "properties")
                    .contains(if (config.caseSensitive) ext else ext.lowercase())
            }
    }

    private fun isMockFile(file: File): Boolean {
        val fileName = if (config.caseSensitive) file.name else file.name.lowercase()
        val filePath =
            if (config.caseSensitive) {
                file.absolutePath.replace("\\", "/")
            } else {
                file.absolutePath.replace("\\", "/").lowercase()
            }

        val mockPatterns =
            listOf(
                "mock",
                "mocks",
                "__mocks__",
                "stub",
                "stubs",
                "fake",
                "fakes",
                "double",
                "doubles"
            )

        return mockPatterns.any { pattern ->
            fileName.contains(pattern) ||
                filePath.contains("/$pattern/") ||
                fileName.startsWith("${pattern}_") ||
                fileName.endsWith("_$pattern")
        }
    }

    private fun isFixtureFile(file: File): Boolean {
        val fileName = if (config.caseSensitive) file.name else file.name.lowercase()
        val filePath =
            if (config.caseSensitive) {
                file.absolutePath.replace("\\", "/")
            } else {
                file.absolutePath.replace("\\", "/").lowercase()
            }

        val fixturePatterns =
            listOf(
                "fixture",
                "fixtures",
                "factory",
                "factories",
                "builder",
                "builders",
                "generator",
                "generators"
            )

        return fixturePatterns.any { pattern ->
            fileName.contains(pattern) || filePath.contains("/$pattern/")
        }
    }

    private fun looksLikeRealSecret(finding: Finding): Boolean {
        val secretValue = finding.secretInfo.detectedValue
        val lineContent = finding.location.lineContent

        // Check for production-like environment indicators
        val productionIndicators = listOf("prod", "production", "live", "staging", "stage")

        if (productionIndicators.any { indicator ->
                lineContent.contains(indicator, ignoreCase = true)
            }
        ) {
            return true
        }

        // Check if secret has high entropy and doesn't look like test data
        val entropy = calculateEntropy(secretValue)
        if (entropy > 4.0 && !looksLikeTestData(secretValue)) {
            return true
        }

        // Check for real-looking patterns
        return hasRealSecretCharacteristics(secretValue)
    }

    private fun isObviousTestSecret(finding: Finding): Boolean {
        val secretValue =
            if (config.caseSensitive) finding.secretInfo.detectedValue else finding.secretInfo.detectedValue.lowercase()
        val lineContent =
            if (config.caseSensitive) finding.location.lineContent else finding.location.lineContent.lowercase()

        val testIndicators =
            listOf(
                "test",
                "example",
                "sample",
                "demo",
                "mock",
                "fake",
                "dummy",
                "placeholder",
                "xxx",
                "yyy",
                "zzz",
                "abc",
                "123",
                "password",
                "secret",
                "key",
                "token",
                "lorem",
                "ipsum"
            )

        return testIndicators.any { indicator ->
            secretValue.contains(indicator) || lineContent.contains(indicator)
        } || isSimpleTestPattern(secretValue)
    }

    private fun looksLikeTestData(value: String): Boolean {
        val lowerValue = value.lowercase()

        // Common test data patterns
        val testPatterns =
            listOf(
                "test",
                "example",
                "sample",
                "demo",
                "mock",
                "fake",
                "dummy",
                "abc",
                "xyz",
                "foo",
                "bar",
                "baz",
                "qux",
                "lorem",
                "ipsum"
            )

        return testPatterns.any { pattern -> lowerValue.contains(pattern) } ||
            isRepeatingPattern(value) ||
            isSequentialPattern(value)
    }

    private fun hasRealSecretCharacteristics(value: String): Boolean {
        // Real secrets typically have:
        // - Mixed case
        // - Special characters
        // - Decent length
        // - High entropy

        if (value.length < 8) return false

        val hasLower = value.any { it.isLowerCase() }
        val hasUpper = value.any { it.isUpperCase() }
        val hasDigit = value.any { it.isDigit() }
        val hasSpecial = value.any { !it.isLetterOrDigit() }

        val entropy = calculateEntropy(value)

        return (hasLower && hasUpper && (hasDigit || hasSpecial)) && entropy > 3.5
    }

    private fun isSimpleTestPattern(value: String): Boolean {
        // Patterns like "123456", "abcdef", "password123", etc.
        return value.matches(Regex("^[0-9]+$")) ||
            value.matches(Regex("^[a-zA-Z]+$")) ||
            value.matches(Regex("^[a-zA-Z]+[0-9]+$")) ||
            value.matches(Regex("^[0-9]+[a-zA-Z]+$")) ||
            value.length < 6
    }

    private fun isRepeatingPattern(value: String): Boolean {
        if (value.length < 4) return false

        // Check for repeating characters
        val repeatingChars = value.groupBy { it }.values.any { it.size > value.length / 2 }
        if (repeatingChars) return true

        // Check for repeating substrings
        for (len in 2..value.length / 2) {
            val substring = value.substring(0, len)
            if (value == substring.repeat(value.length / len)) {
                return true
            }
        }

        return false
    }

    private fun isSequentialPattern(value: String): Boolean {
        if (value.length < 3) return false

        var sequential = 0
        for (i in 1 until value.length) {
            if (value[i].code == value[i - 1].code + 1 || value[i].code == value[i - 1].code - 1) {
                sequential++
            }
        }

        return sequential >= value.length * 0.7
    }

    private fun calculateEntropy(value: String): Double {
        if (value.isEmpty()) return 0.0

        val frequency = value.groupBy { it }.mapValues { it.value.size.toDouble() / value.length }
        return -frequency.values.sumOf { it * kotlin.math.ln(it) / kotlin.math.ln(2.0) }
    }

    companion object {
        /** Create filter that excludes all test files */
        fun createExcludeAll(): TestFileFilter {
            return TestFileFilter(
                TestFileConfig.builder()
                    .policy(TestFilePolicy.EXCLUDE_ALL)
                    .strictnessLevel(StrictnessLevel.BALANCED)
                    .build()
            )
        }

        /** Create filter for development environments with relaxed test scanning */
        fun createDevelopment(): TestFileFilter {
            return TestFileFilter(
                TestFileConfig.builder()
                    .policy(TestFilePolicy.RELAXED_SCANNING)
                    .allowRealSecrets(false)
                    .strictnessLevel(StrictnessLevel.BALANCED)
                    .excludeTestDataFiles(true)
                    .excludeMockFiles(true)
                    .excludeFixtureFiles(true)
                    .build()
            )
        }

        /** Create filter for CI/CD environments */
        fun createCICD(): TestFileFilter {
            return TestFileFilter(
                TestFileConfig.builder()
                    .policy(TestFilePolicy.INTEGRATION_ONLY)
                    .allowRealSecrets(true)
                    .strictnessLevel(StrictnessLevel.STRICT)
                    .treatIntegrationTestsAsProduction(true)
                    .build()
            )
        }

        /** Create permissive filter that scans all files */
        fun createPermissive(): TestFileFilter {
            return TestFileFilter(
                TestFileConfig.builder()
                    .policy(TestFilePolicy.INCLUDE_ALL)
                    .allowRealSecrets(true)
                    .strictnessLevel(StrictnessLevel.LOOSE)
                    .build()
            )
        }
    }
}
