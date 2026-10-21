package com.example.money.ui.theme

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import com.example.money.ui.util.MoneyHaptic
import com.example.money.ui.util.hapticConstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyMotionTest {
    @Test
    fun pressScale_isALightShrink() {
        assertEquals(0.97f, MoneyMotion.pressScale)
    }

    @Test
    fun spring_snapsWhenMotionIsOff() {
        assertTrue(MoneyMotion.springSpec<Float>(false) is SnapSpec)
        assertTrue(MoneyMotion.pressSpec<Float>(false) is SnapSpec)
    }

    @Test
    fun spring_isUsedWhenMotionIsOn() {
        assertTrue(MoneyMotion.springSpec<Float>(true) is SpringSpec)
        assertTrue(MoneyMotion.pressSpec<Float>(true) is SpringSpec)
        assertTrue(MoneyMotion.sheetSpec<Float>(true) is SpringSpec)
    }

    @Test
    fun durations_stayInsideMaterialShortAndMedium() {
        assertTrue(MoneyMotion.short in 50..200)
        assertTrue(MoneyMotion.medium in 200..300)
        assertTrue(MoneyMotion.long in 300..450)
        assertTrue(MoneyMotion.fadeOut <= 120)
        assertTrue(MoneyMotion.fadeIn <= 200)
    }

    @Test
    fun springs_doNotBounce() {
        val progress = MoneyMotion.springSpec<Float>(true) as SpringSpec
        val press = MoneyMotion.pressSpec<Float>(true) as SpringSpec
        val sheet = MoneyMotion.sheetSpec<Float>(true) as SpringSpec
        assertTrue(progress.dampingRatio >= 1f)
        assertTrue(press.dampingRatio >= 1f)
        assertTrue(sheet.dampingRatio >= 1f)
        assertTrue(progress.stiffness >= Spring.StiffnessMedium)
        assertTrue(press.stiffness >= Spring.StiffnessMedium)
        assertTrue(sheet.stiffness >= 400f)
    }

    @Test
    fun haptics_matchPlatformConstants() {
        assertEquals(
            HapticFeedbackConstants.CONFIRM,
            hapticConstant(MoneyHaptic.Confirm, Build.VERSION_CODES.R),
        )
        assertEquals(
            HapticFeedbackConstants.VIRTUAL_KEY,
            hapticConstant(MoneyHaptic.Confirm, Build.VERSION_CODES.N),
        )
        assertEquals(
            HapticFeedbackConstants.REJECT,
            hapticConstant(MoneyHaptic.Reject, Build.VERSION_CODES.R),
        )
        assertEquals(
            HapticFeedbackConstants.CLOCK_TICK,
            hapticConstant(MoneyHaptic.ClockTick, Build.VERSION_CODES.N),
        )
        assertEquals(
            HapticFeedbackConstants.LONG_PRESS,
            hapticConstant(MoneyHaptic.LongPress, Build.VERSION_CODES.N),
        )
    }
}
