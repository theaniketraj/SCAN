package com.scan.detectors

import com.scan.core.*
import java.io.File
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min

/**
 * Entropy-based detector that identifies potential secrets by analyzing the randomness of strings.
 * High entropy strings are often indicators of cryptographic keys, tokens, or other sensitive data.
 */
class EntropyDetector : AbstractDetector() {

    override val detectorId: String = "entropy"
    override val detectorName: String = "Entropy Detector"
    override val version: String = "1.0.0"
    override val supportedFileTypes: Set<String> = setOf("*")

    companion object {
        private const val DEFAULT_MIN_LENGTH = 8
        private const val DEFAULT_MAX_LENGTH = 512
        private const val DEFAULT_ENTROPY_THRESHOLD = 3.5
        private const val DEFAULT_BASE64_THRESHOLD = 4.5
        private const val DEFAULT_HEX_THRESHOLD = 3.0
        private const val DEFAULT_CONFIDENCE_MULTIPLIER = 1.0

        // Character sets for different encoding types
        private val BASE64_CHARS =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".toSet()
        private val HEX_CHARS = "0123456789ABCDEFabcdef".toSet()
        private val ALPHANUMERIC_CHARS =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toSet()
    }

    private var minLength: Int = DEFAULT_MIN_LENGTH
    private var maxLength: Int = DEFAULT_MAX_LENGTH
    private var entropyThreshold: Double = DEFAULT_ENTROPY_THRESHOLD
    private var base64Threshold: Double = DEFAULT_BASE64_THRESHOLD
    private var hexThreshold: Double = DEFAULT_HEX_THRESHOLD
    private var confidenceMultiplier: Double = DEFAULT_CONFIDENCE_MULTIPLIER
    private var enableCharsetSpecificThresholds: Boolean = true
    private var excludeCommonWords: Boolean = true
    private var excludePlaceholders: Boolean = true

    // Common words and patterns to exclude (reduce false positives)
    private val commonWords =
            setOf(
                    "password",
                    "username",
                    "email",
                    "name",
                    "title",
                    "description",
                    "example",
                    "test",
                    "demo",
                    "sample",
                    "placeholder",
                    "default",
                    "admin",
                    "user",
                    "guest",
                    "anonymous",
                    "public",
                    "private"
            )

    // Common placeholder patterns
    private val placeholderPatterns =
            listOf(
                    Regex("^[x]{4,}$", RegexOption.IGNORE_CASE),
                    Regex("^[0]{4,}$"),
                    Regex("^[1]{4,}$"),
                    Regex("^(abc|xyz|test|demo){2,}$", RegexOption.IGNORE_CASE),
                    Regex("^[a-z]*(123|456|789)+[a-z]*$", RegexOption.IGNORE_CASE)
            )

    /** Data class representing an entropy analysis result */
    data class EntropyAnalysis(
            val text: String,
            val entropy: Double,
            val length: Int,
            val charsetType: CharsetType,
            val normalizedEntropy: Double,
            val confidence: Double,
            val isLikelySecret: Boolean
    )

    /** Enum representing different character set types */
    enum class CharsetType(val description: String, val maxEntropy: Double) {
        BASE64("Base64 encoded", log2(64.0)),
        HEX("Hexadecimal", log2(16.0)),
        ALPHANUMERIC("Alphanumeric", log2(62.0)),
        EXTENDED_ASCII("Extended ASCII", log2(256.0)),
        UNKNOWN("Unknown character set", log2(256.0))
    }

    /** Data class for entropy match results */
    data class EntropyMatch(
            val text: String,
            val startIndex: Int,
            val endIndex: Int,
            val lineNumber: Int,
            val columnNumber: Int,
            val analysis: EntropyAnalysis,
            val context: String
    )

    override fun performDetection(context: ScanContext): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = context.lines

        // Extract potential strings from content using different strategies
        val candidates = extractStringCandidates(context.content)

        candidates.forEach { candidate ->
            val analysis = analyzeEntropy(candidate.text)

            if (analysis.isLikelySecret && passesFilters(candidate.text, analysis)) {
                val (lineNumber, columnNumber) = getLineAndColumn(context.content, candidate.startIndex)
                val contextLines = getContext(lines, lineNumber - 1, 2)

                val match =
                        EntropyMatch(
                                text = candidate.text,
                                startIndex = candidate.startIndex,
                                endIndex = candidate.endIndex,
                                lineNumber = lineNumber,
                                columnNumber = columnNumber,
                                analysis = analysis,
                                context = contextLines
                        )

                findings.add(createFinding(context, match))
            }
        }

        return findings.sortedWith(
                compareByDescending<Finding> { it.confidence }
                        .thenBy { it.location.lineNumber }
                        .thenBy { it.location.columnStart }
        )
    }

    // Configuration methods removed - handled by base class

    /** Analyze the entropy of a given string */
    fun analyzeEntropy(text: String): EntropyAnalysis {
        if (text.length < minLength) {
            return EntropyAnalysis(
                    text = text,
                    entropy = 0.0,
                    length = text.length,
                    charsetType = CharsetType.UNKNOWN,
                    normalizedEntropy = 0.0,
                    confidence = 0.0,
                    isLikelySecret = false
            )
        }

        // Calculate Shannon entropy
        val entropy = calculateShannonEntropy(text)

        // Determine character set type
        val charsetType = determineCharsetType(text)

        // Calculate normalized entropy (0-1 scale)
        val normalizedEntropy = entropy / charsetType.maxEntropy

        // Determine if this meets our threshold criteria
        val threshold = getThresholdForCharset(charsetType)
        val isLikelySecret = entropy >= threshold

        // Calculate confidence score
        val confidence = calculateConfidence(text, entropy, charsetType, normalizedEntropy)

        return EntropyAnalysis(
                text = text,
                entropy = entropy,
                length = text.length,
                charsetType = charsetType,
                normalizedEntropy = normalizedEntropy,
                confidence = confidence,
                isLikelySecret = isLikelySecret
        )
    }

    /** Calculate Shannon entropy for a string */
    private fun calculateShannonEntropy(text: String): Double {
        if (text.isEmpty()) return 0.0

        val charFrequency = mutableMapOf<Char, Int>()
        text.forEach { char -> charFrequency[char] = charFrequency.getOrDefault(char, 0) + 1 }

        val length = text.length.toDouble()
        return charFrequency.values.sumOf { count ->
            val probability = count / length
            -probability * log2(probability)
        }
    }

    /** Determine the character set type of a string */
    private fun determineCharsetType(text: String): CharsetType {
        val chars = text.toSet()

        return when {
            chars.all { it in BASE64_CHARS } && text.contains(Regex("[A-Za-z0-9+/]")) ->
                    CharsetType.BASE64
            chars.all { it in HEX_CHARS } && text.length >= 8 -> CharsetType.HEX
            chars.all { it in ALPHANUMERIC_CHARS } -> CharsetType.ALPHANUMERIC
            chars.all { it.code <= 255 } -> CharsetType.EXTENDED_ASCII
            else -> CharsetType.UNKNOWN
        }
    }

    /** Get the appropriate entropy threshold for a character set type */
    private fun getThresholdForCharset(charsetType: CharsetType): Double {
        return if (enableCharsetSpecificThresholds) {
            when (charsetType) {
                CharsetType.BASE64 -> base64Threshold
                CharsetType.HEX -> hexThreshold
                else -> entropyThreshold
            }
        } else {
            entropyThreshold
        }
    }

    /** Calculate confidence score for a potential secret */
    private fun calculateConfidence(
            text: String,
            entropy: Double,
            charsetType: CharsetType,
            normalizedEntropy: Double
    ): Double {
        var confidence = normalizedEntropy * confidenceMultiplier

        // Length-based adjustments
        confidence *=
                when {
                    text.length >= 32 -> 1.2 // Longer strings are more likely to be secrets
                    text.length >= 16 -> 1.1
                    text.length < 8 -> 0.7 // Very short strings are less reliable
                    else -> 1.0
                }

        // Character set specific adjustments
        confidence *=
                when (charsetType) {
                    CharsetType.BASE64 -> 1.3 // Base64 is common for encoded secrets
                    CharsetType.HEX -> 1.1 // Hex is common for keys
                    CharsetType.ALPHANUMERIC -> 1.0
                    else -> 0.9
                }

        // Pattern-based adjustments
        if (hasRepeatingPatterns(text)) {
            confidence *= 0.6 // Repeating patterns are less likely to be real secrets
        }

        if (isSequential(text)) {
            confidence *= 0.4 // Sequential characters are likely test data
        }

        // Context-based adjustments would be handled by caller

        return min(1.0, max(0.0, confidence))
    }

    /** Extract potential string candidates from content */
    private fun extractStringCandidates(content: String): List<StringCandidate> {
        val candidates = mutableListOf<StringCandidate>()

        // Strategy 1: Extract quoted strings
        candidates.addAll(extractQuotedStrings(content))

        // Strategy 2: Extract assignment values
        candidates.addAll(extractAssignmentValues(content))

        // Strategy 3: Extract URL parameters and headers
        candidates.addAll(extractUrlParameters(content))

        // Strategy 4: Extract JSON values
        candidates.addAll(extractJsonValues(content))

        // Strategy 5: Extract continuous alphanumeric sequences
        candidates.addAll(extractAlphanumericSequences(content))

        return candidates.distinctBy { it.text }
    }

    /** Extract strings from quoted contexts */
    private fun extractQuotedStrings(content: String): List<StringCandidate> {
        val candidates = mutableListOf<StringCandidate>()
        val quotedStringRegex = Regex("""["']([^"']{$minLength,$maxLength})["']""")

        quotedStringRegex.findAll(content).forEach { match ->
            val quotedText = match.groupValues[1]
            if (quotedText.length in minLength..maxLength) {
                candidates.add(
                        StringCandidate(
                                text = quotedText,
                                startIndex = match.range.first + 1, // Skip opening quote
                                endIndex = match.range.last, // Skip closing quote
                                context = "quoted_string"
                        )
                )
            }
        }

        return candidates
    }

    /** Extract values from assignment statements */
    private fun extractAssignmentValues(content: String): List<StringCandidate> {
        val candidates = mutableListOf<StringCandidate>()
        val assignmentRegex =
                Regex(
                        """(?i)(api[_-]?key|token|secret|password|auth)\s*[=:]\s*["']?([a-zA-Z0-9+/=_-]{$minLength,$maxLength})["']?"""
                )

        assignmentRegex.findAll(content).forEach { match ->
            val value = match.groupValues[2]
            if (value.length in minLength..maxLength) {
                candidates.add(
                        StringCandidate(
                                text = value,
                                startIndex =
                                        match.range.first + match.groupValues[0].indexOf(value),
                                endIndex =
                                        match.range.first +
                                                match.groupValues[0].indexOf(value) +
                                                value.length - 1,
                                context = "assignment_value"
                        )
                )
            }
        }

        return candidates
    }

    /** Extract URL parameters and headers */
    private fun extractUrlParameters(content: String): List<StringCandidate> {
        val candidates = mutableListOf<StringCandidate>()
        val urlParamRegex =
                Regex("""[?&](token|key|auth|secret)=([a-zA-Z0-9+/=_-]{$minLength,$maxLength})""")

        urlParamRegex.findAll(content).forEach { match ->
            val value = match.groupValues[2]
            if (value.length in minLength..maxLength) {
                candidates.add(
                        StringCandidate(
                                text = value,
                                startIndex =
                                        match.range.first + match.groupValues[0].indexOf(value),
                                endIndex =
                                        match.range.first +
                                                match.groupValues[0].indexOf(value) +
                                                value.length - 1,
                                context = "url_parameter"
                        )
                )
            }
        }

        return candidates
    }

    /** Extract JSON values */
    private fun extractJsonValues(content: String): List<StringCandidate> {
        val candidates = mutableListOf<StringCandidate>()
        val jsonValueRegex =
                Regex(
                        """"(token|key|auth|secret|password)"\s*:\s*"([^"]{$minLength,$maxLength})""""
                )

        jsonValueRegex.findAll(content).forEach { match ->
            val value = match.groupValues[2]
            if (value.length in minLength..maxLength) {
                candidates.add(
                        StringCandidate(
                                text = value,
                                startIndex =
                                        match.range.first + match.groupValues[0].indexOf(value),
                                endIndex =
                                        match.range.first +
                                                match.groupValues[0].indexOf(value) +
                                                value.length - 1,
                                context = "json_value"
                        )
                )
            }
        }

        return candidates
    }

    /** Extract continuous alphanumeric sequences */
    private fun extractAlphanumericSequences(content: String): List<StringCandidate> {
        val candidates = mutableListOf<StringCandidate>()
        val alphanumericRegex = Regex("""[a-zA-Z0-9+/=_-]{$minLength,$maxLength}""")

        alphanumericRegex.findAll(content).forEach { match ->
            val text = match.value
            if (text.length in minLength..maxLength && hasGoodEntropyIndicators(text)) {
                candidates.add(
                        StringCandidate(
                                text = text,
                                startIndex = match.range.first,
                                endIndex = match.range.last,
                                context = "alphanumeric_sequence"
                        )
                )
            }
        }

        return candidates
    }

    /** Check if a string has indicators of good entropy without full calculation */
    private fun hasGoodEntropyIndicators(text: String): Boolean {
        val uniqueChars = text.toSet().size
        val lengthRatio = uniqueChars.toDouble() / text.length

        // Quick heuristic: good entropy strings have diverse characters
        return lengthRatio > 0.3 && uniqueChars >= minOf(8, text.length / 2)
    }

    /** Apply filters to reduce false positives */
    private fun passesFilters(text: String, analysis: EntropyAnalysis): Boolean {
        // Filter out common words
        if (excludeCommonWords && commonWords.contains(text.lowercase())) {
            return false
        }

        // Filter out placeholder patterns
        if (excludePlaceholders && placeholderPatterns.any { it.matches(text) }) {
            return false
        }

        // Filter out very low confidence matches
        if (analysis.confidence < 0.3) {
            return false
        }

        return true
    }

    /** Check if string has repeating patterns */
    private fun hasRepeatingPatterns(text: String): Boolean {
        if (text.length < 4) return false

        for (patternLength in 2..text.length / 2) {
            val pattern = text.substring(0, patternLength)
            var pos = patternLength
            var repeats = 1

            while (pos + patternLength <= text.length) {
                if (text.substring(pos, pos + patternLength) == pattern) {
                    repeats++
                    pos += patternLength
                } else {
                    break
                }
            }

            if (repeats >= 3) return true
        }

        return false
    }

    /** Check if string is sequential (like "abcdef" or "123456") */
    private fun isSequential(text: String): Boolean {
        if (text.length < 4) return false

        var ascending = 0
        var descending = 0

        for (i in 1 until text.length) {
            val diff = text[i].code - text[i - 1].code
            when (diff) {
                1 -> ascending++
                -1 -> descending++
            }
        }

        val threshold = text.length * 0.7
        return ascending >= threshold || descending >= threshold
    }

    /** Get line and column number for a character index */
    private fun getLineAndColumn(content: String, index: Int): Pair<Int, Int> {
        var lineNumber = 1
        var columnNumber = 1

        for (i in 0 until min(index, content.length)) {
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
        val start = max(0, lineIndex - contextSize)
        val end = min(lines.size, lineIndex + contextSize + 1)

        return lines.subList(start, end).joinToString("\n")
    }

    /** Create a Finding using the base class helper method */
    private fun createFinding(context: ScanContext, match: EntropyMatch): Finding {
        val analysis = match.analysis

        return createFinding(
            type = "High Entropy String",
            value = match.text,
            lineNumber = match.lineNumber,
            columnStart = match.columnNumber,
            columnEnd = match.columnNumber + match.text.length,
            confidence = analysis.confidence,
            rule = "entropy_${analysis.charsetType.name.lowercase()}",
            context = context
        )
    }

    /** Data class for string candidates */
    private data class StringCandidate(
            val text: String,
            val startIndex: Int,
            val endIndex: Int,
            val context: String
    )
}
