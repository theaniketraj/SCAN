package com.scan.detectors

import com.scan.core.*
import com.scan.utils.PatternMatcher
import java.io.File
import java.util.regex.Pattern

/**
 * Context-aware detector that understands code structure and reduces false positives
 * by analyzing the context in which potential secrets appear.
 *
 * This detector:
 * - Distinguishes between comments, strings, and actual code
 * - Identifies variable assignments and their context
 * - Recognizes test files and applies different rules
 * - Understands common false positive patterns
 * - Analyzes surrounding code for contextual clues
 */
class ContextAwareDetectorImpl(
    private val patternMatcher: PatternMatcher,
    private val entropyThreshold: Double = 4.5,
    private val minimumSecretLength: Int = 12
) : AbstractDetector() {

    override val detectorId: String = "context-aware"
    override val detectorName: String = "Context-Aware Detector"
    override val version: String = "2.2.0"
    override val supportedFileTypes: Set<String> = setOf("*")

    companion object {
        // Context patterns for different programming languages
        private val COMMENT_PATTERNS = mapOf(
            "kotlin" to listOf("//.*", "/\\*[\\s\\S]*?\\*/", "\\*.*"),
            "java" to listOf("//.*", "/\\*[\\s\\S]*?\\*/", "\\*.*"),
            "javascript" to listOf("//.*", "/\\*[\\s\\S]*?\\*/"),
            "python" to listOf("#.*", "\"\"\"[\\s\\S]*?\"\"\"", "'''[\\s\\S]*?'''"),
            "yaml" to listOf("#.*"),
            "properties" to listOf("#.*", "!.*"),
            "xml" to listOf("<!--[\\s\\S]*?-->")
        )

        // String literal patterns
        private val STRING_PATTERNS = mapOf(
            "kotlin" to listOf("\"([^\"\\\\]|\\\\.)*\"", "'([^'\\\\]|\\\\.)*'", "\"\"\"[\\s\\S]*?\"\"\""),
            "java" to listOf("\"([^\"\\\\]|\\\\.)*\"", "'([^'\\\\]|\\\\.)*'"),
            "javascript" to listOf("\"([^\"\\\\]|\\\\.)*\"", "'([^'\\\\]|\\\\.)*'", "`[^`]*`"),
            "python" to listOf("\"([^\"\\\\]|\\\\.)*\"", "'([^'\\\\]|\\\\.)*'", "\"\"\"[\\s\\S]*?\"\"\"", "'''[\\s\\S]*?'''"),
            "yaml" to listOf("\"([^\"\\\\]|\\\\.)*\"", "'([^'\\\\]|\\\\.)*'"),
            "properties" to listOf("\"([^\"\\\\]|\\\\.)*\"", "'([^'\\\\]|\\\\.)*'"),
            "json" to listOf("\"([^\"\\\\]|\\\\.)*\"")
        )

        // Variable assignment patterns
        private val ASSIGNMENT_PATTERNS = listOf(
            "\\b(\\w+)\\s*[:=]\\s*([\"'][^\"']*[\"'])", // Simple assignment
            "\\b(const|val|var|let)\\s+(\\w+)\\s*[:=]\\s*([\"'][^\"']*[\"'])", // Declaration with assignment
            "\\b(\\w+)\\s*[:=]\\s*\\$\\{([^}]+)\\}", // Environment variable assignment
            "\\bsetProperty\\s*\\(\\s*[\"']([^\"']+)[\"']\\s*,\\s*([\"'][^\"']*[\"'])\\)", // Property setting
            "(\\w+)\\.(\\w+)\\s*=\\s*([\"'][^\"']*[\"'])" // Object property assignment
        )

        // False positive indicators
        private val FALSE_POSITIVE_PATTERNS = listOf(
            "(?i)\\b(example|sample|test|dummy|fake|mock|placeholder|your[_-]?key|api[_-]?key[_-]?here)\\b",
            "(?i)\\b(todo|fixme|replace|change|update)\\b",
            "\\b(123456|abcdef|qwerty|password|secret)\\b",
            "\\$\\{[^}]+\\}", // Environment variable placeholders
            "\\[[^\\]]+\\]", // Placeholder brackets
            "<[^>]+>", // XML/HTML-like placeholders
            "(?i)\\b(localhost|127\\.0\\.0\\.1|example\\.com)\\b"
        )

        // Test file indicators
        private val TEST_FILE_INDICATORS = listOf(
            "test",
            "spec",
            "mock",
            "fixture",
            "sample"
        )

        // High-confidence secret patterns that should always be flagged
        private val HIGH_CONFIDENCE_PATTERNS = listOf(
            "\\b[A-Za-z0-9]{20,}\\b", // Long alphanumeric strings
            "\\b[A-Za-z0-9+/]{32,}={0,2}\\b", // Base64-like strings
            "\\b[0-9a-fA-F]{32,}\\b", // Hex strings
            "-----BEGIN [A-Z ]+-----[\\s\\S]*?-----END [A-Z ]+-----" // PEM format
        )
    }

    override fun performDetection(context: ScanContext): List<Finding> {
        val findings = mutableListOf<Finding>()
        val fileExtension = context.fileExtension.lowercase()
        val fileName = context.fileName.lowercase()
        val isTestFile = isTestFile(context.filePath.toFile(), fileName)

        // Parse file content into structured data
        val fileContext = parseFileContext(context.content, fileExtension)

        // Analyze each line with context
        context.content.lines().forEachIndexed { lineIndex, line ->
            val lineNumber = lineIndex + 1
            val lineContext = determineLineContext(line, lineIndex, fileContext, fileExtension)

            // Skip analysis for certain contexts unless high confidence
            if (shouldSkipLine(lineContext, isTestFile)) {
                return@forEachIndexed
            }

            // Extract potential secrets from the line
            val potentialSecrets = extractPotentialSecrets(line, lineContext)

            potentialSecrets.forEach { secretCandidate ->
                val confidence = calculateConfidence(
                    secretCandidate,
                    lineContext,
                    fileContext,
                    isTestFile,
                    context.filePath.toFile()
                )

                if (confidence > 0.3) { // Only report findings with reasonable confidence
                    findings.add(createFinding(
                        type = secretCandidate.type.toString(),
                        value = secretCandidate.value,
                        lineNumber = lineNumber,
                        columnStart = line.indexOf(secretCandidate.value),
                        columnEnd = line.indexOf(secretCandidate.value) + secretCandidate.value.length,
                        confidence = confidence,
                        rule = "context-aware-${secretCandidate.type}",
                        context = context
                    ))
                }
            }
        }

        return findings
    }

    private fun parseFileContext(content: String, fileExtension: String): FileContext {
        val imports = extractImports(content, fileExtension)
        val functions = extractFunctions(content, fileExtension)
        val classes = extractClasses(content, fileExtension)
        val variables = extractVariables(content, fileExtension)

        return FileContext(
            extension = fileExtension,
            imports = imports,
            functions = functions,
            classes = classes,
            variables = variables,
            totalLines = content.lines().size
        )
    }

    private fun determineLineContext(
        line: String,
        lineIndex: Int,
        fileContext: FileContext,
        fileExtension: String
    ): LineContext {
        val trimmedLine = line.trim()

        // Check if line is a comment
        val isComment = isComment(trimmedLine, fileExtension)

        // Check if line is in a string literal
        val isInString = isInStringLiteral(trimmedLine, fileExtension)

        // Check for variable assignment
        val assignment = extractAssignment(trimmedLine)

        // Check if line is in a function
        val functionContext = findContainingFunction(lineIndex, fileContext)

        // Check if line is in a class
        val classContext = findContainingClass(lineIndex, fileContext)

        return LineContext(
            lineNumber = lineIndex + 1,
            content = line,
            isComment = isComment,
            isInString = isInString,
            assignment = assignment,
            functionContext = functionContext,
            classContext = classContext,
            indentationLevel = line.length - line.trimStart().length
        )
    }

    private fun extractPotentialSecrets(line: String, context: LineContext): List<SecretCandidate> {
        val candidates = mutableListOf<SecretCandidate>()

        // Extract from string literals
        extractFromStrings(line).forEach { (value, start, end) ->
            if (value.length >= minimumSecretLength) {
                val type = classifySecret(value)
                if (type != SecretType.UNKNOWN || calculateEntropy(value) > entropyThreshold) {
                    candidates.add(SecretCandidate(value, type, start, end, "string_literal"))
                }
            }
        }

        // Extract from assignments
        context.assignment?.let { assignment ->
            val value = assignment.value
            if (value.length >= minimumSecretLength) {
                val type = classifySecret(value)
                if (type != SecretType.UNKNOWN || calculateEntropy(value) > entropyThreshold) {
                    candidates.add(SecretCandidate(value, type, 0, value.length, "assignment"))
                }
            }
        }

        return candidates
    }

    @Suppress("UNUSED_PARAMETER")
    private fun calculateConfidence(
        candidate: SecretCandidate,
        lineContext: LineContext,
        fileContext: FileContext,
        isTestFile: Boolean,
        file: File
    ): Double {
        var confidence = 0.0

        // Base confidence from secret type
        confidence += when (candidate.type) {
            SecretType.API_KEY -> 0.7
            SecretType.PRIVATE_KEY -> 0.9
            SecretType.PASSWORD -> 0.6
            SecretType.TOKEN -> 0.7
            SecretType.DATABASE_URL -> 0.8
            SecretType.CERTIFICATE -> 0.9
            SecretType.HIGH_ENTROPY -> calculateEntropy(candidate.value) / 8.0 // Normalize entropy
            SecretType.UNKNOWN -> 0.3
        }

        // Adjust based on context
        if (lineContext.isComment) {
            confidence *= 0.3 // Comments are less likely to contain real secrets
        }

        if (isTestFile) {
            confidence *= 0.4 // Test files often contain dummy data
        }

        // Check for false positive patterns
        if (containsFalsePositivePattern(candidate.value)) {
            confidence *= 0.1
        }

        // Check variable name context
        lineContext.assignment?.let { assignment ->
            val variableName = assignment.variableName.lowercase()
            when {
                variableName.contains("secret") || variableName.contains("key") ||
                    variableName.contains("token") || variableName.contains("password") -> {
                    confidence *= 1.5
                }
                variableName.contains("test") || variableName.contains("mock") ||
                    variableName.contains("example") -> {
                    confidence *= 0.3
                }
            }
        }

        // Check for high-confidence patterns
        if (matchesHighConfidencePattern(candidate.value)) {
            confidence *= 1.3
        }

        // File path context
        val filePath = file.path.lowercase()
        when {
            filePath.contains("config") || filePath.contains("env") -> confidence *= 1.2
            filePath.contains("test") || filePath.contains("mock") -> confidence *= 0.5
            filePath.contains("example") || filePath.contains("sample") -> confidence *= 0.3
        }

        return minOf(1.0, maxOf(0.0, confidence))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun shouldSkipLine(context: LineContext, isTestFile: Boolean): Boolean {
        // Skip comments unless they contain high-confidence patterns
        if (context.isComment && !containsHighConfidencePattern(context.content)) {
            return true
        }

        // Skip empty lines
        if (context.content.trim().isEmpty()) {
            return true
        }

        return false
    }

    private fun isTestFile(file: File, fileName: String): Boolean {
        val filePath = file.path.lowercase()
        return TEST_FILE_INDICATORS.any { indicator ->
            fileName.contains(indicator) || filePath.contains("/$indicator/") || filePath.contains("\\$indicator\\")
        }
    }

    private fun isComment(line: String, fileExtension: String): Boolean {
        val patterns = COMMENT_PATTERNS[fileExtension] ?: return false
        return patterns.any { pattern ->
            Pattern.compile(pattern).matcher(line).find()
        }
    }

    private fun isInStringLiteral(line: String, fileExtension: String): Boolean {
        val patterns = STRING_PATTERNS[fileExtension] ?: return false
        return patterns.any { pattern ->
            Pattern.compile(pattern).matcher(line).find()
        }
    }

    private fun extractAssignment(line: String): Assignment? {
        ASSIGNMENT_PATTERNS.forEach { pattern ->
            val matcher = Pattern.compile(pattern).matcher(line)
            if (matcher.find()) {
                return Assignment(
                    variableName = matcher.group(1),
                    value = matcher.group(matcher.groupCount()).removeSurrounding("\"", "'")
                )
            }
        }
        return null
    }

    private fun extractFromStrings(line: String): List<Triple<String, Int, Int>> {
        val results = mutableListOf<Triple<String, Int, Int>>()
        val stringPattern = Pattern.compile("([\"'])([^\"'\\\\]|\\\\.)*\\1")
        val matcher = stringPattern.matcher(line)

        while (matcher.find()) {
            val value = matcher.group().removeSurrounding("\"", "'")
            if (value.isNotBlank()) {
                results.add(Triple(value, matcher.start(), matcher.end()))
            }
        }

        return results
    }

    private fun classifySecret(value: String): SecretType {
        return when {
            patternMatcher.matchesApiKeyPattern(value) -> SecretType.API_KEY
            patternMatcher.matchesPrivateKeyPattern(value) -> SecretType.PRIVATE_KEY
            patternMatcher.matchesPasswordPattern(value) -> SecretType.PASSWORD
            patternMatcher.matchesTokenPattern(value) -> SecretType.TOKEN
            patternMatcher.matchesDatabaseUrlPattern(value) -> SecretType.DATABASE_URL
            patternMatcher.matchesCertificatePattern(value) -> SecretType.CERTIFICATE
            calculateEntropy(value) > entropyThreshold -> SecretType.HIGH_ENTROPY
            else -> SecretType.UNKNOWN
        }
    }

    private fun calculateEntropy(text: String): Double {
        if (text.isEmpty()) return 0.0

        val frequencies = text.groupingBy { it }.eachCount()
        val length = text.length.toDouble()

        return frequencies.values.sumOf { count ->
            val probability = count / length
            -probability * kotlin.math.log2(probability)
        }
    }

    private fun containsFalsePositivePattern(value: String): Boolean {
        return FALSE_POSITIVE_PATTERNS.any { pattern ->
            Pattern.compile(pattern).matcher(value).find()
        }
    }

    private fun matchesHighConfidencePattern(value: String): Boolean {
        return HIGH_CONFIDENCE_PATTERNS.any { pattern ->
            Pattern.compile(pattern).matcher(value).find()
        }
    }

    private fun containsHighConfidencePattern(line: String): Boolean {
        return HIGH_CONFIDENCE_PATTERNS.any { pattern ->
            Pattern.compile(pattern).matcher(line).find()
        }
    }

    private fun buildContextualMessage(candidate: SecretCandidate, context: LineContext): String {
        val baseMessage = when (candidate.type) {
            SecretType.API_KEY -> "Potential API key detected"
            SecretType.PRIVATE_KEY -> "Private key detected"
            SecretType.PASSWORD -> "Potential password detected"
            SecretType.TOKEN -> "Potential token detected"
            SecretType.DATABASE_URL -> "Database connection string detected"
            SecretType.CERTIFICATE -> "Certificate detected"
            SecretType.HIGH_ENTROPY -> "High-entropy string detected (potential secret)"
            SecretType.UNKNOWN -> "Potential secret detected"
        }

        val contextInfo = when {
            context.isComment -> " in comment"
            context.assignment != null -> " in variable assignment '${context.assignment.variableName}'"
            context.isInString -> " in string literal"
            else -> ""
        }

        return "$baseMessage$contextInfo"
    }

    // Helper methods for extracting code structure
    private fun extractImports(content: String, fileExtension: String): List<String> {
        val imports = mutableListOf<String>()
        val patterns = when (fileExtension) {
            "kotlin", "java" -> listOf("import\\s+([\\w.]+)")
            "javascript", "typescript" -> listOf("import\\s+.*from\\s+['\"][^'\"]+['\"]")
            "python" -> listOf("from\\s+([\\w.]+)\\s+import", "import\\s+([\\w.]+)")
            else -> emptyList()
        }

        patterns.forEach { pattern ->
            Pattern.compile(pattern).matcher(content).let { matcher ->
                while (matcher.find()) {
                    imports.add(matcher.group(1))
                }
            }
        }

        return imports
    }

    private fun extractFunctions(content: String, fileExtension: String): List<FunctionInfo> {
        // Simplified function extraction - can be enhanced based on specific language needs
        val functions = mutableListOf<FunctionInfo>()
        val pattern = when (fileExtension) {
            "kotlin" -> "fun\\s+(\\w+)\\s*\\("
            "java" -> "(?:public|private|protected)?\\s*(?:static)?\\s*\\w+\\s+(\\w+)\\s*\\("
            else -> return functions
        }

        val matcher = Pattern.compile(pattern).matcher(content)
        while (matcher.find()) {
            functions.add(FunctionInfo(matcher.group(1), matcher.start()))
        }

        return functions
    }

    private fun extractClasses(content: String, fileExtension: String): List<ClassInfo> {
        val classes = mutableListOf<ClassInfo>()
        val pattern = when (fileExtension) {
            "kotlin" -> "class\\s+(\\w+)"
            "java" -> "(?:public|private|protected)?\\s*class\\s+(\\w+)"
            else -> return classes
        }

        val matcher = Pattern.compile(pattern).matcher(content)
        while (matcher.find()) {
            classes.add(ClassInfo(matcher.group(1), matcher.start()))
        }

        return classes
    }

    private fun extractVariables(content: String, fileExtension: String): List<VariableInfo> {
        val variables = mutableListOf<VariableInfo>()
        val patterns = when (fileExtension) {
            "kotlin" -> listOf("val\\s+(\\w+)", "var\\s+(\\w+)")
            "java" -> listOf("(?:private|public|protected)?\\s*(?:static)?\\s*\\w+\\s+(\\w+)")
            else -> return variables
        }

        patterns.forEach { pattern ->
            val matcher = Pattern.compile(pattern).matcher(content)
            while (matcher.find()) {
                variables.add(VariableInfo(matcher.group(1), matcher.start()))
            }
        }

        return variables
    }

    private fun findContainingFunction(lineIndex: Int, fileContext: FileContext): String? {
        // Simplified - in real implementation, would need proper parsing
        return fileContext.functions.lastOrNull { it.startPosition <= lineIndex * 50 }?.name
    }

    private fun findContainingClass(lineIndex: Int, fileContext: FileContext): String? {
        // Simplified - in real implementation, would need proper parsing
        return fileContext.classes.lastOrNull { it.startPosition <= lineIndex * 50 }?.name
    }

    // Data classes for context information
    data class FileContext(
        val extension: String,
        val imports: List<String>,
        val functions: List<FunctionInfo>,
        val classes: List<ClassInfo>,
        val variables: List<VariableInfo>,
        val totalLines: Int
    )

    data class LineContext(
        val lineNumber: Int,
        val content: String,
        val isComment: Boolean,
        val isInString: Boolean,
        val assignment: Assignment?,
        val functionContext: String?,
        val classContext: String?,
        val indentationLevel: Int
    )

    data class Assignment(
        val variableName: String,
        val value: String
    )

    data class SecretCandidate(
        val value: String,
        val type: SecretType,
        val startPosition: Int,
        val endPosition: Int,
        val context: String
    )

    data class FunctionInfo(val name: String, val startPosition: Int)
    data class ClassInfo(val name: String, val startPosition: Int)
    data class VariableInfo(val name: String, val startPosition: Int)

    enum class SecretType {
        API_KEY,
        PRIVATE_KEY,
        PASSWORD,
        TOKEN,
        DATABASE_URL,
        CERTIFICATE,
        HIGH_ENTROPY,
        UNKNOWN
    }
}
