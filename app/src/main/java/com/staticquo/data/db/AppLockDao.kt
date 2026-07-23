package com.staticquo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppLockDao {

    @Query("SELECT * FROM app_lock WHERE id = 1")
    suspend fun getLockConfig(): AppLockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLockConfig(lock: AppLockEntity)

    @Query("UPDATE app_lock SET failed_attempts = :attempts, lockout_until = :lockoutUntil, updated_at = :now WHERE id = 1")
    suspend fun updateFailedAttempts(attempts: Int, lockoutUntil: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE app_lock SET pin_hash = :pinHash, duress_pin_hash = :duressPinHash, updated_at = :now WHERE id = 1")
    suspend fun updatePinHashes(pinHash: String, duressPinHash: String?, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM app_lock")
    suspend fun clearAll()
}
