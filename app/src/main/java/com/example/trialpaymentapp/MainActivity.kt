package com.example.trialpaymentapp

import android.app.Application // Ensure this import is present
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.trialpaymentapp.QrUtils
import java.text.SimpleDateFormat
import java.util.*

sealed class Screen {
    object Home : Screen()
    object SendMoney : Screen()
    object ReceiveMoney : Screen()
    object TransactionHistory : Screen()
}

@Suppress("UNCHECKED_CAST")
class BaseViewModelFactory(
    private val app: Application,
    private val transactionDaoParam: TransactionDao // Renamed for clarity and specific use
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SendMoneyViewModel::class.java) -> {
                SendMoneyViewModel(app, transactionDao = transactionDaoParam) as T
            }
            modelClass.isAssignableFrom(ReceiveMoneyViewModel::class.java) -> {
                // CORRECTED: Parameter name is 'transactionDao' in ReceiveMoneyViewModel
                ReceiveMoneyViewModel(transactionDao = transactionDaoParam) as T
            }
            modelClass.isAssignableFrom(TransactionHistoryViewModel::class.java) -> {
                // CORRECTED: Parameter name is 'transactionDao' in TransactionHistoryViewModel
                TransactionHistoryViewModel(transactionDao = transactionDaoParam) as T
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
    val applicationContext = context.applicationContext as Application // Explicitly get Application context

    val transactionDao = (applicationContext as? com.example.trialpaymentapp.PaymentApp)
        ?.database
        ?.transactionDao()

    if (transactionDao == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: Could not initialize database. App functionality will be limited.")
        }
        return
    }

    // Instantiate factory: Pass Application context first, then the transactionDao
    // This maps the local 'transactionDao' to the factory's 'transactionDaoParam'
    val factory = BaseViewModelFactory(applicationContext, transactionDaoParam = transactionDao)

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

// ... (Rest of your MainActivity.kt code: HomeScreen, SendMoneyScreen, etc. remain unchanged by this fix)
// Make sure SendMoneyScreen, ReceiveMoneyScreen, TransactionHistoryScreen, etc. are below

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
                color = if (feedback.startsWith("Error:")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }

        encryptedQrString?.let { data ->
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Scan QR Code:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(8.dp))

            val qrBitmap: Bitmap? by remember(data) {
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
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .border(BorderStroke(1.dp, Color.LightGray))
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Generating QR Code...", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Encrypted Data:",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = data,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
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
        val context = LocalContext.current
        Text("Preview for SendMoneyScreen - ViewModel requires context")
    }
}
