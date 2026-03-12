package com.jetbrains.deepsearch.api

import com.jetbrains.deepsearch.DeepResearchAgent
import com.jetbrains.deepsearch.api.routes.agentRoutes
import com.jetbrains.deepsearch.api.routes.researchRoutes
import com.jetbrains.deepsearch.api.services.AgentService
import com.jetbrains.deepsearch.api.services.ResearchService
import com.jetbrains.deepsearch.util.ConfigLoader
import com.jetbrains.deepsearch.util.DatabaseFactory
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import java.time.Duration

fun startApiServer(
    agent: DeepResearchAgent,
    port: Int = 8080,
    host: String = "0.0.0.0"
) {
    DatabaseFactory.init(dbPath = ConfigLoader.get("DB_PATH", "deep_research.db"))
    
    val researchService = ResearchService(agent)
    val agentService = AgentService()
     
    embeddedServer(CIO, port = port, host = host) {
        configureServer(researchService, agentService)
    }.start(wait = true)
}

/**
 * Configure the Ktor server.
 */
fun Application.configureServer(researchService: ResearchService, agentService: AgentService) {
    
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    
    install(CORS) {
        anyHost()
        allowHeader("Content-Type")
        allowMethod(io.ktor.http.HttpMethod.Options)
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
    }
    
    routing {
        researchRoutes(researchService, agentService)
        agentRoutes(agentService)
    }
}

