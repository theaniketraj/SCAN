package com.scan.filters

import java.io.File
import java.util.Locale

/**
 * Filter implementation that includes or excludes files based on their file extensions.
 *
 * This filter supports both inclusion and exclusion patterns, with inclusion taking precedence over
 * exclusion. It handles case-insensitive matching and provides predefined sets of common file types
 * for convenience.
 *
 * Examples:
 * - Include only source code files: FileExtensionFilter(include = SOURCE_CODE_EXTENSIONS)
 * - Exclude binary files: FileExtensionFilter(exclude = BINARY_EXTENSIONS)
 * - Custom filtering: FileExtensionFilter(include = setOf("kt", "java"), exclude = setOf("class"))
 */
class FileExtensionFilter(
        private val includeExtensions: Set<String> = emptySet(),
        private val excludeExtensions: Set<String> = emptySet(),
        private val caseSensitive: Boolean = false,
        private val priority: Int = 10
) : BaseFilter() {

    /** Normalized extension sets for consistent comparison */
    private val normalizedIncludeExtensions: Set<String>
    private val normalizedExcludeExtensions: Set<String>

    init {
        normalizedIncludeExtensions =
                if (caseSensitive) {
                    includeExtensions.map { it.removePrefix(".") }.toSet()
                } else {
                    includeExtensions
                            .map { it.removePrefix(".").lowercase(Locale.getDefault()) }
                            .toSet()
                }

        normalizedExcludeExtensions =
                if (caseSensitive) {
                    excludeExtensions.map { it.removePrefix(".") }.toSet()
                } else {
                    excludeExtensions
                            .map { it.removePrefix(".").lowercase(Locale.getDefault()) }
                            .toSet()
                }
    }

    override fun shouldIncludeFile(file: File, relativePath: String): Boolean {
        if (!file.isFile) return false

        val extension = getFileExtension(file)
        val normalizedExtension =
                if (caseSensitive) extension else extension.lowercase(Locale.getDefault())

        // If include list is specified, file must be in it
        if (normalizedIncludeExtensions.isNotEmpty()) {
            if (normalizedExtension !in normalizedIncludeExtensions) {
                return false
            }
        }

        // If exclude list is specified, file must not be in it
        if (normalizedExcludeExtensions.isNotEmpty()) {
            if (normalizedExtension in normalizedExcludeExtensions) {
                return false
            }
        }

        return true
    }

    override fun shouldIncludeLine(line: String, lineNumber: Int, file: File): Boolean {
        // File extension filter doesn't filter at line level
        return true
    }

    override fun getDescription(): String {
        val includeDesc =
                if (normalizedIncludeExtensions.isNotEmpty()) {
                    "include: [${normalizedIncludeExtensions.joinToString(", ")}]"
                } else ""

        val excludeDesc =
                if (normalizedExcludeExtensions.isNotEmpty()) {
                    "exclude: [${normalizedExcludeExtensions.joinToString(", ")}]"
                } else ""

        val parts =
                listOfNotNull(
                        includeDesc.takeIf { it.isNotEmpty() },
                        excludeDesc.takeIf { it.isNotEmpty() }
                )

        val sensitivity = if (caseSensitive) "case-sensitive" else "case-insensitive"

        return "File extension filter (${parts.joinToString(", ")}) - $sensitivity"
    }

    override fun getPriority(): Int = priority

    override fun computeApplicability(file: File, fileExtension: String): Boolean {
        // This filter is always applicable as it works with all files
        return true
    }

    override fun validateConfiguration(): List<String> {
        val errors = mutableListOf<String>()

        // Check for conflicting include/exclude patterns
        val includeExcludeOverlap =
                normalizedIncludeExtensions.intersect(normalizedExcludeExtensions)
        if (includeExcludeOverlap.isNotEmpty()) {
            errors.add(
                    "Extensions cannot be both included and excluded: ${includeExcludeOverlap.joinToString(", ")}"
            )
        }

        // Validate extension formats
        val invalidExtensions =
                (includeExtensions + excludeExtensions).filter { ext ->
                    ext.contains('/') ||
                            ext.contains('\\') ||
                            ext.contains('*') ||
                            ext.contains('?')
                }
        if (invalidExtensions.isNotEmpty()) {
            errors.add(
                    "Invalid extension formats (should not contain path separators or wildcards): ${invalidExtensions.joinToString(", ")}"
            )
        }

        return errors
    }

    override fun getMetadata(): Map<String, Any> {
        return super.getMetadata() +
                mapOf(
                        "includeExtensions" to includeExtensions,
                        "excludeExtensions" to excludeExtensions,
                        "caseSensitive" to caseSensitive,
                        "normalizedIncludeExtensions" to normalizedIncludeExtensions,
                        "normalizedExcludeExtensions" to normalizedExcludeExtensions
                )
    }

    companion object {
        /** Common source code file extensions */
        val SOURCE_CODE_EXTENSIONS =
                setOf(
                        "kt",
                        "java",
                        "scala",
                        "groovy",
                        "kts",
                        "js",
                        "ts",
                        "jsx",
                        "tsx",
                        "vue",
                        "svelte",
                        "py",
                        "rb",
                        "php",
                        "go",
                        "rs",
                        "swift",
                        "m",
                        "mm",
                        "c",
                        "cpp",
                        "cc",
                        "cxx",
                        "h",
                        "hpp",
                        "hxx",
                        "cs",
                        "vb",
                        "fs",
                        "fsx",
                        "sh",
                        "bash",
                        "zsh",
                        "fish",
                        "ps1",
                        "bat",
                        "cmd"
                )

        /** Configuration and data file extensions */
        val CONFIG_EXTENSIONS =
                setOf(
                        "yml",
                        "yaml",
                        "json",
                        "xml",
                        "toml",
                        "ini",
                        "cfg",
                        "conf",
                        "properties",
                        "env",
                        "config",
                        "settings"
                )

        /** Documentation file extensions */
        val DOCUMENTATION_EXTENSIONS =
                setOf(
                        "md",
                        "rst",
                        "txt",
                        "adoc",
                        "asciidoc",
                        "org",
                        "tex",
                        "latex",
                        "html",
                        "htm",
                        "xhtml"
                )

        /** Build and project file extensions */
        val BUILD_EXTENSIONS =
                setOf(
                        "gradle",
                        "kts",
                        "xml",
                        "pom",
                        "sbt",
                        "build",
                        "mk",
                        "makefile",
                        "cmake",
                        "bazel",
                        "buck"
                )

        /** Binary and compiled file extensions (typically excluded) */
        val BINARY_EXTENSIONS =
                setOf(
                        "class",
                        "jar",
                        "war",
                        "ear",
                        "aar",
                        "exe",
                        "dll",
                        "so",
                        "dylib",
                        "a",
                        "lib",
                        "zip",
                        "tar",
                        "gz",
                        "bz2",
                        "xz",
                        "7z",
                        "rar",
                        "jpg",
                        "jpeg",
                        "png",
                        "gif",
                        "bmp",
                        "svg",
                        "ico",
                        "mp3",
                        "mp4",
                        "avi",
                        "mov",
                        "wmv",
                        "flv",
                        "pdf",
                        "doc",
                        "docx",
                        "xls",
                        "xlsx",
                        "ppt",
                        "pptx"
                )

        /** Test file extensions and patterns */
        val TEST_EXTENSIONS =
                setOf(
                        "test.kt",
                        "test.java",
                        "spec.kt",
                        "spec.java",
                        "test.js",
                        "spec.js",
                        "test.ts",
                        "spec.ts"
                )

        /** Temporary and cache file extensions (typically excluded) */
        val TEMPORARY_EXTENSIONS =
                setOf("tmp", "temp", "cache", "log", "bak", "backup", "swp", "swo", "lock", "pid")

        /** Creates a filter that includes only source code files */
        fun forSourceCode(caseSensitive: Boolean = false): FileExtensionFilter {
            return FileExtensionFilter(
                    includeExtensions = SOURCE_CODE_EXTENSIONS,
                    caseSensitive = caseSensitive,
                    priority = 10
            )
        }

        /** Creates a filter that includes source code and configuration files */
        fun forSourceAndConfig(caseSensitive: Boolean = false): FileExtensionFilter {
            return FileExtensionFilter(
                    includeExtensions = SOURCE_CODE_EXTENSIONS + CONFIG_EXTENSIONS,
                    caseSensitive = caseSensitive,
                    priority = 10
            )
        }

        /** Creates a filter that excludes binary and temporary files */
        fun excludeBinaryAndTemp(caseSensitive: Boolean = false): FileExtensionFilter {
            return FileExtensionFilter(
                    excludeExtensions = BINARY_EXTENSIONS + TEMPORARY_EXTENSIONS,
                    caseSensitive = caseSensitive,
                    priority = 10
            )
        }

        /** Creates a filter that includes all typical scannable files */
        fun forScannableFiles(caseSensitive: Boolean = false): FileExtensionFilter {
            return FileExtensionFilter(
                    includeExtensions =
                            SOURCE_CODE_EXTENSIONS +
                                    CONFIG_EXTENSIONS +
                                    DOCUMENTATION_EXTENSIONS +
                                    BUILD_EXTENSIONS,
                    excludeExtensions = BINARY_EXTENSIONS + TEMPORARY_EXTENSIONS,
                    caseSensitive = caseSensitive,
                    priority = 10
            )
        }

        /** Creates a filter with custom extensions */
        fun custom(
                include: Set<String> = emptySet(),
                exclude: Set<String> = emptySet(),
                caseSensitive: Boolean = false,
                priority: Int = 10
        ): FileExtensionFilter {
            return FileExtensionFilter(
                    includeExtensions = include,
                    excludeExtensions = exclude,
                    caseSensitive = caseSensitive,
                    priority = priority
            )
        }
    }
}

/** Builder class for creating FileExtensionFilter instances with a fluent API */
class FileExtensionFilterBuilder {
    private val includeExtensions = mutableSetOf<String>()
    private val excludeExtensions = mutableSetOf<String>()
    private var caseSensitive = false
    private var priority = 10

    /** Add extensions to include */
    fun include(vararg extensions: String): FileExtensionFilterBuilder {
        includeExtensions.addAll(extensions)
        return this
    }

    /** Add a set of extensions to include */
    fun include(extensions: Set<String>): FileExtensionFilterBuilder {
        includeExtensions.addAll(extensions)
        return this
    }

    /** Add extensions to exclude */
    fun exclude(vararg extensions: String): FileExtensionFilterBuilder {
        excludeExtensions.addAll(extensions)
        return this
    }

    /** Add a set of extensions to exclude */
    fun exclude(extensions: Set<String>): FileExtensionFilterBuilder {
        excludeExtensions.addAll(extensions)
        return this
    }

    /** Include all source code extensions */
    fun includeSourceCode(): FileExtensionFilterBuilder {
        includeExtensions.addAll(FileExtensionFilter.SOURCE_CODE_EXTENSIONS)
        return this
    }

    /** Include all configuration file extensions */
    fun includeConfig(): FileExtensionFilterBuilder {
        includeExtensions.addAll(FileExtensionFilter.CONFIG_EXTENSIONS)
        return this
    }

    /** Exclude all binary file extensions */
    fun excludeBinary(): FileExtensionFilterBuilder {
        excludeExtensions.addAll(FileExtensionFilter.BINARY_EXTENSIONS)
        return this
    }

    /** Exclude all temporary file extensions */
    fun excludeTemporary(): FileExtensionFilterBuilder {
        excludeExtensions.addAll(FileExtensionFilter.TEMPORARY_EXTENSIONS)
        return this
    }

    /** Set case sensitivity */
    fun caseSensitive(sensitive: Boolean = true): FileExtensionFilterBuilder {
        this.caseSensitive = sensitive
        return this
    }

    /** Set filter priority */
    fun priority(priority: Int): FileExtensionFilterBuilder {
        this.priority = priority
        return this
    }

    /** Build the FileExtensionFilter */
    fun build(): FileExtensionFilter {
        return FileExtensionFilter(
                includeExtensions = includeExtensions.toSet(),
                excludeExtensions = excludeExtensions.toSet(),
                caseSensitive = caseSensitive,
                priority = priority
        )
    }
}

/** Extension function to create FileExtensionFilterBuilder */
fun fileExtensionFilter(): FileExtensionFilterBuilder = FileExtensionFilterBuilder()
