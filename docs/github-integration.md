# GitHub Security Scanning Integration

This guide explains how to integrate the SCAN plugin with GitHub Code Scanning to automatically upload security findings to your GitHub repository.

## Table of Contents

- [GitHub Security Scanning Integration](#github-security-scanning-integration)
  - [Table of Contents](#table-of-contents)
  - [Overview](#overview)
  - [Prerequisites](#prerequisites)
  - [Quick Start (GitHub Actions)](#quick-start-github-actions)
    - [1. Add Workflow File](#1-add-workflow-file)
    - [2. Configure build.gradle.kts](#2-configure-buildgradlekts)
  - [Configuration Options](#configuration-options)
    - [Basic Configuration](#basic-configuration)
    - [Advanced GitHub Configuration](#advanced-github-configuration)
    - [Environment Variables](#environment-variables)
  - [Manual SARIF Upload](#manual-sarif-upload)
    - [1. Generate SARIF Report Only](#1-generate-sarif-report-only)
    - [2. Upload Using GitHub CLI](#2-upload-using-github-cli)
    - [3. Upload Using curl](#3-upload-using-curl)
  - [Viewing Results](#viewing-results)
  - [SARIF Report Format](#sarif-report-format)
  - [GitHub Security Severity Mapping](#github-security-severity-mapping)
  - [CI/CD Integration Examples](#cicd-integration-examples)
    - [GitLab CI](#gitlab-ci)
    - [Jenkins Pipeline](#jenkins-pipeline)
    - [Azure Pipelines](#azure-pipelines)
  - [Troubleshooting](#troubleshooting)
    - [Upload Fails with 403 Forbidden](#upload-fails-with-403-forbidden)
    - [SARIF File Too Large](#sarif-file-too-large)
    - [Results Not Appearing](#results-not-appearing)
    - [Authentication Errors](#authentication-errors)
  - [Best Practices](#best-practices)
  - [Additional Resources](#additional-resources)
  - [Support](#support)

## Overview

GitHub Code Scanning allows you to find, triage, and prioritize fixes for security vulnerabilities and errors in your code. SCAN integrates with GitHub Code Scanning by:

1. Generating SARIF (Static Analysis Results Interchange Format) reports
2. Automatically uploading results to GitHub's Code Scanning API
3. Creating security alerts visible in the GitHub Security tab

## Prerequisites

- GitHub repository with Code Scanning enabled (available for all public repositories and private repositories with GitHub Advanced Security)
- GitHub Actions workflow or appropriate GitHub token
- SCAN plugin version 2.2.0 or higher

## Quick Start (GitHub Actions)

### 1. Add Workflow File

Create `.github/workflows/security-scan.yml`:

```yaml
name: Security Scan

on:
    push:
        branches: [main, develop]
    pull_request:
        branches: [main]
    schedule:
        - cron: "0 0 * * 1" # Weekly on Monday

permissions:
    contents: read
    security-events: write # Required for uploading SARIF

jobs:
    scan:
        runs-on: ubuntu-latest

        steps:
            - name: Checkout code
              uses: actions/checkout@v4

            - name: Set up JDK 21
              uses: actions/setup-java@v4
              with:
                  java-version: "21"
                  distribution: "temurin"

            - name: Run SCAN Security Scanner
              run: ./gradlew scanForSecrets -PgenerateSarif=true
              env:
                  GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

            - name: Upload SARIF to GitHub
              uses: github/codeql-action/upload-sarif@v3
              if: always()
              with:
                  sarif_file: build/reports/scan/scan-results.sarif
```

### 2. Configure build.gradle.kts

```kotlin
plugins {
    id("io.github.theaniketraj.scan") version "2.2.0"
}

scan {
    // Enable SARIF report generation
    generateSarifReport.set(true)

    // GitHub integration (auto-detected in GitHub Actions)
    github {
        enabled = true
        uploadSarif = true
        // token, repository, ref, and commitSha are auto-detected from environment
    }
}
```

## Configuration Options

### Basic Configuration

```kotlin
scan {
    generateSarifReport.set(true)  // Enable SARIF report generation
}
```

### Advanced GitHub Configuration

```kotlin
scan {
    generateSarifReport.set(true)

    github {
        enabled = true              // Enable GitHub integration
        uploadSarif = true          // Upload SARIF to GitHub Code Scanning

        // Override environment variables (optional)
        token = System.getenv("GITHUB_TOKEN")  // Default: auto-detected
        repository = "owner/repo"              // Default: GITHUB_REPOSITORY
        ref = "refs/heads/main"                // Default: GITHUB_REF
        commitSha = "abc123..."                // Default: GITHUB_SHA or git rev-parse HEAD
        apiUrl = "https://api.github.com"     // Default: public GitHub
    }
}
```

### Environment Variables

The plugin automatically detects these environment variables in GitHub Actions:

| Variable            | Description                       | Required |
| ------------------- | --------------------------------- | -------- |
| `GITHUB_TOKEN`      | GitHub authentication token       | Yes      |
| `GITHUB_REPOSITORY` | Repository in format `owner/repo` | Yes      |
| `GITHUB_REF`        | Git reference (branch/tag)        | Yes      |
| `GITHUB_SHA`        | Commit SHA being analyzed         | Yes      |
| `GITHUB_ACTIONS`    | Set to `true` in GitHub Actions   | No       |

## Manual SARIF Upload

If you prefer to upload SARIF manually:

### 1. Generate SARIF Report Only

```kotlin
scan {
    generateSarifReport.set(true)

    github {
        enabled = false  // Don't auto-upload
    }
}
```

### 2. Upload Using GitHub CLI

```bash
./gradlew scanForSecrets

gh api repos/:owner/:repo/code-scanning/sarifs \
  -F commit_sha="$(git rev-parse HEAD)" \
  -F ref="refs/heads/main" \
  -F sarif=@build/reports/scan/scan-results.sarif
```

### 3. Upload Using curl

```bash
SARIF_BASE64=$(base64 -w 0 build/reports/scan/scan-results.sarif)

curl -X POST \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/OWNER/REPO/code-scanning/sarifs \
  -d "{
    \"commit_sha\": \"$(git rev-parse HEAD)\",
    \"ref\": \"refs/heads/main\",
    \"sarif\": \"$SARIF_BASE64\"
  }"
```

## Viewing Results

After uploading, security findings appear in:

1. **Security Tab**: Navigate to your repository → Security → Code scanning alerts
2. **Pull Requests**: Annotations appear on changed files
3. **Code View**: Alerts show inline in the code browser

## SARIF Report Format

The generated SARIF report includes:

- **Tool Information**: SCAN plugin details and version
- **Rules**: Descriptions of each secret type detected
- **Results**: Location, severity, and details of each finding
- **Metadata**: Scan timing and configuration

Example SARIF structure:

```json
{
  "$schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json",
  "version": "2.1.0",
  "runs": [{
    "tool": {
      "driver": {
        "name": "SCAN",
        "version": "2.2.0",
        "rules": [...]
      }
    },
    "results": [
      {
        "ruleId": "AWS_ACCESS_KEY",
        "level": "error",
        "message": {
          "text": "Found AWS Access Key ID"
        },
        "locations": [{
          "physicalLocation": {
            "artifactLocation": {
              "uri": "src/main/kotlin/config/Config.kt"
            },
            "region": {
              "startLine": 42,
              "startColumn": 15
            }
          }
        }]
      }
    ]
  }]
}
```

## GitHub Security Severity Mapping

| SCAN Severity | GitHub Level | Security Severity Score |
| ------------- | ------------ | ----------------------- |
| CRITICAL      | error        | 9.0                     |
| HIGH          | error        | 7.0                     |
| MEDIUM        | warning      | 5.0                     |
| LOW           | note         | 3.0                     |
| INFO          | note         | 1.0                     |

## CI/CD Integration Examples

### GitLab CI

```yaml
security_scan:
    stage: test
    script:
        - ./gradlew scanForSecrets -PgenerateSarif=true
        - |
            # Upload to GitHub if repository is mirrored
            if [ -n "$GITHUB_TOKEN" ]; then
              ./gradlew uploadSarifToGitHub
            fi
    artifacts:
        reports:
            sast: build/reports/scan/scan-results.sarif
        expire_in: 1 week
```

### Jenkins Pipeline

```groovy
pipeline {
    agent any

    stages {
        stage('Security Scan') {
            steps {
                sh './gradlew scanForSecrets -PgenerateSarif=true'
            }
        }

        stage('Upload to GitHub') {
            when {
                expression { env.GITHUB_TOKEN != null }
            }
            steps {
                sh '''
                    curl -X POST \
                      -H "Authorization: Bearer $GITHUB_TOKEN" \
                      https://api.github.com/repos/$REPO/code-scanning/sarifs \
                      -d @build/reports/scan/scan-results.json
                '''
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'build/reports/scan/*.sarif'
        }
    }
}
```

### Azure Pipelines

```yaml
- task: Gradle@3
  displayName: "Security Scan"
  inputs:
      tasks: "scanForSecrets"
      options: "-PgenerateSarif=true"

- task: PublishBuildArtifacts@1
  displayName: "Publish SARIF"
  inputs:
      PathtoPublish: "build/reports/scan/scan-results.sarif"
      ArtifactName: "SecurityScan"
```

## Troubleshooting

### Upload Fails with 403 Forbidden

**Cause**: Code Scanning not enabled or insufficient permissions.

**Solution**:

1. Enable GitHub Advanced Security (for private repos)
2. Ensure workflow has `security-events: write` permission
3. Verify GITHUB_TOKEN has correct scopes

### SARIF File Too Large

**Cause**: GitHub has a 10MB limit for SARIF uploads.

**Solution**:

```kotlin
scan {
    // Filter to reduce findings
    severityThreshold.set("MEDIUM")  // Only MEDIUM and above

    // Or exclude low-confidence findings
    confidenceThreshold.set(0.7)
}
```

### Results Not Appearing

**Cause**: May take a few minutes to process.

**Solution**:

1. Check GitHub Actions logs for upload confirmation
2. Verify SARIF file is valid: `cat build/reports/scan/scan-results.sarif | jq .`
3. Ensure commit SHA matches the analyzed code

### Authentication Errors

**Cause**: Invalid or expired GitHub token.

**Solution**:

```bash
# Test token validity
curl -H "Authorization: Bearer $GITHUB_TOKEN" \
     https://api.github.com/user

# In GitHub Actions, ensure you're using the provided token:
env:
  GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

## Best Practices

1. **Run on Every Push**: Catch secrets before they reach main branch
2. **Scan Pull Requests**: Review findings before merging
3. **Schedule Regular Scans**: Weekly scans catch historical issues
4. **Set Severity Thresholds**: Focus on critical and high-severity findings
5. **Review False Positives**: Use whitelisting for legitimate patterns
6. **Rotate Detected Secrets**: Treat all findings as compromised

## Additional Resources

- [GitHub Code Scanning Documentation](https://docs.github.com/en/code-security/code-scanning)
- [SARIF Specification](https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html)
- [GitHub Advanced Security](https://docs.github.com/en/get-started/learning-about-github/about-github-advanced-security)
- [SCAN Plugin Documentation](../README.md)

## Support

For issues or questions:

- GitHub Issues: <https://github.com/theaniketraj/SCAN/issues>
- Security Concerns: <security@scan-plugin.dev>
