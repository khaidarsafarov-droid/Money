package com.example.money.domain.model.business

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class RecurringBusinessRule(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val type: BusinessTransactionType,
    val amountCents: Long,
    val category: String,
    val intervalDays: Int = 30,
    val lastRunAt: Long? = null,
    val enabled: Boolean = true,
) {
    fun isDue(now: Long = System.currentTimeMillis()): Boolean {
        if (!enabled) return false
        val last = lastRunAt ?: return true
        return now - last >= intervalDays * 24L * 60 * 60 * 1000
    }
}

object RecurringBusinessRuleCodec {
    fun encode(rules: List<RecurringBusinessRule>): String {
        val array = JSONArray()
        rules.forEach { rule ->
            array.put(
                JSONObject().apply {
                    put("id", rule.id)
                    put("label", rule.label)
                    put("type", rule.type.name)
                    put("amountCents", rule.amountCents)
                    put("category", rule.category)
                    put("intervalDays", rule.intervalDays)
                    rule.lastRunAt?.let { put("lastRunAt", it) }
                    put("enabled", rule.enabled)
                },
            )
        }
        return array.toString()
    }

    fun decode(json: String): List<RecurringBusinessRule> = runCatching {
        val array = JSONArray(json)
        buildList {
            for (index in 0 until array.length()) {
                val obj = array.getJSONObject(index)
                add(
                    RecurringBusinessRule(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        label = obj.getString("label"),
                        type = BusinessTransactionType.valueOf(obj.getString("type")),
                        amountCents = obj.getLong("amountCents"),
                        category = obj.getString("category"),
                        intervalDays = obj.optInt("intervalDays", 30),
                        lastRunAt = obj.optLong("lastRunAt").takeIf { obj.has("lastRunAt") && it > 0 },
                        enabled = obj.optBoolean("enabled", true),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())
}
