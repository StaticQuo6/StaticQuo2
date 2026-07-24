package com.staticquo.search

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4(contentEntity = SearchDocument::class)
@Entity(tableName = "search_index_fts")
data class SearchIndexFts(
    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "source_file")
    val sourceFile: String
)

@Entity(tableName = "search_documents")
data class SearchDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "source_file")
    val sourceFile: String,

    @ColumnInfo(name = "imported_at")
    val importedAt: Long = System.currentTimeMillis()
)
