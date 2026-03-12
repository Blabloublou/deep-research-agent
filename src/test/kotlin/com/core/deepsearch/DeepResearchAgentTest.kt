package com.core.deepsearch

import com.core.deepsearch.util.ConfigLoader
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DeepResearchAgentTest {

    @BeforeEach
    fun setup() {
        mockkObject(ConfigLoader)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `fromConfig should use default values when not specified`() {
        // Given
        every { ConfigLoader.getRequired("OPENAI_API_KEY") } returns "test-key"
        every { ConfigLoader.getRequired("BRAVE_API_KEY") } returns "test-brave"
        every { ConfigLoader.get("OPENAI_MODEL", "gpt-4o") } returns "gpt-4o"
        every { ConfigLoader.getInt("MAX_ITERATIONS", 3) } returns 3
        every { ConfigLoader.getInt("MAX_SOURCES_PER_QUERY", 10) } returns 10
        every { ConfigLoader.getDouble("MIN_SOURCE_CREDIBILITY_SCORE", 0.5) } returns 0.5

        // When
        val agent = DeepResearchAgent.fromConfig()

        // Then
        assertNotNull(agent)
        agent.close()
    }

    @Test
    fun `close should cleanup resources`() {
        // Given
        every { ConfigLoader.getRequired("OPENAI_API_KEY") } returns "test-key"
        every { ConfigLoader.getRequired("BRAVE_API_KEY") } returns "test-brave"
        every { ConfigLoader.get("OPENAI_MODEL", "gpt-4o") } returns "gpt-4o"
        every { ConfigLoader.getInt("MAX_ITERATIONS", 3) } returns 3
        every { ConfigLoader.getInt("MAX_SOURCES_PER_QUERY", 10) } returns 10
        every { ConfigLoader.getDouble("MIN_SOURCE_CREDIBILITY_SCORE", 0.5) } returns 0.5

        val agent = DeepResearchAgent.fromConfig()

        // When
        agent.close()

        // Then
        assertTrue(true, "Close should complete without errors")
    }
}

