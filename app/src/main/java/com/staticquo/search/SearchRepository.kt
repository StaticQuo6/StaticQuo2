package com.staticquo.search

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class SearchResult<T> {
    data class Success<T>(val data: T) : SearchResult<T>()
    data class Error<T>(val message: String) : SearchResult<T>()
}

@Singleton
class SearchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: SearchDao
) {

    suspend fun ensureIndexed() {
        if (dao.documentCount() > 0) return

        try {
            val files = context.assets.list("references") ?: return
            for (file in files) {
                val content = context.assets.open("references/$file")
                    .bufferedReader()
                    .readText()
                val title = file
                    .removeSuffix(".txt")
                    .replace("_", " ")
                    .replaceFirstChar { it.uppercase() }

                dao.insertDocument(
                    SearchDocument(
                        title = title,
                        content = content,
                        sourceFile = file
                    )
                )
            }
        } catch (_: Exception) {}
    }

    suspend fun search(query: String): SearchResult<List<SearchResultItem>> {
        return try {
            sanitizeQuery(query)?.let { safeQuery ->
                val rawResults = dao.search(safeQuery)
                SearchResult.Success(
                    rawResults.map {
                        SearchResultItem(
                            title = it.title,
                            snippet = generateSnippet(it.content, query),
                            sourceFile = it.sourceFile
                        )
                    }
                )
            } ?: SearchResult.Success(emptyList())
        } catch (e: Exception) {
            SearchResult.Error("Search failed: ${e.message}")
        }
    }

    private fun sanitizeQuery(query: String): String? {
        val cleaned = query.trim().filter { it.isLetterOrDigit() || it == ' ' }
        if (cleaned.isBlank()) return null
        return cleaned.split("\\s+".toRegex())
            .joinToString(" ") { "$it*" }
    }

    private fun generateSnippet(content: String, query: String): String {
        val lower = content.lowercase()
        val queryLower = query.lowercase()
        val index = lower.indexOf(queryLower)
        if (index < 0) return content.take(150) + "..."
        val start = maxOf(0, index - 50)
        val end = minOf(content.length, index + query.length + 100)
        val prefix = if (start > 0) "..." else ""
        val suffix = if (end < content.length) "..." else ""
        return prefix + content.substring(start, end).trim() + suffix
    }
}

data class SearchResultItem(
    val title: String,
    val snippet: String,
    val sourceFile: String
)
