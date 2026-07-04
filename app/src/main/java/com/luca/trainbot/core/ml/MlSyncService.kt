package com.luca.trainbot.core.ml

import android.util.Log
import com.luca.trainbot.core.network.MlApiService
import com.luca.trainbot.core.network.MlLabelSyncDto
import com.luca.trainbot.core.network.MlProjectSyncRequest
import java.time.Instant
import java.time.format.DateTimeFormatter

private const val TAG = "MlSyncService"

/**
 * Pushes ML project metadata to the backend so teachers see student projects
 * in the web dashboard. Mirrors iOS MLSyncService.
 *
 * Images stay on-device (privacy). Only metadata is synced:
 * project name, trained status, label names + image counts.
 *
 * Idempotency: the backend upserts via (studentId, clientId) unique index.
 * The clientId is the project's own [MlProject.id] (UUID assigned at creation) —
 * same semantics as iOS, where MLProjectEntity.id (CoreData UUID) is used as clientId.
 */
class MlSyncService(private val mlApiService: MlApiService) {

    /**
     * Sync a single project. Fire-and-forget — catches all exceptions so callers
     * (e.g., TrainingViewModel) can call this without try/catch and without
     * affecting UI state if offline or unauthorized.
     */
    suspend fun syncProject(project: MlProject) {
        runCatching {
            val request = buildRequest(project)
            mlApiService.syncProject(request)
            Log.d(TAG, "Synced project clientId=${project.id}")
        }.onFailure { e ->
            Log.w(TAG, "ML sync failed for project clientId=${project.id}: ${e.message}")
        }
    }

    private fun buildRequest(project: MlProject): MlProjectSyncRequest {
        val labels = project.labels.map { label ->
            MlLabelSyncDto(
                clientId = label.id,
                name = label.name,
                imageCount = label.imageFileNames.size,
            )
        }
        val trainedAt: String? = if (project.isTrained) {
            DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(project.updatedAt))
        } else null

        return MlProjectSyncRequest(
            clientId = project.id,
            name = project.name,
            modelTrained = project.isTrained,
            modelVersion = if (project.isTrained) 1 else 0,
            trainedAt = trainedAt,
            labels = labels,
        )
    }
}
