package com.example.money.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class PaletteContrastTest {

    @Test
    fun balanceGradientEndsKeepWhiteReadable() {
        val stops = listOf(0xFF0B6E5B.toInt(), 0xFF14786E.toInt(), 0xFF0A4F42.toInt(), 0xFF0F6A58.toInt())
        stops.forEach { stop ->
            val blended = composite(0xFFFFFFFF.toInt(), stop, 0.9)
            assertTrue("stop=${stop.toUInt().toString(16)}", contrast(blended, stop) >= 4.5)
        }
    }

    @Test
    fun avatarInitialsMeetTextContrast() {
        assertTrue(contrast(0xFF111614.toInt(), 0xFFE4E9E6.toInt()) >= 4.5)
        assertTrue(contrast(0xFFE8EEEB.toInt(), 0xFF262D2A.toInt()) >= 4.5)
    }

    private fun contrast(foreground: Int, background: Int): Double {
        val lighter = maxOf(luminance(foreground), luminance(background))
        val darker = minOf(luminance(foreground), luminance(background))
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun composite(foreground: Int, background: Int, alpha: Double): Int {
        fun channel(fg: Int, bg: Int): Int = (fg * alpha + bg * (1.0 - alpha)).toInt()
        val r = channel((foreground shr 16) and 0xFF, (background shr 16) and 0xFF)
        val g = channel((foreground shr 8) and 0xFF, (background shr 8) and 0xFF)
        val b = channel(foreground and 0xFF, background and 0xFF)
        return (r shl 16) or (g shl 8) or b
    }

    private fun luminance(color: Int): Double {
        fun channel(value: Int): Double {
            val s = value / 255.0
            return if (s <= 0.04045) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        val r = channel((color shr 16) and 0xFF)
        val g = channel((color shr 8) and 0xFF)
        val b = channel(color and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
}
