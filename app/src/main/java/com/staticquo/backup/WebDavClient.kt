package com.staticquo.backup

import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed class WebDavResult<T> {
    data class Success<T>(val data: T) : WebDavResult<T>()
    data class Error<T>(val message: String) : WebDavResult<T>()
}

@Singleton
class WebDavClient @Inject constructor(
    private val httpClient: OkHttpClient
) {
    private var baseUrl: String = ""
    private var credentials: String = ""

    fun configure(url: String, username: String, password: String) {
        baseUrl = url.trimEnd('/')
        credentials = Credentials.basic(username, password)
    }

    fun isConfigured(): Boolean = baseUrl.isNotBlank()

    fun getBaseUrl(): String = baseUrl

    fun testConnection(): WebDavResult<Unit> {
        if (!isConfigured()) return WebDavResult.Error("WebDAV not configured")
        return propfind("")
    }

    fun upload(path: String, data: ByteArray): WebDavResult<Unit> {
        return try {
            val url = "$baseUrl/$path"
            val body = data.toRequestBody("application/octet-stream".toMediaType())
            val request = Request.Builder()
                .url(url)
                .put(body)
                .header("Authorization", credentials)
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                WebDavResult.Success(Unit)
            } else {
                WebDavResult.Error("Upload failed: HTTP ${response.code}")
            }
        } catch (e: IOException) {
            WebDavResult.Error("Connection failed: ${e.message}")
        }
    }

    fun download(path: String): WebDavResult<ByteArray> {
        return try {
            val url = "$baseUrl/$path"
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Authorization", credentials)
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.bytes()
                    ?: return WebDavResult.Error("Empty response")
                WebDavResult.Success(body)
            } else {
                WebDavResult.Error("Download failed: HTTP ${response.code}")
            }
        } catch (e: IOException) {
            WebDavResult.Error("Connection failed: ${e.message}")
        }
    }

    fun list(path: String): WebDavResult<List<String>> {
        return try {
            val url = "$baseUrl/$path"
            val request = Request.Builder()
                .url(url)
                .method("PROPFIND", null)
                .header("Authorization", credentials)
                .header("Depth", "1")
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val files = Regex("<d:displayname>(.*?)</d:displayname>")
                    .findAll(body)
                    .map { it.groupValues[1] }
                    .filter { it.isNotBlank() && it != path.trim('/').split("/").last() }
                    .toList()
                WebDavResult.Success(files)
            } else {
                WebDavResult.Error("List failed: HTTP ${response.code}")
            }
        } catch (e: IOException) {
            WebDavResult.Error("Connection failed: ${e.message}")
        }
    }

    fun delete(path: String): WebDavResult<Unit> {
        return try {
            val url = "$baseUrl/$path"
            val request = Request.Builder()
                .url(url)
                .delete()
                .header("Authorization", credentials)
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                WebDavResult.Success(Unit)
            } else {
                WebDavResult.Error("Delete failed: HTTP ${response.code}")
            }
        } catch (e: IOException) {
            WebDavResult.Error("Connection failed: ${e.message}")
        }
    }

    private fun propfind(path: String): WebDavResult<Unit> {
        return try {
            val url = "$baseUrl/$path"
            val request = Request.Builder()
                .url(url)
                .method("PROPFIND", null)
                .header("Authorization", credentials)
                .header("Depth", "0")
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                WebDavResult.Success(Unit)
            } else {
                WebDavResult.Error("Connection test failed: HTTP ${response.code}")
            }
        } catch (e: IOException) {
            WebDavResult.Error("Connection failed: ${e.message}")
        }
    }
}
