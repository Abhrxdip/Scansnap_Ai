package com.smartvendor.ai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.smartvendor.ai.model.Bill
import com.smartvendor.ai.repository.SalesRepository
import com.smartvendor.ai.repository.SalesRepositoryImpl
import com.smartvendor.ai.ui.components.NeuBadge
import com.smartvendor.ai.ui.components.NeuButton
import com.smartvendor.ai.ui.components.NeuCard
import com.smartvendor.ai.ui.components.neuShadow
import com.smartvendor.ai.ui.theme.*
import com.smartvendor.ai.utils.SmsUtils
import com.smartvendor.ai.utils.WhatsAppUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    salesRepository: SalesRepository = remember { SalesRepositoryImpl() },
    onNavigateBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var bills by remember { mutableStateOf<List<Bill>>(emptyList()) }
    var selectedBillForDetail by remember { mutableStateOf<Bill?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        salesRepository.getSalesHistoryStream().collect { list ->
            bills = list.filter { it.status == Bill.BILL_STATUS_COMPLETED }
            isLoading = false
        }
    }

    val filteredBills = remember(searchQuery, bills) {
        if (searchQuery.isBlank()) bills
        else bills.filter {
            it.billId.contains(searchQuery, ignoreCase = true) ||
                    it.paymentMethod.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeuSurface)
                    .border(BorderStroke(2.5.dp, NeuBlack))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .neuShadow(2.dp, 2.dp, NeuBlack, 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeuSurface)
                                .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(8.dp))
                                .clickable { onNavigateBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = NeuBlack,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Sales Invoices",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = NeuBlack
                            )
                            Text(
                                text = "Past Completed Transactions",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = NeuGray
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .neuShadow(2.dp, 2.dp, NeuBlack, 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeuYellow)
                            .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(8.dp))
                            .clickable { refreshTrigger++ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NeuBlack, modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
        containerColor = NeuBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Search Field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neuShadow(3.dp, 3.dp, NeuBlack, 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeuSurface)
                        .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = NeuBlack, modifier = Modifier.size(20.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by Bill ID or Payment Mode...", fontSize = 13.sp, color = NeuGray) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeuBlue)
                    }
                } else if (filteredBills.isNotEmpty()) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredBills, key = { it.billId }) { bill ->
                            BillHistoryCard(
                                bill = bill,
                                onClick = { selectedBillForDetail = bill }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        NeuCard(
                            modifier = Modifier.padding(24.dp),
                            backgroundColor = NeuSurface,
                            shadowOffset = 4.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(text = "🧾", fontSize = 48.sp)
                                Text(
                                    text = "No Invoices Recorded Yet",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = NeuBlack
                                )
                                Text(
                                    text = "Completed checkouts from the POS register will appear here.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = NeuGray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                NeuButton(
                                    text = "🔄 Refresh History",
                                    onClick = { refreshTrigger++ },
                                    backgroundColor = NeuYellow,
                                    textColor = NeuBlack
                                )
                            }
                        }
                    }
                }
            }

            selectedBillForDetail?.let { bill ->
                BillDetailDialog(
                    bill = bill,
                    onDismiss = { selectedBillForDetail = null }
                )
            }
        }
    }
}

@Composable
fun BillHistoryCard(
    bill: Bill,
    onClick: () -> Unit
) {
    NeuCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        backgroundColor = NeuSurface,
        shadowOffset = 3.dp,
        cornerRadius = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Invoice #${bill.billId.takeLast(8)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = NeuBlack
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${bill.items.sumOf { it.quantity }} items billed",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = NeuGray
                )
                Spacer(modifier = Modifier.height(6.dp))
                NeuBadge(
                    text = "MODE: ${bill.paymentMethod.uppercase()}",
                    backgroundColor = when (bill.paymentMethod.uppercase()) {
                        "CASH" -> NeuGreen
                        "UPI" -> NeuYellow
                        else -> NeuBlue
                    },
                    textColor = if (bill.paymentMethod.uppercase() == "CARD") Color.White else NeuBlack,
                    shadowOffset = 1.dp
                )
            }

            Text(
                text = "₹${"%.2f".format(bill.grandTotal)}",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = NeuBlue
            )
        }
    }
}

@Composable
fun BillDetailDialog(
    bill: Bill,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var phoneInput by remember { mutableStateOf("") }
    val dailySmsCount = remember { SmsUtils.getDailySmsCount(context) }
    val isLimitReached = dailySmsCount >= SmsUtils.DAILY_SMS_LIMIT

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val sent = SmsUtils.sendSilentSmsReceipt(context, phoneInput, bill)
            if (sent) onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeuSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .border(BorderStroke(3.dp, NeuBlack), RoundedCornerShape(16.dp))
            .neuShadow(6.dp, 6.dp, NeuBlack, 16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "🧾", fontSize = 20.sp)
                Text("Digital Invoice #${bill.billId.takeLast(8)}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = NeuBlack)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                NeuBadge(
                    text = "PAID VIA ${bill.paymentMethod.uppercase()}",
                    backgroundColor = NeuGreen,
                    textColor = NeuBlack
                )

                HorizontalDivider(thickness = 2.dp, color = NeuBlack)

                bill.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.name} x${item.quantity}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = NeuBlack
                        )
                        Text(
                            text = "₹${"%.2f".format(item.lineTotal)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = NeuBlue
                        )
                    }
                }

                HorizontalDivider(thickness = 2.dp, color = NeuBlack)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Grand Total", fontWeight = FontWeight.Black, fontSize = 16.sp, color = NeuBlack)
                    Text("₹${"%.2f".format(bill.grandTotal)}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = NeuBlue)
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Resend to Mobile", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("9876543210") },
                    leadingIcon = { Text("🇮🇳 +91 ", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Black) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                NeuButton(
                    text = "💬 Share on WhatsApp",
                    onClick = {
                        if (phoneInput.length >= 10) {
                            WhatsAppUtils.sendWhatsAppBill(context, phoneInput, bill)
                            onDismiss()
                        }
                    },
                    enabled = phoneInput.length >= 10,
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = NeuGreen,
                    textColor = NeuBlack,
                    shadowOffset = 2.dp
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Close", fontWeight = FontWeight.Black, color = NeuBlack)
            }
        }
    )
}
