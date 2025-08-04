package com.scan.core

/**
 * Confidence levels for detection accuracy
 */
enum class Confidence {
    LOW,
    MEDIUM,
    HIGH;

    companion object {
        fun fromDouble(value: Double): Confidence {
            return when {
                value >= 0.8 -> HIGH
                value >= 0.5 -> MEDIUM
                else -> LOW
            }
        }
    }
}
