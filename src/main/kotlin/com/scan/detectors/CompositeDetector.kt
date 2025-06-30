package com.scan.detectors

import com.scan.core.ScanResult
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min
import kotlinx.coroutines.*

/**
 * Composite detector that combines multiple detection strategies to provide comprehensive secret
 * detection with intelligent result aggregation and deduplication.
 *
 * Features:
 * - Parallel execution of multiple detectors
 * - Intelligent result merging and deduplication
 * - Confidence score aggregation from multiple sources
 * - Customizable detection strategies and weights
 * - Performance optimization with caching
 * - Detailed reporting of detection sources
 */
class CompositeDetector(
        private val detectors: List<DetectorConfig> = emptyList(),
        private val executionMode: ExecutionMode = ExecutionMode.PARALLEL,
        private val mergingStrategy: MergingStrategy = MergingStrategy.WEIGHTED_AVERAGE,
        private val deduplicationStrategy: DeduplicationStrategy =
                DeduplicationStrategy.POSITION_AND_CONTENT,
        private val enableCaching: Boolean = true,
        private val maxCacheSize: Int = 1000,
        private val timeoutMillis: Long = 30000
) : DetectorInterface {

    private val resultCache =
            if (enableCaching) ConcurrentHashMap<String, List<ScanResult.Finding>>() else null
    private val detectorInstances = detectors.map { it.detector }

    companion object {
        /** Creates a default composite detector with commonly used detection strategies */
        fun createDefault(
                patternDetector: PatternDetector,
                entropyDetector: EntropyDetector,
                contextAwareDetector: ContextAwareDetector
        ): CompositeDetector {
            return CompositeDetector(
                    detectors =
                            listOf(
                                    DetectorConfig(
                                            patternDetector,
                                            weight = 1.0,
                                            priority = DetectorPriority.HIGH
                                    ),
                                    DetectorConfig(
                                            entropyDetector,
                                            weight = 0.8,
                                            priority = DetectorPriority.MEDIUM
                                    ),
                                    DetectorConfig(
                                            contextAwareDetector,
                                            weight = 1.2,
                                            priority = DetectorPriority.HIGH
                                    )
                            ),
                    mergingStrategy = MergingStrategy.WEIGHTED_AVERAGE,
                    deduplicationStrategy = DeduplicationStrategy.SMART_MERGE
            )
        }

        /** Creates a high-precision composite detector focused on reducing false positives */
        fun createHighPrecision(
                contextAwareDetector: ContextAwareDetector,
                patternDetector: PatternDetector
        ): CompositeDetector {
            return CompositeDetector(
                    detectors =
                            listOf(
                                    DetectorConfig(
                                            contextAwareDetector,
                                            weight = 1.5,
                                            priority = DetectorPriority.HIGH
                                    ),
                                    DetectorConfig(
                                            patternDetector,
                                            weight = 1.0,
                                            priority = DetectorPriority.MEDIUM
                                    )
                            ),
                    mergingStrategy = MergingStrategy.CONSERVATIVE,
                    deduplicationStrategy = DeduplicationStrategy.SMART_MERGE
            )
        }

        /** Creates a high-recall composite detector focused on finding all potential secrets */
        fun createHighRecall(
                patternDetector: PatternDetector,
                entropyDetector: EntropyDetector,
                contextAwareDetector: ContextAwareDetector
        ): CompositeDetector {
            return CompositeDetector(
                    detectors =
                            listOf(
                                    DetectorConfig(
                                            patternDetector,
                                            weight = 1.0,
                                            priority = DetectorPriority.HIGH
                                    ),
                                    DetectorConfig(
                                            entropyDetector,
                                            weight = 1.0,
                                            priority = DetectorPriority.HIGH
                                    ),
                                    DetectorConfig(
                                            contextAwareDetector,
                                            weight = 0.8,
                                            priority = DetectorPriority.MEDIUM
                                    )
                            ),
                    mergingStrategy = MergingStrategy.OPTIMISTIC,
                    deduplicationStrategy = DeduplicationStrategy.POSITION_BASED
            )
        }
    }

    override fun detect(file: File, content: String): List<ScanResult.Finding> {
        if (detectorInstances.isEmpty()) {
            return emptyList()
        }

        // Check cache first
        val cacheKey = generateCacheKey(file, content)
        resultCache?.get(cacheKey)?.let { cachedResult ->
            return cachedResult
        }

        val results =
                when (executionMode) {
                    ExecutionMode.SEQUENTIAL -> executeSequential(file, content)
                    ExecutionMode.PARALLEL -> executeParallel(file, content)
                    ExecutionMode.FAIL_FAST -> executeFailFast(file, content)
                    ExecutionMode.PRIORITY_BASED -> executePriorityBased(file, content)
                }

        val mergedResults = mergeResults(results, file)
        val deduplicatedResults = deduplicateResults(mergedResults)
        val finalResults = postProcessResults(deduplicatedResults, file, content)

        // Cache results
        resultCache?.let { cache ->
            if (cache.size >= maxCacheSize) {
                // Simple cache eviction - remove oldest entries
                val keysToRemove = cache.keys.take(maxCacheSize / 4)
                keysToRemove.forEach { cache.remove(it) }
            }
            cache[cacheKey] = finalResults
        }

        return finalResults
    }

    private fun executeSequential(file: File, content: String): List<DetectorResult> {
        val results = mutableListOf<DetectorResult>()

        detectors.forEach { config ->
            try {
                val findings = config.detector.detect(file, content)
                results.add(DetectorResult(config, findings, null))
            } catch (e: Exception) {
                results.add(DetectorResult(config, emptyList(), e))
            }
        }

        return results
    }

    private fun executeParallel(file: File, content: String): List<DetectorResult> {
        return runBlocking {
            val deferredResults =
                    detectors.map { config ->
                        async(Dispatchers.Default) {
                            try {
                                withTimeout(timeoutMillis) {
                                    val findings = config.detector.detect(file, content)
                                    DetectorResult(config, findings, null)
                                }
                            } catch (e: TimeoutCancellationException) {
                                DetectorResult(
                                        config,
                                        emptyList(),
                                        Exception(
                                                "Detector timed out: ${config.detector::class.simpleName}"
                                        )
                                )
                            } catch (e: Exception) {
                                DetectorResult(config, emptyList(), e)
                            }
                        }
                    }

            deferredResults.awaitAll()
        }
    }

    private fun executeFailFast(file: File, content: String): List<DetectorResult> {
        val results = mutableListOf<DetectorResult>()

        // Sort detectors by priority (high priority first)
        val sortedDetectors = detectors.sortedByDescending { it.priority.value }

        for (config in sortedDetectors) {
            try {
                val findings = config.detector.detect(file, content)
                results.add(DetectorResult(config, findings, null))

                // If high-priority detector finds high-confidence results, we can stop early
                if (config.priority == DetectorPriority.HIGH && findings.any { it.confidence > 0.8 }
                ) {
                    break
                }
            } catch (e: Exception) {
                results.add(DetectorResult(config, emptyList(), e))
            }
        }

        return results
    }

    private fun executePriorityBased(file: File, content: String): List<DetectorResult> {
        val results = mutableListOf<DetectorResult>()
        val detectorsByPriority = detectors.groupBy { it.priority }

        // Execute high priority detectors first
        DetectorPriority.values().sortedByDescending { it.value }.forEach { priority ->
            detectorsByPriority[priority]?.let { priorityDetectors ->
                val priorityResults = runBlocking {
                    priorityDetectors
                            .map { config ->
                                async(Dispatchers.Default) {
                                    try {
                                        val findings = config.detector.detect(file, content)
                                        DetectorResult(config, findings, null)
                                    } catch (e: Exception) {
                                        DetectorResult(config, emptyList(), e)
                                    }
                                }
                            }
                            .awaitAll()
                }
                results.addAll(priorityResults)

                // If high priority detectors found significant results, we might skip lower
                // priority ones
                if (priority == DetectorPriority.HIGH &&
                                priorityResults.any {
                                    it.findings.any { finding -> finding.confidence > 0.7 }
                                }
                ) {
                    // Continue with lower priority but with reduced timeout
                    // This is a simple heuristic - can be made more sophisticated
                }
            }
        }

        return results
    }

    private fun mergeResults(results: List<DetectorResult>, file: File): List<ScanResult.Finding> {
        val allFindings = results.flatMap { it.findings }

        if (allFindings.isEmpty()) {
            return emptyList()
        }

        return when (mergingStrategy) {
            MergingStrategy.SIMPLE_UNION -> allFindings
            MergingStrategy.WEIGHTED_AVERAGE -> mergeWithWeightedAverage(results)
            MergingStrategy.CONSERVATIVE -> mergeConservatively(results)
            MergingStrategy.OPTIMISTIC -> mergeOptimistically(results)
        }
    }

    private fun mergeWithWeightedAverage(results: List<DetectorResult>): List<ScanResult.Finding> {
        val findingGroups =
                groupSimilarFindings(
                        results.flatMap { result ->
                            result.findings.map { finding ->
                                WeightedFinding(
                                        finding,
                                        result.config.weight,
                                        result.config.detector::class.simpleName ?: "Unknown"
                                )
                            }
                        }
                )

        return findingGroups.map { group ->
            val baseFinding = group.first().finding
            val totalWeight = group.sumOf { it.weight }
            val weightedConfidence = group.sumOf { it.finding.confidence * it.weight } / totalWeight
            val detectorSources = group.map { it.detectorName }.distinct()

            baseFinding.copy(
                    confidence = weightedConfidence,
                    context = buildMergedContext(baseFinding.context, detectorSources),
                    message = enhanceMessageWithSources(baseFinding.message, detectorSources)
            )
        }
    }

    private fun mergeConservatively(results: List<DetectorResult>): List<ScanResult.Finding> {
        val allFindings = results.flatMap { it.findings }
        val findingGroups = groupSimilarFindings(allFindings.map { WeightedFinding(it, 1.0, "") })

        // Only keep findings that are detected by multiple detectors OR have very high confidence
        return findingGroups.mapNotNull { group ->
            val baseFinding = group.first().finding
            when {
                group.size > 1 -> {
                    // Multiple detectors found this - increase confidence
                    val avgConfidence = group.map { it.finding.confidence }.average()
                    baseFinding.copy(
                            confidence = min(1.0, avgConfidence * 1.2),
                            message =
                                    "${baseFinding.message} (confirmed by ${group.size} detectors)"
                    )
                }
                baseFinding.confidence > 0.8 -> baseFinding
                else -> null
            }
        }
    }

    private fun mergeOptimistically(results: List<DetectorResult>): List<ScanResult.Finding> {
        val allFindings = results.flatMap { it.findings }
        val findingGroups = groupSimilarFindings(allFindings.map { WeightedFinding(it, 1.0, "") })

        return findingGroups.map { group ->
            val baseFinding = group.first().finding
            val maxConfidence = group.maxOf { it.finding.confidence }

            baseFinding.copy(
                    confidence = maxConfidence,
                    severity = group.maxOf { it.finding.severity },
                    message =
                            "${baseFinding.message} (detected by ${group.size} detector${if (group.size > 1) "s" else ""})"
            )
        }
    }

    private fun deduplicateResults(findings: List<ScanResult.Finding>): List<ScanResult.Finding> {
        return when (deduplicationStrategy) {
            DeduplicationStrategy.NONE -> findings
            DeduplicationStrategy.EXACT_MATCH -> deduplicateExactMatches(findings)
            DeduplicationStrategy.POSITION_BASED -> deduplicateByPosition(findings)
            DeduplicationStrategy.POSITION_AND_CONTENT -> deduplicateByPositionAndContent(findings)
            DeduplicationStrategy.SMART_MERGE -> smartMergeFindings(findings)
        }
    }

    private fun deduplicateExactMatches(
            findings: List<ScanResult.Finding>
    ): List<ScanResult.Finding> {
        return findings.distinctBy { Triple(it.file.absolutePath, it.lineNumber, it.message) }
    }

    private fun deduplicateByPosition(
            findings: List<ScanResult.Finding>
    ): List<ScanResult.Finding> {
        return findings
                .groupBy { Triple(it.file.absolutePath, it.lineNumber, it.columnStart) }
                .map { (_, group) ->
                    // Keep the finding with highest confidence
                    group.maxByOrNull { it.confidence } ?: group.first()
                }
    }

    private fun deduplicateByPositionAndContent(
            findings: List<ScanResult.Finding>
    ): List<ScanResult.Finding> {
        return findings
                .groupBy { finding ->
                    QuadTuple(
                            finding.file.absolutePath,
                            finding.lineNumber,
                            finding.columnStart,
                            extractSecretValue(finding)
                    )
                }
                .map { (_, group) ->
                    // Merge findings for the same secret
                    mergeIdenticalFindings(group)
                }
    }

    private fun smartMergeFindings(findings: List<ScanResult.Finding>): List<ScanResult.Finding> {
        val groups = mutableListOf<MutableList<ScanResult.Finding>>()

        findings.forEach { finding ->
            val existingGroup =
                    groups.find { group ->
                        group.any { existing -> areSimilarFindings(existing, finding) }
                    }

            if (existingGroup != null) {
                existingGroup.add(finding)
            } else {
                groups.add(mutableListOf(finding))
            }
        }

        return groups.map { group ->
            if (group.size == 1) {
                group.first()
            } else {
                mergeIdenticalFindings(group)
            }
        }
    }

    private fun groupSimilarFindings(
            weightedFindings: List<WeightedFinding>
    ): List<List<WeightedFinding>> {
        val groups = mutableListOf<MutableList<WeightedFinding>>()

        weightedFindings.forEach { weightedFinding ->
            val existingGroup =
                    groups.find { group ->
                        group.any { existing ->
                            areSimilarFindings(existing.finding, weightedFinding.finding)
                        }
                    }

            if (existingGroup != null) {
                existingGroup.add(weightedFinding)
            } else {
                groups.add(mutableListOf(weightedFinding))
            }
        }

        return groups
    }

    private fun areSimilarFindings(
            finding1: ScanResult.Finding,
            finding2: ScanResult.Finding
    ): Boolean {
        // Same file and line
        if (finding1.file.absolutePath == finding2.file.absolutePath &&
                        finding1.lineNumber == finding2.lineNumber
        ) {

            // Check if positions overlap or are very close
            val pos1Range = finding1.columnStart..finding1.columnEnd
            val pos2Range = finding2.columnStart..finding2.columnEnd

            return pos1Range.intersect(pos2Range).isNotEmpty() ||
                    kotlin.math.abs(finding1.columnStart - finding2.columnStart) <= 3
        }

        return false
    }

    private fun mergeIdenticalFindings(findings: List<ScanResult.Finding>): ScanResult.Finding {
        if (findings.size == 1) return findings.first()

        val baseFinding = findings.first()
        val maxConfidence = findings.maxOf { it.confidence }
        val maxSeverity = findings.maxOf { it.severity }
        val allRuleIds = findings.map { it.ruleId }.distinct()

        return baseFinding.copy(
                confidence = maxConfidence,
                severity = maxSeverity,
                ruleId = allRuleIds.joinToString(","),
                message = "${baseFinding.message} (merged from ${findings.size} detections)",
                context = findings.map { it.context }.distinct().joinToString(" | ")
        )
    }

    private fun postProcessResults(
            findings: List<ScanResult.Finding>,
            file: File,
            content: String
    ): List<ScanResult.Finding> {
        return findings
                .filter { it.confidence > 0.1 } // Remove very low confidence findings
                .sortedWith(
                        compareByDescending<ScanResult.Finding> { it.severity }
                                .thenByDescending { it.confidence }
                                .thenBy { it.lineNumber }
                )
                .map { enhanceFindingWithMetadata(it, file, content) }
    }

    private fun enhanceFindingWithMetadata(
            finding: ScanResult.Finding,
            file: File,
            content: String
    ): ScanResult.Finding {
        val lines = content.lines()
        val lineContent =
                if (finding.lineNumber <= lines.size) {
                    lines[finding.lineNumber - 1]
                } else {
                    ""
                }

        return finding.copy(
                context =
                        if (finding.context.isBlank()) {
                            "Line: ${lineContent.trim()}"
                        } else {
                            "${finding.context} | Line: ${lineContent.trim()}"
                        }
        )
    }

    private fun generateCacheKey(file: File, content: String): String {
        return "${file.absolutePath}:${content.hashCode()}"
    }

    private fun extractSecretValue(finding: ScanResult.Finding): String {
        // This is a simplified extraction - in practice, you'd parse the context
        // or store the actual secret value in the Finding object
        return finding.message.substringAfter("'").substringBefore("'").ifEmpty {
            finding.context.substringAfter(": ").take(20)
        }
    }

    private fun buildMergedContext(originalContext: String, detectorSources: List<String>): String {
        val sources = "Detected by: ${detectorSources.joinToString(", ")}"
        return if (originalContext.isBlank()) sources else "$originalContext | $sources"
    }

    private fun enhanceMessageWithSources(
            originalMessage: String,
            detectorSources: List<String>
    ): String {
        return if (detectorSources.size > 1) {
            "$originalMessage (confirmed by ${detectorSources.size} detectors)"
        } else {
            originalMessage
        }
    }

    // Configuration and result classes
    data class DetectorConfig(
            val detector: DetectorInterface,
            val weight: Double = 1.0,
            val priority: DetectorPriority = DetectorPriority.MEDIUM,
            val enabled: Boolean = true,
            val timeoutMillis: Long = 10000
    )

    private data class DetectorResult(
            val config: DetectorConfig,
            val findings: List<ScanResult.Finding>,
            val error: Exception?
    )

    private data class WeightedFinding(
            val finding: ScanResult.Finding,
            val weight: Double,
            val detectorName: String
    )

    private data class QuadTuple<T1, T2, T3, T4>(
            val first: T1,
            val second: T2,
            val third: T3,
            val fourth: T4
    )

    enum class ExecutionMode {
        SEQUENTIAL, // Execute detectors one by one
        PARALLEL, // Execute all detectors in parallel
        FAIL_FAST, // Stop early if high-confidence results found
        PRIORITY_BASED // Execute by priority order
    }

    enum class MergingStrategy {
        SIMPLE_UNION, // Just combine all results
        WEIGHTED_AVERAGE, // Use detector weights to calculate confidence
        CONSERVATIVE, // Only keep findings confirmed by multiple detectors
        OPTIMISTIC // Keep all findings, use maximum confidence
    }

    enum class DeduplicationStrategy {
        NONE, // No deduplication
        EXACT_MATCH, // Remove exact duplicates
        POSITION_BASED, // Deduplicate by file position
        POSITION_AND_CONTENT, // Deduplicate by position and content
        SMART_MERGE // Intelligent merging of similar findings
    }

    enum class DetectorPriority(val value: Int) {
        LOW(1),
        MEDIUM(2),
        HIGH(3)
    }

    /** Builder for creating composite detectors with fluent API */
    class Builder {
        private val detectors = mutableListOf<DetectorConfig>()
        private var executionMode = ExecutionMode.PARALLEL
        private var mergingStrategy = MergingStrategy.WEIGHTED_AVERAGE
        private var deduplicationStrategy = DeduplicationStrategy.SMART_MERGE
        private var enableCaching = true
        private var maxCacheSize = 1000
        private var timeoutMillis = 30000L

        fun addDetector(
                detector: DetectorInterface,
                weight: Double = 1.0,
                priority: DetectorPriority = DetectorPriority.MEDIUM
        ): Builder {
            detectors.add(DetectorConfig(detector, weight, priority))
            return this
        }

        fun executionMode(mode: ExecutionMode): Builder {
            this.executionMode = mode
            return this
        }

        fun mergingStrategy(strategy: MergingStrategy): Builder {
            this.mergingStrategy = strategy
            return this
        }

        fun deduplicationStrategy(strategy: DeduplicationStrategy): Builder {
            this.deduplicationStrategy = strategy
            return this
        }

        fun enableCaching(enable: Boolean): Builder {
            this.enableCaching = enable
            return this
        }

        fun maxCacheSize(size: Int): Builder {
            this.maxCacheSize = size
            return this
        }

        fun timeout(millis: Long): Builder {
            this.timeoutMillis = millis
            return this
        }

        fun build(): CompositeDetector {
            return CompositeDetector(
                    detectors = detectors.toList(),
                    executionMode = executionMode,
                    mergingStrategy = mergingStrategy,
                    deduplicationStrategy = deduplicationStrategy,
                    enableCaching = enableCaching,
                    maxCacheSize = maxCacheSize,
                    timeoutMillis = timeoutMillis
            )
        }
    }
}
