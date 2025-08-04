package com.scan.core

/**
 * Validation status for detected secrets
 */
enum class ValidationStatus {
    NOT_VALIDATED,
    VALID,
    INVALID,
    EXPIRED,
    REVOKED
}
