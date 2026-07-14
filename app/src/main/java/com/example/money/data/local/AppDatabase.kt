package com.example.money.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.money.data.local.dao.DebtPaymentDao
import com.example.money.data.local.dao.LuckDiaryDao
import com.example.money.data.local.dao.ExpenseDao
import com.example.money.data.local.dao.SavingsGoalDao
import com.example.money.data.local.dao.SyncQueueDao
import com.example.money.data.local.dao.TransferDao
import com.example.money.data.local.dao.BusinessClientDao
import com.example.money.data.local.dao.BusinessInvoiceDao
import com.example.money.data.local.dao.BusinessProjectDao
import com.example.money.data.local.dao.BusinessTransactionDao
import com.example.money.data.local.dao.DebtorDao
import com.example.money.data.local.entity.BusinessClientEntity
import com.example.money.data.local.entity.BusinessInvoiceEntity
import com.example.money.data.local.entity.BusinessProjectEntity
import com.example.money.data.local.entity.BusinessTransactionEntity
import com.example.money.data.local.entity.DebtorEntity
import com.example.money.data.local.entity.DebtPaymentEntity
import com.example.money.data.local.entity.LuckDiaryEntryEntity
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
import com.example.money.data.local.migration.MIGRATION_9_10
import com.example.money.data.local.migration.MIGRATION_10_11
import com.example.money.data.local.migration.MIGRATION_11_12
import com.example.money.data.local.migration.MIGRATION_12_13
import com.example.money.data.local.migration.MIGRATION_13_14

@Database(
    entities = [
        DebtorEntity::class,
        DebtPaymentEntity::class,
        TransferEntity::class,
        ExpenseEntity::class,
        SavingsGoalEntity::class,
        SyncQueueEntity::class,
        LuckDiaryEntryEntity::class,
        BusinessClientEntity::class,
        BusinessProjectEntity::class,
        BusinessInvoiceEntity::class,
        BusinessTransactionEntity::class,
    ],
    version = 14,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun luckDiaryDao(): LuckDiaryDao

    abstract fun debtorDao(): DebtorDao
    abstract fun debtPaymentDao(): DebtPaymentDao
    abstract fun transferDao(): TransferDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun businessClientDao(): BusinessClientDao
    abstract fun businessProjectDao(): BusinessProjectDao
    abstract fun businessInvoiceDao(): BusinessInvoiceDao
    abstract fun businessTransactionDao(): BusinessTransactionDao

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
                    MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14,
                ).build().also { instance = it }
            }
        }
    }
}
