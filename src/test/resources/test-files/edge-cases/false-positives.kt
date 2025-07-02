package com.example.myapp.edgecases

import java.security.SecureRandom
import java.util.*
import kotlin.random.Random
import org.springframework.stereotype.Component

/**
 * This file contains code patterns that might trigger false positives in secret detection systems
 * but are actually legitimate code. Used for testing the SCAN plugin's ability to avoid false
 * positives.
 */
@Component
class FalsePositivesTestClass {

    // Example API-like strings that are NOT actual API keys
    companion object {
        // These look like API keys but are examples/documentation
        const val EXAMPLE_API_KEY_FORMAT = "sk-1234567890abcdef1234567890abcdef"
        const val DEMO_TOKEN_PATTERN = "ghp_1234567890abcdef1234567890abcdef12345678"
        const val SAMPLE_AWS_KEY_FORMAT = "AKIA1234567890ABCDEF"

        // These are legitimate constants that might look suspicious
        const val ENCRYPTION_ALGORITHM = "AES-256-GCM"
        const val JWT_ALGORITHM = "HS256"
        const val HASH_ALGORITHM = "SHA-256"

        // Base64 encoded strings that are NOT secrets
        const val SAMPLE_BASE64_DATA =
                "VGhpcyBpcyBqdXN0IGEgdGVzdCBzdHJpbmc=" // "This is just a test string"
        const val EXAMPLE_JWT_HEADER =
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" // {"alg":"HS256","typ":"JWT"}

        // UUIDs that might look like secrets
        const val EXAMPLE_UUID = "12345678-1234-5678-9012-123456789012"
        const val SAMPLE_GUID = "a1b2c3d4-e5f6-7890-1234-567890abcdef"

        // Database connection strings that are obviously fake
        const val EXAMPLE_DB_URL = "jdbc:postgresql://localhost:5432/testdb"
        const val SAMPLE_REDIS_URL = "redis://localhost:6379/0"

        // Configuration keys that look like secrets but are just property names
        const val SECRET_KEY_PROPERTY = "app.security.secret.key"
        const val API_KEY_CONFIG = "external.service.api.key"
        const val PASSWORD_FIELD = "database.password"
    }

    /** Function that generates random strings that look like secrets but aren't */
    fun generateRandomIds(): List<String> {
        val random = SecureRandom()
        return listOf(
                // Generate random hex strings (common in IDs, not secrets)
                generateRandomHex(32),
                generateRandomHex(64),

                // Generate random base64 strings (common in tokens, but these are random)
                generateRandomBase64(32),
                generateRandomBase64(64),

                // Generate UUIDs
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString().replace("-", "")
        )
    }

    /** Function that creates test data with high entropy strings */
    fun createTestData(): Map<String, String> {
        return mapOf(
                // These are legitimate test data, not secrets
                "sessionId" to "sess_1234567890abcdef1234567890abcdef",
                "requestId" to "req_abcdef1234567890abcdef1234567890",
                "transactionId" to "txn_${Random.nextLong()}",
                "correlationId" to UUID.randomUUID().toString(),

                // Mock external service responses
                "mockApiResponse" to """{"token": "mock_token_1234567890", "expires": 3600}""",
                "sampleOAuthResponse" to """{"access_token": "sample_access_token_abcdef123456"}""",

                // Test configuration values
                "encryptionKeyId" to "key_id_12345678",
                "certificateThumbprint" to "1234567890abcdef1234567890abcdef12345678",

                // Legitimate hash values (not secrets)
                "fileChecksum" to
                        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "dataHash" to "d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2"
        )
    }

    /** Function demonstrating legitimate cryptographic operations */
    fun demonstrateCryptoOperations() {
        // These are legitimate crypto operations, not secret storage
        val keyGenerator = javax.crypto.KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        val secretKey = keyGenerator.generateKey()

        // The encoded key here is generated, not a hardcoded secret
        val encodedKey = Base64.getEncoder().encodeToString(secretKey.encoded)
        println("Generated key (not a secret): $encodedKey")

        // Example of legitimate key derivation
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltString = Base64.getEncoder().encodeToString(salt)
        println("Generated salt (not a secret): $saltString")
    }

    /** Function with legitimate environment variable and system property access */
    fun getSystemConfiguration(): Map<String, String?> {
        return mapOf(
                // These are legitimate system properties, not secrets
                "javaVersion" to System.getProperty("java.version"),
                "osName" to System.getProperty("os.name"),
                "userHome" to System.getProperty("user.home"),
                "tempDir" to System.getProperty("java.io.tmpdir"),

                // These environment variables are typically non-sensitive
                "path" to System.getenv("PATH"),
                "home" to System.getenv("HOME"),
                "user" to System.getenv("USER"),

                // Placeholder for actual config (not the secret itself)
                "secretKeyPath" to System.getenv("SECRET_KEY_PATH"), // Path to secret, not secret
                "configFile" to System.getenv("CONFIG_FILE_PATH") // Path to config, not secret
        )
    }

    /** Function demonstrating test tokens and mock data */
    fun createMockAuthData(): AuthData {
        return AuthData(
                // These are obviously test/mock tokens
                accessToken = "test_access_token_1234567890abcdef",
                refreshToken = "test_refresh_token_abcdef1234567890",
                apiKey = "test_api_key_${System.currentTimeMillis()}",

                // Mock user data
                userId = "test_user_${Random.nextInt(1000, 9999)}",
                sessionId = "test_session_${UUID.randomUUID().toString().take(8)}",

                // Legitimate configuration references
                tokenEndpoint = "https://auth.example.com/oauth/token",
                apiEndpoint = "https://api.example.com/v1"
        )
    }

    /** Function with legitimate JWT token handling (not containing real secrets) */
    fun processJwtToken(token: String): Map<String, Any> {
        // Example JWT processing - these are legitimate operations
        val parts = token.split(".")

        if (parts.size != 3) {
            throw IllegalArgumentException("Invalid JWT format")
        }

        // These are legitimate JWT parts, not secrets being exposed
        val header = parts[0] // JWT header
        val payload = parts[1] // JWT payload
        val signature = parts[2] // JWT signature (verification, not secret)

        return mapOf(
                "header" to header,
                "payload" to payload,
                "signature" to signature,
                "isValid" to verifySignature(signature) // Mock verification
        )
    }

    /** Mock signature verification (not exposing secrets) */
    private fun verifySignature(signature: String): Boolean {
        // This is mock verification logic, not secret handling
        return signature.isNotEmpty() && signature.length > 10
    }

    /** Function demonstrating legitimate password hashing (not storing plaintext) */
    fun hashPassword(plainPassword: String): String {
        // This is legitimate password hashing, not secret storage
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        // In real code, this would use proper password hashing like bcrypt
        val hashedPassword =
                "${plainPassword}_hashed_with_salt_${Base64.getEncoder().encodeToString(salt)}"

        // Return the hash, not the original password
        return hashedPassword
    }

    /** Function with legitimate database connection handling */
    fun createDatabaseConnection(): String {
        // These are legitimate connection string construction, not secrets
        val host = "localhost"
        val port = 5432
        val database = "testdb"
        val username = "testuser"

        // Note: In real code, password would come from secure config, not hardcoded
        // This is just demonstrating connection string format
        return "jdbc:postgresql://$host:$port/$database?user=$username"
    }

    /** Function demonstrating legitimate external service configuration */
    fun configureExternalServices(): Map<String, String> {
        return mapOf(
                // These are legitimate service configurations
                "paymentService" to "https://api.stripe.com/v1",
                "emailService" to "https://api.sendgrid.com/v3",
                "storageService" to "https://s3.amazonaws.com",

                // These are configuration keys, not the actual secrets
                "paymentApiKeyRef" to "PAYMENT_API_KEY",
                "emailApiKeyRef" to "EMAIL_API_KEY",
                "storageKeyRef" to "STORAGE_ACCESS_KEY",

                // These are legitimate public endpoints
                "webhookUrl" to "https://myapp.com/webhooks/payment",
                "callbackUrl" to "https://myapp.com/auth/callback"
        )
    }

    /** Utility function for generating random hex strings */
    private fun generateRandomHex(length: Int): String {
        val chars = "0123456789abcdef"
        return (1..length).map { chars.random() }.joinToString("")
    }

    /** Utility function for generating random base64 strings */
    private fun generateRandomBase64(length: Int): String {
        val bytes = ByteArray(length)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }
}

/** Data class for mock authentication data */
data class AuthData(
        val accessToken: String,
        val refreshToken: String,
        val apiKey: String,
        val userId: String,
        val sessionId: String,
        val tokenEndpoint: String,
        val apiEndpoint: String
)

/** Example enum with values that might look like secrets */
enum class ConfigKeys(val key: String) {
    API_KEY("api_key_placeholder"),
    SECRET_KEY("secret_key_placeholder"),
    DATABASE_PASSWORD("db_password_placeholder"),
    JWT_SECRET("jwt_secret_placeholder"),
    ENCRYPTION_KEY("encryption_key_placeholder")
}

/** Example annotation that might contain suspicious-looking values */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiKeyRequired(
        val keyName: String = "default_api_key",
        val example: String = "example_api_key_1234567890abcdef"
)

/** Example class demonstrating legitimate use of Base64 encoding */
class Base64Examples {

    // These are legitimate Base64 operations, not secret storage
    fun encodeUserData(userData: String): String {
        return Base64.getEncoder().encodeToString(userData.toByteArray())
    }

    fun decodeUserData(encodedData: String): String {
        return String(Base64.getDecoder().decode(encodedData))
    }

    // Example of legitimate Base64 constants (not secrets)
    companion object {
        const val SAMPLE_ENCODED_JSON =
                "eyJ1c2VyIjoidGVzdCIsInJvbGUiOiJ1c2VyIn0=" // {"user":"test","role":"user"}
        const val EXAMPLE_ENCODED_TEXT = "SGVsbG8gV29ybGQ=" // "Hello World"
    }
}
