package com.example.money.domain.util

import com.example.money.domain.model.FinanceNoteLine

object NoteCalculator {
    fun totalCents(lines: List<FinanceNoteLine>): Long = lines.sumOf { it.amountCents }
}
