package com.jetbrains.deepsearch.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object AgentsTable : LongIdTable("agents") {
    val name = varchar("name", 100)
    val model = varchar("model", 100).default("gpt-4o")
    val temperature = double("temperature").nullable()
    val maxTokens = integer("max_tokens").nullable()
    val maxIterations = integer("max_iterations").nullable()
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
}

