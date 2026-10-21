package com.example.money.ui.home

import java.util.concurrent.atomic.AtomicBoolean

/** One snackbar undo window. [finish] runs the undo or the commit exactly once. */
class UndoSession(
    private val commit: () -> Unit,
    private val undo: () -> Unit,
) {
    private val settled = AtomicBoolean(false)

    fun finish(undone: Boolean) {
        if (!settled.compareAndSet(false, true)) return
        if (undone) undo() else commit()
    }
}
