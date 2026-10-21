package com.example.money.ui.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import com.example.money.ui.theme.MoneyTheme
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "ru-rRU-xhdpi",
    application = android.app.Application::class,
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeStatusPreviewRenderTest {

    @Test
    fun statusStripShowsOverdueAndToday() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            MoneyTheme {
                HomeStatusStrip(
                    overdueCount = 2,
                    overdueCents = 15_000L,
                    todayCount = 1,
                    weekCount = 3,
                    hideAmounts = false,
                    onOverdue = {},
                    onDueSoon = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        ShadowLooper.idleMainLooper()
        val view = activity.window.decorView
        val width = 900
        val height = 160
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, width, height)
        ShadowLooper.idleMainLooper()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val root = generateSequence(File(System.getProperty("user.dir") ?: ".")) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
        val out = File(root, "docs/store/home_status_strip.png")
        out.parentFile?.mkdirs()
        FileOutputStream(out).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        assertTrue(out.length() > 500)
        assertTrue(countNear(bitmap, 0xFFB3261E.toInt()) > 10)
    }

    private fun countNear(bitmap: Bitmap, color: Int): Int {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val tr = (color shr 16) and 0xFF
        val tg = (color shr 8) and 0xFF
        val tb = color and 0xFF
        return pixels.count { pixel ->
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            kotlin.math.abs(r - tr) + kotlin.math.abs(g - tg) + kotlin.math.abs(b - tb) <= 48
        }
    }
}
