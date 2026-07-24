package com.staticquo.vault

import android.content.Context
import com.staticquo.data.db.VaultEntryDao
import com.staticquo.data.db.VaultEntryEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class DecryptedEntry(
    val id: Long,
    val title: String,
    val contentType: String,
    val content: ByteArray,
    val sizeBytes: Long,
    val createdAt: Long,
    val updatedAt: Long
)

sealed class VaultResult<T> {
    data class Success<T>(val data: T) : VaultResult<T>()
    data class Error<T>(val message: String) : VaultResult<T>()
}

@Singleton
class VaultRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: VaultEntryDao,
    private val encryption: VaultEncryptionManager
) {

    private fun vaultDir(): File = File(context.filesDir, "vault")

    suspend fun getAllEntries(): VaultResult<List<VaultEntryEntity>> {
        return try {
            VaultResult.Success(dao.getAll())
        } catch (e: Exception) {
            VaultResult.Error("Failed to load entries: ${e.message}")
        }
    }

    suspend fun getEntry(id: Long): VaultResult<DecryptedEntry> {
        return try {
            val entity = dao.getById(id)
                ?: return VaultResult.Error("Entry not found")

            val encryptedFile = File(entity.encryptedFilePath)
            if (!encryptedFile.exists()) {
                return VaultResult.Error("Encrypted data file missing")
            }

            when (val result = encryption.decryptFile(encryptedFile)) {
                is VaultEncryptionResult.Success -> {
                    VaultResult.Success(
                        DecryptedEntry(
                            id = entity.id,
                            title = entity.title,
                            contentType = entity.contentType,
                            content = result.data,
                            sizeBytes = entity.sizeBytes,
                            createdAt = entity.createdAt,
                            updatedAt = entity.updatedAt
                        )
                    )
                }
                is VaultEncryptionResult.Error -> {
                    VaultResult.Error(result.message)
                }
            }
        } catch (e: Exception) {
            VaultResult.Error("Failed to read entry: ${e.message}")
        }
    }

    suspend fun createNote(title: String, plaintext: String): VaultResult<Long> {
        return try {
            val dir = vaultDir()
            dir.mkdirs()

            val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)

            val entity = VaultEntryEntity(
                title = title,
                contentType = "text/plain",
                encryptedFilePath = "",
                sizeBytes = 0
            )
            val id = dao.insert(entity)
            val targetFile = File(dir, "$id.enc")

            when (val encryptionResult = encryption.encryptToFile(plaintextBytes, targetFile)) {
                is VaultEncryptionResult.Success -> {
                    val updated = entity.copy(
                        id = id,
                        encryptedFilePath = targetFile.absolutePath,
                        sizeBytes = encryptionResult.data
                    )
                    dao.insert(updated)
                    VaultResult.Success(id)
                }
                is VaultEncryptionResult.Error -> {
                    dao.delete(id)
                    targetFile.delete()
                    VaultResult.Error(encryptionResult.message)
                }
            }
        } catch (e: Exception) {
            VaultResult.Error("Failed to create entry: ${e.message}")
        }
    }

    suspend fun deleteEntry(id: Long): VaultResult<Unit> {
        return try {
            val entity = dao.getById(id)
            if (entity != null) {
                File(entity.encryptedFilePath).delete()
            }
            dao.delete(id)
            VaultResult.Success(Unit)
        } catch (e: Exception) {
            VaultResult.Error("Failed to delete entry: ${e.message}")
        }
    }
}
