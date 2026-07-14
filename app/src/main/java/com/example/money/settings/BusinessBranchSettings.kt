package com.example.money.settings

import com.example.money.domain.model.FinanceBranch
import com.example.money.domain.model.business.BusinessDashboardView
import com.example.money.domain.model.business.BusinessFilterPreset
import com.example.money.domain.model.business.InvoiceStatus
import com.example.money.domain.model.business.SavedFilterPreset
import org.json.JSONArray
import org.json.JSONObject

data class BusinessBranchSettings(
    val activeBranch: FinanceBranch = FinanceBranch.PERSONAL,
    val businessName: String = "",
    val dashboardView: BusinessDashboardView = BusinessDashboardView.FINANCIAL,
    val savedFilterPresetsJson: String = "[]",
) {
    fun savedFilterPresets(): List<SavedFilterPreset> = decodeSavedFilterPresets(savedFilterPresetsJson)

    companion object {
        fun encodeSavedFilterPresets(presets: List<SavedFilterPreset>): String {
            val array = JSONArray()
            presets.forEach { preset ->
                array.put(
                    JSONObject().apply {
                        put("name", preset.name)
                        preset.clientId?.let { put("clientId", it) }
                        preset.projectId?.let { put("projectId", it) }
                        preset.invoiceStatus?.let { put("invoiceStatus", it.name) }
                        put("periodPreset", preset.periodPreset.name)
                    },
                )
            }
            return array.toString()
        }

        fun decodeSavedFilterPresets(json: String): List<SavedFilterPreset> = runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.getJSONObject(index)
                    add(
                        SavedFilterPreset(
                            name = obj.getString("name"),
                            clientId = obj.optLong("clientId").takeIf { obj.has("clientId") && it > 0 },
                            projectId = obj.optLong("projectId").takeIf { obj.has("projectId") && it > 0 },
                            invoiceStatus = obj.optString("invoiceStatus").takeIf { it.isNotBlank() }
                                ?.let { runCatching { InvoiceStatus.valueOf(it) }.getOrNull() },
                            periodPreset = runCatching {
                                BusinessFilterPreset.valueOf(obj.optString("periodPreset", BusinessFilterPreset.THIS_MONTH.name))
                            }.getOrDefault(BusinessFilterPreset.THIS_MONTH),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
