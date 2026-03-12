package com.core.deepsearch.core

import com.core.deepsearch.clients.openai.OpenAIClient
import com.core.deepsearch.model.ResearchPlan
import com.core.deepsearch.model.ResearchQuery
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Plans research strategy 
 */
class ResearchPlanner(private val openAIClient: OpenAIClient) {

    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Generates a comprehensive research plan for a topic.
     */
    suspend fun createResearchPlan(topic: String, iterations: Int = 3): ResearchPlan {

        val planResponse = openAIClient.generateResearchPlan(topic, iterations)
        return parsePlanResponse(topic, planResponse, iterations)
    }

    /**
     * Refines the research plan based on insights from previous iteration.
     */
    suspend fun refineResearchPlan(
        originalPlan: ResearchPlan,
        currentIteration: Int,
        insights: List<String>,
        gaps: List<String>
    ): List<ResearchQuery> {
        val refinementPrompt = """
            Based on the research conducted so far on "${originalPlan.topic}", we need to refine our approach.
            
            Current iteration: $currentIteration
            
            Insights gathered:
            ${insights.joinToString("\n") { "- $it" }}
            
            Knowledge gaps identified:
            ${gaps.joinToString("\n") { "- $it" }}
            
            Generate 2-3 refined search queries that:
            1. Address the identified gaps
            2. Deepen understanding of key insights
            3. Explore contradictions or uncertainties
            4. Seek diverse perspectives
            
            Return in JSON format:
            {
              "queries": [
                {
                  "query": "refined search query",
                  "purpose": "what this aims to discover",
                  "expected_insights": "what we hope to learn"
                }
              ]
            }
        """.trimIndent()

        val response = openAIClient.chatCompletion(
            messages = listOf(
                com.core.deepsearch.model.Message("system", "You are an expert research strategist."),
                com.core.deepsearch.model.Message("user", refinementPrompt)
            ),
            temperature = 0.8
        )

        return parseRefinedQueries(response, currentIteration)
    }

    /**
     * Parses the initial plan response from LLM.
     */
    private fun parsePlanResponse(
        topic: String,
        response: String,
        iterations: Int
    ): ResearchPlan {
        return try {
            // Extract JSON from response
            val jsonStart = response.indexOf('{')
            val jsonEnd = response.lastIndexOf('}') + 1
            
            if (jsonStart == -1 || jsonEnd <= jsonStart) {
                logger.warn { "No JSON found in plan response, using fallback" }
                return createFallbackPlan(topic, iterations)
            }

            val jsonStr = response.substring(jsonStart, jsonEnd)
            val planData = json.decodeFromString(PlanResponse.serializer(), jsonStr)

            val queries = planData.queries.mapIndexed { index, queryData ->
                ResearchQuery(
                    query = queryData.query,
                    purpose = queryData.purpose,
                    expectedInsights = queryData.expected_insights,
                    iteration = index / (planData.queries.size / iterations) + 1
                )
            }

            ResearchPlan(
                topic = topic,
                queries = queries,
                rationale = planData.rationale ?: "Systematic exploration of the topic through progressive queries"
            )

        } catch (e: Exception) {
            logger.error(e) { "Failed to parse research plan, using fallback" }
            createFallbackPlan(topic, iterations)
        }
    }

    /**
     * Parses refined queries from LLM response.
     */
    private fun parseRefinedQueries(response: String, iteration: Int): List<ResearchQuery> {
        return try {
            val jsonStart = response.indexOf('{')
            val jsonEnd = response.lastIndexOf('}') + 1
            
            if (jsonStart == -1 || jsonEnd <= jsonStart) {
                return emptyList()
            }

            val jsonStr = response.substring(jsonStart, jsonEnd)
            val refinedData = json.decodeFromString(RefinedQueriesResponse.serializer(), jsonStr)

            refinedData.queries.map { queryData ->
                ResearchQuery(
                    query = queryData.query,
                    purpose = queryData.purpose,
                    expectedInsights = queryData.expected_insights,
                    iteration = iteration
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse refined queries" }
            emptyList()
        }
    }

    /**
     * Creates a fallback research plan if LLM fails.
     */
    private fun createFallbackPlan(topic: String, iterations: Int): ResearchPlan {
        val queries = listOf(
            ResearchQuery(
                query = "$topic overview",
                purpose = "Understand the basic concepts and landscape",
                expectedInsights = "Core definitions and context",
                iteration = 1
            ),
            ResearchQuery(
                query = "$topic latest research findings",
                purpose = "Discover recent developments and trends",
                expectedInsights = "Current state of knowledge",
                iteration = 1
            ),
            ResearchQuery(
                query = "$topic expert opinions analysis",
                purpose = "Gather expert perspectives",
                expectedInsights = "Professional insights and debates",
                iteration = 2
            ),
            ResearchQuery(
                query = "$topic practical applications case studies",
                purpose = "Explore real-world implementations",
                expectedInsights = "Practical implications and use cases",
                iteration = 2
            ),
            ResearchQuery(
                query = "$topic challenges limitations future",
                purpose = "Identify limitations and future directions",
                expectedInsights = "Critical analysis and future outlook",
                iteration = 3
            )
        )

        return ResearchPlan(
            topic = topic,
            queries = queries.take(iterations * 2),
            rationale = "Systematic exploration from overview to detailed analysis"
        )
    }
}

@Serializable
private data class PlanResponse(
    val queries: List<QueryData>,
    val rationale: String? = null
)

@Serializable
private data class QueryData(
    val query: String,
    val purpose: String,
    val expected_insights: String
)

@Serializable
private data class RefinedQueriesResponse(
    val queries: List<QueryData>
)

