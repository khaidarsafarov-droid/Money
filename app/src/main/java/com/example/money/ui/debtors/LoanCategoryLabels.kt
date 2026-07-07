package com.example.money.ui.debtors

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.money.R
import com.example.money.domain.model.LoanCategory

@Composable
fun loanCategoryLabel(category: LoanCategory): String = when (category) {
    LoanCategory.CREDIT_CARD -> stringResource(R.string.loan_category_credit_card)
    LoanCategory.CONSUMER -> stringResource(R.string.loan_category_consumer)
    LoanCategory.AUTO -> stringResource(R.string.loan_category_auto)
    LoanCategory.MORTGAGE -> stringResource(R.string.loan_category_mortgage)
    LoanCategory.EDUCATION -> stringResource(R.string.loan_category_education)
    LoanCategory.OTHER -> stringResource(R.string.loan_category_other)
}
