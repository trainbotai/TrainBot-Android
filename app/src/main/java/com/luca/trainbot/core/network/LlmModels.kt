package com.luca.trainbot.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---- Shared domain types ----

@Serializable
data class ExampleDto(
    val user: String,
    val ai: String,
)

// ---- Session list ----

@Serializable
data class SessionSummary(
    val id: String,
    val name: String,
    @SerialName("assignmentId") val assignmentId: String? = null,
    val currentVersionNumber: Int,
    val versionsCount: Int,
    val queriesCount: Int,
    val flaggedCount: Int,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class SessionListResponse(
    val sessions: List<SessionSummary>,
)

// ---- Session detail ----

@Serializable
data class BotVersion(
    val id: String,
    val versionNumber: Int,
    val examples: List<ExampleDto>,
    val createdAt: String,
)

@Serializable
data class SessionDetail(
    val id: String,
    val name: String,
    @SerialName("assignmentId") val assignmentId: String? = null,
    val currentVersionNumber: Int,
    val versionsCount: Int,
    val queriesCount: Int,
    val flaggedCount: Int,
    val createdAt: String,
    val updatedAt: String,
    val versions: List<BotVersion>,
)

// ---- Create / update bodies ----

@Serializable
data class CreateSessionRequest(
    val name: String,
    val examples: List<ExampleDto>,
    @SerialName("assignmentId") val assignmentId: String? = null,
)

@Serializable
data class AddVersionRequest(
    val examples: List<ExampleDto>,
)

// ---- Query history ----

@Serializable
data class ChatHistoryItem(
    val id: String,
    val userPrompt: String,
    val aiResponse: String,
    val flagged: Boolean,
    val createdAt: String,
)

@Serializable
data class ChatHistoryResponse(
    val queries: List<ChatHistoryItem>,
)

// ---- Query request ----

@Serializable
data class QueryRequest(
    val prompt: String,
)

// ---- Quota ----

@Serializable
data class QueryQuota(
    val used: Int,
    val limit: Int,
    val remaining: Int,
    val resetsAt: String,
)

// ---- Report ----

@Serializable
data class ReportSessionRequest(
    val reason: String? = null,
)

@Serializable
data class ReportSessionResponse(
    val reportId: String,
)
