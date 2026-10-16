package com.example.money.data.local

import android.content.Context
import androidx.room.Room
import com.example.money.data.local.entity.DebtorEntity
import com.example.money.data.local.entity.ExpenseEntity
import com.example.money.settings.AppMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ModeIsolationTest {

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
    fun writeInOneModeIsInvisibleInTheOther() = runBlocking {
        database.debtorDao().insert(
            DebtorEntity(
                name = "Личный",
                targetAmountCents = 1_000L,
                dateGiven = 1L,
                dateDue = 2L,
                mode = AppMode.PERSONAL.name,
            ),
        )
        database.debtorDao().insert(
            DebtorEntity(
                name = "Клиентский",
                targetAmountCents = 5_000L,
                dateGiven = 1L,
                dateDue = 2L,
                mode = AppMode.PROFESSIONAL.name,
            ),
        )
        database.expenseDao().insert(
            ExpenseEntity(
                amountCents = 250L,
                category = "Food",
                date = 10L,
                isConfirmed = true,
                mode = AppMode.PERSONAL.name,
            ),
        )

        val personal = database.debtorDao().getAllDebtorsWithPayments(AppMode.PERSONAL.name).first()
        val professional = database.debtorDao().getAllDebtorsWithPayments(AppMode.PROFESSIONAL.name).first()
        assertEquals(listOf("Личный"), personal.map { it.debtor.name })
        assertEquals(listOf("Клиентский"), professional.map { it.debtor.name })

        val personalSpend = database.expenseDao()
            .observeCategorySpend(AppMode.PERSONAL.name, 0L, 100L)
            .first()
        val professionalSpend = database.expenseDao()
            .observeCategorySpend(AppMode.PROFESSIONAL.name, 0L, 100L)
            .first()
        assertEquals(250L, personalSpend.single().totalCents)
        assertTrue(professionalSpend.isEmpty())
    }
}
