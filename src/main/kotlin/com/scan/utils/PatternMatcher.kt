package com.scan.utils

import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import kotlin.math.min

/**
 * Advanced pattern matching utility for detecting secrets and sensitive data using regex patterns.
 * Provides optimized pattern compilation, caching, and matching with context awareness.
 *
 * Features:
 * - Pattern compilation caching for performance
 * - Context-aware matching (ignoring comments, strings, etc.)
 * - Multi-line pattern support
 * - Pattern validation and error handling
 * - Match result with detailed information
 * - Support for named capture groups
 *
 * @author SCAN Plugin Team
 * @since 1.0.0
 */
object PatternMatcher {

    /**
     * Cache for compiled patterns to avoid recompilation overhead. Thread-safe concurrent map for
     * multi-threaded scanning.
     */
    private val patternCache = ConcurrentHashMap<String, Pattern>()

    /** Maximum cache size to prevent memory issues with dynamic patterns. */
    private const val MAX_CACHE_SIZE = 1000

    /** Maximum length of content to analyze in a single pass for performance. */
    private const val MAX_CONTENT_LENGTH = 1_000_000

    /** Default flags for pattern compilation. */
    private const val DEFAULT_PATTERN_FLAGS = Pattern.MULTILINE or Pattern.DOTALL

    /** Patterns for detecting different types of code contexts to ignore. */
    private val COMMENT_PATTERNS =
            listOf(
                    Pattern.compile("//.*?$", Pattern.MULTILINE), // Single-line comments
                    Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL), // Multi-line comments
                    Pattern.compile("#.*?$", Pattern.MULTILINE), // Shell/Python comments
                    Pattern.compile("<!--.*?-->", Pattern.DOTALL), // HTML comments
                    Pattern.compile("\\{\\s*#.*?#\\s*\\}", Pattern.DOTALL) // Template comments
            )

    private val STRING_PATTERNS =
            listOf(
                    Pattern.compile(
                            "\"(?:[^\"\\\\]|\\\\.)*\"",
                            Pattern.DOTALL
                    ), // Double-quoted strings
                    Pattern.compile(
                            "'(?:[^'\\\\]|\\\\.)*'",
                            Pattern.DOTALL
                    ), // Single-quoted strings
                    Pattern.compile("`(?:[^`\\\\]|\\\\.)*`", Pattern.DOTALL) // Backtick strings
            )

    /**
     * Compiles a regex pattern with caching for performance optimization.
     *
     * @param regex The regular expression pattern
     * @param flags Optional regex flags (default: MULTILINE | DOTALL)
     * @return Compiled Pattern object
     * @throws PatternSyntaxException if the regex is invalid
     */
    fun compilePattern(regex: String, flags: Int = DEFAULT_PATTERN_FLAGS): Pattern {
        val cacheKey = "$regex:$flags"

        return patternCache.computeIfAbsent(cacheKey) { _ ->
            // Clear cache if it gets too large
            if (patternCache.size >= MAX_CACHE_SIZE) {
                patternCache.clear()
            }

            try {
                Pattern.compile(regex, flags)
            } catch (e: PatternSyntaxException) {
                throw PatternSyntaxException(
                        "Invalid regex pattern: ${e.description}",
                        regex,
                        e.index
                )
            }
        }
    }

    /**
     * Finds all matches of a pattern in the given content.
     *
     * @param content The content to search in
     * @param pattern The regex pattern to match
     * @param ignoreContext Whether to ignore matches in comments/strings
     * @param maxMatches Maximum number of matches to return (0 = unlimited)
     * @return List of PatternMatch objects
     */
    fun findMatches(
            content: String,
            pattern: String,
            ignoreContext: Boolean = true,
            maxMatches: Int = 0
    ): List<PatternMatch> {
        return findMatches(content, compilePattern(pattern), ignoreContext, maxMatches)
    }

    /**
     * Finds all matches of a compiled pattern in the given content.
     *
     * @param content The content to search in
     * @param pattern The compiled Pattern to match
     * @param ignoreContext Whether to ignore matches in comments/strings
     * @param maxMatches Maximum number of matches to return (0 = unlimited)
     * @return List of PatternMatch objects
     */
    fun findMatches(
            content: String,
            pattern: Pattern,
            ignoreContext: Boolean = true,
            maxMatches: Int = 0
    ): List<PatternMatch> {
        if (content.isEmpty()) return emptyList()

        // Truncate very large content for performance
        val searchContent =
                if (content.length > MAX_CONTENT_LENGTH) {
                    content.substring(0, MAX_CONTENT_LENGTH)
                } else {
                    content
                }

        val matches = mutableListOf<PatternMatch>()
        val matcher = pattern.matcher(searchContent)
        val ignoredRanges = if (ignoreContext) getIgnoredRanges(searchContent) else emptyList()

        while (matcher.find() && (maxMatches == 0 || matches.size < maxMatches)) {
            val start = matcher.start()
            val end = matcher.end()

            // Skip if match is in an ignored context (comment/string)
            if (ignoreContext && isInIgnoredRange(start, end, ignoredRanges)) {
                continue
            }

            val matchedText = matcher.group()
            val groups = mutableMapOf<String, String>()

            // Extract named capture groups
            try {
                for (i in 1..matcher.groupCount()) {
                    val groupValue = matcher.group(i)
                    if (groupValue != null) {
                        groups["group$i"] = groupValue
                    }
                }
            } catch (e: Exception) {
                // Ignore group extraction errors
            }

            val lineInfo = getLineInfo(searchContent, start)

            matches.add(
                    PatternMatch(
                            matchedText = matchedText,
                            startIndex = start,
                            endIndex = end,
                            lineNumber = lineInfo.lineNumber,
                            columnNumber = lineInfo.columnNumber,
                            lineContent = lineInfo.lineContent,
                            pattern = pattern.pattern(),
                            groups = groups
                    )
            )
        }

        return matches
    }

    /**
     * Checks if a pattern matches anywhere in the content.
     *
     * @param content The content to search in
     * @param pattern The regex pattern to match
     * @param ignoreContext Whether to ignore matches in comments/strings
     * @return true if pattern matches, false otherwise
     */
    fun hasMatch(content: String, pattern: String, ignoreContext: Boolean = true): Boolean {
        return findMatches(content, pattern, ignoreContext, maxMatches = 1).isNotEmpty()
    }

    /**
     * Validates if a regex pattern is syntactically correct.
     *
     * @param regex The regular expression to validate
     * @return PatternValidationResult with validation status and error details
     */
    fun validatePattern(regex: String): PatternValidationResult {
        return try {
            Pattern.compile(regex)
            PatternValidationResult(isValid = true, errorMessage = null)
        } catch (e: PatternSyntaxException) {
            PatternValidationResult(
                    isValid = false,
                    errorMessage = "Invalid regex at position ${e.index}: ${e.description}"
            )
        }
    }

    /**
     * Finds matches across multiple patterns efficiently.
     *
     * @param content The content to search in
     * @param patterns List of regex patterns to match
     * @param ignoreContext Whether to ignore matches in comments/strings
     * @param maxMatchesPerPattern Maximum matches per pattern (0 = unlimited)
     * @return Map of pattern to list of matches
     */
    fun findMultiplePatternMatches(
            content: String,
            patterns: List<String>,
            ignoreContext: Boolean = true,
            maxMatchesPerPattern: Int = 0
    ): Map<String, List<PatternMatch>> {
        val results = mutableMapOf<String, List<PatternMatch>>()

        patterns.forEach { pattern ->
            try {
                results[pattern] =
                        findMatches(content, pattern, ignoreContext, maxMatchesPerPattern)
            } catch (e: PatternSyntaxException) {
                // Skip invalid patterns, could log this
                results[pattern] = emptyList()
            }
        }

        return results
    }

    /**
     * Extracts potential secrets using a combined approach of patterns and context analysis.
     *
     * @param content The content to analyze
     * @param secretPatterns List of regex patterns for different secret types
     * @param contextKeywords Keywords that might indicate secrets nearby
     * @return List of potential secrets with confidence scores
     */
    fun extractPotentialSecrets(
            content: String,
            secretPatterns: List<String>,
            contextKeywords: List<String> = DEFAULT_CONTEXT_KEYWORDS
    ): List<PotentialSecret> {
        val results = mutableListOf<PotentialSecret>()
        val allMatches = findMultiplePatternMatches(content, secretPatterns, ignoreContext = true)

        allMatches.forEach { (pattern, matches) ->
            matches.forEach { match ->
                val confidence = calculateConfidence(match, content, contextKeywords)
                results.add(
                        PotentialSecret(
                                value = match.matchedText,
                                pattern = pattern,
                                location = match,
                                confidence = confidence,
                                secretType = inferSecretType(pattern, match.matchedText)
                        )
                )
            }
        }

        return results.sortedByDescending { it.confidence }
    }

    /** Gets ranges of content that should be ignored (comments, strings, etc.). */
    private fun getIgnoredRanges(content: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()

        // Add comment ranges
        COMMENT_PATTERNS.forEach { pattern ->
            val matcher = pattern.matcher(content)
            while (matcher.find()) {
                ranges.add(matcher.start()..matcher.end())
            }
        }

        // Add string literal ranges
        STRING_PATTERNS.forEach { pattern ->
            val matcher = pattern.matcher(content)
            while (matcher.find()) {
                ranges.add(matcher.start()..matcher.end())
            }
        }

        // Merge overlapping ranges and sort
        return mergeOverlappingRanges(ranges).sortedBy { it.first }
    }

    /** Checks if a match falls within any ignored range. */
    private fun isInIgnoredRange(start: Int, end: Int, ignoredRanges: List<IntRange>): Boolean {
        return ignoredRanges.any { range -> start >= range.first && end <= range.last }
    }

    /** Merges overlapping ranges to optimize ignored range checking. */
    private fun mergeOverlappingRanges(ranges: List<IntRange>): List<IntRange> {
        if (ranges.isEmpty()) return emptyList()

        val sorted = ranges.sortedBy { it.first }
        val merged = mutableListOf<IntRange>()
        var current = sorted.first()

        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (current.last >= next.first - 1) {
                // Overlapping or adjacent ranges, merge them
                current = current.first..maxOf(current.last, next.last)
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)

        return merged
    }

    /** Gets line information for a position in the content. */
    private fun getLineInfo(content: String, position: Int): LineInfo {
        val lines = content.substring(0, min(position, content.length)).split('\n')
        val lineNumber = lines.size
        val columnNumber = lines.lastOrNull()?.length ?: 0

        // Get the full line content
        val allLines = content.split('\n')
        val lineContent =
                if (lineNumber <= allLines.size) {
                    allLines[lineNumber - 1]
                } else {
                    ""
                }

        return LineInfo(lineNumber, columnNumber, lineContent)
    }

    /** Calculates confidence score for a potential secret based on context. */
    private fun calculateConfidence(
            match: PatternMatch,
            content: String,
            contextKeywords: List<String>
    ): Double {
        var confidence = 0.5 // Base confidence

        // Check for context keywords nearby
        val contextWindow = getContextWindow(content, match.startIndex, match.endIndex, 100)
        val keywordMatches =
                contextKeywords.count { keyword ->
                    contextWindow.contains(keyword, ignoreCase = true)
                }

        confidence += keywordMatches * 0.1

        // Length-based confidence adjustment
        when (match.matchedText.length) {
            in 20..60 -> confidence += 0.2 // Typical secret length
            in 8..19 -> confidence += 0.1 // Short but possible
            in 61..100 -> confidence += 0.1 // Long but possible
            else -> confidence -= 0.1 // Very short or very long
        }

        // Pattern complexity (more specific patterns get higher confidence)
        if (match.pattern.contains("\\b") || match.pattern.contains("(?i)")) {
            confidence += 0.1
        }

        return minOf(1.0, maxOf(0.0, confidence))
    }

    /** Gets a context window around a match for analysis. */
    private fun getContextWindow(content: String, start: Int, end: Int, windowSize: Int): String {
        val contextStart = maxOf(0, start - windowSize)
        val contextEnd = minOf(content.length, end + windowSize)
        return content.substring(contextStart, contextEnd)
    }

    /** Infers the type of secret based on pattern and content. */
    private fun inferSecretType(pattern: String, content: String): SecretType {
        return when {
            pattern.contains("api[_-]?key", ignoreCase = true) -> SecretType.API_KEY
            pattern.contains("password", ignoreCase = true) -> SecretType.PASSWORD
            pattern.contains("token", ignoreCase = true) -> SecretType.TOKEN
            pattern.contains("secret", ignoreCase = true) -> SecretType.SECRET
            pattern.contains("key", ignoreCase = true) -> SecretType.PRIVATE_KEY
            content.startsWith("sk_") -> SecretType.API_KEY
            content.startsWith("pk_") -> SecretType.PUBLIC_KEY
            content.length == 32 && content.all { it.isLetterOrDigit() } -> SecretType.HASH
            else -> SecretType.UNKNOWN
        }
    }

    /** Default context keywords that might indicate secrets. */
    val DEFAULT_CONTEXT_KEYWORDS =
            listOf(
                    "key",
                    "secret",
                    "password",
                    "token",
                    "auth",
                    "api",
                    "credential",
                    "private",
                    "public",
                    "cert",
                    "signature",
                    "hash",
                    "salt"
            )

    // Pattern matching convenience methods for ContextAwareDetector
    fun matchesApiKeyPattern(value: String): Boolean {
        val apiKeyPatterns = listOf(
            "AIza[0-9A-Za-z-_]{35}", // Google API key
            "AKIA[0-9A-Z]{16}", // AWS Access Key
            "xox[baprs]-([0-9a-zA-Z]{10,48})?", // Slack token
            "sk-[a-zA-Z0-9]{48}" // OpenAI API key
        )
        return apiKeyPatterns.any { hasMatch(value, it, false) }
    }

    fun matchesPrivateKeyPattern(value: String): Boolean {
        val privateKeyPatterns = listOf(
            "-----BEGIN PRIVATE KEY-----",
            "-----BEGIN RSA PRIVATE KEY-----",
            "-----BEGIN OPENSSH PRIVATE KEY-----",
            "-----BEGIN EC PRIVATE KEY-----"
        )
        return privateKeyPatterns.any { value.contains(it, ignoreCase = true) }
    }

    fun matchesPasswordPattern(value: String): Boolean {
        val passwordPatterns = listOf(
            "password\\s*[=:]\\s*['\"]?([^'\"\\s]+)",
            "passwd\\s*[=:]\\s*['\"]?([^'\"\\s]+)",
            "pwd\\s*[=:]\\s*['\"]?([^'\"\\s]+)"
        )
        return passwordPatterns.any { hasMatch(value, it, false) }
    }

    fun matchesTokenPattern(value: String): Boolean {
        val tokenPatterns = listOf(
            "gh[pousr]_[A-Za-z0-9_]{36}", // GitHub token
            "xox[baprs]-([0-9a-zA-Z]{10,48})?", // Slack token
            "[a-zA-Z0-9]{40}" // Generic 40-char token
        )
        return tokenPatterns.any { hasMatch(value, it, false) }
    }

    fun matchesDatabaseUrlPattern(value: String): Boolean {
        val dbPatterns = listOf(
            "jdbc:[a-zA-Z0-9]+://[^\\s]+",
            "mongodb://[^\\s]+",
            "mysql://[^\\s]+",
            "postgresql://[^\\s]+",
            "redis://[^\\s]+"
        )
        return dbPatterns.any { hasMatch(value, it, false) }
    }

    fun matchesCertificatePattern(value: String): Boolean {
        val certPatterns = listOf(
            "-----BEGIN CERTIFICATE-----",
            "-----BEGIN X509 CERTIFICATE-----",
            "-----BEGIN TRUSTED CERTIFICATE-----"
        )
        return certPatterns.any { value.contains(it, ignoreCase = true) }
    }
}

/** Represents a pattern match with detailed location and context information. */
data class PatternMatch(
        val matchedText: String,
        val startIndex: Int,
        val endIndex: Int,
        val lineNumber: Int,
        val columnNumber: Int,
        val lineContent: String,
        val pattern: String,
        val groups: Map<String, String> = emptyMap()
) {
    val length: Int
        get() = matchedText.length

    fun getDescription(): String {
        return "Match at line $lineNumber, column $columnNumber: '$matchedText'"
    }
}

/** Line information for a position in content. */
private data class LineInfo(val lineNumber: Int, val columnNumber: Int, val lineContent: String)

/** Result of pattern validation. */
data class PatternValidationResult(val isValid: Boolean, val errorMessage: String?)

/** Represents a potential secret found through pattern matching. */
data class PotentialSecret(
        val value: String,
        val pattern: String,
        val location: PatternMatch,
        val confidence: Double,
        val secretType: SecretType
) {
    fun getDescription(): String {
        val confidencePercent = (confidence * 100).toInt()
        return "Potential ${secretType.displayName} (${confidencePercent}% confidence) at line ${location.lineNumber}"
    }
}

/** Types of secrets that can be detected. */
enum class SecretType(val displayName: String) {
    API_KEY("API Key"),
    PASSWORD("Password"),
    TOKEN("Token"),
    SECRET("Secret"),
    PRIVATE_KEY("Private Key"),
    PUBLIC_KEY("Public Key"),
    HASH("Hash"),
    CERTIFICATE("Certificate"),
    DATABASE_URL("Database URL"),
    HIGH_ENTROPY("High Entropy String"),
    UNKNOWN("Unknown Secret")
}
