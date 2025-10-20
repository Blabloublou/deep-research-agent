package com.jetbrains.deepsearch.clients.openai

import com.jetbrains.deepsearch.model.Message
import com.jetbrains.deepsearch.model.OpenAIRequest
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Client for interacting with OpenAI's API.
 */
class OpenAIClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val model: String = "gpt-4o"
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Sends a chat completion request to OpenAI.
     */
    suspend fun chatCompletion(
        messages: List<Message>,
        temperature: Double = 0.7,
        maxTokens: Int? = null
    ): String {
        return try {
            val request = OpenAIRequest(
                model = model,
                messages = messages,
                temperature = temperature,
                max_tokens = maxTokens
            )

            val response: HttpResponse = httpClient.post("https://api.openai.com/v1/chat/completions") {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(OpenAIRequest.serializer(), request))
            }

            val responseBody = response.bodyAsText()

            OpenAIResponseParser.parseResponse(responseBody, response.status.value)

        } catch (e: Exception) {
            logger.error(e) { "Error calling OpenAI API" }
            throw Exception("OpenAI API error: ${e.message}", e)
        }
    }

    /**
     * Generates a research plan for a given topic.
     */
    suspend fun generateResearchPlan(topic: String, iterations: Int = 3): String {
        return chatCompletion(
            messages = OpenAIPrompts.researchPlan(topic, iterations),
            temperature = 0.8
        )
    }

    /**
     * Extracts key claims from source content.
     */
    suspend fun extractClaims(content: String, sourceUrl: String): String {
        return chatCompletion(
            messages = OpenAIPrompts.extractClaims(content),
            temperature = 0.3
        )
    }

    /**
     * Synthesizes information from multiple sources.
     */
    suspend fun synthesizeInformation(
        topic: String,
        sources: List<String>,
        claims: List<String>
    ): String {
        return chatCompletion(
            messages = OpenAIPrompts.synthesizeInformation(topic, sources, claims),
            temperature = 0.5
        )
    }

    /**
     * Analyzes contradictions between claims.
     */
    suspend fun analyzeContradiction(claim1: String, claim2: String): String {
        return chatCompletion(
            messages = OpenAIPrompts.analyzeContradiction(claim1, claim2),
            temperature = 0.4
        )
    }

    /**
     * Generates the final research report.
     */
    suspend fun generateReport(
        topic: String,
        synthesis: String,
        metrics: String
    ): String {
        return chatCompletion(
            messages = OpenAIPrompts.generateReport(topic, synthesis, metrics),
            temperature = 0.6,
            maxTokens = 3000
        )
    }
}

