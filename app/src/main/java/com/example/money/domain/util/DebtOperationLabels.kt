package com.example.money.domain.util

import com.example.money.domain.model.DebtType
import java.util.Locale

object DebtOperationLabels {
    private val isEnglish: Boolean
        get() = Locale.getDefault().language.startsWith("en")

    val categoryDebt: String
        get() = if (isEnglish) "Debt" else "Долг"

    fun creationNote(name: String): String =
        if (isEnglish) "Debt: $name" else "Долг: $name"

    fun returnNote(name: String): String =
        if (isEnglish) "Return from $name" else "Возврат от $name"

    fun paymentNote(name: String): String =
        if (isEnglish) "Payment to $name" else "Оплата $name"

    fun creationTransferNote(debtType: DebtType, name: String): String = when (debtType) {
        DebtType.CREDITOR -> creationNote(name)
        DebtType.DEBTOR -> creationNote(name)
    }
}
