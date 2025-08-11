package com.scan.filters

import java.io.File

/**
 * Common interface for all filtering implementations in the SCAN plugin.
 *
 * Filters determine whether files or content should be included in or excluded from the scanning
 * process. This provides a flexible way to customize what gets scanned based on various criteria
 * like file paths, extensions, content patterns, etc.
 *
 * Implementations should be stateless and thread-safe to support concurrent scanning.
 */
interface FilterInterface {

    /**
     * Determines whether a file should be included in the scanning process.
     *
     * @param file The file to evaluate for inclusion/exclusion
     * @param relativePath The path of the file relative to the project root
     * @return true if the file should be scanned, false if it should be excluded
     *
     * @throws FilterException if an error occurs during filtering evaluation
     */
    fun shouldIncludeFile(file: File, relativePath: String): Boolean

    /**
     * Determines whether a specific line of content should be included in scanning. This allows for
     * fine-grained filtering at the content level, useful for excluding comments, test data, or
     * other non-critical content.
     *
     * @param line The line of content to evaluate
     * @param lineNumber The line number within the file (1-based)
     * @param file The file containing this line
     * @return true if the line should be scanned, false if it should be excluded
     *
     * @throws FilterException if an error occurs during filtering evaluation
     */
    fun shouldIncludeLine(line: String, lineNumber: Int, file: File): Boolean

    /**
     * Returns a human-readable description of what this filter does. Used for logging, debugging,
     * and configuration validation.
     *
     * @return A descriptive string explaining the filter's purpose and behavior
     */
    fun getDescription(): String

    /**
     * Returns the priority of this filter. Higher priority filters are evaluated first. This is
     * important when multiple filters are chained together.
     *
     * Default priority is 0. Negative values indicate lower priority, positive values indicate
     * higher priority.
     *
     * @return The priority value for this filter
     */
    fun getPriority(): Int = 0

    /**
     * Determines if this filter is applicable to the given file type or context. This allows
     * filters to opt-out of evaluation for irrelevant files, improving performance.
     *
     * @param file The file to check applicability for
     * @param fileExtension The file extension (without the dot, e.g., "kt", "java")
     * @return true if this filter should be applied to the given file, false otherwise
     */
    fun isApplicable(file: File, fileExtension: String): Boolean = true

    /**
     * Validates the filter configuration and reports any issues. Called during plugin
     * initialization to catch configuration errors early.
     *
     * @return A list of validation error messages, empty if configuration is valid
     */
    fun validateConfiguration(): List<String> = emptyList()

    /**
     * Returns metadata about this filter for reporting and debugging purposes.
     *
     * @return A map containing filter metadata (name, version, configuration summary, etc.)
     */
    fun getMetadata(): Map<String, Any> =
        mapOf(
            "name" to this::class.simpleName.orEmpty(),
            "priority" to getPriority(),
            "description" to getDescription()
        )
}

/**
 * Exception thrown when an error occurs during filter evaluation.
 *
 * @param message A descriptive error message
 * @param cause The underlying cause of the error, if any
 * @param filterName The name of the filter that caused the error
 */
class FilterException(message: String, cause: Throwable? = null, val filterName: String? = null) :
    Exception(buildMessage(message, filterName), cause) {

    companion object {
        private fun buildMessage(message: String, filterName: String?): String {
            return if (filterName != null) {
                "Filter '$filterName': $message"
            } else {
                message
            }
        }
    }
}

/**
 * Result class for batch filtering operations. Contains information about what was filtered and
 * why.
 *
 * @param includedFiles List of files that passed all filters
 * @param excludedFiles Map of excluded files to the reason for exclusion
 * @param filteringStats Statistics about the filtering process
 */
data class FilterResult(
    val includedFiles: List<File>,
    val excludedFiles: Map<File, String>,
    val filteringStats: FilteringStats
)

/**
 * Statistics about the filtering process.
 *
 * @param totalFilesEvaluated Total number of files evaluated by filters
 * @param filesIncluded Number of files that passed all filters
 * @param filesExcluded Number of files that were excluded
 * @param filterExecutionTime Time spent executing filters (in milliseconds)
 * @param filterCounts Map of filter names to how many times they were applied
 */
data class FilteringStats(
    val totalFilesEvaluated: Int,
    val filesIncluded: Int,
    val filesExcluded: Int,
    val filterExecutionTime: Long,
    val filterCounts: Map<String, Int>
)

/**
 * Composite filter that combines multiple filters with logical operations. This is a convenience
 * interface for creating complex filtering logic.
 */
interface CompositeFilterInterface : FilterInterface {

    /** The list of filters that make up this composite filter. */
    val filters: List<FilterInterface>

    /** The logical operation to apply when combining filter results. */
    val operation: FilterOperation

    enum class FilterOperation {
        /** All filters must return true (AND operation) */
        ALL,

        /** At least one filter must return true (OR operation) */
        ANY,

        /** Exactly one filter must return true (XOR operation) */
        EXCLUSIVE,

        /** No filters should return true (NOT operation) */
        NONE
    }
}

/**
 * Abstract base class that provides common functionality for filter implementations. Implements
 * thread-safe caching and provides utility methods.
 */
abstract class BaseFilter : FilterInterface {

    /**
     * Cache for file applicability checks to improve performance. Thread-safe concurrent map for
     * multi-threaded scanning.
     */
    private val applicabilityCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    /** Clears the internal caches. Should be called when filter configuration changes. */
    protected fun clearCaches() {
        applicabilityCache.clear()
    }

    /** Cached version of isApplicable that stores results for better performance. */
    final override fun isApplicable(file: File, fileExtension: String): Boolean {
        val cacheKey = "${file.absolutePath}:$fileExtension"
        return applicabilityCache.computeIfAbsent(cacheKey) {
            computeApplicability(file, fileExtension)
        }
    }

    /**
     * Implement this method instead of isApplicable to provide applicability logic. Results will be
     * automatically cached.
     */
    protected open fun computeApplicability(file: File, fileExtension: String): Boolean = true

    /** Utility method to extract file extension from a file. */
    protected fun getFileExtension(file: File): String {
        val name = file.name
        val lastDot = name.lastIndexOf('.')
        return if (lastDot > 0 && lastDot < name.length - 1) {
            name.substring(lastDot + 1).lowercase()
        } else {
            ""
        }
    }

    /** Utility method to normalize file paths for consistent comparison. */
    protected fun normalizePath(path: String): String {
        return path.replace('\\', '/').removePrefix("./")
    }
}
