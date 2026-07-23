package com.staticquo.lock

import com.staticquo.data.db.AppLockDao
import com.staticquo.data.db.AppLockEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinRepository @Inject constructor(
    private val dao: AppLockDao,
    private val hashUtil: PinHashUtil
) {

    suspend fun isPinSet(): Boolean {
        return dao.getLockConfig() != null
    }

    suspend fun setPin(pin: String, duressPin: String? = null) {
        val pinHash = hashUtil.hash(pin)
        val duressHash = duressPin?.let { hashUtil.hash(it) }
        dao.upsertLockConfig(
            AppLockEntity(
                pinHash = pinHash.hash,
                duressPinHash = duressHash?.hash
            )
        )
    }

    enum class PinResult {
        CORRECT,
        DURESS_CORRECT,
        INCORRECT,
        LOCKED_OUT
    }

    suspend fun verifyPin(pin: String): PinResult {
        val config = dao.getLockConfig() ?: return PinResult.INCORRECT

        val duressHash = config.duressPinHash
        val isDuress = duressHash != null && hashUtil.verify(pin, duressHash)

        if (isDuress) {
            return PinResult.DURESS_CORRECT
        }

        if (isLockedOut(config)) return PinResult.LOCKED_OUT

        val mainMatch = hashUtil.verify(pin, config.pinHash)
        if (mainMatch) {
            dao.updateFailedAttempts(0, 0)
            return PinResult.CORRECT
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
        return PinResult.INCORRECT
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

    private fun isLockedOut(config: AppLockEntity): Boolean {
        if (config.lockoutUntil == 0L) return false
        return System.currentTimeMillis() < config.lockoutUntil
    }
}
