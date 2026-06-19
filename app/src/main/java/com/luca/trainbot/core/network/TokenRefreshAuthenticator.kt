package com.luca.trainbot.core.network

import android.util.Log
import com.luca.trainbot.core.data.AuthState
import com.luca.trainbot.core.data.TokenStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

private const val TAG = "TokenRefreshAuth"

/**
 * OkHttp [Authenticator] that handles 401 responses on the authenticated client.
 *
 * Flow on a 401:
 * 1. Read the current refreshToken from [TokenStore].
 * 2. POST /auth/refresh synchronously (via a dedicated unauthenticated OkHttp client
 *    to avoid recursive interception on the same client).
 * 3. On success — persist the new tokens and retry the original request with the new accessToken.
 * 4. On failure (refresh 401 / network error) — clear TokenStore so [AuthState] becomes
 *    [AuthState.Unauthenticated] and the app returns to the login screen. Return null
 *    to abort the retried request.
 *
 * Loop guard: OkHttp counts retries internally; we additionally bail out immediately
 * if the request already carries the same (new) token we just obtained, which means
 * a second retry is being attempted and would loop infinitely.
 */
class TokenRefreshAuthenticator(
    private val tokenStore: TokenStore,
    private val refreshApiService: AuthApiService,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Guard: if the response is already to a /auth/refresh call, don't recurse.
        if (response.request.url.encodedPath.contains("/auth/refresh")) {
            Log.w(TAG, "Refresh endpoint itself returned 401 — clearing tokens")
            runBlocking { tokenStore.clear() }
            return null
        }

        // Guard: if we have already retried once for this request, give up.
        if (response.priorResponse?.code == 401) {
            Log.w(TAG, "Already retried once — giving up and clearing tokens")
            runBlocking { tokenStore.clear() }
            return null
        }

        return runBlocking {
            val state = tokenStore.authStateFlow.first()
            val refreshToken = (state as? AuthState.Authenticated)?.refreshToken
                ?: return@runBlocking null // no stored refresh token → stay unauthenticated

            Log.d(TAG, "Attempting token refresh…")
            val result = safeApiCall {
                refreshApiService.refresh(RefreshTokenRequest(refreshToken))
            }

            when (result) {
                is ApiResult.Success -> {
                    val r = result.data
                    // Preserve user info fields from the current auth state.
                    val currentState = state
                    tokenStore.saveTokens(
                        accessToken = r.accessToken,
                        refreshToken = r.refreshToken,
                        userId = currentState.userId,
                        userRole = currentState.userRole,
                        userName = currentState.userName,
                        tenantId = currentState.tenantId,
                    )
                    Log.d(TAG, "Token refreshed successfully — retrying request")
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${r.accessToken}")
                        .build()
                }
                is ApiResult.Error -> {
                    Log.w(TAG, "Token refresh failed: ${result.message} — clearing auth")
                    tokenStore.clear()
                    null
                }
            }
        }
    }
}
