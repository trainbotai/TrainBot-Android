package com.luca.trainbot.core.network

/** Thin wrapper so ViewModels never leak Retrofit exceptions into the UI directly. */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}

/**
 * Wraps a suspend network call and maps exceptions to [ApiResult.Error].
 * Parses the backend's `{ detail, title }` error body when available.
 */
suspend fun <T> safeApiCall(call: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(call())
    } catch (e: retrofit2.HttpException) {
        val errorBody = runCatching {
            val json = e.response()?.errorBody()?.string() ?: ""
            kotlinx.serialization.json.Json.decodeFromString<ApiErrorBody>(json)
        }.getOrNull()
        val message = errorBody?.detail
            ?: errorBody?.title
            ?: e.message()
            ?: "Eroare de rețea"
        ApiResult.Error(message, e.code())
    } catch (e: java.io.IOException) {
        ApiResult.Error("Fără conexiune la internet")
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Eroare necunoscută")
    }
}
