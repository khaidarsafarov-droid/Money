package com.example.money.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class UndoSessionTest {

    @Test
    fun undoRunsOnce() {
        val calls = mutableListOf<String>()
        val session = UndoSession(commit = { calls.add("commit") }, undo = { calls.add("undo") })
        session.finish(undone = true)
        session.finish(undone = false)
        assertEquals(listOf("undo"), calls)
    }

    @Test
    fun timeoutCommitsOnce() {
        val calls = mutableListOf<String>()
        val session = UndoSession(commit = { calls.add("commit") }, undo = { calls.add("undo") })
        session.finish(undone = false)
        session.finish(undone = false)
        assertEquals(listOf("commit"), calls)
    }
}
