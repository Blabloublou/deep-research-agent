package com.jetbrains.deepsearch.clients.brave

import com.jetbrains.deepsearch.model.SearchResult
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Search client using Brave Search API.
 */
class BraveSearchClient(
    private val braveApiKey: String
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val rateLimiter = SearchRateLimiter(minRequestIntervalMs = 1000L)

    // Client HTTP spécial pour Brave Search
    private val braveHttpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 30000
        }
        followRedirects = true
    }

    /**
     * Performs a search using Brave Search API.
     */
    suspend fun search(query: String, maxResults: Int = 10): List<SearchResult> {
        var retryCount = 0
        val maxRetries = 3
        var lastException: Exception? = null

        while (retryCount <= maxRetries) {
            try {
                rateLimiter.acquirePermit()

                val response: HttpResponse = braveHttpClient.get("https://api.search.brave.com/res/v1/web/search") {
                    parameter("q", query)
                    parameter("count", maxResults)
                    header("Accept", "*/*")
                    header("X-Subscription-Token", braveApiKey)
                }

                val body = response.bodyAsText()

                // Check HTTP status
                if (response.status.value !in 200..299) {
                    throw Exception("Brave Search HTTP ${response.status.value}: ${body.take(300)}")
                }

                // Parse response
                val braveResponse = try {
                    json.decodeFromString(BraveSearchResponse.serializer(), body)
                } catch (e: Exception) {
                    throw Exception("Brave Search parse error: ${e.message}. Check API key.")
                }

                // Extract results
                val results = braveResponse.web?.results?.map { result ->
                    SearchResult(
                        title = result.title,
                        url = result.url,
                        snippet = result.description,
                        publishedDate = result.age
                    )
                } ?: emptyList()

                if (results.isEmpty()) {
                    logger.warn { "Brave Search returned no results for query: $query" }
                }

                return results

            } catch (e: Exception) {
                lastException = e

                if (retryCount >= maxRetries) {
                    break
                }

                retryCount++
            }
        }

        throw Exception("Brave Search API failed after $maxRetries retries: ${lastException?.message}", lastException)
    }
}

