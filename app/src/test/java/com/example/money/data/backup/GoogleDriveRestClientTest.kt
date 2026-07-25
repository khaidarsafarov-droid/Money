package com.example.money.data.backup

import com.example.money.domain.backup.BackupRemoteFile
import com.example.money.domain.backup.BackupStorageHttpException
import com.example.money.domain.backup.BackupStorageSizeLimitException
import com.example.money.domain.backup.BackupStorageUnauthorizedException
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleDriveRestClientTest {

    @Test
    fun list_paginatesAppDataFolderAndEncodesPageToken() = runBlocking {
        val transport = FakeGoogleDriveHttpTransport(
            GoogleDriveHttpResponse(
                statusCode = 200,
                body = """
                    {
                      "files": [
                        {
                          "id": "first-id",
                          "name": "first.moneybackup",
                          "size": "42",
                          "createdTime": "2026-07-25T10:00:00.000Z",
                          "modifiedTime": "2026-07-25T10:01:00.000Z"
                        }
                      ],
                      "nextPageToken": "next/token +?"
                    }
                """.trimIndent().toByteArray(),
            ),
            GoogleDriveHttpResponse(
                statusCode = 200,
                body = """
                    {
                      "files": [
                        {
                          "id": "second-id",
                          "name": "second.moneybackup",
                          "size": "84",
                          "createdTime": "2026-07-25T11:00:00.000Z",
                          "modifiedTime": "2026-07-25T11:01:00.000Z"
                        }
                      ]
                    }
                """.trimIndent().toByteArray(),
            ),
        )
        val client = GoogleDriveRestClient(transport)

        val files = client.list(ACCESS_TOKEN)

        assertEquals(listOf("first-id", "second-id"), files.map(BackupRemoteFile::id))
        assertEquals(42L, files.first().sizeBytes)
        assertEquals(2, transport.requests.size)
        assertTrue(transport.requests.first().url.contains("spaces=appDataFolder"))
        assertTrue(transport.requests.first().url.contains("trashed%3Dfalse"))
        assertFalse(transport.requests.first().url.contains("pageToken="))
        assertTrue(
            transport.requests[1].url.contains("pageToken=next%2Ftoken%20%2B%3F"),
        )
        assertEquals("Bearer $ACCESS_TOKEN", transport.requests.first().headers["Authorization"])
    }

    @Test
    fun upload_createsNewFileWithAppDataFolderParent() = runBlocking {
        val transport = FakeGoogleDriveHttpTransport(
            GoogleDriveHttpResponse(
                statusCode = 200,
                body = """
                    {
                      "id": "created-id",
                      "name": "backup \"one\".moneybackup",
                      "size": "4",
                      "createdTime": "2026-07-25T12:00:00.000Z",
                      "modifiedTime": "2026-07-25T12:00:00.000Z"
                    }
                """.trimIndent().toByteArray(),
            ),
        )
        val client = GoogleDriveRestClient(transport)
        val content = byteArrayOf(1, 2, 3, 4)

        val uploaded = client.upload(ACCESS_TOKEN, """backup "one".moneybackup""", content)

        val request = transport.requests.single()
        val requestBody = request.body!!.toString(StandardCharsets.UTF_8)
        assertEquals("POST", request.method)
        assertTrue(request.url.contains("/upload/drive/v3/files"))
        assertTrue(request.url.contains("uploadType=multipart"))
        assertTrue(request.headers.getValue("Content-Type").startsWith("multipart/related; boundary="))
        assertTrue(requestBody.contains(""""parents":["appDataFolder"]"""))
        assertTrue(requestBody.contains(""""name":"backup \"one\".moneybackup""""))
        assertTrue(requestBody.contains(content.toString(StandardCharsets.ISO_8859_1)))
        assertEquals("created-id", uploaded.id)
        assertEquals(4L, uploaded.sizeBytes)
    }

    @Test
    fun download_encodesFileIdAndReturnsMedia() = runBlocking {
        val expected = byteArrayOf(9, 7, 5, 3)
        val transport = FakeGoogleDriveHttpTransport(
            GoogleDriveHttpResponse(statusCode = 200, body = expected),
        )
        val client = GoogleDriveRestClient(transport)

        val actual = client.download(ACCESS_TOKEN, "id/with space?")

        val request = transport.requests.single()
        assertEquals("GET", request.method)
        assertTrue(request.url.contains("/files/id%2Fwith%20space%3F"))
        assertTrue(request.url.endsWith("alt=media"))
        assertArrayEquals(expected, actual)
    }

    @Test
    fun delete_permanentlyDeletesEncodedFileId() = runBlocking {
        val transport = FakeGoogleDriveHttpTransport(
            GoogleDriveHttpResponse(statusCode = 204),
        )
        val client = GoogleDriveRestClient(transport)

        client.delete(ACCESS_TOKEN, "delete/id")

        val request = transport.requests.single()
        assertEquals("DELETE", request.method)
        assertTrue(request.url.endsWith("/files/delete%2Fid"))
    }

    @Test
    fun providerBindsTokenAndDelegatesStorageContract() = runBlocking {
        val transport = FakeGoogleDriveHttpTransport(
            GoogleDriveHttpResponse(statusCode = 200, body = """{"files":[]}""".toByteArray()),
        )
        val provider = GoogleDriveBackupStorageProvider(
            accessToken = ACCESS_TOKEN,
            client = GoogleDriveRestClient(transport),
        )

        assertTrue(provider.list().isEmpty())

        assertEquals("Bearer $ACCESS_TOKEN", transport.requests.single().headers["Authorization"])
    }

    @Test
    fun unauthorizedResponse_throwsTypedError() {
        val client = GoogleDriveRestClient(
            FakeGoogleDriveHttpTransport(
                GoogleDriveHttpResponse(
                    statusCode = 401,
                    body = """{"error":{"message":"expired"}}""".toByteArray(),
                ),
            ),
        )

        assertThrows(BackupStorageUnauthorizedException::class.java) {
            runBlocking { client.list(ACCESS_TOKEN) }
        }
    }

    @Test
    fun otherNonSuccessResponse_includesStatusAndOperation() {
        val client = GoogleDriveRestClient(
            FakeGoogleDriveHttpTransport(
                GoogleDriveHttpResponse(
                    statusCode = 503,
                    body = "temporarily unavailable".toByteArray(),
                ),
            ),
        )

        val error = assertThrows(BackupStorageHttpException::class.java) {
            runBlocking { client.list(ACCESS_TOKEN) }
        }

        assertEquals(503, error.statusCode)
        assertEquals("list", error.operation)
        assertTrue(error.responseBody.contains("temporarily unavailable"))
    }

    @Test
    fun oversizedDownload_throwsTypedSizeLimitError() {
        val client = GoogleDriveRestClient(
            transport = FakeGoogleDriveHttpTransport(
                GoogleDriveHttpResponse(statusCode = 200, body = ByteArray(5)),
            ),
            maxBackupBytes = 4,
        )

        val error = assertThrows(BackupStorageSizeLimitException::class.java) {
            runBlocking { client.download(ACCESS_TOKEN, "file-id") }
        }

        assertEquals(4L, error.maxBytes)
        assertEquals(5L, error.actualBytes)
    }

    private class FakeGoogleDriveHttpTransport(
        vararg responses: GoogleDriveHttpResponse,
    ) : GoogleDriveHttpTransport {
        private val remainingResponses = ArrayDeque(responses.asList())
        val requests = mutableListOf<GoogleDriveHttpRequest>()

        override suspend fun execute(request: GoogleDriveHttpRequest): GoogleDriveHttpResponse {
            requests += request
            return remainingResponses.removeFirst()
        }
    }

    private companion object {
        const val ACCESS_TOKEN = "test-access-token"
    }
}
