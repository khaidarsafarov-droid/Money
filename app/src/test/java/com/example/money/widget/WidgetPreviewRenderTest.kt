package com.example.money.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceRemoteViews
import com.example.money.R
import com.example.money.settings.CurrencyRegion
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.abs
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "ru-rRU-xhdpi",
    application = android.app.Application::class,
)
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalGlanceRemoteViewsApi::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetPreviewRenderTest {

    private val previousLocale = Locale.getDefault()

    @Before
    fun russianCurrency() {
        Locale.setDefault(Locale.forLanguageTag("ru-RU"))
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(previousLocale)
        Dispatchers.resetMain()
    }

    @Test
    fun pickerPreviewLabelsTheGoalInLightAndDark() {
        val light = configuredContext(night = false)
        val lightView = inflatePreview(light)
        assertEquals("My second 100K", text(lightView, R.string.widget_preview_goal_name).text.toString())
        assertEquals("33%", text(lightView, R.string.widget_preview_goal_percent).text.toString())
        assertEquals("33 000 / 100 000 $", text(lightView, R.string.widget_preview_goal_ratio).text.toString())
        assertEquals("+", text(lightView, R.string.widget_plus).text.toString())
        assertEquals(0xFF5FD3B3.toInt(), text(lightView, R.string.widget_preview_goal_percent).currentTextColor)
        assertEquals(0xFF5FD3B3.toInt(), text(lightView, R.string.widget_preview_owes).currentTextColor)
        assertEquals(0xFFF2B8B5.toInt(), text(lightView, R.string.widget_preview_i_owe).currentTextColor)
        val lightBitmap = draw(lightView)

        val dark = configuredContext(night = true)
        val darkView = inflatePreview(dark)
        assertEquals(0xFF5FD3B3.toInt(), text(darkView, R.string.widget_preview_goal_percent).currentTextColor)
        assertEquals(0xFFF2B8B5.toInt(), text(darkView, R.string.widget_preview_i_owe).currentTextColor)
        val darkBitmap = draw(darkView)
        val lightGreen = countNear(lightBitmap, 0xFF0B6E5B.toInt(), tolerance = 48)
        val lightMint = countNear(lightBitmap, 0xFF5FD3B3.toInt())
        val lightPink = countNear(lightBitmap, 0xFFF2B8B5.toInt())
        assertTrue("green pixels=$lightGreen size=${lightBitmap.width}x${lightBitmap.height}", lightGreen > 200)
        assertTrue("mint pixels=$lightMint", lightMint > 20)
        assertTrue("pink pixels=$lightPink", lightPink > 8)
        assertTrue(countNear(darkBitmap, 0xFF0B6E5B.toInt(), tolerance = 48) > 200)
    }

    @Test
    fun glanceHidesTheGoalBlockWhenThereIsNoGoal() {
        val context = configuredContext(night = false)
        val view = renderGlance(context, sample(withGoal = false), 160.dp, 180.dp)
        val labels = texts(view)
        assertTrue(labels.any { it.contains("Мне должны") })
        assertFalse(labels.any { it.contains("%") })
        assertFalse(labels.any { it.contains("Отпуск") })
    }

    @Test
    fun glanceLabelsGoalProgressAndRendersLightDarkAndWide() {
        val snapshot = sample(withGoal = true)
        val stackedLight = renderGlance(configuredContext(night = false), snapshot, 170.dp, 210.dp)
        val stackedDark = renderGlance(configuredContext(night = true), snapshot, 170.dp, 210.dp)
        val wide = renderGlance(configuredContext(night = false), snapshot, 360.dp, 150.dp)

        listOf(stackedLight, stackedDark, wide).forEach { view ->
            val labels = texts(view)
            assertTrue("${view.width}x${view.height} ${labels.joinToString()}", labels.any { it.contains("Отпуск") })
            assertTrue(labels.joinToString(), labels.any { it.contains("33%") })
            assertFalse(labels.joinToString(), labels.any { it.contains("40%") })
            assertTrue(labels.any { it.contains("Мне должны") })
            assertTrue(labels.any { it.contains("Я должен") })
            assertTrue(labels.any { it == "+" })
        }

        val wideOwe = findText(wide, "Я должен")
        val wideOwes = findText(wide, "Мне должны")
        val oweX = offsetIn(wide, wideOwe).first
        val owesX = offsetIn(wide, wideOwes).first
        assertTrue("oweX=$oweX owesX=$owesX wide=${wide.width}", oweX > owesX + 40)

        val wideGoal = findText(wide, "Отпуск")
        val goalY = offsetIn(wide, wideGoal).second
        val owesY = offsetIn(wide, wideOwes).second
        assertTrue("goalY=$goalY owesY=$owesY", goalY > owesY)
        val goalBottom = goalY + wideGoal.height
        assertTrue(
            "goal should sit near the bottom bottom=$goalBottom height=${wide.height}",
            goalBottom > wide.height * 0.55f,
        )

        val stackedGoal = findText(stackedLight, "Отпуск")
        val stackedOwes = findText(stackedLight, "Мне должны")
        assertTrue(offsetIn(stackedLight, stackedGoal).second > offsetIn(stackedLight, stackedOwes).second)

        val card = renderGlance(configuredContext(night = false), designSample(), 340.dp, 168.dp)
        val lightBitmap = draw(stackedLight)
        val darkBitmap = draw(stackedDark)
        val wideBitmap = draw(wide)
        val cardBitmap = draw(card)
        assertTrue(countNear(lightBitmap, 0xFF0B6E5B.toInt(), tolerance = 48) > 80)
        assertTrue(countNear(darkBitmap, 0xFF5FD3B3.toInt()) > 20)
        assertTrue(countNear(cardBitmap, 0xFFF2B8B5.toInt()) > 8)
        val barRun = longestRun(cardBitmap, 0xFF5FD3B3.toInt())
        assertTrue(
            "bar run=$barRun width=${cardBitmap.width}",
            barRun > cardBitmap.width / 10 && barRun < (cardBitmap.width * 0.7f).toInt(),
        )

        val preview = stitch(listOf(cardBitmap, wideBitmap))
        val root = generateSequence(File(System.getProperty("user.dir") ?: ".")) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
        FileOutputStream(File(root, "docs/store/widget_green_card_preview.png")).use { stream ->
            cardBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        val out = File(root, "docs/store/widget_finora_preview.png")
        out.parentFile?.mkdirs()
        FileOutputStream(out).use { stream ->
            preview.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        assertTrue(out.length() > 1_000)
    }

    private fun sample(withGoal: Boolean) = WidgetGoalSnapshot(
        currentAmount = 99.0,
        targetAmount = 300.0,
        progressFraction = 0.4f,
        progressPercent = 40,
        remainingAmount = 201.0,
        pendingCount = 0,
        updatedAt = 1L,
        currencyRegion = CurrencyRegion.US,
        owedToMeCents = 3_375_100L,
        iOweCents = 0L,
        featuredGoalName = if (withGoal) "Отпуск" else "",
        featuredGoalSavedCents = if (withGoal) 990_000L else 0L,
        featuredGoalTargetCents = if (withGoal) 3_000_000L else 0L,
        featuredGoalPercent = if (withGoal) 33 else 0,
    )

    private fun renderGlance(
        context: Context,
        snapshot: WidgetGoalSnapshot,
        width: androidx.compose.ui.unit.Dp,
        height: androidx.compose.ui.unit.Dp,
    ): View = runBlocking {
        val result = GlanceRemoteViews().compose(context, DpSize(width, height)) {
            GoalWidgetContent(context = context, snapshot = snapshot)
        }
        val host = FrameLayout(context)
        val applied = result.remoteViews.apply(context, host)
        host.addView(applied)
        val density = context.resources.displayMetrics.density
        val pxW = (width.value * density).toInt()
        val pxH = (height.value * density).toInt()
        val widthSpec = View.MeasureSpec.makeMeasureSpec(pxW, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(pxH, View.MeasureSpec.EXACTLY)
        host.measure(widthSpec, heightSpec)
        host.layout(0, 0, pxW, pxH)
        host
    }

    private fun designSample() = sample(withGoal = true).copy(
        owedToMeCents = 3_377_920L,
        iOweCents = 23_700L,
        featuredGoalName = "My second 100K",
        featuredGoalSavedCents = 3_300_000L,
        featuredGoalTargetCents = 10_000_000L,
        featuredGoalPercent = 33,
    )

    private fun inflatePreview(context: Context): View {
        val view = android.view.LayoutInflater.from(context).inflate(R.layout.widget_preview, null)
        val density = context.resources.displayMetrics.density
        val width = (340 * density).toInt()
        val height = (156 * density).toInt()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        return view
    }

    private fun configuredContext(night: Boolean): Context {
        val qualifiers = if (night) "ru-rRU-night-xhdpi" else "ru-rRU-xhdpi"
        RuntimeEnvironment.setQualifiers(qualifiers)
        return RuntimeEnvironment.getApplication()
    }

    private fun offsetIn(root: View, target: View): Pair<Int, Int> {
        var x = 0
        var y = 0
        var view: View? = target
        while (view != null && view !== root) {
            x += view.left
            y += view.top
            view = view.parent as? View
        }
        return x to y
    }

    private fun text(root: View, resId: Int): TextView =
        findText(root, root.context.getString(resId))

    private fun findText(root: View, value: String): TextView =
        textsViews(root).first { it.text?.toString()?.contains(value) == true }

    private fun texts(root: View): List<String> = textsViews(root).map { it.text?.toString().orEmpty() }

    private fun textsViews(root: View): List<TextView> {
        val out = mutableListOf<TextView>()
        fun walk(view: View) {
            if (view is TextView) out += view
            if (view is ViewGroup) {
                for (index in 0 until view.childCount) walk(view.getChildAt(index))
            }
        }
        walk(root)
        return out
    }

    private fun draw(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    private fun longestRun(bitmap: Bitmap, target: Int): Int {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var best = 0
        for (y in 0 until bitmap.height) {
            var run = 0
            val row = y * bitmap.width
            for (x in 0 until bitmap.width) {
                if (near(pixels[row + x], target, 36)) {
                    run++
                    if (run > best) best = run
                } else {
                    run = 0
                }
            }
        }
        return best
    }

    private fun countNear(bitmap: Bitmap, target: Int, tolerance: Int = 28): Int {
        var count = 0
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (pixel in pixels) {
            if (near(pixel, target, tolerance)) count++
        }
        return count
    }

    private fun near(pixel: Int, target: Int, tolerance: Int): Boolean {
        val alpha = pixel ushr 24
        if (alpha < 200) return false
        return channel(pixel, 16, target) <= tolerance &&
            channel(pixel, 8, target) <= tolerance &&
            channel(pixel, 0, target) <= tolerance
    }

    private fun channel(pixel: Int, shift: Int, target: Int): Int =
        abs(((pixel shr shift) and 0xFF) - ((target shr shift) and 0xFF))

    private fun stitch(bitmaps: List<Bitmap>): Bitmap {
        val gap = 24
        val width = bitmaps.sumOf { it.width } + gap * (bitmaps.size + 1)
        val height = bitmaps.maxOf { it.height } + gap * 2
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(0xFFE8EEEB.toInt())
        var x = gap
        for (bitmap in bitmaps) {
            canvas.drawBitmap(bitmap, x.toFloat(), gap.toFloat(), null)
            x += bitmap.width + gap
        }
        return out
    }
}
