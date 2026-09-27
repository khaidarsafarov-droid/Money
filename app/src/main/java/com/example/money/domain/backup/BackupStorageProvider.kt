package com.example.money.domain.backup

data class BackupRemoteFile(
    val id: String,
    val name: String,
    val sizeBytes: Long?,
    val createdTime: String?,
    val modifiedTime: String?,
)

interface BackupStorageProvider {
    suspend fun list(): List<BackupRemoteFile>

    suspend fun upload(fileName: String, content: ByteArray): BackupRemoteFile

    suspend fun download(fileId: String): ByteArray

    suspend fun delete(fileId: String)
}

sealed class BackupStorageException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class BackupStorageSizeLimitException(
    val maxBytes: Long,
    val actualBytes: Long?,
) : BackupStorageException(
    buildString {
        append("Backup exceeds the ")
        append(maxBytes)
        append("-byte size limit")
        actualBytes?.let {
            append(" (actual: ")
            append(it)
            append(" bytes)")
        }
    },
)

class BackupStorageAccessException(
    val operation: String,
    cause: Throwable,
) : BackupStorageException("Unable to $operation the selected backup document", cause)

class BackupStorageNetworkException(
    val operation: String,
    cause: Throwable,
) : BackupStorageException("Network failure during Google Drive $operation", cause)

class BackupStorageUnauthorizedException(
    val operation: String,
) : BackupStorageException("Google Drive authorization is missing or expired during $operation")

class BackupStorageHttpException(
    val operation: String,
    val statusCode: Int,
    val responseBody: String,
) : BackupStorageException("Google Drive $operation failed with HTTP $statusCode")

class BackupStorageProtocolException(
    val operation: String,
    message: String,
    cause: Throwable? = null,
) : BackupStorageException("Invalid Google Drive $operation response: $message", cause)
