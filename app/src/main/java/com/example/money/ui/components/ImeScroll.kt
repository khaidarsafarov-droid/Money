package com.example.money.ui.components

import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider

internal fun shouldHideAmountKeypad(textFieldFocused: Boolean, imeBottomPx: Int): Boolean =
    textFieldFocused || imeBottomPx > 0

class TextFocusState {
    var count by mutableIntStateOf(0)
        private set

    val focused: Boolean
        get() = count > 0

    internal fun change(delta: Int) {
        count = (count + delta).coerceAtLeast(0)
    }
}

@Composable
fun rememberTextFocus(): TextFocusState = remember { TextFocusState() }

/**
 * Shrinks the scroll viewport by the IME, then scrolls inside that viewport.
 * Padding has to come before [verticalScroll], otherwise the focused field is
 * treated as visible while it sits under the keyboard.
 */
@Composable
fun Modifier.scrollAboveIme(state: ScrollState): Modifier = imePadding().verticalScroll(state)

/**
 * Scrolls this node fully into the nearest scroll parent whenever it or a child is focused,
 * including after the keyboard inset changes.
 */
@Composable
fun Modifier.bringIntoViewOnFocus(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    var focused by remember { mutableStateOf(false) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(focused, imeBottom, size) {
        if (focused && size != IntSize.Zero) {
            val extra = with(density) { 24.dp.toPx() }
            requester.bringIntoView(
                Rect(0f, 0f, size.width.toFloat(), size.height.toFloat() + extra),
            )
        }
    }
    return this
        .bringIntoViewRequester(requester)
        .onSizeChanged { size = it }
        .onFocusEvent { focused = it.hasFocus }
}

/** Tracks editable text focus and keeps the focused field above the keyboard. */
@Composable
fun Modifier.imeTextField(focus: TextFocusState): Modifier {
    val holder = remember { FocusFlag() }
    DisposableEffect(focus) {
        onDispose {
            if (holder.focused) focus.change(-1)
        }
    }
    return bringIntoViewOnFocus().onFocusEvent { event ->
        val now = event.hasFocus
        if (now == holder.focused) return@onFocusEvent
        focus.change(if (now) 1 else -1)
        holder.focused = now
    }
}

private class FocusFlag {
    var focused: Boolean = false
}

@Composable
fun hideAmountKeypad(textFieldFocused: Boolean): Boolean {
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    return shouldHideAmountKeypad(textFieldFocused, imeBottom)
}

/**
 * Dialog windows default to adjust-nothing or adjust-pan. Ask this one to resize so
 * [WindowInsets.ime] reaches the sheet content.
 */
@Composable
fun DialogImeEffect() {
    val view = LocalView.current
    SideEffect {
        val window = findDialogWindow(view) ?: return@SideEffect
        @Suppress("DEPRECATION")
        val resize = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        val current = window.attributes.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST
        if (current != resize) {
            window.setSoftInputMode(resize)
        }
    }
}

private fun findDialogWindow(view: View): android.view.Window? {
    var current: View? = view
    while (current != null) {
        if (current is DialogWindowProvider) return current.window
        current = current.parent as? View
    }
    return null
}
