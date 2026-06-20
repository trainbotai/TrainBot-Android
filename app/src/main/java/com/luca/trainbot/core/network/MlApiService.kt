package com.luca.trainbot.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

// ---- Request DTOs — mirror backend ml.schemas.ts SyncProjectBody ----

@Serializable
data class MlLabelSyncDto(
    val clientId: String,
    val name: String,
    val imageCount: Int,
)

@Serializable
data class MlProjectSyncRequest(
    val clientId: String,
    val name: String,
    val modelTrained: Boolean,
    val modelVersion: Int,
    val trainedAt: String? = null,
    val labels: List<MlLabelSyncDto> = emptyList(),
)

// ---- Response DTOs ----

@Serializable
data class MlLabelSyncResponse(
    val id: String,
    val clientId: String,
)

@Serializable
data class MlProjectSyncResponse(
    val id: String,
    val labels: List<MlLabelSyncResponse> = emptyList(),
)

/**
 * Retrofit service for the student ML sync endpoint.
 * POST /student/ml/projects — upsert (idempotent via clientId).
 * Authorization: Bearer token attached by [AuthInterceptor].
 */
interface MlApiService {

    /** POST /student/ml/projects */
    @POST("student/ml/projects")
    suspend fun syncProject(@Body body: MlProjectSyncRequest): MlProjectSyncResponse
}
