package com.staticquo.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_lock")
data class VaultLockEntity(
    @PrimaryKey
    val id: Int = 1,

    @ColumnInfo(name = "pin_hash")
    val pinHash: String,

    @ColumnInfo(name = "failed_attempts")
    val failedAttempts: Int = 0,

    @ColumnInfo(name = "lockout_until")
    val lockoutUntil: Long = 0,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
