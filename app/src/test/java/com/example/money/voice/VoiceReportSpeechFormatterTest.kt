package com.example.money.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VoiceReportSpeechFormatterTest {

    @Test
    fun formatAmountForSpeech_wholeDollars_english() {
        assertEquals(
            "5000 dollars",
            VoiceReportSpeechFormatter.formatAmountForSpeech(500_000, VoiceLanguage.EN),
        )
    }

    @Test
    fun formatAmountForSpeech_withCents_english() {
        assertEquals(
            "5000 dollars and 50 cents",
            VoiceReportSpeechFormatter.formatAmountForSpeech(500_050, VoiceLanguage.EN),
        )
    }

    @Test
    fun formatAmountForSpeech_wholeDollars_russian() {
        assertEquals(
            "5000 долларов",
            VoiceReportSpeechFormatter.formatAmountForSpeech(500_000, VoiceLanguage.RU),
        )
    }

    @Test
    fun prepareForSpeech_stripsCurrencyFormatting() {
        val raw = "Hello. Saved $5,000.00 of $10,000.00. Remaining $5,000.00."
        val cleaned = VoiceReportSpeechFormatter.prepareForSpeech(raw, VoiceLanguage.EN)

        assertFalse(cleaned.contains(","))
        assertFalse(cleaned.contains(".00"))
        assertFalse(cleaned.contains("$"))
        assertEquals(
            "Hello. Saved 5000 dollars of 10000 dollars. Remaining 5000 dollars.",
            cleaned,
        )
    }

    @Test
    fun prepareForSpeech_removesGroupedNumberSeparators() {
        val cleaned = VoiceReportSpeechFormatter.prepareForSpeech(
            "Progress 85. Saved 1,250 of 5,000.",
            VoiceLanguage.EN,
        )

        assertEquals("Progress 85. Saved 1250 of 5000.", cleaned)
    }
}
