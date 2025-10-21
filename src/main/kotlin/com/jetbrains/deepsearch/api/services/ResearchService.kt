package com.jetbrains.deepsearch.api.services

import com.jetbrains.deepsearch.DeepResearchAgent
import com.jetbrains.deepsearch.db.repositories.ReportRepository
import com.jetbrains.deepsearch.db.repositories.AgentRepository
import com.jetbrains.deepsearch.model.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import mu.KotlinLogging
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * Service to manage ongoing research tasks.
 * Handles research lifecycle and streaming updates.
 */
class ResearchService(
    private val agent: DeepResearchAgent
) {
    // Store active researches
    private val researches = ConcurrentHashMap<String, ResearchTask>()
    
    // Coroutine scope for background research
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val reportRepo = ReportRepository()
    private val agentRepo = AgentRepository()

    /**
     * Start a new research.
     */
    fun startResearch(request: ResearchRequest): String {
        val id = UUID.randomUUID().toString()
        
        val task = ResearchTask(
            id = id,
            topic = request.topic,
            maxIterations = request.maxIterations,
            startTime = System.currentTimeMillis()
        )
        
        researches[id] = task
        
        scope.launch {
            try {
                task.updateState(ResearchState.PLANNING)
                
                conductResearch(id, request)
                
            } catch (e: Exception) {
                logger.error(e) { "Research $id failed" }
                task.updateState(ResearchState.FAILED)
                task.error = e.message
                task.sendUpdate(UpdateType.ERROR, "Research failed: ${e.message}")
            }
        }
        
        return id
    }

    /**
     * Get research status.
     */
    fun getStatus(id: String): ResearchStatus? {
        val task = researches[id] ?: return null
        
        return ResearchStatus(
            id = id,
            status = task.state,
            progress = if (task.state == ResearchState.RESEARCHING) {
                ResearchProgress(
                    currentIteration = task.currentIteration,
                    totalIterations = task.maxIterations,
                    sourcesFound = task.sourcesFound,
                    claimsExtracted = task.claimsExtracted,
                    currentPhase = task.currentPhase
                )
            } else null,
            error = task.error
        )
    }

    /**
     * Get research result including report and metrics.
     */
    fun getResult(id: String): ResearchResult? {
        val task = researches[id] ?: return null
        return ResearchResult(
            id = task.id,
            topic = task.topic,
            status = task.state,
            report = task.report,
            metrics = task.metrics,
            startTime = task.startTime,
            endTime = task.endTime
        )
    }


    /**
     * Subscribe to research updates.
     */
    fun subscribeToUpdates(id: String): SharedFlow<StreamUpdate>? {
        return researches[id]?.updates
    }

    /**
     * Conduct the actual research.
     */
    private suspend fun conductResearch(id: String, request: ResearchRequest) {
        val task = researches[id] ?: return
        
        task.updateState(ResearchState.RESEARCHING)
        task.sendUpdate(UpdateType.STATUS, "Starting deep research on: ${request.topic} (0%)")
        
        try {
            val agentConfig = request.agentId?.let { agentRepo.findById(it) }
            val totalIterations = agentConfig?.maxIterations ?: request.maxIterations
            task.sendUpdate(UpdateType.STATUS, "Planning research strategy... (5%)")
            
            var currentProgress = 5
            
            val reportPath = agent.researchWithOverridesWithCallbacks(
                topic = request.topic,
                openAIModelOverride = agentConfig?.model,
                maxIterationsOverride = totalIterations,
                onIterationStart = { iteration, total ->
                    task.currentIteration = iteration
                    task.currentPhase = "Iteration $iteration/$total"
                    val iterationSize = 85.0 / total
                    currentProgress = (5 + (iteration - 1) * iterationSize).toInt()
                    task.sendUpdate(UpdateType.ITERATION, "Iteration $iteration/$total (${currentProgress}%)")
                },
                onQuery = { query ->
                    task.sendUpdate(UpdateType.QUERY, query)
                },
                onSourceFound = { url, title ->
                    task.sourcesFound++
                },
                onClaimExtracted = { claim ->
                    task.claimsExtracted++
                },
                onSynthesis = { iteration ->
                    val iterationSize = 85.0 / totalIterations
                    currentProgress = (5 + iteration * iterationSize).toInt().coerceAtMost(90)
                    task.sendUpdate(UpdateType.SYNTHESIS, "Synthesizing insights from iteration $iteration (${currentProgress}%)")
                },
                onReportGeneration = {
                    task.currentPhase = "Generating final report"
                    task.sendUpdate(UpdateType.STATUS, "Generating report... (95%)")
                }
            )
            
            val report = java.io.File(reportPath).readText()
            
            task.updateState(ResearchState.GENERATING_REPORT)
            task.sendUpdate(UpdateType.REPORT, report)
            
            task.report = report
            task.updateState(ResearchState.COMPLETED)
            task.endTime = System.currentTimeMillis()
            
            task.metrics = ResearchMetricsApi(
                totalDurationMs = task.endTime!! - task.startTime,
                iterations = totalIterations,
                sourcesUsed = task.sourcesFound,
                claimsExtracted = task.claimsExtracted,
                claimsVerified = 0,
                averageCredibility = 0.0
            )
            
            task.sendUpdate(UpdateType.STATUS, "Research completed! (100%)")

            reportRepo.create(
                agentId = request.agentId,
                topic = request.topic,
                report = report
            )
            
        } catch (e: Exception) {
            logger.error(e) { "Research execution failed" }
            throw e
        }
    }

    /**
     * Clean up old researches.
     */
    fun cleanup(maxAge: Long = 3600000) { 
        val now = System.currentTimeMillis()
        researches.entries.removeIf { (_, task) ->
            task.endTime != null && (now - task.endTime!!) > maxAge
        }
    }

    /**
     * Shutdown the service.
     */
    fun shutdown() {
        scope.cancel()
    }
}

/**
 * Represents an ongoing research task.
 */
private class ResearchTask(
    val id: String,
    val topic: String,
    val maxIterations: Int,
    val startTime: Long
) {
    var state: ResearchState = ResearchState.PENDING
    var currentIteration: Int = 0
    var sourcesFound: Int = 0
    var claimsExtracted: Int = 0
    var currentPhase: String = "Initializing"
    var report: String? = null
    var metrics: ResearchMetricsApi? = null
    var endTime: Long? = null
    var error: String? = null
    
    private val _updates = MutableSharedFlow<StreamUpdate>(replay = 10)
    val updates: SharedFlow<StreamUpdate> = _updates
    
    fun updateState(newState: ResearchState) {
        state = newState
    }
    
    suspend fun sendUpdate(type: UpdateType, data: String) {
        _updates.emit(StreamUpdate(type, data))
    }
}

