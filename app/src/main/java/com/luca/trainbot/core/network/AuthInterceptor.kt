package com.luca.trainbot.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that attaches [Authorization: Bearer <token>] to every request.
 * The token is resolved lazily via [tokenProvider] so it always reflects the current
 * value from TokenStore without holding a hard reference to the DataStore flow.
 */
class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()
        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
