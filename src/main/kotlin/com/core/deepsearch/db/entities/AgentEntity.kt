package com.core.deepsearch.db.entities

data class AgentEntity(
    val id: Long,
    val name: String,
    val model: String,
    val temperature: Double?,
    val maxTokens: Int?,
    val maxIterations: Int?,
)

