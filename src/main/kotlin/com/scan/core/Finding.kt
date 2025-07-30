package com.scan.core

/**
 * Represents a single security finding detected during scanning.
 * This is the core data structure that contains information about discovered secrets or sensitive data.
 */
data class Finding(
    val id: String,
    val title: String,
    val description: String,
    val severity: Severity,
    val confidence: Confidence,
    val detectorType: String,
    val detectorName: String,
    val location: FindingLocation,
    val secretInfo: SecretInfo,
    val context: FindingContext,
    val remediation: RemediationInfo,
    val metadata: Map<String, Any> = emptyMap()
) {
    /** Check if this finding should block the build */
    fun shouldBlockBuild(blockingThreshold: Severity): Boolean {
        return severity.ordinal <= blockingThreshold.ordinal
    }

    /** Get a short description for display */
    fun getShortDescription(): String {
        return if (description.length > 100) {
            "${description.take(97)}..."
        } else {
            description
        }
    }

    /** Get the detected secret value (masked for security) */
    fun getMaskedSecret(): String {
        val secret = secretInfo.detectedValue
        return when {
            secret.length <= 4 -> "*".repeat(secret.length)
            secret.length <= 8 ->
                    "${secret.take(2)}${"*".repeat(secret.length - 4)}${secret.takeLast(2)}"
            else ->
                    "${secret.take(3)}${"*".repeat(secret.length - 6)}${secret.takeLast(3)}"
        }
    }

    /** Get confidence as percentage */
    fun getConfidencePercentage(): Int = when (confidence) {
        Confidence.HIGH -> 85
        Confidence.MEDIUM -> 65
        Confidence.LOW -> 40
    }
}

/** Location information for a finding */
data class FindingLocation(
    val filePath: String,
    val lineNumber: Int,
    val columnStart: Int,
    val columnEnd: Int,
    val startIndex: Int = 0,
    val endIndex: Int = 0
) {
    /** Get position as a readable string */
    fun getPositionString(): String = "$filePath:$lineNumber:$columnStart"
    
    /** Check if this location contains another location */
    fun contains(other: FindingLocation): Boolean {
        return filePath == other.filePath &&
               lineNumber == other.lineNumber &&
               columnStart <= other.columnStart &&
               columnEnd >= other.columnEnd
    }
}

/** Information about the detected secret */
data class SecretInfo(
    val detectedValue: String,
    val secretType: SecretType,
    val entropy: Double = 0.0,
    val matchedPattern: String? = null,
    val validationStatus: ValidationStatus = ValidationStatus.NOT_VALIDATED,
    val additionalInfo: Map<String, Any> = emptyMap()
) {
    /** Check if secret appears to be valid/active */
    fun isLikelyValid(): Boolean = validationStatus == ValidationStatus.VALID
    
    /** Get entropy category */
    fun getEntropyCategory(): String = when {
        entropy >= 4.5 -> "High"
        entropy >= 3.5 -> "Medium"
        entropy >= 2.5 -> "Low"
        else -> "Very Low"
    }
}

/** Context information around the finding */
data class FindingContext(
    val lineContent: String,
    val before: List<String> = emptyList(),
    val after: List<String> = emptyList(),
    val isInComment: Boolean = false,
    val isInString: Boolean = false,
    val isInTestFile: Boolean = false,
    val isInConfigFile: Boolean = false,
    val functionName: String? = null,
    val className: String? = null,
    val variableName: String? = null
) {
    /** Check if finding is in a risky context */
    fun isRiskyContext(): Boolean {
        return !isInComment && !isInTestFile && (isInString || isInConfigFile)
    }

    /** Get context description */
    fun getContextDescription(): String {
        val contexts = mutableListOf<String>()
        if (isInComment) contexts.add("comment")
        if (isInString) contexts.add("string literal")
        if (isInTestFile) contexts.add("test file")
        if (isInConfigFile) contexts.add("configuration file")
        functionName?.let { contexts.add("function '$it'") }
        className?.let { contexts.add("class '$it'") }
        variableName?.let { contexts.add("variable '$it'") }

        return when {
            contexts.isEmpty() -> "code"
            contexts.size == 1 -> contexts.first()
            else -> contexts.dropLast(1).joinToString(", ") + " and " + contexts.last()
        }
    }
}

/** Remediation guidance for the finding */
data class RemediationInfo(
    val recommendation: String,
    val actionItems: List<String>,
    val references: List<String> = emptyList(),
    val automatedFix: AutomatedFix? = null
) {
    /** Check if automated fix is available */
    fun hasAutomatedFix(): Boolean = automatedFix != null
}

/** Automated fix information */
data class AutomatedFix(
    val description: String,
    val replacement: String,
    val requiresManualReview: Boolean = true
)
