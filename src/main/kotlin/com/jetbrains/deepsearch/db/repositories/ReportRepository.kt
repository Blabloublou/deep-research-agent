package com.jetbrains.deepsearch.db.repositories

import com.jetbrains.deepsearch.db.entities.ReportEntity
import com.jetbrains.deepsearch.db.tables.ReportsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class ReportRepository {
    fun create(agentId: Long?, topic: String, report: String): ReportEntity = transaction {
        val now = System.currentTimeMillis()
        val id = ReportsTable.insertAndGetId {
            it[ReportsTable.agentId] = agentId
            it[ReportsTable.topic] = topic
            it[ReportsTable.report] = report
            it[ReportsTable.createdAt] = now
        }.value
        findById(id)!!
    }

    fun findById(id: Long): ReportEntity? = transaction {
        ReportsTable.select { ReportsTable.id eq id }.singleOrNull()?.toReport()
    }

    fun listByAgent(agentId: Long?): List<ReportEntity> = transaction {
        if (agentId == null) {
            ReportsTable.selectAll().orderBy(ReportsTable.createdAt, SortOrder.DESC).map { it.toReport() }
        } else {
            ReportsTable.select { ReportsTable.agentId eq agentId }
                .orderBy(ReportsTable.createdAt, SortOrder.DESC)
                .map { it.toReport() }
        }
    }

    private fun ResultRow.toReport() = ReportEntity(
        id = this[ReportsTable.id].value,
        agentId = this[ReportsTable.agentId],
        topic = this[ReportsTable.topic],
        report = this[ReportsTable.report],
        createdAt = this[ReportsTable.createdAt]
    )
}

