package com.staticquo.vault

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.android.AndroidKeysetManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class VaultEncryptionResult<T> {
    data class Success<T>(val data: T) : VaultEncryptionResult<T>()
    data class Error<T>(val message: String, val cause: Throwable? = null) : VaultEncryptionResult<T>()
}

@Singleton
class VaultEncryptionManager @Inject constructor(
    private val context: Context
) {
    private var aead: Aead? = null
    private var initError: String? = null

    init {
        try {
            AeadConfig.register()
            aead = AndroidKeysetManager.Builder()
                .withSharedPref(context, "vault_keyset", "vault_prefs")
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri("android-keystore://staticquo_vault_master_key")
                .build()
                .keysetHandle
                .getPrimitive(Aead::class.java)
        } catch (e: Exception) {
            initError = "Encryption init failed: ${e.message}"
        }
    }

    fun encrypt(plaintext: ByteArray): VaultEncryptionResult<ByteArray> {
        val aead = aead ?: return VaultEncryptionResult.Error(initError ?: "AEAD not initialized")
        return try {
            val ciphertext = aead.encrypt(plaintext, null)
            VaultEncryptionResult.Success(ciphertext)
        } catch (e: Exception) {
            VaultEncryptionResult.Error("Encryption failed: ${e.message}", e)
        }
    }

    fun decrypt(ciphertext: ByteArray): VaultEncryptionResult<ByteArray> {
        val aead = aead ?: return VaultEncryptionResult.Error(initError ?: "AEAD not initialized")
        return try {
            val plaintext = aead.decrypt(ciphertext, null)
            VaultEncryptionResult.Success(plaintext)
        } catch (e: Exception) {
            VaultEncryptionResult.Error("Decryption failed: ${e.message}", e)
        }
    }

    fun encryptToFile(plaintext: ByteArray, targetFile: File): VaultEncryptionResult<Long> {
        val aead = aead ?: return VaultEncryptionResult.Error(initError ?: "AEAD not initialized")
        return try {
            targetFile.parentFile?.mkdirs()
            val ciphertext = aead.encrypt(plaintext, null)
            FileOutputStream(targetFile).use { it.write(ciphertext) }
            VaultEncryptionResult.Success(ciphertext.size.toLong())
        } catch (e: Exception) {
            VaultEncryptionResult.Error("Encrypt to file failed: ${e.message}", e)
        }
    }

    fun decryptFile(sourceFile: File): VaultEncryptionResult<ByteArray> {
        val aead = aead ?: return VaultEncryptionResult.Error(initError ?: "AEAD not initialized")
        return try {
            val ciphertext = sourceFile.readBytes()
            val plaintext = aead.decrypt(ciphertext, null)
            VaultEncryptionResult.Success(plaintext)
        } catch (e: Exception) {
            VaultEncryptionResult.Error("Decrypt file failed: ${e.message}", e)
        }
    }
}
