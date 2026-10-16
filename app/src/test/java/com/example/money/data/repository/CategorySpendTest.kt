package com.example.money.data.repository

import android.content.Context
import androidx.room.Room
import com.example.money.data.local.AppDatabase
import com.example.money.data.local.entity.ExpenseEntity
import com.example.money.domain.util.BudgetPeriods
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CategorySpendTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun categorySpend_usesHalfOpenMonthAndGroupsCategories() = runBlocking {
        val zone = ZoneId.of("America/New_York")
        val march = BudgetPeriods.monthContaining(Instant.parse("2026-03-15T15:00:00Z"), zone)
        database.expenseDao().insert(
            ExpenseEntity(
                amountCents = 250L,
                category = "Food",
                date = march.startMillis,
                isConfirmed = true,
            ),
        )
        database.expenseDao().insert(
            ExpenseEntity(
                amountCents = 100L,
                category = "Food",
                date = march.endMillis - 1L,
                isConfirmed = true,
            ),
        )
        database.expenseDao().insert(
            ExpenseEntity(
                amountCents = 9_999L,
                category = "Food",
                date = march.endMillis,
                isConfirmed = true,
            ),
        )
        database.expenseDao().insert(
            ExpenseEntity(
                amountCents = 40L,
                category = "Rent",
                date = march.startMillis + 1L,
                isConfirmed = true,
            ),
        )

        val totals = database.expenseDao().observeCategorySpend(march.startMillis, march.endMillis).first()

        assertEquals(listOf("Food" to 350L, "Rent" to 40L), totals.map { it.category to it.totalCents })
    }
}
