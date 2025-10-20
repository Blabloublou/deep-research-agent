package com.jetbrains.deepsearch.clients.brave

import kotlinx.coroutines.delay
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * Rate limiter to respect API rate limits.
 * Thread-safe implementation using atomic operations.
 */
class SearchRateLimiter(
    private val minRequestIntervalMs: Long = 1000L
) {
    private val lastRequestTime = AtomicLong(0)

    /**
     * Waits if necessary to respect the rate limit before making a request.
     */
    suspend fun acquirePermit() {
        val now = System.currentTimeMillis()
        val lastRequest = lastRequestTime.get()
        val timeSinceLastRequest = now - lastRequest

        if (timeSinceLastRequest < minRequestIntervalMs) {
            val waitTime = minRequestIntervalMs - timeSinceLastRequest
            logger.debug { "Rate limiting: waiting ${waitTime}ms before next request" }
            delay(waitTime)
        }

        lastRequestTime.set(System.currentTimeMillis())
    }
}

