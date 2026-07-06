package com.example.money.data.mapper

import com.example.money.data.local.entity.DebtorEntity
import com.example.money.domain.model.DebtStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DebtorMapperTest {

    @Test
    fun toDomain_unknownStatusFallsBackToActive() {
        val entity = DebtorEntity(
            id = 1L,
            name = "Alex",
            targetAmountCents = 10_000L,
            currentAmountCents = 0L,
            currency = "USD",
            status = "PAID",
            dateGiven = 1L,
            dateDue = 2L,
        )

        assertEquals(DebtStatus.ACTIVE, entity.toDomain().status)
    }
}
