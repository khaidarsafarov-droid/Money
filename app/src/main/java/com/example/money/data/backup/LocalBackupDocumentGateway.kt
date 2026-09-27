package com.example.money.data.backup

import android.content.ContentResolver
import android.net.Uri
import com.example.money.domain.backup.BackupStorageAccessException
import com.example.money.domain.backup.BackupStorageException
import com.example.money.domain.backup.BackupStorageSizeLimitException
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface BackupDocumentGateway {
    suspend fun read(uri: Uri): ByteArray

    suspend fun write(uri: Uri, content: ByteArray)
}

class LocalBackupDocumentGateway(
    private val contentResolver: ContentResolver,
    private val maxBackupBytes: Int = DEFAULT_MAX_BACKUP_BYTES,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BackupDocumentGateway {
    init {
        require(maxBackupBytes > 0) { "maxBackupBytes must be positive" }
    }

    override suspend fun read(uri: Uri): ByteArray = withContext(ioDispatcher) {
        requireContentUri(uri, operation = "read")
        try {
            rejectKnownOversize(uri)
            val input = contentResolver.openInputStream(uri)
                ?: throw IOException("The document provider returned no input stream")
            input.use { stream ->
                val output = ByteArrayOutputStream(minOf(DEFAULT_BUFFER_SIZE, maxBackupBytes))
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var totalBytes = 0L
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    totalBytes += read
                    if (totalBytes > maxBackupBytes) {
                        throw BackupStorageSizeLimitException(
                            maxBytes = maxBackupBytes.toLong(),
                            actualBytes = totalBytes,
                        )
                    }
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: BackupStorageException) {
            throw error
        } catch (error: Exception) {
            throw BackupStorageAccessException("read", error)
        }
    }

    override suspend fun write(uri: Uri, content: ByteArray) = withContext(ioDispatcher) {
        requireContentUri(uri, operation = "write")
        if (content.size > maxBackupBytes) {
            throw BackupStorageSizeLimitException(
                maxBytes = maxBackupBytes.toLong(),
                actualBytes = content.size.toLong(),
            )
        }
        try {
            val output = contentResolver.openOutputStream(uri, "wt")
                ?: throw IOException("The document provider returned no output stream")
            output.use { stream ->
                stream.write(content)
                stream.flush()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: BackupStorageException) {
            throw error
        } catch (error: Exception) {
            throw BackupStorageAccessException("write", error)
        }
    }

    private fun rejectKnownOversize(uri: Uri) {
        val length = contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            descriptor.length
        } ?: return
        if (length > maxBackupBytes) {
            throw BackupStorageSizeLimitException(
                maxBytes = maxBackupBytes.toLong(),
                actualBytes = length,
            )
        }
    }

    private fun requireContentUri(uri: Uri, operation: String) {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) {
            throw BackupStorageAccessException(
                operation = operation,
                cause = IllegalArgumentException("Expected a content Uri"),
            )
        }
    }

    companion object {
        const val DEFAULT_MAX_BACKUP_BYTES = 32 * 1024 * 1024
    }
}
