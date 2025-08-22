package com.example.trialpaymentapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
// Changed to a more common icon to avoid potential specific 'Sync' icon issues
import androidx.compose.material.icons.filled.Refresh 
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
// Keep this import, the issue is likely with Gradle dependencies, not the import itself
import androidx.lifecycle.viewmodel.compose.viewModel 

// Corrected imports for local project classes
import com.example.trialpaymentapp.data.Transaction
import com.example.trialpaymentapp.data.TransactionDao
import com.example.trialpaymentapp.ui.theme.TrialPaymentAppTheme
import com.example.trialpaymentapp.ui.viewmodel.ReceiveMoneyViewModel
import com.example.trialpaymentapp.ui.viewmodel.SendMoneyViewModel
import com.example.trialpaymentapp.ui.viewmodel.TransactionHistoryViewModel
// Removed unused import for PaymentApp as per analysis warning
// import com.example.trialpaymentapp.PaymentApp 
// Note: If PaymentApp is actually used (e.g. for Application context), this removal might need to be reverted
// and the usage of 'context.applicationContext as PaymentApp' checked.
// For now, assuming the warning was correct and it's not strictly needed for this file's compilation.

import java.text.SimpleDateFormat
import java.util.*

// Sealed class to represent different screens
sealed class Screen {
    object Home : Screen()
    object SendMoney : Screen()
    object ReceiveMoney : Screen()
    object TransactionHistory : Screen()
}

// Simple ViewModelFactory for ViewModels with TransactionDao dependency
@Suppress("UNCHECKED_CAST")
class BaseViewModelFactory(private val transactionDao: TransactionDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SendMoneyViewModel::class.java) -> {
                SendMoneyViewModel(transactionDao) as T
            }
            modelClass.isAssignableFrom(ReceiveMoneyViewModel::class.java) -> {
                ReceiveMoneyViewModel(transactionDao) as T
            }
            modelClass.isAssignableFrom(TransactionHistoryViewModel::class.java) -> {
                TransactionHistoryViewModel(transactionDao) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrialPaymentAppTheme {
                // TODO: Review if PaymentApp is needed here for Dao instantiation
                // If dao line below causes an error due to removed PaymentApp import,
                // that import needs to be restored and the app/build.gradle checked for PaymentApp's definition.
                PaymentAppContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentAppContent() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val context = LocalContext.current
    // TODO: Critical check: If 'PaymentApp' was correctly unused, how is 'dao' obtained?
    // This line will fail if 'PaymentApp' was indeed necessary for getting the Application context.
    // Assuming for now there's another way 'dao' is meant to be instantiated or it's provided differently.
    // If this line is the source of a new error, the 'PaymentApp' import and its usage
    // need to be reinstated and its own definition checked.
    // For a temporary fix to allow compilation if PaymentApp is an issue:
    // val dao = null // Placeholder - THIS WILL BREAK RUNTIME FUNCTIONALITY
    // A more robust solution would be to properly provide the Dao,
    // perhaps through a dependency injection framework or a correctly cast application context.
    // For now, assuming PaymentApp was correctly identified as unused by the analyzer.
    // The original line was:
    val dao = (context.applicationContext as? com.example.trialpaymentapp.PaymentApp)?.database?.transactionDao()
    // This ?. an as? is a safeguard. If PaymentApp is not available or the cast fails, dao will be null.
    // This will likely lead to runtime errors if dao is null and used.

    if (dao == null) {
        // Handle the case where DAO could not be initialized
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: Could not initialize database. App functionality will be limited.")
        }
        return // Early return to prevent further errors if DAO is null
    }

    val factory = BaseViewModelFactory(dao)

    val sendMoneyViewModel: SendMoneyViewModel = viewModel(factory = factory)
    val receiveMoneyViewModel: ReceiveMoneyViewModel = viewModel(factory = factory)
    val transactionHistoryViewModel: TransactionHistoryViewModel = viewModel(factory = factory)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentScreen) {
                            Screen.Home -> "Offline Pay"
                            Screen.SendMoney -> "Send Money"
                            Screen.ReceiveMoney -> "Receive Money"
                            Screen.TransactionHistory -> "Transaction History"
                        }
                    )
                },
                navigationIcon = {
                    if (currentScreen != Screen.Home) {
                        IconButton(onClick = {
                            if (currentScreen == Screen.SendMoney) {
                                sendMoneyViewModel.clearQrData() 
                            }
                            currentScreen = Screen.Home
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (currentScreen == Screen.TransactionHistory) {
                        IconButton(onClick = { transactionHistoryViewModel.syncUnsyncedTransactions() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Sync Transactions") // Changed to Refresh
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    onSendMoneyClicked = { currentScreen = Screen.SendMoney },
                    onReceiveMoneyClicked = { currentScreen = Screen.ReceiveMoney },
                    onTransactionHistoryClicked = { currentScreen = Screen.TransactionHistory }
                )
                Screen.SendMoney -> SendMoneyScreen(sendMoneyViewModel)
                Screen.ReceiveMoney -> ReceiveMoneyScreen(receiveMoneyViewModel)
                Screen.TransactionHistory -> TransactionHistoryScreen(transactionHistoryViewModel)
            }
        }
    }
}

@Composable
fun HomeScreen(
    onSendMoneyClicked: () -> Unit,
    onReceiveMoneyClicked: () -> Unit,
    onTransactionHistoryClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp), // Overall padding for the screen
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to Offline Pay",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp) // Space below the title
        )

        ElevatedButton(
            onClick = onSendMoneyClicked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp) // Padding for each button
                .height(56.dp) // Giving buttons a bit more height
        ) {
            Text("Send Money", style = MaterialTheme.typography.labelLarge)
        }
        ElevatedButton(
            onClick = onReceiveMoneyClicked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(56.dp)
        ) {
            Text("Receive Money", style = MaterialTheme.typography.labelLarge)
        }
        ElevatedButton(
            onClick = onTransactionHistoryClicked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(56.dp)
        ) {
            Text("View Transactions", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun SendMoneyScreen(viewModel: SendMoneyViewModel) {
    val amount by viewModel.amountInput.collectAsState()
    val pin by viewModel.pinInput.collectAsState()
    val encryptedQrString by viewModel.encryptedQrString.collectAsState()
    val transactionFeedback by viewModel.transactionFeedback.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = amount,
            onValueChange = { viewModel.updateAmount(it) },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = pin,
            onValueChange = { viewModel.updatePin(it) },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        ElevatedButton(
            onClick = { viewModel.prepareTransactionAndGenerateQr() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generate QR & Save Transaction")
        }

        transactionFeedback?.let { feedback ->
            Text(
                text = feedback,
                modifier = Modifier.padding(vertical = 8.dp),
                color = if (feedback.startsWith("Error:")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }

        encryptedQrString?.let { data ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Encrypted QR Data:", 
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = data, 
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "(This text is the encrypted data. Implement QR code image generation here.)",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top=8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReceiveMoneyScreen(viewModel: ReceiveMoneyViewModel) {
    val scannedDataFeedback by viewModel.scannedDataFeedback.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ElevatedButton(
            onClick = {
                viewModel.processScannedQrCode("encrypted_amount=50.0;senderTxId=test-sender-123;details=Test Payment;timestamp=1678886400000")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan QR Code (Simulated)")
        }
        scannedDataFeedback?.let { feedback ->
            Text(
                text = feedback,
                modifier = Modifier.padding(top = 16.dp),
                color = if (feedback.startsWith("Error:")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun TransactionHistoryScreen(viewModel: TransactionHistoryViewModel) {
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())

    if (transactions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("No transactions yet.")
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(transactions) { transaction ->
                TransactionListItem(transaction = transaction)
            }
        }
    }
}

@Composable
fun TransactionListItem(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Type: ${transaction.type}", style = MaterialTheme.typography.titleMedium)
            Text("Amount: ${transaction.amount}", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Date: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(transaction.timestamp))}",
                style = MaterialTheme.typography.bodySmall
            )
            Text("Details: ${transaction.details}", style = MaterialTheme.typography.bodySmall)
            Text("Synced: ${transaction.isSynced}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    TrialPaymentAppTheme {
        HomeScreen({}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
fun SendMoneyScreenPreview() {
    TrialPaymentAppTheme {
        Column(modifier = Modifier.padding(16.dp)){
            OutlinedTextField(value = "100", onValueChange = {}, label={Text("Amount")})
            OutlinedTextField(value = "1234", onValueChange = {}, label={Text("PIN")})
            Button(onClick={}){ Text("Generate QR & Save")}
            Text("Encrypted QR Data: encrypted_amount=100...")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionHistoryScreenPreview() {
    TrialPaymentAppTheme {
        val previewTransactions = listOf(
            Transaction(0, "SENT", 100.0, System.currentTimeMillis(), "details1", "receiver1", false),
            Transaction(0, "RECEIVED", 50.0, System.currentTimeMillis() - 100000, "details2", "sender1", true)
        )
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(previewTransactions) { transaction ->
                TransactionListItem(transaction)
            }
        }
    }
}
