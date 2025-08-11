package com.scan.utils

import kotlin.math.log2

/**
 * Utility class for calculating Shannon entropy of strings to detect high-entropy sequences that
 * may indicate secrets, API keys, or cryptographic material.
 *
 * Shannon entropy measures the randomness/information content of a string. Higher entropy values
 * typically indicate more random data, which is characteristic of secrets, tokens, hashes, and
 * encrypted data.
 *
 * @author SCAN Plugin Team
 * @since 1.0.0
 */
object EntropyCalculator {

    /**
     * Default minimum length for strings to be considered for entropy analysis. Shorter strings are
     * typically not secrets and can produce misleading entropy values.
     */
    const val DEFAULT_MIN_LENGTH = 8

    /**
     * Default maximum length for strings to be analyzed. Very long strings can be expensive to
     * analyze and are often not secrets.
     */
    const val DEFAULT_MAX_LENGTH = 512

    /**
     * Entropy threshold for base64-encoded content (typically 4.5-6.0). Base64 encoded secrets
     * usually have entropy around 6.0.
     */
    const val BASE64_ENTROPY_THRESHOLD = 4.5

    /**
     * Entropy threshold for hexadecimal content (typically 3.5-4.0). Hex-encoded secrets usually
     * have entropy around 4.0.
     */
    const val HEX_ENTROPY_THRESHOLD = 3.5

    /**
     * Entropy threshold for general high-entropy content. Most natural language text has entropy <
     * 4.0, while secrets are typically > 4.5.
     */
    const val GENERAL_ENTROPY_THRESHOLD = 4.5

    /** Common character sets used for entropy calculation optimization. */
    private val BASE64_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
    private val HEX_CHARSET = "0123456789ABCDEFabcdef"
    private val ALPHANUMERIC_CHARSET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    /**
     * Calculates the Shannon entropy of a given string.
     *
     * Shannon entropy formula: H(X) = -Σ P(xi) * log2(P(xi)) where P(xi) is the probability of
     * character xi occurring in the string.
     *
     * @param input The string to analyze
     * @return The Shannon entropy value (0.0 to theoretical maximum based on character set)
     * @throws IllegalArgumentException if input is empty
     */
    fun calculateEntropy(input: String): Double {
        require(input.isNotEmpty()) { "Input string cannot be empty" }

        // Count frequency of each character
        val charCounts = mutableMapOf<Char, Int>()
        input.forEach { char -> charCounts[char] = charCounts.getOrDefault(char, 0) + 1 }

        // Calculate entropy using Shannon's formula
        val length = input.length.toDouble()
        var entropy = 0.0

        charCounts.values.forEach { count ->
            val probability = count / length
            entropy -= probability * log2(probability)
        }

        return entropy
    }

    /**
     * Calculates normalized entropy (0.0 to 1.0) based on the theoretical maximum entropy for the
     * character set used in the string.
     *
     * @param input The string to analyze
     * @return Normalized entropy value between 0.0 and 1.0
     */
    fun calculateNormalizedEntropy(input: String): Double {
        require(input.isNotEmpty()) { "Input string cannot be empty" }

        val entropy = calculateEntropy(input)
        val uniqueChars = input.toSet().size
        val maxEntropy = log2(uniqueChars.toDouble())

        return if (maxEntropy > 0) entropy / maxEntropy else 0.0
    }

    /**
     * Determines if a string has high entropy that might indicate it's a secret. Uses different
     * thresholds based on the detected character set.
     *
     * @param input The string to analyze
     * @param customThreshold Optional custom threshold (overrides automatic detection)
     * @return true if the string has high entropy, false otherwise
     */
    fun isHighEntropy(input: String, customThreshold: Double? = null): Boolean {
        if (input.length < DEFAULT_MIN_LENGTH || input.length > DEFAULT_MAX_LENGTH) {
            return false
        }

        val entropy = calculateEntropy(input)
        val threshold = customThreshold ?: determineThreshold(input)

        return entropy >= threshold
    }

    /**
     * Analyzes a string and returns detailed entropy information.
     *
     * @param input The string to analyze
     * @return EntropyAnalysis containing detailed entropy metrics
     */
    fun analyzeEntropy(input: String): EntropyAnalysis {
        if (input.isEmpty()) {
            return EntropyAnalysis(
                entropy = 0.0,
                normalizedEntropy = 0.0,
                charsetType = CharsetType.UNKNOWN,
                isHighEntropy = false,
                threshold = 0.0,
                characterCount = 0,
                uniqueCharacters = 0
            )
        }

        val entropy = calculateEntropy(input)
        val normalizedEntropy = calculateNormalizedEntropy(input)
        val charsetType = detectCharsetType(input)
        val threshold = determineThreshold(input)
        val isHighEntropy =
            entropy >= threshold &&
                input.length >= DEFAULT_MIN_LENGTH &&
                input.length <= DEFAULT_MAX_LENGTH

        return EntropyAnalysis(
            entropy = entropy,
            normalizedEntropy = normalizedEntropy,
            charsetType = charsetType,
            isHighEntropy = isHighEntropy,
            threshold = threshold,
            characterCount = input.length,
            uniqueCharacters = input.toSet().size
        )
    }

    /**
     * Calculates entropy for multiple substrings of different lengths within a string. Useful for
     * finding high-entropy sequences embedded in larger text.
     *
     * @param input The string to analyze
     * @param windowSizes List of substring lengths to analyze (default: 16, 24, 32, 40)
     * @param minEntropy Minimum entropy threshold for reporting (default:
     * GENERAL_ENTROPY_THRESHOLD)
     * @return List of high-entropy substrings found
     */
    fun findHighEntropySubstrings(
        input: String,
        windowSizes: List<Int> = listOf(16, 24, 32, 40),
        minEntropy: Double = GENERAL_ENTROPY_THRESHOLD
    ): List<EntropySubstring> {
        val results = mutableListOf<EntropySubstring>()

        windowSizes.forEach { windowSize ->
            if (input.length >= windowSize) {
                for (i in 0..input.length - windowSize) {
                    val substring = input.substring(i, i + windowSize)
                    val analysis = analyzeEntropy(substring)

                    if (analysis.entropy >= minEntropy) {
                        results.add(
                            EntropySubstring(
                                text = substring,
                                startIndex = i,
                                endIndex = i + windowSize - 1,
                                entropy = analysis.entropy,
                                normalizedEntropy = analysis.normalizedEntropy,
                                charsetType = analysis.charsetType
                            )
                        )
                    }
                }
            }
        }

        // Remove overlapping results, keeping the highest entropy ones
        return deduplicateOverlappingSubstrings(results)
    }

    /** Determines the appropriate entropy threshold based on the detected character set. */
    private fun determineThreshold(input: String): Double {
        return when (detectCharsetType(input)) {
            CharsetType.BASE64 -> BASE64_ENTROPY_THRESHOLD
            CharsetType.HEXADECIMAL -> HEX_ENTROPY_THRESHOLD
            CharsetType.ALPHANUMERIC -> GENERAL_ENTROPY_THRESHOLD
            CharsetType.MIXED -> GENERAL_ENTROPY_THRESHOLD
            CharsetType.UNKNOWN -> GENERAL_ENTROPY_THRESHOLD
        }
    }

    /** Detects the character set type of a string to apply appropriate thresholds. */
    private fun detectCharsetType(input: String): CharsetType {
        val chars = input.toSet()

        return when {
            chars.all { it in BASE64_CHARSET } &&
                chars.intersect(BASE64_CHARSET.toSet()).size >= 10 -> CharsetType.BASE64
            chars.all { it in HEX_CHARSET } &&
                chars.intersect("0123456789".toSet()).isNotEmpty() &&
                chars.intersect("ABCDEFabcdef".toSet()).isNotEmpty() -> CharsetType.HEXADECIMAL
            chars.all { it in ALPHANUMERIC_CHARSET } -> CharsetType.ALPHANUMERIC
            chars.any { !it.isLetterOrDigit() && it !in "+/=" } -> CharsetType.MIXED
            else -> CharsetType.UNKNOWN
        }
    }

    /** Removes overlapping substrings, keeping the ones with highest entropy. */
    private fun deduplicateOverlappingSubstrings(
        substrings: List<EntropySubstring>
    ): List<EntropySubstring> {
        if (substrings.isEmpty()) return emptyList()

        val sorted = substrings.sortedByDescending { it.entropy }
        val result = mutableListOf<EntropySubstring>()

        sorted.forEach { candidate ->
            val hasOverlap =
                result.any { existing ->
                    candidate.startIndex < existing.endIndex &&
                        existing.startIndex < candidate.endIndex
                }

            if (!hasOverlap) {
                result.add(candidate)
            }
        }

        return result.sortedBy { it.startIndex }
    }
}

/** Represents the character set type detected in a string. */
enum class CharsetType {
    BASE64, // Base64 encoded content
    HEXADECIMAL, // Hexadecimal encoded content
    ALPHANUMERIC, // Letters and numbers only
    MIXED, // Mixed character set including symbols
    UNKNOWN // Unable to determine or empty
}

/**
 * Detailed entropy analysis results for a string.
 *
 * @property entropy Shannon entropy value
 * @property normalizedEntropy Normalized entropy (0.0 to 1.0)
 * @property charsetType Detected character set type
 * @property isHighEntropy Whether this qualifies as high entropy
 * @property threshold The threshold used for high entropy determination
 * @property characterCount Total number of characters
 * @property uniqueCharacters Number of unique characters
 */
data class EntropyAnalysis(
    val entropy: Double,
    val normalizedEntropy: Double,
    val charsetType: CharsetType,
    val isHighEntropy: Boolean,
    val threshold: Double,
    val characterCount: Int,
    val uniqueCharacters: Int
) {
    /** Returns a human-readable description of the entropy analysis. */
    fun getDescription(): String {
        val entropyLevel =
            when {
                entropy < 2.0 -> "Very Low"
                entropy < 3.0 -> "Low"
                entropy < 4.0 -> "Medium"
                entropy < 5.0 -> "High"
                else -> "Very High"
            }

        return "Entropy: ${"%.2f".format(entropy)} ($entropyLevel), " +
            "Normalized: ${"%.2f".format(normalizedEntropy)}, " +
            "Charset: ${charsetType.name.lowercase()}, " +
            "High Entropy: $isHighEntropy"
    }
}

/**
 * Represents a high-entropy substring found within a larger string.
 *
 * @property text The substring text
 * @property startIndex Starting position in the original string
 * @property endIndex Ending position in the original string
 * @property entropy Shannon entropy of the substring
 * @property normalizedEntropy Normalized entropy of the substring
 * @property charsetType Detected character set type
 */
data class EntropySubstring(
    val text: String,
    val startIndex: Int,
    val endIndex: Int,
    val entropy: Double,
    val normalizedEntropy: Double,
    val charsetType: CharsetType
) {
    /** Returns the length of the substring. */
    val length: Int
        get() = text.length

    /** Returns a human-readable description of the high-entropy substring. */
    fun getDescription(): String {
        return "High-entropy substring at positions $startIndex-$endIndex: " +
            "entropy=${"%.2f".format(entropy)}, " +
            "charset=${charsetType.name.lowercase()}, " +
            "length=$length"
    }
}
