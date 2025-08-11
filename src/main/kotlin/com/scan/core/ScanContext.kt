package com.scan.core

import java.nio.file.Path

/**
 * Context object passed to detectors containing file information and content.
 * This provides all the necessary information for detectors to analyze files.
 */
data class ScanContext(
    val filePath: Path,
    val fileName: String,
    val fileExtension: String,
    val isTestFile: Boolean,
    val fileSize: Long,
    val configuration: ScanConfiguration,
    var content: String = "",
    var lines: List<String> = emptyList(),
    val metadata: MutableMap<String, Any> = mutableMapOf(),
    val relativePath: String? = null
) {
    /** Get the normalized file extension (lowercase, without dot) */
    fun getNormalizedExtension(): String = fileExtension.lowercase().removePrefix(".")

    /** Check if this is a binary file based on content */
    fun isBinaryFile(): Boolean {
        if (content.isEmpty()) return false
        val nonPrintableCount = content.count { it.code < 32 && it !in "\t\n\r" }
        return nonPrintableCount.toDouble() / content.length > 0.3
    }

    /** Get line at specific index (1-based) */
    fun getLine(lineNumber: Int): String? {
        return lines.getOrNull(lineNumber - 1)
    }

    /** Get context around a specific line */
    fun getContextLines(lineNumber: Int, contextSize: Int = 2): List<String> {
        val startLine = maxOf(0, lineNumber - 1 - contextSize)
        val endLine = minOf(lines.size, lineNumber + contextSize)
        return lines.subList(startLine, endLine)
    }
}
