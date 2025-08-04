package com.scan.filters

import java.io.File
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Filter implementation that includes or excludes files based on their file paths.
 * 
 * This filter supports multiple pattern matching strategies:
 * - Glob patterns (like *.kt, **\/*.test.*, etc.)
 * - Regular expressions
 * - Exact path matching
 * - Directory-based filtering
 * 
 * Path matching is performed on normalized paths (using forward slashes) and
 * supports both absolute and relative path patterns.
 * 
 * Examples:
 * - Exclude test directories: PathFilter(excludeGlobs = setOf("**\/test\/**", "**\/tests\/**"))
 * - Include only source directories: PathFilter(includeGlobs = setOf("src\/**", "main\/**"))
 * - Exclude specific files: PathFilter(excludeExact = setOf("build.gradle.kts"))
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
) : BaseFilter() {

    /**
     * Compiled regex patterns for performance
     */
    private val compiledIncludeRegex: List<Pattern>
    private val compiledExcludeRegex: List<Pattern>
    
    /**
     * Compiled glob patterns converted to regex
     */
    private val compiledIncludeGlobs: List<Pattern>
    private val compiledExcludeGlobs: List<Pattern>
    
    /**
     * Normalized exact paths for matching
     */
    private val normalizedIncludeExact: Set<String>
    private val normalizedExcludeExact: Set<String>
    
    /**
     * Normalized directory paths
     */
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
        normalizedIncludeDirectories = includeDirectories.map { 
            normalizePath(it).removeSuffix("/") 
        }.toSet()
        normalizedExcludeDirectories = excludeDirectories.map { 
            normalizePath(it).removeSuffix("/") 
        }.toSet()
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
        if (includeRegex.isNotEmpty()) {
            parts.add("include regex: [${includeRegex.joinToString(", ")}]")
        }
        if (excludeRegex.isNotEmpty()) {
            parts.add("exclude regex: [${excludeRegex.joinToString(", ")}]")
        }
        if (includeExact.isNotEmpty()) {
            parts.add("include exact: [${includeExact.joinToString(", ")}]")
        }
        if (excludeExact.isNotEmpty()) {
            parts.add("exclude exact: [${excludeExact.joinToString(", ")}]")
        }
        if (includeDirectories.isNotEmpty()) {
            parts.add("include dirs: [${includeDirectories.joinToString(", ")}]")
        }
        if (excludeDirectories.isNotEmpty()) {
            parts.add("exclude dirs: [${excludeDirectories.joinToString(", ")}]")
        }
        
        val sensitivity = if (caseSensitive) "case-sensitive" else "case-insensitive"
        val pathType = if (matchRelativePaths) "relative paths" else "absolute paths"
        
        return "Path filter (${parts.joinToString(", ")}) - $sensitivity, $pathType"
    }

    override fun getPriority(): Int = priority

    override fun computeApplicability(file: File, fileExtension: String): Boolean {
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
        
        // Check for conflicting exact paths
        val conflictingExact = normalizedIncludeExact.intersect(normalizedExcludeExact)
        if (conflictingExact.isNotEmpty()) {
            errors.add("Paths cannot be both included and excluded: ${conflictingExact.joinToString(", ")}")
        }
        
        // Check for conflicting directories
        val conflictingDirs = normalizedIncludeDirectories.intersect(normalizedExcludeDirectories)
        if (conflictingDirs.isNotEmpty()) {
            errors.add("Directories cannot be both included and excluded: ${conflictingDirs.joinToString(", ")}")
        }
        
        return errors
    }

    override fun getMetadata(): Map<String, Any> {
        return super.getMetadata() + mapOf(
            "includeGlobs" to includeGlobs,
            "excludeGlobs" to excludeGlobs,
            "includeRegex" to includeRegex,
            "excludeRegex" to excludeRegex,
            "includeExact" to includeExact,
            "excludeExact" to excludeExact,
            "includeDirectories" to includeDirectories,
            "excludeDirectories" to excludeDirectories,
            "caseSensitive" to caseSensitive,
            "matchRelativePaths" to matchRelativePaths
        )
    }

    /**
     * Helper methods for pattern matching
     */
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

    /**
     * Converts a glob pattern to a regular expression
     */
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

    companion object {
        /**
         * Common directory patterns to exclude
         */
        val COMMON_EXCLUDE_DIRECTORIES = setOf(
            "node_modules", ".git", ".svn", ".hg", ".bzr",
            "build", "target", "dist", "out", "bin",
            ".gradle", ".maven", ".idea", ".vscode",
            "__pycache__", ".pytest_cache", ".mypy_cache",
            "vendor", "Pods", "Library/Caches"
        )

        /**
         * Common build and dependency directories
         */
        val BUILD_DIRECTORIES = setOf(
            "build", "target", "dist", "out", "bin", "obj",
            ".gradle", "gradle", ".maven", "maven"
        )

        /**
         * Common version control directories
         */
        val VCS_DIRECTORIES = setOf(
            ".git", ".svn", ".hg", ".bzr", "CVS", ".fossil-settings"
        )

        /**
         * Common IDE and editor directories
         */
        val IDE_DIRECTORIES = setOf(
            ".idea", ".vscode", ".vs", ".eclipse", ".settings",
            "*.xcworkspace", "*.xcodeproj"
        )

        /**
         * Common test directory patterns
         */
        val TEST_DIRECTORY_GLOBS = setOf(
            "**/test/**", "**/tests/**", "**/spec/**", "**/specs/**",
            "**/__tests__/**", "**/*.test/**", "**/*.spec/**"
        )

        /**
         * Common source directory patterns
         */
        val SOURCE_DIRECTORY_GLOBS = setOf(
            "src/**", "main/**", "lib/**", "source/**", "sources/**"
        )

        /**
         * Creates a filter that excludes common build and dependency directories
         */
        fun excludeCommonDirectories(caseSensitive: Boolean = true): PathFilter {
            return PathFilter(
                excludeDirectories = COMMON_EXCLUDE_DIRECTORIES,
                caseSensitive = caseSensitive,
                priority = 5
            )
        }

        /**
         * Creates a filter that excludes version control directories
         */
        fun excludeVcsDirectories(caseSensitive: Boolean = true): PathFilter {
            return PathFilter(
                excludeDirectories = VCS_DIRECTORIES,
                caseSensitive = caseSensitive,
                priority = 8
            )
        }

        /**
         * Creates a filter that excludes test directories
         */
        fun excludeTestDirectories(caseSensitive: Boolean = true): PathFilter {
            return PathFilter(
                excludeGlobs = TEST_DIRECTORY_GLOBS,
                caseSensitive = caseSensitive,
                priority = 6
            )
        }

        /**
         * Creates a filter that includes only source directories
         */
        fun includeSourceDirectories(caseSensitive: Boolean = true): PathFilter {
            return PathFilter(
                includeGlobs = SOURCE_DIRECTORY_GLOBS,
                caseSensitive = caseSensitive,
                priority = 5
            )
        }

        /**
         * Creates a comprehensive filter for typical scanning scenarios
         */
        fun forScanning(
            excludeTests: Boolean = true,
            excludeVcs: Boolean = true,
            excludeBuild: Boolean = true,
            caseSensitive: Boolean = true
        ): PathFilter {
            val excludeDirs = mutableSetOf<String>()
            val excludeGlobs = mutableSetOf<String>()
            
            if (excludeVcs) {
                excludeDirs.addAll(VCS_DIRECTORIES)
            }
            if (excludeBuild) {
                excludeDirs.addAll(BUILD_DIRECTORIES)
                excludeDirs.addAll(IDE_DIRECTORIES)
            }
            if (excludeTests) {
                excludeGlobs.addAll(TEST_DIRECTORY_GLOBS)
            }
            
            return PathFilter(
                excludeDirectories = excludeDirs,
                excludeGlobs = excludeGlobs,
                caseSensitive = caseSensitive,
                priority = 5
            )
        }

        /**
         * Creates a custom path filter with builder pattern support
         */
        fun custom(): PathFilterBuilder = PathFilterBuilder()
    }
}

/**
 * Builder class for creating PathFilter instances with a fluent API
 */
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

    fun includeRegex(vararg patterns: String): PathFilterBuilder {
        includeRegex.addAll(patterns)
        return this
    }

    fun excludeRegex(vararg patterns: String): PathFilterBuilder {
        excludeRegex.addAll(patterns)
        return this
    }

    fun includeExact(vararg paths: String): PathFilterBuilder {
        includeExact.addAll(paths)
        return this
    }

    fun excludeExact(vararg paths: String): PathFilterBuilder {
        excludeExact.addAll(paths)
        return this
    }

    fun includeDirectory(vararg directories: String): PathFilterBuilder {
        includeDirectories.addAll(directories)
        return this
    }

    fun excludeDirectory(vararg directories: String): PathFilterBuilder {
        excludeDirectories.addAll(directories)
        return this
    }

    fun excludeCommonDirectories(): PathFilterBuilder {
        excludeDirectories.addAll(PathFilter.COMMON_EXCLUDE_DIRECTORIES)
        return this
    }

    fun excludeVcsDirectories(): PathFilterBuilder {
        excludeDirectories.addAll(PathFilter.VCS_DIRECTORIES)
        return this
    }

    fun excludeTestDirectories(): PathFilterBuilder {
        excludeGlobs.addAll(PathFilter.TEST_DIRECTORY_GLOBS)
        return this
    }

    fun includeSourceDirectories(): PathFilterBuilder {
        includeGlobs.addAll(PathFilter.SOURCE_DIRECTORY_GLOBS)
        return this
    }

    fun caseSensitive(sensitive: Boolean = true): PathFilterBuilder {
        this.caseSensitive = sensitive
        return this
    }

    fun matchRelativePaths(relative: Boolean = true): PathFilterBuilder {
        this.matchRelativePaths = relative
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
fun pathFilter(): PathFilterBuilder = PathFilterBuilder()
fun pathFilter(): PathFilterBuilder = PathFilterBuilder()