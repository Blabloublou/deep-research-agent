package com.jetbrains.deepsearch.api.routes

import com.jetbrains.deepsearch.api.services.AgentService
import com.jetbrains.deepsearch.api.services.ResearchService
import com.jetbrains.deepsearch.model.api.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
private val json = Json { prettyPrint = true }

/**
 * Configure research API routes.
 */
fun Routing.researchRoutes(service: ResearchService, agentService: AgentService) {
    
    route("/api") {
        
        // Health check
        get("/health") {
            call.respond(HealthResponse(status = "ok"))
        }
        
        route("/research") {
            
            post {
                try {
                    val request = call.receive<ResearchRequest>()
                                        
                    if (request.topic.isBlank()) {
                        call.respond(
                            io.ktor.http.HttpStatusCode.BadRequest,
                            mapOf("error" to "Topic cannot be empty")
                        )
                        return@post
                    }
                    
                    if (request.maxIterations < 1 || request.maxIterations > 5) {
                        call.respond(
                            io.ktor.http.HttpStatusCode.BadRequest,
                            mapOf("error" to "Max iterations must be between 1 and 5")
                        )
                        return@post
                    }
                    
                    val id = service.startResearch(request)
                    
                    call.respond(
                        ResearchResponse(
                            id = id,
                            status = "started",
                            message = "Research started successfully"
                        )
                    )
                    
                } catch (e: Exception) {
                    logger.error(e) { "Error starting research" }
                    call.respond(
                        io.ktor.http.HttpStatusCode.InternalServerError,
                        mapOf("error" to "Failed to start research: ${e.message}")
                    )
                }
            }
            
            // Get research status
            get("/{id}/status") {
                val id = call.parameters["id"]
                
                if (id == null) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.BadRequest,
                        mapOf("error" to "Research ID required")
                    )
                    return@get
                }
                
                val status = service.getStatus(id)
                
                if (status == null) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.NotFound,
                        mapOf("error" to "Research not found")
                    )
                    return@get
                }
                
                call.respond(status)
            }
            
            
            // WebSocket for real-time updates
            webSocket("/{id}/stream") {
                val id = call.parameters["id"]
                
                if (id == null) {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Research ID required"))
                    return@webSocket
                }
                
                val updates = service.subscribeToUpdates(id)
                
                if (updates == null) {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Research not found"))
                    return@webSocket
                }
                
                try {
                    updates.collect { update ->
                        send(Frame.Text(json.encodeToString(update)))
                    }
                    
                } catch (e: Exception) {
                    logger.error(e) { "WebSocket error for research $id" }
                } finally {
                    logger.info { "WebSocket closed for research $id" }
                }
            }
        }
        
        // Routes for saved reports
        route("/reports") {       
            get {
                try {
                    val agentId = call.request.queryParameters["agentId"]?.toLongOrNull()
                    val reports = agentService.listReports(agentId)
                    
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
                    logger.error(e) { "Failed to list reports" }
                    call.respond(
                        io.ktor.http.HttpStatusCode.InternalServerError,
                        mapOf("error" to "Failed to list reports: ${e.message}")
                    )
                }
            }
            
            // Get specific report by ID
            get("/{id}") {
                try {
                    val id = call.parameters["id"]?.toLongOrNull()
                    
                    if (id == null) {
                        call.respond(
                            io.ktor.http.HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid report ID")
                        )
                        return@get
                    }
                    
                    val report = agentService.getReport(id)
                    
                    if (report == null) {
                        call.respond(
                            io.ktor.http.HttpStatusCode.NotFound,
                            mapOf("error" to "Report not found")
                        )
                        return@get
                    }
                    
                    call.respond(
                        SavedReportResponse(
                            id = report.id,
                            agentId = report.agentId,
                            topic = report.topic,
                            report = report.report,
                            createdAt = report.createdAt
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Failed to get report" }
                    call.respond(
                        io.ktor.http.HttpStatusCode.InternalServerError,
                        mapOf("error" to "Failed to get report: ${e.message}")
                    )
                }
            }
        }
    }
}

