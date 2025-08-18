package com.scan.utils

import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.*

/**
 * Comprehensive file utility class providing robust file operations for the security scanning
 * plugin with performance optimizations, encoding detection, and safe file handling.
 */
object FileUtils {

    // Common text file extensions that should be scanned
    private val DEFAULT_TEXT_EXTENSIONS =
        setOf(
            "txt",
            "md",
            "java",
            "kt",
            "scala",
            "groovy",
            "js",
            "ts",
            "jsx",
            "tsx",
            "py",
            "rb",
            "php",
            "go",
            "rs",
            "cpp",
            "c",
            "h",
            "hpp",
            "cs",
            "vb",
            "sql",
            "xml",
            "html",
            "htm",
            "css",
            "scss",
            "sass",
            "less",
            "json",
            "yaml",
            "yml",
            "toml",
            "ini",
            "cfg",
            "conf",
            "config",
            "properties",
            "sh",
            "bash",
            "zsh",
            "fish",
            "bat",
            "cmd",
            "ps1",
            "dockerfile",
            "gradle",
            "maven",
            "pom",
            "build",
            "make",
            "makefile",
            "cmake",
            "proto",
            "thrift",
            "avro",
            "graphql",
            "gql",
            "vue",
            "svelte",
            "r",
            "matlab",
            "swift",
            "dart",
            "elm",
            "clj",
            "cljs",
            "hs",
            "ml"
        )

    // Binary file extensions that should typically be skipped
    private val BINARY_EXTENSIONS =
        setOf(
            "jpg",
            "jpeg",
            "png",
            "gif",
            "bmp",
            "tiff",
            "webp",
            "ico",
            "svg",
            "mp3",
            "mp4",
            "avi",
            "mov",
            "wmv",
            "flv",
            "webm",
            "ogg",
            "wav",
            "pdf",
            "doc",
            "docx",
            "xls",
            "xlsx",
            "ppt",
            "pptx",
            "zip",
            "rar",
            "7z",
            "tar",
            "gz",
            "bz2",
            "xz",
            "jar",
            "war",
            "ear",
            "class",
            "exe",
            "dll",
            "so",
            "dylib",
            "bin",
            "deb",
            "rpm",
            "msi",
            "dmg",
            "iso",
            "img",
            "vdi",
            "vmdk",
            "ova",
            "ovf",
            "db",
            "sqlite",
            "mdb"
        )

    // Large directories that are typically safe to skip
    private val SKIP_DIRECTORIES =
        setOf(
            ".git",
            ".svn",
            ".hg",
            ".bzr",
            "node_modules",
            "bower_components",
            "vendor",
            "target",
            "build",
            "dist",
            "out",
            ".gradle",
            ".idea",
            "build-cache",
            ".npm",
            ".yarn",
            ".pnpm",
            "__pycache__",
            ".pytest_cache",
            ".mypy_cache",
            ".tox",
            "venv",
            "env",
            ".env",
            "virtualenv",
            ".venv",
            ".DS_Store",
            "Thumbs.db",
            ".tmp",
            "tmp",
            "temp",
            ".cache",
            "cache"
        )

    // File size limits for safety (configurable)
    private const val DEFAULT_MAX_FILE_SIZE = 50L * 1024 * 1024 // 50MB
    private const val LARGE_FILE_THRESHOLD = 10 * 1024 * 1024 // 10MB

    // Encoding detection cache
    private val encodingCache = ConcurrentHashMap<String, Charset>()

    /** Configuration class for file traversal options */
    data class TraversalOptions(
        val maxDepth: Int = Int.MAX_VALUE,
        val followSymlinks: Boolean = false,
        val includeHidden: Boolean = false,
        val maxFileSize: Long = DEFAULT_MAX_FILE_SIZE,
        val customExtensions: Set<String>? = null,
        val excludePatterns: Set<Regex> = emptySet(),
        val includePatterns: Set<Regex> = emptySet(),
        val skipDirectories: Set<String> = SKIP_DIRECTORIES,
        val parallelProcessing: Boolean = true
    )

    /** Represents file metadata for scanning decisions */
    data class FileMetadata(
        val path: Path,
        val size: Long,
        val isText: Boolean,
        val encoding: Charset?,
        val lineCount: Int?,
        val hash: String?
    )

    /** Find all scannable files in the given directory with specified options */
    fun findScannableFiles(
        rootPath: Path,
        options: TraversalOptions = TraversalOptions()
    ): List<Path> {
        if (!Files.exists(rootPath)) {
            throw IllegalArgumentException("Path does not exist: $rootPath")
        }

        if (!Files.isReadable(rootPath)) {
            throw IllegalArgumentException("Path is not readable: $rootPath")
        }

        val result = mutableListOf<Path>()

        try {
            Files.walkFileTree(
                rootPath,
                EnumSet.of(FileVisitOption.FOLLOW_LINKS.takeIf { options.followSymlinks }),
                options.maxDepth,
                object : SimpleFileVisitor<Path>() {

                    override fun preVisitDirectory(
                        dir: Path,
                        attrs: BasicFileAttributes
                    ): FileVisitResult {
                        val dirName = dir.name

                        // Skip hidden directories unless explicitly included
                        if (!options.includeHidden && dirName.startsWith(".") && dir != rootPath
                        ) {
                            return FileVisitResult.SKIP_SUBTREE
                        }

                        // Skip common build/cache directories
                        if (options.skipDirectories.contains(dirName)) {
                            return FileVisitResult.SKIP_SUBTREE
                        }

                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFile(
                        file: Path,
                        attrs: BasicFileAttributes
                    ): FileVisitResult {
                        try {
                            if (shouldScanFile(file, attrs, options)) {
                                result.add(file)
                            }
                        } catch (e: SecurityException) {
                            System.err.println("Security exception processing file $file: ${e.message}")
                        } catch (e: IOException) {
                            System.err.println("I/O error processing file $file: ${e.message}")
                        }

                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFileFailed(
                        file: Path,
                        exc: IOException
                    ): FileVisitResult {
                        System.err.println("Failed to access file $file: ${exc.message}")
                        return FileVisitResult.CONTINUE
                    }
                }
            )
        } catch (e: IOException) {
            throw IOException("Failed to traverse directory: $rootPath", e)
        }

        return result.toList()
    }

    /** Determine if a file should be scanned based on various criteria */
    private fun shouldScanFile(
        file: Path,
        attrs: BasicFileAttributes,
        options: TraversalOptions
    ): Boolean {
        // Skip if not a regular file
        if (!attrs.isRegularFile) return false

        // Skip if file is too large
        if (attrs.size() > options.maxFileSize) return false

        // Skip hidden files unless explicitly included
        if (!options.includeHidden && file.name.startsWith(".")) return false

        // Check include patterns first (if any)
        if (options.includePatterns.isNotEmpty()) {
            val matches =
                options.includePatterns.any { pattern -> pattern.matches(file.toString()) }
            if (!matches) return false
        }

        // Check exclude patterns
        if (options.excludePatterns.isNotEmpty()) {
            val matches =
                options.excludePatterns.any { pattern -> pattern.matches(file.toString()) }
            if (matches) return false
        }

        // Check file extension
        return isScannableByExtension(file, options)
    }

    /** Check if file should be scanned based on its extension */
    private fun isScannableByExtension(file: Path, options: TraversalOptions): Boolean {
        val extension = getFileExtension(file)

        // Use custom extensions if provided
        val allowedExtensions = options.customExtensions ?: DEFAULT_TEXT_EXTENSIONS

        // Skip known binary extensions
        if (BINARY_EXTENSIONS.contains(extension)) return false

        // Include if extension is in allowed list
        if (allowedExtensions.contains(extension)) return true

        // For files without extensions, check if they're text files
        if (extension.isEmpty()) {
            return isTextFile(file)
        }

        return false
    }

    /** Get file extension in lowercase */
    fun getFileExtension(file: Path): String {
        val fileName = file.name
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot > 0 && lastDot < fileName.length - 1) {
            fileName.substring(lastDot + 1).lowercase()
        } else {
            ""
        }
    }

    /** Check if a file is a text file by examining its content */
    fun isTextFile(file: Path): Boolean {
        if (!Files.exists(file) || !Files.isReadable(file)) return false

        try {
            // Read first few KB to determine if it's text
            val sampleSize = minOf(Files.size(file), 8192)
            val bytes = ByteArray(sampleSize.toInt())

            Files.newInputStream(file).use { input -> input.read(bytes) }

            // Check for null bytes (strong indicator of binary content)
            if (bytes.contains(0.toByte())) return false

            // Check for high percentage of printable characters
            val printableCount =
                bytes.count { byte ->
                    val char = byte.toInt() and 0xFF
                    char in 32..126 ||
                        char in 9..13 ||
                        char > 127 // ASCII printable + whitespace + UTF-8
                }

            val printableRatio = printableCount.toDouble() / bytes.size
            return printableRatio > 0.7 // At least 70% printable characters
        } catch (_: IOException) {
            return false
        } catch (_: SecurityException) {
            return false
        }
    }

    /** Detect file encoding with caching */
    fun detectEncoding(file: Path): Charset {
        val filePath = file.toString()

        // Check cache first
        encodingCache[filePath]?.let {
            return it
        }

        val detectedEncoding =
            try {
                detectEncodingInternal(file)
            } catch (_: IOException) {
                StandardCharsets.UTF_8 // Fallback to UTF-8
            } catch (_: SecurityException) {
                StandardCharsets.UTF_8
            }

        // Cache the result
        encodingCache[filePath] = detectedEncoding
        return detectedEncoding
    }

    /** Internal encoding detection logic */
    private fun detectEncodingInternal(file: Path): Charset {
        if (!Files.exists(file)) return StandardCharsets.UTF_8

        // Read sample bytes for detection
        val sampleSize = minOf(Files.size(file), 4096)
        val bytes = ByteArray(sampleSize.toInt())

        Files.newInputStream(file).use { input -> input.read(bytes) }

        // Check for BOM (Byte Order Mark)
        when {
            bytes.size >= 3 &&
                bytes[0] == 0xEF.toByte() &&
                bytes[1] == 0xBB.toByte() &&
                bytes[2] == 0xBF.toByte() -> return StandardCharsets.UTF_8
            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() ->
                return StandardCharsets.UTF_16LE
            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() ->
                return StandardCharsets.UTF_16BE
        }

        // Try to decode as UTF-8 first
        return try {
            val decoded = String(bytes, StandardCharsets.UTF_8)
            // If it decodes without replacement characters, it's likely UTF-8
            if (!decoded.contains('\uFFFD')) {
                StandardCharsets.UTF_8
            } else {
                StandardCharsets.ISO_8859_1 // Fallback to Latin-1
            }
        } catch (_: IllegalArgumentException) {
            StandardCharsets.ISO_8859_1
        }
    }

    /** Read file content safely with encoding detection */
    fun readFileContent(file: Path, maxSize: Long = DEFAULT_MAX_FILE_SIZE): String {
        if (!Files.exists(file)) {
            throw IllegalArgumentException("File does not exist: $file")
        }

        if (!Files.isReadable(file)) {
            throw IllegalArgumentException("File is not readable: $file")
        }

        val fileSize = Files.size(file)
        if (fileSize > maxSize) {
            throw IllegalArgumentException("File too large: $fileSize bytes (max: $maxSize)")
        }

        val encoding = detectEncoding(file)

        return try {
            Files.readString(file, encoding)
        } catch (_: IOException) {
            // Fallback to reading as bytes and converting
            val bytes = Files.readAllBytes(file)
            String(bytes, encoding)
        }
    }

    /** Read file content in chunks for large files */
    fun readFileContentChunked(
        file: Path,
        chunkSize: Int = 8192,
        processor: (String, Int) -> Unit
    ) {
        if (!Files.exists(file) || !Files.isReadable(file)) return

        val encoding = detectEncoding(file)
        val buffer = CharArray(chunkSize)
        var chunkIndex = 0

        Files.newBufferedReader(file, encoding).use { reader ->
            var charsRead: Int
            while (reader.read(buffer).also { charsRead = it } != -1) {
                val chunk = String(buffer, 0, charsRead)
                processor(chunk, chunkIndex++)
            }
        }
    }

    /** Get comprehensive file metadata */
    fun getFileMetadata(file: Path, includeHash: Boolean = false): FileMetadata {
        if (!Files.exists(file)) {
            throw IllegalArgumentException("File does not exist: $file")
        }

        val attrs = Files.readAttributes(file, BasicFileAttributes::class.java)
        val isText = isTextFile(file)
        val encoding = if (isText) detectEncoding(file) else null

        var lineCount: Int? = null
        var hash: String? = null

        if (isText && attrs.size() < LARGE_FILE_THRESHOLD) {
            try {
                lineCount = countLines(file)
                if (includeHash) {
                    hash = calculateFileHash(file)
                }
            } catch (_: IOException) {
                // Ignore errors in metadata calculation
            }
        }

        return FileMetadata(
            path = file,
            size = attrs.size(),
            isText = isText,
            encoding = encoding,
            lineCount = lineCount,
            hash = hash
        )
    }

    /** Count lines in a text file efficiently */
    fun countLines(file: Path): Int {
        if (!Files.exists(file)) return 0

        return try {
            Files.lines(file).use { lines -> lines.mapToInt { 1 }.sum() }
        } catch (_: IOException) {
            // Fallback method
            try {
                Files.readAllLines(file).size
            } catch (_: IOException) {
                0
            }
        }
    }

    /** Calculate SHA-256 hash of file content */
    fun calculateFileHash(file: Path): String {
        if (!Files.exists(file)) return ""

        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            Files.newInputStream(file).use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }

            digest.digest().joinToString("") { String.format("%02x", it) }
        } catch (_: IOException) {
            ""
        }
    }

    /** Create a safe temporary file for processing */
    fun createTempFile(prefix: String = "scan", suffix: String = ".tmp"): Path {
        return try {
            Files.createTempFile(prefix, suffix)
        } catch (_: IOException) {
            // Fallback to system temp directory
            val tempDir = System.getProperty("java.io.tmpdir")
            val fileName = "${prefix}_${System.currentTimeMillis()}$suffix"
            Paths.get(tempDir, fileName)
        }
    }

    /** Safe file copy with progress tracking */
    fun copyFile(
        source: Path,
        target: Path,
        replaceExisting: Boolean = false,
        progressCallback: ((Long, Long) -> Unit)? = null
    ) {
        if (!Files.exists(source)) {
            throw IllegalArgumentException("Source file does not exist: $source")
        }

        val sourceSize = Files.size(source)
        var bytesCopied = 0L

        val options = mutableListOf<CopyOption>()
        if (replaceExisting) {
            options.add(StandardCopyOption.REPLACE_EXISTING)
        }

        try {
            if (progressCallback != null && sourceSize > LARGE_FILE_THRESHOLD) {
                // Copy with progress tracking for large files
                Files.newInputStream(source).use { input ->
                    Files.newOutputStream(target).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            bytesCopied += bytesRead
                            progressCallback(bytesCopied, sourceSize)
                        }
                    }
                }
            } else {
                // Simple copy for smaller files
                Files.copy(source, target, *options.toTypedArray())
            }
        } catch (e: IOException) {
            throw IOException("Failed to copy file from $source to $target", e)
        }
    }

    /** Get relative path from base directory */
    fun getRelativePath(basePath: Path, targetPath: Path): String {
        return try {
            basePath.relativize(targetPath).toString()
        } catch (_: IllegalArgumentException) {
            targetPath.toString()
        }
    }

    /** Check if path matches any of the given patterns */
    fun matchesPatterns(path: Path, patterns: Collection<Regex>): Boolean {
        if (patterns.isEmpty()) return false

        val pathString = path.toString()
        val fileName = path.name

        return patterns.any { pattern -> pattern.matches(pathString) || pattern.matches(fileName) }
    }

    /** Create directory if it doesn't exist */
    fun ensureDirectoryExists(directory: Path) {
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory)
            } catch (e: IOException) {
                throw RuntimeException("Failed to create directory: $directory", e)
            }
        } else if (!Files.isDirectory(directory)) {
            throw IllegalArgumentException("Path exists but is not a directory: $directory")
        }
    }

    /** Clean up temporary files and resources */
    fun cleanup() {
        encodingCache.clear()
        System.gc() // Suggest garbage collection
    }

    /** Get human-readable file size */
    fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return String.format("%.1f %s", size, units[unitIndex])
    }

    /** Calculate SHA-256 hash of a file */
    fun calculateSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
    }

    /** Validate file path for security (prevent path traversal attacks) */
    fun isSecurePath(basePath: Path, targetPath: Path): Boolean {
        return try {
            val normalizedBase = basePath.normalize().toAbsolutePath()
            val normalizedTarget = targetPath.normalize().toAbsolutePath()
            normalizedTarget.startsWith(normalizedBase)
        } catch (_: InvalidPathException) {
            false
        }
    }
}
