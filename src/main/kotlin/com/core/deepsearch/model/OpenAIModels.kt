package com.core.deepsearch.model

import kotlinx.serialization.Serializable

/**
 * OpenAI API request/response models.
 */
@Serializable
data class OpenAIRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.7,
    val max_tokens: Int? = null
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class OpenAIResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: Message,
    val finish_reason: String? = null
)

@Serializable
data class OpenAIErrorResponse(
    val error: OpenAIError? = null
)

@Serializable
data class OpenAIError(
    val message: String? = null,
    val type: String? = null,
    val param: String? = null,
    val code: String? = null
)

