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

    // Un singur refresh la un moment dat: două 401-uri concurente ar trimite
    // două POST /auth/refresh cu același token — al doilea pică la rotire și
    // ar curăța TokenStore, delogând copilul forțat.
    private val refreshLock = Any()

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

        return synchronized(refreshLock) {
            runBlocking {
            val state = tokenStore.authStateFlow.first()
            val authState = state as? AuthState.Authenticated
                ?: return@runBlocking null // no stored refresh token → stay unauthenticated

            // Alt request a terminat refresh-ul cât am așteptat lock-ul? Atunci
            // token-ul curent diferă de cel respins cu 401 — îl refolosim direct.
            val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (failedToken != null && failedToken != authState.accessToken) {
                Log.d(TAG, "Token already refreshed by a concurrent request — reusing it")
                return@runBlocking response.request.newBuilder()
                    .header("Authorization", "Bearer ${authState.accessToken}")
                    .build()
            }
            val refreshToken = authState.refreshToken

            Log.d(TAG, "Attempting token refresh…")
            val result = safeApiCall {
                refreshApiService.refresh(RefreshTokenRequest(refreshToken))
            }

            when (result) {
                is ApiResult.Success -> {
                    val r = result.data
                    // Preserve user info fields from the current auth state.
                    tokenStore.saveTokens(
                        accessToken = r.accessToken,
                        refreshToken = r.refreshToken,
                        userId = authState.userId,
                        userRole = authState.userRole,
                        userName = authState.userName,
                        tenantId = authState.tenantId,
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
}
