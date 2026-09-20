package com.smartvendor.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartvendor.ai.ui.components.NeuBadge
import com.smartvendor.ai.ui.components.NeuButton
import com.smartvendor.ai.ui.components.NeuCard
import com.smartvendor.ai.ui.components.neuShadow
import com.smartvendor.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onNavigateToScan: (String) -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            DashboardTopBar(
                userName = uiState.userName,
                storeName = uiState.storeName,
                urgentAlertCount = uiState.urgentStockAlerts.size,
                onNotificationClick = { viewModel.toggleNotificationDialog(true) },
                onProfileClick = { onNavigateToSettings() }
            )
        },
        containerColor = NeuBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Action: Neubrutalist Start New Bill Card
                item {
                    NewBillCard(
                        openBillId = uiState.openBillId,
                        isLoading = uiState.isLoading,
                        onClick = {
                            viewModel.startOrResumeNewBill { billId ->
                                onNavigateToScan(billId)
                            }
                        }
                    )
                }

                // Section Title
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "COMMAND HUB",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = NeuBlack,
                                letterSpacing = 1.sp
                            )
                        )
                        NeuBadge(
                            text = "EDGE POS",
                            backgroundColor = NeuYellow,
                            textColor = NeuBlack
                        )
                    }
                }

                // 2x2 Neubrutalist Quick Action Grid
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        DashboardActionCard(
                            title = "Inventory",
                            description = "Stock & Catalog",
                            icon = Icons.Outlined.Inventory2,
                            accentColor = NeuGreen,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToInventory
                        )
                        DashboardActionCard(
                            title = "Analytics",
                            description = "Revenue & Sales",
                            icon = Icons.Outlined.BarChart,
                            accentColor = NeuYellow,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToReports
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        DashboardActionCard(
                            title = "Bill History",
                            description = "Past Receipts",
                            icon = Icons.Outlined.ReceiptLong,
                            accentColor = NeuPurple,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToHistory
                        )
                        DashboardActionCard(
                            title = "Settings",
                            description = "GST & UPI Config",
                            icon = Icons.Outlined.Settings,
                            accentColor = NeuCyan,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToSettings
                        )
                    }
                }
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

    if (uiState.showNotificationDialog) {
        UrgentStockNotificationSheet(
            alerts = uiState.urgentStockAlerts,
            onDismiss = { viewModel.toggleNotificationDialog(false) },
            onQuickRestock = { productId -> viewModel.quickRestock(productId) },
            onNavigateToInventory = {
                viewModel.toggleNotificationDialog(false)
                onNavigateToInventory()
            }
        )
    }
}

@Composable
fun DashboardTopBar(
    userName: String,
    storeName: String,
    urgentAlertCount: Int,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NeuSurface)
            .border(BorderStroke(2.5.dp, NeuBlack))
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ScanSnap",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = NeuBlack
                    )
                    Text(
                        text = "AI",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = NeuBlue
                    )
                }
                Text(
                    text = if (storeName.isNotBlank()) storeName else "Smart Kirana Store",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NeuGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Notification Button with Solid Shadow
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .neuShadow(offsetX = 2.dp, offsetY = 2.dp, cornerRadius = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeuSurface)
                        .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(8.dp))
                        .clickable { onNotificationClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (urgentAlertCount > 0) {
                        BadgedBox(
                            badge = {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(NeuRed, CircleShape)
                                        .border(BorderStroke(1.dp, NeuBlack), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$urgentAlertCount",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = "Urgent Alerts",
                                tint = NeuRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = NeuBlack,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Profile Avatar Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .neuShadow(offsetX = 2.dp, offsetY = 2.dp, cornerRadius = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeuYellow)
                        .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(8.dp))
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (userName.isNotBlank()) userName.take(1).uppercase() else "S",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = NeuBlack
                    )
                }
            }
        }
    }
}

@Composable
fun NewBillCard(
    openBillId: String?,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    NeuCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NeuBlue,
        borderColor = NeuBlack,
        shadowOffset = 5.dp,
        cornerRadius = 16.dp,
        onClick = if (!isLoading) onClick else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeuBadge(
                    text = "⚡ INSTANT AI BILLING",
                    backgroundColor = NeuYellow,
                    textColor = NeuBlack
                )
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .neuShadow(2.dp, 2.dp, NeuBlack, 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "📸", fontSize = 18.sp)
                }
            }

            Text(
                text = "Launch Visual POS",
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )

            Text(
                text = "Point camera at FMCG products for sub-50ms YOLO object detection & instant cart billing.",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Embedded Action Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neuShadow(3.dp, 3.dp, NeuBlack, 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeuSurface)
                    .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⚡",
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isLoading) "Initializing AI Scanner..." else "Start Camera Scanner",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = NeuBlack
                        )
                    }
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = NeuBlack,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = NeuBlack,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    NeuCard(
        modifier = modifier.height(130.dp),
        backgroundColor = NeuSurface,
        borderColor = NeuBlack,
        shadowOffset = 4.dp,
        cornerRadius = 14.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .neuShadow(2.dp, 2.dp, NeuBlack, 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor)
                    .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = NeuBlack,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = NeuBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = NeuGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrgentStockNotificationSheet(
    alerts: List<UrgentStockAlert>,
    onDismiss: () -> Unit,
    onQuickRestock: (String) -> Unit,
    onNavigateToInventory: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NeuBackground,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "🚨", fontSize = 20.sp)
                    Column {
                        Text(
                            text = "Urgent Stock Alerts",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = NeuBlack
                        )
                        Text(
                            text = if (alerts.isNotEmpty())
                                "${alerts.size} items require immediate replenishment"
                            else
                                "All stock levels healthy",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeuGray
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeuSurface)
                        .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(6.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "✕", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeuBlack)
                }
            }

            HorizontalDivider(thickness = 2.dp, color = NeuBlack)

            if (alerts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "🎉", fontSize = 36.sp)
                        Text(
                            text = "All Inventory Healthy!",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = NeuBlack
                        )
                        Text(
                            text = "Zero critical stock alerts right now.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NeuGray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(alerts) { item ->
                        NeuCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = if (item.isOutOfStock) Color(0xFFFFE4E6) else Color(0xFFFEF3C7),
                            shadowOffset = 3.dp,
                            cornerRadius = 10.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        NeuBadge(
                                            text = if (item.isOutOfStock) "OUT OF STOCK" else "LOW STOCK",
                                            backgroundColor = if (item.isOutOfStock) NeuRed else NeuYellow,
                                            textColor = if (item.isOutOfStock) Color.White else NeuBlack
                                        )
                                        Text(
                                            text = item.category,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeuGray
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = NeuBlack,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = "Current: ${item.currentStock} units (Threshold: ${item.lowStockThreshold})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (item.isOutOfStock) NeuRed else NeuBlack
                                    )
                                }

                                NeuButton(
                                    text = "+20 Stock",
                                    onClick = { onQuickRestock(item.id) },
                                    backgroundColor = NeuGreen,
                                    textColor = NeuBlack,
                                    shadowOffset = 2.dp
                                )
                            }
                        }
                    }
                }
            }

            NeuButton(
                text = "📦 Open Inventory Manager",
                onClick = {
                    onDismiss()
                    onNavigateToInventory()
                },
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = NeuBlue,
                textColor = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
