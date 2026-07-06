package com.example.money.voice

data class VoiceReportSnapshot(
    val activeGoalCount: Int,
    val completedGoalCount: Int,
    val totalCurrentCents: Long,
    val totalTargetCents: Long,
    val remainingCents: Long,
    val progressPercent: Int,
)
