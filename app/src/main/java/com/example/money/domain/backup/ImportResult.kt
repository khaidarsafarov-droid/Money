package com.example.money.domain.backup

data class ImportWarning(
    val subsystem: String,
    val message: String,
)

data class ImportResult(
    val warnings: List<ImportWarning> = emptyList(),
)
