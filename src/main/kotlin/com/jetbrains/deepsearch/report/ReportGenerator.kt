package com.jetbrains.deepsearch.report

import com.jetbrains.deepsearch.clients.openai.OpenAIClient
import com.jetbrains.deepsearch.eval.CrossChecker
import com.jetbrains.deepsearch.model.*
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Generates comprehensive research reports in Markdown format.
 */
class ReportGenerator(
    private val openAIClient: OpenAIClient,
    private val crossChecker: CrossChecker
) {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    
    /**
     * Generate a complete research report.
     */
    suspend fun generateReport(
        context: ResearchContext,
        startTime: Long,
        endTime: Long
    ): String {        
        // Find contradictions
        val contradictions = crossChecker.findContradictions(context.claims)
        
        // Calculate metrics
        val metrics = calculateMetrics(context, startTime, endTime)
        
        val report = buildString {
            appendTitle(context.topic)
            appendLine()
            appendExecutiveSummary(context)
            appendLine()
            appendConsensusPoints(context)
            appendLine()
            if (contradictions.isNotEmpty()) {
                appendContradictions(contradictions)
                appendLine()
            }
            appendSourceList(context.sources)
            appendLine()
            appendResearchMetrics(metrics)
        }
        
        return report
    }
    
    /**
     * Export report to a Markdown file.
     */
    fun exportToMarkdown(report: String, outputPath: String): File {
        val file = File(outputPath)
        file.parentFile?.mkdirs()
        file.writeText(report)
        return file
    }
    
    private fun StringBuilder.appendTitle(topic: String) {
        appendLine("# $topic")
        appendLine("*Generated: ${dateFormatter.format(Instant.now())}*")
    }
    
    private fun StringBuilder.appendExecutiveSummary(context: ResearchContext) {
        appendLine("## Executive Summary")
        
        val topClaims = context.claims
            .filter { it.verificationStatus == VerificationStatus.VERIFIED || 
                     it.verificationStatus == VerificationStatus.LIKELY_TRUE }
            .sortedByDescending { it.confidence }
            
        
        val distinctTopClaims = topClaims
            .distinctBy { normalizeText(it.statement) }
            .take(5)

        if (distinctTopClaims.isNotEmpty()) {
            appendLine("# Key Findings:")
            appendLine()
            distinctTopClaims.forEach { claim ->
                appendLine("- ${claim.statement}")
            }
        } else {
            appendLine("Research conducted across ${context.sources.size} sources to investigate ${context.topic}.")
        }
        
        appendLine()
        appendLine("# Research Insights:")
        val keyFindingNorms = distinctTopClaims.map { normalizeText(it.statement) }.toSet()
        val filteredInsights = context.insights
            .map { stripInsightHeadings(it) }
            .distinctBy { normalizeText(it) }
            .filter { insight ->
                val inNorm = normalizeText(insight)
                keyFindingNorms.none { k -> areSimilarNormalized(k, inNorm) }
            }
            .take(1)
        filteredInsights.forEach { insight -> appendLine("- $insight") }
    }
    
    private fun StringBuilder.appendConsensusPoints(context: ResearchContext) {
        appendLine("## Consensus Points")
        val top = selectTopClaims(context.claims, 10)
        if (top.isEmpty()) {
            appendLine("*No verified findings available yet.*")
            return
        }
        top.forEach { claim ->
            val verificationBadge = when (claim.verificationStatus) {
                VerificationStatus.VERIFIED -> "✓ VERIFIED"
                VerificationStatus.LIKELY_TRUE -> "✓ Likely"
                VerificationStatus.CONFLICTING -> "⚠ Conflicting"
                VerificationStatus.LIKELY_FALSE -> "✗ Disputed"
                VerificationStatus.UNVERIFIED -> "? Unverified"
            }
            appendLine("- ${verificationBadge} | ${claim.statement} (Source: ${claim.sourceUrl})")
        }
    }
    
    private fun StringBuilder.appendContradictions(contradictions: List<Contradiction>) {
        appendLine("## Contradictions and Debates")
        
        if (contradictions.isEmpty()) {
            appendLine("*No major contradictions identified across sources.*")
            return
        }
        
        contradictions.take(3).forEach { contradiction ->
            appendLine("### Contradiction Identified")
            appendLine("**Position A:**")
            appendLine("> ${contradiction.claim1.statement}")
            appendLine()
            appendLine("*Source: ${contradiction.claim1.sourceUrl}*")
            appendLine()
            appendLine("**Position B:**")
            appendLine("> ${contradiction.claim2.statement}")
            appendLine()
            appendLine("*Source: ${contradiction.claim2.sourceUrl}*")
            appendLine()
            appendLine("**Analysis:**")
            appendLine(contradiction.analysis)
            appendLine()
        }
    }
    
    /**
     * Calculate research metrics.
     */
    private fun calculateMetrics(
        context: ResearchContext,
        startTime: Long,
        endTime: Long
    ): ResearchMetrics {
        val verifiedCount = context.claims.count { 
            it.verificationStatus == VerificationStatus.VERIFIED ||
            it.verificationStatus == VerificationStatus.LIKELY_TRUE
        }

        val avgCredibility = if (context.sources.isNotEmpty()) {
            context.sources.map { it.credibilityScore.overall }.average()
        } else {
            0.0
        }

        val confidence = if (context.claims.isNotEmpty()) {
            verifiedCount.toDouble() / context.claims.size
        } else {
            0.0
        }

        return ResearchMetrics(
            totalDurationMs = endTime - startTime,
            iterations = context.currentIteration,
            totalQueries = context.currentIteration * 2,
            sourcesEvaluated = context.sources.size,
            sourcesUsed = context.sources.size,
            claimsExtracted = context.claims.size,
            claimsVerified = verifiedCount,
            averageSourceCredibility = avgCredibility,
            overallConfidenceScore = confidence
        )
    }
    
    private fun StringBuilder.appendResearchMetrics(metrics: ResearchMetrics) {
        appendLine("## Research Metrics")
        appendLine("- **Verified Claims**: ${metrics.claimsVerified}")
        appendLine("- **Claims Extracted**: ${metrics.claimsExtracted}")
        appendLine("- **Avg Source Credibility**: ${(metrics.averageSourceCredibility * 100).toInt()}%")
        appendLine("- **Sources Used**: ${metrics.sourcesUsed} / ${metrics.sourcesEvaluated}")
        appendLine("- **Iterations**: ${metrics.iterations}")
        appendLine("- **Total Duration**: ${formatDuration(metrics.totalDurationMs)}")
    }
    
    private fun normalizeUrl(raw: String): String {
        return try {
            val uri = java.net.URI(raw)
            val scheme = uri.scheme?.lowercase() ?: "http"
            var host = uri.host?.lowercase() ?: uri.authority?.lowercase() ?: ""
            if (host.startsWith("www.")) host = host.removePrefix("www.")
            val path = (uri.path ?: "").trimEnd('/')
            if (host.isEmpty()) raw.trim().lowercase().removePrefix("www.").trimEnd('/') else "$scheme://$host$path"
        } catch (e: Exception) {
            raw.trim().lowercase().removePrefix("www.").trimEnd('/')
        }
    }

    private fun stripInsightHeadings(text: String): String {
        val withoutHashes = text.replace(Regex("^\\s*#{3,}\\s*"), "")
        val withoutNumbering = withoutHashes.replace(Regex("^\\s*(?:\\(?\\d+\\)?|\\d+[.)])\\s*"), "")
        val withoutBullets = withoutNumbering.replace(Regex("^\\s*[-*+]\\s+"), "")
        return withoutBullets.trim()
    }

    private fun normalizeText(input: String): String {
        return input
            .lowercase()
            .trim()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
            .replace(Regex("\\s+"), " ")
    }

    private fun areSimilarNormalized(aNorm: String, bNorm: String): Boolean {
        if (aNorm == bNorm) return true
        // Consider long substring containment as similar
        if (aNorm.length >= 20 && bNorm.contains(aNorm)) return true
        if (bNorm.length >= 20 && aNorm.contains(bNorm)) return true
        return false
    }

    private fun StringBuilder.appendSourceList(sources: List<Source>) {
        appendLine("## Sources")
        
        sources
            .sortedByDescending { it.credibilityScore.overall }
            .distinctBy { normalizeUrl(it.url) }
            .take(5)
            .forEach { source ->
                val credibilityPercent = (source.credibilityScore.overall * 100).toInt()
                appendLine("### ${source.title}")
                appendLine("- **URL:** ${source.url}")
                appendLine("- **Credibility Score:** $credibilityPercent%")
                appendLine("  - Authority: ${(source.credibilityScore.authority * 100).toInt()}%")
                appendLine("  - Recency: ${(source.credibilityScore.recency * 100).toInt()}%")
                appendLine("  - Objectivity: ${(source.credibilityScore.objectivity * 100).toInt()}%")
                if (source.publishedDate != null) {
                    appendLine("- **Published:** ${dateFormatter.format(source.publishedDate)}")
                }
                appendLine("- **Claims Extracted:** ${source.extractedClaims.size}")
                appendLine()
            }
    }
    
    /**
     * Group claims by theme using keyword similarity.
     */
    private fun groupClaimsByTheme(claims: List<Claim>): Map<String, List<Claim>> {
        if (claims.isEmpty()) return emptyMap()
        
        // Simple keyword-based grouping
        val themes = mutableMapOf<String, MutableList<Claim>>()
        
        claims.forEach { claim ->
            val keywords = extractKeywords(claim.statement)
            val themeName = keywords.firstOrNull()?.replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase() else it.toString() 
            } ?: "General"
            
            themes.getOrPut(themeName) { mutableListOf() }.add(claim)
        }
        
        return themes.filter { it.value.size > 1 } // Only include themes with multiple claims
    }
    
    /**
     * Extract keywords from text.
     */
    private fun extractKeywords(text: String): List<String> {
        val stopwords = setOf(
            "the", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would",
            "could", "should", "may", "might", "must", "can", "a", "an",
            "and", "or", "but", "in", "on", "at", "to", "for", "of", "with"
        )
        
        return text
            .lowercase()
            .split(Regex("\\W+"))
            .filter { it.length > 4 && it !in stopwords }
    }
    
    /**
     * Select claims prioritizing VERIFIED, then LIKELY_TRUE, then others by confidence.
     */
    private fun selectTopClaims(claims: List<Claim>, limit: Int): List<Claim> {
        if (claims.isEmpty() || limit <= 0) return emptyList()
        val verified = claims
            .filter { it.verificationStatus == VerificationStatus.VERIFIED }
            .sortedByDescending { it.confidence }
        if (verified.size >= limit) return verified.take(limit)
        val likely = claims
            .filter { it.verificationStatus == VerificationStatus.LIKELY_TRUE }
            .sortedByDescending { it.confidence }
            .filter { it !in verified }
        val combined = (verified + likely).let { selected ->
            if (selected.size >= limit) return selected.take(limit)
            val others = claims
                .filter { it.verificationStatus != VerificationStatus.VERIFIED && it.verificationStatus != VerificationStatus.LIKELY_TRUE }
                .sortedByDescending { it.confidence }
                .filter { it !in selected }
            (selected + others).take(limit)
        }
        return combined
    }
    
    /**
     * Format duration in readable format.
     */
    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        
        return when {
            minutes > 0 -> "${minutes}m ${remainingSeconds}s"
            else -> "${seconds}s"
        }
    }
}
