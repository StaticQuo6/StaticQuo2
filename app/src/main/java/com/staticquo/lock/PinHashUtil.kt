package com.staticquo.lock

import com.lambdapioneer.argon2kt.Argon2Kotlin
import com.lambdapioneer.argon2kt.Argon2Mode
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinHashUtil @Inject constructor(
    private val argon2: Argon2Kotlin
) {
    companion object {
        private const val SALT_LENGTH = 16
        private const val HASH_LENGTH = 32
        private const val ITERATIONS = 3
        private const val MEMORY_COST_KB = 65536
        private const val PARALLELISM = 4
    }

    data class HashResult(
        val hash: String,
        val salt: String
    )

    fun hash(pin: String): HashResult {
        val salt = ByteArray(SALT_LENGTH).apply {
            SecureRandom().nextBytes(this)
        }
        val hash = argon2.hash(
            mode = Argon2Mode.ARGON2ID,
            password = pin.toByteArray(),
            salt = salt,
            iterations = ITERATIONS,
            memoryCostInKib = MEMORY_COST_KB,
            parallelism = PARALLELISM,
            hashLengthInBytes = HASH_LENGTH
        )
        return HashResult(
            hash = hash.encodedHash,
            salt = hash.encodedSalt
        )
    }

    fun verify(pin: String, encodedHash: String): Boolean {
        return try {
            argon2.verify(
                mode = Argon2Mode.ARGON2ID,
                encodedHash = encodedHash,
                password = pin.toByteArray()
            )
        } catch (_: Exception) {
            false
        }
    }
}
