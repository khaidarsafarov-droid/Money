package com.example.money.ui.home

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class OnboardingPreferencesTest {

    @Test
    fun completedFlag_survivesANewReadAndClears() {
        val context: Context = RuntimeEnvironment.getApplication()
        OnboardingPreferences.clear(context)
        assertFalse(OnboardingPreferences.isCompleted(context))

        OnboardingPreferences.markCompleted(context)
        assertTrue(OnboardingPreferences.isCompleted(context))

        OnboardingPreferences.clear(context)
        assertFalse(OnboardingPreferences.isCompleted(context))
    }
}
