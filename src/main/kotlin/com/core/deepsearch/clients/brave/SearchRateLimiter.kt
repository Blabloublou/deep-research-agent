package com.core.deepsearch.clients.brave

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong


/**
 * Rate limiter to respect Brevo API rate limits.
 * Thread-safe implementation using atomic operations.
 */
class SearchRateLimiter(
    private val minRequestIntervalMs: Long = 1000L // 1 second
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
            delay(waitTime)
        }

        lastRequestTime.set(System.currentTimeMillis())
    }
}

