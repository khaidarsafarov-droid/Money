package com.example.money.ui.debtors

import android.content.Context
import android.content.Intent
import com.example.money.R
import com.example.money.domain.model.Debtor
import com.example.money.ui.util.Formatters

object DebtReminderShare {

    fun buildMessage(context: Context, debtor: Debtor): String {
        val remaining = Formatters.currencyFromCents(debtor.remainingAmountCents, debtor.currency)
        val dueDate = Formatters.shortDate(debtor.dateDue)
        return when (debtor.dueStatus()) {
            DebtDueStatus.OVERDUE -> context.getString(
                R.string.debt_remind_message_overdue,
                debtor.name,
                remaining,
                dueDate,
            )
            else -> context.getString(
                R.string.debt_remind_message,
                debtor.name,
                remaining,
                dueDate,
            )
        }
    }

    fun createShareIntent(context: Context, debtor: Debtor): Intent {
        val message = buildMessage(context, debtor)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.debt_remind_subject, debtor.name))
        }
    }
}
