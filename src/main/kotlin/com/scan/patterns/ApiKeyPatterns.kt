package com.scan.patterns

import java.util.regex.Pattern

/**
 * Comprehensive collection of API key patterns for detecting various service credentials. This
 * class provides regex patterns to identify API keys, tokens, and authentication credentials from
 * popular cloud services, APIs, and third-party integrations.
 */
object ApiKeyPatterns {

    /** Data class representing an API key pattern with metadata */
    data class ApiKeyPattern(
            val name: String,
            val pattern: Pattern,
            val description: String,
            val severity: Severity = Severity.HIGH,
            val provider: String,
            val examples: List<String> = emptyList()
    )

    /** Severity levels for detected API keys */
    enum class Severity {
        CRITICAL, // Keys with immediate security impact
        HIGH, // Standard API keys
        MEDIUM, // Development/testing keys
        LOW // Potentially false positives
    }

    /** AWS (Amazon Web Services) API Keys and Secrets */
    private val awsPatterns =
            listOf(
                    ApiKeyPattern(
                            name = "AWS Access Key ID",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:aws.{0,20})?(?:access.{0,20})?key.{0,20}[\"'`]?(AKIA[0-9A-Z]{16})[\"'`]?"
                                    ),
                            description = "AWS Access Key ID starting with AKIA",
                            severity = Severity.CRITICAL,
                            provider = "AWS",
                            examples = listOf("AKIAIOSFODNN7EXAMPLE")
                    ),
                    ApiKeyPattern(
                            name = "AWS Secret Access Key",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:aws.{0,20})?(?:secret.{0,20})?(?:access.{0,20})?key.{0,20}[\"'`]?([A-Za-z0-9/+=]{40})[\"'`]?"
                                    ),
                            description = "AWS Secret Access Key (40 characters)",
                            severity = Severity.CRITICAL,
                            provider = "AWS",
                            examples = listOf("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                    ),
                    ApiKeyPattern(
                            name = "AWS Session Token",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:aws.{0,20})?(?:session.{0,20})?token.{0,20}[\"'`]?([A-Za-z0-9/+=]{100,})[\"'`]?"
                                    ),
                            description = "AWS Session Token (temporary credentials)",
                            severity = Severity.HIGH,
                            provider = "AWS"
                    )
            )

    /** Google Cloud Platform API Keys */
    private val googlePatterns =
            listOf(
                    ApiKeyPattern(
                            name = "Google API Key",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:google.{0,20})?(?:api.{0,20})?key.{0,20}[\"'`]?(AIza[0-9A-Za-z\\-_]{35})[\"'`]?"
                                    ),
                            description = "Google API Key starting with AIza",
                            severity = Severity.HIGH,
                            provider = "Google",
                            examples = listOf("AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI")
                    ),
                    ApiKeyPattern(
                            name = "Google OAuth 2.0 Client ID",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:google.{0,20})?(?:client.{0,20})?id.{0,20}[\"'`]?([0-9]+-[0-9A-Za-z_]{32}\\.apps\\.googleusercontent\\.com)[\"'`]?"
                                    ),
                            description = "Google OAuth 2.0 Client ID",
                            severity = Severity.HIGH,
                            provider = "Google"
                    ),
                    ApiKeyPattern(
                            name = "Google OAuth 2.0 Client Secret",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:google.{0,20})?(?:client.{0,20})?secret.{0,20}[\"'`]?([A-Za-z0-9\\-_]{24})[\"'`]?"
                                    ),
                            description = "Google OAuth 2.0 Client Secret",
                            severity = Severity.CRITICAL,
                            provider = "Google"
                    )
            )

    /** Microsoft Azure API Keys */
    private val azurePatterns =
            listOf(
                    ApiKeyPattern(
                            name = "Azure Subscription Key",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:azure.{0,20})?(?:subscription.{0,20})?key.{0,20}[\"'`]?([a-f0-9]{32})[\"'`]?"
                                    ),
                            description = "Azure Subscription Key (32 hex characters)",
                            severity = Severity.HIGH,
                            provider = "Azure"
                    ),
                    ApiKeyPattern(
                            name = "Azure Client Secret",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:azure.{0,20})?(?:client.{0,20})?secret.{0,20}[\"'`]?([A-Za-z0-9\\-_~]{34,44})[\"'`]?"
                                    ),
                            description = "Azure Application Client Secret",
                            severity = Severity.CRITICAL,
                            provider = "Azure"
                    )
            )

    /** GitHub API Tokens */
    private val githubPatterns =
            listOf(
                    ApiKeyPattern(
                            name = "GitHub Personal Access Token",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:github.{0,20})?(?:token|pat).{0,20}[\"'`]?(ghp_[A-Za-z0-9]{36})[\"'`]?"
                                    ),
                            description = "GitHub Personal Access Token starting with ghp_",
                            severity = Severity.HIGH,
                            provider = "GitHub",
                            examples = listOf("ghp_1234567890abcdef1234567890abcdef12345678")
                    ),
                    ApiKeyPattern(
                            name = "GitHub OAuth App Token",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:github.{0,20})?(?:oauth.{0,20})?(?:token|access).{0,20}[\"'`]?(gho_[A-Za-z0-9]{36})[\"'`]?"
                                    ),
                            description = "GitHub OAuth App Token starting with gho_",
                            severity = Severity.HIGH,
                            provider = "GitHub"
                    ),
                    ApiKeyPattern(
                            name = "GitHub App Installation Token",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:github.{0,20})?(?:installation.{0,20})?token.{0,20}[\"'`]?(ghs_[A-Za-z0-9]{36})[\"'`]?"
                                    ),
                            description = "GitHub App Installation Token starting with ghs_",
                            severity = Severity.HIGH,
                            provider = "GitHub"
                    ),
                    ApiKeyPattern(
                            name = "GitHub Refresh Token",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:github.{0,20})?(?:refresh.{0,20})?token.{0,20}[\"'`]?(ghr_[A-Za-z0-9]{76})[\"'`]?"
                                    ),
                            description = "GitHub Refresh Token starting with ghr_",
                            severity = Severity.MEDIUM,
                            provider = "GitHub"
                    )
            )

    /** Slack API Tokens */
    private val slackPatterns =
            listOf(
                    ApiKeyPattern(
                            name = "Slack Bot Token",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:slack.{0,20})?(?:bot.{0,20})?token.{0,20}[\"'`]?(xoxb-[0-9]{11,13}-[0-9]{11,13}-[A-Za-z0-9]{24})[\"'`]?"
                                    ),
                            description = "Slack Bot User OAuth Token starting with xoxb-",
                            severity = Severity.HIGH,
                            provider = "Slack"
                    ),
                    ApiKeyPattern(
                            name = "Slack User Token",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:slack.{0,20})?(?:user.{0,20})?token.{0,20}[\"'`]?(xoxp-[0-9]{11,13}-[0-9]{11,13}-[0-9]{11,13}-[A-Za-z0-9]{32})[\"'`]?"
                                    ),
                            description = "Slack User OAuth Token starting with xoxp-",
                            severity = Severity.CRITICAL,
                            provider = "Slack"
                    ),
                    ApiKeyPattern(
                            name = "Slack Webhook URL",
                            pattern =
                                    Pattern.compile(
                                            "https://hooks\\.slack\\.com/services/[A-Z0-9]{9}/[A-Z0-9]{9}/[A-Za-z0-9]{24}"
                                    ),
                            description = "Slack Incoming Webhook URL",
                            severity = Severity.MEDIUM,
                            provider = "Slack"
                    )
            )

    /** Stripe API Keys */
    private val stripePatterns =
            listOf(
                    ApiKeyPattern(
                            name = "Stripe Live Secret Key",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:stripe.{0,20})?(?:secret.{0,20})?key.{0,20}[\"'`]?(sk_live_[0-9a-zA-Z]{24})[\"'`]?"
                                    ),
                            description = "Stripe Live Secret Key starting with sk_live_",
                            severity = Severity.CRITICAL,
                            provider = "Stripe"
                    ),
                    ApiKeyPattern(
                            name = "Stripe Test Secret Key",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:stripe.{0,20})?(?:secret.{0,20})?key.{0,20}[\"'`]?(sk_test_[0-9a-zA-Z]{24})[\"'`]?"
                                    ),
                            description = "Stripe Test Secret Key starting with sk_test_",
                            severity = Severity.MEDIUM,
                            provider = "Stripe"
                    ),
                    ApiKeyPattern(
                            name = "Stripe Publishable Key",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:stripe.{0,20})?(?:publishable.{0,20})?key.{0,20}[\"'`]?(pk_(?:live|test)_[0-9a-zA-Z]{24})[\"'`]?"
                                    ),
                            description =
                                    "Stripe Publishable Key starting with pk_live_ or pk_test_",
                            severity = Severity.LOW,
                            provider = "Stripe"
                    )
            )

    /** Twitter API Keys */
    private val twitterPatterns =
            listOf(
                    ApiKeyPattern(
                            name = "Twitter Access Token",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:twitter.{0,20})?(?:access.{0,20})?token.{0,20}[\"'`]?([0-9]{15,20}-[A-Za-z0-9]{20,40})[\"'`]?"
                                    ),
                            description = "Twitter Access Token",
                            severity = Severity.HIGH,
                            provider = "Twitter"
                    ),
                    ApiKeyPattern(
                            name = "Twitter API Key",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:twitter.{0,20})?(?:api.{0,20})?key.{0,20}[\"'`]?([A-Za-z0-9]{25})[\"'`]?"
                                    ),
                            description = "Twitter API Key (25 characters)",
                            severity = Severity.HIGH,
                            provider = "Twitter"
                    ),
                    ApiKeyPattern(
                            name = "Twitter API Secret",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:twitter.{0,20})?(?:api.{0,20})?secret.{0,20}[\"'`]?([A-Za-z0-9]{50})[\"'`]?"
                                    ),
                            description = "Twitter API Secret Key (50 characters)",
                            severity = Severity.CRITICAL,
                            provider = "Twitter"
                    )
            )

    /** Additional Service Patterns */
    private val miscPatterns =
            listOf(
                    ApiKeyPattern(
                            name = "Twilio Account SID",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:twilio.{0,20})?(?:account.{0,20})?sid.{0,20}[\"'`]?(AC[a-f0-9]{32})[\"'`]?"
                                    ),
                            description = "Twilio Account SID starting with AC",
                            severity = Severity.HIGH,
                            provider = "Twilio"
                    ),
                    ApiKeyPattern(
                            name = "Twilio Auth Token",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:twilio.{0,20})?(?:auth.{0,20})?token.{0,20}[\"'`]?([a-f0-9]{32})[\"'`]?"
                                    ),
                            description = "Twilio Auth Token (32 hex characters)",
                            severity = Severity.CRITICAL,
                            provider = "Twilio"
                    ),
                    ApiKeyPattern(
                            name = "SendGrid API Key",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:sendgrid.{0,20})?(?:api.{0,20})?key.{0,20}[\"'`]?(SG\\.[A-Za-z0-9\\-_]{22}\\.[A-Za-z0-9\\-_]{43})[\"'`]?"
                                    ),
                            description = "SendGrid API Key starting with SG.",
                            severity = Severity.HIGH,
                            provider = "SendGrid"
                    ),
                    ApiKeyPattern(
                            name = "Mailgun API Key",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:mailgun.{0,20})?(?:api.{0,20})?key.{0,20}[\"'`]?(key-[a-f0-9]{32})[\"'`]?"
                                    ),
                            description = "Mailgun API Key starting with key-",
                            severity = Severity.HIGH,
                            provider = "Mailgun"
                    ),
                    ApiKeyPattern(
                            name = "Firebase API Key",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:firebase.{0,20})?(?:api.{0,20})?key.{0,20}[\"'`]?([A-Za-z0-9\\-_]{39})[\"'`]?"
                                    ),
                            description = "Firebase API Key (39 characters)",
                            severity = Severity.HIGH,
                            provider = "Firebase"
                    ),
                    ApiKeyPattern(
                            name = "Heroku API Key",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:heroku.{0,20})?(?:api.{0,20})?key.{0,20}[\"'`]?([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})[\"'`]?"
                                    ),
                            description = "Heroku API Key (UUID format)",
                            severity = Severity.HIGH,
                            provider = "Heroku"
                    ),
                    ApiKeyPattern(
                            name = "DigitalOcean Token",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:digitalocean.{0,20})?(?:token|key).{0,20}[\"'`]?([a-f0-9]{64})[\"'`]?"
                                    ),
                            description = "DigitalOcean Personal Access Token (64 hex characters)",
                            severity = Severity.HIGH,
                            provider = "DigitalOcean"
                    )
            )

    /** Generic API Key Patterns (lower confidence) */
    private val genericPatterns =
            listOf(
                    ApiKeyPattern(
                            name = "Generic API Key",
                            pattern =
                                    Pattern.compile(
                                            "(?i)(?:api.{0,20})?key.{0,20}[\"'`]?([A-Za-z0-9\\-_]{20,50})[\"'`]?"
                                    ),
                            description = "Generic API key pattern (20-50 alphanumeric characters)",
                            severity = Severity.LOW,
                            provider = "Generic"
                    ),
                    ApiKeyPattern(
                            name = "Bearer Token",
                            pattern = Pattern.compile("(?i)bearer\\s+([A-Za-z0-9\\-_=]{20,250})"),
                            description = "Bearer token in Authorization header",
                            severity = Severity.MEDIUM,
                            provider = "Generic"
                    ),
                    ApiKeyPattern(
                            name = "Basic Auth Credentials",
                            pattern = Pattern.compile("(?i)basic\\s+([A-Za-z0-9+/=]{20,250})"),
                            description = "Basic authentication credentials (base64 encoded)",
                            severity = Severity.HIGH,
                            provider = "Generic"
                    )
            )

    /** Get all API key patterns grouped by provider */
    fun getAllPatterns(): Map<String, List<ApiKeyPattern>> {
        return mapOf(
                "AWS" to awsPatterns,
                "Google" to googlePatterns,
                "Azure" to azurePatterns,
                "GitHub" to githubPatterns,
                "Slack" to slackPatterns,
                "Stripe" to stripePatterns,
                "Twitter" to twitterPatterns,
                "Miscellaneous" to miscPatterns,
                "Generic" to genericPatterns
        )
    }

    /** Get all patterns as a flat list */
    fun getAllPatternsFlat(): List<ApiKeyPattern> {
        return getAllPatterns().values.flatten()
    }

    /** Get patterns by provider */
    fun getPatternsByProvider(provider: String): List<ApiKeyPattern> {
        return getAllPatterns()[provider] ?: emptyList()
    }

    /** Get patterns by severity level */
    fun getPatternsBySeverity(severity: Severity): List<ApiKeyPattern> {
        return getAllPatternsFlat().filter { it.severity == severity }
    }

    /** Get high-confidence patterns (CRITICAL and HIGH severity) */
    fun getHighConfidencePatterns(): List<ApiKeyPattern> {
        return getAllPatternsFlat().filter {
            it.severity == Severity.CRITICAL || it.severity == Severity.HIGH
        }
    }

    /** Check if a string matches any API key pattern */
    fun matchesAnyPattern(input: String): List<ApiKeyPattern> {
        return getAllPatternsFlat().filter { pattern -> pattern.pattern.matcher(input).find() }
    }

    /** Extract all API key matches from a string */
    fun extractMatches(input: String): List<Pair<ApiKeyPattern, String>> {
        val matches = mutableListOf<Pair<ApiKeyPattern, String>>()

        getAllPatternsFlat().forEach { pattern ->
            val matcher = pattern.pattern.matcher(input)
            while (matcher.find()) {
                val match =
                        if (matcher.groupCount() > 0) {
                            matcher.group(1) // Use first capture group if available
                        } else {
                            matcher.group(0) // Use entire match
                        }
                matches.add(pattern to match)
            }
        }

        return matches
    }

    /** Validate if a potential API key matches expected format */
    fun validateApiKeyFormat(potentialKey: String, provider: String): Boolean {
        val providerPatterns = getPatternsByProvider(provider)
        return providerPatterns.any { pattern -> pattern.pattern.matcher(potentialKey).matches() }
    }

    /** Get pattern statistics */
    fun getPatternStatistics(): Map<String, Any> {
        val allPatterns = getAllPatternsFlat()
        return mapOf(
                "totalPatterns" to allPatterns.size,
                "providerCount" to getAllPatterns().keys.size,
                "severityDistribution" to
                        allPatterns.groupBy { it.severity }.mapValues { it.value.size },
                "providerDistribution" to
                        allPatterns.groupBy { it.provider }.mapValues { it.value.size }
        )
    }
}
