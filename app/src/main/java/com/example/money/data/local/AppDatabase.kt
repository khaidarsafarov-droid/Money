package com.example.money.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.money.data.local.dao.FinanceNoteDao
import com.example.money.data.local.dao.GoalActionDao
import com.example.money.data.local.dao.DebtorDao
import com.example.money.data.local.dao.DebtPaymentDao
import com.example.money.data.local.dao.LuckDiaryDao
import com.example.money.data.local.dao.ExpenseDao
import com.example.money.data.local.dao.SavingsGoalDao
import com.example.money.data.local.dao.SyncQueueDao
import com.example.money.data.local.dao.SyncTombstoneDao
import com.example.money.data.local.dao.TransferDao
import com.example.money.data.local.entity.FinanceNoteEntity
import com.example.money.data.local.entity.FinanceNoteLineEntity
import com.example.money.data.local.entity.GoalActionEntity
import com.example.money.data.local.entity.DebtorEntity
import com.example.money.data.local.entity.DebtPaymentEntity
import com.example.money.data.local.entity.LuckDiaryEntryEntity
import com.example.money.data.local.entity.ExpenseEntity
import com.example.money.data.local.entity.SavingsGoalEntity
import com.example.money.data.local.entity.SyncQueueEntity
import com.example.money.data.local.entity.SyncTombstoneEntity
import com.example.money.data.local.entity.TransferEntity
import com.example.money.data.local.migration.MIGRATION_1_2
import com.example.money.data.local.migration.MIGRATION_2_3
import com.example.money.data.local.migration.MIGRATION_3_4
import com.example.money.data.local.migration.MIGRATION_4_5
import com.example.money.data.local.migration.MIGRATION_5_6
import com.example.money.data.local.migration.MIGRATION_6_7
import com.example.money.data.local.migration.MIGRATION_7_8
import com.example.money.data.local.migration.MIGRATION_8_9
import com.example.money.data.local.migration.MIGRATION_9_10
import com.example.money.data.local.migration.MIGRATION_10_11
import com.example.money.data.local.migration.MIGRATION_11_12
import com.example.money.data.local.migration.MIGRATION_12_13
import com.example.money.data.local.migration.MIGRATION_13_14
import com.example.money.data.local.migration.MIGRATION_14_15
import com.example.money.data.local.migration.MIGRATION_15_16
import com.example.money.data.local.migration.MIGRATION_16_17
import com.example.money.data.local.migration.MIGRATION_17_18
import com.example.money.data.local.migration.MIGRATION_18_19
import com.example.money.data.local.migration.MIGRATION_19_20
import com.example.money.data.local.migration.MIGRATION_20_21
import com.example.money.data.local.migration.MIGRATION_21_22
import com.example.money.data.local.migration.MIGRATION_22_23
import com.example.money.data.local.migration.MIGRATION_23_24
import com.example.money.data.local.migration.MIGRATION_24_25
import com.example.money.data.local.migration.MIGRATION_25_26

@Database(
    entities = [
        DebtorEntity::class,
        DebtPaymentEntity::class,
        TransferEntity::class,
        ExpenseEntity::class,
        SavingsGoalEntity::class,
        GoalActionEntity::class,
        SyncQueueEntity::class,
        SyncTombstoneEntity::class,
        LuckDiaryEntryEntity::class,
        FinanceNoteEntity::class,
        FinanceNoteLineEntity::class,
    ],
    version = 26,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun luckDiaryDao(): LuckDiaryDao
    abstract fun financeNoteDao(): FinanceNoteDao

    abstract fun debtorDao(): DebtorDao
    abstract fun debtPaymentDao(): DebtPaymentDao
    abstract fun transferDao(): TransferDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun goalActionDao(): GoalActionDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun syncTombstoneDao(): SyncTombstoneDao

    companion object {
        private const val DATABASE_NAME = "money.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                ).addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                    MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11,
                    MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17,
                    MIGRATION_17_18,
                    MIGRATION_18_19,
                    MIGRATION_19_20,
                    MIGRATION_20_21,
                    MIGRATION_21_22,
                    MIGRATION_22_23,
                    MIGRATION_23_24,
                    MIGRATION_24_25,
                    MIGRATION_25_26,
                ).build().also { instance = it }
            }
        }
    }
}
