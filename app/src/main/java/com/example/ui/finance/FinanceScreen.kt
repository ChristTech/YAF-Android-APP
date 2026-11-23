package com.example.ui.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FinancialRecordEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.model.FinancialCategory
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.ui.components.ModernEmptyState
import com.example.ui.theme.FlameGoldAccent
import com.example.ui.theme.FlameRedPrimary
import com.example.ui.theme.GlobeBlack
import com.example.ui.theme.ShieldSilver
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    viewModel: FinanceViewModel,
    session: SessionEntity,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val myRecords by viewModel.myRecords.collectAsState()
    val allRecords by viewModel.allRecords.collectAsState()
    val myTotal by viewModel.myTotalCompleted.collectAsState()
    val globalTotal by viewModel.globalTotalCompleted.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "NG")) }
    val displayRecords = if (uiState.showExecutiveView && session.userRole.canViewAllFinances()) {
        allRecords
    } else {
        myRecords
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setGiveModalOpen(true) },
                containerColor = FlameRedPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("record_giving_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Give / Pledge")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Stats Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GlobeBlack),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(FlameRedPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Paid, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Financial Commitments & Giving",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "Tithes, faith pledges, project dues & donations",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Stats Summary Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Your Total Giving",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "₦%,.2f".format(myTotal ?: 0.0),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }
                            }

                            if (session.userRole.canViewAllFinances()) {
                                Surface(
                                    color = FlameRedPrimary.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "The Gideonites - Youth Alive Total",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "₦%,.2f".format(globalTotal ?: 0.0),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = FlameGoldAccent
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Official Bank Details Card
            item {
                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = FlameRedPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Official Bank Account Details",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = GlobeBlack
                                )
                            }
                            TextButton(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString("8135711999"))
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Account number 8135711999 copied to clipboard")
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("btn_copy_account_number")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Account Number",
                                    tint = FlameRedPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Copy",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = FlameRedPrimary
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Account Number: 8135711999\nBank Name: Opay\nAccount Name: Akande Peace Olubunmi",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Executive Toggle Tab (if leadership)
            if (session.userRole.canViewAllFinances()) {
                item {
                    TabRow(
                        selectedTabIndex = if (uiState.showExecutiveView) 1 else 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = FlameRedPrimary
                    ) {
                        Tab(
                            selected = !uiState.showExecutiveView,
                            onClick = { viewModel.setExecutiveView(false) },
                            text = { Text("My Giving History") }
                        )
                        Tab(
                            selected = uiState.showExecutiveView,
                            onClick = { viewModel.setExecutiveView(true) },
                            text = { Text("All Fellowship Records") }
                        )
                    }
                }
            }

            // Transaction History List
            if (displayRecords.isEmpty()) {
                item {
                    ModernEmptyState(
                        icon = Icons.Default.ReceiptLong,
                        title = "No Contributions Logged Yet",
                        subtitle = "Welfare dues, project contributions, and love offerings will appear here once recorded and verified.",
                        accentColor = FlameGoldAccent
                    )
                }
            } else {
                items(displayRecords, key = { it.id }) { record ->
                    FinancialRecordCard(
                        record = record,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    if (uiState.isGiveModalOpen) {
        RecordGivingDialog(
            onDismiss = { viewModel.setGiveModalOpen(false) },
            onSubmit = { amount, category, method, isPledge, notes ->
                viewModel.recordGiving(amount, category, method, isPledge, notes)
            }
        )
    }
}

@Composable
fun FinancialRecordCard(
    record: FinancialRecordEntity,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.category.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = GlobeBlack
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Ref: ${record.referenceCode} • ${record.paymentMethod.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${record.userName} • ${dateFormat.format(Date(record.dateMillis))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (record.purposeOrNotes.isNotBlank()) {
                    Text(
                        text = record.purposeOrNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₦%,.2f".format(record.amount),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = FlameRedPrimary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = when (record.status) {
                        PaymentStatus.COMPLETED -> Color(0xFF10B981)
                        PaymentStatus.PLEDGED -> FlameGoldAccent
                        PaymentStatus.PENDING_VERIFICATION -> ShieldSilver
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = record.status.name,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordGivingDialog(
    onDismiss: () -> Unit,
    onSubmit: (
        amount: Double,
        category: FinancialCategory,
        method: PaymentMethod,
        isPledge: Boolean,
        notes: String
    ) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(FinancialCategory.TITHE) }
    var method by remember { mutableStateOf(PaymentMethod.BANK_TRANSFER) }
    var isPledge by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var methodDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isPledge) "Record Faith Pledge" else "Record Giving / Contribution",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Amount (NGN) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = category.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        FinancialCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName) },
                                onClick = {
                                    category = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                ExposedDropdownMenuBox(
                    expanded = methodDropdownExpanded,
                    onExpandedChange = { methodDropdownExpanded = !methodDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = method.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method / Channel") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = methodDropdownExpanded,
                        onDismissRequest = { methodDropdownExpanded = false }
                    ) {
                        PaymentMethod.values().forEach { meth ->
                            DropdownMenuItem(
                                text = { Text(meth.displayName) },
                                onClick = {
                                    method = meth
                                    methodDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isPledge,
                        onCheckedChange = { isPledge = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "This is a future Faith Pledge commitment")
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Purpose / Bank Transaction Reference") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = amountText.toDoubleOrNull() ?: 0.0
                    onSubmit(parsed, category, method, isPledge, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary)
            ) {
                Text("Submit Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
