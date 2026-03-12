package com.jetbrains.deepsearch

import com.jetbrains.deepsearch.api.startApiServer
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main() {

    try {
        val agent = DeepResearchAgent.fromConfig()

        startApiServer(agent, port = 8080, host = "0.0.0.0")

    } catch (e: Exception) {
        logger.error(e) { "Fatal error during initialization: ${e.message}" }
        System.exit(1)
    }
}

