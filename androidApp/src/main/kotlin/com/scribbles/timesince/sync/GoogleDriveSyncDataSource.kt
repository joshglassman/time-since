package com.scribbles.timesince.sync

import com.scribbles.timesince.data.sync.SyncDataSource
import com.scribbles.timesince.data.sync.SyncPayload
import com.scribbles.timesince.data.sync.SyncResult
import com.scribbles.timesince.domain.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.time.Clock

/**
 * Stores tasks as a single JSON file in the user's Drive appDataFolder.
 * Uses the Drive REST API v3 directly with an access token provided by [tokenProvider].
 */
class GoogleDriveSyncDataSource(
    private val tokenProvider: suspend () -> String?,
    private val clock: Clock = Clock.System,
) : SyncDataSource {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    override suspend fun upload(tasks: List<Task>): SyncResult = withContext(Dispatchers.IO) {
        val token = tokenProvider() ?: return@withContext SyncResult.Error("Not signed in.")
        try {
            val payload = SyncPayload.from(tasks, clock.now())
            val body = json.encodeToString(SyncPayload.serializer(), payload)

            val fileId = findSyncFileId(token)
            if (fileId != null) {
                updateFile(token, fileId, body)
            } else {
                createFile(token, body)
            }
            SyncResult.Success(tasks.size)
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Upload failed.")
        }
    }

    override suspend fun download(): Pair<SyncResult, List<Task>> = withContext(Dispatchers.IO) {
        val token = tokenProvider() ?: return@withContext Pair(SyncResult.Error("Not signed in."), emptyList())
        try {
            val fileId = findSyncFileId(token)
                ?: return@withContext Pair(SyncResult.Success(0), emptyList())

            val content = downloadFile(token, fileId)
            val payload = json.decodeFromString(SyncPayload.serializer(), content)
            val tasks = payload.toTasks()
            Pair(SyncResult.Success(tasks.size), tasks)
        } catch (e: Exception) {
            Pair(SyncResult.Error(e.message ?: "Download failed."), emptyList())
        }
    }

    private fun findSyncFileId(token: String): String? {
        val query = "name='$SYNC_FILE_NAME' and 'appDataFolder' in parents and trashed=false"
        val url = URL("$DRIVE_FILES_URL?spaces=appDataFolder&q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id)")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            if (conn.responseCode != 200) return null
            val response = conn.inputStream.bufferedReader().readText()
            // Parse minimal JSON: {"files":[{"id":"..."}]}
            val idRegex = Regex(""""id"\s*:\s*"([^"]+)"""")
            return idRegex.find(response)?.groupValues?.get(1)
        } finally {
            conn.disconnect()
        }
    }

    private fun createFile(token: String, content: String) {
        val boundary = "===TimeSinceBoundary==="
        val metadata = """{"name":"$SYNC_FILE_NAME","parents":["appDataFolder"]}"""

        val body = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata)
            append("\r\n--$boundary\r\n")
            append("Content-Type: application/json\r\n\r\n")
            append(content)
            append("\r\n--$boundary--")
        }

        val url = URL("$DRIVE_UPLOAD_URL?uploadType=multipart")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        }
        try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
            check(conn.responseCode in 200..299) { "Create failed: ${conn.responseCode}" }
        } finally {
            conn.disconnect()
        }
    }

    private fun updateFile(token: String, fileId: String, content: String) {
        val url = URL("$DRIVE_UPLOAD_URL/$fileId?uploadType=media")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "PATCH"
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(content) }
            check(conn.responseCode in 200..299) { "Update failed: ${conn.responseCode}" }
        } finally {
            conn.disconnect()
        }
    }

    private fun downloadFile(token: String, fileId: String): String {
        val url = URL("$DRIVE_FILES_URL/$fileId?alt=media")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            check(conn.responseCode == 200) { "Download failed: ${conn.responseCode}" }
            return conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        private const val SYNC_FILE_NAME = "time-since-tasks.json"
        private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
    }
}
