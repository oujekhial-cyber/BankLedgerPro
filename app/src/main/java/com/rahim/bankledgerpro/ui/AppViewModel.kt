package com.rahim.bankledgerpro.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahim.bankledgerpro.data.*
import com.rahim.bankledgerpro.sms.BankSmsParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class Screen { DASHBOARD, TRANSACTIONS, BANKS, SETTINGS }

class AppViewModel(private val context: Context) : ViewModel() {
    private val db = AppDatabase.get(context)
    private val repo = BankRepository(db)

    val transactions = repo.transactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val banks = repo.profiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var screen by mutableStateOf(Screen.DASHBOARD)
    var currency by mutableStateOf("تومان")
    var pending by mutableStateOf<TransactionEntity?>(null)
        private set

    init {
        checkPending()
    }

    fun checkPending() {
        val p = context.getSharedPreferences("pending_sms", Context.MODE_PRIVATE)
        val amount = p.getLong("amount", -1L)
        if (amount > 0) {
            pending = TransactionEntity(
                amount = amount,
                type = p.getString("type", "OUT") ?: "OUT",
                bank = p.getString("bank", "نامشخص") ?: "نامشخص",
                description = "",
                timestamp = System.currentTimeMillis(),
                balanceAfter = p.getLong("balance", -1L).takeIf { it >= 0 },
                rawSms = p.getString("body", null)
            )
        }
    }

    fun consumePending() {
        context.getSharedPreferences("pending_sms", Context.MODE_PRIVATE).edit().clear().apply()
        pending = null
    }

    fun add(amount: Long, type: String, bank: String, description: String, balance: Long? = null, raw: String? = null) {
        viewModelScope.launch {
            repo.add(TransactionEntity(amount = amount, type = type, bank = bank, description = description, timestamp = System.currentTimeMillis(), balanceAfter = balance, rawSms = raw))
        }
    }

    fun update(t: TransactionEntity) = viewModelScope.launch { repo.update(t) }
    fun delete(t: TransactionEntity) = viewModelScope.launch { repo.delete(t) }

    fun saveBank(name: String, sample: String) = viewModelScope.launch {
        repo.saveProfile(BankProfileEntity(name, sample))
    }

    fun detectedBanks(): List<String> = BankSmsParser.knownBanks()
}
