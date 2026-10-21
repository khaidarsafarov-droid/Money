package com.example.money.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

private val avatarPalette = listOf(
    Color(0xFFE2725B),
    Color(0xFF4C8DF6),
    Color(0xFFF2A93B),
    Color(0xFF8E6CEF),
    Color(0xFF2FA884),
    Color(0xFFE85D9A),
    Color(0xFF3BB3C3),
)

private val genericNameWords = setOf(
    "магазин", "кафе", "ип", "ооо", "аптека", "пекарня", "shop", "cafe",
)

private val goalGradients = listOf(
    listOf(Color(0xFF7FD1B9), Color(0xFF2FA884)),
    listOf(Color(0xFF8FE0E8), Color(0xFF3B8FC3)),
    listOf(Color(0xFFF7C98B), Color(0xFFE2725B)),
    listOf(Color(0xFF9DB8FF), Color(0xFF5B6CEF)),
)

fun finoraAvatarColor(name: String): Color {
    val index = abs(name.lowercase().hashCode()) % avatarPalette.size
    return avatarPalette[index]
}

fun finoraInitials(name: String): String {
    val words = name
        .replace('«', ' ')
        .replace('»', ' ')
        .lowercase()
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.isNotBlank() }
    if (words.isEmpty()) return "?"
    val meaningful = words.firstOrNull { it !in genericNameWords } ?: words.first()
    val shop = words.any { it in genericNameWords } || meaningful.length > 8
    val count = if (shop) 2 else 1
    val slice = meaningful.take(count)
    return slice.replaceFirstChar { it.titlecase() }
}

fun finoraGoalBrush(seed: Int): Brush {
    val colors = goalGradients[abs(seed) % goalGradients.size]
    return Brush.linearGradient(colors)
}

@Composable
fun finoraBalanceBrush(): Brush {
    val colors = if (isSystemInDarkTheme()) {
        listOf(Color(0xFF0A4F42), Color(0xFF0D6553), Color(0xFF178A6F))
    } else {
        listOf(Color(0xFF0B6E5B), Color(0xFF13896F), Color(0xFF2FA884))
    }
    return Brush.linearGradient(colors)
}
