package com.example.money.domain.repository

import com.example.money.domain.backup.BackupBundle
import com.example.money.domain.backup.BackupPreview
import com.example.money.domain.backup.ImportResult

interface BackupRepository {
    suspend fun exportEncrypted(password: CharArray): ByteArray

    fun preview(encrypted: ByteArray, password: CharArray): BackupPreview

    fun previewEncrypted(encrypted: ByteArray, password: CharArray): BackupPreview =
        preview(encrypted, password)

    suspend fun importEncrypted(encrypted: ByteArray, password: CharArray): ImportResult

    suspend fun exportBundle(): BackupBundle

    suspend fun importBundle(bundle: BackupBundle): ImportResult
}
