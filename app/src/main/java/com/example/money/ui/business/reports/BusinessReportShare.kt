package com.example.money.ui.business.reports

import android.content.Context
import android.content.Intent
import com.example.money.domain.model.business.BusinessReportType
import com.example.money.domain.util.BusinessCsvExporter

object BusinessReportShare {

    fun createShareIntent(context: Context, type: BusinessReportType, csvContent: String): Intent =
        Intent(Intent.ACTION_SEND).apply {
            this.type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, csvContent)
            putExtra(Intent.EXTRA_SUBJECT, BusinessCsvExporter.fileName(type))
        }
}
