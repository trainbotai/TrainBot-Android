package com.luca.trainbot.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---- Request bodies ----

@Serializable
data class StudentLoginRequest(
    val classCode: String,
    val username: String,
    val password: String,
)

@Serializable
data class TeacherLoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String,
)

@Serializable
data class LogoutRequest(
    val refreshToken: String,
)

// ---- Response models ----

/**
 * Mirrors backend AuthResult and iOS AuthUser / AuthResponse.
 * Response shape: { accessToken, refreshToken, user: { id, role, name, tenantId } }
 */
@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthUser,
)

@Serializable
data class AuthUser(
    val id: String,
    val role: String,   // "student" | "teacher"
    val name: String,
    val tenantId: String,
)

/** Error shape returned by the backend: { detail, title } */
@Serializable
data class ApiErrorBody(
    val detail: String? = null,
    val title: String? = null,
)
