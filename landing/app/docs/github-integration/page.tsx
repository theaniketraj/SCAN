import React from "react";
import DocsLayout from "../../../components/DocsLayout";

export default function GitHubIntegrationPage() {
    const sections = [
        { id: "overview", title: "Overview" },
        { id: "prerequisites", title: "Prerequisites" },
        { id: "quick-start", title: "Quick Start" },
        { id: "configuration-options", title: "Configuration Options" },
        { id: "manual-sarif-upload", title: "Manual SARIF Upload" },
        { id: "viewing-results", title: "Viewing Results" },
        { id: "sarif-report-format", title: "SARIF Report Format" },
        {
            id: "github-security-severity-mapping",
            title: "GitHub Security Severity Mapping",
        },
        {
            id: "cicd-integration-examples",
            title: "CI/CD Integration Examples",
        },
        { id: "troubleshooting", title: "Troubleshooting" },
        { id: "best-practices", title: "Best Practices" },
        { id: "additional-resources", title: "Additional Resources" },
        { id: "support", title: "Support" },
    ];

    return (
        <DocsLayout
            sections={sections}
            title="GitHub Security Scanning Integration"
        >
            <div className="prose prose-lg dark:prose-invert max-w-none">
                <h1>GitHub Security Scanning Integration</h1>
                <p className="text-xl text-gray-600 dark:text-gray-300">
                    This guide explains how to integrate the SCAN plugin with
                    GitHub Code Scanning to automatically upload security
                    findings to your GitHub repository.
                </p>

                <section id="overview">
                    <h2>Overview</h2>
                    <p>
                        GitHub Code Scanning allows you to find, triage, and
                        prioritize fixes for security vulnerabilities and errors
                        in your code. SCAN integrates with GitHub Code Scanning
                        by:
                    </p>
                    <ol>
                        <li>
                            Generating SARIF (Static Analysis Results
                            Interchange Format) reports
                        </li>
                        <li>
                            Automatically uploading results to GitHub&apos;s
                            Code Scanning API
                        </li>
                        <li>
                            Creating security alerts visible in the GitHub
                            Security tab
                        </li>
                    </ol>
                </section>

                <section id="prerequisites">
                    <h2>Prerequisites</h2>
                    <ul>
                        <li>
                            GitHub repository with Code Scanning enabled
                            (available for all public repositories and private
                            repositories with GitHub Advanced Security)
                        </li>
                        <li>
                            GitHub Actions workflow or appropriate GitHub token
                        </li>
                        <li>SCAN plugin version 2.0.0 or higher</li>
                    </ul>
                </section>

                <section id="quick-start">
                    <h2>Quick Start (GitHub Actions)</h2>

                    <h3>1. Add Workflow File</h3>
                    <p>
                        Create <code>.github/workflows/security-scan.yml</code>:
                    </p>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto">
                        <code>{`name: Security Scan

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
                  GITHUB_TOKEN: \${{ secrets.GITHUB_TOKEN }}

            - name: Upload SARIF to GitHub
              uses: github/codeql-action/upload-sarif@v3
              if: always()
              with:
                  sarif_file: build/reports/scan/scan-results.sarif`}</code>
                    </pre>

                    <h3>2. Configure build.gradle.kts</h3>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto">
                        <code>{`plugins {
    id("io.github.theaniketraj.scan") version "2.0.0"
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
}`}</code>
                    </pre>
                </section>

                <section id="configuration-options">
                    <h2>Configuration Options</h2>

                    <h3>Basic Configuration</h3>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto">
                        <code>{`scan {
    generateSarifReport.set(true)  // Enable SARIF report generation
}`}</code>
                    </pre>

                    <h3>Advanced GitHub Configuration</h3>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto">
                        <code>{`scan {
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
}`}</code>
                    </pre>

                    <h3>Environment Variables</h3>
                    <p>
                        The plugin automatically detects these environment
                        variables in GitHub Actions:
                    </p>
                    <div className="overflow-x-auto">
                        <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                            <thead className="bg-gray-50 dark:bg-gray-800">
                                <tr>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                                        Variable
                                    </th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                                        Description
                                    </th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                                        Required
                                    </th>
                                </tr>
                            </thead>
                            <tbody className="bg-white dark:bg-gray-900 divide-y divide-gray-200 dark:divide-gray-700">
                                <tr>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        <code>GITHUB_TOKEN</code>
                                    </td>
                                    <td className="px-6 py-4 text-sm">
                                        GitHub authentication token
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        Yes
                                    </td>
                                </tr>
                                <tr>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        <code>GITHUB_REPOSITORY</code>
                                    </td>
                                    <td className="px-6 py-4 text-sm">
                                        Repository in format{" "}
                                        <code>owner/repo</code>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        Yes
                                    </td>
                                </tr>
                                <tr>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        <code>GITHUB_REF</code>
                                    </td>
                                    <td className="px-6 py-4 text-sm">
                                        Git reference (branch/tag)
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        Yes
                                    </td>
                                </tr>
                                <tr>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        <code>GITHUB_SHA</code>
                                    </td>
                                    <td className="px-6 py-4 text-sm">
                                        Commit SHA being analyzed
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        Yes
                                    </td>
                                </tr>
                                <tr>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        <code>GITHUB_ACTIONS</code>
                                    </td>
                                    <td className="px-6 py-4 text-sm">
                                        Set to <code>true</code> in GitHub
                                        Actions
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        No
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </section>

                <section id="manual-sarif-upload">
                    <h2>Manual SARIF Upload</h2>
                    <p>If you prefer to upload SARIF manually:</p>

                    <h3>1. Generate SARIF Report Only</h3>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto">
                        <code>{`scan {
    generateSarifReport.set(true)

    github {
        enabled = false  // Don't auto-upload
    }
}`}</code>
                    </pre>

                    <h3>2. Upload Using GitHub CLI</h3>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto">
                        <code>{`./gradlew scanForSecrets

gh api repos/:owner/:repo/code-scanning/sarifs \\
  -F commit_sha="$(git rev-parse HEAD)" \\
  -F ref="refs/heads/main" \\
  -F sarif=@build/reports/scan/scan-results.sarif`}</code>
                    </pre>

                    <h3>3. Upload Using curl</h3>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto">
                        <code>{`SARIF_BASE64=$(base64 -w 0 build/reports/scan/scan-results.sarif)

curl -X POST \\
  -H "Authorization: Bearer $GITHUB_TOKEN" \\
  -H "Accept: application/vnd.github+json" \\
  -H "X-GitHub-Api-Version: 2022-11-28" \\
  https://api.github.com/repos/OWNER/REPO/code-scanning/sarifs \\
  -d "{
    \\"commit_sha\\": \\"$(git rev-parse HEAD)\\",
    \\"ref\\": \\"refs/heads/main\\",
    \\"sarif\\": \\"$SARIF_BASE64\\"
  }"`}</code>
                    </pre>
                </section>

                <section id="viewing-results">
                    <h2>Viewing Results</h2>
                    <p>After uploading, security findings appear in:</p>
                    <ol>
                        <li>
                            <strong>Security Tab:</strong> Navigate to your
                            repository → Security → Code scanning alerts
                        </li>
                        <li>
                            <strong>Pull Requests:</strong> Annotations appear
                            on changed files
                        </li>
                        <li>
                            <strong>Code View:</strong> Alerts show inline in
                            the code browser
                        </li>
                    </ol>
                </section>

                <section id="sarif-report-format">
                    <h2>SARIF Report Format</h2>
                    <p>The generated SARIF report includes:</p>
                    <ul>
                        <li>
                            <strong>Tool Information:</strong> SCAN plugin
                            details and version
                        </li>
                        <li>
                            <strong>Rules:</strong> Descriptions of each secret
                            type detected
                        </li>
                        <li>
                            <strong>Results:</strong> Location, severity, and
                            details of each finding
                        </li>
                        <li>
                            <strong>Metadata:</strong> Scan timing and
                            configuration
                        </li>
                    </ul>

                    <p>Example SARIF structure:</p>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto">
                        <code>{`{
  "$schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json",
  "version": "2.1.0",
  "runs": [{
    "tool": {
      "driver": {
        "name": "SCAN",
        "version": "2.0.0",
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
}`}</code>
                    </pre>
                </section>

                <section id="github-security-severity-mapping">
                    <h2>GitHub Security Severity Mapping</h2>
                    <div className="overflow-x-auto">
                        <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                            <thead className="bg-gray-50 dark:bg-gray-800">
                                <tr>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                                        SCAN Severity
                                    </th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                                        GitHub Level
                                    </th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                                        Security Severity Score
                                    </th>
                                </tr>
                            </thead>
                            <tbody className="bg-white dark:bg-gray-900 divide-y divide-gray-200 dark:divide-gray-700">
                                <tr>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        CRITICAL
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        error
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        9.0
                                    </td>
                                </tr>
                                <tr>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        HIGH
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        error
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        7.0
                                    </td>
                                </tr>
                                <tr>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        MEDIUM
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        warning
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        5.0
                                    </td>
                                </tr>
                                <tr>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        LOW
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        note
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        3.0
                                    </td>
                                </tr>
                                <tr>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        INFO
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        note
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        1.0
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </section>

                <section id="cicd-integration-examples">
                    <h2>CI/CD Integration Examples</h2>

                    <h3>GitLab CI</h3>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto">
                        <code>{`security_scan:
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
        expire_in: 1 week`}</code>
                    </pre>

                    <h3>Jenkins Pipeline</h3>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto">
                        <code>{`pipeline {
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
                    curl -X POST \\
                      -H "Authorization: Bearer $GITHUB_TOKEN" \\
                      https://api.github.com/repos/$REPO/code-scanning/sarifs \\
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
}`}</code>
                    </pre>

                    <h3>Azure Pipelines</h3>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto">
                        <code>{`- task: Gradle@3
  displayName: "Security Scan"
  inputs:
      tasks: "scanForSecrets"
      options: "-PgenerateSarif=true"

- task: PublishBuildArtifacts@1
  displayName: "Publish SARIF"
  inputs:
      PathtoPublish: "build/reports/scan/scan-results.sarif"
      ArtifactName: "SecurityScan"`}</code>
                    </pre>
                </section>

                <section id="troubleshooting">
                    <h2>Troubleshooting</h2>

                    <h3>Upload Fails with 403 Forbidden</h3>
                    <p>
                        <strong>Cause:</strong> Code Scanning not enabled or
                        insufficient permissions.
                    </p>
                    <p>
                        <strong>Solution:</strong>
                    </p>
                    <ol>
                        <li>
                            Enable GitHub Advanced Security (for private repos)
                        </li>
                        <li>
                            Ensure workflow has{" "}
                            <code>security-events: write</code> permission
                        </li>
                        <li>Verify GITHUB_TOKEN has correct scopes</li>
                    </ol>

                    <h3>SARIF File Too Large</h3>
                    <p>
                        <strong>Cause:</strong> GitHub has a 10MB limit for
                        SARIF uploads.
                    </p>
                    <p>
                        <strong>Solution:</strong>
                    </p>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto">
                        <code>{`scan {
    // Filter to reduce findings
    severityThreshold.set("MEDIUM")  // Only MEDIUM and above

    // Or exclude low-confidence findings
    confidenceThreshold.set(0.7)
}`}</code>
                    </pre>

                    <h3>Results Not Appearing</h3>
                    <p>
                        <strong>Cause:</strong> May take a few minutes to
                        process.
                    </p>
                    <p>
                        <strong>Solution:</strong>
                    </p>
                    <ol>
                        <li>
                            Check GitHub Actions logs for upload confirmation
                        </li>
                        <li>
                            Verify SARIF file is valid:{" "}
                            <code>
                                cat build/reports/scan/scan-results.sarif | jq .
                            </code>
                        </li>
                        <li>Ensure commit SHA matches the analyzed code</li>
                    </ol>

                    <h3>Authentication Errors</h3>
                    <p>
                        <strong>Cause:</strong> Invalid or expired GitHub token.
                    </p>
                    <p>
                        <strong>Solution:</strong>
                    </p>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto">
                        <code>{`# Test token validity
curl -H "Authorization: Bearer $GITHUB_TOKEN" \\
     https://api.github.com/user

# In GitHub Actions, ensure you're using the provided token:
env:
  GITHUB_TOKEN: \${{ secrets.GITHUB_TOKEN }}`}</code>
                    </pre>
                </section>

                <section id="best-practices">
                    <h2>Best Practices</h2>
                    <ul>
                        <li>
                            <strong>Run on Every Push:</strong> Catch secrets
                            before they reach main branch
                        </li>
                        <li>
                            <strong>Scan Pull Requests:</strong> Review findings
                            before merging
                        </li>
                        <li>
                            <strong>Schedule Regular Scans:</strong> Weekly
                            scans catch historical issues
                        </li>
                        <li>
                            <strong>Set Severity Thresholds:</strong> Focus on
                            critical and high-severity findings
                        </li>
                        <li>
                            <strong>Review False Positives:</strong> Use
                            whitelisting for legitimate patterns
                        </li>
                        <li>
                            <strong>Rotate Detected Secrets:</strong> Treat all
                            findings as compromised
                        </li>
                    </ul>
                </section>

                <section id="additional-resources">
                    <h2>Additional Resources</h2>
                    <ul>
                        <li>
                            <a
                                href="https://docs.github.com/en/code-security/code-scanning"
                                className="text-primary-600 dark:text-primary-400 hover:underline"
                                target="_blank"
                                rel="noopener noreferrer"
                            >
                                GitHub Code Scanning Documentation
                            </a>
                        </li>
                        <li>
                            <a
                                href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html"
                                className="text-primary-600 dark:text-primary-400 hover:underline"
                                target="_blank"
                                rel="noopener noreferrer"
                            >
                                SARIF Specification
                            </a>
                        </li>
                        <li>
                            <a
                                href="https://docs.github.com/en/get-started/learning-about-github/about-github-advanced-security"
                                className="text-primary-600 dark:text-primary-400 hover:underline"
                                target="_blank"
                                rel="noopener noreferrer"
                            >
                                GitHub Advanced Security
                            </a>
                        </li>
                        <li>
                            <a
                                href="https://github.com/theaniketraj/SCAN"
                                className="text-primary-600 dark:text-primary-400 hover:underline"
                                target="_blank"
                                rel="noopener noreferrer"
                            >
                                SCAN Plugin Documentation
                            </a>
                        </li>
                    </ul>
                </section>

                <section id="support">
                    <h2>Support</h2>
                    <p>For issues or questions:</p>
                    <ul>
                        <li>
                            GitHub Issues:{" "}
                            <a
                                href="https://github.com/theaniketraj/SCAN/issues"
                                className="text-primary-600 dark:text-primary-400 hover:underline"
                                target="_blank"
                                rel="noopener noreferrer"
                            >
                                https://github.com/theaniketraj/SCAN/issues
                            </a>
                        </li>
                        <li>Security Concerns: security@scan-plugin.dev</li>
                    </ul>
                </section>
            </div>
        </DocsLayout>
    );
}
