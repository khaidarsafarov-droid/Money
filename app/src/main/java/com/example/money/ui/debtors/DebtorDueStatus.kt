package com.example.money.ui.debtors

import com.example.money.domain.model.Debtor
import com.example.money.domain.util.DebtDueDateUtils
import java.util.Locale

enum class DebtDueStatus {
    PAID,
    OVERDUE,
    URGENT,
    NORMAL,
}

private const val URGENT_WINDOW_MS = 7L * 24 * 60 * 60 * 1000

fun Debtor.dueStatus(now: Long = System.currentTimeMillis()): DebtDueStatus {
    if (isPaid) return DebtDueStatus.PAID
    if (dateDue <= 0L) return DebtDueStatus.NORMAL
    if (DebtDueDateUtils.isOverdue(dateDue, now)) return DebtDueStatus.OVERDUE
    if (dateDue - now <= URGENT_WINDOW_MS) return DebtDueStatus.URGENT
    return DebtDueStatus.NORMAL
}

fun String.initials(): String {
    val parts = trim().split(" ").filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase(Locale.ROOT)
        else -> "${parts[0].first()}${parts[1].first()}".uppercase(Locale.ROOT)
    }
}
