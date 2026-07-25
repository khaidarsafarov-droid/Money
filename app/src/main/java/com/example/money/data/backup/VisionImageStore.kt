package com.example.money.data.backup

import android.content.Context
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.domain.backup.BackupValidationException
import com.example.money.domain.backup.BackupVisionImage
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlinx.coroutines.flow.first

interface VisionImageStorage {
    suspend fun readCurrent(): BackupVisionImage?

    suspend fun replaceCurrent(image: BackupVisionImage?)
}

class VisionImageStore(
    context: Context,
    private val preferencesStore: MoneyPreferencesStore,
    private val maxImageBytes: Int = BackupBundleCodec.DEFAULT_MAX_VISION_IMAGE_BYTES,
) : VisionImageStorage {

    private val filesDir = (context.applicationContext ?: context).filesDir

    override suspend fun readCurrent(): BackupVisionImage? {
        val path = preferencesStore.goalPsychologyProfile.first().visionPhotoPath
            ?.takeIf(String::isNotBlank)
            ?: return null
        val file = File(path)
        if (!file.isFile || file.length() <= 0L || file.length() > maxImageBytes) {
            return null
        }
        return BackupVisionImage(
            fileName = file.name,
            mimeType = mimeTypeFor(file.extension),
            base64Data = Base64.encode(file.readBytes()),
        )
    }

    suspend fun writeCurrent(image: BackupVisionImage) {
        validateMetadata(image)
        val bytes = try {
            Base64.decode(image.base64Data)
        } catch (error: IllegalArgumentException) {
            throw BackupValidationException("Vision image is not valid base64")
        }
        if (bytes.isEmpty() || bytes.size > maxImageBytes) {
            throw BackupValidationException("Vision image size is outside the allowed range")
        }

        val previousPath = preferencesStore.goalPsychologyProfile.first().visionPhotoPath
        val rootDirectory = File(filesDir, DIRECTORY_NAME)
        val installDirectory = File(rootDirectory, UUID.randomUUID().toString())
        if (!installDirectory.mkdirs()) {
            throw IOException("Unable to create the vision image directory")
        }
        val temporary = File.createTempFile("vision_", ".tmp", installDirectory)
        val target = File(installDirectory, image.fileName)
        try {
            temporary.outputStream().use { output ->
                output.write(bytes)
                output.flush()
            }
            if (!temporary.renameTo(target)) {
                throw IOException("Unable to install the restored vision image")
            }
            try {
                preferencesStore.updateGoalPsychologyProfile {
                    it.copy(visionPhotoPath = target.absolutePath)
                }
            } catch (error: Exception) {
                target.delete()
                throw error
            }
        } finally {
            temporary.delete()
            if (!target.exists()) {
                installDirectory.delete()
            }
        }
        previousPath
            ?.takeIf { it != target.absolutePath }
            ?.let(::deleteManagedFile)
    }

    suspend fun deleteCurrent() {
        val previousPath = preferencesStore.goalPsychologyProfile.first().visionPhotoPath
        preferencesStore.updateGoalPsychologyProfile { it.copy(visionPhotoPath = null) }
        previousPath?.let(::deleteManagedFile)
    }

    override suspend fun replaceCurrent(image: BackupVisionImage?) {
        if (image == null) {
            deleteCurrent()
        } else {
            writeCurrent(image)
        }
    }

    private fun validateMetadata(image: BackupVisionImage) {
        if (image.fileName.isBlank() ||
            File(image.fileName).name != image.fileName ||
            !image.mimeType.startsWith("image/")
        ) {
            throw BackupValidationException("Vision image metadata is invalid")
        }
    }

    private fun deleteManagedFile(path: String) {
        runCatching {
            val file = File(path)
            val rootPath = filesDir.canonicalFile.path + File.separator
            if (file.canonicalFile.path.startsWith(rootPath) && file.delete()) {
                val visionRoot = File(filesDir, DIRECTORY_NAME).canonicalFile
                file.parentFile?.canonicalFile
                    ?.takeIf { it.parentFile == visionRoot }
                    ?.delete()
            }
        }
    }

    private fun mimeTypeFor(extension: String): String = when (extension.lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        else -> "image/jpeg"
    }

    private companion object {
        const val DIRECTORY_NAME = "vision_images"
    }
}
