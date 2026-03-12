package com.core.deepsearch.db.entities

data class ReportEntity(
    val id: Long,
    val agentId: Long?,
    val topic: String,
    val report: String,
    val createdAt: Long
)

