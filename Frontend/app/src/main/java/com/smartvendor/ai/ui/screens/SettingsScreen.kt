package com.smartvendor.ai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvendor.ai.model.Store
import com.smartvendor.ai.repository.AuthRepository
import com.smartvendor.ai.repository.AuthRepositoryImpl
import com.smartvendor.ai.repository.StoreRepository
import com.smartvendor.ai.repository.StoreRepositoryImpl
import com.smartvendor.ai.ui.components.NeuBadge
import com.smartvendor.ai.ui.components.NeuButton
import com.smartvendor.ai.ui.components.NeuCard
import com.smartvendor.ai.ui.components.neuShadow
import com.smartvendor.ai.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authRepository: AuthRepository = remember { AuthRepositoryImpl() },
    onNavigateBack: () -> Unit,
    onLogoutSuccess: () -> Unit = onNavigateBack
) {
    val storeRepository: StoreRepository = remember { StoreRepositoryImpl() }
    val storeInfo by storeRepository.getStoreInfo().collectAsState(initial = Store())
    val currentUser by authRepository.getCurrentUser().collectAsState(initial = null)

    val coroutineScope = rememberCoroutineScope()
    var isDarkMode by remember { mutableStateOf(false) }
    var showStoreInfoDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val displayStoreName = storeInfo.name.ifBlank { currentUser?.name ?: "Om Sai Kirana Store" }
    val displayAddress = storeInfo.address.ifBlank { "Tap to configure store address" }
    val displayGst = if (storeInfo.gst.isNotBlank()) "GST: ${storeInfo.gst}" else "GST: Unregistered"

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
                                text = "Settings & Store",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = NeuBlack
                            )
                            Text(
                                text = "POS & Business Configuration",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = NeuGray
                            )
                        }
                    }

                    NeuBadge(
                        text = "TERMINAL",
                        backgroundColor = NeuCyan,
                        textColor = NeuBlack
                    )
                }
            }
        },
        containerColor = NeuBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Store Profile Hero Card in NeuCard
            NeuCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showStoreInfoDialog = true },
                backgroundColor = NeuSurface,
                shadowOffset = 4.dp,
                cornerRadius = 14.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .neuShadow(2.dp, 2.dp, NeuBlack, 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeuYellow)
                            .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🏪", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayStoreName,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = NeuBlack
                        )
                        Text(
                            text = displayAddress,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NeuBlue,
                            maxLines = 1
                        )
                        Text(
                            text = "$displayGst  •  UPI: ${storeInfo.upi.ifBlank { "Not set" }}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = NeuGray,
                            maxLines = 1
                        )
                    }
                }
            }

            Text(
                text = "STORE PREFERENCES",
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                color = NeuBlack,
                letterSpacing = 0.5.sp
            )

            SettingsItemRow(
                icon = Icons.Outlined.Storefront,
                title = "Edit Store Profile & GST",
                subtitle = "Store name, address, GSTIN, and UPI ID for QR bills",
                badgeColor = NeuYellow,
                onClick = { showStoreInfoDialog = true }
            )

            SettingsItemRow(
                icon = Icons.Outlined.Sync,
                title = "FastAPI Backend Connection",
                subtitle = "http://localhost:8000 (YOLO & Master Catalog active)",
                badgeColor = NeuGreen,
                onClick = { }
            )

            SettingsItemRow(
                icon = Icons.Outlined.Info,
                title = "About ScanSnap AI",
                subtitle = "Edge AI Visual POS & Inventory Terminal v1.0",
                badgeColor = NeuPurple,
                onClick = { showAboutDialog = true }
            )

            Spacer(modifier = Modifier.weight(1f))

            NeuButton(
                text = "🚪 Logout Store Account",
                onClick = {
                    coroutineScope.launch {
                        authRepository.logout()
                        onLogoutSuccess()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = NeuRed,
                textColor = Color.White
            )
        }

        if (showStoreInfoDialog) {
            EditStoreInfoDialog(
                currentStore = storeInfo,
                defaultName = currentUser?.name ?: "",
                onDismiss = { showStoreInfoDialog = false },
                onSave = { updatedStore ->
                    coroutineScope.launch {
                        storeRepository.saveStoreInfo(updatedStore)
                        showStoreInfoDialog = false
                    }
                }
            )
        }

        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                containerColor = NeuSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .border(BorderStroke(3.dp, NeuBlack), RoundedCornerShape(16.dp))
                    .neuShadow(6.dp, 6.dp, NeuBlack, 16.dp),
                title = {
                    Text("About ScanSnap AI", fontWeight = FontWeight.Black, fontSize = 18.sp, color = NeuBlack)
                },
                text = {
                    Text(
                        text = "Next-Gen Edge AI-Powered Smart POS, Object Detection Billing & Inventory Intelligence for Kirana Stores. Engineered with Jetpack Compose, CameraX, YOLOv11 Computer Vision, HSV Color Profiling, and FastAPI.",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = NeuGray,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    NeuButton(
                        text = "Got It",
                        onClick = { showAboutDialog = false },
                        backgroundColor = NeuBlue,
                        textColor = Color.White,
                        shadowOffset = 2.dp
                    )
                }
            )
        }
    }
}

@Composable
fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeColor: Color = NeuBlue,
    trailing: (@Composable () -> Unit)? = null,
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .neuShadow(1.5.dp, 1.5.dp, NeuBlack, 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor)
                        .border(BorderStroke(1.8.dp, NeuBlack), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = NeuBlack, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = NeuBlack
                    )
                    Text(
                        text = subtitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = NeuGray
                    )
                }
            }

            if (trailing != null) {
                trailing()
            } else {
                Text(text = "→", fontWeight = FontWeight.Black, fontSize = 16.sp, color = NeuBlack)
            }
        }
    }
}

@Composable
fun EditStoreInfoDialog(
    currentStore: Store,
    defaultName: String,
    onDismiss: () -> Unit,
    onSave: (Store) -> Unit
) {
    var name by remember { mutableStateOf(currentStore.name.ifBlank { defaultName }) }
    var address by remember { mutableStateOf(currentStore.address) }
    var gst by remember { mutableStateOf(currentStore.gst) }
    var phone by remember { mutableStateOf(currentStore.phone) }
    var upi by remember { mutableStateOf(currentStore.upi) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeuSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .border(BorderStroke(3.dp, NeuBlack), RoundedCornerShape(16.dp))
            .neuShadow(6.dp, 6.dp, NeuBlack, 16.dp),
        title = {
            Text("Edit Store Profile", fontWeight = FontWeight.Black, fontSize = 18.sp, color = NeuBlack)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Store Name", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Store Address", fontWeight = FontWeight.Bold) },
                    singleLine = false,
                    maxLines = 2,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = gst,
                    onValueChange = { gst = it },
                    label = { Text("GSTIN", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = upi,
                    onValueChange = { upi = it },
                    label = { Text("UPI ID (For QR Payments)", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            NeuButton(
                text = "Save Profile",
                onClick = {
                    onSave(
                        currentStore.copy(
                            name = name,
                            address = address,
                            gst = gst,
                            phone = phone,
                            upi = upi
                        )
                    )
                },
                backgroundColor = NeuGreen,
                textColor = NeuBlack,
                shadowOffset = 2.dp
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontWeight = FontWeight.Bold, color = NeuBlack)
            }
        }
    )
}
