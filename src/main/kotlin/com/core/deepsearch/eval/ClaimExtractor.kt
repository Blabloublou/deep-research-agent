package com.jetbrains.deepsearch.eval

import com.jetbrains.deepsearch.clients.openai.OpenAIClient
import com.jetbrains.deepsearch.model.Claim
import com.jetbrains.deepsearch.model.VerificationStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Extracts claims from source content using LLM.
 */
class ClaimExtractor(private val openAIClient: OpenAIClient) {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Extracts structured claims from content.
     */
    suspend fun extractClaims(content: String, sourceUrl: String): List<Claim> {
        if (content.isBlank() || content.length < 100) {
            return emptyList()
        }

        return try {
            val truncatedContent = content.take(4000)
            
            val response = openAIClient.extractClaims(truncatedContent, sourceUrl)
            parseClaims(response, sourceUrl)

        } catch (e: Exception) {
            logger.error(e) { "Failed to extract claims from $sourceUrl" }
            emptyList()
        }
    }

    /**
     * Parses claims from LLM response.
     */
    private fun parseClaims(response: String, sourceUrl: String): List<Claim> {
        return try {
            val jsonStart = response.indexOf('{')
            val jsonEnd = response.lastIndexOf('}') + 1
            
            if (jsonStart == -1 || jsonEnd <= jsonStart) {
                logger.warn { "No JSON found in claim extraction response" }
                return emptyList()
            }

            val jsonStr = response.substring(jsonStart, jsonEnd)
            val claimsData = json.decodeFromString(ClaimsResponse.serializer(), jsonStr)

            claimsData.claims.map { claimData ->
                Claim(
                    statement = claimData.statement,
                    sourceUrl = sourceUrl,
                    confidence = claimData.confidence,
                    supportingEvidence = emptyList(),
                    contradictingEvidence = emptyList(),
                    verificationStatus = VerificationStatus.UNVERIFIED
                )
            }

        } catch (e: Exception) {
            logger.error(e) { "Failed to parse claims response" }
            emptyList()
        }
    }
}

@Serializable
private data class ClaimsResponse(
    val claims: List<ClaimData>
)

@Serializable
private data class ClaimData(
    val statement: String,
    val confidence: Double,
    val type: String? = null
)

