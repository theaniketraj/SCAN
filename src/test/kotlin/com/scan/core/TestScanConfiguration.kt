package com.scan.core

import com.scan.detectors.DetectorInterface
import com.scan.filters.FilterInterface

/**
 * Test-specific scan configuration for backward compatibility with existing tests
 */
data class TestScanConfiguration(
    val detectors: List<DetectorInterface> = emptyList(),
    val filters: List<FilterInterface> = emptyList(),
    val maxFileSize: Long = 1024 * 1024,
    val includeHidden: Boolean = false,
    val followSymlinks: Boolean = false,
    val parallelProcessing: Boolean = false,
    val maxThreads: Int = 1
) {
    
    /**
     * Convert to the actual ScanConfiguration for use with the real scanner
     */
    fun toScanConfiguration(): ScanConfiguration {
        return ScanConfiguration(
            maxFileSize = this.maxFileSize,
            followSymlinks = this.followSymlinks,
            performance = ScanConfiguration().performance.copy(
                maxConcurrency = this.maxThreads
            )
        )
    }
}
