package com.staticquo.maps

import android.content.Context
import com.staticquo.data.db.MapRegionDao
import com.staticquo.data.db.MapRegionEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: MapRegionDao,
    private val httpClient: OkHttpClient
) {

    companion object {
        private const val GITHUB_OWNER = "StaticQuo6"
        private const val GITHUB_REPO = "StaticQuo2"
        private const val TILE_TAG_PREFIX = "tiles-"
        private const val DEMO_ASSET = "maps/demo.mbtiles"
        private const val DEMO_REGION_ID = "nyc-demo"
    }

    data class AvailableRegion(
        val id: String,
        val name: String,
        val downloadUrl: String,
        val sizeBytes: Long,
        val version: String
    )

    suspend fun getDownloadedRegions(): List<MapRegionEntity> {
        return dao.getDownloaded()
    }

    suspend fun getActiveRegion(): MapRegionEntity? {
        var region = dao.getDownloaded().firstOrNull()
        if (region == null) {
            region = tryInstallDemoRegion()
        }
        return region
    }

    private suspend fun tryInstallDemoRegion(): MapRegionEntity? {
        val demoPath = File(context.filesDir, "maps/$DEMO_REGION_ID.mbtiles")
        if (demoPath.exists()) return null

        try {
            context.assets.open(DEMO_ASSET).use { input ->
                demoPath.parentFile?.mkdirs()
                FileOutputStream(demoPath).use { output ->
                    input.copyTo(output)
                }
            }
            val size = demoPath.length()
            val entity = MapRegionEntity(
                id = DEMO_REGION_ID,
                name = "NYC Demo Region",
                mbtilesPath = demoPath.absolutePath,
                sizeBytes = size,
                version = "1.0.0",
                isDownloaded = true
            )
            dao.upsert(entity)
            return entity
        } catch (_: Exception) {
            return null
        }
    }

    suspend fun fetchAvailableRegions(): List<AvailableRegion> {
        val downloadedIds = dao.getDownloaded().map { it.id }.toSet()
        return try {
            val url = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases?per_page=5"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            parseTileReleases(body, TILE_TAG_PREFIX).filter { it.id !in downloadedIds }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseTileReleases(json: String, tagPrefix: String): List<AvailableRegion> {
        val releases = JSONArray(json)
        val regions = mutableListOf<AvailableRegion>()
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
                if (!assetName.endsWith(".mbtiles")) continue

                val regionId = assetName.removeSuffix(".mbtiles")
                regions.add(
                    AvailableRegion(
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

    suspend fun downloadRegion(
        region: AvailableRegion,
        targetDir: File,
        onProgress: (Float) -> Unit = {}
    ): File {
        targetDir.mkdirs()
        val targetFile = File(targetDir, "${region.id}.mbtiles")

        val request = Request.Builder()
            .url(region.downloadUrl)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Download failed: HTTP ${response.code}")
        }

        val body = response.body ?: throw IOException("Empty response body")
        val contentLength = body.contentLength()

        body.byteStream().use { input ->
            FileOutputStream(targetFile).use { output ->
                val buffer = ByteArray(8192)
                var totalRead = 0L
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    totalRead += read
                    if (contentLength > 0) {
                        onProgress(totalRead.toFloat() / contentLength)
                    }
                }
            }
        }

        return targetFile
    }

    suspend fun saveRegion(
        id: String,
        name: String,
        filePath: String,
        sizeBytes: Long,
        version: String
    ) {
        dao.upsert(
            MapRegionEntity(
                id = id,
                name = name,
                mbtilesPath = filePath,
                sizeBytes = sizeBytes,
                version = version,
                isDownloaded = true
            )
        )
    }

    suspend fun removeRegion(id: String, file: File?) {
        file?.delete()
        dao.delete(id)
    }
}
