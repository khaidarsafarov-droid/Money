package com.example.money.ui.onboarding

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingPsychotypeDraftTest {

    @Test
    fun psychotypeAnswers_defaultToUnanswered() {
        val draft = OnboardingDraft()
        assertTrue(draft.psychotypeAnswers.all { it == null })
        assertNull(draft.psychotypeResult)
        assertFalse(draft.showPsychotypeResult)
    }
}
