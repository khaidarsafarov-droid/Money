package com.example.money.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object WindowSize {
    /** Material compact / medium breakpoint — tablet and foldable inner display. */
    val TabletMinWidth: Dp = 600.dp
}

@Composable
fun isTabletWidth(width: Dp): Boolean = width >= WindowSize.TabletMinWidth
