package com.scan.patterns

import java.util.regex.Pattern

/**
 * Comprehensive collection of regex patterns for detecting various types of secrets and sensitive
 * information. This class serves as the central repository for all secret detection patterns used
 * by the SCAN plugin.
 *
 * Pattern categories include:
 * - API Keys and Tokens
 * - Database Connection Strings
 * - Cryptographic Keys and Certificates
 * - Cloud Provider Credentials
 * - Generic High-Entropy Strings
 * - Authentication Tokens
 * - Webhook URLs and Secrets
 */
object SecretPatterns {

    /** Data class representing a secret pattern with metadata */
    data class SecretPattern(
        val name: String,
        val pattern: Pattern,
        val description: String,
        val severity: Severity = Severity.HIGH,
        val category: Category,
        val examples: List<String> = emptyList(),
        val falsePositiveIndicators: List<String> = emptyList()
    ) {
        enum class Severity {
            LOW,
            MEDIUM,
            HIGH,
            CRITICAL
        }
        enum class Category {
            API_KEY,
            DATABASE,
            CRYPTO,
            CLOUD,
            GENERIC,
            AUTH,
            WEBHOOK,
            PASSWORD,
            CERTIFICATE
        }
    }

    // =================================
    // API Keys and Service Tokens
    // =================================

    private val AWS_ACCESS_KEY =
        SecretPattern(
            name = "AWS Access Key",
            pattern =
            Pattern.compile(
                "(?i)(?:aws_access_key_id|aws_access_key|access_key_id)\\s*[=:]\\s*['\"]?([A-Z0-9]{20})['\"]?",
                Pattern.MULTILINE
            ),
            description = "AWS Access Key ID",
            severity = SecretPattern.Severity.CRITICAL,
            category = SecretPattern.Category.CLOUD,
            examples = listOf("aws_access_key_id=AKIAIOSFODNN7EXAMPLE"),
            falsePositiveIndicators = listOf("EXAMPLE", "DUMMY", "TEST", "PLACEHOLDER")
        )

    private val AWS_SECRET_KEY =
        SecretPattern(
            name = "AWS Secret Key",
            pattern =
            Pattern.compile(
                "(?i)(?:aws_secret_access_key|secret_access_key|secret_key)\\s*[=:]\\s*['\"]?([A-Za-z0-9/+=]{40})['\"]?",
                Pattern.MULTILINE
            ),
            description = "AWS Secret Access Key",
            severity = SecretPattern.Severity.CRITICAL,
            category = SecretPattern.Category.CLOUD,
            examples =
            listOf(
                "aws_secret_access_key=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
            ),
            falsePositiveIndicators = listOf("EXAMPLE", "DUMMY", "TEST", "PLACEHOLDER")
        )

    private val GITHUB_TOKEN =
        SecretPattern(
            name = "GitHub Token",
            pattern =
            Pattern.compile(
                "(?i)(?:github_token|gh_token|github_pat)\\s*[=:]\\s*['\"]?(gh[pousr]_[A-Za-z0-9_]{36,255})['\"]?",
                Pattern.MULTILINE
            ),
            description = "GitHub Personal Access Token",
            severity = SecretPattern.Severity.HIGH,
            category = SecretPattern.Category.API_KEY,
            examples = listOf("github_token=ghp_1234567890abcdef1234567890abcdef12345678"),
            falsePositiveIndicators = listOf("example", "dummy", "test", "placeholder")
        )

    private val SLACK_TOKEN =
        SecretPattern(
            name = "Slack Token",
            pattern =
            Pattern.compile(
                "(?i)(?:slack_token|slack_api_token)\\s*[=:]\\s*['\"]?(xox[baprs]-[0-9a-zA-Z-]+)['\"]?",
                Pattern.MULTILINE
            ),
            description = "Slack API Token",
            severity = SecretPattern.Severity.HIGH,
            category = SecretPattern.Category.API_KEY,
            examples =
            listOf("slack_token=xoxb-123456789-123456789-abcdefghijklmnopqrstuvwx"),
            falsePositiveIndicators = listOf("example", "dummy", "test", "placeholder")
        )

    private val GOOGLE_API_KEY =
        SecretPattern(
            name = "Google API Key",
            pattern =
            Pattern.compile(
                "(?i)(?:google_api_key|google_key|gcp_api_key)\\s*[=:]\\s*['\"]?(AIza[0-9A-Za-z\\-_]{35})['\"]?",
                Pattern.MULTILINE
            ),
            description = "Google Cloud Platform API Key",
            severity = SecretPattern.Severity.HIGH,
            category = SecretPattern.Category.CLOUD,
            examples = listOf("google_api_key=AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI"),
            falsePositiveIndicators = listOf("example", "dummy", "test", "placeholder")
        )

    private val STRIPE_API_KEY =
        SecretPattern(
            name = "Stripe API Key",
            pattern =
            Pattern.compile(
                "(?i)(?:stripe_api_key|stripe_key)\\s*[=:]\\s*['\"]?(sk_(?:test_|live_)?[0-9a-zA-Z]{24,})['\"]?",
                Pattern.MULTILINE
            ),
            description = "Stripe API Secret Key",
            severity = SecretPattern.Severity.CRITICAL,
            category = SecretPattern.Category.API_KEY,
            examples = listOf("stripe_api_key=sk_test_4eC39HqLyjWDarjtT1zdp7dc"),
            falsePositiveIndicators = listOf("example", "dummy", "test", "placeholder")
        )

    // =================================
    // Database Connection Strings
    // =================================

    private val MYSQL_CONNECTION =
        SecretPattern(
            name = "MySQL Connection String",
            pattern =
            Pattern.compile(
                "(?i)(?:mysql|mariadb)://[^\\s:]+:[^\\s@]+@[^\\s/]+(?:/[^\\s?]+)?(?:\\?[^\\s]*)?",
                Pattern.MULTILINE
            ),
            description = "MySQL/MariaDB connection string with credentials",
            severity = SecretPattern.Severity.HIGH,
            category = SecretPattern.Category.DATABASE,
            examples = listOf("mysql://user:password@localhost:3306/database"),
            falsePositiveIndicators =
            listOf("example", "dummy", "test", "placeholder", "password", "user")
        )

    private val POSTGRESQL_CONNECTION =
        SecretPattern(
            name = "PostgreSQL Connection String",
            pattern =
            Pattern.compile(
                "(?i)(?:postgresql|postgres)://[^\\s:]+:[^\\s@]+@[^\\s/]+(?:/[^\\s?]+)?(?:\\?[^\\s]*)?",
                Pattern.MULTILINE
            ),
            description = "PostgreSQL connection string with credentials",
            severity = SecretPattern.Severity.HIGH,
            category = SecretPattern.Category.DATABASE,
            examples = listOf("postgresql://user:password@localhost:5432/database"),
            falsePositiveIndicators =
            listOf("example", "dummy", "test", "placeholder", "password", "user")
        )

    private val MONGODB_CONNECTION =
        SecretPattern(
            name = "MongoDB Connection String",
            pattern =
            Pattern.compile(
                "(?i)mongodb(?:\\+srv)?://[^\\s:]+:[^\\s@]+@[^\\s/]+(?:/[^\\s?]+)?(?:\\?[^\\s]*)?",
                Pattern.MULTILINE
            ),
            description = "MongoDB connection string with credentials",
            severity = SecretPattern.Severity.HIGH,
            category = SecretPattern.Category.DATABASE,
            examples = listOf("mongodb://user:password@cluster0.mongodb.net/database"),
            falsePositiveIndicators =
            listOf("example", "dummy", "test", "placeholder", "password", "user")
        )

    private val REDIS_CONNECTION =
        SecretPattern(
            name = "Redis Connection String",
            pattern =
            Pattern.compile(
                "(?i)redis://[^\\s:]+:[^\\s@]+@[^\\s/]+(?:/[0-9]+)?",
                Pattern.MULTILINE
            ),
            description = "Redis connection string with credentials",
            severity = SecretPattern.Severity.MEDIUM,
            category = SecretPattern.Category.DATABASE,
            examples = listOf("redis://user:password@localhost:6379/0"),
            falsePositiveIndicators =
            listOf("example", "dummy", "test", "placeholder", "password", "user")
        )

    // =================================
    // Cryptographic Keys and Certificates
    // =================================

    private val RSA_PRIVATE_KEY =
        SecretPattern(
            name = "RSA Private Key",
            pattern =
            Pattern.compile(
                "-----BEGIN\\s+(?:RSA\\s+)?PRIVATE\\s+KEY-----[\\s\\S]*?-----END\\s+(?:RSA\\s+)?PRIVATE\\s+KEY-----",
                Pattern.MULTILINE or Pattern.DOTALL
            ),
            description = "RSA Private Key in PEM format",
            severity = SecretPattern.Severity.CRITICAL,
            category = SecretPattern.Category.CRYPTO,
            examples =
            listOf(
                "-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEA...\n-----END RSA PRIVATE KEY-----"
            ),
            falsePositiveIndicators = listOf("example", "dummy", "test", "placeholder")
        )

    private val OPENSSH_PRIVATE_KEY =
        SecretPattern(
            name = "OpenSSH Private Key",
            pattern =
            Pattern.compile(
                "-----BEGIN\\s+OPENSSH\\s+PRIVATE\\s+KEY-----[\\s\\S]*?-----END\\s+OPENSSH\\s+PRIVATE\\s+KEY-----",
                Pattern.MULTILINE or Pattern.DOTALL
            ),
            description = "OpenSSH Private Key in PEM format",
            severity = SecretPattern.Severity.CRITICAL,
            category = SecretPattern.Category.CRYPTO,
            examples =
            listOf(
                "-----BEGIN OPENSSH PRIVATE KEY-----\nb3BlbnNzaC1rZXktdjE...\n-----END OPENSSH PRIVATE KEY-----"
            ),
            falsePositiveIndicators = listOf("example", "dummy", "test", "placeholder")
        )

    private val ECDSA_PRIVATE_KEY =
        SecretPattern(
            name = "ECDSA Private Key",
            pattern =
            Pattern.compile(
                "-----BEGIN\\s+EC\\s+PRIVATE\\s+KEY-----[\\s\\S]*?-----END\\s+EC\\s+PRIVATE\\s+KEY-----",
                Pattern.MULTILINE or Pattern.DOTALL
            ),
            description = "ECDSA Private Key in PEM format",
            severity = SecretPattern.Severity.CRITICAL,
            category = SecretPattern.Category.CRYPTO,
            examples =
            listOf(
                "-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEI...\n-----END EC PRIVATE KEY-----"
            ),
            falsePositiveIndicators = listOf("example", "dummy", "test", "placeholder")
        )

    private val X509_CERTIFICATE =
        SecretPattern(
            name = "X.509 Certificate",
            pattern =
            Pattern.compile(
                "-----BEGIN\\s+CERTIFICATE-----[\\s\\S]*?-----END\\s+CERTIFICATE-----",
                Pattern.MULTILINE or Pattern.DOTALL
            ),
            description = "X.509 Certificate in PEM format",
            severity = SecretPattern.Severity.MEDIUM,
            category = SecretPattern.Category.CERTIFICATE,
            examples =
            listOf(
                "-----BEGIN CERTIFICATE-----\nMIIDXTCCAkWgAwIBAgIJAKoK...\n-----END CERTIFICATE-----"
            ),
            falsePositiveIndicators = listOf("example", "dummy", "test", "placeholder")
        )

    // =================================
    // JWT Tokens
    // =================================

    private val JWT_TOKEN =
        SecretPattern(
            name = "JWT Token",
            pattern =
            Pattern.compile(
                "(?i)(?:jwt|token|bearer)\\s*[=:]\\s*['\"]?(eyJ[A-Za-z0-9_-]+\\.eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+)['\"]?",
                Pattern.MULTILINE
            ),
            description = "JSON Web Token",
            severity = SecretPattern.Severity.HIGH,
            category = SecretPattern.Category.AUTH,
            examples =
            listOf(
                "jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
            ),
            falsePositiveIndicators = listOf("example", "dummy", "test", "placeholder")
        )

    // =================================
    // Generic High-Entropy Patterns
    // =================================

    private val GENERIC_API_KEY =
        SecretPattern(
            name = "Generic API Key",
            pattern =
            Pattern.compile(
                "(?i)(?:api_key|apikey|api-key|key|secret|token|auth|authorization)\\s*[=:]\\s*['\"]?([A-Za-z0-9_-]{32,})['\"]?",
                Pattern.MULTILINE
            ),
            description = "Generic API key or secret with high entropy",
            severity = SecretPattern.Severity.MEDIUM,
            category = SecretPattern.Category.GENERIC,
            examples = listOf("api_key=abc123def456ghi789jkl012mno345pqr678"),
            falsePositiveIndicators =
            listOf(
                "example",
                "dummy",
                "test",
                "placeholder",
                "your",
                "key",
                "here",
                "secret"
            )
        )

    private val GENERIC_PASSWORD =
        SecretPattern(
            name = "Generic Password",
            pattern =
            Pattern.compile(
                "(?i)(?:password|passwd|pwd|pass)\\s*[=:]\\s*['\"]?([^\\s'\",;]{8,})['\"]?",
                Pattern.MULTILINE
            ),
            description = "Generic password field",
            severity = SecretPattern.Severity.MEDIUM,
            category = SecretPattern.Category.PASSWORD,
            examples = listOf("password=mysecretpassword123"),
            falsePositiveIndicators =
            listOf(
                "example",
                "dummy",
                "test",
                "placeholder",
                "your",
                "password",
                "here",
                "secret"
            )
        )

    private val BASE64_ENCODED_SECRET =
        SecretPattern(
            name = "Base64 Encoded Secret",
            pattern =
            Pattern.compile(
                "(?i)(?:secret|key|token|auth)\\s*[=:]\\s*['\"]?([A-Za-z0-9+/]{40,}={0,2})['\"]?",
                Pattern.MULTILINE
            ),
            description = "Base64 encoded secret or key",
            severity = SecretPattern.Severity.MEDIUM,
            category = SecretPattern.Category.GENERIC,
            examples = listOf("secret=dGhpc2lzYXNlY3JldGtleWZvcnRlc3RpbmdwdXJwb3Nlcw=="),
            falsePositiveIndicators = listOf("example", "dummy", "test", "placeholder")
        )

    // =================================
    // Cloud Provider Specific
    // =================================

    private val AZURE_CLIENT_SECRET =
        SecretPattern(
            name = "Azure Client Secret",
            pattern =
            Pattern.compile(
                "(?i)azure_client_secret\\s*[=:]\\s*['\"]?([A-Za-z0-9~._-]{34,})['\"]?",
                Pattern.MULTILINE
            ),
            description = "Azure Application Client Secret",
            severity = SecretPattern.Severity.HIGH,
            category = SecretPattern.Category.CLOUD,
            examples =
            listOf("azure_client_secret=8Q~abcdefghijklmnopqrstuvwxyz1234567890AB"),
            falsePositiveIndicators = listOf("example", "dummy", "test", "placeholder")
        )

    private val GCP_SERVICE_ACCOUNT_KEY =
        SecretPattern(
            name = "GCP Service Account Key",
            pattern =
            Pattern.compile(
                "\"type\"\\s*:\\s*\"service_account\"[\\s\\S]*?\"private_key\"\\s*:\\s*\"[^\"]*\"",
                Pattern.MULTILINE or Pattern.DOTALL
            ),
            description = "Google Cloud Platform Service Account Key JSON",
            severity = SecretPattern.Severity.CRITICAL,
            category = SecretPattern.Category.CLOUD,
            examples =
            listOf(
                "{\"type\": \"service_account\", \"private_key\": \"-----BEGIN PRIVATE KEY-----\\n...\\n-----END PRIVATE KEY-----\\n\"}"
            ),
            falsePositiveIndicators = listOf("example", "dummy", "test", "placeholder")
        )

    // =================================
    // Webhook and API Endpoints
    // =================================

    private val WEBHOOK_URL =
        SecretPattern(
            name = "Webhook URL",
            pattern =
            Pattern.compile(
                "(?i)(?:webhook|hook)\\s*[=:]\\s*['\"]?(https?://[^\\s'\"]+)['\"]?",
                Pattern.MULTILINE
            ),
            description = "Webhook URL endpoint",
            severity = SecretPattern.Severity.LOW,
            category = SecretPattern.Category.WEBHOOK,
            examples =
            listOf(
                "webhook=https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX"
            ),
            falsePositiveIndicators =
            listOf(
                "example",
                "dummy",
                "test",
                "placeholder",
                "localhost",
                "127.0.0.1"
            )
        )

    private val DISCORD_WEBHOOK =
        SecretPattern(
            name = "Discord Webhook",
            pattern =
            Pattern.compile(
                "(?i)discord\\s*[=:]\\s*['\"]?(https://discord(?:app)?\\.com/api/webhooks/[0-9]+/[A-Za-z0-9_-]+)['\"]?",
                Pattern.MULTILINE
            ),
            description = "Discord Webhook URL",
            severity = SecretPattern.Severity.MEDIUM,
            category = SecretPattern.Category.WEBHOOK,
            examples =
            listOf(
                "discord=https://discord.com/api/webhooks/123456789012345678/abcdefghijklmnopqrstuvwxyz1234567890ABCDEFGHIJKLMN"
            ),
            falsePositiveIndicators = listOf("example", "dummy", "test", "placeholder")
        )

    // =================================
    // All Patterns Collection
    // =================================

    /** Complete list of all secret patterns available for detection */
    val ALL_PATTERNS: List<SecretPattern> =
        listOf(
            // API Keys and Tokens
            AWS_ACCESS_KEY,
            AWS_SECRET_KEY,
            GITHUB_TOKEN,
            SLACK_TOKEN,
            GOOGLE_API_KEY,
            STRIPE_API_KEY,

            // Database Connections
            MYSQL_CONNECTION,
            POSTGRESQL_CONNECTION,
            MONGODB_CONNECTION,
            REDIS_CONNECTION,

            // Cryptographic Keys
            RSA_PRIVATE_KEY,
            OPENSSH_PRIVATE_KEY,
            ECDSA_PRIVATE_KEY,
            X509_CERTIFICATE,

            // JWT Tokens
            JWT_TOKEN,

            // Generic Patterns
            GENERIC_API_KEY,
            GENERIC_PASSWORD,
            BASE64_ENCODED_SECRET,

            // Cloud Provider Specific
            AZURE_CLIENT_SECRET,
            GCP_SERVICE_ACCOUNT_KEY,

            // Webhooks
            WEBHOOK_URL,
            DISCORD_WEBHOOK
        )

    /** Get patterns filtered by category */
    fun getPatternsByCategory(category: SecretPattern.Category): List<SecretPattern> {
        return ALL_PATTERNS.filter { it.category == category }
    }

    /** Get patterns filtered by severity level */
    fun getPatternsBySeverity(severity: SecretPattern.Severity): List<SecretPattern> {
        return ALL_PATTERNS.filter { it.severity == severity }
    }

    /** Get patterns filtered by minimum severity level */
    fun getPatternsWithMinimumSeverity(minSeverity: SecretPattern.Severity): List<SecretPattern> {
        val severityOrder =
            listOf(
                SecretPattern.Severity.LOW,
                SecretPattern.Severity.MEDIUM,
                SecretPattern.Severity.HIGH,
                SecretPattern.Severity.CRITICAL
            )
        val minIndex = severityOrder.indexOf(minSeverity)
        return ALL_PATTERNS.filter { severityOrder.indexOf(it.severity) >= minIndex }
    }

    /** Get pattern by name */
    fun getPatternByName(name: String): SecretPattern? {
        return ALL_PATTERNS.find { it.name.equals(name, ignoreCase = true) }
    }

    /** Get all pattern names */
    fun getAllPatternNames(): List<String> {
        return ALL_PATTERNS.map { it.name }
    }

    /** Get all categories */
    fun getAllCategories(): List<SecretPattern.Category> {
        return ALL_PATTERNS.map { it.category }.distinct()
    }

    /** Check if a potential match is likely a false positive based on common indicators */
    fun isLikelyFalsePositive(pattern: SecretPattern, match: String): Boolean {
        val lowerMatch = match.lowercase()
        return pattern.falsePositiveIndicators.any { indicator ->
            lowerMatch.contains(indicator.lowercase())
        }
    }

    /** Get all patterns as a map grouped by name */
    fun getAllPatterns(): Map<String, List<SecretPattern>> {
        return ALL_PATTERNS.groupBy { it.name }
    }

    /** Create a custom pattern for project-specific secrets */
    fun createCustomPattern(
        name: String,
        regex: String,
        description: String,
        severity: SecretPattern.Severity = SecretPattern.Severity.MEDIUM,
        category: SecretPattern.Category = SecretPattern.Category.GENERIC,
        examples: List<String> = emptyList(),
        falsePositiveIndicators: List<String> = emptyList()
    ): SecretPattern {
        return SecretPattern(
            name = name,
            pattern = Pattern.compile(regex, Pattern.MULTILINE),
            description = description,
            severity = severity,
            category = category,
            examples = examples,
            falsePositiveIndicators = falsePositiveIndicators
        )
    }
}
