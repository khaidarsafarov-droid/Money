package com.example.money.domain.util

import com.example.money.domain.model.DebtStatus
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.settings.AppMode
import java.util.Locale

/**
 * A work-mode client is a debtor. The same shop or person is one debtor:
 * later amounts are history entries, not a second row.
 */
object ClientIdentity {

    data class Group(
        val open: Debtor,
        val display: Debtor,
    )

    fun normalizeName(raw: String): String =
        raw.lowercase(Locale.ROOT)
            .replace('ё', 'е')
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

    fun normalizePhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.length >= 7) digits else ""
    }

    fun phonesMatch(leftRaw: String, rightRaw: String): Boolean {
        val left = normalizePhone(leftRaw)
        val right = normalizePhone(rightRaw)
        if (left.isEmpty() || right.isEmpty()) return false
        val leftKey = if (left.length > 10) left.takeLast(10) else left
        val rightKey = if (right.length > 10) right.takeLast(10) else right
        return leftKey == rightKey || leftKey.endsWith(rightKey) || rightKey.endsWith(leftKey)
    }

    fun sameParty(leftName: String, leftPhone: String, rightName: String, rightPhone: String): Boolean {
        val left = normalizeName(leftName)
        val right = normalizeName(rightName)
        if (left.isNotBlank() && left == right) return true
        return phonesMatch(leftPhone, rightPhone)
    }

    fun findMatch(
        existing: List<Debtor>,
        name: String,
        phone: String,
        debtType: DebtType,
    ): Debtor? {
        val nameKey = normalizeName(name)
        return existing
            .filter { it.debtType == debtType && sameParty(it.name, it.phone, name, phone) }
            .minWithOrNull(
                compareBy<Debtor> { it.isPaid }
                    .thenBy { if (nameKey.isNotBlank() && normalizeName(it.name) == nameKey) 0 else 1 }
                    .thenBy { it.id },
            )
    }

    fun groups(debtors: List<Debtor>, debtType: DebtType, query: String = ""): List<Group> {
        val typed = debtors.filter { it.debtType == debtType }
        if (typed.isEmpty()) return emptyList()
        val parent = IntArray(typed.size) { it }
        fun find(index: Int): Int {
            var cursor = index
            while (parent[cursor] != cursor) cursor = parent[cursor]
            var compress = index
            while (parent[compress] != cursor) {
                val next = parent[compress]
                parent[compress] = cursor
                compress = next
            }
            return cursor
        }
        fun union(left: Int, right: Int) {
            val leftRoot = find(left)
            val rightRoot = find(right)
            if (leftRoot != rightRoot) parent[rightRoot] = leftRoot
        }
        for (left in typed.indices) {
            for (right in left + 1 until typed.size) {
                if (sameParty(typed[left].name, typed[left].phone, typed[right].name, typed[right].phone)) {
                    union(left, right)
                }
            }
        }
        val queryName = normalizeName(query)
        val queryPhone = normalizePhone(query)
        return typed.indices
            .groupBy { find(it) }
            .values
            .map { indexes -> toGroup(indexes.map { typed[it] }) }
            .filter { group ->
                if (query.isBlank()) return@filter true
                val nameHit = queryName.isNotBlank() && normalizeName(group.open.name).contains(queryName)
                val phoneHit = queryPhone.isNotEmpty() && normalizePhone(group.open.phone).contains(queryPhone)
                nameHit || phoneHit
            }
    }

    private fun toGroup(members: List<Debtor>): Group {
        val nameKey = normalizeName(members.first().name)
        val open = members.minWith(
            compareBy<Debtor> { it.isPaid }
                .thenBy { if (normalizeName(it.name) == nameKey) 0 else 1 }
                .thenBy { it.id },
        )
        val remaining = members.sumOf { it.remainingAmountCents }
        val due = members
            .filter { !it.isPaid && it.dateDue > 0L }
            .minOfOrNull { it.dateDue }
            ?: 0L
        val display = if (members.size == 1) {
            open
        } else {
            open.copy(
                targetAmountCents = remaining,
                currentAmountCents = 0L,
                dateDue = due,
                status = if (members.all { it.isPaid }) DebtStatus.COMPLETED else DebtStatus.ACTIVE,
                mode = AppMode.PROFESSIONAL,
            )
        }
        return Group(open = open, display = display)
    }
}
