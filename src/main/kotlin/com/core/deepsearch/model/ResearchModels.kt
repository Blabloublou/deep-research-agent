package com.core.deepsearch.model

/**
 * Represents a research plan with progressive queries.
 */
data class ResearchPlan(
    val topic: String,
    val queries: List<ResearchQuery>,
    val rationale: String
)

/**
 * Individual research query in the plan.
 */
data class ResearchQuery(
    val query: String,
    val purpose: String,
    val expectedInsights: String,
    val iteration: Int
)

/**
 * Represents the context and state of research iterations.
 */
data class ResearchContext(
    val topic: String,
    var currentIteration: Int,
    val sources: MutableList<Source> = mutableListOf(),
    val claims: MutableList<Claim> = mutableListOf(),
    val insights: MutableList<String> = mutableListOf(),
    val gaps: MutableList<String> = mutableListOf()
)

