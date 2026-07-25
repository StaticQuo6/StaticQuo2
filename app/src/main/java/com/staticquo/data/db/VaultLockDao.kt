package com.staticquo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VaultLockDao {

    @Query("SELECT * FROM vault_lock WHERE id = 1")
    suspend fun getLockConfig(): VaultLockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLockConfig(lock: VaultLockEntity)

    @Query("UPDATE vault_lock SET failed_attempts = :attempts, lockout_until = :lockoutUntil, updated_at = :now WHERE id = 1")
    suspend fun updateFailedAttempts(attempts: Int, lockoutUntil: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE vault_lock SET pin_hash = :pinHash, updated_at = :now WHERE id = 1")
    suspend fun updatePinHash(pinHash: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM vault_lock")
    suspend fun clearAll()
}
