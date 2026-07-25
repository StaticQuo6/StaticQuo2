package com.staticquo.backup

import android.content.Context
import android.util.Base64
import com.staticquo.heatmap.HeatmapDao
import com.staticquo.heatmap.HeatmapEntity
import com.staticquo.data.db.VaultEntryDao
import com.staticquo.data.db.VaultEntryEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class BackupResult<T> {
    data class Success<T>(val data: T) : BackupResult<T>()
    data class Error<T>(val message: String) : BackupResult<T>()
}

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val webDav: WebDavClient,
    private val vaultDao: VaultEntryDao,
    private val heatmapDao: HeatmapDao
) {

    companion object {
        private const val BACKUP_DIR = "StaticQuoBackup"
        private const val VAULT_DATA_FILE = "$BACKUP_DIR/vault.json"
        private const val HEATMAP_FILE = "$BACKUP_DIR/heatmap.json"
        private const val METADATA_FILE = "$BACKUP_DIR/metadata.json"
    }

    fun configure(url: String, username: String, password: String) {
        webDav.configure(url, username, password)
    }

    fun getServerUrl(): String = webDav.getBaseUrl()

    fun isConfigured(): Boolean = webDav.isConfigured()

    fun testConnection(): BackupResult<Unit> {
        return when (val result = webDav.testConnection()) {
            is WebDavResult.Success -> BackupResult.Success(Unit)
            is WebDavResult.Error -> BackupResult.Error(result.message)
        }
    }

    suspend fun performBackup(onProgress: (String) -> Unit = {}): BackupResult<String> {
        if (!webDav.isConfigured()) return BackupResult.Error("WebDAV not configured")

        try {
            onProgress("Collecting vault data...")
            val vaultEntries = vaultDao.getAll()
            val vaultJson = JSONArray()
            for (entry in vaultEntries) {
                val encryptedFile = File(entry.encryptedFilePath)
                val encryptedData = if (encryptedFile.exists()) {
                    encryptedFile.readBytes()
                } else ByteArray(0)

                vaultJson.put(JSONObject().apply {
                    put("id", entry.id)
                    put("title", entry.title)
                    put("contentType", entry.contentType)
                    put("encryptedData", android.util.Base64.encodeToString(encryptedData, android.util.Base64.NO_WRAP))
                    put("sizeBytes", entry.sizeBytes)
                    put("createdAt", entry.createdAt)
                    put("updatedAt", entry.updatedAt)
                })
            }

            onProgress("Collecting heatmap data...")
            val beacons = heatmapDao.getAll()
            val heatmapJson = JSONArray()
            for (beacon in beacons) {
                heatmapJson.put(JSONObject().apply {
                    put("id", beacon.id)
                    put("latitude", beacon.latitude)
                    put("longitude", beacon.longitude)
                    put("beaconType", beacon.beaconType)
                    put("title", beacon.title)
                    put("description", beacon.description)
                    put("sourceDeviceId", beacon.sourceDeviceId)
                    put("createdAt", beacon.createdAt)
                })
            }

            val metadataJson = JSONObject().apply {
                put("app", "StaticQuo")
                put("version", "1.6.0")
                put("timestamp", System.currentTimeMillis())
                put("dataTypes", JSONArray(listOf("vault", "heatmap")))
            }

            onProgress("Uploading backup...")
            val results = mutableListOf<String>()

            when (webDav.upload(METADATA_FILE, metadataJson.toString().toByteArray())) {
                is WebDavResult.Success -> results.add("metadata")
                is WebDavResult.Error -> {}
            }
            when (webDav.upload(VAULT_DATA_FILE, vaultJson.toString().toByteArray())) {
                is WebDavResult.Success -> results.add("vault")
                is WebDavResult.Error -> {}
            }
            when (webDav.upload(HEATMAP_FILE, heatmapJson.toString().toByteArray())) {
                is WebDavResult.Success -> results.add("heatmap")
                is WebDavResult.Error -> {}
            }

            if (results.isEmpty()) {
                return BackupResult.Error("All uploads failed")
            }

            return BackupResult.Success("Backed up: ${results.joinToString(", ")}")
        } catch (e: Exception) {
            return BackupResult.Error("Backup failed: ${e.message}")
        }
    }

    suspend fun performRestore(onProgress: (String) -> Unit = {}): BackupResult<String> {
        if (!webDav.isConfigured()) return BackupResult.Error("WebDAV not configured")

        try {
            onProgress("Downloading vault data...")
            val vaultResult = webDav.download(VAULT_DATA_FILE)
            if (vaultResult is WebDavResult.Success) {
                val vaultJson = JSONArray(String(vaultResult.data))
                for (i in 0 until vaultJson.length()) {
                    val obj = vaultJson.getJSONObject(i)
                    val title = obj.getString("title")
                    val contentType = obj.optString("contentType", "text/plain")
                    val encryptedData = android.util.Base64.decode(
                        obj.getString("encryptedData"), android.util.Base64.NO_WRAP
                    )
                    val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    val updatedAt = obj.optLong("updatedAt", createdAt)

                    if (encryptedData.isNotEmpty()) {
                        val vaultDir = File(context.filesDir, "vault")
                        vaultDir.mkdirs()
                        val entry = VaultEntryEntity(
                            title = title,
                            contentType = contentType,
                            encryptedFilePath = "",
                            sizeBytes = encryptedData.size.toLong(),
                            createdAt = createdAt,
                            updatedAt = updatedAt
                        )
                        val id = vaultDao.insert(entry)
                        val file = File(vaultDir, "$id.enc")
                        file.writeBytes(encryptedData)
                        vaultDao.insert(entry.copy(id = id, encryptedFilePath = file.absolutePath))
                    }
                }
            }

            onProgress("Downloading heatmap data...")
            val heatmapResult = webDav.download(HEATMAP_FILE)
            if (heatmapResult is WebDavResult.Success) {
                val heatmapJson = JSONArray(String(heatmapResult.data))
                for (i in 0 until heatmapJson.length()) {
                    val obj = heatmapJson.getJSONObject(i)
                    heatmapDao.insert(HeatmapEntity(
                        latitude = obj.getDouble("latitude"),
                        longitude = obj.getDouble("longitude"),
                        beaconType = obj.getString("beaconType"),
                        title = obj.getString("title"),
                        description = obj.optString("description", ""),
                        sourceDeviceId = obj.optString("sourceDeviceId", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    ))
                }
            }

            return BackupResult.Success("Restore complete")
        } catch (e: Exception) {
            return BackupResult.Error("Restore failed: ${e.message}")
        }
    }

    fun listBackups(): BackupResult<List<String>> {
        return when (val result = webDav.list(BACKUP_DIR)) {
            is WebDavResult.Success -> BackupResult.Success(result.data)
            is WebDavResult.Error -> BackupResult.Error(result.message)
        }
    }

    fun deleteBackup(): BackupResult<Unit> {
        return when (val result = webDav.delete(BACKUP_DIR)) {
            is WebDavResult.Success -> BackupResult.Success(Unit)
            is WebDavResult.Error -> BackupResult.Error(result.message)
        }
    }
}
