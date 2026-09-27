package com.example.money.data.backup

import com.example.money.domain.backup.BackupStorageSizeLimitException
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GoogleDriveHttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray? = null,
    val connectTimeoutMillis: Int,
    val readTimeoutMillis: Int,
    val maxResponseBytes: Int,
)

data class GoogleDriveHttpResponse(
    val statusCode: Int,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray = ByteArray(0),
)

fun interface GoogleDriveHttpTransport {
    suspend fun execute(request: GoogleDriveHttpRequest): GoogleDriveHttpResponse
}

class UrlConnectionGoogleDriveHttpTransport(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GoogleDriveHttpTransport {
    override suspend fun execute(
        request: GoogleDriveHttpRequest,
    ): GoogleDriveHttpResponse = withContext(ioDispatcher) {
        require(request.maxResponseBytes > 0) { "maxResponseBytes must be positive" }
        val connection = URL(request.url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = request.method
            connection.connectTimeout = request.connectTimeoutMillis
            connection.readTimeout = request.readTimeoutMillis
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            request.headers.forEach(connection::setRequestProperty)
            request.body?.let { body ->
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(body.size.toLong())
                connection.outputStream.use { it.write(body) }
            }

            val statusCode = connection.responseCode
            val declaredLength = connection.contentLengthLong
            if (declaredLength > request.maxResponseBytes) {
                throw BackupStorageSizeLimitException(
                    maxBytes = request.maxResponseBytes.toLong(),
                    actualBytes = declaredLength,
                )
            }
            val responseStream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseBody = responseStream?.use {
                readBounded(it::read, request.maxResponseBytes)
            } ?: ByteArray(0)
            val responseHeaders = connection.headerFields
                .filterKeys { it != null }
                .mapValues { (_, values) -> values.joinToString(",") }

            GoogleDriveHttpResponse(
                statusCode = statusCode,
                headers = responseHeaders,
                body = responseBody,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun readBounded(
        read: (ByteArray) -> Int,
        maxBytes: Int,
    ): ByteArray {
        val output = ByteArrayOutputStream(minOf(DEFAULT_BUFFER_SIZE, maxBytes))
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var totalBytes = 0L
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            totalBytes += count
            if (totalBytes > maxBytes) {
                throw BackupStorageSizeLimitException(
                    maxBytes = maxBytes.toLong(),
                    actualBytes = totalBytes,
                )
            }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
