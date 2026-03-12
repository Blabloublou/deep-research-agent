package com.core.deepsearch.clients.parsers

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import mu.KotlinLogging
import org.jsoup.Jsoup

private val logger = KotlinLogging.logger {}

/**
 * Extracts main textual content from HTML pages.
 */
class HtmlContentExtractor(
    private val httpClient: HttpClient
) {
    /**
     * Fetches and extracts main content from a URL.
     */
    suspend fun fetchContent(url: String): String {
        return try {
            val response: HttpResponse = httpClient.get(url) {
                header("User-Agent", "Mozilla/5.0 (compatible; DeepResearchBot/1.0)")
            }

            val html = response.bodyAsText()
            extractMainContent(html)

        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch content from $url" }
            ""
        }
    }

    /**
     * Extracts main textual content from HTML using Jsoup.
     */
    private fun extractMainContent(html: String): String {
        return try {
            val doc = Jsoup.parse(html)

            doc.select("script, style, nav, header, footer, aside, .advertisement").remove()

            val mainContent = doc.selectFirst("main, article, .content, .post, #content")
                ?: doc.body()

            val paragraphs = mainContent.select("p")
                .map { it.text() }
                .filter { it.length > 50 } 
                .joinToString("\n\n")

            paragraphs.take(5000)

        } catch (e: Exception) {
            logger.error(e) { "Failed to extract content" }
            ""
        }
    }
}

