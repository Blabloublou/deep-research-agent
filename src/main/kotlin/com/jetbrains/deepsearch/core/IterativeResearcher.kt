package com.jetbrains.deepsearch.core

import com.jetbrains.deepsearch.clients.openai.OpenAIClient
import com.jetbrains.deepsearch.clients.SearchClient
import com.jetbrains.deepsearch.eval.ClaimExtractor
import com.jetbrains.deepsearch.eval.CrossChecker
import com.jetbrains.deepsearch.eval.SourceEvaluator
import com.jetbrains.deepsearch.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger {}

/**
 * Core iterative research engine.
 */
class IterativeResearcher(
    private val searchClient: SearchClient,
    private val openAIClient: OpenAIClient,
    private val sourceEvaluator: SourceEvaluator,
    private val claimExtractor: ClaimExtractor,
    private val crossChecker: CrossChecker,
    private val researchPlanner: ResearchPlanner,
    private val minCredibilityScore: Double = 0.5,
    private val maxSourcesPerQuery: Int = 10
) {

    /**
     * Conducts deep research with progress callbacks.
     */
    suspend fun conductResearchWithCallbacks(
        topic: String,
        maxIterations: Int = 3,
        onIterationStart: suspend (iteration: Int, total: Int) -> Unit = { _, _ -> },
        onQuery: suspend (query: String) -> Unit = {},
        onSourceFound: suspend (url: String, title: String) -> Unit = { _, _ -> },
        onClaimExtracted: suspend (claim: String) -> Unit = {},
        onSynthesis: suspend (iteration: Int) -> Unit = {}
    ): ResearchContext = coroutineScope {

        val context = ResearchContext(topic = topic, currentIteration = 1)

        val plan = researchPlanner.createResearchPlan(topic, maxIterations)

        for (iteration in 1..maxIterations) {
            context.currentIteration = iteration
            onIterationStart(iteration, maxIterations)

            val iterationQueries = if (iteration == 1) {
                plan.queries.filter { it.iteration == 1 }
            } else {
                researchPlanner.refineResearchPlan(
                    plan,
                    iteration,
                    context.insights,
                    context.gaps
                )
            }

            val newSources = iterationQueries.flatMap { query ->
                onQuery(query.query)
                executeQueryWithCallbacks(query, context, onSourceFound, onClaimExtracted)
            }

            context.sources.addAll(newSources)

            val newClaims = newSources.flatMap { source ->
                source.extractedClaims
            }
            context.claims.addAll(newClaims)

            // Cross-check claims
            val verifiedClaims = crossChecker.crossCheckClaims(context.claims)
            context.claims.clear()
            context.claims.addAll(verifiedClaims)

            // Synthesize insights
            onSynthesis(iteration)
            val synthesis = synthesizeIteration(context)
            context.insights.add(synthesis)

            // Identify gaps
            val gaps = identifyGaps(context)
            context.gaps.clear()
            context.gaps.addAll(gaps)
        }

        context
    }

    /**
     * Executes a single research query with callbacks.
     */
    private suspend fun executeQueryWithCallbacks(
        query: ResearchQuery,
        context: ResearchContext,
        onSourceFound: suspend (url: String, title: String) -> Unit,
        onClaimExtracted: suspend (claim: String) -> Unit
    ): List<Source> = coroutineScope {

        val searchResults = searchClient.search(query.query, maxSourcesPerQuery)

        val sources = searchResults.map { result ->
            async {
                try {
                    val content = searchClient.fetchContent(result.url)
                    
                    if (content.isBlank()) {
                        logger.debug { "Empty content from ${result.url}" }
                        return@async null
                    }

                    val publishedDate = parseDate(result.publishedDate)
                    val existingContents = context.sources.map { it.content }
                    
                    val credibilityScore = sourceEvaluator.evaluateSource(
                        result,
                        content,
                        existingContents,
                        publishedDate
                    )

                    if (credibilityScore.overall < minCredibilityScore) {
                        return@async null
                    }

                    val claims = claimExtractor.extractClaims(content, result.url)
                    
                    onSourceFound(result.url, result.title)
                    claims.forEach { claim ->
                        onClaimExtracted(claim.statement)
                    }

                    Source(
                        url = result.url,
                        title = result.title,
                        content = content,
                        publishedDate = publishedDate,
                        credibilityScore = credibilityScore,
                        extractedClaims = claims
                    )

                } catch (e: Exception) {
                    logger.error(e) { "Error processing ${result.url}" }
                    null
                }
            }
        }.awaitAll().filterNotNull()

        logger.info { "Processed ${sources.size} credible sources from query" }
        sources
    }

    /**
     * Synthesizes insights from the current iteration.
     */
    private suspend fun synthesizeIteration(context: ResearchContext): String {
        val sourceUrls = context.sources.map { it.url }
        val claimStatements = context.claims.map { it.statement }

        return try {
            openAIClient.synthesizeInformation(
                topic = context.topic,
                sources = sourceUrls,
                claims = claimStatements
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to synthesize iteration" }
            "Synthesis failed for iteration ${context.currentIteration}"
        }
    }

    /**
     * Identifies knowledge gaps for next iteration.
     */
    private fun identifyGaps(context: ResearchContext): List<String> {
        val gaps = mutableListOf<String>()

        val unverifiedClaims = context.claims.filter { 
            it.verificationStatus == VerificationStatus.UNVERIFIED 
        }
        if (unverifiedClaims.isNotEmpty()) {
            gaps.add("${unverifiedClaims.size} claims lack verification")
        }

        // Check for conflicting claims
        val conflictingClaims = context.claims.filter {
            it.verificationStatus == VerificationStatus.CONFLICTING
        }
        if (conflictingClaims.isNotEmpty()) {
            gaps.add("${conflictingClaims.size} claims have conflicting evidence")
        }

        // Check for low source diversity
        val domains = context.sources.map { 
            java.net.URI(it.url).host 
        }.distinct()
        if (domains.size < 5 && context.sources.size > 10) {
            gaps.add("Limited source diversity (only ${domains.size} unique domains)")
        }

        // Check credibility distribution
        val avgCredibility = context.sources.map { it.credibilityScore.overall }.average()
        if (avgCredibility < 0.7) {
            gaps.add("Average source credibility below optimal (${String.format("%.2f", avgCredibility)})")
        }

        return gaps
    }

    /**
     * Parses publication date string to Instant.
     */
    private fun parseDate(dateStr: String?): Instant? {
        if (dateStr == null) return null

        return try {
            val formatters = listOf(
                DateTimeFormatter.ISO_INSTANT,
                DateTimeFormatter.ISO_DATE_TIME,
                DateTimeFormatter.ISO_DATE
            )

            for (formatter in formatters) {
                try {
                    return Instant.from(formatter.parse(dateStr))
                } catch (e: Exception) {
                    continue
                }
            }

            null
        } catch (e: Exception) {
            logger.debug { "Failed to parse date: $dateStr" }
            null
        }
    }
}

