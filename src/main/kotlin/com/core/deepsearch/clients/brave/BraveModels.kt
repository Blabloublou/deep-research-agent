package com.jetbrains.deepsearch.clients.brave

import kotlinx.serialization.Serializable

/**
 * Brave Search API response models.
 */
@Serializable
internal data class BraveSearchResponse(
    val web: WebResults?
)

@Serializable
internal data class WebResults(
    val results: List<BraveResult>
)

@Serializable
internal data class BraveResult(
    val title: String,
    val url: String,
    val description: String,
    val age: String? = null
)

