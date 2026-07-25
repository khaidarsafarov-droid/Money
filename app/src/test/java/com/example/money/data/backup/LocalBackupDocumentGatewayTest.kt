package com.example.money.data.backup

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.money.domain.backup.BackupStorageAccessException
import com.example.money.domain.backup.BackupStorageSizeLimitException
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LocalBackupDocumentGatewayTest {

    @Test
    fun contentUri_roundTripsThroughDocumentProvider() = runBlocking {
        val fixture = documentFixture()
        val gateway = LocalBackupDocumentGateway(fixture.context.contentResolver)
        val expected = byteArrayOf(1, 3, 5, 7, 9)

        gateway.write(fixture.uri, expected)
        val actual = gateway.read(fixture.uri)

        assertArrayEquals(expected, actual)
    }

    @Test
    fun readRejectsContentLargerThanConfiguredLimit() {
        val fixture = documentFixture()
        fixture.file.writeBytes(ByteArray(5))
        val gateway = LocalBackupDocumentGateway(
            contentResolver = fixture.context.contentResolver,
            maxBackupBytes = 4,
        )

        val error = assertThrows(BackupStorageSizeLimitException::class.java) {
            runBlocking { gateway.read(fixture.uri) }
        }

        assertEquals(4L, error.maxBytes)
        assertEquals(5L, error.actualBytes)
    }

    @Test
    fun writeRejectsContentLargerThanConfiguredLimitBeforeOpeningUri() {
        val fixture = documentFixture()
        val gateway = LocalBackupDocumentGateway(
            contentResolver = fixture.context.contentResolver,
            maxBackupBytes = 4,
        )

        assertThrows(BackupStorageSizeLimitException::class.java) {
            runBlocking { gateway.write(fixture.uri, ByteArray(5)) }
        }

        assertEquals(0L, fixture.file.length())
    }

    @Test
    fun inaccessibleUri_throwsTypedAccessError() {
        val context: Context = RuntimeEnvironment.getApplication()
        val gateway = LocalBackupDocumentGateway(context.contentResolver)

        val error = assertThrows(BackupStorageAccessException::class.java) {
            runBlocking {
                gateway.read(Uri.parse("content://missing-provider/backup.moneybackup"))
            }
        }

        assertEquals("read", error.operation)
    }

    private fun documentFixture(): DocumentFixture {
        val context: Context = RuntimeEnvironment.getApplication()
        val file = File(context.cacheDir, "backup-${System.nanoTime()}.moneybackup")
        val provider = FileDocumentProvider(file)
        provider.attachInfo(
            context,
            ProviderInfo().apply {
                authority = AUTHORITY
                exported = false
            },
        )
        ShadowContentResolver.registerProviderInternal(AUTHORITY, provider)
        return DocumentFixture(
            context = context,
            file = file,
            uri = Uri.parse("content://$AUTHORITY/${file.name}"),
        )
    }

    private data class DocumentFixture(
        val context: Context,
        val file: File,
        val uri: Uri,
    )

    private class FileDocumentProvider(
        private val file: File,
    ) : ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
            val flags = if ('w' in mode) {
                ParcelFileDescriptor.MODE_CREATE or
                    ParcelFileDescriptor.MODE_TRUNCATE or
                    ParcelFileDescriptor.MODE_WRITE_ONLY
            } else {
                ParcelFileDescriptor.MODE_READ_ONLY
            }
            return ParcelFileDescriptor.open(file, flags)
        }

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor? = null

        override fun getType(uri: Uri): String = "application/octet-stream"

        override fun insert(uri: Uri, values: ContentValues?): Uri? = null

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0
    }

    private companion object {
        const val AUTHORITY = "com.example.money.test.backup.documents"
    }
}
