package com.jetbrains.deepsearch.clients.openai

import com.jetbrains.deepsearch.model.OpenAIErrorResponse
import com.jetbrains.deepsearch.model.OpenAIResponse
import kotlinx.serialization.json.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Parses OpenAI API responses with robust fallbacks for different formats.
 */
object OpenAIResponseParser {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Parses OpenAI response.
     */
    fun parseResponse(responseBody: String, statusCode: Int): String {
        // Handle error responses
        if (statusCode !in 200..299) {
            val errorMessage = try {
                json.decodeFromString(OpenAIErrorResponse.serializer(), responseBody).error?.message
            } catch (_: Exception) {
                null
            }
            throw Exception("HTTP $statusCode from OpenAI: ${errorMessage ?: responseBody.take(300)}")
        }

        return try {
            val root = json.parseToJsonElement(responseBody).jsonObject

            root["choices"]?.let {
                val openAIResponse = json.decodeFromString(OpenAIResponse.serializer(), responseBody)
                val content = openAIResponse.choices.firstOrNull()?.message?.content
                    ?: throw Exception("No response from OpenAI")
                return content
            }

            root["output_text"]?.let { ot ->
                val texts = ot.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull }
                if (texts.isNotEmpty()) {
                    val content = texts.joinToString("\n")
                    return content
                }
            }

            root["output"]?.jsonArray?.firstOrNull()?.jsonObject?.let { firstOut ->
                val contentArr = firstOut["content"]?.jsonArray
                val text = contentArr?.firstNotNullOfOrNull { item ->
                    item.jsonObject["text"]?.jsonPrimitive?.contentOrNull
                }
                if (text != null) {
                    return text
                }
            }

            root["content"]?.jsonArray?.firstOrNull()?.jsonObject?.let { c0 ->
                val text = c0["text"]?.jsonPrimitive?.contentOrNull
                if (text != null) {
                    return text
                }
            }

            root["error"]?.let {
                val errorMessage = try {
                    json.decodeFromString(OpenAIErrorResponse.serializer(), responseBody).error?.message
                } catch (_: Exception) {
                    null
                }
                throw Exception("OpenAI API error (200 OK but contains error): ${errorMessage ?: responseBody.take(300)}")
            }

            logger.error { "Failed to parse OpenAI response. Full body: $responseBody" }
            throw Exception("Invalid OpenAI response format: missing 'choices' and no fallback fields. Body: ${responseBody.take(500)}")

        } catch (e: Exception) {
            logger.error { "Error parsing OpenAI response: ${e.message}. Full body: $responseBody" }
            throw Exception("OpenAI API error: ${e.message}", e)
        }
    }
}

