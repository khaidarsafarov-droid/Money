package com.example.money.voice

/**
 * Prepares report text for TTS so currency symbols, grouping separators,
 * and trailing decimals are not read as separate digits (e.g. "zero zero").
 */
object VoiceReportSpeechFormatter {

    fun formatAmountForSpeech(cents: Long, language: VoiceLanguage): String {
        val whole = cents / 100
        val fraction = cents % 100
        return when (language) {
            VoiceLanguage.EN -> if (fraction == 0L) {
                "$whole dollars"
            } else {
                "$whole dollars and $fraction cents"
            }
            VoiceLanguage.RU -> if (fraction == 0L) {
                "$whole долларов"
            } else {
                "$whole долларов $fraction центов"
            }
        }
    }

    fun prepareForSpeech(text: String, language: VoiceLanguage): String {
        var result = text

        // UI-style currency leftovers: $5,000.00 / $5,000 / 5,000.00
        result = CURRENCY_WITH_SYMBOL.replace(result) { match ->
            val digits = match.groupValues[1].replace(",", "").replace(" ", "")
            val amountCents = parseDisplayAmountToCents(digits) ?: return@replace match.value
            formatAmountForSpeech(amountCents, language)
        }

        // Bare grouped numbers that may still carry separators or decimals
        result = GROUPED_NUMBER.replace(result) { match ->
            val raw = match.value
            if (raw.contains('.') || raw.contains(',')) {
                val normalized = raw.replace(",", "").replace(".00", "")
                normalized
            } else {
                raw
            }
        }

        result = result
            .replace("$", "")
            .replace(Regex("""\.00(?=\D|$)"""), "")
            .replace(Regex(""",00(?=\D|$)"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()

        return result
    }

    private fun parseDisplayAmountToCents(digits: String): Long? {
        val parts = digits.split('.')
        if (parts.size > 2) return null
        val whole = parts[0].toLongOrNull() ?: return null
        val fraction = when (parts.size) {
            1 -> 0L
            else -> {
                val frac = parts[1]
                when (frac.length) {
                    0 -> 0L
                    1 -> (frac.toLongOrNull() ?: return null) * 10
                    2 -> frac.toLongOrNull() ?: return null
                    else -> return null
                }
            }
        }
        return whole * 100 + fraction
    }

    private val CURRENCY_WITH_SYMBOL = Regex("""\$\s*([\d,]+(?:\.\d{1,2})?)""")
    private val GROUPED_NUMBER = Regex("""\b\d{1,3}(?:,\d{3})+(?:\.\d{1,2})?\b""")
}
