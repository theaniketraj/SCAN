package com.scan.detectors

import com.scan.core.*
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Pattern-based detector that uses regular expressions to identify potential secrets in source code
 * files. This detector supports custom patterns, confidence scoring, and context-aware matching.
 */
class PatternDetector : AbstractDetector() {

    override val detectorId: String = "pattern"
    override val detectorName: String = "Pattern Detector"
    override val version: String = "1.0.0"
    override val supportedFileTypes: Set<String> = emptySet() // Supports all file types

    private val compiledPatterns = mutableMapOf<String, CompiledPattern>()
    private var caseSensitive: Boolean = true
    private var multilineMode: Boolean = false
    private var minimumConfidence: Double = 0.5

    /** Data class representing a compiled pattern with metadata */
    data class CompiledPattern(
        val name: String,
        val pattern: Pattern,
        val description: String,
        val category: String,
        val baseConfidence: Double,
        val requiresContext: Boolean = false,
        val contextPatterns: List<Pattern> = emptyList()
    )

    /** Data class for pattern match results */
    data class PatternMatch(
        val patternName: String,
        val matchedText: String,
        val startIndex: Int,
        val endIndex: Int,
        val lineNumber: Int,
        val columnNumber: Int,
        val confidence: Double,
        val context: String = ""
    )

    init {
        loadDefaultPatterns()
    }

    /** Configure the detector with custom settings */
    fun configure(
        caseSensitive: Boolean = true,
        multilineMode: Boolean = false,
        minimumConfidence: Double = 0.5
    ): PatternDetector {
        this.caseSensitive = caseSensitive
        this.multilineMode = multilineMode
        this.minimumConfidence = minimumConfidence
        return this
    }

    /** Add a custom pattern to the detector */
    fun addPattern(
        name: String,
        regex: String,
        description: String,
        category: String,
        baseConfidence: Double = 0.8,
        requiresContext: Boolean = false,
        contextPatterns: List<String> = emptyList()
    ): PatternDetector {
        try {
            val flags = buildPatternFlags()
            val compiledPattern = Pattern.compile(regex, flags)
            val compiledContextPatterns = contextPatterns.map { Pattern.compile(it, flags) }

            compiledPatterns[name] =
                CompiledPattern(
                    name = name,
                    pattern = compiledPattern,
                    description = description,
                    category = category,
                    baseConfidence = baseConfidence,
                    requiresContext = requiresContext,
                    contextPatterns = compiledContextPatterns
                )
        } catch (e: PatternSyntaxException) {
            throw IllegalArgumentException(
                "Invalid regex pattern '$regex' for '$name': ${e.message}",
                e
            )
        }

        return this
    }

    /** Add multiple patterns from a map */
    fun addPatterns(patterns: Map<String, PatternDefinition>): PatternDetector {
        patterns.forEach { (name, definition) ->
            addPattern(
                name = name,
                regex = definition.regex,
                description = definition.description,
                category = definition.category,
                baseConfidence = definition.baseConfidence,
                requiresContext = definition.requiresContext,
                contextPatterns = definition.contextPatterns
            )
        }
        return this
    }

    /** Remove a pattern by name */
    fun removePattern(name: String): PatternDetector {
        compiledPatterns.remove(name)
        return this
    }

    /** Clear all patterns */
    fun clearPatterns(): PatternDetector {
        compiledPatterns.clear()
        return this
    }

    /** Get all loaded pattern names */
    fun getLoadedPatterns(): Set<String> = compiledPatterns.keys.toSet()

    override fun performDetection(context: ScanContext): List<Finding> {
        if (compiledPatterns.isEmpty()) {
            return emptyList()
        }

        val findings = mutableListOf<Finding>()

        // Process each pattern
        compiledPatterns.values.forEach { compiledPattern ->
            val matches = findMatches(context.content, compiledPattern, context.lines)
            matches.forEach { match ->
                if (match.confidence >= minimumConfidence) {
                    findings.add(
                        createFinding(
                            type = compiledPattern.category,
                            value = match.matchedText,
                            lineNumber = match.lineNumber,
                            columnStart = match.columnNumber,
                            columnEnd = match.columnNumber + match.matchedText.length,
                            confidence = match.confidence,
                            rule = compiledPattern.name,
                            context = context
                        )
                    )
                }
            }
        }

        return findings.sortedWith(
            compareByDescending<Finding> { it.confidence }
                .thenBy { it.location.lineNumber }
                .thenBy { it.location.columnStart }
        )
    }

    // Configuration methods removed - handled by base class

    /** Find all matches for a given pattern in the content */
    private fun findMatches(
        content: String,
        compiledPattern: CompiledPattern,
        lines: List<String>
    ): List<PatternMatch> {
        val matches = mutableListOf<PatternMatch>()
        val matcher = compiledPattern.pattern.matcher(content)

        while (matcher.find()) {
            val matchedText = matcher.group()
            val startIndex = matcher.start()
            val endIndex = matcher.end()

            // Calculate line and column numbers
            val (lineNumber, columnNumber) = getLineAndColumn(content, startIndex)

            // Calculate confidence based on various factors
            val confidence =
                calculateConfidence(
                    compiledPattern,
                    matchedText,
                    content,
                    startIndex,
                    lines.getOrNull(lineNumber - 1) ?: ""
                )

            // Get context around the match
            val context = getContext(lines, lineNumber - 1, 2)

            matches.add(
                PatternMatch(
                    patternName = compiledPattern.name,
                    matchedText = matchedText,
                    startIndex = startIndex,
                    endIndex = endIndex,
                    lineNumber = lineNumber,
                    columnNumber = columnNumber,
                    confidence = confidence,
                    context = context
                )
            )
        }

        return matches
    }

    /** Calculate confidence score for a match */
    private fun calculateConfidence(
        compiledPattern: CompiledPattern,
        matchedText: String,
        fullContent: String,
        startIndex: Int,
        line: String
    ): Double {
        var confidence = compiledPattern.baseConfidence

        // Adjust confidence based on match characteristics
        confidence *=
            when {
                matchedText.length > 50 -> 1.2 // Longer matches are more likely to be real
                matchedText.length > 30 -> 1.1
                matchedText.length < 10 -> 0.8 // Very short matches are less reliable
                else -> 1.0
            }

        // Check for context patterns if required
        if (compiledPattern.requiresContext && compiledPattern.contextPatterns.isNotEmpty()) {
            val hasContext =
                compiledPattern.contextPatterns.any { contextPattern ->
                    contextPattern.matcher(line).find() ||
                        contextPattern
                            .matcher(getContextAround(fullContent, startIndex, 100))
                            .find()
                }
            if (!hasContext) {
                confidence *= 0.5 // Reduce confidence if required context is missing
            }
        }

        // Reduce confidence if the match appears to be in a comment
        if (isInComment(line)) {
            confidence *= 0.7
        }

        // Reduce confidence if the match appears to be a placeholder or example
        if (isPlaceholder(matchedText)) {
            confidence *= 0.3
        }

        // Boost confidence if the match is in a configuration context
        if (isInConfigurationContext(line)) {
            confidence *= 1.3
        }

        return minOf(1.0, maxOf(0.0, confidence))
    }

    /** Load default patterns from SecretPatterns */
    private fun loadDefaultPatterns() {
        // This would typically load from SecretPatterns class or configuration files
        // Adding some common patterns as examples

        addPattern(
            name = "aws_access_key",
            regex = "AKIA[0-9A-Z]{16}",
            description = "AWS Access Key ID",
            category = "AWS Credentials",
            baseConfidence = 0.9
        )

        addPattern(
            name = "generic_api_key",
            regex = "(?i)(api[_-]?key|apikey)\\s*[=:]\\s*['\"]?([a-zA-Z0-9_\\-]{16,})['\"]?",
            description = "Generic API Key",
            category = "API Keys",
            baseConfidence = 0.7,
            requiresContext = true,
            contextPatterns = listOf("(?i)(key|token|secret|password)")
        )

        addPattern(
            name = "password",
            regex = "(?i)password\\s*[=:]\\s*['\"]?([a-zA-Z0-9_\\-]{8,})['\"]?",
            description = "Password",
            category = "Passwords",
            baseConfidence = 0.6,
            requiresContext = true
        )

        addPattern(
            name = "private_key",
            regex = "-----BEGIN (RSA )?PRIVATE KEY-----",
            description = "Private Key",
            category = "Cryptographic Keys",
            baseConfidence = 0.95
        )

        addPattern(
            name = "jwt_token",
            regex = "eyJ[A-Za-z0-9_-]*\\.[A-Za-z0-9_-]*\\.[A-Za-z0-9_-]*",
            description = "JWT Token",
            category = "Authentication Tokens",
            baseConfidence = 0.8
        )

        addPattern(
            name = "stripe_live_key",
            regex = "sk_live_",
            description = "Stripe Live API Key",
            category = "API Keys",
            baseConfidence = 0.95
        )

        addPattern(
            name = "stripe_test_key",
            regex = "sk_test_[0-9a-zA-Z]{24,}",
            description = "Stripe Test API Key",
            category = "API Keys",
            baseConfidence = 0.9
        )

        addPattern(
            name = "stripe_quoted_live_key",
            regex = "\"sk_live_[0-9a-zA-Z]{24,}\"",
            description = "Stripe Live API Key (quoted)",
            category = "API Keys",
            baseConfidence = 0.95
        )

        addPattern(
            name = "database_url",
            regex = "(?i)(jdbc:|mongodb://|mysql://|postgresql://)[^\\s'\"]+",
            description = "Database Connection URL",
            category = "Database Credentials",
            baseConfidence = 0.8
        )
    }

    /** Build pattern flags based on configuration */
    private fun buildPatternFlags(): Int {
        var flags = 0
        if (!caseSensitive) flags = flags or Pattern.CASE_INSENSITIVE
        if (multilineMode) flags = flags or Pattern.MULTILINE
        return flags
    }

    /** Get line and column number for a character index */
    private fun getLineAndColumn(content: String, index: Int): Pair<Int, Int> {
        var lineNumber = 1
        var columnNumber = 1

        for (i in 0 until minOf(index, content.length)) {
            if (content[i] == '\n') {
                lineNumber++
                columnNumber = 1
            } else {
                columnNumber++
            }
        }

        return Pair(lineNumber, columnNumber)
    }

    /** Get context lines around a specific line */
    private fun getContext(lines: List<String>, lineIndex: Int, contextSize: Int): String {
        val start = maxOf(0, lineIndex - contextSize)
        val end = minOf(lines.size, lineIndex + contextSize + 1)

        return lines.subList(start, end).joinToString("\n")
    }

    /** Get context around a character position */
    private fun getContextAround(content: String, index: Int, contextSize: Int): String {
        val start = maxOf(0, index - contextSize)
        val end = minOf(content.length, index + contextSize)
        return content.substring(start, end)
    }

    /** Check if a match appears to be in a comment */
    private fun isInComment(line: String): Boolean {
        val trimmedLine = line.trim()
        return trimmedLine.startsWith("//") ||
            trimmedLine.startsWith("#") ||
            trimmedLine.startsWith("/*") ||
            line.contains("<!--")
    }

    /** Check if a match appears to be a placeholder or example value */
    private fun isPlaceholder(matchedText: String): Boolean {
        val lowerMatch = matchedText.lowercase()
        val placeholderIndicators =
            listOf(
                "example",
                "placeholder",
                "dummy",
                "test",
                "fake",
                "sample",
                "your_",
                "my_",
                "xxx",
                "000",
                "123",
                "abc"
            )

        return placeholderIndicators.any { indicator -> lowerMatch.contains(indicator) }
    }

    /** Check if a match is in a configuration context (more likely to be real) */
    private fun isInConfigurationContext(line: String): Boolean {
        val configIndicators = listOf("=", ":", "config", "properties", "env")
        return configIndicators.any { indicator -> line.contains(indicator, ignoreCase = true) }
    }

    /** Data class for pattern definitions */
    data class PatternDefinition(
        val regex: String,
        val description: String,
        val category: String,
        val baseConfidence: Double = 0.8,
        val requiresContext: Boolean = false,
        val contextPatterns: List<String> = emptyList()
    )
}
