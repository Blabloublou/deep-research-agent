package com.core.deepsearch.clients

import com.core.deepsearch.clients.brave.BraveSearchClient
import com.core.deepsearch.clients.parsers.HtmlContentExtractor
import com.core.deepsearch.model.SearchResult
import io.ktor.client.*

class SearchClient(
    httpClient: HttpClient,
    braveApiKey: String
) {
    private val braveClient = BraveSearchClient(braveApiKey)
    private val contentExtractor = HtmlContentExtractor(httpClient)

    /**
     * Performs a search using Brave Search API.
     */
    suspend fun search(query: String, maxResults: Int = 10): List<SearchResult> {
        return braveClient.search(query, maxResults)
    }

    /**
     * Fetches and extracts main content from a URL.
     */
    suspend fun fetchContent(url: String): String {
        return contentExtractor.fetchContent(url)
    }
}
