package com.core.deepsearch.model.api

/**
 * Research state enum.
 */
enum class ResearchState {
    PENDING,
    PLANNING,
    RESEARCHING,
    GENERATING_REPORT,
    COMPLETED,
    FAILED
}

/**
 * Update type enum for streaming.
 */
enum class UpdateType {
    STATUS,
    ITERATION,
    QUERY,
    SOURCE,
    CLAIM,
    SYNTHESIS,
    REPORT,
    ERROR
}

