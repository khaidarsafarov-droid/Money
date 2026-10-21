package com.example.money.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class FinoraInitialsTest {
    @Test
    fun peopleUseOneLetterAndShopsUseTwo() {
        assertEquals("Ю", finoraInitials("Юсуф"))
        assertEquals("А", finoraInitials("Амина"))
        assertEquals("Ул", finoraInitials("Магазин «Улыбка»"))
        assertEquals("Са", finoraInitials("Кафе «Самовар»"))
        assertEquals("Га", finoraInitials("ИП Гасанов"))
        assertEquals("Ст", finoraInitials("Строймаркет"))
    }
}
