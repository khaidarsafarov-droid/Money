package com.example.money.domain.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StagedDeletesTest {

    @Test
    fun stagedIdDisappearsUntilUndo() = runTest {
        val staged = StagedDeletes<String>()
        val source = MutableStateFlow(listOf(1L to "Ann", 2L to "Bo"))
        val seen = mutableListOf<List<Long>>()
        val job = launch {
            staged.excluding(source) { it.first }.collect { rows ->
                seen.add(rows.map { it.first })
            }
        }
        advanceUntilIdle()
        staged.stage(1L, "Ann")
        advanceUntilIdle()
        staged.unstage(1L)
        advanceUntilIdle()
        job.cancel()

        assertEquals(
            listOf(listOf(1L, 2L), listOf(2L), listOf(1L, 2L)),
            seen,
        )
        assertTrue(staged.isEmpty())
    }

    @Test
    fun snapshotKeepsTheHiddenItemForCommit() {
        val staged = StagedDeletes<String>()
        staged.stage(4L, "Goal")
        assertEquals(listOf("Goal"), staged.snapshot())
        staged.unstage(4L)
        assertTrue(staged.snapshot().isEmpty())
    }
}
