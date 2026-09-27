package com.example.money.data.backup

import com.example.money.domain.backup.BackupBundle
import com.example.money.domain.backup.BackupPreview
import com.example.money.domain.backup.ImportResult
import com.example.money.domain.repository.BackupRepository

class BackupRepositoryImpl(
    private val exporter: BackupExporter,
    private val importer: BackupImporter,
) : BackupRepository {

    override suspend fun exportEncrypted(password: CharArray): ByteArray =
        exporter.exportEncrypted(password)

    override fun preview(encrypted: ByteArray, password: CharArray): BackupPreview =
        importer.previewEncrypted(encrypted, password)

    override suspend fun importEncrypted(encrypted: ByteArray, password: CharArray): ImportResult =
        importer.importEncrypted(encrypted, password)

    override suspend fun exportBundle(): BackupBundle = exporter.exportBundle()

    override suspend fun importBundle(bundle: BackupBundle): ImportResult =
        importer.importBundle(bundle)
}
