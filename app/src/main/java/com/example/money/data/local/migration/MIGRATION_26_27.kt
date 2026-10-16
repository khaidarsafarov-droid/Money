package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.money.data.sync.SyncIds

/**
 * Makes sync_id a UUID for every entity. Numeric and other legacy ids are
 * replaced once, and references such as goal_sync_id follow the new goal id.
 */
val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val replaced = linkedMapOf<String, String>()
        val goalIds = rewriteSyncIds(db, "savings_goal", "GOAL")
        replaced.putAll(goalIds)
        listOf(
            "goal_action" to "GOAL_ACTION",
            "debtor" to "DEBT",
            "debt_payment" to "DEBT_PAYMENT",
            "transfers" to "TRANSACTION",
            "expenses" to "TRANSACTION",
            "finance_notes" to "NOTE",
            "finance_note_lines" to "NOTE_LINE",
        ).forEach { (table, type) ->
            rewriteSyncIds(db, table, type).forEach { (oldId, newId) ->
                if (oldId !in replaced) replaced[oldId] = newId
            }
        }
        rewriteSyncIds(db, "luck_diary_entry", tombstoneType = null).forEach { (oldId, newId) ->
            if (oldId !in replaced) replaced[oldId] = newId
        }
        rewriteReferences(db, "transfers", "goal_sync_id", goalIds)
        rewriteReferences(db, "expenses", "goal_sync_id", goalIds)
        rewriteQueuePayloads(db, replaced)
    }

    private fun rewriteSyncIds(
        db: SupportSQLiteDatabase,
        table: String,
        tombstoneType: String?,
    ): Map<String, String> {
        if (!tableExists(db, table)) return emptyMap()
        val pending = linkedMapOf<String, String>()
        db.query("SELECT sync_id FROM $table").use { cursor ->
            while (cursor.moveToNext()) {
                val current = cursor.getString(0) ?: continue
                if (!SyncIds.isUuid(current) && current !in pending) {
                    pending[current] = SyncIds.newId()
                }
            }
        }
        pending.forEach { (oldId, newId) ->
            db.execSQL("UPDATE $table SET sync_id = ? WHERE sync_id = ?", arrayOf(newId, oldId))
            if (tombstoneType != null) {
                rewriteTombstone(db, tombstoneType, oldId, newId)
            }
        }
        return pending
    }

    private fun rewriteTombstone(
        db: SupportSQLiteDatabase,
        entityType: String,
        oldId: String,
        newId: String,
    ) {
        if (!tableExists(db, "sync_tombstone")) return
        db.query(
            "SELECT payload_json FROM sync_tombstone WHERE entity_type = ? AND entity_id = ?",
            arrayOf(entityType, oldId),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return
            val payload = replacePayloadId(cursor.getString(0).orEmpty(), oldId, newId)
            db.execSQL(
                """
                UPDATE sync_tombstone
                SET entity_id = ?, payload_json = ?
                WHERE entity_type = ? AND entity_id = ?
                """.trimIndent(),
                arrayOf(newId, payload, entityType, oldId),
            )
        }
    }

    private fun rewriteReferences(
        db: SupportSQLiteDatabase,
        table: String,
        column: String,
        replaced: Map<String, String>,
    ) {
        if (!tableExists(db, table)) return
        replaced.forEach { (oldId, newId) ->
            db.execSQL(
                "UPDATE $table SET $column = ? WHERE $column = ?",
                arrayOf(newId, oldId),
            )
        }
    }

    private fun rewriteQueuePayloads(db: SupportSQLiteDatabase, replaced: Map<String, String>) {
        if (!tableExists(db, "sync_queue") || replaced.isEmpty()) return
        db.query("SELECT id, payloadJson FROM sync_queue").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val payload = cursor.getString(1) ?: continue
                val updated = replaced.entries.fold(payload) { current, (oldId, newId) ->
                    replacePayloadId(current, oldId, newId)
                }
                if (updated != payload) {
                    db.execSQL(
                        "UPDATE sync_queue SET payloadJson = ? WHERE id = ?",
                        arrayOf(updated, id),
                    )
                }
            }
        }
    }

    private fun replacePayloadId(payload: String, oldId: String, newId: String): String =
        payload
            .replace("\"id\":\"$oldId\"", "\"id\":\"$newId\"")
            .replace("\"id\": \"$oldId\"", "\"id\": \"$newId\"")

    private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean {
        db.query(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(table),
        ).use { return it.moveToFirst() }
    }
}
