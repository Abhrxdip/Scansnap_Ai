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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Payments
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartvendor.ai.model.Bill
import com.smartvendor.ai.model.BillItem
import com.smartvendor.ai.ui.components.NeuBadge
import com.smartvendor.ai.ui.components.NeuButton
import com.smartvendor.ai.ui.components.NeuCard
import com.smartvendor.ai.ui.components.neuShadow
import com.smartvendor.ai.ui.theme.*
import com.smartvendor.ai.utils.SmsUtils
import com.smartvendor.ai.utils.WhatsAppUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    billId: String,
    viewModel: BillingViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onStartNewBill: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDigitalReceiptDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = billId) {
        viewModel.loadBill(billId)
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
                                text = "POS Checkout",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = NeuBlack
                            )
                            Text(
                                text = "Bill ID: ${billId.take(12)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = NeuGray
                            )
                        }
                    }

                    NeuBadge(
                        text = "ACTIVE CART",
                        backgroundColor = NeuYellow,
                        textColor = NeuBlack
                    )
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
            val bill = uiState.bill

            if (uiState.checkoutSuccess && bill != null) {
                // Checkout Success View
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NeuCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = NeuSurface,
                        shadowOffset = 6.dp,
                        cornerRadius = 16.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            NeuBadge(
                                text = "✓ TRANSACTION COMPLETED",
                                backgroundColor = NeuGreen,
                                textColor = NeuBlack,
                                shadowOffset = 2.dp
                            )

                            Text(
                                text = "Checkout Successful!",
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                color = NeuBlack
                            )

                            Text(
                                text = "Total Billed: ₹${"%.2f".format(bill.grandTotal)}  •  ${bill.paymentMethod.uppercase()}",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = NeuBlue
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // 1. Send Digital Receipt
                            NeuButton(
                                text = "📱 Send Digital Receipt",
                                onClick = { showDigitalReceiptDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = NeuBlue,
                                textColor = Color.White
                            )

                            // 2. Scan New Bill
                            NeuButton(
                                text = "⚡ Scan Next Customer",
                                onClick = onStartNewBill,
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = NeuGreen,
                                textColor = NeuBlack
                            )

                            // 3. Return to Dashboard
                            NeuButton(
                                text = "🏠 Return to Dashboard",
                                onClick = onNavigateToDashboard,
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = NeuSurface,
                                textColor = NeuBlack
                            )
                        }
                    }
                }
            } else if (bill != null && bill.items.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(bill.items, key = { it.productId }) { item ->
                            BillItemRow(
                                item = item,
                                onIncrease = { viewModel.increaseItemQuantity(item.productId) },
                                onDecrease = { viewModel.decreaseItemQuantity(item.productId) },
                                onDelete = { viewModel.removeItem(item.productId) }
                            )
                        }
                    }

                    // Checkout & Summary Footer
                    BillingFooterCard(
                        subtotal = bill.subtotal,
                        gst = bill.gst,
                        discount = bill.discount,
                        grandTotal = bill.grandTotal,
                        selectedPaymentMethod = uiState.selectedPaymentMethod,
                        isProcessing = uiState.isProcessingCheckout,
                        onPaymentMethodSelect = { viewModel.setPaymentMethod(it) },
                        onCheckout = {
                            viewModel.performCheckout {}
                        }
                    )
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
                            Text(text = "🛒", fontSize = 48.sp)
                            Text(
                                text = "Current Bill is Empty",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = NeuBlack
                            )
                            Text(
                                text = "Point camera or use barcode scanner to add products.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NeuGray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            NeuButton(
                                text = "⚡ Open AI Scanner",
                                onClick = onNavigateBack,
                                backgroundColor = NeuBlue,
                                textColor = Color.White
                            )
                        }
                    }
                }
            }

            if (showDigitalReceiptDialog && uiState.bill != null) {
                DigitalReceiptDialog(
                    bill = uiState.bill!!,
                    onDismiss = { showDigitalReceiptDialog = false },
                    onSendSms = { phone ->
                        val sent = SmsUtils.sendSilentSmsReceipt(context, phone, uiState.bill!!)
                        if (sent) showDigitalReceiptDialog = false
                    },
                    onSendWhatsApp = { phone ->
                        WhatsAppUtils.sendWhatsAppBill(context, phone, uiState.bill!!)
                        showDigitalReceiptDialog = false
                    }
                )
            }

            if (uiState.errorMessage != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = NeuRed,
                    contentColor = Color.White,
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("DISMISS", color = Color.White, fontWeight = FontWeight.Black)
                        }
                    }
                ) {
                    Text(uiState.errorMessage!!, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BillItemRow(
    item: BillItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onDelete: () -> Unit
) {
    NeuCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NeuSurface,
        shadowOffset = 3.dp,
        cornerRadius = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = NeuBlack
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "₹${"%.2f".format(item.unitPrice)} each  •  GST ${item.gst}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = NeuGray
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .neuShadow(1.5.dp, 1.5.dp, NeuBlack, 6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeuSurface)
                        .border(BorderStroke(1.8.dp, NeuBlack), RoundedCornerShape(6.dp))
                        .clickable { onDecrease() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "−", fontWeight = FontWeight.Black, fontSize = 16.sp, color = NeuBlack)
                }

                Text(
                    text = "${item.quantity}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = NeuBlack
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .neuShadow(1.5.dp, 1.5.dp, NeuBlack, 6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeuSurface)
                        .border(BorderStroke(1.8.dp, NeuBlack), RoundedCornerShape(6.dp))
                        .clickable { onIncrease() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "+", fontWeight = FontWeight.Black, fontSize = 16.sp, color = NeuBlack)
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "₹${"%.2f".format(item.lineTotal)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = NeuBlue
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .neuShadow(1.5.dp, 1.5.dp, NeuBlack, 6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFFE4E6))
                        .border(BorderStroke(1.8.dp, NeuBlack), RoundedCornerShape(6.dp))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = NeuRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun BillingFooterCard(
    subtotal: Double,
    gst: Double,
    discount: Double,
    grandTotal: Double,
    selectedPaymentMethod: String,
    isProcessing: Boolean,
    onPaymentMethodSelect: (String) -> Unit,
    onCheckout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NeuSurface)
            .border(BorderStroke(3.dp, NeuBlack))
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "PAYMENT METHOD",
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                color = NeuBlack,
                letterSpacing = 0.5.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("CASH", "UPI", "CARD").forEach { mode ->
                    val isSelected = selectedPaymentMethod.equals(mode, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .neuShadow(
                                offsetX = if (isSelected) 3.dp else 1.5.dp,
                                offsetY = if (isSelected) 3.dp else 1.5.dp,
                                cornerRadius = 8.dp
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) {
                                    when (mode) {
                                        "CASH" -> NeuGreen
                                        "UPI" -> NeuYellow
                                        else -> NeuBlue
                                    }
                                } else NeuSurface
                            )
                            .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(8.dp))
                            .clickable { onPaymentMethodSelect(mode) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = if (isSelected && mode == "CARD") Color.White else NeuBlack
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 2.dp, color = NeuBlack)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtotal", fontWeight = FontWeight.Bold, color = NeuGray, fontSize = 13.sp)
                Text("₹${"%.2f".format(subtotal)}", fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("GST Tax", fontWeight = FontWeight.Bold, color = NeuGray, fontSize = 13.sp)
                Text("₹${"%.2f".format(gst)}", fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
            if (discount > 0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Discount", fontWeight = FontWeight.Bold, color = NeuGreen, fontSize = 13.sp)
                    Text("-₹${"%.2f".format(discount)}", fontWeight = FontWeight.Black, color = NeuGreen, fontSize = 13.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("GRAND TOTAL", fontWeight = FontWeight.Black, fontSize = 16.sp, color = NeuBlack)
                Text(
                    text = "₹${"%.2f".format(grandTotal)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = NeuBlue
                )
            }

            NeuButton(
                text = if (isProcessing) "Processing..." else "⚡ Complete Checkout",
                onClick = onCheckout,
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = NeuBlue,
                textColor = Color.White
            )
        }
    }
}

@Composable
fun DigitalReceiptDialog(
    bill: Bill,
    onDismiss: () -> Unit,
    onSendSms: (String) -> Unit,
    onSendWhatsApp: (String) -> Unit
) {
    val context = LocalContext.current
    var phoneInput by remember { mutableStateOf("") }
    val dailySmsCount = remember { SmsUtils.getDailySmsCount(context) }
    val isLimitReached = dailySmsCount >= SmsUtils.DAILY_SMS_LIMIT

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onSendSms(phoneInput)
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
                Text(text = "📱", fontSize = 20.sp)
                Text("Send Digital Receipt", fontWeight = FontWeight.Black, fontSize = 18.sp, color = NeuBlack)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Enter customer phone number to dispatch digital bill:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = NeuGray
                )

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Mobile Number", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("e.g. 9876543210") },
                    leadingIcon = { Text("🇮🇳 +91 ", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Black) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isLimitReached) {
                    NeuCard(
                        backgroundColor = Color(0xFFFFE4E6),
                        shadowOffset = 2.dp,
                        cornerRadius = 8.dp
                    ) {
                        Text(
                            text = "⚠️ Daily free SMS limit reached. Send via WhatsApp below 💬",
                            modifier = Modifier.padding(8.dp),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = NeuRed
                        )
                    }
                }

                NeuButton(
                    text = "🚀 Send Silent SMS",
                    onClick = {
                        if (phoneInput.length >= 10) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                                onSendSms(phoneInput)
                            } else {
                                smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                            }
                        }
                    },
                    enabled = !isLimitReached && phoneInput.length >= 10,
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = NeuYellow,
                    textColor = NeuBlack,
                    shadowOffset = 2.dp
                )

                NeuButton(
                    text = "💬 Send via WhatsApp",
                    onClick = {
                        if (phoneInput.length >= 10) {
                            onSendWhatsApp(phoneInput)
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
