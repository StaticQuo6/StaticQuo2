package com.staticquo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VaultEntryDao {

    @Query("SELECT * FROM vault_entries ORDER BY updated_at DESC")
    suspend fun getAll(): List<VaultEntryEntity>

    @Query("SELECT * FROM vault_entries WHERE id = :id")
    suspend fun getById(id: Long): VaultEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: VaultEntryEntity): Long

    @Query("DELETE FROM vault_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM vault_entries")
    suspend fun clearAll()
}
