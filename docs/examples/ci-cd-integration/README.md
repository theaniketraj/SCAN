# CI/CD Integration Examples

This directory contains examples for integrating SCAN with various CI/CD platforms and tools.

## Table of Contents

1. [GitHub Actions](#github-actions)
2. [Jenkins](#jenkins)
3. [GitLab CI](#gitlab-ci)
4. [Azure DevOps](#azure-devops)
5. [CircleCI](#circleci)
6. [Docker Integration](#docker-integration)
7. [Gradle Composite Builds](#gradle-composite-builds)

## GitHub Actions

### Basic Integration

**.github/workflows/security-scan.yml**

```yaml
name: Security Scan

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  security-scan:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        
    - name: Cache Gradle dependencies
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
          
    - name: Make gradlew executable
      run: chmod +x ./gradlew
      
    - name: Run security scan
      run: ./gradlew scanForSecrets --no-configuration-cache
      
    - name: Upload security reports
      if: always()
      uses: actions/upload-artifact@v3
      with:
        name: security-reports
        path: build/reports/scan/
        retention-days: 30
```

### Advanced GitHub Actions with Matrix

**.github/workflows/comprehensive-scan.yml**

```yaml
name: Comprehensive Security Scan

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]
  schedule:
    - cron: '0 2 * * *'

jobs:
  security-scan:
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
        java-version: [21, 22]
        
    runs-on: ${{ matrix.os }}
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK ${{ matrix.java-version }}
      uses: actions/setup-java@v4
      with:
        java-version: ${{ matrix.java-version }}
        distribution: 'temurin'
        
    - name: Run security scan
      run: ./gradlew scanForSecrets --no-configuration-cache
      
    - name: Generate security badge
      if: matrix.os == 'ubuntu-latest' && matrix.java-version == '21'
      run: |
        echo "SCAN_RESULT=passed" >> $GITHUB_ENV
        
    - name: Comment PR with results
      if: github.event_name == 'pull_request'
      uses: actions/github-script@v6
      with:
        script: |
          const fs = require('fs');
          const path = 'build/reports/scan/scan-report.json';
          
          if (fs.existsSync(path)) {
            const report = JSON.parse(fs.readFileSync(path, 'utf8'));
            const comment = `## 🔍 Security Scan Results
            
            - **Files Scanned**: ${report.summary.filesScanned}
            - **Secrets Found**: ${report.summary.secretsFound}
            - **Critical Findings**: ${report.summary.criticalFindings}
            
            ${report.summary.secretsFound > 0 ? '❌ Security issues detected!' : '✅ No security issues found'}`;
            
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: comment
            });
          }
```

### GitHub Actions with Slack Notifications

**.github/workflows/scan-with-notifications.yml**

```yaml
name: Security Scan with Notifications

on:
  push:
    branches: [ main ]

jobs:
  security-scan:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        
    - name: Run security scan
      id: scan
      continue-on-error: true
      run: ./gradlew scanForSecrets --no-configuration-cache
      
    - name: Notify Slack on failure
      if: steps.scan.outcome == 'failure'
      uses: 8398a7/action-slack@v3
      with:
        status: failure
        text: '🚨 Security scan failed! Potential secrets detected in the codebase.'
        webhook_url: ${{ secrets.SLACK_WEBHOOK_URL }}
        
    - name: Notify Slack on success
      if: steps.scan.outcome == 'success'
      uses: 8398a7/action-slack@v3
      with:
        status: success
        text: '✅ Security scan passed! No secrets detected.'
        webhook_url: ${{ secrets.SLACK_WEBHOOK_URL }}
```

## Jenkins

### Declarative Pipeline

**Jenkinsfile**

```groovy
pipeline {
    agent any
    
    tools {
        jdk 'OpenJDK-21'
    }
    
    environment {
        GRADLE_OPTS = '-Dorg.gradle.daemon=false'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Security Scan') {
            steps {
                script {
                    try {
                        sh './gradlew scanForSecrets --no-configuration-cache --info'
                    } catch (Exception e) {
                        currentBuild.result = 'UNSTABLE'
                        error "Security scan failed: ${e.getMessage()}"
                    }
                }
            }
            post {
                always {
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'build/reports/scan',
                        reportFiles: 'scan-report.html',
                        reportName: 'Security Scan Report'
                    ])
                    
                    archiveArtifacts artifacts: 'build/reports/scan/**', allowEmptyArchive: true
                }
                failure {
                    emailext (
                        subject: "Security Scan Failed: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                        body: "Security scan detected potential secrets in the codebase. Please review the security report.",
                        recipientProviders: [developers(), requestor()]
                    )
                }
            }
        }
        
        stage('Build') {
            when {
                expression { currentBuild.result != 'UNSTABLE' }
            }
            steps {
                sh './gradlew build'
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
    }
}
```

### Scripted Pipeline with Advanced Features

**Jenkinsfile.advanced**

```groovy
node {
    def scanResults = [:]
    
    try {
        stage('Checkout') {
            checkout scm
        }
        
        stage('Security Scan') {
            sh './gradlew scanForSecrets --no-configuration-cache'
            
            // Parse scan results
            if (fileExists('build/reports/scan/scan-report.json')) {
                def report = readJSON file: 'build/reports/scan/scan-report.json'
                scanResults = [
                    filesScanned: report.summary.filesScanned,
                    secretsFound: report.summary.secretsFound,
                    criticalFindings: report.summary.criticalFindings
                ]
            }
        }
        
        stage('Quality Gate') {
            if (scanResults.criticalFindings > 0) {
                error "Security scan failed: ${scanResults.criticalFindings} critical findings detected"
            }
        }
        
        stage('Build') {
            sh './gradlew build'
        }
        
    } catch (Exception e) {
        currentBuild.result = 'FAILURE'
        
        // Send detailed notification
        slackSend(
            channel: '#security',
            color: 'danger',
            message: """🚨 Security Scan Failed
            
            Job: ${env.JOB_NAME}
            Build: ${env.BUILD_NUMBER}
            Files Scanned: ${scanResults.filesScanned ?: 'Unknown'}
            Secrets Found: ${scanResults.secretsFound ?: 'Unknown'}
            
            Build URL: ${env.BUILD_URL}"""
        )
        
        throw e
    } finally {
        // Always publish reports
        publishHTML([
            allowMissing: false,
            alwaysLinkToLastBuild: true,
            keepAll: true,
            reportDir: 'build/reports/scan',
            reportFiles: 'scan-report.html',
            reportName: 'Security Scan Report'
        ])
    }
}
```

## GitLab CI

### Basic GitLab CI Configuration

**.gitlab-ci.yml**

```yaml
stages:
  - security
  - build
  - test

variables:
  GRADLE_OPTS: "-Dorg.gradle.daemon=false"
  GRADLE_USER_HOME: "$CI_PROJECT_DIR/.gradle"

cache:
  paths:
    - .gradle/wrapper
    - .gradle/caches

before_script:
  - chmod +x ./gradlew

security-scan:
  stage: security
  image: openjdk:21-jdk
  script:
    - ./gradlew scanForSecrets --no-configuration-cache
  artifacts:
    when: always
    reports:
      junit: build/reports/scan/scan-report.xml
    paths:
      - build/reports/scan/
    expire_in: 1 week
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'

build:
  stage: build
  image: openjdk:21-jdk
  script:
    - ./gradlew build
  dependencies:
    - security-scan
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'
```

### Advanced GitLab CI with Security Dashboard

**.gitlab-ci.advanced.yml**

```yaml
include:
  - template: Security/SAST.gitlab-ci.yml

stages:
  - security
  - build
  - deploy

variables:
  GRADLE_OPTS: "-Dorg.gradle.daemon=false"

.gradle_cache: &gradle_cache
  cache:
    key: "$CI_COMMIT_REF_SLUG"
    paths:
      - .gradle/wrapper
      - .gradle/caches

security-scan:
  <<: *gradle_cache
  stage: security
  image: openjdk:21-jdk
  script:
    - ./gradlew scanForSecrets --no-configuration-cache
    - |
      if [ -f "build/reports/scan/scan-report.json" ]; then
        SECRETS_COUNT=$(jq '.summary.secretsFound' build/reports/scan/scan-report.json)
        echo "SECRETS_FOUND=$SECRETS_COUNT" >> scan.env
        
        if [ "$SECRETS_COUNT" -gt 0 ]; then
          echo "🚨 $SECRETS_COUNT potential secrets detected!"
          exit 1
        else
          echo "✅ No secrets detected"
        fi
      fi
  artifacts:
    when: always
    reports:
      dotenv: scan.env
    paths:
      - build/reports/scan/
    expire_in: 1 week
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'

# Merge request integration
security-scan-mr-comment:
  stage: security
  image: alpine:latest
  before_script:
    - apk add --no-cache curl jq
  script:
    - |
      if [ -f "build/reports/scan/scan-report.json" ]; then
        REPORT=$(cat build/reports/scan/scan-report.json)
        FILES_SCANNED=$(echo $REPORT | jq '.summary.filesScanned')
        SECRETS_FOUND=$(echo $REPORT | jq '.summary.secretsFound')
        
        COMMENT="## 🔍 Security Scan Results\n\n"
        COMMENT="${COMMENT}- **Files Scanned**: $FILES_SCANNED\n"
        COMMENT="${COMMENT}- **Secrets Found**: $SECRETS_FOUND\n\n"
        
        if [ "$SECRETS_FOUND" -gt 0 ]; then
          COMMENT="${COMMENT}❌ **Security issues detected!** Please review and fix before merging."
        else
          COMMENT="${COMMENT}✅ **No security issues found.** Safe to merge."
        fi
        
        curl --request POST \
          --header "PRIVATE-TOKEN: $CI_JOB_TOKEN" \
          --data "body=$COMMENT" \
          "$CI_API_V4_URL/projects/$CI_PROJECT_ID/merge_requests/$CI_MERGE_REQUEST_IID/notes"
      fi
  dependencies:
    - security-scan
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
```

## Azure DevOps

### Azure Pipelines YAML

**azure-pipelines.yml**

```yaml
trigger:
  branches:
    include:
    - main
    - develop

pr:
  branches:
    include:
    - main

pool:
  vmImage: 'ubuntu-latest'

variables:
  GRADLE_USER_HOME: $(Pipeline.Workspace)/.gradle

stages:
- stage: Security
  displayName: 'Security Scan'
  jobs:
  - job: ScanForSecrets
    displayName: 'Scan for Secrets'
    steps:
    - task: Cache@2
      inputs:
        key: 'gradle | "$(Agent.OS)" | **/gradle-wrapper.properties'
        restoreKeys: |
          gradle | "$(Agent.OS)"
          gradle
        path: $(GRADLE_USER_HOME)
      displayName: Cache Gradle packages
      
    - task: JavaToolInstaller@0
      inputs:
        versionSpec: '21'
        jdkArchitectureOption: 'x64'
        jdkSourceOption: 'PreInstalled'
      displayName: 'Set up JDK 21'
      
    - script: |
        chmod +x ./gradlew
        ./gradlew scanForSecrets --no-configuration-cache
      displayName: 'Run Security Scan'
      continueOnError: true
      
    - task: PublishTestResults@2
      condition: always()
      inputs:
        testResultsFormat: 'JUnit'
        testResultsFiles: 'build/reports/scan/scan-report.xml'
        failTaskOnFailedTests: true
      displayName: 'Publish Security Scan Results'
      
    - task: PublishHtmlReport@1
      condition: always()
      inputs:
        reportDir: 'build/reports/scan'
        tabName: 'Security Scan'
      displayName: 'Publish Security Report'

- stage: Build
  displayName: 'Build'
  dependsOn: Security
  condition: succeeded()
  jobs:
  - job: BuildProject
    displayName: 'Build Project'
    steps:
    - script: ./gradlew build
      displayName: 'Build'
```

## CircleCI

### CircleCI Configuration

**.circleci/config.yml**

```yaml
version: 2.1

orbs:
  gradle: circleci/gradle@3.0.0

executors:
  jdk-executor:
    docker:
      - image: cimg/openjdk:21.0
    working_directory: ~/project

jobs:
  security-scan:
    executor: jdk-executor
    steps:
      - checkout
      - gradle/with_cache:
          steps:
            - run:
                name: Run Security Scan
                command: ./gradlew scanForSecrets --no-configuration-cache
      - store_artifacts:
          path: build/reports/scan
          destination: security-reports
      - store_test_results:
          path: build/reports/scan

  build:
    executor: jdk-executor
    steps:
      - checkout
      - gradle/with_cache:
          steps:
            - run:
                name: Build Project
                command: ./gradlew build

workflows:
  security-and-build:
    jobs:
      - security-scan
      - build:
          requires:
            - security-scan
```

## Docker Integration

### Dockerfile for Security Scanning

**Dockerfile.security**

```dockerfile
FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradle/ gradle/
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Copy source code
COPY src/ src/

# Make gradlew executable
RUN chmod +x ./gradlew

# Run security scan
RUN ./gradlew scanForSecrets --no-configuration-cache

# Build if scan passes
RUN ./gradlew build

# Set entrypoint
ENTRYPOINT ["java", "-jar", "build/libs/app.jar"]
```

### Docker Compose with Security Scanning

**docker-compose.security.yml**

```yaml
version: '3.8'

services:
  security-scanner:
    build:
      context: .
      dockerfile: Dockerfile.security
    volumes:
      - ./build/reports:/app/build/reports
    environment:
      - GRADLE_OPTS=-Dorg.gradle.daemon=false
    command: |
      sh -c "
        ./gradlew scanForSecrets --no-configuration-cache &&
        echo 'Security scan completed successfully'
      "

  app:
    build: .
    depends_on:
      - security-scanner
    ports:
      - "8080:8080"
```

## Gradle Composite Builds

### Multi-Module Security Scanning

**settings.gradle.kts**

```kotlin
rootProject.name = "my-application"

include(
    ":core",
    ":api",
    ":web",
    ":security"
)

// Configure SCAN for all modules
gradle.beforeProject { project ->
    project.apply(plugin = "io.github.theaniketraj.scan")
    
    project.extensions.configure<com.scan.plugin.ScanExtension> {
        failOnSecrets = System.getenv("CI")?.toBoolean() ?: false
        generateJsonReport = true
        
        // Module-specific configuration
        when (project.name) {
            "security" -> {
                strictMode = true
                entropyThreshold = 4.0
            }
            "core" -> {
                customPatterns = listOf(
                    "CORE_API_[A-Z0-9]{32}",
                    "INTERNAL_SECRET_.*"
                )
            }
        }
    }
}
```

### Root Build Script for Composite Security

**build.gradle.kts**

```kotlin
plugins {
    id("io.github.theaniketraj.scan") version "1.0.0" apply false
}

// Configure security scanning for all projects
allprojects {
    apply(plugin = "io.github.theaniketraj.scan")
    
    scan {
        failOnSecrets = System.getenv("CI")?.toBoolean() ?: false
        generateHtmlReport = true
        
        // Aggregate reports
        reportOutputDir = rootProject.layout.buildDirectory.dir("reports/security/${project.name}")
    }
}

// Aggregate security scan task
tasks.register("scanAllModules") {
    group = "security"
    description = "Run security scan on all modules"
    
    dependsOn(subprojects.map { "${it.path}:scanForSecrets" })
    
    doLast {
        println("Security scan completed for all modules")
        
        // Aggregate results
        val allReports = subprojects.mapNotNull { project ->
            val reportFile = project.layout.buildDirectory.file("reports/scan/scan-report.json").get().asFile
            if (reportFile.exists()) {
                project.name to reportFile.readText()
            } else null
        }
        
        println("Modules scanned: ${allReports.size}")
    }
}
```

## Environment-Specific Configuration

### Production Deployment Pipeline

**deploy-pipeline.yml**

```yaml
name: Production Deployment

on:
  push:
    tags:
      - 'v*'

jobs:
  security-audit:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
    
    # Strict security scan for production
    - name: Production Security Scan
      run: |
        ./gradlew scanForSecrets --no-configuration-cache \
          -Pscan.strictMode=true \
          -Pscan.entropyThreshold=3.5 \
          -Pscan.failOnSecrets=true
    
    - name: Upload audit report
      uses: actions/upload-artifact@v3
      with:
        name: production-security-audit
        path: build/reports/scan/
        retention-days: 90

  deploy:
    needs: security-audit
    runs-on: ubuntu-latest
    steps:
    - name: Deploy to production
      run: echo "Deploying secure application to production"
```

## Monitoring and Alerting

### Slack Integration Script

**scripts/notify-slack.sh**

```bash
#!/bin/bash

REPORT_FILE="build/reports/scan/scan-report.json"
WEBHOOK_URL="$SLACK_WEBHOOK_URL"

if [ -f "$REPORT_FILE" ]; then
    SECRETS_COUNT=$(jq '.summary.secretsFound' "$REPORT_FILE")
    FILES_SCANNED=$(jq '.summary.filesScanned' "$REPORT_FILE")
    
    if [ "$SECRETS_COUNT" -gt 0 ]; then
        MESSAGE="🚨 *Security Alert*: $SECRETS_COUNT potential secrets detected in $FILES_SCANNED files!"
        COLOR="danger"
    else
        MESSAGE="✅ *Security Check*: No secrets detected in $FILES_SCANNED files"
        COLOR="good"
    fi
    
    curl -X POST -H 'Content-type: application/json' \
        --data "{\"text\":\"$MESSAGE\", \"color\":\"$COLOR\"}" \
        "$WEBHOOK_URL"
fi
```

---

These examples provide comprehensive CI/CD integration patterns for the SCAN plugin across different platforms and scenarios. Choose the configuration that best fits your infrastructure and security requirements.
