package com.jetbrains.deepsearch

import com.jetbrains.deepsearch.clients.openai.OpenAIClient
import com.jetbrains.deepsearch.clients.SearchClient
import com.jetbrains.deepsearch.core.IterativeResearcher
import com.jetbrains.deepsearch.core.ResearchPlanner
import com.jetbrains.deepsearch.eval.ClaimExtractor
import com.jetbrains.deepsearch.eval.CrossChecker
import com.jetbrains.deepsearch.eval.SourceEvaluator
import com.jetbrains.deepsearch.report.ReportGenerator
import com.jetbrains.deepsearch.util.ConfigLoader
import com.jetbrains.deepsearch.util.HttpClientFactory
import io.ktor.client.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Main Deep Research Agent orchestrator.
 */
class DeepResearchAgent private constructor(
    private val openAIApiKey: String,
    private val braveApiKey: String,
    private val openAIModel: String = "gpt-4o",
    private val maxIterations: Int = 3,
    private val maxSourcesPerQuery: Int = 10,
    private val minCredibilityScore: Double = 0.5
) {
    private val httpClient: HttpClient = HttpClientFactory.create()
    
    private val openAIClient = OpenAIClient(httpClient, openAIApiKey, openAIModel)
    private val searchClient = SearchClient(httpClient, braveApiKey)
    private val sourceEvaluator = SourceEvaluator()
    private val claimExtractor = ClaimExtractor(openAIClient)
    private val crossChecker = CrossChecker(openAIClient)
    private val researchPlanner = ResearchPlanner(openAIClient)
    
    private val iterativeResearcher = IterativeResearcher(
        searchClient = searchClient,
        openAIClient = openAIClient,
        sourceEvaluator = sourceEvaluator,
        claimExtractor = claimExtractor,
        crossChecker = crossChecker,
        researchPlanner = researchPlanner,
        minCredibilityScore = minCredibilityScore,
        maxSourcesPerQuery = maxSourcesPerQuery
    )
    
    private val reportGenerator = ReportGenerator(openAIClient, crossChecker)

    /**
     * Conducts research
     */
    suspend fun researchWithOverridesWithCallbacks(
        topic: String,
        openAIModelOverride: String? = null,
        maxIterationsOverride: Int? = null,
        outputPath: String? = null,
        onIterationStart: suspend (iteration: Int, total: Int) -> Unit = { _, _ -> },
        onQuery: suspend (query: String) -> Unit = {},
        onSourceFound: suspend (url: String, title: String) -> Unit = { _, _ -> },
        onClaimExtracted: suspend (claim: String) -> Unit = {},
        onSynthesis: suspend (iteration: Int) -> Unit = {},
        onReportGeneration: suspend () -> Unit = {}
    ): String {
        val startTime = System.currentTimeMillis()
        val effectiveModel = openAIModelOverride ?: openAIModel
        val effectiveIterations = maxIterationsOverride ?: this.maxIterations

        val tempOpenAIClient = OpenAIClient(httpClient, openAIApiKey, effectiveModel)
        val tempClaimExtractor = ClaimExtractor(tempOpenAIClient)
        val tempCrossChecker = CrossChecker(tempOpenAIClient)
        val tempResearchPlanner = ResearchPlanner(tempOpenAIClient)
        val tempIterativeResearcher = IterativeResearcher(
            searchClient = searchClient,
            openAIClient = tempOpenAIClient,
            sourceEvaluator = sourceEvaluator,
            claimExtractor = tempClaimExtractor,
            crossChecker = tempCrossChecker,
            researchPlanner = tempResearchPlanner,
            minCredibilityScore = minCredibilityScore,
            maxSourcesPerQuery = maxSourcesPerQuery
        )
        val tempReportGenerator = ReportGenerator(tempOpenAIClient, tempCrossChecker)

        try {
            val context = tempIterativeResearcher.conductResearchWithCallbacks(
                topic = topic,
                maxIterations = effectiveIterations,
                onIterationStart = onIterationStart,
                onQuery = onQuery,
                onSourceFound = onSourceFound,
                onClaimExtracted = onClaimExtracted,
                onSynthesis = onSynthesis
            )

            val endTime = System.currentTimeMillis()

            onReportGeneration()
            val report = tempReportGenerator.generateReport(context, startTime, endTime)

            val reportPath = outputPath ?: "research_reports/${sanitizeFilename(topic)}_${System.currentTimeMillis()}.md"
            val reportFile = tempReportGenerator.exportToMarkdown(report, reportPath)
            return reportFile.absolutePath
        } catch (e: Exception) {
            logger.error(e) { "Research with overrides failed" }
            throw e
        }
    }

    /**
     * Closes resources.
     */
    fun close() {
        httpClient.close()
    }

    private fun sanitizeFilename(name: String): String {
        return name
            .replace(Regex("[^a-zA-Z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "_")
            .lowercase()
            .take(50)
    }

    companion object {
        /**
         * Creates an agent instance from configuration.
         */
        fun fromConfig(): DeepResearchAgent {
            val openAIApiKey = ConfigLoader.getRequired("OPENAI_API_KEY")
            val braveApiKey = ConfigLoader.getRequired("BRAVE_API_KEY")
            val openAIModel = ConfigLoader.get("OPENAI_MODEL", "gpt-4o")
            val maxIterations = ConfigLoader.getInt("MAX_ITERATIONS", 3)
            val maxSourcesPerQuery = ConfigLoader.getInt("MAX_SOURCES_PER_QUERY", 10)
            val minCredibilityScore = ConfigLoader.getDouble("MIN_SOURCE_CREDIBILITY_SCORE", 0.5)

            return DeepResearchAgent(
                openAIApiKey = openAIApiKey,
                braveApiKey = braveApiKey,
                openAIModel = openAIModel,
                maxIterations = maxIterations,
                maxSourcesPerQuery = maxSourcesPerQuery,
                minCredibilityScore = minCredibilityScore
            )
        }
    }
}

