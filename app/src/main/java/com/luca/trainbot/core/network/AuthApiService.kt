package com.luca.trainbot.core.network

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit service for auth endpoints under /auth/.
 * Base URL already includes /api/v1/ so paths here are relative.
 * SSE streaming (LLM chat) is out of scope for Phase 1 — will be added with OkHttp EventSource.
 */
interface AuthApiService {

    @POST("auth/student/login")
    suspend fun studentLogin(@Body request: StudentLoginRequest): AuthResponse

    @POST("auth/teacher/login")
    suspend fun teacherLogin(@Body request: TeacherLoginRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequest): AuthResponse

    @POST("auth/logout")
    suspend fun logout(@Body request: LogoutRequest)
}
