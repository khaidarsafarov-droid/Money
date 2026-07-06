package com.example.money.domain.model

enum class DebtType {
    /** Money owed to me (I am the creditor). */
    CREDITOR,
    /** Money I owe (I am the debtor). */
    DEBTOR,
}
