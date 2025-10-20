package com.jetbrains.deepsearch.clients

import com.jetbrains.deepsearch.clients.brave.BraveSearchClient
import com.jetbrains.deepsearch.clients.parsers.HtmlContentExtractor
import com.jetbrains.deepsearch.model.SearchResult
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
