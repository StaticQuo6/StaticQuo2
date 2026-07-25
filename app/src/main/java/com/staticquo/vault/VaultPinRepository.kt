package com.staticquo.vault

import com.staticquo.data.db.VaultLockDao
import com.staticquo.data.db.VaultLockEntity
import com.staticquo.lock.PinHashUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultPinRepository @Inject constructor(
    private val dao: VaultLockDao,
    private val hashUtil: PinHashUtil
) {

    suspend fun isPinSet(): Boolean {
        return dao.getLockConfig() != null
    }

    suspend fun setPin(pin: String) {
        val pinHash = hashUtil.hash(pin)
        dao.upsertLockConfig(
            VaultLockEntity(
                pinHash = pinHash.hash
            )
        )
    }

    enum class VaultPinResult {
        CORRECT,
        INCORRECT,
        LOCKED_OUT
    }

    suspend fun verifyPin(pin: String): VaultPinResult {
        val config = dao.getLockConfig() ?: return VaultPinResult.INCORRECT

        if (isLockedOut(config)) return VaultPinResult.LOCKED_OUT

        val mainMatch = hashUtil.verify(pin, config.pinHash)
        if (mainMatch) {
            dao.updateFailedAttempts(0, 0)
            return VaultPinResult.CORRECT
        }

        val newAttempts = config.failedAttempts + 1
        val lockoutDuration = when {
            newAttempts >= 15 -> 30 * 60 * 1000L
            newAttempts >= 10 -> 5 * 60 * 1000L
            newAttempts >= 5 -> 30 * 1000L
            else -> 0L
        }
        val lockoutUntil = if (lockoutDuration > 0) {
            System.currentTimeMillis() + lockoutDuration
        } else 0L

        dao.updateFailedAttempts(newAttempts, lockoutUntil)
        return VaultPinResult.INCORRECT
    }

    suspend fun getRemainingLockoutMs(): Long {
        val config = dao.getLockConfig() ?: return 0
        if (!isLockedOut(config)) return 0
        return config.lockoutUntil - System.currentTimeMillis()
    }

    suspend fun getFailedAttempts(): Int {
        return dao.getLockConfig()?.failedAttempts ?: 0
    }

    suspend fun clearPin() {
        dao.clearAll()
    }

    private fun isLockedOut(config: VaultLockEntity): Boolean {
        if (config.lockoutUntil == 0L) return false
        return System.currentTimeMillis() < config.lockoutUntil
    }
}
