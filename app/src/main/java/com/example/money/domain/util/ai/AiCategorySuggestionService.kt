package com.example.money.domain.util.ai

import com.example.money.domain.model.business.BusinessCategories
import com.example.money.domain.model.business.BusinessTransaction
import com.example.money.domain.model.business.BusinessTransactionType

object AiCategorySuggestionService {

    private val keywordMap = mapOf(
        "software" to "Software",
        "saas" to "Software",
        "hosting" to "Software",
        "marketing" to "Marketing",
        "ads" to "Marketing",
        "advert" to "Marketing",
        "travel" to "Travel",
        "flight" to "Travel",
        "hotel" to "Travel",
        "rent" to "Rent",
        "office" to "Rent",
        "payroll" to "Payroll",
        "salary" to "Payroll",
        "tax" to "Taxes",
        "utility" to "Utilities",
        "electric" to "Utilities",
        "consult" to "Consulting",
        "service" to "Services",
        "product" to "Products",
        "revenue" to "Revenue",
    )

    fun suggest(
        note: String,
        type: BusinessTransactionType,
        clientId: Long?,
        transactions: List<BusinessTransaction>,
        customCategories: List<String> = emptyList(),
    ): String? {
        val normalized = note.trim().lowercase()
        if (normalized.isNotBlank()) {
            keywordMap.entries.firstOrNull { (keyword, _) -> normalized.contains(keyword) }?.let {
                return it.value
            }
        }

        if (clientId != null) {
            val clientCategory = transactions
                .filter { it.clientId == clientId && it.type == type }
                .groupBy { it.category }
                .maxByOrNull { it.value.size }
                ?.key
            if (clientCategory != null) return clientCategory
        }

        val globalCategory = transactions
            .filter { it.type == type }
            .groupBy { it.category }
            .maxByOrNull { it.value.size }
            ?.key
        if (globalCategory != null) return globalCategory

        val defaults = BusinessCategories.mergeWithCustom(customCategories)
        return when (type) {
            BusinessTransactionType.INCOME -> defaults.firstOrNull { it in BusinessCategories.revenue }
            BusinessTransactionType.EXPENSE -> defaults.firstOrNull { it in BusinessCategories.expenses }
        }
    }
}
