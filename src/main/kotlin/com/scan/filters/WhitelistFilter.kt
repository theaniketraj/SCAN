package com.scan.filters

import com.scan.core.Finding
import com.scan.core.ScanResult
import java.io.File
import java.util.regex.Pattern

/**
 * Filter implementation that manages whitelisting and exclusion of scan results. This filter can
 * exclude results based on:
 * - File paths
 * - Content patterns
 * - Specific line content
 * - Secret values
 * - Comment patterns
 */
class WhitelistFilter(private val whitelistConfig: WhitelistConfig = WhitelistConfig()) :
        FilterInterface {

    /** Configuration class for whitelist filter options */
    data class WhitelistConfig(
            val excludedPaths: Set<String> = emptySet(),
            val excludedPathPatterns: Set<Pattern> = emptySet(),
            val excludedSecretPatterns: Set<Pattern> = emptySet(),
            val excludedLinePatterns: Set<Pattern> = emptySet(),
            val excludedFileExtensions: Set<String> = emptySet(),
            val excludedCommentPatterns: Set<Pattern> = emptySet(),
            val caseSensitive: Boolean = true,
            val allowTestFiles: Boolean = true,
            val excludedDirectories: Set<String> =
                    setOf("node_modules", ".git", ".gradle", "build", "target", ".idea", ".vscode")
    ) {
        companion object {
            fun builder() = WhitelistConfigBuilder()
        }
    }

    /** Builder class for creating WhitelistConfig instances */
    class WhitelistConfigBuilder {
        private val excludedPaths = mutableSetOf<String>()
        private val excludedPathPatterns = mutableSetOf<Pattern>()
        private val excludedSecretPatterns = mutableSetOf<Pattern>()
        private val excludedLinePatterns = mutableSetOf<Pattern>()
        private val excludedFileExtensions = mutableSetOf<String>()
        private val excludedCommentPatterns = mutableSetOf<Pattern>()
        private val excludedDirectories = mutableSetOf<String>()
        private var caseSensitive = true
        private var allowTestFiles = true

        fun excludePath(path: String) = apply { excludedPaths.add(path) }
        fun excludePaths(paths: Collection<String>) = apply { excludedPaths.addAll(paths) }

        fun excludePathPattern(pattern: String) = apply {
            excludedPathPatterns.add(compilePattern(pattern))
        }
        fun excludePathPatterns(patterns: Collection<String>) = apply {
            patterns.forEach { excludedPathPatterns.add(compilePattern(it)) }
        }

        fun excludeSecretPattern(pattern: String) = apply {
            excludedSecretPatterns.add(compilePattern(pattern))
        }
        fun excludeSecretPatterns(patterns: Collection<String>) = apply {
            patterns.forEach { excludedSecretPatterns.add(compilePattern(it)) }
        }

        fun excludeLinePattern(pattern: String) = apply {
            excludedLinePatterns.add(compilePattern(pattern))
        }
        fun excludeLinePatterns(patterns: Collection<String>) = apply {
            patterns.forEach { excludedLinePatterns.add(compilePattern(it)) }
        }

        fun excludeFileExtension(extension: String) = apply {
            excludedFileExtensions.add(extension.removePrefix("."))
        }
        fun excludeFileExtensions(extensions: Collection<String>) = apply {
            extensions.forEach { excludedFileExtensions.add(it.removePrefix(".")) }
        }

        fun excludeCommentPattern(pattern: String) = apply {
            excludedCommentPatterns.add(compilePattern(pattern))
        }
        fun excludeCommentPatterns(patterns: Collection<String>) = apply {
            patterns.forEach { excludedCommentPatterns.add(compilePattern(it)) }
        }

        fun excludeDirectory(directory: String) = apply { excludedDirectories.add(directory) }
        fun excludeDirectories(directories: Collection<String>) = apply {
            excludedDirectories.addAll(directories)
        }

        fun caseSensitive(sensitive: Boolean) = apply { caseSensitive = sensitive }
        fun allowTestFiles(allow: Boolean) = apply { allowTestFiles = allow }

        private fun compilePattern(pattern: String): Pattern {
            val flags = if (caseSensitive) 0 else Pattern.CASE_INSENSITIVE
            return Pattern.compile(pattern, flags)
        }

        fun build() =
                WhitelistConfig(
                        excludedPaths = excludedPaths.toSet(),
                        excludedPathPatterns = excludedPathPatterns.toSet(),
                        excludedSecretPatterns = excludedSecretPatterns.toSet(),
                        excludedLinePatterns = excludedLinePatterns.toSet(),
                        excludedFileExtensions = excludedFileExtensions.toSet(),
                        excludedCommentPatterns = excludedCommentPatterns.toSet(),
                        caseSensitive = caseSensitive,
                        allowTestFiles = allowTestFiles,
                        excludedDirectories = excludedDirectories.toSet()
                )
    }

    override fun shouldIncludeFile(file: File, relativePath: String): Boolean {
        val filePath = file.absolutePath.replace("\\", "/")

        // Check if file is in excluded directory
        if (isInExcludedDirectory(filePath)) {
            return false
        }

        // Check if file extension is excluded
        if (isFileExtensionExcluded(file)) {
            return false
        }

        // Check if specific path is excluded
        if (isPathExcluded(filePath)) {
            return false
        }

        // Check if path matches excluded patterns
        if (isPathPatternExcluded(filePath)) {
            return false
        }

        // Check test file policy
        if (!whitelistConfig.allowTestFiles && isTestFile(file)) {
            return false
        }

        return true
    }

    override fun shouldIncludeLine(line: String, lineNumber: Int, file: File): Boolean {
        return shouldIncludeFile(file, file.path)
    }

    override fun getDescription(): String {
        return "Whitelist filter that excludes files and content based on patterns and paths"
    }

    fun shouldIncludeFinding(finding: Finding): Boolean {
        // Check if the secret value itself should be excluded
        if (isSecretExcluded(finding.secretInfo.detectedValue)) {
            return false
        }

        // Check if the line content should be excluded
        if (isLineContentExcluded(finding.location.lineContent)) {
            return false
        }

        // Check if it's in a comment and comments are excluded
        if (isCommentExcluded(finding.location.lineContent)) {
            return false
        }

        return true
    }

    private fun isInExcludedDirectory(filePath: String): Boolean {
        return whitelistConfig.excludedDirectories.any { excludedDir ->
            val normalizedPath = filePath.replace("\\", "/")
            normalizedPath.contains("/$excludedDir/") ||
                    normalizedPath.endsWith("/$excludedDir") ||
                    normalizedPath.startsWith("$excludedDir/")
        }
    }

    private fun isFileExtensionExcluded(file: File): Boolean {
        val extension = file.extension.lowercase()
        return whitelistConfig.excludedFileExtensions.any { excludedExt ->
            if (whitelistConfig.caseSensitive) {
                extension == excludedExt
            } else {
                extension.equals(excludedExt, ignoreCase = true)
            }
        }
    }

    private fun isPathExcluded(filePath: String): Boolean {
        return whitelistConfig.excludedPaths.any { excludedPath ->
            if (whitelistConfig.caseSensitive) {
                filePath.contains(excludedPath)
            } else {
                filePath.contains(excludedPath, ignoreCase = true)
            }
        }
    }

    private fun isPathPatternExcluded(filePath: String): Boolean {
        return whitelistConfig.excludedPathPatterns.any { pattern ->
            pattern.matcher(filePath).find()
        }
    }

    private fun isSecretExcluded(secretValue: String): Boolean {
        return whitelistConfig.excludedSecretPatterns.any { pattern ->
            pattern.matcher(secretValue).find()
        }
    }

    private fun isLineContentExcluded(lineContent: String): Boolean {
        return whitelistConfig.excludedLinePatterns.any { pattern ->
            pattern.matcher(lineContent).find()
        }
    }

    private fun isCommentExcluded(lineContent: String): Boolean {
        if (whitelistConfig.excludedCommentPatterns.isEmpty()) {
            return false
        }

        // Check if line appears to be a comment
        val trimmedLine = lineContent.trim()
        val isComment = isCommentLine(trimmedLine)

        if (!isComment) {
            return false
        }

        // Check if comment content matches excluded patterns
        return whitelistConfig.excludedCommentPatterns.any { pattern ->
            pattern.matcher(lineContent).find()
        }
    }

    private fun isCommentLine(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith("//") ||
                trimmed.startsWith("#") ||
                trimmed.startsWith("/*") ||
                trimmed.startsWith("*") ||
                trimmed.startsWith("<!--") ||
                trimmed.startsWith("'''") ||
                trimmed.startsWith("\"\"\"") ||
                trimmed.startsWith("--") ||
                trimmed.startsWith("rem ", ignoreCase = true) ||
                trimmed.startsWith(";")
    }

    private fun isTestFile(file: File): Boolean {
        val fileName = file.name.lowercase()
        val filePath = file.absolutePath.replace("\\", "/").lowercase()

        return fileName.contains("test") ||
                fileName.contains("spec") ||
                filePath.contains("/test/") ||
                filePath.contains("/tests/") ||
                filePath.contains("/spec/") ||
                filePath.contains("/specs/") ||
                filePath.contains("/__tests__/") ||
                filePath.contains("/test-") ||
                filePath.contains("-test") ||
                fileName.endsWith("test.kt") ||
                fileName.endsWith("test.java") ||
                fileName.endsWith("spec.kt") ||
                fileName.endsWith("spec.java") ||
                fileName.endsWith(".test.js") ||
                fileName.endsWith(".spec.js") ||
                fileName.endsWith(".test.ts") ||
                fileName.endsWith(".spec.ts")
    }

    /** Add additional exclusion patterns at runtime */
    fun addExcludedSecretPattern(pattern: String): WhitelistFilter {
        val newPatterns = whitelistConfig.excludedSecretPatterns.toMutableSet()
        val flags = if (whitelistConfig.caseSensitive) 0 else Pattern.CASE_INSENSITIVE
        newPatterns.add(Pattern.compile(pattern, flags))

        return WhitelistFilter(whitelistConfig.copy(excludedSecretPatterns = newPatterns))
    }

    /** Add additional path exclusions at runtime */
    fun addExcludedPath(path: String): WhitelistFilter {
        val newPaths = whitelistConfig.excludedPaths.toMutableSet()
        newPaths.add(path)

        return WhitelistFilter(whitelistConfig.copy(excludedPaths = newPaths))
    }

    /** Create a new filter with updated configuration */
    fun withConfig(config: WhitelistConfig): WhitelistFilter {
        return WhitelistFilter(config)
    }

    /** Get current configuration */
    fun getConfig(): WhitelistConfig = whitelistConfig

    companion object {
        /** Create a default whitelist filter with common exclusions */
        fun createDefault(): WhitelistFilter {
            return WhitelistFilter(
                    WhitelistConfig.builder()
                            .excludeFileExtensions(
                                    listOf(
                                            "class",
                                            "jar",
                                            "war",
                                            "ear",
                                            "zip",
                                            "tar",
                                            "gz",
                                            "7z",
                                            "exe",
                                            "dll",
                                            "so",
                                            "dylib",
                                            "bin",
                                            "obj",
                                            "o",
                                            "a",
                                            "png",
                                            "jpg",
                                            "jpeg",
                                            "gif",
                                            "bmp",
                                            "ico",
                                            "svg",
                                            "webp",
                                            "mp3",
                                            "mp4",
                                            "avi",
                                            "mov",
                                            "wmv",
                                            "flv",
                                            "wav",
                                            "ogg",
                                            "pdf",
                                            "doc",
                                            "docx",
                                            "xls",
                                            "xlsx",
                                            "ppt",
                                            "pptx",
                                            "lock",
                                            "cache",
                                            "tmp",
                                            "temp",
                                            "log"
                                    )
                            )
                            .excludePathPatterns(
                                    listOf(
                                            ".*\\.git/.*",
                                            ".*node_modules/.*",
                                            ".*\\.gradle/.*",
                                            ".*/build/.*",
                                            ".*/target/.*",
                                            ".*\\.idea/.*",
                                            ".*\\.vscode/.*",
                                            ".*/\\..*cache.*/.*"
                                    )
                            )
                            .excludeSecretPatterns(
                                    listOf(
                                            "(?i)(example|sample|test|demo|placeholder|dummy|fake|mock)",
                                            "^[0-9]+$", // Pure numbers
                                            "^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$", // UUIDs (common false positives)
                                            "(?i)(lorem|ipsum|dolor|consectetur|adipiscing)"
                                    )
                            )
                            .excludeLinePatterns(
                                    listOf(
                                            "(?i).*todo.*",
                                            "(?i).*fixme.*",
                                            "(?i).*@deprecated.*",
                                            ".*println.*",
                                            ".*console\\.log.*",
                                            ".*System\\.out\\.print.*"
                                    )
                            )
                            .excludeCommentPatterns(
                                    listOf(
                                            "(?i).*example.*",
                                            "(?i).*sample.*",
                                            "(?i).*test.*key.*",
                                            "(?i).*dummy.*",
                                            "(?i).*placeholder.*"
                                    )
                            )
                            .caseSensitive(false)
                            .allowTestFiles(false)
                            .build()
            )
        }

        /** Create a strict filter that only excludes binary files and build artifacts */
        fun createStrict(): WhitelistFilter {
            return WhitelistFilter(
                    WhitelistConfig.builder()
                            .excludeFileExtensions(
                                    listOf(
                                            "class",
                                            "jar",
                                            "war",
                                            "ear",
                                            "zip",
                                            "tar",
                                            "gz",
                                            "exe",
                                            "dll",
                                            "so",
                                            "dylib",
                                            "bin"
                                    )
                            )
                            .excludeDirectories(listOf(".git", ".gradle", "build", "target"))
                            .caseSensitive(true)
                            .allowTestFiles(true)
                            .build()
            )
        }

        /** Create a permissive filter for development environments */
        fun createPermissive(): WhitelistFilter {
            return WhitelistFilter(
                    WhitelistConfig.builder()
                            .excludeFileExtensions(
                                    listOf(
                                            "class",
                                            "jar",
                                            "war",
                                            "ear",
                                            "zip",
                                            "tar",
                                            "gz",
                                            "7z",
                                            "exe",
                                            "dll",
                                            "so",
                                            "dylib",
                                            "bin",
                                            "obj",
                                            "o",
                                            "a",
                                            "png",
                                            "jpg",
                                            "jpeg",
                                            "gif",
                                            "bmp",
                                            "ico",
                                            "svg",
                                            "mp3",
                                            "mp4",
                                            "avi",
                                            "mov",
                                            "wmv",
                                            "flv",
                                            "wav",
                                            "pdf",
                                            "doc",
                                            "docx",
                                            "xls",
                                            "xlsx",
                                            "ppt",
                                            "pptx"
                                    )
                            )
                            .excludePathPatterns(
                                    listOf(
                                            ".*\\.git/.*",
                                            ".*node_modules/.*",
                                            ".*\\.gradle/.*",
                                            ".*/build/.*",
                                            ".*/target/.*"
                                    )
                            )
                            .excludeSecretPatterns(
                                    listOf(
                                            "(?i)(example|sample|test|demo|placeholder|dummy|fake|mock|lorem|ipsum)",
                                            "^[0-9]+$",
                                            "^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$"
                                    )
                            )
                            .caseSensitive(false)
                            .allowTestFiles(false)
                            .build()
            )
        }
    }
}
