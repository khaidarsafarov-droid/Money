package com.example.money.data.backup

import com.example.money.domain.backup.BackupRemoteFile
import com.example.money.domain.backup.BackupStorageProvider

class GoogleDriveBackupStorageProvider(
    private val accessToken: String,
    private val client: GoogleDriveRestClient,
) : BackupStorageProvider {
    init {
        require(accessToken.isNotBlank()) { "accessToken must not be blank" }
    }

    override suspend fun list(): List<BackupRemoteFile> = client.list(accessToken)

    override suspend fun upload(
        fileName: String,
        content: ByteArray,
    ): BackupRemoteFile = client.upload(accessToken, fileName, content)

    override suspend fun download(fileId: String): ByteArray =
        client.download(accessToken, fileId)

    override suspend fun delete(fileId: String) {
        client.delete(accessToken, fileId)
    }
}
