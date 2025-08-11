package com.scan.core

import java.io.File

/**
 * Legacy scan result for test compatibility.
 * This represents an individual finding for backward compatibility with existing tests.
 */
data class LegacyScanResult(
    val file: File,
    val lineNumber: Int,
    val columnStart: Int,
    val columnEnd: Int,
    val content: String,
    val ruleId: String,
    val severity: Severity,
    val message: String
) {
    enum class Severity {
        HIGH,
        MEDIUM,
        LOW
    }
}
