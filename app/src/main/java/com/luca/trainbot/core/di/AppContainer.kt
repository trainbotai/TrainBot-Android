package com.luca.trainbot.core.di

import android.content.Context
import com.luca.trainbot.core.data.AuthRepository
import com.luca.trainbot.core.data.TokenStore
import com.luca.trainbot.core.ml.MlProjectRepository
import com.luca.trainbot.core.network.NetworkModule

/**
 * Manual service-locator / DI container.
 * Created once in [com.luca.trainbot.TrainBotApplication] and held as a companion object accessor.
 * Hilt will replace this in a future phase if the project grows.
 */
class AppContainer(context: Context) {

    private val tokenStore = TokenStore(context)

    private val okHttpClient = NetworkModule.buildOkHttpClient()
    private val retrofit = NetworkModule.buildRetrofit(okHttpClient)
    private val authApiService = NetworkModule.buildAuthApiService(retrofit)

    val authRepository = AuthRepository(authApiService, tokenStore)

    /** Shared ML project repository — single instance so file writes don't race. */
    val mlProjectRepository = MlProjectRepository(context)
}
