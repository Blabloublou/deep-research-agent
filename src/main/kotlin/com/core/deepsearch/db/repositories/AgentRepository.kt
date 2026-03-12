package com.core.deepsearch.db.repositories

import com.core.deepsearch.db.entities.AgentEntity
import com.core.deepsearch.db.tables.AgentsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class AgentRepository {
    fun create(
        name: String,
        model: String,
        temperature: Double?,
        maxTokens: Int?,
        maxIterations: Int?
    ): AgentEntity = transaction {
        val now = System.currentTimeMillis()
        val id = AgentsTable.insertAndGetId {
            it[AgentsTable.name] = name
            it[AgentsTable.model] = model
            it[AgentsTable.temperature] = temperature
            it[AgentsTable.maxTokens] = maxTokens
            it[AgentsTable.maxIterations] = maxIterations
            it[AgentsTable.createdAt] = now
            it[AgentsTable.updatedAt] = now
        }.value
        findById(id)!!
    }

    fun findById(id: Long): AgentEntity? = transaction {
        AgentsTable.select { AgentsTable.id eq id }.singleOrNull()?.toAgent()
    }

    fun list(): List<AgentEntity> = transaction {
        AgentsTable.selectAll().orderBy(AgentsTable.createdAt, SortOrder.DESC).map { it.toAgent() }
    }

    fun update(
        id: Long,
        name: String?,
        model: String?,
        temperature: Double?,
        maxTokens: Int?,
        maxIterations: Int?
    ): AgentEntity? = transaction {
        val updated = AgentsTable.update({ AgentsTable.id eq id }) {
            val now = System.currentTimeMillis()
            if (name != null) it[AgentsTable.name] = name
            if (model != null) it[AgentsTable.model] = model
            it[AgentsTable.temperature] = temperature
            it[AgentsTable.maxTokens] = maxTokens
            it[AgentsTable.maxIterations] = maxIterations
            it[AgentsTable.updatedAt] = now
        }
        if (updated > 0) findById(id) else null
    }

    fun delete(id: Long): Boolean = transaction {
        AgentsTable.deleteWhere { AgentsTable.id eq id } > 0
    }

    private fun ResultRow.toAgent() = AgentEntity(
        id = this[AgentsTable.id].value,
        name = this[AgentsTable.name],
        model = this[AgentsTable.model],
        temperature = this[AgentsTable.temperature],
        maxTokens = this[AgentsTable.maxTokens],
        maxIterations = this[AgentsTable.maxIterations]
    )
}

