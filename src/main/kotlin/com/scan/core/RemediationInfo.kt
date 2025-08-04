package com.scan.core

/**
 * Remediation information for a finding
 */
data class RemediationInfo(
    val recommendation: String,
    val actionItems: List<String> = emptyList(),
    val severity: String = "medium",
    val effort: String = "low",
    val references: List<String> = emptyList()
)
