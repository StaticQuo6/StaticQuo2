package com.staticquo.lock

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinHashUtil @Inject constructor(
    private val argon2: Argon2Kt
) {
    companion object {
        private const val SALT_LENGTH = 16
        private const val HASH_LENGTH = 32
        private const val ITERATIONS = 3
        private const val MEMORY_COST_KB = 65536
        private const val PARALLELISM = 4
    }

    data class HashResult(
        val hash: String
    )

    suspend fun hash(pin: String): HashResult = withContext(Dispatchers.Default) {
        val salt = ByteArray(SALT_LENGTH).apply {
            SecureRandom().nextBytes(this)
        }
        val result = argon2.hash(
            mode = Argon2Mode.ARGON2_ID,
            password = pin.toByteArray(),
            salt = salt,
            tCostInIterations = ITERATIONS,
            mCostInKibibyte = MEMORY_COST_KB,
            parallelism = PARALLELISM,
            hashLengthInBytes = HASH_LENGTH
        )
        HashResult(hash = result.encodedOutputAsString())
    }

    suspend fun verify(pin: String, encodedHash: String): Boolean = withContext(Dispatchers.Default) {
        try {
            argon2.verify(
                mode = Argon2Mode.ARGON2_ID,
                encoded = encodedHash,
                password = pin.toByteArray()
            )
        } catch (_: Exception) {
            false
        }
    }
}
