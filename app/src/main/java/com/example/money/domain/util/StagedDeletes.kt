package com.example.money.domain.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * Hides rows immediately and keeps the domain object until undo or commit.
 * Lists that use [excluding] drop staged ids without a schema change, so sync
 * tombstones are written only when the delete is actually committed.
 */
class StagedDeletes<T> {
    private val items = LinkedHashMap<Long, T>()
    private val ids = MutableStateFlow<Set<Long>>(emptySet())

    fun stage(id: Long, item: T) {
        synchronized(items) {
            items[id] = item
            ids.value = items.keys.toSet()
        }
    }

    fun unstage(id: Long) {
        synchronized(items) {
            if (items.remove(id) != null) {
                ids.value = items.keys.toSet()
            }
        }
    }

    fun snapshot(): List<T> = synchronized(items) { items.values.toList() }

    fun isEmpty(): Boolean = synchronized(items) { items.isEmpty() }

    fun <R> excluding(source: Flow<List<R>>, idOf: (R) -> Long): Flow<List<R>> =
        combine(source, ids) { list, hidden ->
            if (hidden.isEmpty()) list else list.filter { idOf(it) !in hidden }
        }
}
