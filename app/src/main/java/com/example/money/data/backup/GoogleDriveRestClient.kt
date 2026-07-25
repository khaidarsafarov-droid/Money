package com.example.money.data.backup

import com.example.money.domain.backup.BackupRemoteFile
import com.example.money.domain.backup.BackupStorageException
import com.example.money.domain.backup.BackupStorageHttpException
import com.example.money.domain.backup.BackupStorageNetworkException
import com.example.money.domain.backup.BackupStorageProtocolException
import com.example.money.domain.backup.BackupStorageSizeLimitException
import com.example.money.domain.backup.BackupStorageUnauthorizedException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class GoogleDriveRestClient(
    private val transport: GoogleDriveHttpTransport,
    apiBaseUrl: String = DEFAULT_API_BASE_URL,
    uploadBaseUrl: String = DEFAULT_UPLOAD_BASE_URL,
    private val maxBackupBytes: Int = LocalBackupDocumentGateway.DEFAULT_MAX_BACKUP_BYTES,
    private val connectTimeoutMillis: Int = DEFAULT_CONNECT_TIMEOUT_MILLIS,
    private val readTimeoutMillis: Int = DEFAULT_READ_TIMEOUT_MILLIS,
    private val maxMetadataResponseBytes: Int = DEFAULT_MAX_METADATA_RESPONSE_BYTES,
) {
    private val apiBaseUrl = apiBaseUrl.trimEnd('/')
    private val uploadBaseUrl = uploadBaseUrl.trimEnd('/')

    init {
        require(maxBackupBytes > 0) { "maxBackupBytes must be positive" }
        require(connectTimeoutMillis > 0) { "connectTimeoutMillis must be positive" }
        require(readTimeoutMillis > 0) { "readTimeoutMillis must be positive" }
        require(maxMetadataResponseBytes > 0) {
            "maxMetadataResponseBytes must be positive"
        }
    }

    suspend fun list(accessToken: String): List<BackupRemoteFile> {
        val files = mutableListOf<BackupRemoteFile>()
        val seenPageTokens = mutableSetOf<String>()
        var pageToken: String? = null
        var pageCount = 0
        do {
            pageCount += 1
            if (pageCount > MAX_LIST_PAGES) {
                throw BackupStorageProtocolException(
                    operation = "list",
                    message = "pagination exceeded $MAX_LIST_PAGES pages",
                )
            }
            val query = linkedMapOf(
                "spaces" to APP_DATA_FOLDER,
                "q" to "trashed=false",
                "fields" to "nextPageToken,files(id,name,size,createdTime,modifiedTime)",
                "orderBy" to "createdTime desc",
                "pageSize" to PAGE_SIZE.toString(),
            )
            pageToken?.let { query["pageToken"] = it }
            val response = execute(
                operation = "list",
                accessToken = accessToken,
                method = "GET",
                url = "$apiBaseUrl/files?${encodeQuery(query)}",
                maxResponseBytes = maxMetadataResponseBytes,
            )
            val page = parseListPage(response.body)
            files += page.files
            pageToken = page.nextPageToken
            if (pageToken != null && !seenPageTokens.add(pageToken)) {
                throw BackupStorageProtocolException(
                    operation = "list",
                    message = "pagination returned a repeated page token",
                )
            }
        } while (pageToken != null)
        return files
    }

    suspend fun upload(
        accessToken: String,
        fileName: String,
        content: ByteArray,
    ): BackupRemoteFile {
        require(fileName.isNotBlank()) { "fileName must not be blank" }
        if (content.size > maxBackupBytes) {
            throw BackupStorageSizeLimitException(
                maxBytes = maxBackupBytes.toLong(),
                actualBytes = content.size.toLong(),
            )
        }

        val boundary = "money-backup-${UUID.randomUUID()}"
        val metadata = JSONObject()
            .put("name", fileName)
            .put("parents", JSONArray().put(APP_DATA_FOLDER))
            .toString()
        val body = buildMultipartBody(boundary, metadata, content)
        val url = "$uploadBaseUrl/files?${encodeQuery(
            linkedMapOf(
                "uploadType" to "multipart",
                "fields" to "id,name,size,createdTime,modifiedTime",
            ),
        )}"
        val response = execute(
            operation = "upload",
            accessToken = accessToken,
            method = "POST",
            url = url,
            body = body,
            contentType = "multipart/related; boundary=$boundary",
            maxResponseBytes = maxMetadataResponseBytes,
        )
        return parseRemoteFile(response.body, operation = "upload")
    }

    suspend fun download(accessToken: String, fileId: String): ByteArray {
        require(fileId.isNotBlank()) { "fileId must not be blank" }
        val response = execute(
            operation = "download",
            accessToken = accessToken,
            method = "GET",
            url = "$apiBaseUrl/files/${percentEncode(fileId)}?alt=media",
            maxResponseBytes = maxBackupBytes,
        )
        return response.body
    }

    suspend fun delete(accessToken: String, fileId: String) {
        require(fileId.isNotBlank()) { "fileId must not be blank" }
        execute(
            operation = "delete",
            accessToken = accessToken,
            method = "DELETE",
            url = "$apiBaseUrl/files/${percentEncode(fileId)}",
            maxResponseBytes = maxMetadataResponseBytes,
        )
    }

    private suspend fun execute(
        operation: String,
        accessToken: String,
        method: String,
        url: String,
        body: ByteArray? = null,
        contentType: String? = null,
        maxResponseBytes: Int,
    ): GoogleDriveHttpResponse {
        if (accessToken.isBlank()) {
            throw BackupStorageUnauthorizedException(operation)
        }
        val headers = buildMap {
            put("Authorization", "Bearer $accessToken")
            put("Accept", "application/json")
            contentType?.let { put("Content-Type", it) }
        }
        val response = try {
            transport.execute(
                GoogleDriveHttpRequest(
                    method = method,
                    url = url,
                    headers = headers,
                    body = body,
                    connectTimeoutMillis = connectTimeoutMillis,
                    readTimeoutMillis = readTimeoutMillis,
                    maxResponseBytes = maxResponseBytes,
                ),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: BackupStorageException) {
            throw error
        } catch (error: IOException) {
            throw BackupStorageNetworkException(operation, error)
        }
        if (response.body.size > maxResponseBytes) {
            throw BackupStorageSizeLimitException(
                maxBytes = maxResponseBytes.toLong(),
                actualBytes = response.body.size.toLong(),
            )
        }
        when {
            response.statusCode == 401 ->
                throw BackupStorageUnauthorizedException(operation)
            response.statusCode !in 200..299 ->
                throw BackupStorageHttpException(
                    operation = operation,
                    statusCode = response.statusCode,
                    responseBody = response.body.toString(StandardCharsets.UTF_8),
                )
        }
        return response
    }

    private fun parseListPage(body: ByteArray): DriveListPage {
        val json = parseObject(body, operation = "list")
        val filesJson = json.optJSONArray("files")
            ?: throw BackupStorageProtocolException("list", "missing files array")
        val files = buildList {
            for (index in 0 until filesJson.length()) {
                val fileJson = filesJson.optJSONObject(index)
                    ?: throw BackupStorageProtocolException(
                        "list",
                        "files[$index] is not an object",
                    )
                add(parseRemoteFile(fileJson, operation = "list"))
            }
        }
        val nextPageToken = if (json.isNull("nextPageToken")) {
            null
        } else {
            json.optString("nextPageToken").takeIf(String::isNotBlank)
        }
        return DriveListPage(files, nextPageToken)
    }

    private fun parseRemoteFile(body: ByteArray, operation: String): BackupRemoteFile =
        parseRemoteFile(parseObject(body, operation), operation)

    private fun parseRemoteFile(
        json: JSONObject,
        operation: String,
    ): BackupRemoteFile {
        val id = json.optString("id").takeIf(String::isNotBlank)
            ?: throw BackupStorageProtocolException(operation, "missing file id")
        val name = json.optString("name").takeIf(String::isNotBlank)
            ?: throw BackupStorageProtocolException(operation, "missing file name")
        val size = json.optionalString("size")?.toLongOrNull()
        if (size != null && size < 0) {
            throw BackupStorageProtocolException(operation, "file size is negative")
        }
        return BackupRemoteFile(
            id = id,
            name = name,
            sizeBytes = size,
            createdTime = json.optionalString("createdTime"),
            modifiedTime = json.optionalString("modifiedTime"),
        )
    }

    private fun parseObject(body: ByteArray, operation: String): JSONObject =
        try {
            JSONObject(body.toString(StandardCharsets.UTF_8))
        } catch (error: JSONException) {
            throw BackupStorageProtocolException(
                operation = operation,
                message = "body is not valid JSON",
                cause = error,
            )
        }

    private fun buildMultipartBody(
        boundary: String,
        metadata: String,
        content: ByteArray,
    ): ByteArray {
        val output = ByteArrayOutputStream(metadata.length + content.size + 256)
        output.write("--$boundary\r\n".toByteArray(StandardCharsets.UTF_8))
        output.write(
            "Content-Type: application/json; charset=UTF-8\r\n\r\n"
                .toByteArray(StandardCharsets.UTF_8),
        )
        output.write(metadata.toByteArray(StandardCharsets.UTF_8))
        output.write("\r\n--$boundary\r\n".toByteArray(StandardCharsets.UTF_8))
        output.write(
            "Content-Type: application/octet-stream\r\n\r\n"
                .toByteArray(StandardCharsets.UTF_8),
        )
        output.write(content)
        output.write("\r\n--$boundary--\r\n".toByteArray(StandardCharsets.UTF_8))
        return output.toByteArray()
    }

    private fun encodeQuery(parameters: Map<String, String>): String =
        parameters.entries.joinToString("&") { (key, value) ->
            "${percentEncode(key)}=${percentEncode(value)}"
        }

    private fun percentEncode(value: String): String = buildString {
        value.toByteArray(StandardCharsets.UTF_8).forEach { byte ->
            val unsigned = byte.toInt() and 0xff
            val character = unsigned.toChar()
            if (
                character in 'a'..'z' ||
                character in 'A'..'Z' ||
                character in '0'..'9' ||
                character == '-' ||
                character == '.' ||
                character == '_' ||
                character == '~'
            ) {
                append(character)
            } else {
                append('%')
                append(HEX_DIGITS[unsigned ushr 4])
                append(HEX_DIGITS[unsigned and 0x0f])
            }
        }
    }

    private fun JSONObject.optionalString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)

    private data class DriveListPage(
        val files: List<BackupRemoteFile>,
        val nextPageToken: String?,
    )

    companion object {
        private const val DEFAULT_API_BASE_URL = "https://www.googleapis.com/drive/v3"
        private const val DEFAULT_UPLOAD_BASE_URL =
            "https://www.googleapis.com/upload/drive/v3"
        private const val APP_DATA_FOLDER = "appDataFolder"
        private const val PAGE_SIZE = 1_000
        private const val MAX_LIST_PAGES = 100
        private const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 15_000
        private const val DEFAULT_READ_TIMEOUT_MILLIS = 30_000
        private const val DEFAULT_MAX_METADATA_RESPONSE_BYTES = 2 * 1024 * 1024
        private const val HEX_DIGITS = "0123456789ABCDEF"
    }
}
