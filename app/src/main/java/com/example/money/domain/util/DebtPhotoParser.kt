package com.example.money.domain.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.Locale

data class ParsedDebtLine(
    val name: String,
    val amountCents: Long?,
    val dueAt: Long?,
    val phone: String,
    val nameUnsure: Boolean,
    val amountUnsure: Boolean,
    val dateUnsure: Boolean,
) {
    val selectedByDefault: Boolean
        get() = amountCents != null && name.isNotBlank() && !nameUnsure && !amountUnsure
}

data class ImportedDebt(
    val name: String,
    val amountCents: Long,
    val dateDue: Long,
    val phone: String,
)

/**
 * Turns OCR lines from a notebook or spreadsheet photo into debt rows.
 * Dates are stored at noon on the parsed day.
 */
object DebtPhotoParser {

    private val phoneRegex = Regex("""(?<!\d)\+?\d[\d\s\-()]{6,}\d(?!\d)""")
    private val numericDate = Regex("""(?<!\d)(\d{1,2})[./](\d{1,2})(?:[./](\d{2,4}))?(?!\d)""")
    private val dayMonth = Regex(
        """(?i)(?<!\d)(\d{1,2})\s+(января|январь|янв|февраля|февраль|фев|марта|март|мар|апреля|апрель|апр|мая|май|июня|июнь|июн|июля|июль|июл|августа|август|авг|сентября|сентябрь|сен|октября|октябрь|окт|ноября|ноябрь|ноя|декабря|декабрь|дек|january|jan|february|feb|march|mar|april|apr|may|june|jun|july|jul|august|aug|september|sep|sept|october|oct|november|nov|december|dec)(?:\s+(\d{4}))?""",
    )
    private val monthDay = Regex(
        """(?i)(january|jan|february|feb|march|mar|april|apr|may|june|jun|july|jul|august|aug|september|sep|sept|october|oct|november|nov|december|dec)\s+(\d{1,2})(?:,)?(?:\s+(\d{4}))?""",
    )
    private val relativeDate = Regex("""(?i)(?<!\p{L})(сегодня|завтра|today|tomorrow)(?!\p{L})""")
    private val amountRegex = Regex(
        """(?i)(?:\$|usd|uzs|сум\.?|so'm|som|руб\.?|₽|rub)?\s*(\d{1,3}(?:[ \u00A0]\d{3})+(?:[.,]\d{1,2})?|\d{1,3}(?:\.\d{3})+(?:,\d{1,2})?|\d{1,3}(?:,\d{3})+(?:\.\d{1,2})?|\d+[.,]\d{1,2}|\d+)\s*(?:\$|usd|uzs|сум\.?|so'm|som|руб\.?|₽|rub)?""",
    )
    private val currencyHint = Regex("""(?i)\$|usd|uzs|сум|so'm|som|руб|₽|rub""")
    private val headers = setOf(
        "имя", "магазин", "клиент", "название", "фио", "name", "shop", "client",
        "сумма", "долг", "amount", "sum", "дата", "срок", "date", "due", "телефон", "phone", "тел",
    )
    private val months = mapOf(
        "января" to 1, "январь" to 1, "янв" to 1,
        "февраля" to 2, "февраль" to 2, "фев" to 2,
        "марта" to 3, "март" to 3, "мар" to 3,
        "апреля" to 4, "апрель" to 4, "апр" to 4,
        "мая" to 5, "май" to 5,
        "июня" to 6, "июнь" to 6, "июн" to 6,
        "июля" to 7, "июль" to 7, "июл" to 7,
        "августа" to 8, "август" to 8, "авг" to 8,
        "сентября" to 9, "сентябрь" to 9, "сен" to 9,
        "октября" to 10, "октябрь" to 10, "окт" to 10,
        "ноября" to 11, "ноябрь" to 11, "ноя" to 11,
        "декабря" to 12, "декабрь" to 12, "дек" to 12,
        "january" to 1, "jan" to 1,
        "february" to 2, "feb" to 2,
        "march" to 3, "mar" to 3,
        "april" to 4, "apr" to 4,
        "may" to 5,
        "june" to 6, "jun" to 6,
        "july" to 7, "jul" to 7,
        "august" to 8, "aug" to 8,
        "september" to 9, "sep" to 9, "sept" to 9,
        "october" to 10, "oct" to 10,
        "november" to 11, "nov" to 11,
        "december" to 12, "dec" to 12,
    )

    fun parse(text: String, now: Long): List<ParsedDebtLine> =
        parseLines(text.lines().map { it to 100 }, now)

    fun parseLines(lines: List<Pair<String, Int>>, now: Long): List<ParsedDebtLine> =
        lines.mapNotNull { (line, confidence) -> parseLine(line, confidence, now) }

    fun parseAmount(text: String): Long? = parseMoneyToken(text.trim())

    fun parseDue(text: String, now: Long): Long? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val relative = relativeDate.find(trimmed)
        if (relative != null) return relativeDue(relative.value, now)
        numericDate.find(trimmed)?.let { match ->
            return dateOrNull(match.groupValues[1].toInt(), match.groupValues[2].toInt(), yearOrNull(match.groupValues[3]), now)
        }
        dayMonth.find(trimmed)?.let { match ->
            val month = months[match.groupValues[2].lowercase(Locale.ROOT)] ?: return null
            return dateOrNull(match.groupValues[1].toInt(), month, yearOrNull(match.groupValues[3]), now)
        }
        monthDay.find(trimmed)?.let { match ->
            val month = months[match.groupValues[1].lowercase(Locale.ROOT)] ?: return null
            return dateOrNull(match.groupValues[2].toInt(), month, yearOrNull(match.groupValues[3]), now)
        }
        return null
    }

    fun formatAmount(cents: Long?): String {
        if (cents == null) return ""
        val major = cents / 100
        val minor = (cents % 100).toInt()
        return if (minor == 0) major.toString() else "$major.${minor.toString().padStart(2, '0')}"
    }

    fun formatDue(millis: Long?): String {
        if (millis == null || millis <= 0L) return ""
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        val day = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val year = calendar.get(Calendar.YEAR)
        return "$day.$month.$year"
    }

    private fun parseLine(raw: String, confidence: Int, now: Long): ParsedDebtLine? {
        var line = raw.trim()
        if (line.isEmpty()) return null
        val phoneMatch = phoneRegex.findAll(line)
            .map { it to it.value.count { char -> char.isDigit() } }
            .filter { it.second >= 10 }
            .maxByOrNull { it.second }
        val phone = phoneMatch?.first?.value?.trim().orEmpty()
        if (phoneMatch != null) {
            line = line.removeRange(phoneMatch.first.range).trim()
        }
        val dateMatch = findDate(line, now)
        val dueAt = dateMatch?.second
        val dateSeen = dateMatch != null
        if (dateMatch != null) {
            line = (line.removeRange(dateMatch.first.first..dateMatch.first.last)).trim()
        }
        val amountMatch = amountRegex.findAll(line).maxByOrNull { candidateScore(it.value) }
        val amountCents = amountMatch?.let { parseMoneyToken(it.value) }
        if (amountMatch != null) {
            line = line.removeRange(amountMatch.range)
        }
        val name = cleanName(line)
        if (amountCents == null && dueAt == null) return null
        if (name.isBlank() && amountCents == null) return null
        val nameWords = ClientIdentity.normalizeName(name).split(' ').filter { it.isNotBlank() }
        if (amountCents == null && nameWords.isNotEmpty() && nameWords.all { it in headers }) return null
        val lowOcr = confidence in 0..59
        val letters = name.count { it.isLetter() }
        val digits = name.count { it.isDigit() }
        return ParsedDebtLine(
            name = name,
            amountCents = amountCents,
            dueAt = dueAt,
            phone = phone,
            nameUnsure = lowOcr || name.length < 2 || digits > letters,
            amountUnsure = lowOcr || amountCents == null,
            dateUnsure = lowOcr || !dateSeen || dueAt == null,
        )
    }

    private fun findDate(line: String, now: Long): Pair<IntRange, Long?>? {
        relativeDate.find(line)?.let { match ->
            return match.range to relativeDue(match.value, now)
        }
        numericDate.findAll(line).forEach { match ->
            val due = dateOrNull(
                match.groupValues[1].toInt(),
                match.groupValues[2].toInt(),
                yearOrNull(match.groupValues[3]),
                now,
            )
            if (due != null) return match.range to due
        }
        dayMonth.findAll(line).forEach { match ->
            val month = months[match.groupValues[2].lowercase(Locale.ROOT)] ?: return@forEach
            val due = dateOrNull(match.groupValues[1].toInt(), month, yearOrNull(match.groupValues[3]), now)
            if (due != null) return match.range to due
        }
        monthDay.findAll(line).forEach { match ->
            val month = months[match.groupValues[1].lowercase(Locale.ROOT)] ?: return@forEach
            val due = dateOrNull(match.groupValues[2].toInt(), month, yearOrNull(match.groupValues[3]), now)
            if (due != null) return match.range to due
        }
        return null
    }

    private fun cleanName(raw: String): String =
        raw.replace(Regex("""[|/;]+"""), " ")
            .replace(Regex("""^\s*\d{1,3}[.)]\s+"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', '-', '•', '.', ',', ':')

    private fun candidateScore(token: String): Int {
        val money = parseMoneyToken(token) ?: return -1
        val currency = if (currencyHint.containsMatchIn(token)) 1_000_000_000 else 0
        val separated = if (token.any { it == ' ' || it == '\u00A0' || it == ',' || it == '.' }) 1_000 else 0
        return currency + separated + (money / 100).coerceAtMost(500_000_000L).toInt()
    }

    fun parseMoneyToken(raw: String): Long? {
        var token = raw.trim().replace('\u00A0', ' ')
        if (token.isEmpty()) return null
        val hasCurrency = currencyHint.containsMatchIn(token)
        token = token.replace(currencyHint, "").replace(" ", "")
        if (token.isEmpty() || token.any { it.isLetter() }) return null
        val normalized = when {
            token.count { it == ',' } > 0 && token.count { it == '.' } > 0 -> {
                if (token.lastIndexOf(',') > token.lastIndexOf('.')) {
                    token.replace(".", "").replace(',', '.')
                } else {
                    token.replace(",", "")
                }
            }
            token.contains(',') -> commaOrThousands(token, ',')
            token.contains('.') -> commaOrThousands(token, '.')
            else -> token
        }
        if (!hasCurrency && !raw.any { it == ' ' || it == '\u00A0' || it == ',' || it == '.' }) {
            val bare = normalized.toIntOrNull()
            if (bare != null && bare in 1900..2100) return null
        }
        val decimal = try {
            BigDecimal(normalized)
        } catch (_: NumberFormatException) {
            return null
        }
        if (decimal.signum() <= 0) return null
        return try {
            val cents = decimal.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
            if (cents <= 0L || cents > 1_000_000_000L) null else cents
        } catch (_: ArithmeticException) {
            null
        }
    }

    private fun commaOrThousands(token: String, separator: Char): String {
        val parts = token.split(separator)
        val thousands = parts.size > 2 || (parts.size == 2 && parts[1].length == 3 && parts[0].length <= 3)
        return if (thousands) token.replace(separator.toString(), "") else token.replace(separator, '.')
    }

    private fun relativeDue(word: String, now: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        if (word.equals("завтра", true) || word.equals("tomorrow", true)) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return noon(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
    }

    private fun yearOrNull(raw: String): Int? {
        if (raw.isBlank()) return null
        val value = raw.toInt()
        return if (raw.length <= 2) {
            if (value >= 70) 1900 + value else 2000 + value
        } else {
            value
        }
    }

    private fun dateOrNull(day: Int, month: Int, year: Int?, now: Long): Long? {
        if (month !in 1..12 || day !in 1..31) return null
        val resolvedYear = year ?: Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.YEAR)
        val calendar = Calendar.getInstance().apply {
            isLenient = false
            set(Calendar.YEAR, resolvedYear)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return try {
            calendar.timeInMillis
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun noon(year: Int, month: Int, day: Int): Long =
        dateOrNull(day, month, year, 0L) ?: 0L
}
