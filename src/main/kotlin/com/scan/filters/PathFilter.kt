package com.scan.filters

import java.io.File
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * PathFilter class for filtering files based on path patterns.
 */
class PathFilter(
    private val includeGlobs: Set<String> = emptySet(),
    private val excludeGlobs: Set<String> = emptySet(),
    private val includeRegex: Set<String> = emptySet(),
    private val excludeRegex: Set<String> = emptySet(),
    private val includeExact: Set<String> = emptySet(),
    private val excludeExact: Set<String> = emptySet(),
    private val includeDirectories: Set<String> = emptySet(),
    private val excludeDirectories: Set<String> = emptySet(),
    private val caseSensitive: Boolean = true,
    private val matchRelativePaths: Boolean = true,
    private val priority: Int = 5
) : FilterInterface {

    /** Compiled regex patterns for performance */
    private val compiledIncludeRegex: List<Pattern>
    private val compiledExcludeRegex: List<Pattern>

    /** Compiled glob patterns converted to regex */
    private val compiledIncludeGlobs: List<Pattern>
    private val compiledExcludeGlobs: List<Pattern>

    /** Normalized exact paths for matching */
    private val normalizedIncludeExact: Set<String>
    private val normalizedExcludeExact: Set<String>

    /** Normalized directory paths */
    private val normalizedIncludeDirectories: Set<String>
    private val normalizedExcludeDirectories: Set<String>

    init {
        val flags = if (caseSensitive) 0 else Pattern.CASE_INSENSITIVE

        // Compile regex patterns
        compiledIncludeRegex = includeRegex.map { Pattern.compile(it, flags) }
        compiledExcludeRegex = excludeRegex.map { Pattern.compile(it, flags) }

        // Convert globs to regex and compile
        compiledIncludeGlobs = includeGlobs.map { Pattern.compile(globToRegex(it), flags) }
        compiledExcludeGlobs = excludeGlobs.map { Pattern.compile(globToRegex(it), flags) }

        // Normalize exact paths
        normalizedIncludeExact = includeExact.map { normalizePath(it) }.toSet()
        normalizedExcludeExact = excludeExact.map { normalizePath(it) }.toSet()

        // Normalize directory paths
        normalizedIncludeDirectories = includeDirectories.map { normalizePath(it).removeSuffix("/") }.toSet()
        normalizedExcludeDirectories = excludeDirectories.map { normalizePath(it).removeSuffix("/") }.toSet()
    }

    override fun shouldIncludeFile(file: File, relativePath: String): Boolean {
        val pathToMatch = if (matchRelativePaths) {
            normalizePath(relativePath)
        } else {
            normalizePath(file.absolutePath)
        }

        val pathForComparison = if (caseSensitive) pathToMatch else pathToMatch.lowercase()

        // Check exact path exclusions first (highest priority)
        if (isExactMatch(pathForComparison, normalizedExcludeExact)) {
            return false
        }

        // Check directory exclusions
        if (isDirectoryMatch(pathForComparison, normalizedExcludeDirectories)) {
            return false
        }

        // Check regex exclusions
        if (isRegexMatch(pathToMatch, compiledExcludeRegex)) {
            return false
        }

        // Check glob exclusions
        if (isGlobMatch(pathToMatch, compiledExcludeGlobs)) {
            return false
        }

        // If any include patterns are specified, file must match at least one
        val hasIncludePatterns = normalizedIncludeExact.isNotEmpty() ||
            normalizedIncludeDirectories.isNotEmpty() ||
            compiledIncludeRegex.isNotEmpty() ||
            compiledIncludeGlobs.isNotEmpty()

        if (hasIncludePatterns) {
            // Check exact path inclusions
            if (isExactMatch(pathForComparison, normalizedIncludeExact)) {
                return true
            }

            // Check directory inclusions
            if (isDirectoryMatch(pathForComparison, normalizedIncludeDirectories)) {
                return true
            }

            // Check regex inclusions
            if (isRegexMatch(pathToMatch, compiledIncludeRegex)) {
                return true
            }

            // Check glob inclusions
            if (isGlobMatch(pathToMatch, compiledIncludeGlobs)) {
                return true
            }

            // No include patterns matched
            return false
        }

        // No include patterns specified and not excluded
        return true
    }

    override fun shouldIncludeLine(line: String, lineNumber: Int, file: File): Boolean {
        // Path filter doesn't filter at line level
        return true
    }

    override fun getDescription(): String {
        val parts = mutableListOf<String>()

        if (includeGlobs.isNotEmpty()) {
            parts.add("include globs: [${includeGlobs.joinToString(", ")}]")
        }
        if (excludeGlobs.isNotEmpty()) {
            parts.add("exclude globs: [${excludeGlobs.joinToString(", ")}]")
        }

        val sensitivity = if (caseSensitive) "case-sensitive" else "case-insensitive"
        val pathType = if (matchRelativePaths) "relative paths" else "absolute paths"

        return "Path filter (${parts.joinToString(", ")}) - $sensitivity, $pathType"
    }

    override fun getPriority(): Int = priority

    fun isApplicable(file: File): Boolean {
        // Path filter is always applicable
        return true
    }

    override fun validateConfiguration(): List<String> {
        val errors = mutableListOf<String>()

        // Validate regex patterns
        (includeRegex + excludeRegex).forEach { regex ->
            try {
                Pattern.compile(regex)
            } catch (e: PatternSyntaxException) {
                errors.add("Invalid regex pattern '$regex': ${e.message}")
            }
        }

        // Validate glob patterns by converting to regex
        (includeGlobs + excludeGlobs).forEach { glob ->
            try {
                Pattern.compile(globToRegex(glob))
            } catch (e: PatternSyntaxException) {
                errors.add("Invalid glob pattern '$glob': ${e.message}")
            }
        }

        return errors
    }

    /** Helper methods for pattern matching */
    private fun isExactMatch(path: String, patterns: Set<String>): Boolean {
        val pathToCheck = if (caseSensitive) path else path.lowercase()
        val patternsToCheck = if (caseSensitive) patterns else patterns.map { it.lowercase() }.toSet()
        return pathToCheck in patternsToCheck
    }

    private fun isDirectoryMatch(path: String, directories: Set<String>): Boolean {
        val pathToCheck = if (caseSensitive) path else path.lowercase()
        val dirsToCheck = if (caseSensitive) directories else directories.map { it.lowercase() }.toSet()

        return dirsToCheck.any { dir ->
            val dirToCheck = if (caseSensitive) dir else dir.lowercase()
            pathToCheck.startsWith("$dirToCheck/") || pathToCheck == dirToCheck
        }
    }

    private fun isRegexMatch(path: String, patterns: List<Pattern>): Boolean {
        return patterns.any { it.matcher(path).matches() }
    }

    private fun isGlobMatch(path: String, patterns: List<Pattern>): Boolean {
        return patterns.any { it.matcher(path).matches() }
    }

    /** Converts a glob pattern to a regular expression */
    private fun globToRegex(glob: String): String {
        val regex = StringBuilder()
        var i = 0

        while (i < glob.length) {
            when (val char = glob[i]) {
                '*' -> {
                    if (i + 1 < glob.length && glob[i + 1] == '*') {
                        // ** matches any number of directories
                        regex.append(".*")
                        i++ // Skip the second *
                    } else {
                        // * matches anything except directory separator
                        regex.append("[^/]*")
                    }
                }
                '?' -> regex.append("[^/]")
                '[' -> {
                    // Character class
                    regex.append('[')
                    i++
                    if (i < glob.length && glob[i] == '!') {
                        regex.append('^')
                        i++
                    }
                    while (i < glob.length && glob[i] != ']') {
                        if (glob[i] == '\\') {
                            regex.append("\\\\")
                            i++
                        }
                        if (i < glob.length) {
                            regex.append(glob[i])
                        }
                        i++
                    }
                    regex.append(']')
                }
                '\\' -> {
                    // Escape sequence
                    regex.append("\\\\")
                    i++
                    if (i < glob.length) {
                        regex.append(Pattern.quote(glob[i].toString()))
                    }
                }
                else -> {
                    // Regular character - escape regex special characters
                    if (char in ".^$+{}()|") {
                        regex.append('\\')
                    }
                    regex.append(char)
                }
            }
            i++
        }

        return regex.toString()
    }

    private fun normalizePath(path: String): String {
        return path.replace('\\', '/')
    }

    companion object {
        /** Common directory patterns to exclude */
        val COMMON_EXCLUDE_DIRECTORIES = setOf(
            "node_modules", ".git", ".svn", ".hg", ".bzr", "build", "target", "dist", "out", "bin",
            ".gradle", ".maven", ".idea", ".vscode", "__pycache__", ".pytest_cache", ".mypy_cache"
        )

        /** Creates a filter that excludes common build and dependency directories */
        fun excludeCommonDirectories(caseSensitive: Boolean = true): PathFilter {
            return PathFilter(
                excludeDirectories = COMMON_EXCLUDE_DIRECTORIES,
                caseSensitive = caseSensitive,
                priority = 5
            )
        }

        /** Creates a custom path filter with builder pattern support */
        fun custom(): PathFilterBuilder = PathFilterBuilder()
    }
}

/** Builder class for creating PathFilter instances with a fluent API */
class PathFilterBuilder {
    private val includeGlobs = mutableSetOf<String>()
    private val excludeGlobs = mutableSetOf<String>()
    private val includeRegex = mutableSetOf<String>()
    private val excludeRegex = mutableSetOf<String>()
    private val includeExact = mutableSetOf<String>()
    private val excludeExact = mutableSetOf<String>()
    private val includeDirectories = mutableSetOf<String>()
    private val excludeDirectories = mutableSetOf<String>()
    private var caseSensitive = true
    private var matchRelativePaths = true
    private var priority = 5

    fun includeGlob(vararg patterns: String): PathFilterBuilder {
        includeGlobs.addAll(patterns)
        return this
    }

    fun excludeGlob(vararg patterns: String): PathFilterBuilder {
        excludeGlobs.addAll(patterns)
        return this
    }

    fun caseSensitive(sensitive: Boolean = true): PathFilterBuilder {
        this.caseSensitive = sensitive
        return this
    }

    fun priority(priority: Int): PathFilterBuilder {
        this.priority = priority
        return this
    }

    fun build(): PathFilter {
        return PathFilter(
            includeGlobs = includeGlobs.toSet(),
            excludeGlobs = excludeGlobs.toSet(),
            includeRegex = includeRegex.toSet(),
            excludeRegex = excludeRegex.toSet(),
            includeExact = includeExact.toSet(),
            excludeExact = excludeExact.toSet(),
            includeDirectories = includeDirectories.toSet(),
            excludeDirectories = excludeDirectories.toSet(),
            caseSensitive = caseSensitive,
            matchRelativePaths = matchRelativePaths,
            priority = priority
        )
    }
}

/**
 * Extension function to create PathFilterBuilder
 */
fun pathFilter(action: PathFilterBuilder.() -> Unit): PathFilter {
    return PathFilterBuilder().apply(action).build()
}
