package com.staticquo.routing

import android.content.Context
import com.staticquo.data.db.RoutingDao
import com.staticquo.data.db.RoutingRegionEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: RoutingDao,
    private val httpClient: OkHttpClient,
    private val engine: GraphHopperEngine
) {
    companion object {
        private const val GITHUB_OWNER = "StaticQuo6"
        private const val GITHUB_REPO = "StaticQuo2"
        private const val GRAPH_TAG_PREFIX = "graph-"
    }

    data class AvailableRoutingRegion(
        val id: String,
        val name: String,
        val downloadUrl: String,
        val sizeBytes: Long,
        val version: String
    )

    suspend fun getDownloadedRegions(): List<RoutingRegionEntity> {
        return dao.getAll()
    }

    suspend fun fetchAvailableRegions(): List<AvailableRoutingRegion> {
        val downloadedIds = dao.getAll().map { it.id }.toSet()
        return try {
            val url = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases?per_page=5"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            parseGraphReleases(body, GRAPH_TAG_PREFIX).filter { it.id !in downloadedIds }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseGraphReleases(json: String, tagPrefix: String): List<AvailableRoutingRegion> {
        val releases = JSONArray(json)
        val regions = mutableListOf<AvailableRoutingRegion>()
        for (i in 0 until releases.length()) {
            val release = releases.getJSONObject(i)
            val tag = release.optString("tag_name", "")
            if (!tag.startsWith(tagPrefix)) continue

            val version = tag.removePrefix(tagPrefix)
            val releaseName = release.optString("name", version)
            val assets = release.optJSONArray("assets") ?: continue

            for (j in 0 until assets.length()) {
                val asset = assets.getJSONObject(j)
                val assetName = asset.optString("name", "")
                if (!assetName.endsWith(".zip")) continue

                val regionId = assetName.removeSuffix(".zip")
                regions.add(
                    AvailableRoutingRegion(
                        id = regionId,
                        name = releaseName,
                        downloadUrl = asset.optString("browser_download_url", ""),
                        sizeBytes = asset.optLong("size", 0),
                        version = version
                    )
                )
            }
        }
        return regions
    }

    suspend fun downloadAndExtract(
        region: AvailableRoutingRegion,
        onProgress: (Float) -> Unit = {}
    ): File {
        val routingDir = File(context.filesDir, "routing")
        routingDir.mkdirs()
        val targetDir = File(routingDir, region.id)
        if (targetDir.exists()) targetDir.deleteRecursively()
        targetDir.mkdirs()

        val request = Request.Builder()
            .url(region.downloadUrl)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw java.io.IOException("Download failed: HTTP ${response.code}")
        }

        val body = response.body ?: throw java.io.IOException("Empty response body")
        val contentLength = body.contentLength()

        body.byteStream().use { inputStream ->
            val buffer = ByteArray(8192)
            var totalRead = 0L
            val tmpFile = File(routingDir, "${region.id}.zip")
            FileOutputStream(tmpFile).use { output ->
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    totalRead += read
                    if (contentLength > 0) {
                        onProgress(totalRead.toFloat() / contentLength)
                    }
                }
            }

            val zis = ZipInputStream(tmpFile.inputStream())
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val outFile = File(targetDir, entry.name)
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { out ->
                        zis.copyTo(out)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
            zis.close()
            tmpFile.delete()
        }

        return targetDir
    }

    suspend fun saveRegion(
        id: String,
        name: String,
        graphPath: String,
        sizeBytes: Long,
        version: String
    ) {
        dao.upsert(
            RoutingRegionEntity(
                id = id,
                name = name,
                graphPath = graphPath,
                sizeBytes = sizeBytes,
                version = version
            )
        )
    }

    suspend fun removeRegion(id: String) {
        val entity = dao.getById(id)
        if (entity != null) {
            val dir = File(entity.graphPath)
            if (dir.exists()) dir.deleteRecursively()
            dao.delete(id)
        }
        val currentLoaded = engine.getLoadedPath()
        if (currentLoaded?.contains(id) == true) {
            engine.close()
        }
    }

    suspend fun loadRegion(id: String): Result<Unit> {
        val entity = dao.getById(id) ?: return Result.failure(Exception("Region not found"))
        return engine.load(entity.graphPath)
    }
}
