package com.example.trialpaymentapp

import android.graphics.Bitmap // Required for qrBitmap state
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image // Required for Image composable
import androidx.compose.foundation.BorderStroke // Required for BorderStroke class
import androidx.compose.foundation.border // Required for border on Image/Box
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Required for BorderStroke color
import androidx.compose.ui.graphics.asImageBitmap // Required to convert Bitmap to ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign // For text alignment
import androidx.compose.ui.text.style.TextOverflow // For text overflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trialpaymentapp.data.Transaction
import com.example.trialpaymentapp.data.TransactionDao
import com.example.trialpaymentapp.ui.theme.TrialPaymentAppTheme
import com.example.trialpaymentapp.ui.viewmodel.ReceiveMoneyViewModel
import com.example.trialpaymentapp.ui.viewmodel.SendMoneyViewModel
import com.example.trialpaymentapp.ui.viewmodel.TransactionHistoryViewModel
// Ensure QrUtils is imported
import com.example.trialpaymentapp.QrUtils 

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
    val dao = (context.applicationContext as? com.example.trialpaymentapp.PaymentApp)?.database?.transactionDao()

    if (dao == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: Could not initialize database. App functionality will be limited.")
        }
        return
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
                            Icon(Icons.Filled.Refresh, contentDescription = "Sync Transactions")
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
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to Offline Pay",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        ElevatedButton(
            onClick = onSendMoneyClicked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(56.dp)
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
        verticalArrangement = Arrangement.spacedBy(16.dp) // Adds space between direct children
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
                modifier = Modifier.padding(vertical = 8.dp), // Consistent padding
                color = if (feedback.startsWith("Error:")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }

        encryptedQrString?.let { data ->
            // This Spacer provides space above the QR code section if transactionFeedback is also present
            Spacer(modifier = Modifier.height(8.dp)) 
            
            Text(
                text = "Scan QR Code:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Generate QR Code Bitmap when encryptedQrString is available
            val qrBitmap: Bitmap? by remember(data) { // Use data as key for remember
                derivedStateOf {
                    QrUtils.generateQrCodeBitmap(text = data, width = 200, height = 200)
                }
            }

            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap!!.asImageBitmap(),
                    contentDescription = "Transaction QR Code",
                    modifier = Modifier
                        .size(200.dp)
                        .align(Alignment.CenterHorizontally)
                        .border(BorderStroke(1.dp, Color.Gray))
                )
            } else {
                // Fallback if bitmap is null (e.g., error in generation, or still processing)
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .border(BorderStroke(1.dp, Color.LightGray))
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp), // Padding inside the box
                    contentAlignment = Alignment.Center
                ) {
                    Text("Generating QR Code...", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Encrypted Data:",
                style = MaterialTheme.typography.titleSmall, // Changed for visual hierarchy
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = data,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodySmall, // Smaller for the raw data
                textAlign = TextAlign.Center,
                maxLines = 3, // Prevent very long strings from taking too much space
                overflow = TextOverflow.Ellipsis // Add ellipsis for overflow
            )
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
                // Example of a more complete payload for testing ReceiveMoneyScreen
                viewModel.processScannedQrCode("amount=50.0;senderTxId=test-sender-123;details=Test Payment by QR;timestamp=1678886400000;securityKey=someSecureString")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan QR Code (Simulated)")
        }
        scannedDataFeedback?.let { feedback ->
            Text(
                text = feedback,
                modifier = Modifier.padding(top = 16.dp),
                color = if (feedback.startsWith("Error:")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
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
        // Updated preview to reflect more of the SendMoneyScreen structure
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(value = "100", onValueChange = {}, label={Text("Amount")}, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = "1234", onValueChange = {}, label={Text("PIN")}, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Button(onClick={}, modifier = Modifier.fillMaxWidth()){ Text("Generate QR & Save Transaction")}
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("Scan QR Code:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.size(200.dp).border(BorderStroke(1.dp, Color.Gray)).padding(16.dp), contentAlignment = Alignment.Center) {
                Text("QR Code Area (Preview)")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Encrypted Data:", style = MaterialTheme.typography.titleSmall)
            Text("encrypted_data_string_for_preview_123...", textAlign = TextAlign.Center)
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
