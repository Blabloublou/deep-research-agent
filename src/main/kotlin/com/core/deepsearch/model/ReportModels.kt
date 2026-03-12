package com.core.deepsearch.model

/**
 * Final research report structure.
 */
data class ResearchReport(
    val topic: String,
    val executiveSummary: String,
    val thematicSynthesis: Map<String, ThematicSection>,
    val contradictions: List<Contradiction>,
    val consensus: List<ConsensusPoint>,
    val limitations: List<String>,
    val uncertainties: List<String>,
    val metrics: ResearchMetrics,
    val sources: List<Source>
)

/**
 * Thematic section in the report.
 */
data class ThematicSection(
    val theme: String,
    val synthesis: String,
    val keyClaims: List<Claim>,
    val supportingSources: List<String>
)

/**
 * Represents a contradiction found during research.
 */
data class Contradiction(
    val claim1: Claim,
    val claim2: Claim,
    val analysis: String
)

/**
 * Represents a consensus point.
 */
data class ConsensusPoint(
    val statement: String,
    val supportingClaims: List<Claim>,
    val confidenceLevel: Double
)

/**
 * Research process metrics.
 */
data class ResearchMetrics(
    val totalDurationMs: Long,
    val iterations: Int,
    val totalQueries: Int,
    val sourcesEvaluated: Int,
    val sourcesUsed: Int,
    val claimsExtracted: Int,
    val claimsVerified: Int,
    val averageSourceCredibility: Double,
    val overallConfidenceScore: Double
)

