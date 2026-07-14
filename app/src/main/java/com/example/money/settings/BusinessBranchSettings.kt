package com.example.money.settings

import com.example.money.domain.model.FinanceBranch

data class BusinessBranchSettings(
    val activeBranch: FinanceBranch = FinanceBranch.PERSONAL,
    val businessName: String = "",
)
