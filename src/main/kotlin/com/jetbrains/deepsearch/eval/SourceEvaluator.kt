package com.jetbrains.deepsearch.eval

import com.jetbrains.deepsearch.model.CredibilityScore
import com.jetbrains.deepsearch.model.Source
import mu.KotlinLogging
import java.net.URL
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.exp

private val logger = KotlinLogging.logger {}

/**
 * Evaluates the credibility of sources based on multiple dimensions.
 */
class SourceEvaluator {
    
    companion object {
        // High-quality TLDs
        private val TRUSTED_TLDS = setOf("edu", "gov", "org", "ac")
        
        // Academic and scientific domains
        private val ACADEMIC_DOMAINS = setOf(
            "arxiv.org", "scholar.google", "pubmed.ncbi.nlm.nih.gov", 
            "ieee.org", "acm.org", "springer.com", "sciencedirect.com",
            "nature.com", "science.org", "cell.com", "thelancet.com"
        )
        
        // News outlets with fact-checking reputation
        private val CREDIBLE_NEWS = setOf(
            "reuters.com", "apnews.com", "bbc.com", "nytimes.com",
            "wsj.com", "ft.com", "economist.com", "theguardian.com"
        )
        
        // Bias keywords that reduce objectivity score
        private val BIAS_KEYWORDS = setOf(
            "shocking", "unbelievable", "click here", "you won't believe",
            "secret", "they don't want you to know", "miracle", "guaranteed",
            "scam", "hoax", "conspiracy", "exclusive leak"
        )
    }
    
    /**
     * Evaluate the credibility of a source.
     */
    fun evaluateSource(
        searchResult: com.jetbrains.deepsearch.model.SearchResult,
        content: String,
        existingContents: List<String>,
        publishedDate: Instant?
    ): CredibilityScore {
        val authority = calculateAuthority(searchResult.url)
        val recency = calculateRecency(publishedDate)
        val objectivity = calculateObjectivity(content)
        val diversity = calculateDiversity(content, existingContents)
        val coherence = calculateCoherence(content, existingContents)
        
        return CredibilityScore.calculate(
            authority = authority,
            recency = recency,
            objectivity = objectivity,
            diversityContribution = diversity,
            coherence = coherence
        )
    }
    
    /**
     * Calculate authority score based on domain reputation and TLD.
     */
    private fun calculateAuthority(url: String): Double {
        return try {
            val domain = URL(url).host.lowercase()
            val tld = domain.substringAfterLast('.')
            
            var score = 0.5 // Base score
            
            // Academic/scientific domains get highest score
            if (ACADEMIC_DOMAINS.any { domain.contains(it) }) {
                score += 0.4
            }
            
            // Credible news outlets
            if (CREDIBLE_NEWS.any { domain.contains(it) }) {
                score += 0.3
            }
            
            // High-quality TLDs
            if (tld in TRUSTED_TLDS) {
                score += 0.2
            }
            
            // Wikipedia bonus (good for overview, but not primary source)
            if (domain.contains("wikipedia.org")) {
                score += 0.15
            }
            
            score.coerceIn(0.0, 1.0)
        } catch (e: Exception) {
            logger.warn { "Failed to parse URL for authority calculation: $url" }
            0.5
        }
    }
    
    /**
     * Calculate recency score based on publication date.
     * More recent = higher score, with exponential decay.
     */
    private fun calculateRecency(publishedDate: Instant?): Double {
        if (publishedDate == null) return 0.5 // Unknown date gets neutral score
        
        val now = Instant.now()
        val daysSincePublication = ChronoUnit.DAYS.between(publishedDate, now)
        
        // Exponential decay: 1.0 for today, 0.5 at 180 days, approaches 0 after that
        val halfLifeDays = 180.0
        val score = exp(-daysSincePublication / halfLifeDays)
        
        return score.coerceIn(0.0, 1.0)
    }
    
    /**
     * Calculate objectivity score by detecting bias indicators.
     */
    private fun calculateObjectivity(content: String): Double {
        val lowerContent = content.lowercase()
        
        var score = 1.0
        
        // Deduct points for bias keywords
        val biasCount = BIAS_KEYWORDS.count { lowerContent.contains(it) }
        score -= biasCount * 0.1
        
        // Excessive use of exclamation marks or ALL CAPS
        val exclamationCount = content.count { it == '!' }
        if (exclamationCount > 5) score -= 0.1
        
        val capsWords = content.split(" ").count { word ->
            word.length > 3 && word.all { it.isUpperCase() || !it.isLetter() }
        }
        if (capsWords > 3) score -= 0.1
        
        // Very short content might be low quality
        if (content.length < 200) score -= 0.2
        
        return score.coerceIn(0.0, 1.0)
    }
    
    /**
     * Calculate how much this source adds diversity to existing sources.
     * Measures vocabulary uniqueness and perspective novelty.
     */
    private fun calculateDiversity(content: String, existingContents: List<String>): Double {
        if (existingContents.isEmpty()) return 1.0
        
        // Extract significant words (> 4 characters)
        val sourceWords = extractSignificantWords(content)
        
        // Calculate overlap with existing sources
        val existingWords = existingContents
            .flatMap { extractSignificantWords(it) }
            .toSet()
        
        val newWords = sourceWords - existingWords
        val diversityRatio = if (sourceWords.isEmpty()) 0.0 
                            else newWords.size.toDouble() / sourceWords.size
        
        return diversityRatio.coerceIn(0.0, 1.0)
    }
    
    /**
     * Calculate coherence with existing sources.
     * High coherence means claims align with other sources.
     */
    private fun calculateCoherence(content: String, existingContents: List<String>): Double {
        if (existingContents.isEmpty()) return 0.5 // Neutral for first source
        
        // Simplified coherence: check keyword overlap
        val sourceWords = extractSignificantWords(content)
        val existingWords = existingContents
            .flatMap { extractSignificantWords(it) }
            .toSet()
        
        val commonWords = sourceWords.intersect(existingWords)
        val coherenceRatio = if (sourceWords.isEmpty()) 0.0
                            else commonWords.size.toDouble() / sourceWords.size
        
        // High overlap = high coherence
        return coherenceRatio.coerceIn(0.0, 1.0)
    }
    
    /**
     * Extract significant words from content (lowercase, length > 4, not common stopwords).
     */
    private fun extractSignificantWords(content: String): Set<String> {
        val stopwords = setOf(
            "the", "this", "that", "with", "from", "have", "been",
            "were", "their", "which", "would", "there", "could", "about"
        )
        
        return content
            .lowercase()
            .split(Regex("\\W+"))
            .filter { it.length > 4 && it !in stopwords }
            .toSet()
    }
}

