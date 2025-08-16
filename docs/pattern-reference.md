# SCAN Plugin Pattern Reference

This document provides a comprehensive reference for all built-in patterns used by the SCAN plugin and guidance on creating custom patterns.

## Table of Contents

1. [Pattern Overview](#pattern-overview)
2. [Built-in Patterns](#built-in-patterns)
3. [Pattern Categories](#pattern-categories)
4. [Custom Pattern Creation](#custom-pattern-creation)
5. [Pattern Testing](#pattern-testing)
6. [Best Practices](#best-practices)

## Pattern Overview

SCAN uses regular expressions (regex) to identify potential secrets in your codebase. The plugin includes over 50 built-in patterns covering common secret types, and you can add custom patterns for organization-specific secrets.

### How Patterns Work

1. **Pattern Matching**: SCAN scans each file line by line, applying regex patterns
2. **Context Analysis**: The surrounding code context is analyzed to reduce false positives
3. **Entropy Analysis**: High-entropy strings are flagged even if they don't match specific patterns
4. **Confidence Scoring**: Each finding is assigned a confidence level based on multiple factors

## Built-in Patterns

### Cloud Provider Credentials

#### AWS Patterns

**AWS Access Key ID**

```regex
AKIA[0-9A-Z]{16}
```

- **Description**: AWS Access Key identifiers
- **Example**: `AKIAIOSFODNN7EXAMPLE`
- **Confidence**: High

**AWS Secret Access Key**

```regex
aws(.{0,20})?['\"][0-9a-zA-Z\/+]{40}['\"]
```

- **Description**: AWS Secret Access Keys
- **Example**: `aws_secret_access_key="wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"`
- **Confidence**: High

**AWS Session Token**

```regex
aws(.{0,20})?session(.{0,20})?token
```

- **Description**: AWS session tokens
- **Confidence**: Medium

#### Google Cloud Platform (GCP)

**GCP API Key**

```regex
AIza[0-9A-Za-z\\-_]{35}
```

- **Description**: Google Cloud Platform API keys
- **Example**: `AIzaSyDaGmWKa4JsXZ-HjGw7ISLn_3namBGewQe`
- **Confidence**: High

**GCP Service Account Key**

```regex
"type": "service_account"
```

- **Description**: GCP service account JSON key files
- **Confidence**: High (when in JSON context)

#### Azure

**Azure Storage Account Key**

```regex
DefaultEndpointsProtocol=https;AccountName=.*;AccountKey=.*
```

- **Description**: Azure storage connection strings
- **Confidence**: High

**Azure Client Secret**

```regex
azure(.{0,20})?client(.{0,20})?secret
```

- **Description**: Azure application client secrets
- **Confidence**: Medium

### Version Control Systems

#### GitHub

**GitHub Personal Access Token**

```regex
ghp_[A-Za-z0-9]{36}
```

- **Description**: GitHub personal access tokens (new format)
- **Example**: `ghp_1234567890abcdef1234567890abcdef12345678`
- **Confidence**: High

**GitHub OAuth Token**

```regex
gho_[A-Za-z0-9]{36}
```

- **Description**: GitHub OAuth access tokens
- **Confidence**: High

**GitHub App Token**

```regex
ghs_[A-Za-z0-9]{36}
```

- **Description**: GitHub App installation access tokens
- **Confidence**: High

**GitHub Refresh Token**

```regex
ghr_[A-Za-z0-9]{76}
```

- **Description**: GitHub refresh tokens
- **Confidence**: High

#### GitLab

**GitLab Personal Access Token**

```regex
glpat-[A-Za-z0-9\\-_]{20}
```

- **Description**: GitLab personal access tokens
- **Confidence**: High

#### Bitbucket

**Bitbucket App Password**

```regex
bitbucket(.{0,20})?app(.{0,20})?password
```

- **Description**: Bitbucket app passwords
- **Confidence**: Medium

### Database Credentials

#### Generic Database URLs

**Database Connection String**

```regex
(mysql|postgresql|mongodb|redis)://[^\\s]+:[^\\s]+@[^\\s]+
```

- **Description**: Database connection strings with credentials
- **Example**: `mysql://user:password@localhost:3306/database`
- **Confidence**: High

**JDBC URL with Credentials**

```regex
jdbc:[^\\s]+user=[^\\s]+.*password=[^\\s]+
```

- **Description**: JDBC connection strings
- **Confidence**: High

#### Specific Database Types

**MySQL**

```regex
mysql(.{0,20})?password['"\\s]*[:=]['"\\s]*[^\\s'"]+
```

- **Description**: MySQL password configurations
- **Confidence**: Medium

**PostgreSQL**

```regex
postgres(.{0,20})?password['"\\s]*[:=]['"\\s]*[^\\s'"]+
```

- **Description**: PostgreSQL password configurations
- **Confidence**: Medium

**MongoDB**

```regex
mongodb(.{0,20})?password['"\\s]*[:=]['"\\s]*[^\\s'"]+
```

- **Description**: MongoDB password configurations
- **Confidence**: Medium

### API Keys and Tokens

#### Generic API Patterns

**Generic API Key**

```regex
api(.{0,20})?key['"\\s]*[:=]['"\\s]*[A-Za-z0-9]{20,}
```

- **Description**: Generic API key patterns
- **Confidence**: Medium

**API Secret**

```regex
api(.{0,20})?secret['"\\s]*[:=]['"\\s]*[A-Za-z0-9]{20,}
```

- **Description**: Generic API secret patterns
- **Confidence**: Medium

**Bearer Token**

```regex
bearer\\s+[A-Za-z0-9\\-_\\.]+
```

- **Description**: Bearer authentication tokens
- **Confidence**: Medium

#### Service-Specific APIs

**Slack Token**

```regex
xox[baprs]-[A-Za-z0-9]{10,48}
```

- **Description**: Slack API tokens
- **Example**: `xoxb-1234567890-1234567890-abcdefghijklmnopqrstuvwx`
- **Confidence**: High

**Discord Bot Token**

```regex
[MN][A-Za-z\\d]{23}\\.[\\w-]{6}\\.[\\w-]{27}
```

- **Description**: Discord bot tokens
- **Confidence**: High

**Stripe API Key**

```regex
sk_live_[A-Za-z0-9]{24}
```

- **Description**: Stripe live API keys
- **Confidence**: High

**Twilio API Key**

```regex
SK[a-z0-9]{32}
```

- **Description**: Twilio API keys
- **Confidence**: High

### Cryptographic Keys

#### Private Keys

**RSA Private Key**

```regex
-----BEGIN RSA PRIVATE KEY-----
```

- **Description**: RSA private key headers
- **Confidence**: High

**EC Private Key**

```regex
-----BEGIN EC PRIVATE KEY-----
```

- **Description**: Elliptic Curve private key headers
- **Confidence**: High

**OpenSSH Private Key**

```regex
-----BEGIN OPENSSH PRIVATE KEY-----
```

- **Description**: OpenSSH private key headers
- **Confidence**: High

#### Certificates

**Certificate**

```regex
-----BEGIN CERTIFICATE-----
```

- **Description**: X.509 certificate headers
- **Confidence**: Medium (certificates are often public)

### Passwords and Secrets

#### Generic Password Patterns

**Password Assignment**

```regex
password['"\\s]*[:=]['"\\s]*[^\\s'"]{6,}
```

- **Description**: Generic password assignments
- **Confidence**: Low (many false positives)

**Secret Assignment**

```regex
secret['"\\s]*[:=]['"\\s]*[^\\s'"]{8,}
```

- **Description**: Generic secret assignments
- **Confidence**: Low

#### Specific Password Types

**Basic Auth**

```regex
Basic\\s+[A-Za-z0-9+/]+=*
```

- **Description**: HTTP Basic Authentication headers
- **Confidence**: Medium

**JWT Token**

```regex
eyJ[A-Za-z0-9\\-_=]+\\.[A-Za-z0-9\\-_=]+\\.[A-Za-z0-9\\-_.+/=]*
```

- **Description**: JSON Web Tokens
- **Confidence**: Medium

## Pattern Categories

### High Confidence Patterns

These patterns have very low false positive rates:

- Cloud provider keys (AWS, GCP, Azure)
- Version control tokens (GitHub, GitLab)
- Service-specific API keys (Slack, Stripe, etc.)
- Private key headers
- Database connection strings with credentials

### Medium Confidence Patterns

These patterns may have some false positives but are generally reliable:

- Generic API keys/secrets
- Bearer tokens
- JWT tokens
- Basic auth headers

### Low Confidence Patterns

These patterns are more prone to false positives but catch common mistakes:

- Generic password assignments
- Generic secret assignments
- Configuration file patterns

## Custom Pattern Creation

### Basic Custom Patterns

Add custom patterns to catch organization-specific secrets:

```kotlin
scan {
    customPatterns = listOf(
        "MYCOMPANY_API_[A-Z0-9]{32}",
        "INTERNAL_SECRET_[a-f0-9]{64}",
        "PROD_KEY_[A-Za-z0-9\\-_]{40}"
    )
}
```

### Advanced Custom Patterns

#### Context-Aware Patterns

```kotlin
scan {
    customPatterns = listOf(
        // Match passwords in specific file types
        "(?i)password\\s*[=:]\\s*[\"'][^\"']{8,}[\"']",
        
        // Match API keys with specific prefixes
        "(?:api[_-]?key|apikey)\\s*[=:]\\s*[\"']?([A-Za-z0-9]{20,})[\"']?",
        
        // Match secrets in environment variable format
        "^[A-Z_]+_SECRET=[A-Za-z0-9+/=]{20,}$"
    )
}
```

#### Organization-Specific Patterns

```kotlin
scan {
    customPatterns = listOf(
        // Company-specific API key format
        "ACME_[A-Z]{2}_[0-9]{8}_[A-Za-z0-9]{16}",
        
        // Internal service tokens
        "svc_[a-z]{3,10}_[A-Za-z0-9]{32}",
        
        // Database identifiers
        "db_prod_[a-f0-9]{40}",
        
        // Certificate thumbprints
        "cert_[A-F0-9]{40}",
        
        // License keys
        "lic_[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}"
    )
}
```

### Pattern Optimization

#### Performance Considerations

```kotlin
scan {
    customPatterns = listOf(
        // ✅ Good: Specific, efficient pattern
        "API_KEY_[A-Z0-9]{32}",
        
        // ❌ Avoid: Too broad, slow
        ".*secret.*",
        
        // ✅ Good: Anchored pattern
        "^SECRET=[A-Za-z0-9]{20,}$",
        
        // ❌ Avoid: Unanchored, may cause backtracking
        "(secret|password|key).*[A-Za-z0-9]+.*"
    )
}
```

## Pattern Testing

### Testing Custom Patterns

Create test files to validate your custom patterns:

```kotlin
// test-secrets.kt (for testing only)
val testCases = listOf(
    "MYCOMPANY_API_12345678901234567890123456789012", // Should match
    "MYCOMPANY_API_short",                             // Should not match
    "OTHER_API_12345678901234567890123456789012",      // Should not match
    "MYCOMPANY_API_12345678901234567890123456789"      // Should not match (wrong length)
)
```

### Regex Testing Tools

Use online regex testers to validate patterns:

- [regex101.com](https://regex101.com)
- [regexpal.com](https://regexpal.com)
- [regexr.com](https://regexr.com)

### Pattern Validation

Test patterns against known good and bad examples:

```bash
# Test with verbose output
./gradlew scanForSecrets --info

# Check specific files
./gradlew scanForSecrets -Dscan.include="test-patterns.kt"
```

## Best Practices

### Pattern Design

1. **Be Specific**: Avoid overly broad patterns that cause false positives
2. **Use Anchors**: Use `^` and `$` when matching entire lines
3. **Consider Context**: Think about where the pattern might appear
4. **Test Thoroughly**: Validate against real codebases

### Pattern Maintenance

1. **Regular Reviews**: Periodically review and update patterns
2. **False Positive Tracking**: Keep track of common false positives
3. **Team Input**: Get feedback from developers on pattern effectiveness
4. **Documentation**: Document the purpose and examples for each custom pattern

### Common Pitfalls

#### Overly Broad Patterns

```kotlin
// ❌ Too broad - will match everything
".*password.*"

// ✅ Better - more specific
"password\\s*=\\s*[\"'][^\"']{8,}[\"']"
```

#### Inefficient Patterns

```kotlin
// ❌ Inefficient - catastrophic backtracking
"(a+)+b"

// ✅ Efficient - atomic grouping
"(?>a+)+b"
```

#### Case Sensitivity Issues

```kotlin
// ❌ Case sensitive - might miss variations
"Password"

// ✅ Case insensitive
"(?i)password"
```

### Integration with Entropy Detection

Custom patterns work alongside SCAN's entropy detection:

```kotlin
scan {
    // Lower entropy threshold to catch more random strings
    entropyThreshold = 4.0
    
    // Custom patterns for structured secrets
    customPatterns = listOf(
        "STRUCTURED_KEY_[A-Z0-9]{20}"
    )
}
```

### Testing Strategy

1. **Unit Test Patterns**: Test each pattern in isolation
2. **Integration Test**: Test with real codebase samples
3. **Performance Test**: Ensure patterns don't slow down scanning
4. **False Positive Test**: Verify patterns don't trigger on safe code

---

For more information on configuration and usage, see the [Configuration Reference](configuration-reference.md) and [User Guide](user-guide.md).
