package com.example.money.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DebtNotificationIdsTest {

    @Test
    fun tagsAreDistinct() {
        assertEquals(DebtNotificationIds.TAGS.size, DebtNotificationIds.TAGS.toSet().size)
    }

    @Test
    fun legacyAdvanceIdCollidesWithAnotherDebtorsDueId() {
        assertEquals(
            DebtNotificationIds.idFor(51),
            DebtNotificationIds.legacyAdvanceId(debtorId = 5, daysBefore = 1),
        )
        assertNotEquals(DebtNotificationIds.TAG_DUE, DebtNotificationIds.TAG_ADVANCE)
        assertNotEquals(DebtNotificationIds.TAG_DUE, DebtNotificationIds.TAG_MESSAGE)
        assertNotEquals(DebtNotificationIds.TAG_DUE, DebtNotificationIds.TAG_CONFIRM)
    }
}
