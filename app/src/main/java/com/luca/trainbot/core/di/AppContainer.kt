package com.luca.trainbot.core.di

import android.content.Context
import com.luca.trainbot.core.data.AuthRepository
import com.luca.trainbot.core.data.AuthState
import com.luca.trainbot.core.data.TokenStore
import com.luca.trainbot.core.ml.MlProjectRepository
import com.luca.trainbot.core.network.LlmRepository
import com.luca.trainbot.core.network.LlmStreamingRepository
import com.luca.trainbot.core.network.NetworkModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Manual service-locator / DI container.
 * Created once in [com.luca.trainbot.TrainBotApplication] and held as a companion object accessor.
 *
 * Auth flow:
 * - Unauthenticated OkHttp client + Retrofit → auth endpoints only.
 * - Authenticated OkHttp client → LLM endpoints (Bearer token from TokenStore).
 *   The token is read synchronously via [runBlocking] in the interceptor —
 *   acceptable because the interceptor already runs on an IO thread.
 */
class AppContainer(context: Context) {

    private val tokenStore = TokenStore(context)

    // --- Unauthenticated network stack (auth endpoints) ---
    private val okHttpClient = NetworkModule.buildOkHttpClient()
    private val retrofit = NetworkModule.buildRetrofit(okHttpClient)
    private val authApiService = NetworkModule.buildAuthApiService(retrofit)

    val authRepository = AuthRepository(authApiService, tokenStore)

    // --- Authenticated network stack (LLM + future student endpoints) ---
    private val authenticatedClient = NetworkModule.buildAuthenticatedClient {
        // Read the current access token synchronously from DataStore.
        // The interceptor runs on IO thread so runBlocking is safe here.
        runBlocking {
            val state = tokenStore.authStateFlow.first()
            if (state is AuthState.Authenticated) state.accessToken else null
        }
    }
    private val authenticatedRetrofit = NetworkModule.buildRetrofit(authenticatedClient)
    private val llmApiService = NetworkModule.buildLlmApiService(authenticatedRetrofit)

    val llmRepository = LlmRepository(llmApiService)

    val llmStreamingRepository = LlmStreamingRepository(
        client = authenticatedClient,
        tokenProvider = {
            runBlocking {
                val state = tokenStore.authStateFlow.first()
                if (state is AuthState.Authenticated) state.accessToken else null
            }
        },
    )

    /** Shared ML project repository — single instance so file writes don't race. */
    val mlProjectRepository = MlProjectRepository(context)
}
