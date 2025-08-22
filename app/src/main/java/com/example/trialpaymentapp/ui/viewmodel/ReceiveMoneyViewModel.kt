package com.example.trialpaymentapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trialpaymentapp.data.Transaction
import com.example.trialpaymentapp.data.TransactionDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReceiveMoneyViewModel(private val dao: TransactionDao) : ViewModel() {

    private val _scannedDataFeedback = MutableStateFlow<String?>(null)
    val scannedDataFeedback: StateFlow<String?> = _scannedDataFeedback.asStateFlow()

    fun processScannedQrCode(qrData: String) {
        viewModelScope.launch {
            // Step 1: Decrypt data (if it's encrypted)
            // TODO: Implement actual data decryption from QR code.
            //       The encryption method should be securely agreed upon with the sending part of the app.
            val decryptedData = decryptData(qrData) // Placeholder for decryption

            // Step 2: Parse the decrypted data into a Transaction object
            val parsedTransaction = parseQrDataToTransaction(decryptedData)

            if (parsedTransaction != null) {
                // Step 3: Record the transaction as "RECEIVED"
                // The parsedTransaction contains details from the sender.
                // We now adapt it for the receiver's perspective.
                val receivedTransaction = parsedTransaction.copy(
                    id = 0, // Let Room auto-generate the ID for this new local record
                    type = "RECEIVED", // Mark this transaction as received
                    isSynced = false // Mark as unsynced initially, to be synced with backend later
                    // counterpartyId is already set by parseQrDataToTransaction from the QR data (e.g., sender's txId or deviceId)
                )
                dao.insertTransaction(receivedTransaction)
                _scannedDataFeedback.value = "Transaction Received! Amount: ${receivedTransaction.amount}, Details: ${receivedTransaction.details}"
            } else {
                _scannedDataFeedback.value = "Error: Invalid or unreadable QR code data."
            }
        }
    }

    // Placeholder for actual decryption.
    // In a real app, this would involve robust cryptographic methods.
    private fun decryptData(encryptedData: String): String {
        // TODO: Replace with real decryption logic.
        //       This current implementation is a placeholder and NOT secure.
        println("Decrypting QR Data: $encryptedData")
        // Example: If data is prefixed to indicate encryption
        if (encryptedData.startsWith("encrypted_")) {
            return encryptedData.removePrefix("encrypted_")
        }
        // If no recognizable encryption scheme, return as is or handle as an error.
        // For now, returning as is, assuming it might be unencrypted for testing.
        return encryptedData
    }

    // Parses the decrypted QR data string into a Transaction object.
    private fun parseQrDataToTransaction(decryptedData: String): Transaction? {
        println("Parsing Decrypted QR Data: $decryptedData")
        try {
            // Expected format (example): "amount=100.00;senderTxId=sender-uuid-123;details=Payment for goods;timestamp=1678886400000"
            // Keys are predefined: "amount", "senderTxId", "details", "timestamp"
            val parts = decryptedData.split(';').mapNotNull { part ->
                val keyValue = part.split('=', limit = 2)
                if (keyValue.size == 2) keyValue[0].trim() to keyValue[1].trim() else null
            }.toMap()

            val amount = parts["amount"]?.toDoubleOrNull()
            val senderTxId = parts["senderTxId"] // This will be the counterpartyId for the receiver
            val details = parts["details"] ?: decryptedData // Fallback to full string if no specific details field
            val timestamp = parts["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()

            return if (amount != null && senderTxId != null) {
                Transaction(
                    // These fields are from the sender's perspective or generic
                    type = "FROM_QR", // Temporary type, will be overridden to "RECEIVED"
                    amount = amount,
                    timestamp = timestamp,
                    details = details, // Specific details from QR or the whole decrypted string
                    counterpartyId = senderTxId, // ID of the sender or sender's transaction
                    isSynced = false // Default, will be confirmed by processScannedQrCode
                )
            } else {
                println("Error parsing QR data: Required fields (amount, senderTxId) not found or invalid.")
                null
            }
        } catch (e: Exception) {
            println("Exception during QR data parsing: $e")
            return null
        }
    }
}
