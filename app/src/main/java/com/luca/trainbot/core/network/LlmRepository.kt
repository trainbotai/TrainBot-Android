package com.luca.trainbot.core.network

/**
 * Wraps [LlmApiService] Retrofit calls in [ApiResult].
 * Mirrors iOS LLMService — coordinates all non-streaming LLM endpoints.
 */
class LlmRepository(private val api: LlmApiService) {

    suspend fun listSessions(): ApiResult<List<SessionSummary>> =
        safeApiCall { api.listSessions().sessions }

    suspend fun getSession(id: String): ApiResult<SessionDetail> =
        safeApiCall { api.getSession(id) }

    suspend fun createSession(
        name: String,
        examples: List<ExampleDto>,
        assignmentId: String? = null,
    ): ApiResult<SessionDetail> = safeApiCall {
        api.createSession(CreateSessionRequest(name, examples, assignmentId))
    }

    suspend fun addVersion(
        sessionId: String,
        examples: List<ExampleDto>,
    ): ApiResult<SessionDetail> = safeApiCall {
        api.addVersion(sessionId, AddVersionRequest(examples))
    }

    suspend fun deleteSession(id: String): ApiResult<Unit> =
        safeApiCall { api.deleteSession(id) }

    suspend fun listQueries(
        sessionId: String,
        version: Int? = null,
    ): ApiResult<List<ChatHistoryItem>> = safeApiCall {
        api.listQueries(sessionId, version).queries
    }

    suspend fun reportSession(
        sessionId: String,
        reason: String?,
    ): ApiResult<String> = safeApiCall {
        api.reportSession(sessionId, ReportSessionRequest(reason)).reportId
    }

    suspend fun getQuota(): ApiResult<QueryQuota> =
        safeApiCall { api.getQuota() }

    suspend fun listTeacherBots(): ApiResult<List<TeacherBot>> =
        safeApiCall { api.listTeacherBots().bots }
}
