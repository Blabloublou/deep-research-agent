package com.jetbrains.deepsearch.model.api

import kotlinx.serialization.Serializable

/**
 * Response when research is started.
 */
@Serializable
data class ResearchResponse(
    val id: String,
    val status: String,
    val message: String? = null
)

/**
 * Health check response.
 */
@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Agent response.
 */
@Serializable
data class AgentResponse(
    val id: Long,
    val name: String,
    val model: String,
    val temperature: Double?,
    val maxTokens: Int?,
    val maxIterations: Int?
)

/**
 * Research status response.
 */
@Serializable
data class ResearchStatus(
    val id: String,
    val status: String,
    val progress: ResearchProgress? = null,
    val error: String? = null
) {
    constructor(id: String, status: ResearchState, progress: ResearchProgress? = null, error: String? = null) 
        : this(id, status.name, progress, error)
}

/**
 * Research progress info.
 */
@Serializable
data class ResearchProgress(
    val currentIteration: Int,
    val totalIterations: Int,
    val sourcesFound: Int,
    val claimsExtracted: Int,
    val currentPhase: String
)

/**
 * Research result response.
 */
@Serializable
data class ResearchResult(
    val id: String,
    val topic: String,
    val status: String,
    val report: String? = null,
    val metrics: ResearchMetricsApi? = null,
    val startTime: Long,
    val endTime: Long? = null
) {
    constructor(id: String, topic: String, status: ResearchState, report: String?, metrics: ResearchMetricsApi?, startTime: Long, endTime: Long?)
        : this(id, topic, status.name, report, metrics, startTime, endTime)
}

/**
 * Research metrics for API.
 */
@Serializable
data class ResearchMetricsApi(
    val totalDurationMs: Long,
    val iterations: Int,
    val sourcesUsed: Int,
    val claimsExtracted: Int,
    val claimsVerified: Int,
    val averageCredibility: Double
)

/**
 * Saved report response.
 */
@Serializable
data class SavedReportResponse(
    val id: Long,
    val agentId: Long?,
    val topic: String,
    val report: String,
    val createdAt: Long
)

