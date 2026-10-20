package com.example.money.notification

object DebtNotificationIds {
    const val TAG_DUE = "debt_due"
    const val TAG_ADVANCE = "debt_advance"
    const val TAG_MESSAGE = "debt_msg"
    const val TAG_CONFIRM = "debt_confirm"
    const val LEGACY_TAG = "debt_reminder"
    const val CONFIRM_ID_OFFSET = 50_000L

    val TAGS = listOf(TAG_DUE, TAG_ADVANCE, TAG_MESSAGE, TAG_CONFIRM, LEGACY_TAG)

    fun idFor(debtorId: Long): Int = debtorId.toInt()

    fun legacyAdvanceId(debtorId: Long, daysBefore: Int): Int =
        (debtorId * 10 + daysBefore).toInt()

    fun legacyMessageId(debtorId: Long, channelOrdinal: Int): Int =
        (debtorId * 100 + channelOrdinal).toInt()

    fun legacyConfirmId(debtorId: Long): Int =
        (debtorId + CONFIRM_ID_OFFSET).toInt()

    fun legacyIds(debtorId: Long, messageChannelCount: Int): List<Int> = buildList {
        add(legacyConfirmId(debtorId))
        add(legacyAdvanceId(debtorId, 1))
        add(legacyAdvanceId(debtorId, 3))
        repeat(messageChannelCount) { ordinal ->
            add(legacyMessageId(debtorId, ordinal))
        }
    }
}
