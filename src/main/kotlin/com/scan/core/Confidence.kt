package com.scan.core

/**
 * Confidence levels for findings
 */
enum class Confidence(val displayName: String, val description: String, val value: Double) {
    HIGH("High", "Very likely to be a real secret", 0.8),
    MEDIUM("Medium", "Probably a secret, but may be false positive", 0.5),
    LOW("Low", "Possibly a secret, likely needs manual review", 0.2);

    companion object {
        fun fromString(value: String): Confidence? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }

        fun fromValue(value: Double): Confidence {
            return when {
                value >= 0.7 -> HIGH
                value >= 0.4 -> MEDIUM
                else -> LOW
            }
        }
    }
}
