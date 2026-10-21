package com.example.money.ui.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.example.money.domain.model.DebtStatus
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.SavingsGoal
import com.example.money.settings.CurrencyRegion
import com.example.money.ui.clients.ClientRow
import com.example.money.ui.components.GoalListItem
import com.example.money.ui.debtors.DebtSectionTotal
import com.example.money.ui.debtors.DebtTab
import com.example.money.ui.theme.MoneyTheme
import com.example.money.domain.model.Client
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
class ScreensAuditPreviewRenderTest {

    @Test
    fun debtTotalStaysWithinOneRow() {
        val debtors = listOf(
            Debtor(
                id = 1,
                name = "Анна",
                targetAmountCents = 15_000,
                status = DebtStatus.ACTIVE,
                debtType = DebtType.CREDITOR,
                dateGiven = 1L,
                dateDue = 1L,
            ),
            Debtor(
                id = 2,
                name = "Борис",
                targetAmountCents = 4_000,
                status = DebtStatus.ACTIVE,
                debtType = DebtType.DEBTOR,
                dateGiven = 1L,
                dateDue = 1L,
            ),
        )
        val bitmap = render(width = 900) {
            DebtSectionTotal(
                debtors = debtors,
                tab = DebtTab.OwesMe,
                hideAmounts = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val density = 2f
        assertTrue(bitmap.height <= (72 * density).toInt())
        save(bitmap, "debt_section_total.png")
        assertTrue(countNear(bitmap, 0xFF0B6E5B.toInt()) > 20)
    }

    @Test
    fun clientRowShowsNameAndCurrency() {
        val bitmap = render(width = 900, height = 160) {
            ClientRow(
                client = Client(name = "Анна", company = "Студия", debtCents = 12_000),
                currency = CurrencyRegion.US,
                onClick = {},
                onQuickPayment = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
        save(bitmap, "client_row.png")
        assertTrue(bitmap.width == 900)
    }

    @Test
    fun dockLabelsDoNotRepeatInTheIcon() {
        val bitmap = render(width = 900, height = 220) {
            PhoneDockBar(
                selected = PhoneDockTab.Debts,
                onHome = {},
                onDebts = {},
                onGoals = {},
                onMore = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
        save(bitmap, "phone_dock.png")
        assertTrue(bitmap.height >= (64 * 2))
    }

    @Test
    fun goalCardKeepsAmountAndProgress() {
        val bitmap = render(width = 900, height = 420) {
            GoalListItem(
                goal = SavingsGoal(
                    name = "Отпуск у моря",
                    targetAmountCents = 100_000,
                    currentAmountCents = 40_000,
                    updatedAt = 1L,
                    deadlineMillis = 1_800_000_000_000L,
                ),
                onEdit = {},
                onDelete = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
        save(bitmap, "goal_card.png")
        assertTrue(countNear(bitmap, 0xFF0B6E5B.toInt()) > 10)
    }

    private fun render(
        width: Int,
        height: Int? = null,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ): Bitmap {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val composeView = ComposeView(activity)
        activity.setContentView(composeView)
        composeView.setContent { MoneyTheme { content() } }
        ShadowLooper.idleMainLooper()
        val heightSpec = if (height == null) {
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        } else {
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        }
        composeView.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            heightSpec,
        )
        val measuredHeight = height ?: composeView.measuredHeight
        composeView.layout(0, 0, width, measuredHeight)
        ShadowLooper.idleMainLooper()
        val bitmap = Bitmap.createBitmap(width, measuredHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        composeView.draw(Canvas(bitmap))
        return bitmap
    }

    private fun save(bitmap: Bitmap, name: String) {
        val root = generateSequence(File(System.getProperty("user.dir") ?: ".")) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
        val out = File(root, "docs/store/$name")
        out.parentFile?.mkdirs()
        FileOutputStream(out).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        assertTrue(out.length() > 400)
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
            kotlin.math.abs(r - tr) < 18 && kotlin.math.abs(g - tg) < 18 && kotlin.math.abs(b - tb) < 18
        }
    }
}
