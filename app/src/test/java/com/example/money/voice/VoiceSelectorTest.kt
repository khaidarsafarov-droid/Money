package com.example.money.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class VoiceSelectorTest {

    @Test
    fun pickBest_prefersLocaleAndGender() {
        val candidates = listOf(
            VoiceSelector.VoiceCandidate("en-us-x-sfg-local", "en", setOf("female")),
            VoiceSelector.VoiceCandidate("ru-ru-x-rmf-local", "ru", setOf("male")),
            VoiceSelector.VoiceCandidate("ru-ru-x-rff-local", "ru", setOf("female")),
        )

        val picked = VoiceSelector.pickBest(candidates, Locale("ru", "RU"), VoiceGender.FEMALE)

        assertNotNull(picked)
        assertEquals("ru-ru-x-rff-local", picked?.name)
    }

    @Test
    fun pickBest_selectsMaleRussianVoice() {
        val candidates = listOf(
            VoiceSelector.VoiceCandidate("ru-ru-x-ruf-local", "ru", emptySet()),
            VoiceSelector.VoiceCandidate("ru-ru-x-rum-local", "ru", emptySet()),
        )

        val picked = VoiceSelector.pickBest(candidates, Locale("ru", "RU"), VoiceGender.MALE)

        assertNotNull(picked)
        assertEquals("ru-ru-x-rum-local", picked?.name)
    }

    @Test
    fun pickBest_parsesHashGenderTag() {
        val candidates = listOf(
            VoiceSelector.VoiceCandidate("ru-ru-x-ruf#female_1-local", "ru", emptySet()),
            VoiceSelector.VoiceCandidate("ru-ru-x-rum#male_1-local", "ru", emptySet()),
        )

        val female = VoiceSelector.pickBest(candidates, Locale("ru", "RU"), VoiceGender.FEMALE)
        val male = VoiceSelector.pickBest(candidates, Locale("ru", "RU"), VoiceGender.MALE)

        assertEquals("ru-ru-x-ruf#female_1-local", female?.name)
        assertEquals("ru-ru-x-rum#male_1-local", male?.name)
    }

    @Test
    fun pickBest_returnsNullWhenGenderUnavailable() {
        val candidates = listOf(
            VoiceSelector.VoiceCandidate("en-us-x-sfg-local", "en", emptySet()),
            VoiceSelector.VoiceCandidate("ru-ru-x-rmf-local", "ru", emptySet()),
        )

        val picked = VoiceSelector.pickBest(candidates, Locale("ru", "RU"), VoiceGender.FEMALE)

        assertNull(picked)
    }

    @Test
    fun pickBest_returnsNullForEmptyList() {
        assertNull(VoiceSelector.pickBest(emptyList(), Locale.US, VoiceGender.MALE))
    }

    @Test
    fun matchesGender_rejectsAmbiguousVoice() {
        val candidate = VoiceSelector.VoiceCandidate(
            "test-voice",
            "en",
            setOf("male", "female"),
        )
        assertEquals(false, VoiceSelector.matchesGender(candidate, VoiceGender.MALE))
        assertEquals(false, VoiceSelector.matchesGender(candidate, VoiceGender.FEMALE))
    }

    @Test
    fun matchesGender_detectsMaleHints() {
        val candidate = VoiceSelector.VoiceCandidate("ru-ru-x-rmf-local", "ru", setOf("male"))
        assertEquals(true, VoiceSelector.matchesGender(candidate, VoiceGender.MALE))
        assertEquals(false, VoiceSelector.matchesGender(candidate, VoiceGender.FEMALE))
    }

    @Test
    fun inferGender_detectsEnglishMaleAndFemaleCodes() {
        val female = VoiceSelector.VoiceCandidate("en-us-x-sfg-local", "en", emptySet())
        val male = VoiceSelector.VoiceCandidate("en-us-x-sfm-local", "en", emptySet())

        assertEquals(VoiceGender.FEMALE, VoiceSelector.inferGender(female))
        assertEquals(VoiceGender.MALE, VoiceSelector.inferGender(male))
    }
}
