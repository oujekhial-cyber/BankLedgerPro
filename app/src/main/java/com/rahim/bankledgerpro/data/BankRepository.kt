package com.rahim.bankledgerpro.data

import kotlinx.coroutines.flow.Flow

class BankRepository(private val db: AppDatabase) {
    val transactions: Flow<List<TransactionEntity>> = db.transactionDao().observeAll()
    val profiles: Flow<List<BankProfileEntity>> = db.bankProfileDao().observeAll()

    suspend fun add(t: TransactionEntity) = db.transactionDao().insert(t)
    suspend fun update(t: TransactionEntity) = db.transactionDao().update(t)
    suspend fun delete(t: TransactionEntity) = db.transactionDao().delete(t)
    suspend fun saveProfile(p: BankProfileEntity) = db.bankProfileDao().upsert(p)
}
