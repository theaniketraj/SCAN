package com.scan.reporting

import com.scan.core.ScanConfiguration
import com.scan.core.ScanResult
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * HTML reporter that generates a comprehensive, interactive HTML report for security scan results
 * with charts, filtering, and detailed findings.
 */
class HtmlReporter : ReportGenerator {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override fun generateReport(
            results: List<ScanResult>,
            configuration: ScanConfiguration,
            outputFile: File
    ) {
        val htmlContent = buildHtmlReport(results, configuration)
        outputFile.writeText(htmlContent)
    }

    private fun buildHtmlReport(
            results: List<ScanResult>,
            configuration: ScanConfiguration
    ): String {
        val scanTime = LocalDateTime.now().format(dateFormatter)
        val totalFiles = results.size
        val filesWithIssues = results.count { it.findings.isNotEmpty() }
        val totalFindings = results.sumOf { it.findings.size }
        val severityStats = calculateSeverityStats(results)

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Security Scan Report - ${scanTime}</title>
    <style>
        ${getEmbeddedCSS()}
    </style>
</head>
<body>
    <div class="container">
        <header class="header">
            <h1>🔒 Security Scan Report</h1>
            <div class="scan-info">
                <span class="scan-time">Generated: ${scanTime}</span>
                <span class="scan-config">Configuration: ${configuration.name ?: "Default"}</span>
            </div>
        </header>
        
        <div class="summary-section">
            <h2>📊 Executive Summary</h2>
            <div class="stats-grid">
                <div class="stat-card ${if (totalFindings > 0) "critical" else "success"}">
                    <div class="stat-number">${totalFindings}</div>
                    <div class="stat-label">Total Issues Found</div>
                </div>
                <div class="stat-card">
                    <div class="stat-number">${filesWithIssues}</div>
                    <div class="stat-label">Files with Issues</div>
                </div>
                <div class="stat-card">
                    <div class="stat-number">${totalFiles}</div>
                    <div class="stat-label">Total Files Scanned</div>
                </div>
                <div class="stat-card">
                    <div class="stat-number">${String.format("%.1f", (filesWithIssues.toDouble() / totalFiles.toDouble()) * 100)}%</div>
                    <div class="stat-label">Issue Rate</div>
                </div>
            </div>
            
            <div class="severity-breakdown">
                <h3>Severity Breakdown</h3>
                <div class="severity-stats">
                    ${severityStats.map { (severity, count) -> 
                        """<div class="severity-item severity-${severity.lowercase()}">
                            <span class="severity-label">${severity}</span>
                            <span class="severity-count">${count}</span>
                        </div>"""
                    }.joinToString("")}
                </div>
            </div>
        </div>
        
        <div class="controls-section">
            <h2>🔍 Filter Controls</h2>
            <div class="filter-controls">
                <div class="filter-group">
                    <label for="severityFilter">Filter by Severity:</label>
                    <select id="severityFilter" onchange="filterResults()">
                        <option value="all">All Severities</option>
                        <option value="critical">Critical</option>
                        <option value="high">High</option>
                        <option value="medium">Medium</option>
                        <option value="low">Low</option>
                    </select>
                </div>
                
                <div class="filter-group">
                    <label for="typeFilter">Filter by Type:</label>
                    <select id="typeFilter" onchange="filterResults()">
                        <option value="all">All Types</option>
                        ${getUniqueTypes(results).map { type -> 
                            """<option value="${type}">${type}</option>"""
                        }.joinToString("")}
                    </select>
                </div>
                
                <div class="filter-group">
                    <label for="searchFilter">Search Files:</label>
                    <input type="text" id="searchFilter" placeholder="Search by filename..." onkeyup="filterResults()">
                </div>
                
                <button onclick="clearFilters()" class="clear-filters-btn">Clear Filters</button>
            </div>
        </div>
        
        <div class="results-section">
            <h2>📋 Detailed Results</h2>
            ${if (results.isEmpty() || totalFindings == 0) {
                """<div class="no-results">
                    <div class="success-icon">✅</div>
                    <h3>No Security Issues Found!</h3>
                    <p>Great job! Your codebase appears to be free of detectable security issues.</p>
                </div>"""
            } else {
                generateDetailedResults(results)
            }}
        </div>
        
        <footer class="footer">
            <p>Generated by SCAN Gradle Plugin | ${scanTime}</p>
            <p>Scanned ${totalFiles} files using ${configuration.detectors?.size ?: 0} detectors</p>
        </footer>
    </div>
    
    <script>
        ${getEmbeddedJavaScript()}
    </script>
</body>
</html>
        """.trimIndent()
    }

    private fun calculateSeverityStats(results: List<ScanResult>): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()
        results.flatMap { it.findings }.forEach { finding ->
            val severity = finding.severity ?: "UNKNOWN"
            stats[severity] = stats.getOrDefault(severity, 0) + 1
        }
        return stats.toSortedMap(compareByDescending { severityOrder(it) })
    }

    private fun severityOrder(severity: String): Int =
            when (severity.uppercase()) {
                "CRITICAL" -> 4
                "HIGH" -> 3
                "MEDIUM" -> 2
                "LOW" -> 1
                else -> 0
            }

    private fun getUniqueTypes(results: List<ScanResult>): Set<String> {
        return results.flatMap { it.findings }.mapNotNull { it.type }.toSet().sorted()
    }

    private fun generateDetailedResults(results: List<ScanResult>): String {
        val resultsWithFindings = results.filter { it.findings.isNotEmpty() }

        return resultsWithFindings
                .mapIndexed { index, result ->
                    val fileId = "file-${index}"
                    val findingsCount = result.findings.size
                    val highestSeverity =
                            result.findings
                                    .maxByOrNull { severityOrder(it.severity ?: "") }
                                    ?.severity
                                    ?: "UNKNOWN"

                    """
            <div class="file-result" data-file-id="${fileId}" data-severity="${highestSeverity.lowercase()}" data-filename="${result.filePath}">
                <div class="file-header" onclick="toggleFile('${fileId}')">
                    <div class="file-info">
                        <span class="file-path">${result.filePath}</span>
                        <span class="findings-count">${findingsCount} issue${if (findingsCount != 1) "s" else ""}</span>
                    </div>
                    <div class="file-severity severity-${highestSeverity.lowercase()}">
                        <span class="severity-badge">${highestSeverity}</span>
                        <span class="toggle-arrow" id="arrow-${fileId}">▼</span>
                    </div>
                </div>
                
                <div class="file-content" id="content-${fileId}">
                    ${result.findings.mapIndexed { findingIndex, finding ->
                        val findingId = "${fileId}-finding-${findingIndex}"
                        """
                        <div class="finding" data-type="${finding.type ?: "unknown"}">
                            <div class="finding-header">
                                <div class="finding-type">${finding.type ?: "Unknown"}</div>
                                <div class="finding-severity severity-${(finding.severity ?: "unknown").lowercase()}">
                                    ${finding.severity ?: "Unknown"}
                                </div>
                            </div>
                            
                            <div class="finding-details">
                                <div class="finding-location">
                                    <strong>Location:</strong> Line ${finding.lineNumber}, Column ${finding.columnNumber}
                                </div>
                                
                                ${if (finding.description != null) {
                                    """<div class="finding-description">
                                        <strong>Description:</strong> ${escapeHtml(finding.description)}
                                    </div>"""
                                } else ""}
                                
                                <div class="finding-match">
                                    <strong>Detected Pattern:</strong>
                                    <pre class="code-snippet">${escapeHtml(finding.matchedText ?: "")}</pre>
                                </div>
                                
                                ${if (finding.context != null) {
                                    """<div class="finding-context">
                                        <strong>Context:</strong>
                                        <pre class="code-context">${escapeHtml(finding.context)}</pre>
                                    </div>"""
                                } else ""}
                                
                                ${if (finding.confidence != null) {
                                    """<div class="finding-confidence">
                                        <strong>Confidence:</strong> ${String.format("%.1f", finding.confidence * 100)}%
                                        <div class="confidence-bar">
                                            <div class="confidence-fill" style="width: ${finding.confidence * 100}%"></div>
                                        </div>
                                    </div>"""
                                } else ""}
                                
                                ${if (finding.recommendations?.isNotEmpty() == true) {
                                    """<div class="finding-recommendations">
                                        <strong>Recommendations:</strong>
                                        <ul>
                                            ${finding.recommendations.map { recommendation ->
                                                "<li>${escapeHtml(recommendation)}</li>"
                                            }.joinToString("")}
                                        </ul>
                                    </div>"""
                                } else ""}
                            </div>
                        </div>
                        """
                    }.joinToString("")}
                </div>
            </div>
            """
                }
                .joinToString("")
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
    }

    private fun getEmbeddedCSS(): String =
            """
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

        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            border-radius: 12px;
            margin-bottom: 30px;
            text-align: center;
            box-shadow: 0 4px 20px rgba(0,0,0,0.1);
        }

        .header h1 {
            font-size: 2.5em;
            margin-bottom: 10px;
            font-weight: 700;
        }

        .scan-info {
            display: flex;
            justify-content: center;
            gap: 30px;
            font-size: 1.1em;
            opacity: 0.9;
        }

        .summary-section {
            background: white;
            padding: 30px;
            border-radius: 12px;
            margin-bottom: 30px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }

        .summary-section h2 {
            color: #333;
            margin-bottom: 20px;
            font-size: 1.8em;
        }

        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }

        .stat-card {
            background: #f8f9fa;
            padding: 25px;
            border-radius: 8px;
            text-align: center;
            border-left: 4px solid #007bff;
            transition: transform 0.2s;
        }

        .stat-card:hover {
            transform: translateY(-2px);
        }

        .stat-card.critical {
            border-left-color: #dc3545;
            background: #fff5f5;
        }

        .stat-card.success {
            border-left-color: #28a745;
            background: #f0fff4;
        }

        .stat-number {
            font-size: 2.5em;
            font-weight: bold;
            color: #333;
            margin-bottom: 5px;
        }

        .stat-label {
            font-size: 0.9em;
            color: #666;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .severity-breakdown h3 {
            margin-bottom: 15px;
            color: #333;
        }

        .severity-stats {
            display: flex;
            flex-wrap: wrap;
            gap: 15px;
        }

        .severity-item {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 8px 15px;
            border-radius: 20px;
            font-weight: 500;
        }

        .severity-critical {
            background: #fee;
            color: #c53030;
            border: 1px solid #fc8181;
        }

        .severity-high {
            background: #fef5e7;
            color: #dd6b20;
            border: 1px solid #f6ad55;
        }

        .severity-medium {
            background: #fffbeb;
            color: #d69e2e;
            border: 1px solid #f6d55c;
        }

        .severity-low {
            background: #f0fff4;
            color: #38a169;
            border: 1px solid #68d391;
        }

        .controls-section {
            background: white;
            padding: 25px;
            border-radius: 12px;
            margin-bottom: 30px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }

        .controls-section h2 {
            margin-bottom: 20px;
            color: #333;
        }

        .filter-controls {
            display: flex;
            flex-wrap: wrap;
            gap: 20px;
            align-items: end;
        }

        .filter-group {
            display: flex;
            flex-direction: column;
            gap: 5px;
        }

        .filter-group label {
            font-weight: 500;
            color: #555;
        }

        .filter-group select,
        .filter-group input {
            padding: 8px 12px;
            border: 1px solid #ddd;
            border-radius: 6px;
            font-size: 14px;
            min-width: 150px;
        }

        .clear-filters-btn {
            background: #6c757d;
            color: white;
            border: none;
            padding: 8px 16px;
            border-radius: 6px;
            cursor: pointer;
            font-size: 14px;
            transition: background 0.2s;
        }

        .clear-filters-btn:hover {
            background: #5a6268;
        }

        .results-section {
            background: white;
            padding: 30px;
            border-radius: 12px;
            margin-bottom: 30px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }

        .results-section h2 {
            margin-bottom: 25px;
            color: #333;
        }

        .no-results {
            text-align: center;
            padding: 60px 20px;
            color: #666;
        }

        .success-icon {
            font-size: 4em;
            margin-bottom: 20px;
        }

        .file-result {
            border: 1px solid #e9ecef;
            border-radius: 8px;
            margin-bottom: 20px;
            overflow: hidden;
            transition: box-shadow 0.2s;
        }

        .file-result:hover {
            box-shadow: 0 4px 12px rgba(0,0,0,0.1);
        }

        .file-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 15px 20px;
            background: #f8f9fa;
            cursor: pointer;
            transition: background 0.2s;
        }

        .file-header:hover {
            background: #e9ecef;
        }

        .file-info {
            display: flex;
            flex-direction: column;
            gap: 5px;
        }

        .file-path {
            font-family: 'Monaco', 'Menlo', monospace;
            font-weight: 500;
            color: #333;
        }

        .findings-count {
            font-size: 0.9em;
            color: #666;
        }

        .file-severity {
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .severity-badge {
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 0.8em;
            font-weight: 600;
            text-transform: uppercase;
        }

        .toggle-arrow {
            font-size: 1.2em;
            transition: transform 0.2s;
        }

        .toggle-arrow.rotated {
            transform: rotate(-90deg);
        }

        .file-content {
            padding: 0 20px 20px;
            display: block;
        }

        .file-content.hidden {
            display: none;
        }

        .finding {
            background: #f8f9fa;
            border-left: 4px solid #007bff;
            padding: 15px;
            margin-bottom: 15px;
            border-radius: 0 6px 6px 0;
        }

        .finding-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;
        }

        .finding-type {
            font-weight: 600;
            color: #333;
        }

        .finding-details {
            display: flex;
            flex-direction: column;
            gap: 10px;
            font-size: 0.9em;
        }

        .code-snippet,
        .code-context {
            background: #2d3748;
            color: #e2e8f0;
            padding: 10px;
            border-radius: 4px;
            font-family: 'Monaco', 'Menlo', monospace;
            font-size: 0.85em;
            overflow-x: auto;
            margin: 5px 0;
        }

        .confidence-bar {
            width: 100%;
            height: 6px;
            background: #e9ecef;
            border-radius: 3px;
            overflow: hidden;
            margin-top: 5px;
        }

        .confidence-fill {
            height: 100%;
            background: linear-gradient(90deg, #28a745, #ffc107, #dc3545);
            transition: width 0.3s ease;
        }

        .finding-recommendations ul {
            margin-left: 20px;
        }

        .finding-recommendations li {
            margin-bottom: 5px;
        }

        .footer {
            text-align: center;
            padding: 20px;
            color: #666;
            font-size: 0.9em;
            border-top: 1px solid #e9ecef;
        }

        .footer p {
            margin-bottom: 5px;
        }

        .hidden {
            display: none !important;
        }

        @media (max-width: 768px) {
            .container {
                padding: 10px;
            }

            .header {
                padding: 20px;
            }

            .header h1 {
                font-size: 2em;
            }

            .scan-info {
                flex-direction: column;
                gap: 10px;
            }

            .stats-grid {
                grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
            }

            .filter-controls {
                flex-direction: column;
                align-items: stretch;
            }

            .filter-group {
                width: 100%;
            }

            .file-header {
                flex-direction: column;
                align-items: flex-start;
                gap: 10px;
            }

            .file-severity {
                align-self: flex-end;
            }
        }
    """

    private fun getEmbeddedJavaScript(): String =
            """
        let allFileResults = [];
        
        function initializeResults() {
            allFileResults = Array.from(document.querySelectorAll('.file-result'));
        }
        
        function filterResults() {
            const severityFilter = document.getElementById('severityFilter').value;
            const typeFilter = document.getElementById('typeFilter').value;
            const searchFilter = document.getElementById('searchFilter').value.toLowerCase();
            
            allFileResults.forEach(fileResult => {
                let shouldShow = true;
                
                // Severity filter
                if (severityFilter !== 'all') {
                    const fileSeverity = fileResult.getAttribute('data-severity');
                    if (fileSeverity !== severityFilter) {
                        shouldShow = false;
                    }
                }
                
                // Type filter
                if (typeFilter !== 'all' && shouldShow) {
                    const findings = fileResult.querySelectorAll('.finding');
                    const hasMatchingType = Array.from(findings).some(finding => 
                        finding.getAttribute('data-type') === typeFilter
                    );
                    if (!hasMatchingType) {
                        shouldShow = false;
                    }
                }
                
                // Search filter
                if (searchFilter && shouldShow) {
                    const filename = fileResult.getAttribute('data-filename').toLowerCase();
                    if (!filename.includes(searchFilter)) {
                        shouldShow = false;
                    }
                }
                
                // Show/hide based on filters
                if (shouldShow) {
                    fileResult.classList.remove('hidden');
                    
                    // If type filter is active, show only matching findings
                    if (typeFilter !== 'all') {
                        const findings = fileResult.querySelectorAll('.finding');
                        findings.forEach(finding => {
                            const findingType = finding.getAttribute('data-type');
                            if (findingType === typeFilter) {
                                finding.classList.remove('hidden');
                            } else {
                                finding.classList.add('hidden');
                            }
                        });
                    } else {
                        // Show all findings
                        const findings = fileResult.querySelectorAll('.finding');
                        findings.forEach(finding => {
                            finding.classList.remove('hidden');
                        });
                    }
                } else {
                    fileResult.classList.add('hidden');
                }
            });
            
            updateResultsCount();
        }
        
        function clearFilters() {
            document.getElementById('severityFilter').value = 'all';
            document.getElementById('typeFilter').value = 'all';
            document.getElementById('searchFilter').value = '';
            
            allFileResults.forEach(fileResult => {
                fileResult.classList.remove('hidden');
                const findings = fileResult.querySelectorAll('.finding');
                findings.forEach(finding => {
                    finding.classList.remove('hidden');
                });
            });
            
            updateResultsCount();
        }
        
        function updateResultsCount() {
            const visibleResults = allFileResults.filter(result => 
                !result.classList.contains('hidden')
            ).length;
            
            const totalResults = allFileResults.length;
            
            // Update or create results counter
            let counter = document.getElementById('results-counter');
            if (!counter) {
                counter = document.createElement('div');
                counter.id = 'results-counter';
                counter.style.cssText = `
                    margin-bottom: 20px;
                    padding: 10px;
                    background: #e3f2fd;
                    border-radius: 6px;
                    font-weight: 500;
                    color: #1565c0;
                `;
                
                const resultsSection = document.querySelector('.results-section h2');
                resultsSection.insertAdjacentElement('afterend', counter);
            }
            
            counter.textContent = `Showing ${visibleResults} of ${totalResults} files with issues`;
        }
        
        function toggleFile(fileId) {
            const content = document.getElementById(`content-${fileId}`);
            const arrow = document.getElementById(`arrow-${fileId}`);
            
            if (content.classList.contains('hidden')) {
                content.classList.remove('hidden');
                arrow.classList.remove('rotated');
                arrow.textContent = '▼';
            } else {
                content.classList.add('hidden');
                arrow.classList.add('rotated');
                arrow.textContent = '▶';
            }
        }
        
        // Initialize when page loads
        document.addEventListener('DOMContentLoaded', function() {
            initializeResults();
            updateResultsCount();
            
            // Collapse all file contents by default for better overview
            const allContents = document.querySelectorAll('.file-content');
            const allArrows = document.querySelectorAll('.toggle-arrow');
            
            allContents.forEach(content => content.classList.add('hidden'));
            allArrows.forEach(arrow => {
                arrow.classList.add('rotated');
                arrow.textContent = '▶';
            });
        });
        
        // Add smooth scrolling for better UX
        function scrollToTop() {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        }
        
        // Add scroll to top button
        window.addEventListener('scroll', function() {
            let scrollButton = document.getElementById('scroll-to-top');
            if (!scrollButton) {
                scrollButton = document.createElement('button');
                scrollButton.id = 'scroll-to-top';
                scrollButton.innerHTML = '↑';
                scrollButton.onclick = scrollToTop;
                scrollButton.style.cssText = `
                    position: fixed;
                    bottom: 20px;
                    right: 20px;
                    width: 50px;
                    height: 50px;
                    border-radius: 50%;
                    background: #007bff;
                    color: white;
                    border: none;
                    font-size: 20px;
                    cursor: pointer;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
                    transition: all 0.3s ease;
                    z-index: 1000;
                    display: none;
                `;
                document.body.appendChild(scrollButton);
            }
            
            if (window.pageYOffset > 300) {
                scrollButton.style.display = 'block';
            } else {
                scrollButton.style.display = 'none';
            }
        });
    """
}
