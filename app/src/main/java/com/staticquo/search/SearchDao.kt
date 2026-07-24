package com.staticquo.search

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

data class SearchResultRow(
    val title: String,
    val content: String,
    val sourceFile: String
)

@Dao
interface SearchDao {

    @Query("SELECT title, content, source_file FROM search_index_fts WHERE search_index_fts MATCH :query")
    suspend fun search(query: String): List<SearchResultRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: SearchDocument): Long

    @Query("DELETE FROM search_documents")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM search_documents")
    suspend fun documentCount(): Int
}
