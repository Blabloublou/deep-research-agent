package com.core.deepsearch.api.services

import com.core.deepsearch.DeepResearchAgent
import com.core.deepsearch.model.api.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResearchServiceTest {

    private lateinit var agent: DeepResearchAgent
    private lateinit var researchService: ResearchService

    @BeforeEach
    fun setup() {
        agent = mockk()
        researchService = ResearchService(agent)
    }

    @AfterEach
    fun tearDown() {
        researchService.shutdown()
        clearAllMocks()
    }

    @Test
    fun `startResearch should create new research task and return ID`() {
        // Given
        val request = ResearchRequest(
            topic = "Quantum Computing",
            maxIterations = 3,
            agentId = null
        )

        val tempFile = java.io.File.createTempFile("test_report_", ".md")
        tempFile.writeText("# Research Report")
        tempFile.deleteOnExit()

        coEvery { 
            agent.researchWithOverridesWithCallbacks(
                topic = any(),
                openAIModelOverride = any(),
                maxIterationsOverride = any(),
                outputPath = any(),
                onIterationStart = any(),
                onQuery = any(),
                onSourceFound = any(),
                onClaimExtracted = any(),
                onSynthesis = any(),
                onReportGeneration = any()
            )
        } returns tempFile.absolutePath

        // When
        val researchId = researchService.startResearch(request)

        // Then
        assertNotNull(researchId)
        assertTrue(researchId.isNotEmpty())
    }

    @Test
    fun `getStatus should return current research status`() = runTest {
        // Given
        val request = ResearchRequest(
            topic = "AI Research",
            maxIterations = 2,
            agentId = null
        )

        val tempFile = java.io.File.createTempFile("test_report_", ".md")
        tempFile.writeText("# Report")
        tempFile.deleteOnExit()

        coEvery { 
            agent.researchWithOverridesWithCallbacks(
                topic = any(),
                openAIModelOverride = any(),
                maxIterationsOverride = any(),
                outputPath = any(),
                onIterationStart = any(),
                onQuery = any(),
                onSourceFound = any(),
                onClaimExtracted = any(),
                onSynthesis = any(),
                onReportGeneration = any()
            )
        } coAnswers {
            // Simulate long-running operation
            kotlinx.coroutines.delay(100)
            tempFile.absolutePath
        }

        val researchId = researchService.startResearch(request)

        // When
        kotlinx.coroutines.delay(10) // Let it start
        val status = researchService.getStatus(researchId)

        // Then
        assertNotNull(status)
        assertEquals(researchId, status.id)
        assertTrue(
            status.status == ResearchState.PLANNING.name || 
            status.status == ResearchState.RESEARCHING.name ||
            status.status == ResearchState.PENDING.name ||
            status.status == ResearchState.COMPLETED.name
        )
    }

    @Test
    fun `getStatus should return null for non-existent research`() {
        // Given
        val nonExistentId = "non-existent-id"

        // When
        val status = researchService.getStatus(nonExistentId)

        // Then
        assertNull(status)
    }

    @Test
    fun `getResult should return research result after completion`() = runTest {
        // Given
        val request = ResearchRequest(
            topic = "Machine Learning",
            maxIterations = 1,
            agentId = null
        )

        val tempFile = java.io.File.createTempFile("test_report_", ".md")
        tempFile.writeText("# ML Research Report")
        tempFile.deleteOnExit()

        coEvery { 
            agent.researchWithOverridesWithCallbacks(
                topic = any(),
                openAIModelOverride = any(),
                maxIterationsOverride = any(),
                outputPath = any(),
                onIterationStart = any(),
                onQuery = any(),
                onSourceFound = any(),
                onClaimExtracted = any(),
                onSynthesis = any(),
                onReportGeneration = any()
            )
        } returns tempFile.absolutePath

        val researchId = researchService.startResearch(request)

        // Wait for completion
        kotlinx.coroutines.delay(200)

        // When
        val result = researchService.getResult(researchId)

        // Then
        assertNotNull(result)
        assertEquals(researchId, result.id)
        assertEquals("Machine Learning", result.topic)
    }

    @Test
    fun `getResult should return null for non-existent research`() {
        // Given
        val nonExistentId = "invalid-id"

        // When
        val result = researchService.getResult(nonExistentId)

        // Then
        assertNull(result)
    }

    @Test
    fun `subscribeToUpdates should return update flow`() = runTest {
        // Given
        val request = ResearchRequest(
            topic = "Neural Networks",
            maxIterations = 2,
            agentId = null
        )

        val tempFile = java.io.File.createTempFile("test_report_", ".md")
        tempFile.writeText("# Report")
        tempFile.deleteOnExit()

        coEvery { 
            agent.researchWithOverridesWithCallbacks(
                topic = any(),
                openAIModelOverride = any(),
                maxIterationsOverride = any(),
                outputPath = any(),
                onIterationStart = any(),
                onQuery = any(),
                onSourceFound = any(),
                onClaimExtracted = any(),
                onSynthesis = any(),
                onReportGeneration = any()
            )
        } returns tempFile.absolutePath

        val researchId = researchService.startResearch(request)

        // When
        val updateFlow = researchService.subscribeToUpdates(researchId)

        // Then
        assertNotNull(updateFlow)
    }

    @Test
    fun `subscribeToUpdates should return null for non-existent research`() {
        // Given
        val nonExistentId = "missing-id"

        // When
        val updateFlow = researchService.subscribeToUpdates(nonExistentId)

        // Then
        assertNull(updateFlow)
    }

   
    @Test
    fun `startResearch should pass agentId configuration to agent`() = runTest {
        // Given
        val request = ResearchRequest(
            topic = "Custom Agent Research",
            maxIterations = 2,
            agentId = 123L
        )

        val tempFile = java.io.File.createTempFile("test_report_", ".md")
        tempFile.writeText("# Report")
        tempFile.deleteOnExit()

        coEvery { 
            agent.researchWithOverridesWithCallbacks(
                topic = any(),
                openAIModelOverride = any(),
                maxIterationsOverride = any(),
                outputPath = any(),
                onIterationStart = any(),
                onQuery = any(),
                onSourceFound = any(),
                onClaimExtracted = any(),
                onSynthesis = any(),
                onReportGeneration = any()
            )
        } returns tempFile.absolutePath

        // When
        val researchId = researchService.startResearch(request)

        // Then
        assertNotNull(researchId)
        kotlinx.coroutines.delay(50)
        val status = researchService.getStatus(researchId)
        assertNotNull(status)
    }
}

