package com.jetbrains.deepsearch.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Represents a search result from a search engine.
 */
@Serializable
data class SearchResult(
    val title: String,
    val url: String,
    val snippet: String,
    val publishedDate: String? = null
)

/**
 * Represents a verified and evaluated source of information.
 */
data class Source(
    val url: String,
    val title: String,
    val content: String,
    val publishedDate: Instant? = null,
    val credibilityScore: CredibilityScore,
    val extractedClaims: List<Claim> = emptyList()
)

/**
 * Credibility evaluation score for a source.
 */
data class CredibilityScore(
    val authority: Double,           // Domain authority, TLD quality
    val recency: Double,             // How recent is the information
    val objectivity: Double,         // Bias detection, tone analysis
    val diversityContribution: Double, // How much it adds new perspective
    val coherence: Double,           // Consistency with other sources
    val overall: Double              // Weighted average
) {
    companion object {
        fun calculate(
            authority: Double,
            recency: Double,
            objectivity: Double,
            diversityContribution: Double,
            coherence: Double
        ): CredibilityScore {
            val overall = (authority * 0.25 + recency * 0.15 + objectivity * 0.25 + 
                          diversityContribution * 0.15 + coherence * 0.20)
            return CredibilityScore(
                authority, recency, objectivity, diversityContribution, coherence, overall
            )
        }
    }
}

/**
 * Represents an extracted claim from a source.
 */
data class Claim(
    val statement: String,
    val sourceUrl: String,
    val confidence: Double,
    val supportingEvidence: List<String> = emptyList(),
    val contradictingEvidence: List<String> = emptyList(),
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED
)

/**
 * Status of claim verification through cross-checking.
 */
enum class VerificationStatus {
    VERIFIED,           // Confirmed by multiple sources
    LIKELY_TRUE,        // Supported by some sources
    CONFLICTING,        // Mixed evidence
    LIKELY_FALSE,       // Contradicted by sources
    UNVERIFIED          // Not enough information
}

