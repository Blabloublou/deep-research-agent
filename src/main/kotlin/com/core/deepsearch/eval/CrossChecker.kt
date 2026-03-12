package com.core.deepsearch.eval

import com.core.deepsearch.clients.openai.OpenAIClient
import com.core.deepsearch.model.Claim
import com.core.deepsearch.model.Contradiction
import com.core.deepsearch.model.VerificationStatus

/**
 * Cross-checks claims against each other to identify contradictions,
 */
class CrossChecker(private val openAIClient: OpenAIClient) {

    /**
     * Cross-checks all claims to identify contradictions and support.
     */
    suspend fun crossCheckClaims(claims: List<Claim>): List<Claim> {
        val updatedClaims = claims.toMutableList();

        for (i in claims.indices) {
            for (j in i + 1 until claims.size) {
                val claim1 = claims[i]
                val claim2 = claims[j]

                if (areClaimsRelated(claim1.statement, claim2.statement)) {
                    val relationship = analyzeRelationship(claim1, claim2)
                    
                    when (relationship) {
                        ClaimRelationship.SUPPORTING -> {
                            updatedClaims[i] = claim1.copy(
                                supportingEvidence = claim1.supportingEvidence + claim2.statement
                            )
                        }
                        ClaimRelationship.CONTRADICTING -> {
                            updatedClaims[i] = claim1.copy(
                                contradictingEvidence = claim1.contradictingEvidence + claim2.statement
                            )
                        }
                        ClaimRelationship.UNRELATED -> {}
                    }
                }
            }
        }

        // Determine verification status for each claim
        return updatedClaims.map { claim ->
            claim.copy(verificationStatus = determineVerificationStatus(claim))
        }
    }

    /**
     * Finds contradictions between claims.
     */
    suspend fun findContradictions(claims: List<Claim>): List<Contradiction> {
        val contradictions = mutableListOf<Contradiction>()

        for (i in claims.indices) {
            for (j in i + 1 until claims.size) {
                val claim1 = claims[i]
                val claim2 = claims[j]

                if (claim2.statement in claim1.contradictingEvidence) {
                    val analysis = openAIClient.analyzeContradiction(
                        claim1.statement,
                        claim2.statement
                    )

                    contradictions.add(
                        Contradiction(
                            claim1 = claim1,
                            claim2 = claim2,
                            analysis = analysis
                        )
                    )
                }
            }
        }

        return contradictions
    }

    /**
     * Checks if two claims are semantically related.
     */
    private fun areClaimsRelated(claim1: String, claim2: String): Boolean {
        val words1 = claim1.lowercase().split(Regex("\\W+")).filter { it.length > 4 }.toSet()
        val words2 = claim2.lowercase().split(Regex("\\W+")).filter { it.length > 4 }.toSet()

        val overlap = words1.intersect(words2).size
        val total = words1.union(words2).size

        if (total == 0) return false
        return overlap.toDouble() / total > 0.3
    }

    /**
     * Analyzes the relationship between two claims.
     */
    private fun analyzeRelationship(claim1: Claim, claim2: Claim): ClaimRelationship {
        // Simple semantic similarity check
        val similarity = calculateSimilarity(claim1.statement, claim2.statement)

        // Check for negation words
        val negationWords = listOf("not", "no", "never", "neither", "nor", "without")
        val hasNegation = negationWords.any { 
            claim1.statement.lowercase().contains(it) != claim2.statement.lowercase().contains(it)
        }

        return when {
            similarity > 0.7 && !hasNegation -> ClaimRelationship.SUPPORTING
            similarity > 0.5 && hasNegation -> ClaimRelationship.CONTRADICTING
            else -> ClaimRelationship.UNRELATED
        }
    }

    /**
     * Calculates simple similarity between two statements.
     */
    private fun calculateSimilarity(text1: String, text2: String): Double {
        val words1 = text1.lowercase().split(Regex("\\W+")).toSet()
        val words2 = text2.lowercase().split(Regex("\\W+")).toSet()

        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size

        if (union == 0) return 0.0
        return intersection.toDouble() / union
    }

    /**
     * Determines verification status based on supporting/contradicting evidence.
     */
    private fun determineVerificationStatus(claim: Claim): VerificationStatus {
        val supportCount = claim.supportingEvidence.size
        val contradictCount = claim.contradictingEvidence.size

        return when {
            supportCount >= 2 && contradictCount == 0 -> VerificationStatus.VERIFIED
            supportCount >= 1 && contradictCount == 0 -> VerificationStatus.LIKELY_TRUE
            supportCount > 0 && contradictCount > 0 -> VerificationStatus.CONFLICTING
            supportCount == 0 && contradictCount >= 1 -> VerificationStatus.LIKELY_FALSE
            else -> VerificationStatus.UNVERIFIED
        }
    }

    private enum class ClaimRelationship {
        SUPPORTING,
        CONTRADICTING,
        UNRELATED
    }
}

