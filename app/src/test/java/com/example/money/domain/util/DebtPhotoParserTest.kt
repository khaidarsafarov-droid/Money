package com.example.money.domain.util

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtPhotoParserTest {

    private val now = noon(2026, 10, 20)

    @Test
    fun parsesNotebookLines() {
        val text = """
            Имя Сумма Дата
            Магазин Алмаз    150 000 сум    20.10
            Иванов  50,000 руб  21.10.2026  +998 90 123 45 67
            Shop 12 | ${'$'}120 | today
            ООО Рассвет  1 200,50  завтра
            1. Базар  10 000  01.11
            Лавка  20 октября
            Oct 21  75.5 usd
            Пустая строка без чисел
        """.trimIndent()

        val rows = DebtPhotoParser.parse(text, now)
        assertEquals(7, rows.size)

        val alma = rows[0]
        assertEquals("Магазин Алмаз", alma.name)
        assertEquals(15_000_000L, alma.amountCents)
        assertEquals(noon(2026, 10, 20), alma.dueAt)

        val ivanov = rows[1]
        assertEquals("Иванов", ivanov.name)
        assertEquals(5_000_000L, ivanov.amountCents)
        assertEquals(noon(2026, 10, 21), ivanov.dueAt)
        assertTrue(ClientIdentity.phonesMatch(ivanov.phone, "998901234567"))

        val shop = rows[2]
        assertEquals("Shop 12", shop.name)
        assertEquals(12_000L, shop.amountCents)
        assertEquals(now, shop.dueAt)

        val rassvet = rows[3]
        assertEquals("ООО Рассвет", rassvet.name)
        assertEquals(120_050L, rassvet.amountCents)
        assertEquals(noon(2026, 10, 21), rassvet.dueAt)

        val bazar = rows[4]
        assertEquals("Базар", bazar.name)
        assertEquals(1_000_000L, bazar.amountCents)
        assertEquals(noon(2026, 11, 1), bazar.dueAt)

        val lavka = rows[5]
        assertEquals("Лавка", lavka.name)
        assertNull(lavka.amountCents)
        assertEquals(noon(2026, 10, 20), lavka.dueAt)
        assertTrue(lavka.amountUnsure)

        val oct = rows[6]
        assertEquals(7_550L, oct.amountCents)
        assertEquals(noon(2026, 10, 21), oct.dueAt)
    }

    @Test
    fun amountFormats() {
        assertEquals(15_000_000L, DebtPhotoParser.parseMoneyToken("150 000"))
        assertEquals(120_050L, DebtPhotoParser.parseMoneyToken("1 200,50"))
        assertEquals(120_050L, DebtPhotoParser.parseMoneyToken("1.200,50"))
        assertEquals(120_050L, DebtPhotoParser.parseMoneyToken("1,200.50"))
        assertEquals(5_050L, DebtPhotoParser.parseMoneyToken("50,50"))
        assertEquals(5_000_000L, DebtPhotoParser.parseMoneyToken("50,000"))
        assertEquals(12_050L, DebtPhotoParser.parseMoneyToken("120.5"))
        assertNull(DebtPhotoParser.parseMoneyToken("2026"))
    }

    @Test
    fun datesAndLowConfidence() {
        assertEquals(noon(2026, 3, 5), DebtPhotoParser.parseDue("05/03/26", now))
        assertEquals(noon(2026, 10, 21), DebtPhotoParser.parseDue("tomorrow", now))
        val low = DebtPhotoParser.parseLines(listOf("Магазин 1000 21.10" to 40), now).single()
        assertTrue(low.nameUnsure && low.amountUnsure && low.dateUnsure)
        assertEquals(false, low.selectedByDefault)
    }

    @Test
    fun clientNameAndPhoneMatch() {
        assertEquals("магазин елка", ClientIdentity.normalizeName("  Магазин   Ёлка "))
        assertEquals(
            "магазин елка",
            ClientIdentity.normalizeName("МАГАЗИН\tЕЛКА"),
        )
        assertTrue(
            ClientIdentity.sameParty("Ёлка", "", "елка", ""),
        )
        assertTrue(
            ClientIdentity.sameParty("Другое", "+998 90 123 45 67", "Лавка", "998901234567"),
        )
        val first = debtor(1, "Ёлка", "+998901234567")
        val second = debtor(2, "Другой", "")
        val match = ClientIdentity.findMatch(listOf(second, first), "елка", "", com.example.money.domain.model.DebtType.CREDITOR)
        assertEquals(1L, match?.id)
        val byPhone = ClientIdentity.findMatch(
            listOf(second, first),
            "Новое имя",
            "90 123 45 67",
            com.example.money.domain.model.DebtType.CREDITOR,
        )
        assertEquals(1L, byPhone?.id)
        val again = ClientIdentity.groups(listOf(first, debtor(3, "Елка", "")), com.example.money.domain.model.DebtType.CREDITOR)
        assertEquals(1, again.size)
        assertEquals(1L, again.single().open.id)
    }

    private fun debtor(id: Long, name: String, phone: String) = com.example.money.domain.model.Debtor(
        id = id,
        name = name,
        phone = phone,
        targetAmountCents = 100,
        dateGiven = now,
        dateDue = now,
        debtType = com.example.money.domain.model.DebtType.CREDITOR,
    )

    private fun noon(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
