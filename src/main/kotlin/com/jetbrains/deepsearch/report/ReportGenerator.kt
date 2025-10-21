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
            appendThematicAnalysis(context)
            appendLine()
            appendConsensusPoints(context)
            appendLine()
            if (contradictions.isNotEmpty()) {
                appendContradictions(contradictions)
                appendLine()
            }
            appendResearchMetrics(metrics)
            appendLine()
            appendSourceList(context.sources)
            appendLine()
            appendFooter()
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
        appendLine("# Deep Research Report: $topic")
        appendLine()
        appendLine("*Generated: ${dateFormatter.format(Instant.now())}*")
    }
    
    private fun StringBuilder.appendExecutiveSummary(context: ResearchContext) {
        appendLine("## Executive Summary")
        appendLine()
        
        val topClaims = context.claims
            .filter { it.verificationStatus == VerificationStatus.VERIFIED || 
                     it.verificationStatus == VerificationStatus.LIKELY_TRUE }
            .sortedByDescending { it.confidence }
            .take(5)
        
        if (topClaims.isNotEmpty()) {
            appendLine("**Key Findings:**")
            appendLine()
            topClaims.forEach { claim ->
                appendLine("- ${claim.statement}")
            }
        } else {
            appendLine("Research conducted across ${context.sources.size} sources to investigate ${context.topic}.")
        }
        
        appendLine()
        appendLine("**Research Insights:**")
        context.insights.take(5).forEach { insight ->
            appendLine("- $insight")
        }
    }
    
    private fun StringBuilder.appendThematicAnalysis(context: ResearchContext) {
        appendLine("## Thematic Analysis")
        appendLine()
        
        // Group claims by similarity (simplified grouping by keywords)
        val themes = groupClaimsByTheme(context.claims)
        
        if (themes.isEmpty()) {
            appendLine("*Analysis in progress - more iterations needed for comprehensive thematic synthesis.*")
            return
        }
        
        themes.forEach { (themeName, claims) ->
            appendLine("### $themeName")
            appendLine()
            claims.take(5).forEach { claim ->
                val verificationBadge = when (claim.verificationStatus) {
                    VerificationStatus.VERIFIED -> "✓ VERIFIED"
                    VerificationStatus.LIKELY_TRUE -> "✓ Likely"
                    VerificationStatus.CONFLICTING -> "⚠ Conflicting"
                    VerificationStatus.LIKELY_FALSE -> "✗ Disputed"
                    VerificationStatus.UNVERIFIED -> "? Unverified"
                }
                appendLine("**${verificationBadge}** | ${claim.statement}")
                appendLine()
                appendLine("*Source: ${claim.sourceUrl}*")
                appendLine()
            }
        }
    }
    
    private fun StringBuilder.appendConsensusPoints(context: ResearchContext) {
        appendLine("## Consensus Points")
        appendLine()
        
        val verifiedClaims = context.claims
            .filter { it.verificationStatus == VerificationStatus.VERIFIED }
        
        if (verifiedClaims.isEmpty()) {
            appendLine("*No strongly verified consensus points identified yet. More sources needed for cross-verification.*")
            return
        }
        
        verifiedClaims.take(10).forEach { claim ->
            appendLine("### ${claim.statement}")
            appendLine()
            appendLine("**Supporting Evidence:**")
            claim.supportingEvidence.forEach { evidence ->
                appendLine("- $evidence")
            }
            appendLine()
        }
    }
    
    private fun StringBuilder.appendContradictions(contradictions: List<Contradiction>) {
        appendLine("## Contradictions and Debates")
        appendLine()
        
        if (contradictions.isEmpty()) {
            appendLine("*No major contradictions identified across sources.*")
            return
        }
        
        contradictions.take(5).forEach { contradiction ->
            appendLine("### Contradiction Identified")
            appendLine()
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
            appendLine("---")
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
        appendLine()
        appendLine("| Metric | Value |")
        appendLine("|--------|-------|")
        appendLine("| Total Duration | ${formatDuration(metrics.totalDurationMs)} |")
        appendLine("| Iterations | ${metrics.iterations} |")
        appendLine("| Total Queries | ${metrics.totalQueries} |")
        appendLine("| Sources Evaluated | ${metrics.sourcesEvaluated} |")
        appendLine("| Sources Used | ${metrics.sourcesUsed} |")
        appendLine("| Claims Extracted | ${metrics.claimsExtracted} |")
        appendLine("| Claims Verified | ${metrics.claimsVerified} |")
        appendLine("| Avg Source Credibility | ${(metrics.averageSourceCredibility * 100).toInt()}% |")
        appendLine("| Overall Confidence | ${(metrics.overallConfidenceScore * 100).toInt()}% |")
    }
    
    private fun StringBuilder.appendSourceList(sources: List<Source>) {
        appendLine("## Sources")
        appendLine()
        appendLine("*Sources ranked by credibility score:*")
        appendLine()
        
        sources
            .sortedByDescending { it.credibilityScore.overall }
            .forEach { source ->
                val credibilityPercent = (source.credibilityScore.overall * 100).toInt()
                appendLine("### ${source.title}")
                appendLine()
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
    
    private fun StringBuilder.appendFooter() {
        appendLine("---")
        appendLine()
        appendLine("*Report generated by Deep Research Agent*")
        appendLine()
        appendLine("**Methodology:** Multi-iteration research with source evaluation, claim extraction, " +
                   "cross-verification, and thematic synthesis.")
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
     * Format duration in human-readable format.
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
