import React from "react"
import DocsLayout from "../../../components/DocsLayout"

export default function CIPage() {
    const sections = [
        { id: "overview", title: "Overview" },
        { id: "github-actions", title: "GitHub Actions" },
        { id: "jenkins", title: "Jenkins" },
        { id: "gitlab-ci", title: "GitLab CI" },
        { id: "azure-devops", title: "Azure DevOps" },
        { id: "circle-ci", title: "CircleCI" },
        { id: "teamcity", title: "TeamCity" },
        { id: "best-practices", title: "Best Practices" },
        { id: "troubleshooting", title: "Troubleshooting" }
    ]

    return (
        <DocsLayout sections={sections} title="CI/CD Integration">
            <div className="prose prose-lg dark:prose-invert max-w-none">
                <h1>CI/CD Integration</h1>
                <p className="text-xl text-gray-600 dark:text-gray-300">
                    Integrate SCAN into your continuous integration and deployment pipelines to automatically detect secrets and sensitive data.
                </p>

                <section id="overview">
                    <h2>Overview</h2>
                    <p>
                        SCAN can be easily integrated into various CI/CD platforms to provide automatic security scanning 
                        as part of your build pipeline. This ensures that secrets and sensitive data are caught before 
                        they reach production.
                    </p>
                    
                    <div className="bg-blue-50 dark:bg-blue-900/30 border border-blue-200 dark:border-blue-700 rounded-lg p-4 my-6">
                        <h3 className="text-blue-800 dark:text-blue-200 text-lg font-semibold mb-2">Key Benefits</h3>
                        <ul className="text-blue-700 dark:text-blue-300 mb-0">
                            <li>Automated security scanning on every commit</li>
                            <li>Fail builds when secrets are detected</li>
                            <li>Generate security reports for compliance</li>
                            <li>Integrate with existing workflows seamlessly</li>
                        </ul>
                    </div>
                </section>

                <section id="github-actions">
                    <h2>GitHub Actions</h2>
                    <p>
                        Integration with GitHub Actions is straightforward using the standard Gradle wrapper approach.
                    </p>

                    <h3>Basic Workflow</h3>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`name: Security Scan

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  security-scan:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Setup Gradle
      uses: gradle/gradle-build-action@v2
      
    - name: Run SCAN Security Check
      run: ./gradlew scanForSecrets
      
    - name: Upload SCAN Results
      uses: actions/upload-artifact@v4
      if: always()
      with:
        name: scan-results
        path: scan-results/`}</code></pre>

                    <h3>Advanced Configuration</h3>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`    - name: Run SCAN with Custom Config
      run: |
        ./gradlew scanForSecrets \\
          --config-file=ci/scan-config.yml \\
          --output-format=json \\
          --fail-on-secrets=true
          
    - name: Comment PR with Results
      if: github.event_name == 'pull_request'
      uses: actions/github-script@v7
      with:
        script: |
          const fs = require('fs');
          const resultsPath = 'scan-results/scan-report.json';
          if (fs.existsSync(resultsPath)) {
            const results = JSON.parse(fs.readFileSync(resultsPath, 'utf8'));
            // Process and comment results
          }`}</code></pre>
                </section>

                <section id="jenkins">
                    <h2>Jenkins</h2>
                    <p>
                        Jenkins integration can be achieved through pipeline scripts or traditional job configurations.
                    </p>

                    <h3>Declarative Pipeline</h3>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`pipeline {
    agent any
    
    tools {
        jdk 'JDK17'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Security Scan') {
            steps {
                sh './gradlew scanForSecrets'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'scan-results/**/*', 
                                   allowEmptyArchive: true
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'scan-results',
                        reportFiles: 'scan-report.html',
                        reportName: 'SCAN Security Report'
                    ])
                }
                failure {
                    emailext (
                        subject: "Security Scan Failed: \${env.JOB_NAME} - \${env.BUILD_NUMBER}",
                        body: "Security vulnerabilities detected. Check the report for details.",
                        to: "\${env.CHANGE_AUTHOR_EMAIL}"
                    )
                }
            }
        }
    }
}`}</code></pre>

                    <h3>Scripted Pipeline</h3>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`node {
    stage('Checkout') {
        checkout scm
    }
    
    stage('Security Scan') {
        try {
            sh './gradlew scanForSecrets --continue'
        } catch (Exception e) {
            currentBuild.result = 'UNSTABLE'
            echo "Security scan completed with findings"
        } finally {
            archiveArtifacts artifacts: 'scan-results/**/*'
        }
    }
}`}</code></pre>
                </section>

                <section id="gitlab-ci">
                    <h2>GitLab CI</h2>
                    <p>
                        GitLab CI integration using the <code>.gitlab-ci.yml</code> configuration file.
                    </p>

                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`stages:
  - security

variables:
  GRADLE_OPTS: "-Dorg.gradle.daemon=false"
  
security_scan:
  stage: security
  image: openjdk:17-jdk-alpine
  
  before_script:
    - chmod +x ./gradlew
    
  script:
    - ./gradlew scanForSecrets
    
  artifacts:
    when: always
    paths:
      - scan-results/
    reports:
      junit: scan-results/scan-report.xml
    expire_in: 1 week
    
  rules:
    - if: $CI_PIPELINE_SOURCE == "push"
    - if: $CI_PIPELINE_SOURCE == "merge_request_event"

# Security scan for merge requests
security_scan_mr:
  extends: security_scan
  script:
    - ./gradlew scanForSecrets --output-format=json
    - |
      if [ -f "scan-results/scan-report.json" ]; then
        echo "Security scan results:"
        cat scan-results/scan-report.json
      fi
  only:
    - merge_requests`}</code></pre>
                </section>

                <section id="azure-devops">
                    <h2>Azure DevOps</h2>
                    <p>
                        Integration with Azure DevOps using YAML pipelines.
                    </p>

                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`trigger:
  branches:
    include:
      - main
      - develop

pool:
  vmImage: 'ubuntu-latest'

variables:
  GRADLE_USER_HOME: $(Pipeline.Workspace)/.gradle

steps:
- task: JavaToolInstaller@0
  inputs:
    versionSpec: '17'
    jdkArchitectureOption: 'x64'
    jdkSourceOption: 'PreInstalled'

- task: Cache@2
  inputs:
    key: 'gradle | "$(Agent.OS)" | **/gradle-wrapper.properties'
    restoreKeys: gradle
    path: $(GRADLE_USER_HOME)
  displayName: Gradle build cache

- task: Gradle@2
  inputs:
    workingDirectory: ''
    gradleWrapperFile: 'gradlew'
    gradleOptions: '-Xmx3072m'
    javaHomeOption: 'JDKVersion'
    jdkVersionOption: '1.17'
    jdkArchitectureOption: 'x64'
    publishJUnitResults: false
    tasks: 'scanForSecrets'
  displayName: 'Run Security Scan'

- task: PublishBuildArtifacts@1
  condition: always()
  inputs:
    pathToPublish: 'scan-results'
    artifactName: 'security-scan-results'
  displayName: 'Publish Security Results'

- task: PublishTestResults@2
  condition: succeededOrFailed()
  inputs:
    testResultsFormat: 'JUnit'
    testResultsFiles: 'scan-results/scan-report.xml'
    testRunTitle: 'Security Scan Results'`}</code></pre>
                </section>

                <section id="circle-ci">
                    <h2>CircleCI</h2>
                    <p>
                        CircleCI configuration using the <code>.circleci/config.yml</code> file.
                    </p>

                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`version: 2.1

orbs:
  gradle: circleci/gradle@2.2.0

jobs:
  security-scan:
    docker:
      - image: cimg/openjdk:17.0
    
    steps:
      - checkout
      
      - gradle/with_cache:
          steps:
            - run:
                name: Run Security Scan
                command: ./gradlew scanForSecrets
                
      - store_artifacts:
          path: scan-results
          destination: security-reports
          
      - store_test_results:
          path: scan-results

workflows:
  main:
    jobs:
      - security-scan:
          filters:
            branches:
              only:
                - main
                - develop`}</code></pre>
                </section>

                <section id="teamcity">
                    <h2>TeamCity</h2>
                    <p>
                        TeamCity integration through build configuration and build steps.
                    </p>

                    <h3>Build Step Configuration</h3>
                    <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-4 my-4">
                        <h4 className="font-semibold mb-2">Step 1: Gradle Build Step</h4>
                        <ul className="space-y-1 text-sm">
                            <li><strong>Runner type:</strong> Gradle</li>
                            <li><strong>Gradle tasks:</strong> scanForSecrets</li>
                            <li><strong>Gradle home path:</strong> Use Gradle wrapper</li>
                            <li><strong>Additional command line parameters:</strong> --info</li>
                        </ul>
                    </div>

                    <h3>Kotlin DSL Configuration</h3>
                    <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object SecurityScan : BuildType({
    name = "Security Scan"
    
    vcs {
        root(DslContext.settingsRoot)
    }
    
    steps {
        gradle {
            tasks = "scanForSecrets"
            useGradleWrapper = true
            gradleParams = "--info --stacktrace"
        }
    }
    
    triggers {
        vcs {
            branchFilter = "+:*"
        }
    }
    
    features {
        publishArtifacts {
            artifactRules = "scan-results/** => security-reports.zip"
        }
    }
})`}</code></pre>
                </section>

                <section id="best-practices">
                    <h2>Best Practices</h2>
                    
                    <h3>Pipeline Configuration</h3>
                    <div className="grid gap-4 md:grid-cols-2 my-6">
                        <div className="bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-700 rounded-lg p-4">
                            <h4 className="text-green-800 dark:text-green-200 font-semibold mb-2">✅ Do</h4>
                            <ul className="text-green-700 dark:text-green-300 text-sm space-y-1 mb-0">
                                <li>Run scans on every pull request</li>
                                <li>Cache Gradle dependencies</li>
                                <li>Store scan results as artifacts</li>
                                <li>Use appropriate JDK versions</li>
                                <li>Configure proper timeout values</li>
                            </ul>
                        </div>
                        <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-700 rounded-lg p-4">
                            <h4 className="text-red-800 dark:text-red-200 font-semibold mb-2">❌ Don&apos;t</h4>
                            <ul className="text-red-700 dark:text-red-300 text-sm space-y-1 mb-0">
                                <li>Ignore scan failures in production</li>
                                <li>Skip scans for &quot;urgent&quot; deployments</li>
                                <li>Store secrets in CI environment</li>
                                <li>Run scans without proper logging</li>
                                <li>Use overly permissive configurations</li>
                            </ul>
                        </div>
                    </div>

                    <h3>Security Considerations</h3>
                    <ul>
                        <li><strong>Credential Management:</strong> Use CI platform&apos;s secret management features</li>
                        <li><strong>Access Control:</strong> Limit who can modify scan configurations</li>
                        <li><strong>Result Storage:</strong> Ensure scan results are stored securely</li>
                        <li><strong>Notification:</strong> Set up alerts for security violations</li>
                    </ul>

                    <h3>Performance Optimization</h3>
                    <ul>
                        <li><strong>Parallel Execution:</strong> Use Gradle&apos;s parallel execution features</li>
                        <li><strong>Incremental Scanning:</strong> Configure for changed files only</li>
                        <li><strong>Build Caching:</strong> Enable Gradle build cache for faster builds</li>
                        <li><strong>Resource Limits:</strong> Set appropriate memory and CPU limits</li>
                    </ul>
                </section>

                <section id="troubleshooting">
                    <h2>Troubleshooting</h2>
                    
                    <h3>Common Issues</h3>
                    
                    <div className="space-y-4">
                        <div className="border border-gray-200 dark:border-gray-700 rounded-lg p-4">
                            <h4 className="font-semibold text-red-600 dark:text-red-400">Build Timeout</h4>
                            <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
                                Solution: Increase timeout values or optimize scan configuration
                            </p>
                            <pre className="bg-gray-900 text-white p-2 rounded text-xs mt-2"><code>timeout: 30m  # Increase timeout in CI configuration</code></pre>
                        </div>

                        <div className="border border-gray-200 dark:border-gray-700 rounded-lg p-4">
                            <h4 className="font-semibold text-red-600 dark:text-red-400">Memory Issues</h4>
                            <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
                                Solution: Increase JVM heap size
                            </p>
                            <pre className="bg-gray-900 text-white p-2 rounded text-xs mt-2"><code>export GRADLE_OPTS=&quot;-Xmx4g -XX:MaxMetaspaceSize=1g&quot;</code></pre>
                        </div>

                        <div className="border border-gray-200 dark:border-gray-700 rounded-lg p-4">
                            <h4 className="font-semibold text-red-600 dark:text-red-400">False Positives</h4>
                            <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
                                Solution: Configure exclusions and custom patterns
                            </p>
                            <pre className="bg-gray-900 text-white p-2 rounded text-xs mt-2"><code>./gradlew scanForSecrets --exclude-paths=&quot;test/**&quot; --config-file=&quot;ci-config.yml&quot;</code></pre>
                        </div>
                    </div>

                    <h3>Debug Mode</h3>
                    <p>Enable debug output for detailed troubleshooting:</p>
                    <pre className="bg-gray-900 text-white p-3 rounded-lg"><code>./gradlew scanForSecrets --debug --stacktrace</code></pre>

                    <h3>Log Analysis</h3>
                    <p>Check these log sections for common issues:</p>
                    <ul>
                        <li><strong>Configuration Loading:</strong> Verify custom configs are loaded</li>
                        <li><strong>Pattern Matching:</strong> Check pattern compilation and matching</li>
                        <li><strong>File Processing:</strong> Monitor file scanning progress</li>
                        <li><strong>Report Generation:</strong> Ensure output files are created</li>
                    </ul>
                </section>
            </div>
        </DocsLayout>
    )
}