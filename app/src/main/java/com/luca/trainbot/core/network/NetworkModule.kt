package com.luca.trainbot.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.luca.trainbot.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

/**
 * Builds the shared OkHttpClient and Retrofit instances.
 *
 * Two clients are created:
 * - [buildOkHttpClient] — unauthenticated, used for auth endpoints.
 * - [buildAuthenticatedClient] — includes [AuthInterceptor] for all student/LLM calls.
 *
 * SSE streaming is handled via raw OkHttp (using the authenticated client) in
 * [LlmStreamingRepository] — Retrofit cannot stream SSE responses.
 */
object NetworkModule {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun buildOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    fun buildAuthenticatedClient(
        tokenProvider: () -> String?,
        authenticator: Authenticator? = null,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider))
            .apply { if (authenticator != null) authenticator(authenticator) }
            .addInterceptor(logging)
            .build()
    }

    fun buildRetrofit(client: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    fun buildAuthApiService(retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)

    fun buildLlmApiService(retrofit: Retrofit): LlmApiService =
        retrofit.create(LlmApiService::class.java)

    fun buildMlApiService(retrofit: Retrofit): MlApiService =
        retrofit.create(MlApiService::class.java)
}
