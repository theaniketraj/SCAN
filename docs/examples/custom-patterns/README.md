# Custom Patterns Examples

This directory contains examples for creating and using custom patterns with the SCAN plugin to detect organization-specific secrets and sensitive information.

## Table of Contents

1. [Basic Custom Patterns](#basic-custom-patterns)
2. [Organization-Specific Patterns](#organization-specific-patterns)
3. [File Type-Specific Patterns](#file-type-specific-patterns)
4. [Advanced Pattern Techniques](#advanced-pattern-techniques)
5. [Pattern Testing](#pattern-testing)
6. [Configuration Examples](#configuration-examples)

## Basic Custom Patterns

### Simple API Key Patterns

```kotlin
scan {
    customPatterns = listOf(
        // Company API keys with specific format
        "MYCOMPANY_API_[A-Z0-9]{32}",
        
        // Internal service tokens
        "INTERNAL_TOKEN_[a-f0-9]{40}",
        
        // License keys
        "LIC_[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}"
    )
}
```

### Database and Connection Patterns

```kotlin
scan {
    customPatterns = listOf(
        // Custom database connection strings
        "mycompany://[^\\s]+:[^\\s]+@[^\\s]+",
        
        // Internal service URLs with credentials
        "https://[^\\s]+:[^\\s]+@internal\\.[^\\s]+",
        
        // Custom certificate references
        "CERT_[A-F0-9]{40}"
    )
}
```

## Organization-Specific Patterns

### Enterprise Patterns

```kotlin
scan {
    customPatterns = listOf(
        // Active Directory credentials
        "(?i)ldap_password[\"'\\s]*[:=][\"'\\s]*[^\\s\"']{8,}",
        
        // SAP system credentials  
        "SAP_[A-Z]{3}_[0-9]{3}_[A-Za-z0-9]{16}",
        
        // Oracle database identifiers
        "ORACLE_SID_[A-Z0-9]{8,12}",
        
        // Custom encryption keys
        "ENC_KEY_[A-Fa-f0-9]{64}",
        
        // Service account patterns
        "SVC_[A-Z]{2,5}_[A-Za-z0-9]{20,40}"
    )
}
```

### Microservices Patterns

```kotlin
scan {
    customPatterns = listOf(
        // Service-to-service authentication
        "SERVICE_AUTH_[a-zA-Z0-9\\-_]{32}",
        
        // API gateway tokens
        "GATEWAY_TOKEN_[A-Za-z0-9+/]{44}==?",
        
        // Kubernetes secrets references
        "K8S_SECRET_[a-z0-9\\-]{1,253}\\.[a-z0-9\\-]{1,63}",
        
        // Docker registry credentials
        "DOCKER_AUTH_[A-Za-z0-9+/=]{20,}",
        
        // Message queue credentials
        "MQ_PASS_[A-Za-z0-9!@#$%^&*]{12,}"
    )
}
```

### Cloud Provider Patterns

```kotlin
scan {
    customPatterns = listOf(
        // Custom AWS patterns (organization-specific)
        "MYORG_AWS_[A-Z0-9]{20}",
        
        // Azure custom patterns
        "MYORG_AZURE_[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}",
        
        // GCP custom service account patterns
        "MYORG_GCP_[a-z0-9\\-]+@[a-z0-9\\-]+\\.iam\\.gserviceaccount\\.com",
        
        // Private cloud credentials
        "PRIVATE_CLOUD_[A-Za-z0-9]{40}"
    )
}
```

## File Type-Specific Patterns

### Configuration File Patterns

```kotlin
scan {
    customPatterns = listOf(
        // Properties file secrets
        "(?i)^[a-z_]+\\.secret[a-z_]*\\s*=\\s*[A-Za-z0-9+/]{20,}$",
        
        // YAML secrets
        "(?i)secret[a-z_]*:\\s*['\"]?[A-Za-z0-9+/]{20,}['\"]?",
        
        // JSON secrets
        "(?i)\"[a-z_]*secret[a-z_]*\"\\s*:\\s*\"[A-Za-z0-9+/]{20,}\"",
        
        // XML configuration
        "<secret[^>]*>[A-Za-z0-9+/]{20,}</secret>",
        
        // Environment variables
        "^[A-Z_]+_SECRET=[A-Za-z0-9+/=]{20,}$"
    )
}
```

### Source Code Patterns

```kotlin
scan {
    customPatterns = listOf(
        // Kotlin/Java string literals
        "(?i)val\\s+[a-z_]*secret[a-z_]*\\s*=\\s*\"[A-Za-z0-9+/]{20,}\"",
        
        // JavaScript/TypeScript patterns
        "(?i)const\\s+[a-z_]*secret[a-z_]*\\s*=\\s*['\"][A-Za-z0-9+/]{20,}['\"]",
        
        // Python patterns
        "(?i)[a-z_]*secret[a-z_]*\\s*=\\s*['\"][A-Za-z0-9+/]{20,}['\"]",
        
        // Go patterns
        "(?i)var\\s+[a-z_]*Secret[a-z_]*\\s*=\\s*\"[A-Za-z0-9+/]{20,}\"",
        
        // SQL connection strings
        "(?i)connectionstring\\s*=\\s*['\"][^'\"]*password=[^;'\"]+['\"]"
    )
}
```

### Infrastructure as Code Patterns

```kotlin
scan {
    customPatterns = listOf(
        // Terraform variables
        "(?i)variable\\s+['\"][a-z_]*secret[a-z_]*['\"]\\s*{[^}]*default\\s*=\\s*['\"][A-Za-z0-9+/]{20,}['\"]",
        
        // Ansible vault references (should be encrypted)
        "!vault\\s*\\|\\s*[A-Za-z0-9+/\\s=]+",
        
        // CloudFormation parameters
        "(?i)\"[a-z]*secret[a-z]*\"\\s*:\\s*\"[A-Za-z0-9+/]{20,}\"",
        
        // Kubernetes manifest secrets
        "stringData:\\s*\\n\\s*[a-z-]+:\\s*[A-Za-z0-9+/=]{20,}",
        
        // Docker Compose secrets
        "(?i)environment:\\s*\\n\\s*-\\s*[A-Z_]*SECRET[A-Z_]*=[A-Za-z0-9+/=]{20,}"
    )
}
```

## Advanced Pattern Techniques

### Context-Aware Patterns

```kotlin
scan {
    customPatterns = listOf(
        // Patterns that consider context
        "(?i)(?:api[_-]?key|apikey|api[_-]?secret)\\s*[=:]\\s*['\"]?([A-Za-z0-9\\-_]{20,})['\"]?",
        
        // Patterns with negative lookahead (exclude test data)
        "(?!.*test)(?!.*example)(?!.*dummy)API_KEY_[A-Z0-9]{32}",
        
        // Patterns that require specific file extensions
        "(?i)password\\s*=\\s*['\"][^'\"]{8,}['\"](?=.*\\.properties$)",
        
        // Patterns with word boundaries
        "\\b(?i)secret\\b[^=:]*[=:]\\s*['\"]?([A-Za-z0-9+/]{16,})['\"]?",
        
        // Multi-line patterns
        "(?s)BEGIN\\s+PRIVATE\\s+KEY.*?END\\s+PRIVATE\\s+KEY"
    )
}
```

### Performance-Optimized Patterns

```kotlin
scan {
    customPatterns = listOf(
        // Anchored patterns (faster)
        "^API_TOKEN=[A-Za-z0-9]{40}$",
        
        // Specific character classes (more efficient than wildcards)
        "SECRET_[A-F0-9]{32}",
        
        // Atomic groups to prevent backtracking
        "(?>COMPANY_API_)[A-Z0-9]{20}",
        
        // Possessive quantifiers
        "TOKEN_[A-Za-z0-9]++",
        
        // Character classes instead of alternation
        "KEY_[A-Z0-9]{16}(?:[A-Z]{4}|[0-9]{4})"
    )
}
```

### Industry-Specific Patterns

```kotlin
scan {
    customPatterns = listOf(
        // Financial services
        "(?i)swift[_-]?code[\"'\\s]*[:=][\"'\\s]*[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?",
        "(?i)iban[\"'\\s]*[:=][\"'\\s]*[A-Z]{2}[0-9]{2}[A-Z0-9]{4}[0-9]{7}([A-Z0-9]?){0,16}",
        
        // Healthcare
        "(?i)hipaa[_-]?key[\"'\\s]*[:=][\"'\\s]*[A-Za-z0-9+/]{32,}",
        "(?i)phi[_-]?token[\"'\\s]*[:=][\"'\\s]*[A-Za-z0-9\\-_]{20,}",
        
        // Government/Defense
        "(?i)classified[_-]?key[\"'\\s]*[:=][\"'\\s]*[A-Z0-9]{40}",
        "(?i)security[_-]?clearance[\"'\\s]*[:=][\"'\\s]*[A-Z0-9\\-]{20,}",
        
        // Education
        "(?i)ferpa[_-]?token[\"'\\s]*[:=][\"'\\s]*[A-Za-z0-9]{24,}",
        "(?i)student[_-]?id[_-]?key[\"'\\s]*[:=][\"'\\s]*[A-Z0-9]{16,}"
    )
}
```

## Pattern Testing

### Test Configuration

```kotlin
// build.gradle.kts
scan {
    // Test mode configuration
    if (project.hasProperty("testPatterns")) {
        customPatterns = listOf(
            "TEST_SECRET_[A-Z0-9]{20}",
            "DEMO_API_[a-f0-9]{32}"
        )
        
        // Only scan test files
        includePatterns = setOf(
            "src/test/**/*.kt",
            "test-patterns.txt"
        )
        
        // Generate detailed reports for analysis
        verbose = true
        generateHtmlReport = true
    }
}
```

### Pattern Test File

Create a test file to validate your patterns:

```kotlin
// src/test/kotlin/PatternTest.kt
class PatternTest {
    
    // These should be detected by custom patterns
    val testSecrets = mapOf(
        "companyApi" to "MYCOMPANY_API_12345678901234567890123456789012",
        "internalToken" to "INTERNAL_TOKEN_1234567890abcdef1234567890abcdef12345678",
        "licenseKey" to "LIC_ABCD-1234-EFGH-5678",
        "serviceAuth" to "SERVICE_AUTH_ABCDEFGHIJKLMNOPQRSTUVWXYZabcdef",
        "encryptionKey" to "ENC_KEY_1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
    )
    
    // These should NOT be detected (valid test data)
    val validTestData = mapOf(
        "mockApi" to "MOCKCOMPANY_API_test_key_for_unit_tests",
        "placeholder" to "YOUR_API_KEY_HERE",
        "example" to "example_secret_value",
        "template" to "{{API_KEY}}",
        "comment" to "// Replace with actual API key"
    )
}
```

### Pattern Validation Script

```bash
#!/bin/bash
# validate-patterns.sh

echo "Testing custom patterns..."

# Run scan with test patterns
./gradlew scanForSecrets -PtestPatterns=true

# Check results
if [ -f "build/reports/scan/scan-report.json" ]; then
    SECRETS_FOUND=$(jq '.summary.secretsFound' build/reports/scan/scan-report.json)
    echo "Patterns detected $SECRETS_FOUND potential secrets"
    
    # Expected number of test secrets
    EXPECTED=5
    
    if [ "$SECRETS_FOUND" -eq "$EXPECTED" ]; then
        echo "✅ Pattern validation passed!"
    else
        echo "❌ Pattern validation failed. Expected $EXPECTED, found $SECRETS_FOUND"
        exit 1
    fi
else
    echo "❌ No scan report found"
    exit 1
fi
```

## Configuration Examples

### Development Environment

```kotlin
scan {
    // Relaxed patterns for development
    customPatterns = listOf(
        "DEV_API_[A-Z0-9]{20}",
        "LOCAL_SECRET_[a-f0-9]{32}"
    )
    
    // Don't fail build in development
    failOnSecrets = false
    warnOnSecrets = true
    
    // Focus on main source code
    includePatterns = setOf(
        "src/main/**/*.kt",
        "src/main/**/*.properties"
    )
}
```

### Production Environment

```kotlin
scan {
    // Strict patterns for production
    customPatterns = listOf(
        // All organization patterns
        "PROD_API_[A-Z0-9]{32}",
        "LIVE_SECRET_[a-f0-9]{64}",
        "PRODUCTION_KEY_[A-Za-z0-9+/]{44}==?",
        
        // Database patterns
        "prod://[^\\s]+:[^\\s]+@[^\\s]+",
        
        // Service patterns
        "SVC_PROD_[A-Za-z0-9]{40}",
        
        // Certificate patterns
        "PROD_CERT_[A-F0-9]{40}"
    )
    
    // Strict configuration
    strictMode = true
    failOnSecrets = true
    entropyThreshold = 3.5
    
    // Comprehensive scanning
    includePatterns = setOf(
        "src/**/*.kt",
        "src/**/*.java",
        "src/**/*.properties",
        "src/**/*.yml",
        "src/**/*.yaml",
        "config/**/*",
        "scripts/**/*"
    )
    
    // Detailed reporting
    generateHtmlReport = true
    generateJsonReport = true
    verbose = true
}
```

### CI/CD Pipeline Configuration

```kotlin
scan {
    val isCi = System.getenv("CI")?.toBoolean() ?: false
    val branch = System.getenv("BRANCH_NAME") ?: "unknown"
    
    customPatterns = when {
        branch.contains("release") -> listOf(
            // Strict patterns for release branches
            "RELEASE_API_[A-Z0-9]{32}",
            "FINAL_SECRET_[a-f0-9]{64}"
        )
        branch == "main" -> listOf(
            // Standard patterns for main branch
            "MAIN_API_[A-Z0-9]{24}",
            "MASTER_SECRET_[a-f0-9]{48}"
        )
        else -> listOf(
            // Basic patterns for feature branches
            "FEATURE_API_[A-Z0-9]{16}",
            "DEV_SECRET_[a-f0-9]{32}"
        )
    }
    
    failOnSecrets = isCi && (branch == "main" || branch.contains("release"))
    generateJsonReport = isCi
    verbose = isCi
}
```

### Module-Specific Patterns

```kotlin
// In a multi-module project
allprojects {
    apply(plugin = "io.github.theaniketraj.scan")
    
    scan {
        customPatterns = when (project.name) {
            "auth-service" -> listOf(
                "AUTH_SECRET_[A-Za-z0-9]{40}",
                "JWT_SIGNING_KEY_[A-Za-z0-9+/]{88}==?"
            )
            "payment-service" -> listOf(
                "PAYMENT_API_[A-Z0-9]{32}",
                "STRIPE_SECRET_sk_[a-z]+_[A-Za-z0-9]{24}"
            )
            "notification-service" -> listOf(
                "NOTIFICATION_KEY_[A-Za-z0-9]{30}",
                "PUSH_SECRET_[A-Za-z0-9\\-_]{40}"
            )
            else -> listOf(
                "GENERIC_API_[A-Z0-9]{24}"
            )
        }
    }
}
```

## Best Practices for Custom Patterns

### Pattern Design Guidelines

1. **Be Specific**: Avoid overly broad patterns

   ```kotlin
   // ❌ Too broad
   ".*secret.*"
   
   // ✅ Specific
   "API_SECRET_[A-Z0-9]{32}"
   ```

2. **Use Anchors**: Anchor patterns when possible

   ```kotlin
   // ❌ Unanchored
   "SECRET=[A-Z0-9]+"
   
   // ✅ Anchored
   "^SECRET=[A-Z0-9]+$"
   ```

3. **Consider Context**: Include context clues

   ```kotlin
   // ✅ Context-aware
   "(?i)api[_-]?key\\s*[=:]\\s*[A-Za-z0-9]{20,}"
   ```

4. **Optimize Performance**: Use efficient regex constructs

   ```kotlin
   // ✅ Atomic group
   "(?>API_KEY_)[A-Z0-9]{32}"
   ```

### Testing Strategy

1. **Create test files** with known patterns
2. **Validate against false positives** using real code
3. **Performance test** with large files
4. **Regular review** and updates

### Documentation

Document your custom patterns:

```kotlin
scan {
    customPatterns = listOf(
        // Company API keys (format: COMP_API_32chars)
        "COMP_API_[A-Z0-9]{32}",
        
        // Internal service tokens (format: SVC_40hexchars)
        "SVC_[a-f0-9]{40}",
        
        // Database connection strings for internal services
        "internal://[^\\s]+:[^\\s]+@[^\\s]+"
    )
}
```

---

These examples provide a comprehensive foundation for implementing custom patterns tailored to your organization's specific security requirements.
