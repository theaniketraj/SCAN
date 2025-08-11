package com.scan.core

/**
 * Test utility functions for creating test data
 */

/** Simple helper to create Finding instances for tests */
fun createTestFinding(
    file: String,
    line: Int,
    column: Int,
    pattern: String,
    match: String,
    severity: Severity = Severity.MEDIUM,
    confidence: Double = 0.8,
    context: String = ""
): Finding {
    val confidenceEnum = when {
        confidence >= 0.8 -> Confidence.HIGH
        confidence >= 0.5 -> Confidence.MEDIUM
        else -> Confidence.LOW
    }
    
    val location = FindingLocation(
        filePath = file,
        relativePath = file,
        lineNumber = line,
        columnStart = column,
        columnEnd = column + match.length,
        lineContent = context
    )
    
    val secretInfo = SecretInfo(
        detectedValue = match,
        patternName = pattern,
        patternDescription = "Test pattern",
        secretType = SecretType.UNKNOWN
    )
    
    val findingContext = FindingContext(
        lineContent = context,
        surroundingLines = listOf(context),
        isInComment = false,
        isInString = true,
        isInTestFile = false,
        isInConfigFile = false
    )
    
    val remediation = RemediationInfo(
        recommendation = "Test remediation",
        actionItems = listOf("Test action")
    )
    
    return Finding(
        id = "test-${System.nanoTime()}",
        title = "Test Finding: $pattern",
        description = "Test finding for pattern: $pattern",
        severity = severity,
        confidence = confidenceEnum,
        detectorType = "test",
        detectorName = "TestDetector",
        location = location,
        secretInfo = secretInfo,
        context = findingContext,
        remediation = remediation
    )
}
