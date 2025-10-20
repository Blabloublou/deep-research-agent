package com.jetbrains.deepsearch.api.routes

import com.jetbrains.deepsearch.api.services.AgentService
import com.jetbrains.deepsearch.model.api.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Agent management API routes.
 */
fun Route.agentRoutes(agentService: AgentService) {
    
    route("/api/agents") {
        
        // Create a new agent
        post {
            try {
                val request = call.receive<CreateAgentRequest>()
                
                if (request.name.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Agent name cannot be empty"))
                    return@post
                }
                
                val agent = agentService.createAgent(
                    name = request.name,
                    model = request.model,
                    temperature = request.temperature,
                    maxTokens = request.maxTokens,
                    maxIterations = request.maxIterations
                )
                
                call.respond(
                    HttpStatusCode.Created,
                    AgentResponse(
                        id = agent.id,
                        name = agent.name,
                        model = agent.model,
                        temperature = agent.temperature,
                        maxTokens = agent.maxTokens,
                        maxIterations = agent.maxIterations
                    )
                )
                
            } catch (e: Exception) {
                logger.error(e) { "Failed to create agent" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Failed to create agent"))
                )
            }
        }
        
        // List all agents
        get {
            try {
                val agents = agentService.listAgents()
                
                call.respond(
                    agents.map { agent ->
                        AgentResponse(
                            id = agent.id,
                            name = agent.name,
                            model = agent.model,
                            temperature = agent.temperature,
                            maxTokens = agent.maxTokens,
                            maxIterations = agent.maxIterations
                        )
                    }
                )
                
            } catch (e: Exception) {
                logger.error(e) { "Failed to list agents" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Failed to list agents"))
                )
            }
        }
        
        // Get agent by ID
        get("/{id}") {
            try {
                val id = call.parameters["id"]?.toLongOrNull()
                
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid agent ID"))
                    return@get
                }
                
                val agent = agentService.getAgent(id)
                
                if (agent == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Agent not found"))
                    return@get
                }
                
                call.respond(
                    AgentResponse(
                        id = agent.id,
                        name = agent.name,
                        model = agent.model,
                        temperature = agent.temperature,
                        maxTokens = agent.maxTokens,
                        maxIterations = agent.maxIterations
                    )
                )
                
            } catch (e: Exception) {
                logger.error(e) { "Failed to get agent" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Failed to get agent"))
                )
            }
        }
        
        // Update agent
        put("/{id}") {
            try {
                val id = call.parameters["id"]?.toLongOrNull()
                
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid agent ID"))
                    return@put
                }
                
                val request = call.receive<UpdateAgentRequest>()
                
                val agent = agentService.updateAgent(
                    id = id,
                    name = request.name,
                    model = request.model,
                    temperature = request.temperature,
                    maxTokens = request.maxTokens,
                    maxIterations = request.maxIterations
                )
                
                if (agent == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Agent not found"))
                    return@put
                }
                
                call.respond(
                    AgentResponse(
                        id = agent.id,
                        name = agent.name,
                        model = agent.model,
                        temperature = agent.temperature,
                        maxTokens = agent.maxTokens,
                        maxIterations = agent.maxIterations
                    )
                )
                
            } catch (e: Exception) {
                logger.error(e) { "Failed to update agent" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Failed to update agent"))
                )
            }
        }
        
        // Delete agent
        delete("/{id}") {
            try {
                val id = call.parameters["id"]?.toLongOrNull()
                
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid agent ID"))
                    return@delete
                }
                
                val deleted = agentService.deleteAgent(id)
                
                if (deleted) {
                    call.respond(mapOf("message" to "Agent deleted successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Agent not found"))
                }
                
            } catch (e: Exception) {
                logger.error(e) { "Failed to delete agent" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Failed to delete agent"))
                )
            }
        }
        
        // Get reports for a specific agent
        get("/{id}/reports") {
            try {
                val id = call.parameters["id"]?.toLongOrNull()
                
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid agent ID"))
                    return@get
                }
                
                val reports = agentService.getAgentReports(id)
                
                call.respond(
                    reports.map { report ->
                        SavedReportResponse(
                            id = report.id,
                            agentId = report.agentId,
                            topic = report.topic,
                            report = report.report,
                            createdAt = report.createdAt
                        )
                    }
                )
                
            } catch (e: Exception) {
                logger.error(e) { "Failed to get agent reports" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Failed to get agent reports"))
                )
            }
        }
    }
}

