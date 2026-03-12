package com.core.deepsearch.api.services

import com.core.deepsearch.db.entities.AgentEntity
import com.core.deepsearch.db.entities.ReportEntity
import com.core.deepsearch.db.repositories.AgentRepository
import com.core.deepsearch.db.repositories.ReportRepository

/**
 * Service to manage agent configurations and their reports.
 */
class AgentService {
    private val agentRepository = AgentRepository()
    private val reportRepository = ReportRepository()

    /**
     * Create a new agent configuration.
     */
    fun createAgent(
        name: String,
        model: String,
        temperature: Double?,
        maxTokens: Int?,
        maxIterations: Int?
    ): AgentEntity {
        return agentRepository.create(
            name = name,
            model = model,
            temperature = temperature,
            maxTokens = maxTokens,
            maxIterations = maxIterations
        )
    }

    /**
     * Get an agent by ID.
     */
    fun getAgent(id: Long): AgentEntity? {
        return agentRepository.findById(id)
    }

    /**
     * List all agents.
     */
    fun listAgents(): List<AgentEntity> {
        return agentRepository.list()
    }

    /**
     * Update an agent configuration.
     */
    fun updateAgent(
        id: Long,
        name: String?,
        model: String?,
        temperature: Double?,
        maxTokens: Int?,
        maxIterations: Int?
    ): AgentEntity? {
        return agentRepository.update(
            id = id,
            name = name,
            model = model,
            temperature = temperature,
            maxTokens = maxTokens,
            maxIterations = maxIterations
        )
    }

    /**
     * Delete an agent.
     */
    fun deleteAgent(id: Long): Boolean {
        return agentRepository.delete(id)
    }

    /**
     * Get reports for a specific agent.
     */
    fun getAgentReports(agentId: Long): List<ReportEntity> {
        return reportRepository.listByAgent(agentId)
    }

    /**
     * List all reports or filter by agent.
     */
    fun listReports(agentId: Long? = null): List<ReportEntity> {
        return reportRepository.listByAgent(agentId)
    }

    /**
     * Get a specific report by ID.
     */
    fun getReport(id: Long): ReportEntity? {
        return reportRepository.findById(id)
    }
}

