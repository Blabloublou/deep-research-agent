package com.core.deepsearch.model.api

import kotlinx.serialization.Serializable

/**
 * Request to start a research.
 */
@Serializable
data class ResearchRequest(
    val topic: String,
    val maxIterations: Int = 3,
    val model: String? = null,
    val agentId: Long? = null
)

/**
 * Agent creation request.
 */
@Serializable
data class CreateAgentRequest(
    val name: String,
    val model: String = "gpt-4o",
    val temperature: Double? = null,
    val maxTokens: Int? = null,
    val maxIterations: Int? = null
)

/**
 * Agent update request.
 */
@Serializable
data class UpdateAgentRequest(
    val name: String? = null,
    val model: String? = null,
    val temperature: Double? = null,
    val maxTokens: Int? = null,
    val maxIterations: Int? = null
)

