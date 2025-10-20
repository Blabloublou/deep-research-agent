package com.jetbrains.deepsearch.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object ReportsTable : LongIdTable("reports") {
    val agentId = long("agent_id").references(AgentsTable.id).nullable()
    val topic = varchar("topic", 500)
    val report = text("report")
    val createdAt = long("created_at")
}

