package com.core.deepsearch.clients.openai

import com.core.deepsearch.model.Message

/**
 * Centralized OpenAI prompts for research operations.
 */
object OpenAIPrompts {

    fun researchPlan(topic: String, iterations: Int): List<Message> {
        val systemPrompt = """
            You are an expert research planner. Your task is to create a comprehensive research plan
            that will uncover deep insights about a topic through progressive, iterative queries.
            
            Each query should build upon previous knowledge and explore different angles:
            - Start broad to understand the landscape
            - Then dive into specific aspects, mechanisms, or controversies
            - Consider multiple perspectives and stakeholders
            - Look for evidence, data, and expert opinions
            - Identify gaps and uncertainties
            
            Return the plan in JSON format with this structure:
            {
              "queries": [
                {
                  "query": "search query string",
                  "purpose": "what this query aims to discover",
                  "expected_insights": "what we hope to learn"
                }
              ],
              "rationale": "overall strategy explanation"
            }
        """.trimIndent()

        val userPrompt = """
            Create a research plan with ${iterations * 2} progressive queries for the topic:
            "$topic"
            
            The queries should be designed for $iterations iterations of research, with each iteration
            refining and deepening our understanding.
        """.trimIndent()

        return listOf(
            Message("system", systemPrompt),
            Message("user", userPrompt)
        )
    }

    fun extractClaims(content: String): List<Message> {
        val systemPrompt = """
            You are an expert at extracting factual claims from text. Identify the main claims,
            arguments, or assertions made in the content. Focus on:
            - Factual statements that can be verified
            - Key arguments or conclusions
            - Statistical data or research findings
            - Expert opinions or authoritative statements
            
            Return claims in JSON format:
            {
              "claims": [
                {
                  "statement": "the claim text",
                  "confidence": 0.0-1.0,
                  "type": "fact|opinion|data|conclusion"
                }
              ]
            }
        """.trimIndent()

        return listOf(
            Message("system", systemPrompt),
            Message("user", "Extract claims from this content:\n\n$content")
        )
    }

    fun synthesizeInformation(topic: String, sources: List<String>, claims: List<String>): List<Message> {
        val systemPrompt = """
            You are an expert research analyst. Synthesize information from multiple sources to create
            a comprehensive understanding of a topic. Your synthesis should:
            - Identify common themes and patterns
            - Highlight agreements and contradictions
            - Assess the strength of evidence
            - Note gaps or uncertainties
            - Provide balanced, nuanced insights
        """.trimIndent()

        val userPrompt = """
            Topic: $topic
            
            Sources reviewed: ${sources.size}
            Key claims identified: ${claims.size}
            
            Claims:
            ${claims.joinToString("\n") { "- $it" }}
            
            Provide a thematic synthesis identifying:
            1. Main themes and their supporting evidence
            2. Areas of consensus
            3. Contradictions or debates
            4. Limitations and uncertainties
        """.trimIndent()

        return listOf(
            Message("system", systemPrompt),
            Message("user", userPrompt)
        )
    }

    fun analyzeContradiction(claim1: String, claim2: String): List<Message> {
        val prompt = """
            Analyze the relationship between these two claims:
            
            Claim 1: $claim1
            Claim 2: $claim2
            
            Are they contradictory, complementary, or unrelated? Explain the nature of their relationship
            and potential reasons for any discrepancy.
        """.trimIndent()

        return listOf(
            Message("system", "You are an expert at analyzing logical relationships between statements."),
            Message("user", prompt)
        )
    }

    fun generateReport(topic: String, synthesis: String, metrics: String): List<Message> {
        val systemPrompt = """
            You are an expert research writer. Create a comprehensive research report in Markdown format.
            The report should be well-structured, evidence-based, and clearly written.
            
            Include:
            - Executive Summary
            - Thematic Analysis (organized by key themes)
            - Contradictions and Debates
            - Consensus Points
            - Limitations and Uncertainties
            - Research Metrics
        """.trimIndent()

        val userPrompt = """
            Topic: $topic
            
            Synthesis:
            $synthesis
            
            Metrics:
            $metrics
            
            Generate a comprehensive research report in Markdown format.
        """.trimIndent()

        return listOf(
            Message("system", systemPrompt),
            Message("user", userPrompt)
        )
    }
}

