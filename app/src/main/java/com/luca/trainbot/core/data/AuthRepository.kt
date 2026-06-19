package com.luca.trainbot.core.data

import com.luca.trainbot.core.network.ApiResult
import com.luca.trainbot.core.network.AuthApiService
import com.luca.trainbot.core.network.LogoutRequest
import com.luca.trainbot.core.network.RefreshTokenRequest
import com.luca.trainbot.core.network.StudentLoginRequest
import com.luca.trainbot.core.network.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Single source of truth for authentication.
 * Mirrors iOS AuthSession — coordinates network calls with token storage.
 */
class AuthRepository(
    private val apiService: AuthApiService,
    private val tokenStore: TokenStore,
) {
    val authState: Flow<AuthState> = tokenStore.authStateFlow

    /**
     * Student login: POST /api/v1/auth/student/login
     * Body: { classCode, username, password }
     * Success response: { accessToken, refreshToken, user: { id, role, name, tenantId } }
     *
     * classCode is uppercased before sending — mirrors iOS StudentLoginViewModel behaviour.
     */
    suspend fun loginStudent(
        classCode: String,
        username: String,
        password: String,
    ): ApiResult<Unit> {
        val result = safeApiCall {
            apiService.studentLogin(
                StudentLoginRequest(
                    classCode = classCode.trim().uppercase(),
                    username = username.trim(),
                    password = password,
                )
            )
        }
        return when (result) {
            is ApiResult.Success -> {
                val r = result.data
                tokenStore.saveTokens(
                    accessToken = r.accessToken,
                    refreshToken = r.refreshToken,
                    userId = r.user.id,
                    userRole = r.user.role,
                    userName = r.user.name,
                    tenantId = r.user.tenantId,
                )
                ApiResult.Success(Unit)
            }
            is ApiResult.Error -> result
        }
    }

    suspend fun refresh(): ApiResult<Unit> {
        val state = tokenStore.authStateFlow.first()
        val token = if (state is AuthState.Authenticated) state.refreshToken
        else return ApiResult.Error("Nicio sesiune activă")

        val result = safeApiCall {
            apiService.refresh(RefreshTokenRequest(token))
        }
        return when (result) {
            is ApiResult.Success -> {
                val r = result.data
                tokenStore.saveTokens(
                    accessToken = r.accessToken,
                    refreshToken = r.refreshToken,
                    userId = r.user.id,
                    userRole = r.user.role,
                    userName = r.user.name,
                    tenantId = r.user.tenantId,
                )
                ApiResult.Success(Unit)
            }
            is ApiResult.Error -> result
        }
    }

    suspend fun logout() {
        val state = tokenStore.authStateFlow.first()
        if (state is AuthState.Authenticated) {
            // Best-effort — ignore error, always clear local tokens
            safeApiCall { apiService.logout(LogoutRequest(state.refreshToken)) }
        }
        tokenStore.clear()
    }
}
