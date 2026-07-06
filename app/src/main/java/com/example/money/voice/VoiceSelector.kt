package com.example.money.voice

import java.util.Locale

/** Pure selection logic — testable without a live TTS engine. */
object VoiceSelector {

    data class VoiceCandidate(
        val name: String,
        val localeLanguage: String?,
        val features: Set<String>,
    )

    /**
     * Picks a voice matching locale and gender. Returns null when no gender match exists
     * so the engine keeps its locale default instead of switching to the opposite gender.
     */
    fun pickBest(
        candidates: List<VoiceCandidate>,
        locale: Locale,
        gender: VoiceGender,
    ): VoiceCandidate? {
        if (candidates.isEmpty()) return null

        val localeMatches = candidates.filter { candidate ->
            candidate.localeLanguage.equals(locale.language, ignoreCase = true)
        }
        val pool = localeMatches.ifEmpty { candidates }

        return pool
            .mapNotNull { candidate -> inferGender(candidate)?.let { inferred -> candidate to inferred } }
            .filter { (_, inferred) -> inferred == gender }
            .map { (candidate, _) -> candidate }
            .sortedWith(voicePreferenceComparator())
            .firstOrNull()
    }

    internal fun matchesGender(candidate: VoiceCandidate, gender: VoiceGender): Boolean {
        return inferGender(candidate) == gender
    }

    internal fun inferGender(candidate: VoiceCandidate): VoiceGender? {
        val name = candidate.name.lowercase(Locale.US)
        val featureText = candidate.features.joinToString(" ").lowercase(Locale.US)
        val haystack = "$name $featureText"

        val explicitTag = parseExplicitGenderTag(name)
        if (explicitTag != null) return explicitTag

        val voiceCode = extractVoiceCode(name)
        if (voiceCode != null) {
            when {
                voiceCode in FEMALE_VOICE_CODES -> return VoiceGender.FEMALE
                voiceCode in MALE_VOICE_CODES -> return VoiceGender.MALE
            }
        }

        val isFemale = FEMALE_HINTS.any { hint -> containsHint(haystack, hint) }
        val isMale = MALE_HINTS.any { hint -> containsHint(haystack, hint) }

        return when {
            isFemale && !isMale -> VoiceGender.FEMALE
            isMale && !isFemale -> VoiceGender.MALE
            else -> null
        }
    }

    private fun parseExplicitGenderTag(name: String): VoiceGender? {
        val normalized = name.lowercase(Locale.US)
        val hashSegment = normalized.substringAfter('#', "")
        val segments = listOf(normalized, hashSegment)

        val hasFemale = segments.any { segment ->
            FEMALE_TAG_PATTERN.containsMatchIn(segment)
        }
        val hasMale = segments.any { segment ->
            MALE_TAG_PATTERN.containsMatchIn(segment)
        }

        return when {
            hasFemale && !hasMale -> VoiceGender.FEMALE
            hasMale && !hasFemale -> VoiceGender.MALE
            else -> null
        }
    }

    private fun extractVoiceCode(name: String): String? {
        return VOICE_CODE_PATTERN.find(name.lowercase(Locale.US))?.groupValues?.get(1)
    }

    private fun voicePreferenceComparator(): Comparator<VoiceCandidate> {
        return compareBy<VoiceCandidate> { candidate ->
            when {
                candidate.name.contains("-local", ignoreCase = true) -> 0
                candidate.name.contains("-network", ignoreCase = true) -> 1
                else -> 2
            }
        }.thenBy { it.name }
    }

    private fun containsHint(haystack: String, hint: String): Boolean {
        if (hint in WORD_BOUNDARY_HINTS) {
            return Regex("\\b${Regex.escape(hint)}\\b").containsMatchIn(haystack)
        }
        return haystack.contains(hint)
    }

    private val WORD_BOUNDARY_HINTS = setOf("male", "man", "men", "fem")

    private val FEMALE_TAG_PATTERN = Regex("""(?:^|[-_#])female(?:[_\d-]|$)""")
    private val MALE_TAG_PATTERN = Regex("""(?:^|[-_#])male(?:[_\d-]|$)""")
    private val VOICE_CODE_PATTERN = Regex("""-x-([a-z0-9]{3})""")

    /** Google TTS 3-letter voice codes observed on Android devices. */
    private val FEMALE_VOICE_CODES = setOf(
        "sfg", "sfb", "sfc", "sfd", "sfe",
        "ruf", "rff",
        "fis", "nfh", "ana", "vlf", "kda", "jmn", "ism", "kod",
        "frd", "gbb", "iob",
    )

    private val MALE_VOICE_CODES = setOf(
        "sfm", "sfn",
        "rmf", "rum", "rmd", "rud",
        "rjs", "gbc", "waw",
    )

    private val FEMALE_HINTS = listOf(
        "female",
        "woman",
        "women",
        "жен",
        "fem",
        "sfg",
        "sfb",
        "sfc",
        "sfd",
        "-ruf",
        "-rff",
        "ruf-local",
        "rff-local",
    )

    private val MALE_HINTS = listOf(
        "male",
        "man",
        "men",
        "муж",
        "masc",
        "sfm",
        "sfn",
        "-rmf",
        "-rum",
        "-rmd",
        "-rud",
        "rmf-local",
        "rum-local",
        "rmd-local",
        "rud-local",
    )
}
