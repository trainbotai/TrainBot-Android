package com.luca.trainbot.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.luca.trainbot.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

/**
 * Builds the shared OkHttpClient and Retrofit instance.
 * The [authInterceptor] parameter is set from AppContainer once tokens are available,
 * allowing future phases to attach the Bearer token automatically.
 *
 * SSE support will be added in a later phase via OkHttp EventSource — the client
 * is structured to support it (no special restrictions on streaming).
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
}
