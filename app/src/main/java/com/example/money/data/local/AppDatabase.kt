package com.example.money.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.money.data.local.dao.DebtPaymentDao
import com.example.money.data.local.dao.DebtorDao
import com.example.money.data.local.dao.ExpenseDao
import com.example.money.data.local.dao.SavingsGoalDao
import com.example.money.data.local.dao.SyncQueueDao
import com.example.money.data.local.dao.TransferDao
import com.example.money.data.local.entity.DebtPaymentEntity
import com.example.money.data.local.entity.DebtorEntity
import com.example.money.data.local.entity.ExpenseEntity
import com.example.money.data.local.entity.SavingsGoalEntity
import com.example.money.data.local.entity.SyncQueueEntity
import com.example.money.data.local.entity.TransferEntity
import com.example.money.data.local.migration.MIGRATION_1_2
import com.example.money.data.local.migration.MIGRATION_2_3
import com.example.money.data.local.migration.MIGRATION_3_4
import com.example.money.data.local.migration.MIGRATION_4_5
import com.example.money.data.local.migration.MIGRATION_5_6
import com.example.money.data.local.migration.MIGRATION_6_7
import com.example.money.data.local.migration.MIGRATION_7_8
import com.example.money.data.local.migration.MIGRATION_8_9

@Database(
    entities = [
        DebtorEntity::class,
        DebtPaymentEntity::class,
        TransferEntity::class,
        ExpenseEntity::class,
        SavingsGoalEntity::class,
        SyncQueueEntity::class,
    ],
    version = 9,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun debtorDao(): DebtorDao
    abstract fun debtPaymentDao(): DebtPaymentDao
    abstract fun transferDao(): TransferDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun syncQueueDao(): SyncQueueDao

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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9).build().also { instance = it }
            }
        }
    }
}
