package com.scan.core

/**
 * Types of secrets that can be detected
 */
enum class SecretType {
    API_KEY,
    DATABASE_URL,
    PRIVATE_KEY,
    PASSWORD,
    TOKEN,
    CERTIFICATE,
    CRYPTO_KEY,
    AWS_KEY,
    GOOGLE_KEY,
    GITHUB_TOKEN,
    SLACK_TOKEN,
    JWT_TOKEN,
    SSH_KEY,
    RSA_KEY,
    UNKNOWN;

    companion object {
        fun fromString(value: String): SecretType? {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                when (value.lowercase()) {
                    "api_key", "apikey" -> API_KEY
                    "database_url", "db_url" -> DATABASE_URL
                    "private_key", "privatekey" -> PRIVATE_KEY
                    "crypto_key", "cryptokey" -> CRYPTO_KEY
                    "aws_key", "awskey" -> AWS_KEY
                    "google_key", "googlekey" -> GOOGLE_KEY
                    "github_token", "githubtoken" -> GITHUB_TOKEN
                    "slack_token", "slacktoken" -> SLACK_TOKEN
                    "jwt_token", "jwttoken" -> JWT_TOKEN
                    "ssh_key", "sshkey" -> SSH_KEY
                    "rsa_key", "rsakey" -> RSA_KEY
                    else -> UNKNOWN
                }
            }
        }
    }
}
