package com.example.money.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.money.domain.util.DebtPhotoParser
import com.example.money.domain.util.ParsedDebtLine
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DebtPhotoFiles {
    fun newCapture(context: Context): File {
        val dir = File(context.cacheDir, "debt_photos").apply { mkdirs() }
        return File(dir, "cam-${UUID.randomUUID()}.jpg").apply { createNewFile() }
    }

    fun copyUri(context: Context, uri: Uri): File? {
        val dir = File(context.cacheDir, "debt_photos").apply { mkdirs() }
        val file = File(dir, "pick-${UUID.randomUUID()}.jpg")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { input.copyTo(it) }
            } ?: return null
            file
        } catch (_: Exception) {
            file.delete()
            null
        }
    }
}

/**
 * On-device Tesseract (rus+eng). Temporary image files are deleted by the caller.
 */
object DebtPhotoOcr {
    suspend fun recognize(context: Context, images: List<File>, now: Long = System.currentTimeMillis()): List<ParsedDebtLine> =
        withContext(Dispatchers.Default) {
            val lines = mutableListOf<Pair<String, Int>>()
            for (image in images) {
                try {
                    lines += readLines(context, image)
                } catch (_: Exception) {
                    // Skip a photo that Tesseract cannot read. The file is still deleted.
                }
            }
            DebtPhotoParser.parseLines(lines, now)
        }

    private fun readLines(context: Context, image: File): List<Pair<String, Int>> {
        val bitmap = decode(image) ?: return emptyList()
        val api = TessBaseAPI()
        try {
            val dataPath = ensureData(context)
            if (!api.init(dataPath, "rus+eng")) return emptyList()
            api.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
            api.setImage(bitmap)
            api.utF8Text
            return lineConfidences(api).ifEmpty {
                api.utF8Text.orEmpty().lines().map { it to 100 }
            }
        } finally {
            api.recycle()
            bitmap.recycle()
        }
    }

    private fun lineConfidences(api: TessBaseAPI): List<Pair<String, Int>> {
        val iterator = api.resultIterator ?: return emptyList()
        val lines = mutableListOf<Pair<String, Int>>()
        try {
            iterator.begin()
            do {
                if (iterator.isAtBeginningOf(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)) {
                    val text = iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)
                        .orEmpty()
                        .trim()
                    if (text.isNotEmpty()) {
                        val confidence = iterator.confidence(
                            TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE,
                        ).toInt().coerceIn(0, 100)
                        lines += text to confidence
                    }
                }
            } while (iterator.next(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE))
        } catch (_: Exception) {
            return emptyList()
        } finally {
            iterator.delete()
        }
        return lines
    }

    private fun decode(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        val longest = max(bounds.outWidth, bounds.outHeight)
        while (longest / sample > 1800) sample *= 2
        return BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample },
        )
    }

    private fun ensureData(context: Context): String {
        val root = File(context.filesDir, "tesseract")
        val dir = File(root, "tessdata").apply { mkdirs() }
        for (language in listOf("rus", "eng")) {
            val out = File(dir, "$language.traineddata")
            if (out.length() > 0L) continue
            context.assets.open("tessdata/$language.traineddata").use { input ->
                out.outputStream().use { input.copyTo(it) }
            }
        }
        return root.absolutePath
    }
}
