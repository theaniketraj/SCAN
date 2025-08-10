package com.scan.reporting

import com.scan.core.ScanResult
import java.io.File

/**
 * HTML reporter that generates comprehensive HTML reports for scan results.
 * 
 * This reporter creates HTML output with detailed finding information, 
 * severity analysis, and interactive features for easy review.
 */
class HtmlReporter {

    /**
     * Generates an HTML report from scan results.
     *
     * @param scanResult The scan result to generate a report for
     * @param outputFile Output file for the HTML report
     */
    suspend fun generateReport(scanResult: ScanResult, outputFile: File) {
        val htmlContent = createHtmlReport(scanResult)
        
        try {
            outputFile.parentFile?.mkdirs()
            outputFile.writeText(htmlContent)
        } catch (e: Exception) {
            throw ReportGenerationException("Failed to write HTML report to ${outputFile.absolutePath}", e)
        }
    }

    /**
     * Generates an HTML report from scan results (synchronous version).
     *
     * @param scanResult The scan result to generate a report for
     * @param outputFile Output file for the HTML report
     */
    fun generateReportSync(scanResult: ScanResult, outputFile: File) {
        val htmlContent = createHtmlReport(scanResult)
        
        try {
            outputFile.parentFile?.mkdirs()
            outputFile.writeText(htmlContent)
        } catch (e: Exception) {
            throw ReportGenerationException("Failed to write HTML report to ${outputFile.absolutePath}", e)
        }
    }

    private fun createHtmlReport(scanResult: ScanResult): String {
        val findings = scanResult.findings
        val timestamp = java.time.LocalDateTime.now()
        
        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html lang=\"en\">")
            appendLine("<head>")
            appendLine("    <meta charset=\"UTF-8\">")
            appendLine("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
            appendLine("    <title>SCAN Security Report</title>")
            appendLine("    <style>")
            appendLine(getCSS())
            appendLine("    </style>")
            appendLine("</head>")
            appendLine("<body>")
            appendLine("    <div class=\"container\">")
            appendLine("        <header>")
            appendLine("            <h1>🔍 SCAN Security Report</h1>")
            appendLine("            <p>Generated on ${timestamp}</p>")
            appendLine("        </header>")
            appendLine()
            
            // Summary section
            appendLine("        <section class=\"summary\">")
            appendLine("            <h2>Summary</h2>")
            appendLine("            <div class=\"stats\">")
            appendLine("                <div class=\"stat-item\">")
            appendLine("                    <span class=\"stat-number\">${findings.size}</span>")
            appendLine("                    <span class=\"stat-label\">Total Findings</span>")
            appendLine("                </div>")
            appendLine("                <div class=\"stat-item\">")
            appendLine("                    <span class=\"stat-number\">${scanResult.summary.totalFilesScanned}</span>")
            appendLine("                    <span class=\"stat-label\">Files Scanned</span>")
            appendLine("                </div>")
            appendLine("            </div>")
            appendLine("        </section>")
            appendLine()
            
            // Findings section
            if (findings.isNotEmpty()) {
                appendLine("        <section class=\"findings\">")
                appendLine("            <h2>Findings Details</h2>")
                
                findings.forEachIndexed { index, finding ->
                    val severityClass = finding.severity.name.lowercase()
                    appendLine("            <div class=\"finding finding-$severityClass\">")
                    appendLine("                <div class=\"finding-header\">")
                    appendLine("                    <span class=\"finding-number\">#${index + 1}</span>")
                    appendLine("                    <span class=\"severity-badge severity-$severityClass\">${finding.severity.name}</span>")
                    appendLine("                    <h3>${escapeHtml(finding.description)}</h3>")
                    appendLine("                </div>")
                    appendLine("                <div class=\"finding-details\">")
                    appendLine("                    <p><strong>File:</strong> ${escapeHtml(finding.location.filePath)}</p>")
                    appendLine("                    <p><strong>Line:</strong> ${finding.location.lineNumber}</p>")
                    appendLine("                    <p><strong>Column:</strong> ${finding.location.columnStart}-${finding.location.columnEnd}</p>")
                    if (finding.secretInfo.patternName.isNotEmpty()) {
                        appendLine("                    <p><strong>Pattern:</strong> ${escapeHtml(finding.secretInfo.patternName)}</p>")
                    }
                    appendLine("                    <div class=\"code-snippet\">")
                    appendLine("                        <pre><code>${escapeHtml(finding.context.lineContent)}</code></pre>")
                    appendLine("                    </div>")
                    appendLine("                </div>")
                    appendLine("            </div>")
                }
                
                appendLine("        </section>")
            } else {
                appendLine("        <section class=\"no-findings\">")
                appendLine("            <h2>✅ No Security Issues Found</h2>")
                appendLine("            <p>Your codebase appears to be free of detectable security issues.</p>")
                appendLine("        </section>")
            }
            
            appendLine("    </div>")
            appendLine("</body>")
            appendLine("</html>")
        }
    }

    private fun getCSS(): String {
        return """
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                line-height: 1.6;
                color: #333;
                background-color: #f5f5f5;
            }
            
            .container {
                max-width: 1200px;
                margin: 0 auto;
                padding: 20px;
            }
            
            header {
                background: white;
                padding: 30px;
                border-radius: 8px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                margin-bottom: 20px;
                text-align: center;
            }
            
            header h1 {
                color: #2c3e50;
                margin-bottom: 10px;
            }
            
            .summary {
                background: white;
                padding: 30px;
                border-radius: 8px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                margin-bottom: 20px;
            }
            
            .stats {
                display: flex;
                justify-content: space-around;
                margin-top: 20px;
            }
            
            .stat-item {
                text-align: center;
            }
            
            .stat-number {
                display: block;
                font-size: 2.5rem;
                font-weight: bold;
                color: #3498db;
            }
            
            .stat-label {
                color: #7f8c8d;
                font-size: 0.9rem;
            }
            
            .findings {
                background: white;
                padding: 30px;
                border-radius: 8px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            }
            
            .finding {
                margin-bottom: 25px;
                border-left: 4px solid #ddd;
                padding-left: 20px;
            }
            
            .finding-critical {
                border-left-color: #e74c3c;
            }
            
            .finding-high {
                border-left-color: #f39c12;
            }
            
            .finding-medium {
                border-left-color: #f1c40f;
            }
            
            .finding-low {
                border-left-color: #2ecc71;
            }
            
            .finding-info {
                border-left-color: #3498db;
            }
            
            .finding-header {
                display: flex;
                align-items: center;
                margin-bottom: 15px;
            }
            
            .finding-number {
                background: #ecf0f1;
                color: #2c3e50;
                padding: 5px 10px;
                border-radius: 4px;
                font-weight: bold;
                margin-right: 15px;
            }
            
            .severity-badge {
                padding: 4px 12px;
                border-radius: 12px;
                font-size: 0.8rem;
                font-weight: bold;
                text-transform: uppercase;
                margin-right: 15px;
            }
            
            .severity-critical {
                background: #e74c3c;
                color: white;
            }
            
            .severity-high {
                background: #f39c12;
                color: white;
            }
            
            .severity-medium {
                background: #f1c40f;
                color: #2c3e50;
            }
            
            .severity-low {
                background: #2ecc71;
                color: white;
            }
            
            .severity-info {
                background: #3498db;
                color: white;
            }
            
            .finding-details p {
                margin-bottom: 8px;
            }
            
            .code-snippet {
                background: #2c3e50;
                color: #ecf0f1;
                padding: 15px;
                border-radius: 4px;
                margin-top: 15px;
                overflow-x: auto;
            }
            
            .code-snippet pre {
                margin: 0;
            }
            
            .no-findings {
                background: white;
                padding: 60px 30px;
                border-radius: 8px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                text-align: center;
            }
            
            .no-findings h2 {
                color: #27ae60;
                margin-bottom: 15px;
            }
        """.trimIndent()
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;")
    }
}