package com.example.money.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
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
class SwipePaidPreviewRenderTest {

    @Test
    fun armedBackdropIsGreenWithAWhiteCheck() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            MoneyTheme {
                SwipePaidBackdrop(
                    armed = true,
                    progress = 1f,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        ShadowLooper.idleMainLooper()
        val view = activity.window.decorView
        val width = 800
        val height = 200
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, width, height)
        ShadowLooper.idleMainLooper()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val green = countNear(bitmap, 0xFF0B6E5B.toInt())
        val whiteXs = whiteXs(bitmap)
        val white = whiteXs.size
        val root = generateSequence(File(System.getProperty("user.dir") ?: ".")) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
        val out = File(root, "docs/store/swipe_paid_backdrop.png")
        out.parentFile?.mkdirs()
        FileOutputStream(out).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        assertTrue("green=$green white=$white", green > bitmap.width * bitmap.height / 2 && white > 20)
        assertTrue(
            "check should sit on the trailing side, xs=${whiteXs.minOrNull()}..${whiteXs.maxOrNull()}",
            whiteXs.isNotEmpty() && whiteXs.minOrNull()!! > bitmap.width / 2,
        )
        assertTrue(out.length() > 500)
    }

    private fun whiteXs(bitmap: Bitmap): List<Int> {
        val xs = mutableListOf<Int>()
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                if (r > 220 && g > 220 && b > 220) xs.add(x)
            }
        }
        return xs
    }

    private fun countNear(bitmap: Bitmap, color: Int): Int {
        var count = 0
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val tr = (color shr 16) and 0xFF
        val tg = (color shr 8) and 0xFF
        val tb = color and 0xFF
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            if (kotlin.math.abs(r - tr) + kotlin.math.abs(g - tg) + kotlin.math.abs(b - tb) <= 36) {
                count++
            }
        }
        return count
    }
}
