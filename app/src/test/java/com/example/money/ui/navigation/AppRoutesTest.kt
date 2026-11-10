package com.example.money.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRoutesTest {
    @Test
    fun goalDetailRouteUsesGoalId() {
        assertEquals("goals/{goalId}", AppRoutes.GOAL_DETAIL)
        assertEquals("goals/42", AppRoutes.goalDetail(42))
        assertTrue(AppRoutes.GOAL_DETAIL.startsWith("goals/"))
    }

    @Test
    fun clientAndGoalDetailStayOnSeparatePages() {
        assertEquals("clients/7", AppRoutes.clientDetail(7))
        assertEquals("goals/7", AppRoutes.goalDetail(7))
    }
}
