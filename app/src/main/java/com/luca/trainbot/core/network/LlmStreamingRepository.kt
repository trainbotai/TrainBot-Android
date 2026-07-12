package com.luca.trainbot.core.network

import com.luca.trainbot.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * SSE event from the LLM streaming endpoint.
 * Mirrors iOS SSEEvent.
 */
sealed class SseEvent {
    data class Chunk(val text: String) : SseEvent()
    data class Done(val inputTokens: Int, val outputTokens: Int) : SseEvent()
}

/**
 * Handles the faked SSE stream from POST /api/v1/student/llm/sessions/{id}/query.
 *
 * Backend emits newline-delimited SSE lines:
 *   data: {"chunk":"<word>"}
 *   data: {"done":true,"inputTokens":N,"outputTokens":M}
 *
 * Uses raw OkHttp (Retrofit buffers full responses and cannot stream SSE).
 * Mirrors iOS LLMStreamingService + SSEParser.
 */
class LlmStreamingRepository(
    private val client: OkHttpClient,
    private val tokenProvider: () -> String?,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Returns a cold [Flow] of [SseEvent]s for a teacher bot query.
     * POSTs to /student/llm/teacher-bots/{id}/query — same SSE shape as student session query.
     */
    fun streamTeacherBotQuery(botId: String, prompt: String): Flow<SseEvent> =
        streamInternal("student/llm/teacher-bots/$botId/query", prompt)

    /**
     * Returns a cold [Flow] of [SseEvent]s.
     * Throws [UnauthorizedException] or [HttpException] on error before streaming starts.
     * IO errors during streaming are propagated as Flow exceptions.
     */
    fun streamQuery(sessionId: String, prompt: String): Flow<SseEvent> =
        streamInternal("student/llm/sessions/$sessionId/query", prompt)

    private fun streamInternal(path: String, prompt: String): Flow<SseEvent> = flow {
        val token = tokenProvider()
            ?: throw UnauthorizedException()

        // BASE_URL ends with "/" (e.g. "http://10.0.2.2:3000/api/v1/")
        val base = BuildConfig.BASE_URL.trimEnd('/')
        val url = "$base/$path"

        // Encode prompt as JSON body
        val bodyBytes = buildJsonBody(prompt)
        val requestBody = bodyBytes.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Authorization", "Bearer $token")
            .header("Accept", "text/event-stream")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorText = response.body?.string() ?: ""
            response.close()
            val detail = parseErrorDetail(errorText) ?: "Eroare ${response.code}"
            if (response.code == 401) throw UnauthorizedException()
            throw HttpException(response.code, detail)
        }

        val body = response.body ?: run {
            response.close()
            throw IllegalStateException("Răspuns gol de la server")
        }

        try {
            BufferedReader(InputStreamReader(body.byteStream())).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val event = parseSseLine(line!!) ?: continue
                    emit(event)
                    if (event is SseEvent.Done) break
                }
            }
        } finally {
            response.close()
        }
    }.flowOn(Dispatchers.IO)

    private fun buildJsonBody(prompt: String): String {
        // Safe manual encoding — avoid dependency on @Serializable for a one-field body
        val escaped = prompt
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return """{"prompt":"$escaped"}"""
    }

    private fun parseErrorDetail(body: String): String? = runCatching {
        val obj = json.parseToJsonElement(body) as? JsonObject ?: return null
        obj["detail"]?.jsonPrimitive?.content
    }.getOrNull()

    /**
     * Parses one SSE line of the form: `data: <json>`
     * Mirrors iOS SSEParser.parseLine().
     */
    private fun parseSseLine(line: String): SseEvent? {
        if (!line.startsWith("data: ")) return null
        val jsonStr = line.removePrefix("data: ").trim()
        if (jsonStr.isEmpty()) return null

        return runCatching {
            val obj = json.parseToJsonElement(jsonStr) as? JsonObject ?: return null
            when {
                obj.containsKey("done") && obj["done"]?.jsonPrimitive?.boolean == true -> {
                    val inputTokens = obj["inputTokens"]?.jsonPrimitive?.int ?: 0
                    val outputTokens = obj["outputTokens"]?.jsonPrimitive?.int ?: 0
                    SseEvent.Done(inputTokens, outputTokens)
                }
                obj.containsKey("chunk") -> {
                    val chunk = obj["chunk"]?.jsonPrimitive?.content ?: return null
                    SseEvent.Chunk(chunk)
                }
                else -> null
            }
        }.getOrNull()
    }
}

class UnauthorizedException : Exception("Sesiunea ta a expirat")
class HttpException(val code: Int, val detail: String) : Exception(detail)
