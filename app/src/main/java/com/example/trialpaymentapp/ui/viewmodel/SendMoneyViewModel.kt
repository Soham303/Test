package com.example.trialpaymentapp.ui.viewmodel

import android.app.Application // Added
import androidx.lifecycle.AndroidViewModel // Changed from ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trialpaymentapp.PinManager // Added
import com.example.trialpaymentapp.data.Transaction
import com.example.trialpaymentapp.data.TransactionDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

// Changed to AndroidViewModel to get Application context
class SendMoneyViewModel(private val application: Application, private val transactionDao: TransactionDao) : AndroidViewModel(application) {
    private val pinManager = PinManager(application) // Added PinManager instance

    private val _amountInput = MutableStateFlow("")
    val amountInput: StateFlow<String> = _amountInput.asStateFlow()

    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    private val _encryptedQrString = MutableStateFlow<String?>(null)
    val encryptedQrString: StateFlow<String?> = _encryptedQrString.asStateFlow()

    private val _transactionFeedback = MutableStateFlow<String?>(null)
    val transactionFeedback: StateFlow<String?> = _transactionFeedback.asStateFlow()

    init {
        // TEMPORARY: For testing PIN verification.
        // In a real app, you need a proper PIN setup UI.
        if (!pinManager.isPinSet()) {
            pinManager.setPin("1234") // Set a default test PIN
            // Log.d("SendMoneyViewModel", "Default PIN '1234' set for testing as no PIN was found.")
        }
    }

    fun updateAmount(amount: String) {
        _amountInput.value = amount
        _encryptedQrString.value = null
        _transactionFeedback.value = null
    }

    fun updatePin(pin: String) {
        _pinInput.value = pin
        _encryptedQrString.value = null
        _transactionFeedback.value = null
    }

    fun prepareTransactionAndGenerateQr() {
        val currentAmountStr = _amountInput.value
        val currentPin = _pinInput.value

        if (currentAmountStr.isBlank() || currentPin.isBlank()) {
            _transactionFeedback.value = "Error: Amount and PIN cannot be empty."
            _encryptedQrString.value = null
            return
        }

        val amount = currentAmountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _transactionFeedback.value = "Error: Invalid amount."
            _encryptedQrString.value = null
            return
        }

        // --- PIN Verification Logic ---
        if (!pinManager.isPinSet()) {
            // This case should ideally not be hit if we auto-set a PIN in init for testing.
            _transactionFeedback.value = "Error: App PIN not set up. Please set up a PIN first."
            _encryptedQrString.value = null
            return
        }

        if (!pinManager.verifyPin(currentPin)) {
            _transactionFeedback.value = "Error: Invalid PIN entered."
            _encryptedQrString.value = null
            _pinInput.value = "" // Clear PIN input on failure
            return
        }
        // --- End of PIN Verification Logic ---

        // If PIN is correct, proceed with transaction logic
        val transactionId = UUID.randomUUID().toString()
        val currentTime = System.currentTimeMillis()
        val qrDetails = "Payment"

        val qrDataPayload = "amount=$amount;senderTxId=$transactionId;details=$qrDetails;timestamp=$currentTime"
        val encryptedData = encryptData(qrDataPayload) // Using your existing encryptData placeholder

        val transaction = Transaction(
            id = 0,
            type = "SENT",
            amount = amount,
            timestamp = currentTime,
            details = "To QR: $qrDetails (ID: $transactionId)",
            counterpartyId = transactionId,
            isSynced = false
        )

        viewModelScope.launch {
            transactionDao.insertTransaction(transaction)
            _encryptedQrString.value = encryptedData
            _transactionFeedback.value = "PIN Verified. Transaction ready. Show QR code." // Updated feedback
            // Clear inputs after successful operation
            _amountInput.value = ""
            _pinInput.value = ""
        }
    }

    // Placeholder for actual encryption - this was already in your ViewModel
    private fun encryptData(data: String): String {
        println("Encrypting QR Data for Send: $data")
        return "encrypted_$data" // Simulate encryption
    }

    fun clearQrData() {
        _encryptedQrString.value = null
        _transactionFeedback.value = null
    }

    // isValidPin function is no longer needed as PinManager handles verification.
    // If you had complex local format validation for the PIN field before verifying,
    // you could add a simpler form of it back, but PinManager now does the core check.
}
