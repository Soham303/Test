package com.example.trialpaymentapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // e.g., \"SENT\", \"RECEIVED\"
    val amount: Double,
    val timestamp: Long,
    val details: String, // Could be QR data, notes, etc.
    val counterpartyId: String, // Identifier for the other party, if applicable
    var isSynced: Boolean = false
)
