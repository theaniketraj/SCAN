package com.scan.patterns

import java.util.regex.Pattern

/**
 * Comprehensive collection of cryptographic key and certificate patterns. This class provides regex
 * patterns to detect various types of cryptographic materials including private keys, certificates,
 * and cryptographic tokens.
 */
object CryptoPatterns {

    /** Data class representing a cryptographic pattern with metadata */
    data class CryptoPattern(
        val name: String,
        val pattern: Pattern,
        val description: String,
        val severity: Severity = Severity.CRITICAL,
        val keyType: KeyType,
        val algorithm: String? = null,
        val keySize: String? = null,
        val examples: List<String> = emptyList()
    )

    /** Severity levels for detected cryptographic materials */
    enum class Severity {
        CRITICAL, // Private keys, secrets that must never be exposed
        HIGH, // Certificates, public keys that shouldn't be in code
        MEDIUM, // Key fingerprints, hashes
        LOW // Potentially false positives
    }

    /** Types of cryptographic keys and materials */
    enum class KeyType {
        PRIVATE_KEY,
        PUBLIC_KEY,
        CERTIFICATE,
        SYMMETRIC_KEY,
        HASH,
        SIGNATURE,
        TOKEN,
        KEYSTORE
    }

    /** RSA Private Key Patterns */
    private val rsaPrivateKeyPatterns =
        listOf(
            CryptoPattern(
                name = "RSA Private Key (PEM)",
                pattern =
                Pattern.compile(
                    "-----BEGIN RSA PRIVATE KEY-----[\\s\\S]*?-----END RSA PRIVATE KEY-----",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "RSA private key in PEM format",
                severity = Severity.CRITICAL,
                keyType = KeyType.PRIVATE_KEY,
                algorithm = "RSA",
                examples =
                listOf(
                    "-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEA...\n-----END RSA PRIVATE KEY-----"
                )
            ),
            CryptoPattern(
                name = "RSA Private Key (PKCS#8)",
                pattern =
                Pattern.compile(
                    "-----BEGIN PRIVATE KEY-----[\\s\\S]*?-----END PRIVATE KEY-----",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "RSA private key in PKCS#8 PEM format",
                severity = Severity.CRITICAL,
                keyType = KeyType.PRIVATE_KEY,
                algorithm = "RSA",
                examples =
                listOf(
                    "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BA...\n-----END PRIVATE KEY-----"
                )
            ),
            CryptoPattern(
                name = "Encrypted RSA Private Key",
                pattern =
                Pattern.compile(
                    "-----BEGIN ENCRYPTED PRIVATE KEY-----[\\s\\S]*?-----END ENCRYPTED PRIVATE KEY-----",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "Encrypted RSA private key in PEM format",
                severity = Severity.HIGH,
                keyType = KeyType.PRIVATE_KEY,
                algorithm = "RSA"
            )
        )

    /** DSA Private Key Patterns */
    private val dsaPrivateKeyPatterns =
        listOf(
            CryptoPattern(
                name = "DSA Private Key",
                pattern =
                Pattern.compile(
                    "-----BEGIN DSA PRIVATE KEY-----[\\s\\S]*?-----END DSA PRIVATE KEY-----",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "DSA private key in PEM format",
                severity = Severity.CRITICAL,
                keyType = KeyType.PRIVATE_KEY,
                algorithm = "DSA"
            )
        )

    /** Elliptic Curve Private Key Patterns */
    private val ecPrivateKeyPatterns =
        listOf(
            CryptoPattern(
                name = "EC Private Key",
                pattern =
                Pattern.compile(
                    "-----BEGIN EC PRIVATE KEY-----[\\s\\S]*?-----END EC PRIVATE KEY-----",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "Elliptic Curve private key in PEM format",
                severity = Severity.CRITICAL,
                keyType = KeyType.PRIVATE_KEY,
                algorithm = "EC"
            ),
            CryptoPattern(
                name = "ECDSA Private Key",
                pattern =
                Pattern.compile(
                    "-----BEGIN ECDSA PRIVATE KEY-----[\\s\\S]*?-----END ECDSA PRIVATE KEY-----",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "ECDSA private key in PEM format",
                severity = Severity.CRITICAL,
                keyType = KeyType.PRIVATE_KEY,
                algorithm = "ECDSA"
            )
        )

    /** SSH Key Patterns */
    private val sshKeyPatterns =
        listOf(
            CryptoPattern(
                name = "SSH RSA Private Key",
                pattern =
                Pattern.compile(
                    "-----BEGIN OPENSSH PRIVATE KEY-----[\\s\\S]*?-----END OPENSSH PRIVATE KEY-----",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "OpenSSH private key format",
                severity = Severity.CRITICAL,
                keyType = KeyType.PRIVATE_KEY,
                algorithm = "SSH"
            ),
            CryptoPattern(
                name = "SSH RSA Public Key",
                pattern =
                Pattern.compile(
                    "ssh-rsa\\s+[A-Za-z0-9+/=]{100,}(?:\\s+[\\w@.-]+)?",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "SSH RSA public key",
                severity = Severity.HIGH,
                keyType = KeyType.PUBLIC_KEY,
                algorithm = "SSH-RSA",
                examples =
                listOf(
                    "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQ... user@hostname"
                )
            ),
            CryptoPattern(
                name = "SSH DSS Public Key",
                pattern =
                Pattern.compile(
                    "ssh-dss\\s+[A-Za-z0-9+/=]{100,}(?:\\s+[\\w@.-]+)?",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "SSH DSS public key",
                severity = Severity.HIGH,
                keyType = KeyType.PUBLIC_KEY,
                algorithm = "SSH-DSS"
            ),
            CryptoPattern(
                name = "SSH ECDSA Public Key",
                pattern =
                Pattern.compile(
                    "ecdsa-sha2-nistp(?:256|384|521)\\s+[A-Za-z0-9+/=]{100,}(?:\\s+[\\w@.-]+)?",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "SSH ECDSA public key",
                severity = Severity.HIGH,
                keyType = KeyType.PUBLIC_KEY,
                algorithm = "SSH-ECDSA"
            ),
            CryptoPattern(
                name = "SSH Ed25519 Public Key",
                pattern =
                Pattern.compile(
                    "ssh-ed25519\\s+[A-Za-z0-9+/=]{68}(?:\\s+[\\w@.-]+)?",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "SSH Ed25519 public key",
                severity = Severity.HIGH,
                keyType = KeyType.PUBLIC_KEY,
                algorithm = "Ed25519"
            )
        )

    /** Certificate Patterns */
    private val certificatePatterns =
        listOf(
            CryptoPattern(
                name = "X.509 Certificate",
                pattern =
                Pattern.compile(
                    "-----BEGIN CERTIFICATE-----[\\s\\S]*?-----END CERTIFICATE-----",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "X.509 certificate in PEM format",
                severity = Severity.HIGH,
                keyType = KeyType.CERTIFICATE,
                algorithm = "X.509"
            ),
            CryptoPattern(
                name = "Certificate Request",
                pattern =
                Pattern.compile(
                    "-----BEGIN CERTIFICATE REQUEST-----[\\s\\S]*?-----END CERTIFICATE REQUEST-----",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "Certificate Signing Request (CSR)",
                severity = Severity.MEDIUM,
                keyType = KeyType.CERTIFICATE,
                algorithm = "CSR"
            ),
            CryptoPattern(
                name = "New Certificate Request",
                pattern =
                Pattern.compile(
                    "-----BEGIN NEW CERTIFICATE REQUEST-----[\\s\\S]*?-----END NEW CERTIFICATE REQUEST-----",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "New Certificate Request format",
                severity = Severity.MEDIUM,
                keyType = KeyType.CERTIFICATE,
                algorithm = "CSR"
            )
        )

    /** PGP/GPG Key Patterns */
    private val pgpKeyPatterns =
        listOf(
            CryptoPattern(
                name = "PGP Private Key Block",
                pattern =
                Pattern.compile(
                    "-----BEGIN PGP PRIVATE KEY BLOCK-----[\\s\\S]*?-----END PGP PRIVATE KEY BLOCK-----",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "PGP private key block",
                severity = Severity.CRITICAL,
                keyType = KeyType.PRIVATE_KEY,
                algorithm = "PGP"
            ),
            CryptoPattern(
                name = "PGP Public Key Block",
                pattern =
                Pattern.compile(
                    "-----BEGIN PGP PUBLIC KEY BLOCK-----[\\s\\S]*?-----END PGP PUBLIC KEY BLOCK-----",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "PGP public key block",
                severity = Severity.HIGH,
                keyType = KeyType.PUBLIC_KEY,
                algorithm = "PGP"
            ),
            CryptoPattern(
                name = "PGP Message",
                pattern =
                Pattern.compile(
                    "-----BEGIN PGP MESSAGE-----[\\s\\S]*?-----END PGP MESSAGE-----",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "PGP encrypted message",
                severity = Severity.MEDIUM,
                keyType = KeyType.TOKEN,
                algorithm = "PGP"
            ),
            CryptoPattern(
                name = "PGP Signature",
                pattern =
                Pattern.compile(
                    "-----BEGIN PGP SIGNATURE-----[\\s\\S]*?-----END PGP SIGNATURE-----",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "PGP signature block",
                severity = Severity.LOW,
                keyType = KeyType.SIGNATURE,
                algorithm = "PGP"
            )
        )

    /** Public Key Patterns */
    private val publicKeyPatterns =
        listOf(
            CryptoPattern(
                name = "RSA Public Key",
                pattern =
                Pattern.compile(
                    "-----BEGIN RSA PUBLIC KEY-----[\\s\\S]*?-----END RSA PUBLIC KEY-----",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "RSA public key in PEM format",
                severity = Severity.HIGH,
                keyType = KeyType.PUBLIC_KEY,
                algorithm = "RSA"
            ),
            CryptoPattern(
                name = "Public Key (PKCS#1)",
                pattern =
                Pattern.compile(
                    "-----BEGIN PUBLIC KEY-----[\\s\\S]*?-----END PUBLIC KEY-----",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "Public key in PKCS#1 PEM format",
                severity = Severity.HIGH,
                keyType = KeyType.PUBLIC_KEY
            )
        )

    /** Symmetric Key and Password Patterns */
    private val symmetricKeyPatterns =
        listOf(
            CryptoPattern(
                name = "AES Key (Base64)",
                pattern =
                Pattern.compile(
                    "(?i)(?:aes.{0,20})?(?:key|secret).{0,20}[\"'`]?([A-Za-z0-9+/=]{22,44})[\"'`]?",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "AES key in Base64 format (128/192/256 bit)",
                severity = Severity.CRITICAL,
                keyType = KeyType.SYMMETRIC_KEY,
                algorithm = "AES"
            ),
            CryptoPattern(
                name = "DES Key (Hex)",
                pattern =
                Pattern.compile(
                    "(?i)(?:des.{0,20})?(?:key|secret).{0,20}[\"'`]?([a-fA-F0-9]{16,48})[\"'`]?",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "DES/3DES key in hexadecimal format",
                severity = Severity.CRITICAL,
                keyType = KeyType.SYMMETRIC_KEY,
                algorithm = "DES"
            )
        )

    /** Hash and Fingerprint Patterns */
    private val hashPatterns =
        listOf(
            CryptoPattern(
                name = "MD5 Hash",
                pattern =
                Pattern.compile(
                    "(?i)(?:md5.{0,20})?(?:hash|sum|digest).{0,20}[\"'`]?([a-fA-F0-9]{32})[\"'`]?",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "MD5 hash (32 hex characters)",
                severity = Severity.LOW,
                keyType = KeyType.HASH,
                algorithm = "MD5"
            ),
            CryptoPattern(
                name = "SHA1 Hash",
                pattern =
                Pattern.compile(
                    "(?i)(?:sha1?.{0,20})?(?:hash|sum|digest|fingerprint).{0,20}[\"'`]?([a-fA-F0-9]{40})[\"'`]?",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "SHA1 hash (40 hex characters)",
                severity = Severity.MEDIUM,
                keyType = KeyType.HASH,
                algorithm = "SHA1"
            ),
            CryptoPattern(
                name = "SHA256 Hash",
                pattern =
                Pattern.compile(
                    "(?i)(?:sha256.{0,20})?(?:hash|sum|digest|fingerprint).{0,20}[\"'`]?([a-fA-F0-9]{64})[\"'`]?",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "SHA256 hash (64 hex characters)",
                severity = Severity.MEDIUM,
                keyType = KeyType.HASH,
                algorithm = "SHA256"
            ),
            CryptoPattern(
                name = "SHA512 Hash",
                pattern =
                Pattern.compile(
                    "(?i)(?:sha512.{0,20})?(?:hash|sum|digest).{0,20}[\"'`]?([a-fA-F0-9]{128})[\"'`]?",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "SHA512 hash (128 hex characters)",
                severity = Severity.MEDIUM,
                keyType = KeyType.HASH,
                algorithm = "SHA512"
            )
        )

    /** Keystore and Wallet Patterns */
    private val keystorePatterns =
        listOf(
            CryptoPattern(
                name = "Java Keystore Password",
                pattern =
                Pattern.compile(
                    "(?i)(?:keystore|jks).{0,20}(?:password|pass|pwd).{0,20}[\"'`]?([A-Za-z0-9!@#$%^&*()_+-=]{6,50})[\"'`]?",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "Java KeyStore password",
                severity = Severity.CRITICAL,
                keyType = KeyType.KEYSTORE,
                algorithm = "JKS"
            ),
            CryptoPattern(
                name = "PKCS#12 Certificate",
                pattern =
                Pattern.compile(
                    "(?i)(?:pkcs12|p12|pfx).{0,20}(?:password|pass).{0,20}[\"'`]?([A-Za-z0-9!@#$%^&*()_+-=]{6,50})[\"'`]?",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "PKCS#12 certificate password",
                severity = Severity.CRITICAL,
                keyType = KeyType.KEYSTORE,
                algorithm = "PKCS12"
            )
        )

    /** Cryptocurrency Patterns */
    private val cryptoWalletPatterns =
        listOf(
            CryptoPattern(
                name = "Bitcoin Private Key (WIF)",
                pattern =
                Pattern.compile(
                    "(?:^|\\s)([5KL][1-9A-HJ-NP-Za-km-z]{50,51})(?:\\s|$)",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "Bitcoin private key in Wallet Import Format",
                severity = Severity.CRITICAL,
                keyType = KeyType.PRIVATE_KEY,
                algorithm = "Bitcoin-WIF"
            ),
            CryptoPattern(
                name = "Ethereum Private Key",
                pattern =
                Pattern.compile(
                    "(?i)(?:ethereum|eth).{0,20}(?:private.{0,10})?key.{0,20}[\"'`]?([a-fA-F0-9]{64})[\"'`]?",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "Ethereum private key (64 hex characters)",
                severity = Severity.CRITICAL,
                keyType = KeyType.PRIVATE_KEY,
                algorithm = "Ethereum"
            ),
            CryptoPattern(
                name = "Cryptocurrency Mnemonic",
                pattern =
                Pattern.compile(
                    "(?i)(?:mnemonic|seed.{0,10}phrase|recovery.{0,10}phrase).{0,20}[\"'`]?([a-z ]{50,200})[\"'`]?",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "Cryptocurrency mnemonic seed phrase",
                severity = Severity.CRITICAL,
                keyType = KeyType.PRIVATE_KEY,
                algorithm = "BIP39"
            )
        )

    /** JSON Web Token Patterns */
    private val jwtPatterns =
        listOf(
            CryptoPattern(
                name = "JSON Web Token (JWT)",
                pattern =
                Pattern.compile(
                    "(?:^|\\s)(eyJ[A-Za-z0-9_-]+\\.eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+)(?:\\s|$)",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "JSON Web Token (JWT)",
                severity = Severity.HIGH,
                keyType = KeyType.TOKEN,
                algorithm = "JWT"
            ),
            CryptoPattern(
                name = "JWT Signing Key",
                pattern =
                Pattern.compile(
                    "(?i)(?:jwt.{0,20})?(?:signing.{0,20})?(?:secret|key).{0,20}[\"'`]?([A-Za-z0-9+/=]{20,100})[\"'`]?",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "JWT signing secret key",
                severity = Severity.CRITICAL,
                keyType = KeyType.SYMMETRIC_KEY,
                algorithm = "JWT"
            )
        )

    /** Generic Cryptographic Patterns */
    private val genericCryptoPatterns =
        listOf(
            CryptoPattern(
                name = "Base64 Encoded Key",
                pattern =
                Pattern.compile(
                    "(?i)(?:key|secret|token).{0,20}[\"'`]?([A-Za-z0-9+/=]{40,200})[\"'`]?",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "Generic Base64 encoded key or secret",
                severity = Severity.MEDIUM,
                keyType = KeyType.SYMMETRIC_KEY
            ),
            CryptoPattern(
                name = "Hexadecimal Key",
                pattern =
                Pattern.compile(
                    "(?i)(?:key|secret|token).{0,20}[\"'`]?([a-fA-F0-9]{32,128})[\"'`]?",
                    Pattern.CASE_INSENSITIVE
                ),
                description = "Generic hexadecimal encoded key",
                severity = Severity.MEDIUM,
                keyType = KeyType.SYMMETRIC_KEY
            )
        )

    /** Get all cryptographic patterns grouped by algorithm/type */
    fun getAllPatterns(): Map<String, List<CryptoPattern>> {
        return mapOf(
            "RSA Private Keys" to rsaPrivateKeyPatterns,
            "DSA Private Keys" to dsaPrivateKeyPatterns,
            "EC Private Keys" to ecPrivateKeyPatterns,
            "SSH Keys" to sshKeyPatterns,
            "Certificates" to certificatePatterns,
            "PGP Keys" to pgpKeyPatterns,
            "Public Keys" to publicKeyPatterns,
            "Symmetric Keys" to symmetricKeyPatterns,
            "Hashes" to hashPatterns,
            "Keystores" to keystorePatterns,
            "Cryptocurrency" to cryptoWalletPatterns,
            "JWT" to jwtPatterns,
            "Generic" to genericCryptoPatterns
        )
    }

    /** Get all patterns as a flat list */
    fun getAllPatternsFlat(): List<CryptoPattern> {
        return getAllPatterns().values.flatten()
    }

    /** Get patterns by key type */
    fun getPatternsByKeyType(keyType: KeyType): List<CryptoPattern> {
        return getAllPatternsFlat().filter { it.keyType == keyType }
    }

    /** Get patterns by severity level */
    fun getPatternsBySeverity(severity: Severity): List<CryptoPattern> {
        return getAllPatternsFlat().filter { it.severity == severity }
    }

    /** Get critical patterns (private keys and secrets) */
    fun getCriticalPatterns(): List<CryptoPattern> {
        return getAllPatternsFlat().filter { it.severity == Severity.CRITICAL }
    }

    /** Get private key patterns only */
    fun getPrivateKeyPatterns(): List<CryptoPattern> {
        return getPatternsByKeyType(KeyType.PRIVATE_KEY)
    }

    /** Get certificate patterns only */
    fun getCertificatePatterns(): List<CryptoPattern> {
        return getPatternsByKeyType(KeyType.CERTIFICATE)
    }

    /** Check if a string matches any cryptographic pattern */
    fun matchesAnyPattern(input: String): List<CryptoPattern> {
        return getAllPatternsFlat().filter { pattern -> pattern.pattern.matcher(input).find() }
    }

    /** Extract all cryptographic matches from a string */
    fun extractMatches(input: String): List<Pair<CryptoPattern, String>> {
        val matches = mutableListOf<Pair<CryptoPattern, String>>()

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

    /** Validate if a potential key matches expected cryptographic format */
    fun validateCryptoFormat(potentialKey: String, algorithm: String): Boolean {
        val algorithmPatterns =
            getAllPatternsFlat().filter {
                it.algorithm?.equals(algorithm, ignoreCase = true) == true
            }
        return algorithmPatterns.any { pattern -> pattern.pattern.matcher(potentialKey).matches() }
    }

    /** Get patterns by algorithm */
    fun getPatternsByAlgorithm(algorithm: String): List<CryptoPattern> {
        return getAllPatternsFlat().filter {
            it.algorithm?.equals(algorithm, ignoreCase = true) == true
        }
    }

    /** Get pattern statistics */
    fun getPatternStatistics(): Map<String, Any> {
        val allPatterns = getAllPatternsFlat()
        return mapOf(
            "totalPatterns" to allPatterns.size,
            "keyTypeDistribution" to
                allPatterns.groupBy { it.keyType }.mapValues { it.value.size },
            "severityDistribution" to
                allPatterns.groupBy { it.severity }.mapValues { it.value.size },
            "algorithmDistribution" to
                allPatterns.mapNotNull { it.algorithm }.groupBy { it }.mapValues {
                    it.value.size
                },
            "criticalCount" to allPatterns.count { it.severity == Severity.CRITICAL },
            "privateKeyCount" to allPatterns.count { it.keyType == KeyType.PRIVATE_KEY }
        )
    }

    /** Check if input contains PEM-formatted content */
    fun containsPemContent(input: String): Boolean {
        val pemPattern =
            Pattern.compile(
                "-----BEGIN [A-Z ]+-----[\\s\\S]*?-----END [A-Z ]+-----",
                Pattern.CASE_INSENSITIVE
            )
        return pemPattern.matcher(input).find()
    }

    /** Extract PEM blocks from input */
    fun extractPemBlocks(input: String): List<String> {
        val pemPattern =
            Pattern.compile(
                "-----BEGIN [A-Z ]+-----[\\s\\S]*?-----END [A-Z ]+-----",
                Pattern.CASE_INSENSITIVE
            )
        val matcher = pemPattern.matcher(input)
        val blocks = mutableListOf<String>()

        while (matcher.find()) {
            blocks.add(matcher.group(0))
        }

        return blocks
    }

    /** Get high-risk patterns that should never be in source code */
    fun getHighRiskPatterns(): List<CryptoPattern> {
        return getAllPatternsFlat().filter { pattern ->
            pattern.severity == Severity.CRITICAL &&
                (pattern.keyType == KeyType.PRIVATE_KEY ||
                    pattern.keyType == KeyType.SYMMETRIC_KEY)
        }
    }
}
