package com.example.money.ui.components

import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.money.ui.theme.MoneyTheme
import kotlin.math.roundToInt
import androidx.compose.ui.layout.positionInRoot
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "ru-rRU-xhdpi", application = android.app.Application::class)
class ImeScrollTest {

    @Test
    fun keypadStaysWhileAmountIsEditedAndHidesForTextOrKeyboard() {
        assertTrue(!shouldHideAmountKeypad(textFieldFocused = false, imeBottomPx = 0))
        assertTrue(shouldHideAmountKeypad(textFieldFocused = true, imeBottomPx = 0))
        assertTrue(shouldHideAmountKeypad(textFieldFocused = false, imeBottomPx = 480))
    }

    @Test
    fun focusedFieldScrollsAboveTheImeInset() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        @Suppress("DEPRECATION")
        activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val focusRequester = FocusRequester()
        val placed = intArrayOf(0, 0, 0, 0)
        val imeBottom = intArrayOf(0)
        val scrollValue = intArrayOf(0, 0)
        val focusNow = androidx.compose.runtime.mutableStateOf(false)
        activity.setContent {
            MoneyTheme {
                val density = LocalDensity.current
                val scroll = rememberScrollState()
                imeBottom[0] = WindowInsets.ime.getBottom(density)
                scrollValue[0] = scroll.value
                scrollValue[1] = scroll.viewportSize
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .scrollAboveIme(scroll),
                ) {
                    Spacer(Modifier.height(1800.dp))
                    OutlinedTextField(
                        value = "заметка",
                        onValueChange = {},
                        label = { Text("note") },
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .bringIntoViewOnFocus()
                            .onGloballyPositioned { coords ->
                                val top = coords.positionInRoot()
                                placed[0] = top.y.roundToInt()
                                placed[1] = (top.y + coords.size.height).roundToInt()
                                placed[2] = coords.size.height
                                placed[3] = coords.size.width
                            },
                    )
                    Spacer(Modifier.height(32.dp))
                }
                LaunchedEffect(focusNow.value) {
                    if (focusNow.value) focusRequester.requestFocus()
                }
            }
        }
        ShadowLooper.idleMainLooper()
        val content = activity.findViewById<View>(android.R.id.content)
        val imePx = 700
        val insets = WindowInsetsCompat.Builder()
            .setInsets(
                WindowInsetsCompat.Type.ime(),
                Insets.of(0, 0, 0, imePx),
            )
            .build()
        ViewCompat.dispatchApplyWindowInsets(content, insets)
        (content as? ViewGroup)?.getChildAt(0)?.let { child ->
            ViewCompat.dispatchApplyWindowInsets(child, insets)
        }
        ShadowLooper.idleMainLooper()
        focusNow.value = true
        ShadowLooper.idleMainLooper()
        ShadowLooper.shadowMainLooper().idleFor(Duration.ofSeconds(2))
        ShadowLooper.idleMainLooper()
        val viewport = scrollValue[1]
        // Scroll offset is applied as a motion-frame placement, so positionInRoot omits it.
        val visualTop = placed[0] - scrollValue[0]
        val visualBottom = visualTop + placed[2]
        assertTrue(
            "visualTop=$visualTop visualBottom=$visualBottom h=${placed[2]} " +
                "layoutTop=${placed[0]} scroll=${scrollValue[0]} viewport=$viewport ime=${imeBottom[0]}",
            placed[2] > 0 &&
                viewport <= content.height - imeBottom[0] &&
                visualTop >= 0 &&
                visualBottom <= viewport,
        )
    }
}
