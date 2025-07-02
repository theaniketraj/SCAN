package com.example.testapp

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Test file containing various API keys and secrets for testing the SCAN plugin detection
 * capabilities. This file should be detected by the security scanner.
 */
@Component
@ConfigurationProperties(prefix = "api")
class ApiConfiguration {

    // AWS API Keys - Should be detected
    val awsAccessKeyId = "AKIAIOSFODNN7EXAMPLE"
    val awsSecretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"

    // Google API Key - Should be detected
    val googleApiKey = "AIzaSyDGQ4YhdEm8VN6X9hE8yXz7qBkEXAMPLE"

    // GitHub Personal Access Token - Should be detected
    val githubToken = "ghp_1234567890abcdefghijklmnopqrstuvwxyzABC"

    // Slack Bot Token - Should be detected
    val slackBotToken = "xoxb-123456789012-1234567890123-abcdefghijklmnopqrstuvwx"

    // Stripe API Key - Should be detected
    val stripeSecretKey = "sk_test_1234567890abcdefghijklmnopqrstuvwxyz"
    val stripePublishableKey = "pk_test_1234567890abcdefghijklmnopqrstuvwxyz"

    // JWT Secret - Should be detected
    val jwtSecret = "supersecretjwtkey12345"

    // Database password in connection string - Should be detected
    val databaseUrl = "jdbc:postgresql://localhost:5432/mydb?user=admin&password=secretpassword123"

    // API key in URL - Should be detected
    val apiEndpoint = "https://api.example.com/v1/data?api_key=abc123def456ghi789jkl"

    // SSH Private Key - Should be detected
    val sshPrivateKey =
            """
        -----BEGIN OPENSSH PRIVATE KEY-----
        b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAlwAAAAdzc2gtcn
        NhAAAAAwEAAQAAAIEA1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOP
        QRSTUVWXYZ1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVW
        XYZ1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ123456
        7890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ=
        -----END OPENSSH PRIVATE KEY-----
    """.trimIndent()

    // Firebase Config - Should be detected
    val firebaseConfig =
            mapOf(
                    "apiKey" to "AIzaSyB1234567890abcdefghijklmnopqrstuvwx",
                    "authDomain" to "myapp.firebaseapp.com",
                    "projectId" to "myapp-12345",
                    "storageBucket" to "myapp-12345.appspot.com",
                    "messagingSenderId" to "123456789012",
                    "appId" to "1:123456789012:web:abc123def456ghi789"
            )

    // Azure Storage Account Key - Should be detected
    val azureStorageAccountKey =
            "DefaultEndpointsProtocol=https;AccountName=mystorageaccount;AccountKey=abc123def456ghi789jklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ==;EndpointSuffix=core.windows.net"

    // Twilio API Credentials - Should be detected
    val twilioAccountSid = "AC1234567890abcdefghijklmnopqrstuvwx"
    val twilioAuthToken = "abcdef1234567890ghijklmnopqrstuvwxyz"

    // SendGrid API Key - Should be detected
    val sendGridApiKey =
            "SG.abcdefghijklmnopqrstuvwxyz.1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnop"

    // PayPal Client Credentials - Should be detected
    val paypalClientId = "AYSq3RDGsmBLJE-otTkBtM-jBRd1TCQwFf9RGfwddNXWz0uFU9ztymylOhRS"
    val paypalClientSecret = "EGnHDxD_qRPdaLdHCKiZ8NrRQwCCgLuQTuq_-YHCqLmMJxPNJiDH_8NqH8LY"

    // MongoDB Connection String with credentials - Should be detected
    val mongoConnectionString =
            "mongodb://admin:password123@cluster0.mongodb.net:27017/myapp?retryWrites=true&w=majority"

    // Redis URL with password - Should be detected
    val redisUrl = "redis://:password123456@redis-server:6379/0"

    // OAuth Client Secret - Should be detected
    val oauthClientSecret = "client_secret_1234567890abcdefghijklmnopqrstuvwxyz"

    // Webhook Secret - Should be detected
    val webhookSecret = "whsec_1234567890abcdefghijklmnopqrstuvwxyzABCDEF"

    // API Key in different formats - Should be detected
    val apiKeyVariations =
            listOf(
                    "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                    "Basic YWRtaW46cGFzc3dvcmQxMjM=",
                    "Token ghp_1234567890abcdefghijklmnopqrstuvwxyzABC"
            )

    // Credit Card Number (PCI data) - Should be detected
    val testCreditCard = "4532015112830366" // Test Visa card number

    // Social Security Number - Should be detected
    val ssn = "123-45-6789"

    // Email with password in comment - Should be detected
    // TODO: Remove before production - email: admin@example.com, password: TempPassword123!

    // Hardcoded encryption key - Should be detected
    val encryptionKey = "aes256key1234567890abcdefghijklmn"

    // License key - Should be detected
    val softwareLicenseKey = "ABCD-EFGH-IJKL-MNOP-QRST-UVWX-YZ12-3456"

    // FTP credentials - Should be detected
    val ftpUrl = "ftp://username:password123@ftp.example.com:21/path"

    // SMTP credentials - Should be detected
    val smtpConfig =
            mapOf(
                    "host" to "smtp.gmail.com",
                    "port" to 587,
                    "username" to "myapp@gmail.com",
                    "password" to "app_password_16_chars"
            )
}

/**
 * Configuration loaded from environment or properties These should also be detected if they contain
 * secrets
 */
object SecretConfiguration {

    // Environment variables that might contain secrets
    val secretFromEnv = System.getenv("SECRET_API_KEY") ?: "fallback_secret_key_12345"

    // Property with embedded secret
    const val EMBEDDED_SECRET = "sk_live_51234567890abcdefghijklmnopqrstuvwxyz"

    // Base64 encoded secret - Should be detected
    val encodedSecret =
            "c2VjcmV0X2FwaV9rZXlfMTIzNDU2Nzg5MA==" // base64 encoded "secret_api_key_1234567890"

    // Hex encoded secret - Should be detected
    val hexSecret = "736563726574617069" // hex encoded "secretapi"

    // URL with API key parameter - Should be detected
    fun buildApiUrl(endpoint: String): String {
        return "https://api.service.com/$endpoint?token=abc123def456ghi789&format=json"
    }

    // Method that exposes secret in logs - Should be detected
    fun authenticateUser(apiKey: String = "default_secret_key_xyz789") {
        println("Authenticating with key: $apiKey") // This exposes the secret
        // Authentication logic here
    }
}

/** Data class that might accidentally expose secrets */
data class UserCredentials(
        val username: String = "admin",
        val password: String = "SuperSecretPassword123!", // Should be detected
        val apiToken: String = "token_1234567890abcdefghijklmn" // Should be detected
)

/** Enum with secret values */
enum class ServiceCredentials(val key: String) {
    DEVELOPMENT("dev_key_1234567890abcdef"), // Should be detected
    STAGING("staging_key_abcdef1234567890"), // Should be detected
    PRODUCTION("prod_key_xyz789abc123def456") // Should be detected
}

/** Extension function that might leak secrets */
fun String.withApiKey(): String {
    return "$this?api_key=secret_extension_key_123456" // Should be detected
}

/** Companion object with secrets */
class ApiClient {
    companion object {
        const val DEFAULT_API_KEY = "companion_secret_789xyz" // Should be detected
        private const val INTERNAL_SECRET = "internal_key_abc123def456" // Should be detected

        // Even private constants should be detected
        private val secretMap =
                mapOf(
                        "primary" to "primary_secret_key_123",
                        "secondary" to "secondary_secret_key_456"
                )
    }
}

/** Annotation with secret value */
@Target(AnnotationTarget.CLASS)
annotation class SecretAnnotation(val value: String = "annotation_secret_xyz123")

/** Lambda with embedded secret */
val secretLambda = { input: String ->
    val secret = "lambda_secret_abc789" // Should be detected
    "$input-$secret"
}

// Top-level property with secret
private const val GLOBAL_SECRET = "global_api_key_123456789" // Should be detected
