package com.example.trialpaymentapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trialpaymentapp.data.Transaction
import com.example.trialpaymentapp.data.TransactionDao
// Placeholder for actual Supabase Sync Service
// import com.example.trialpaymentapp.network.SupabaseSyncService 
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionHistoryViewModel(private val dao: TransactionDao) : ViewModel() {

    val transactions: StateFlow<List<Transaction>> = dao.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Placeholder for SupabaseSyncService - uncomment and implement when ready
    // private val supabaseSyncService = SupabaseSyncService() 

    fun syncUnsyncedTransactions() {
        viewModelScope.launch {
            val unsynced = dao.getUnsyncedTransactions()
            if (unsynced.isNotEmpty()) {
                // Placeholder: In a real app, call SupabaseSyncService
                // val success = supabaseSyncService.syncTransactions(unsynced)
                // if (success) { // 
                println("Simulating sync for: ${unsynced.size} transactions")
                unsynced.forEach { transaction ->
                    dao.updateTransactionSynced(transaction.id, true)
                }
                // }
            }
        }
    }
}
