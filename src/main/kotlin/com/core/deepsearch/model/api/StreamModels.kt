package com.core.deepsearch.model.api

import kotlinx.serialization.Serializable

/**
 * Stream update message.
 */
@Serializable
data class StreamUpdate(
    val type: String,
    val data: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor(type: UpdateType, data: String) : this(type.name, data)
}

