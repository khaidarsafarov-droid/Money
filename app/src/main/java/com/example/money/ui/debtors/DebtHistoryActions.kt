package com.example.money.ui.debtors

/** Which theme role paints the red “took” action so it stays red and readable. */
internal enum class HistoryTookFill {
    /** Light theme: solid error with onError text. */
    Error,

    /** Dark theme: error container with onErrorContainer text. */
    ErrorContainer,
}

internal fun historyTookFill(darkTheme: Boolean): HistoryTookFill =
    if (darkTheme) HistoryTookFill.ErrorContainer else HistoryTookFill.Error
