package com.example.money.voice

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.money.MoneyApplication

class VoiceReportWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as MoneyApplication
        val outcome = VoiceReportEngine(app).runScheduledReport()

        VoiceReportScheduler.enqueueNextFromSettings(applicationContext)

        return when (outcome) {
            VoiceReportOutcome.Failed -> Result.retry()
            else -> Result.success()
        }
    }
}
