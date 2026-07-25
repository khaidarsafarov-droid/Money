package com.example.money.data.backup

import com.example.money.data.datastore.PortablePreferencesStorage
import com.example.money.data.sync.SyncWriteGuard
import com.example.money.domain.backup.BackupBundle
import com.example.money.domain.backup.BackupPreferences
import com.example.money.domain.backup.BackupPreview
import com.example.money.domain.backup.BackupVisionImage
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class BackupImporter(
    private val roomApplier: BackupRoomApplier,
    private val preferencesStore: PortablePreferencesStorage,
    private val visionImageStorage: VisionImageStorage,
    private val codec: BackupBundleCodec = BackupBundleCodec(),
    private val crypto: PasswordBackupCrypto = PasswordBackupCrypto(),
) {

    fun previewEncrypted(encrypted: ByteArray, password: CharArray): BackupPreview =
        codec.preview(crypto.decrypt(encrypted, password))

    suspend fun importEncrypted(encrypted: ByteArray, password: CharArray) {
        val validated = codec.decode(crypto.decrypt(encrypted, password))
        importValidatedBundle(validated)
    }

    suspend fun importBundle(bundle: BackupBundle) {
        importValidatedBundle(codec.canonicalize(bundle))
    }

    private suspend fun importValidatedBundle(bundle: BackupBundle) {
        SyncWriteGuard.withWriteLock {
            val rollback = RollbackState(
                room = roomApplier.captureRollbackLocked(),
                preferences = preferencesStore.exportPortable(),
                visionImage = visionImageStorage.readCurrent(),
            )
            try {
                roomApplier.applyLocked(bundle)
                preferencesStore.importPortable(bundle.preferences)
                visionImageStorage.replaceCurrent(bundle.visionImage)
            } catch (error: Exception) {
                val rollbackError = restoreRollback(rollback)
                rollbackError?.let(error::addSuppressed)
                throw error
            }
        }
    }

    private suspend fun restoreRollback(rollback: RollbackState): Exception? =
        withContext(NonCancellable) {
            try {
                roomApplier.restoreRollbackLocked(rollback.room)
                preferencesStore.importPortable(rollback.preferences)
                visionImageStorage.replaceCurrent(rollback.visionImage)
                null
            } catch (error: Exception) {
                error
            }
        }

    private data class RollbackState(
        val room: BackupRoomApplier.RoomRollbackSnapshot,
        val preferences: BackupPreferences,
        val visionImage: BackupVisionImage?,
    )
}
