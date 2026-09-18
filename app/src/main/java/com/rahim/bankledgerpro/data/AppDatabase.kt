package com.rahim.bankledgerpro.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val type: String,
    val bank: String,
    val description: String,
    val timestamp: Long,
    val balanceAfter: Long? = null,
    val rawSms: String? = null
)

@Entity(tableName = "bank_profiles")
data class BankProfileEntity(
    @PrimaryKey val bankName: String,
    val sampleSms: String = "",
    val enabled: Boolean = true
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insert(item: TransactionEntity): Long

    @Update
    suspend fun update(item: TransactionEntity)

    @Delete
    suspend fun delete(item: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    suspend fun between(from: Long, to: Long): List<TransactionEntity>
}

@Dao
interface BankProfileDao {
    @Query("SELECT * FROM bank_profiles ORDER BY bankName")
    fun observeAll(): Flow<List<BankProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: BankProfileEntity)

    @Query("SELECT * FROM bank_profiles WHERE bankName = :name LIMIT 1")
    suspend fun get(name: String): BankProfileEntity?
}

@Database(
    entities = [TransactionEntity::class, BankProfileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun bankProfileDao(): BankProfileDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bank_ledger.db"
                ).build().also { INSTANCE = it }
            }
    }
}
