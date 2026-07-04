package com.luca.trainbot.core.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service for the non-streaming LLM student endpoints.
 * All endpoints are under /student/llm/ (relative to base URL which already includes /api/v1/).
 * Authorization: Bearer token is attached via [AuthInterceptor] added to the OkHttpClient.
 *
 * The streaming query endpoint (/sessions/{id}/query) is handled separately
 * via raw OkHttp in [LlmStreamingRepository] — Retrofit cannot stream SSE.
 */
interface LlmApiService {

    /** GET /student/llm/sessions */
    @GET("student/llm/sessions")
    suspend fun listSessions(): SessionListResponse

    /** GET /student/llm/sessions/{id} */
    @GET("student/llm/sessions/{id}")
    suspend fun getSession(@Path("id") id: String): SessionDetail

    /** POST /student/llm/sessions */
    @POST("student/llm/sessions")
    suspend fun createSession(@Body body: CreateSessionRequest): SessionDetail

    /** POST /student/llm/sessions/{id}/versions */
    @POST("student/llm/sessions/{id}/versions")
    suspend fun addVersion(
        @Path("id") sessionId: String,
        @Body body: AddVersionRequest,
    ): SessionDetail

    /** DELETE /student/llm/sessions/{id} */
    @DELETE("student/llm/sessions/{id}")
    suspend fun deleteSession(@Path("id") id: String)

    /** GET /student/llm/sessions/{id}/queries?version={v} */
    @GET("student/llm/sessions/{id}/queries")
    suspend fun listQueries(
        @Path("id") sessionId: String,
        @Query("version") version: Int? = null,
    ): ChatHistoryResponse

    /** POST /student/llm/sessions/{id}/report */
    @POST("student/llm/sessions/{id}/report")
    suspend fun reportSession(
        @Path("id") sessionId: String,
        @Body body: ReportSessionRequest,
    ): ReportSessionResponse

    /** GET /student/llm/quota */
    @GET("student/llm/quota")
    suspend fun getQuota(): QueryQuota

    /** GET /student/llm/teacher-bots */
    @GET("student/llm/teacher-bots")
    suspend fun listTeacherBots(): TeacherBotListResponse
}
