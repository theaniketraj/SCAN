package com.scan.core

/**
 * Types of scan errors
 */
enum class ErrorType(val displayName: String) {
    FILE_READ_ERROR("File Read Error"),
    PATTERN_COMPILATION_ERROR("Pattern Compilation Error"),
    CONFIGURATION_ERROR("Configuration Error"),
    PERMISSION_ERROR("Permission Error"),
    MEMORY_ERROR("Memory Error"),
    TIMEOUT_ERROR("Timeout Error"),
    UNKNOWN_ERROR("Unknown Error");

    companion object {
        fun fromString(value: String): ErrorType? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
    }
}
