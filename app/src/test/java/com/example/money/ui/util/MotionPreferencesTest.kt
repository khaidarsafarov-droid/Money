package com.example.money.ui.util

import android.content.Context
import android.provider.Settings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MotionPreferencesTest {

    @Test
    @Suppress("DEPRECATION")
    fun systemHaptics_followTheSystemToggle() {
        val context: Context = RuntimeEnvironment.getApplication()
        Settings.System.putInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 0)
        assertFalse(systemHapticsEnabled(context))
        Settings.System.putInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1)
        assertTrue(systemHapticsEnabled(context))
    }

    @Test
    fun systemMotion_stopsWhenEitherScaleIsZero() {
        val context: Context = RuntimeEnvironment.getApplication()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        Settings.Global.putFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
        assertFalse(systemMotionEnabled(context))

        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        Settings.Global.putFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
        assertFalse(systemMotionEnabled(context))

        Settings.Global.putFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
        assertTrue(systemMotionEnabled(context))
    }
}
