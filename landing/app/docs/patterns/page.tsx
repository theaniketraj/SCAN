import React from "react"
import Link from "next/link"
import DocsLayout from "../../../components/DocsLayout"

export default function PatternsPage() {
    const sections = [
        { id: "overview", title: "Overview" },
        { id: "built-in-patterns", title: "Built-in Patterns" },
        { id: "api-keys", title: "API Keys & Tokens" },
        { id: "databases", title: "Database Credentials" },
        { id: "cloud-services", title: "Cloud Services" },
        { id: "crypto", title: "Cryptographic Keys" },
        { id: "custom-patterns", title: "Custom Patterns" },
        { id: "pattern-files", title: "Pattern Files" },
        { id: "best-practices", title: "Best Practices" }
    ]

    return (
        <DocsLayout sections={sections} title="Pattern Reference">
            <div className="prose prose-lg dark:prose-invert max-w-none">
                <h1>SCAN Plugin Pattern Reference</h1>
                
                <p className="text-xl text-gray-600 dark:text-gray-300">
                    This document provides a comprehensive reference for all built-in patterns used by the SCAN plugin 
                    and guidance on creating custom patterns.
                </p>

                <h2 id="overview">Pattern Overview</h2>
                
                <p>
                    SCAN uses regular expressions (regex) to identify potential secrets in your codebase. The plugin includes 
                    over 50 built-in patterns covering common secret types, and you can add custom patterns for organization-specific secrets.
                </p>

                <h3>How Patterns Work</h3>
                <ol>
                    <li><strong>Pattern Matching</strong>: SCAN scans each file line by line, applying regex patterns</li>
                    <li><strong>Context Analysis</strong>: The surrounding code context is analyzed to reduce false positives</li>
                    <li><strong>Entropy Analysis</strong>: High-entropy strings are flagged even if they don&apos;t match specific patterns</li>
                    <li><strong>Confidence Scoring</strong>: Each finding is assigned a confidence level based on multiple factors</li>
                </ol>

                <h2 id="built-in-patterns">Built-in Patterns</h2>

                <h3>AWS Patterns</h3>

                <div className="space-y-4">
                    <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
                        <h4 className="text-red-800 dark:text-red-200 mt-0">AWS Access Key ID</h4>
                        <p className="text-red-700 dark:text-red-300"><strong>Pattern:</strong> <code>AKIA[0-9A-Z]{`{16}`}</code></p>
                        <p className="text-red-700 dark:text-red-300"><strong>Example:</strong> <code>AKIAIOSFODNN7EXAMPLE</code></p>
                        <p className="text-red-700 dark:text-red-300"><strong>Confidence:</strong> High</p>
                    </div>

                    <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
                        <h4 className="text-red-800 dark:text-red-200 mt-0">AWS Secret Access Key</h4>
                        <p className="text-red-700 dark:text-red-300"><strong>Pattern:</strong> <code>aws(.{`{0,20}`})?[&apos;&quot;][0-9a-zA-Z/+]{`{40}`}[&apos;&quot;]</code></p>
                        <p className="text-red-700 dark:text-red-300"><strong>Example:</strong> <code>aws_secret_access_key=&quot;wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY&quot;</code></p>
                        <p className="text-red-700 dark:text-red-300"><strong>Confidence:</strong> High</p>
                    </div>
                </div>

                <h3>Google Cloud Platform (GCP)</h3>

                <div className="space-y-4">
                    <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
                        <h4 className="text-red-800 dark:text-red-200 mt-0">GCP API Key</h4>
                        <p className="text-red-700 dark:text-red-300"><strong>Pattern:</strong> <code>AIza[0-9A-Za-z-_]{`{35}`}</code></p>
                        <p className="text-red-700 dark:text-red-300"><strong>Example:</strong> <code>AIzaSyDaGmWKa4JsXZ-HjGw7ISLn_3namBGewQe</code></p>
                        <p className="text-red-700 dark:text-red-300"><strong>Confidence:</strong> High</p>
                    </div>
                </div>

                <h3>Azure</h3>

                <div className="space-y-4">
                    <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
                        <h4 className="text-red-800 dark:text-red-200 mt-0">Azure Storage Account Key</h4>
                        <p className="text-red-700 dark:text-red-300"><strong>Pattern:</strong> <code>DefaultEndpointsProtocol=https;AccountName=.*;AccountKey=.*</code></p>
                        <p className="text-red-700 dark:text-red-300"><strong>Description:</strong> Azure storage connection strings</p>
                        <p className="text-red-700 dark:text-red-300"><strong>Confidence:</strong> High</p>
                    </div>
                </div>

                <h2 id="cloud-services">Cloud Services</h2>

                <h3>GitHub</h3>

                <div className="space-y-4">
                    <div className="bg-orange-50 dark:bg-orange-900/20 border border-orange-200 dark:border-orange-800 rounded-lg p-4">
                        <h4 className="text-orange-800 dark:text-orange-200 mt-0">GitHub Personal Access Token</h4>
                        <p className="text-orange-700 dark:text-orange-300"><strong>Pattern:</strong> <code>ghp_[A-Za-z0-9]{`{36}`}</code></p>
                        <p className="text-orange-700 dark:text-orange-300"><strong>Example:</strong> <code>ghp_1234567890abcdef1234567890abcdef12345678</code></p>
                        <p className="text-orange-700 dark:text-orange-300"><strong>Confidence:</strong> High</p>
                    </div>

                    <div className="bg-orange-50 dark:bg-orange-900/20 border border-orange-200 dark:border-orange-800 rounded-lg p-4">
                        <h4 className="text-orange-800 dark:text-orange-200 mt-0">GitHub OAuth Token</h4>
                        <p className="text-orange-700 dark:text-orange-300"><strong>Pattern:</strong> <code>gho_[A-Za-z0-9]{`{36}`}</code></p>
                        <p className="text-orange-700 dark:text-orange-300"><strong>Description:</strong> GitHub OAuth access tokens</p>
                        <p className="text-orange-700 dark:text-orange-300"><strong>Confidence:</strong> High</p>
                    </div>
                </div>

                <h3>GitLab</h3>

                <div className="space-y-4">
                    <div className="bg-orange-50 dark:bg-orange-900/20 border border-orange-200 dark:border-orange-800 rounded-lg p-4">
                        <h4 className="text-orange-800 dark:text-orange-200 mt-0">GitLab Personal Access Token</h4>
                        <p className="text-orange-700 dark:text-orange-300"><strong>Pattern:</strong> <code>glpat-[A-Za-z0-9-_]{`{20}`}</code></p>
                        <p className="text-orange-700 dark:text-orange-300"><strong>Description:</strong> GitLab personal access tokens</p>
                        <p className="text-orange-700 dark:text-orange-300"><strong>Confidence:</strong> High</p>
                    </div>
                </div>

                <h2 id="databases">Database Credentials</h2>

                <h3>Generic Database URLs</h3>

                <div className="space-y-4">
                    <div className="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg p-4">
                        <h4 className="text-yellow-800 dark:text-yellow-200 mt-0">Database Connection String</h4>
                        <p className="text-yellow-700 dark:text-yellow-300"><strong>Pattern:</strong> <code>(mysql|postgresql|mongodb|redis)://[^s]+:[^s]+@[^s]+</code></p>
                        <p className="text-yellow-700 dark:text-yellow-300"><strong>Example:</strong> <code>mysql://user:password@localhost:3306/database</code></p>
                        <p className="text-yellow-700 dark:text-yellow-300"><strong>Confidence:</strong> High</p>
                    </div>
                </div>

                <h2 id="api-keys">API Keys and Tokens</h2>

                <h3>Service-Specific APIs</h3>

                <div className="space-y-4">
                    <div className="bg-purple-50 dark:bg-purple-900/20 border border-purple-200 dark:border-purple-800 rounded-lg p-4">
                        <h4 className="text-purple-800 dark:text-purple-200 mt-0">Slack Token</h4>
                        <p className="text-purple-700 dark:text-purple-300"><strong>Pattern:</strong> <code>xox[baprs]-[A-Za-z0-9]{`{10,48}`}</code></p>
                        <p className="text-purple-700 dark:text-purple-300"><strong>Example:</strong> <code>xoxb-1234567890-1234567890-abcdefghijklmnopqrstuvwx</code></p>
                        <p className="text-purple-700 dark:text-purple-300"><strong>Confidence:</strong> High</p>
                    </div>

                    <div className="bg-purple-50 dark:bg-purple-900/20 border border-purple-200 dark:border-purple-800 rounded-lg p-4">
                        <h4 className="text-purple-800 dark:text-purple-200 mt-0">Stripe API Key</h4>
                        <p className="text-purple-700 dark:text-purple-300"><strong>Pattern:</strong> <code>sk_live_[A-Za-z0-9]{`{24}`}</code></p>
                        <p className="text-purple-700 dark:text-purple-300"><strong>Description:</strong> Stripe live API keys</p>
                        <p className="text-purple-700 dark:text-purple-300"><strong>Confidence:</strong> High</p>
                    </div>
                </div>

                <h2 id="crypto">Cryptographic Keys</h2>

                <div className="space-y-4">
                    <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
                        <h4 className="text-red-800 dark:text-red-200 mt-0">Private Key</h4>
                        <p className="text-red-700 dark:text-red-300"><strong>Pattern:</strong> <code>-----BEGIN [A-Z]+ PRIVATE KEY-----</code></p>
                        <p className="text-red-700 dark:text-red-300"><strong>Description:</strong> PEM-formatted private keys</p>
                        <p className="text-red-700 dark:text-red-300"><strong>Confidence:</strong> High</p>
                    </div>
                </div>

                <h2 id="custom-patterns">Custom Pattern Creation</h2>

                <h3>Basic Custom Patterns</h3>

                <p>Add custom patterns to catch organization-specific secrets:</p>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`scan {
    customPatterns = listOf(
        "MYCOMPANY_API_[A-Z0-9]{32}",
        "INTERNAL_SECRET_[a-f0-9]{64}",
        "PROD_KEY_[A-Za-z0-9\\\\-_]{40}"
    )
}`}</code></pre>

                <h3>Advanced Custom Patterns</h3>

                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`scan {
    customPatterns = listOf(
        // Match passwords in specific file types
        "(?i)password\\\\s*[=:]\\\\s*[\\\"'][^\\\"']{8,}[\\\"']",
        
        // Match API keys with specific prefixes
        "(?:api[_-]?key|apikey)\\\\s*[=:]\\\\s*[\\\"']?([A-Za-z0-9]{20,})[\\\"']?",
        
        // Organization-specific format
        "ACME_[A-Z]{2}_[0-9]{8}_[A-Za-z0-9]{16}"
    )
}`}</code></pre>

                <h2 id="pattern-files">Pattern Files</h2>
                
                <p>
                    You can organize custom patterns in separate YAML files for better maintainability:
                </p>

                <h3>patterns/custom-patterns.yml</h3>
                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`patterns:
  - name: "Company API Key"
    regex: "COMPANY_API_[A-Z0-9]{32}"
    confidence: "high"
    description: "Internal company API keys"
    
  - name: "Internal Secret"
    regex: "INTERNAL_SECRET_[A-Za-z0-9]{16,}"
    confidence: "medium"
    description: "Internal application secrets"`}</code></pre>

                <h3>Loading Pattern Files</h3>
                <pre className="bg-gray-900 text-white p-4 rounded-lg overflow-x-auto"><code>{`scan {
    customPatternsFile = "patterns/custom-patterns.yml"
}`}</code></pre>

                <h2 id="best-practices">Best Practices</h2>

                <h3>Pattern Design</h3>
                <ol>
                    <li><strong>Be Specific</strong>: Avoid overly broad patterns that cause false positives</li>
                    <li><strong>Use Anchors</strong>: Use <code>^</code> and <code>$</code> when matching entire lines</li>
                    <li><strong>Consider Context</strong>: Think about where the pattern might appear</li>
                    <li><strong>Test Thoroughly</strong>: Validate against real codebases</li>
                </ol>

                <h3>Common Pitfalls</h3>

                <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-6 my-6">
                    <h4 className="text-red-800 dark:text-red-200 mt-0">❌ Overly Broad Patterns</h4>
                    <pre className="bg-gray-900 text-white p-3 rounded mt-3 text-sm"><code>{`// Too broad - will match everything
".*password.*"

// Better - more specific
"password\\\\s*=\\\\s*[\\\"'][^\\\"']{8,}[\\\"']"`}</code></pre>
                </div>

                <div className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-6 my-6">
                    <h4 className="text-green-800 dark:text-green-200 mt-0">✅ Case Insensitive Patterns</h4>
                    <pre className="bg-gray-900 text-white p-3 rounded mt-3 text-sm"><code>{`// Case sensitive - might miss variations
"Password"

// Case insensitive
"(?i)password"`}</code></pre>
                </div>

                <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-6 my-8">
                    <h3 className="text-blue-800 dark:text-blue-200 mt-0">Related Documentation</h3>
                    <ul className="text-blue-700 dark:text-blue-300 space-y-2">
                        <li><Link href="/docs/configuration" className="hover:text-blue-900 dark:hover:text-blue-100">Configuration Reference</Link>: How to configure custom patterns</li>
                        <li><Link href="/docs/user-guide" className="hover:text-blue-900 dark:hover:text-blue-100">User Guide</Link>: Complete walkthrough with examples</li>
                        <li><Link href="/docs/ci" className="hover:text-blue-900 dark:hover:text-blue-100">CI/CD Examples</Link>: Pattern usage in automation</li>
                    </ul>
                </div>
            </div>
        </DocsLayout>
    )
}