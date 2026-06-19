package com.luca.trainbot.core.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Persists ML projects (JSON) and images (JPEG files) in app-private storage.
 * Mirrors iOS MLProjectRepository — same concepts: projects → labels → images → embeddings.
 *
 * Layout:
 *   filesDir/ml/projects.json          — list of MlProject serialized
 *   filesDir/ml/images/<projectId>/<labelId>/<filename>.jpg
 */
class MlProjectRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val mlDir: File get() = File(context.filesDir, "ml").also { it.mkdirs() }
    private val projectsFile: File get() = File(mlDir, "projects.json")

    // ── Projects ────────────────────────────────────────────────────────────

    fun loadAllProjects(): List<MlProject> {
        if (!projectsFile.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<List<MlProject>>(projectsFile.readText())
        }.getOrDefault(emptyList())
    }

    fun saveProject(project: MlProject) {
        val projects = loadAllProjects().toMutableList()
        val idx = projects.indexOfFirst { it.id == project.id }
        if (idx >= 0) projects[idx] = project else projects.add(0, project)
        projectsFile.writeText(json.encodeToString(projects))
    }

    fun createProject(name: String): MlProject {
        val project = MlProject(id = UUID.randomUUID().toString(), name = name)
        saveProject(project)
        return project
    }

    fun deleteProject(projectId: String) {
        val projects = loadAllProjects().toMutableList()
        projects.removeAll { it.id == projectId }
        projectsFile.writeText(json.encodeToString(projects))
        // Delete associated images
        File(mlDir, "images/$projectId").deleteRecursively()
    }

    // ── Labels ───────────────────────────────────────────────────────────────

    fun addLabel(projectId: String, name: String): MlProject {
        val project = loadProject(projectId) ?: error("Project not found: $projectId")
        val label = MlLabel(id = UUID.randomUUID().toString(), name = name)
        val updated = project.copy(
            labels = project.labels + label,
            updatedAt = System.currentTimeMillis(),
            isTrained = false,
        )
        saveProject(updated)
        return updated
    }

    fun deleteLabel(projectId: String, labelId: String): MlProject {
        val project = loadProject(projectId) ?: error("Project not found: $projectId")
        project.labels.find { it.id == labelId }?.imageFileNames?.forEach { fn ->
            imageFile(projectId, labelId, fn).delete()
        }
        val updated = project.copy(
            labels = project.labels.filter { it.id != labelId },
            updatedAt = System.currentTimeMillis(),
            isTrained = false,
        )
        saveProject(updated)
        return updated
    }

    // ── Images ───────────────────────────────────────────────────────────────

    fun addImage(projectId: String, labelId: String, bitmap: Bitmap): MlProject {
        val project = loadProject(projectId) ?: error("Project not found: $projectId")
        val filename = "${UUID.randomUUID()}.jpg"
        val dir = imageDir(projectId, labelId).also { it.mkdirs() }
        File(dir, filename).outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        val updated = project.copy(
            labels = project.labels.map { label ->
                if (label.id == labelId) label.copy(imageFileNames = label.imageFileNames + filename)
                else label
            },
            updatedAt = System.currentTimeMillis(),
            isTrained = false,
        )
        saveProject(updated)
        return updated
    }

    fun addImageFromUri(projectId: String, labelId: String, uri: Uri): MlProject {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: error("Cannot decode image from URI")
        return addImage(projectId, labelId, bitmap)
    }

    fun deleteImage(projectId: String, labelId: String, filename: String): MlProject {
        imageFile(projectId, labelId, filename).delete()
        val project = loadProject(projectId) ?: error("Project not found: $projectId")
        val updated = project.copy(
            labels = project.labels.map { label ->
                if (label.id == labelId) label.copy(imageFileNames = label.imageFileNames - filename)
                else label
            },
            updatedAt = System.currentTimeMillis(),
            isTrained = false,
        )
        saveProject(updated)
        return updated
    }

    fun loadBitmap(projectId: String, labelId: String, filename: String): Bitmap? =
        runCatching { BitmapFactory.decodeFile(imageFile(projectId, labelId, filename).absolutePath) }.getOrNull()

    // ── Model (embeddings) persistence ───────────────────────────────────────

    /** Save the trained project (with centroids) back to the store. */
    fun saveTrainedProject(project: MlProject) {
        saveProject(project.copy(isTrained = true, updatedAt = System.currentTimeMillis()))
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun loadProject(id: String): MlProject? = loadAllProjects().find { it.id == id }

    fun imageDir(projectId: String, labelId: String): File =
        File(mlDir, "images/$projectId/$labelId")

    fun imageFile(projectId: String, labelId: String, filename: String): File =
        File(imageDir(projectId, labelId), filename)
}
