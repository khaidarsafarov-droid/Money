package com.example.money.data.backup

import com.example.money.data.datastore.PortablePreferencesStorage
import com.example.money.data.sync.SyncWriteGuard
import com.example.money.domain.backup.BackupBundle
import com.example.money.domain.backup.BackupPreferences
import com.example.money.domain.backup.BackupPreview
import com.example.money.domain.backup.BackupVisionImage
import com.example.money.domain.backup.ImportResult
import com.example.money.domain.backup.ImportWarning
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class BackupImporter(
    private val roomApplier: BackupRoomApplier,
    private val preferencesStore: PortablePreferencesStorage,
    private val visionImageStorage: VisionImageStorage,
    private val codec: BackupBundleCodec = BackupBundleCodec(),
    private val crypto: PasswordBackupCrypto = PasswordBackupCrypto(),
    private val postRestoreReconciler: PostRestoreReconciler = PostRestoreReconciler.NO_OP,
) {

    fun previewEncrypted(encrypted: ByteArray, password: CharArray): BackupPreview =
        codec.preview(crypto.decrypt(encrypted, password))

    suspend fun importEncrypted(encrypted: ByteArray, password: CharArray): ImportResult {
        val validated = codec.decode(crypto.decrypt(encrypted, password))
        return importValidatedBundle(validated)
    }

    suspend fun importBundle(bundle: BackupBundle): ImportResult =
        importValidatedBundle(codec.canonicalize(bundle))

    private suspend fun importValidatedBundle(bundle: BackupBundle): ImportResult {
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
        return reconcileAfterRestore()
    }

    private suspend fun reconcileAfterRestore(): ImportResult =
        try {
            ImportResult(warnings = postRestoreReconciler.reconcile())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            ImportResult(
                warnings = listOf(
                    ImportWarning(
                        subsystem = "post_restore",
                        message = error.message ?: error::class.java.simpleName,
                    ),
                ),
            )
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
