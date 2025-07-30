package com.scan.core

/**
 * Result of scanning a single file.
 * This represents the outcome of scanning one file for secrets/sensitive data.
 */
data class FileScanResult(
    val filePath: String,
    val detections: List<Finding> = emptyList(),
    val scanTimeMs: Long = 0L,
    val fileSize: Long = 0L,
    val isError: Boolean = false,
    val isSkipped: Boolean = false,
    val errorMessage: String? = null,
    val skippedReason: String? = null
) {
    /** Check if this result has any findings */
    fun hasFindings(): Boolean = detections.isNotEmpty()

    /** Check if scan was successful (no errors) */
    fun isSuccessful(): Boolean = !isError

    /** Get the file name without path */
    fun getFileName(): String = java.io.File(filePath).name

    companion object {
        /** Create an error result for a file that couldn't be scanned */
        fun error(filePath: String, errorMessage: String): FileScanResult {
            return FileScanResult(
                filePath = filePath,
                isError = true,
                errorMessage = errorMessage
            )
        }

        /** Create an empty result for a file with no findings */
        fun empty(filePath: String): FileScanResult {
            return FileScanResult(
                filePath = filePath,
                detections = emptyList()
            )
        }

        /** Create a skipped result for a file that was not scanned */
        fun skipped(filePath: String, reason: String): FileScanResult {
            return FileScanResult(
                filePath = filePath,
                isSkipped = true,
                skippedReason = reason
            )
        }

        /** Create a successful result with findings */
        fun success(filePath: String, detections: List<Finding>, scanTimeMs: Long, fileSize: Long): FileScanResult {
            return FileScanResult(
                filePath = filePath,
                detections = detections,
                scanTimeMs = scanTimeMs,
                fileSize = fileSize
            )
        }
    }
}
