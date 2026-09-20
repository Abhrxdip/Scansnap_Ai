package com.smartvendor.ai.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartvendor.ai.ai.DetectionOverlayView
import com.smartvendor.ai.camera.CameraPreviewView
import com.smartvendor.ai.model.Product
import com.smartvendor.ai.ui.components.NeuBadge
import com.smartvendor.ai.ui.components.NeuButton
import com.smartvendor.ai.ui.components.NeuCard
import com.smartvendor.ai.ui.components.neuShadow
import com.smartvendor.ai.ui.theme.*
import com.smartvendor.ai.utils.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    billId: String,
    viewModel: ScanViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToBilling: (String) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(PermissionUtils.hasCameraPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshBill()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(key1 = billId) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        viewModel.initialize(context, billId)
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeuSurface)
                    .border(BorderStroke(2.5.dp, NeuBlack))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
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
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Visual AI POS Scanner",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = NeuBlack
                            )
                            Text(
                                text = "Bill ID: ${billId.take(8)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = NeuGray
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .neuShadow(2.dp, 2.dp, NeuBlack, 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeuYellow)
                                .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(8.dp))
                                .clickable { viewModel.openManualEntryDialog() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Item", tint = NeuBlack, modifier = Modifier.size(20.dp))
                        }

                        NeuBadge(
                            text = "🛒 ${uiState.currentBill?.items?.sumOf { it.quantity } ?: 0} ITEMS",
                            backgroundColor = NeuGreen,
                            textColor = NeuBlack,
                            shadowOffset = 2.dp
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            NeuButton(
                text = "➕ Manual Entry",
                onClick = { viewModel.openManualEntryDialog() },
                backgroundColor = NeuYellow,
                textColor = NeuBlack,
                shadowOffset = 3.dp
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (hasCameraPermission) {
                // Live Camera View
                CameraPreviewView(
                    onFrameAvailable = { imageProxy ->
                        viewModel.processFrame(imageProxy)
                    },
                    onCameraInitialized = { },
                    onCameraError = { }
                )

                // Bounding Box Overlay (Object Detection)
                DetectionOverlayView(
                    detections = uiState.activeDetections
                )

                // Visual Barcode Scanner Target Box Overlay
                if (uiState.isBarcodeActive) {
                    BarcodeTargetOverlay()
                }

                // Top AI / Barcode / OCR Status Banner & Mode Toggle Row
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiStatusBanner(
                        status = uiState.aiStatus,
                        isBarcodeActive = uiState.isBarcodeActive || uiState.isOcrActive
                    )

                    // Scanner Mode Chips: Barcode vs OCR Label Reader
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .neuShadow(2.dp, 2.dp, NeuBlack, 10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!uiState.isOcrActive) NeuYellow else NeuSurface)
                                .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(10.dp))
                                .clickable { viewModel.toggleScanMode(useOcr = false) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "⚡ Smart Scanner",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = NeuBlack
                            )
                        }

                        Box(
                            modifier = Modifier
                                .neuShadow(2.dp, 2.dp, NeuBlack, 10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (uiState.isOcrActive) NeuBlue else NeuSurface)
                                .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(10.dp))
                                .clickable { viewModel.toggleScanMode(useOcr = true) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "📝 OCR Mode",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = if (uiState.isOcrActive) NeuWhite else NeuBlack
                            )
                        }
                    }
                }

                // Bottom Content: Current Detected Product Card or Bill Bar
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedVisibility(
                        visible = uiState.detectedProduct != null || uiState.detectedProductsList.isNotEmpty(),
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        if (uiState.detectedProductsList.size > 1) {
                            MultiProductDetectedCard(
                                products = uiState.detectedProductsList,
                                selectedProduct = uiState.detectedProduct ?: uiState.detectedProductsList.first(),
                                selectedQuantity = uiState.selectedQuantity,
                                onSelectProduct = { viewModel.selectDetectedProduct(it) },
                                onRemoveProduct = { viewModel.removeDetectedProductFromList(it) },
                                onIncrease = { viewModel.increaseQuantity() },
                                onDecrease = { viewModel.decreaseQuantity() },
                                onAddSingle = { viewModel.addProductToBill() },
                                onAddAll = { viewModel.addAllDetectedProductsToBill() },
                                onCancel = { viewModel.cancelDetection() }
                            )
                        } else {
                            uiState.detectedProduct?.let { product ->
                                DetectedProductCard(
                                    product = product,
                                    selectedQuantity = uiState.selectedQuantity,
                                    onIncrease = { viewModel.increaseQuantity() },
                                    onDecrease = { viewModel.decreaseQuantity() },
                                    onAdd = { viewModel.addProductToBill() },
                                    onCancel = { viewModel.cancelDetection() }
                                )
                            }
                        }
                    }

                    // Live Auto-Added Feedback Pill with Instant Undo
                    AnimatedVisibility(
                        visible = uiState.lastAutoAddedProduct != null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        uiState.lastAutoAddedProduct?.let { product ->
                            NeuCard(
                                backgroundColor = NeuGreen,
                                shadowOffset = 3.dp,
                                cornerRadius = 12.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeuBlack, modifier = Modifier.size(20.dp))
                                        Column {
                                            Text(
                                                text = "+1 ${product.name}",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp,
                                                color = NeuBlack
                                            )
                                            Text(
                                                text = "₹${"%.2f".format(product.price)} added to bill",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = NeuBlack.copy(alpha = 0.8f)
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .neuShadow(2.dp, 2.dp, NeuBlack, 8.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NeuSurface)
                                            .border(BorderStroke(1.5.dp, NeuBlack), RoundedCornerShape(8.dp))
                                            .clickable { viewModel.undoLastAutoAddedProduct() }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Undo, contentDescription = "Undo", tint = NeuBlack, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Undo", fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeuBlack)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Billing Bar
                    NeuCard(
                        backgroundColor = NeuSurface,
                        shadowOffset = 4.dp,
                        cornerRadius = 16.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Current Total",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = NeuGray
                                )
                                Text(
                                    text = "₹${"%.2f".format(uiState.currentBill?.grandTotal ?: 0.0)}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp,
                                    color = NeuBlack
                                )
                            }

                            NeuButton(
                                text = "View Bill →",
                                onClick = {
                                    val activeBillId = uiState.currentBill?.billId ?: billId
                                    if (activeBillId.isNotBlank()) {
                                        onNavigateToBilling(activeBillId)
                                    }
                                },
                                backgroundColor = NeuBlue,
                                textColor = NeuWhite,
                                shadowOffset = 3.dp,
                                cornerRadius = 10.dp
                            )
                        }
                    }
                }
            } else {
                // Permission Denied View
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    NeuCard(
                        modifier = Modifier.padding(24.dp),
                        backgroundColor = NeuSurface,
                        shadowOffset = 5.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .neuShadow(3.dp, 3.dp, NeuBlack, 16.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(NeuYellow)
                                    .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Camera,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = NeuBlack
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Camera Permission Required",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = NeuBlack
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "SmartVendor AI needs camera access to scan barcodes live and recognize items.",
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = NeuGray
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            NeuButton(
                                text = "Grant Camera Permission",
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                backgroundColor = NeuBlue,
                                textColor = NeuWhite
                            )
                        }
                    }
                }
            }

            if (uiState.showManualEntryDialog) {
                ManualProductEntryDialog(
                    initialName = uiState.ocrPrefilledName,
                    initialPrice = uiState.ocrPrefilledPrice,
                    inventoryProducts = uiState.inventoryProducts,
                    onDismiss = { viewModel.dismissManualEntryDialog() },
                    onAddExistingProduct = { product, qty ->
                        viewModel.addExistingProductToBill(product, qty)
                    },
                    onSaveNewProduct = { name, price, stock, cat, barcode, qty ->
                        viewModel.saveManualProduct(name, price, stock, cat, barcode, qty)
                    }
                )
            }
        }
    }
}

@Composable
fun BarcodeTargetOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserYRatio by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserLine"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 260.dp, height = 170.dp)
                    .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .border(2.dp, BluePrimary.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
            ) {
                // Red scanning laser line
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val laserY = size.height * laserYRatio
                    drawLine(
                        color = Color.Red,
                        start = Offset(x = 10.dp.toPx(), y = laserY),
                        end = Offset(x = size.width - 10.dp.toPx(), y = laserY),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Point camera barcode inside box",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
fun AiStatusBanner(
    status: String,
    isBarcodeActive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .neuShadow(3.dp, 3.dp, NeuBlack, 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isBarcodeActive) NeuBlue else NeuGreen)
            .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isBarcodeActive) NeuYellow else NeuBlack)
                    .border(BorderStroke(1.dp, NeuBlack), CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isBarcodeActive) "📷 Barcode Scanner Active" else "🤖 AI Object Detection Active",
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                color = if (isBarcodeActive) NeuWhite else NeuBlack
            )
        }
    }
}

@Composable
fun DetectedProductCard(
    product: Product,
    selectedQuantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onAdd: () -> Unit,
    onCancel: () -> Unit
) {
    NeuCard(
        backgroundColor = NeuSurface,
        shadowOffset = 5.dp,
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = NeuBlack
                    )
                    Text(
                        text = "Category: ${product.category}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = NeuGray
                    )
                }
                Text(
                    text = "₹${"%.2f".format(product.price)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = NeuBlue
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeuBadge(
                    text = if (product.stock > 0) "IN STOCK (${product.stock})" else "OUT OF STOCK",
                    backgroundColor = if (product.stock > 0) NeuGreen else NeuRed,
                    textColor = if (product.stock > 0) NeuBlack else NeuWhite,
                    shadowOffset = 2.dp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .neuShadow(2.dp, 2.dp, NeuBlack, 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeuSurface)
                            .border(BorderStroke(1.5.dp, NeuBlack), RoundedCornerShape(6.dp))
                            .clickable(enabled = selectedQuantity > 1) { onDecrease() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = NeuBlack, modifier = Modifier.size(16.dp))
                    }

                    Text(
                        text = "$selectedQuantity",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = NeuBlack
                    )

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .neuShadow(2.dp, 2.dp, NeuBlack, 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeuYellow)
                            .border(BorderStroke(1.5.dp, NeuBlack), RoundedCornerShape(6.dp))
                            .clickable { onIncrease() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = NeuBlack, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (selectedQuantity > product.stock) {
                NeuBadge(
                    text = "⚠️ Exceeds app stock (${product.stock} listed) — billing allowed",
                    backgroundColor = NeuYellow,
                    textColor = NeuBlack,
                    shadowOffset = 2.dp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NeuButton(
                    text = "Cancel",
                    onClick = onCancel,
                    backgroundColor = NeuSurface,
                    textColor = NeuBlack,
                    modifier = Modifier.weight(1f),
                    shadowOffset = 3.dp,
                    cornerRadius = 10.dp
                )
                NeuButton(
                    text = "Add to Bill",
                    onClick = onAdd,
                    backgroundColor = NeuYellow,
                    textColor = NeuBlack,
                    modifier = Modifier.weight(1f),
                    shadowOffset = 3.dp,
                    cornerRadius = 10.dp
                )
            }
        }
    }
}

@Composable
fun MultiProductDetectedCard(
    products: List<Product>,
    selectedProduct: Product,
    selectedQuantity: Int,
    onSelectProduct: (Product) -> Unit,
    onRemoveProduct: (Product) -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onAddSingle: () -> Unit,
    onAddAll: () -> Unit,
    onCancel: () -> Unit
) {
    NeuCard(
        backgroundColor = NeuSurface,
        shadowOffset = 5.dp,
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Multi-Detection Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 ${products.size} Products in Frame",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = NeuBlack
                )
                NeuBadge(
                    text = "Total ₹${"%.2f".format(products.sumOf { it.price })}",
                    backgroundColor = NeuBlue,
                    textColor = NeuWhite,
                    shadowOffset = 2.dp
                )
            }

            // Horizontal Chips of all detected products with instant 'X' remove button
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(products) { item ->
                    val isSelected = (item.id == selectedProduct.id)
                    Box(
                        modifier = Modifier
                            .neuShadow(2.dp, 2.dp, NeuBlack, 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeuYellow else NeuSurface)
                            .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(8.dp))
                            .clickable { onSelectProduct(item) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${item.name} • ₹${item.price.toInt()}",
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NeuBlack
                            )
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove ${item.name}",
                                tint = NeuBlack,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onRemoveProduct(item) }
                            )
                        }
                    }
                }
            }

            // Focused Product Card Details
            NeuCard(
                backgroundColor = NeuBackground,
                shadowOffset = 2.dp,
                cornerRadius = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedProduct.name,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = NeuBlack
                            )
                            Text(
                                text = "Stock: ${selectedProduct.stock} available",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = NeuGray
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "₹${"%.2f".format(selectedProduct.price)}",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = NeuBlue
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .neuShadow(1.dp, 1.dp, NeuBlack, 6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeuRed)
                                    .border(BorderStroke(1.dp, NeuBlack), RoundedCornerShape(6.dp))
                                    .clickable { onRemoveProduct(selectedProduct) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Remove item",
                                    tint = NeuWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Quantity controls for focused item
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quantity:",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = NeuBlack
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .neuShadow(1.dp, 1.dp, NeuBlack, 6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeuSurface)
                                    .border(BorderStroke(1.dp, NeuBlack), RoundedCornerShape(6.dp))
                                    .clickable(enabled = selectedQuantity > 1) { onDecrease() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = NeuBlack, modifier = Modifier.size(14.dp))
                            }

                            Text(
                                text = "$selectedQuantity",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = NeuBlack
                            )

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .neuShadow(1.dp, 1.dp, NeuBlack, 6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeuYellow)
                                    .border(BorderStroke(1.dp, NeuBlack), RoundedCornerShape(6.dp))
                                    .clickable { onIncrease() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = NeuBlack, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // Action Buttons: Cancel, Add All
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeuButton(
                    text = "Cancel",
                    onClick = onCancel,
                    backgroundColor = NeuSurface,
                    textColor = NeuBlack,
                    modifier = Modifier.weight(0.7f),
                    shadowOffset = 2.dp,
                    cornerRadius = 8.dp
                )

                NeuButton(
                    text = "Add All (${products.size})",
                    onClick = onAddAll,
                    backgroundColor = NeuGreen,
                    textColor = NeuBlack,
                    modifier = Modifier.weight(1.3f),
                    shadowOffset = 2.dp,
                    cornerRadius = 8.dp
                )
            }
        }
    }
}

@Composable
fun ManualProductEntryDialog(
    initialName: String = "",
    initialPrice: String = "",
    inventoryProducts: List<Product> = emptyList(),
    onDismiss: () -> Unit,
    onAddExistingProduct: (Product, Int) -> Unit,
    onSaveNewProduct: (String, Double, Int, String, String, Int) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }

    var price by remember(initialPrice) { mutableStateOf(initialPrice) }
    var stock by remember { mutableStateOf("10") }
    var category by remember { mutableStateOf("General") }
    var barcode by remember { mutableStateOf("") }

    var quantityToSell by remember { mutableStateOf(1) }

    // Live search suggestions as user types name
    val suggestions = remember(name, selectedProduct, inventoryProducts) {
        if (name.isBlank() || selectedProduct != null) {
            emptyList()
        } else {
            inventoryProducts.filter {
                it.name.contains(name, ignoreCase = true)
            }.take(4)
        }
    }

    // Auto select exact match
    LaunchedEffect(name) {
        if (selectedProduct == null && name.isNotBlank()) {
            val exact = inventoryProducts.firstOrNull { it.name.equals(name, ignoreCase = true) }
            if (exact != null) {
                selectedProduct = exact
            }
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
            Text(
                text = if (selectedProduct != null) "Select Quantity to Sell" else "Manual Product Entry",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = NeuBlack
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (selectedProduct != null) {
                    val prod = selectedProduct!!
                    // Existing Inventory Product Found Banner Card
                    NeuCard(
                        backgroundColor = NeuBackground,
                        shadowOffset = 3.dp,
                        cornerRadius = 12.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NeuBadge(
                                    text = "INVENTORY MATCH",
                                    backgroundColor = NeuGreen,
                                    textColor = NeuBlack,
                                    shadowOffset = 1.5.dp
                                )

                                Text(
                                    text = "Change Item",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = NeuBlue,
                                    modifier = Modifier
                                        .clickable {
                                            selectedProduct = null
                                            name = ""
                                        }
                                        .padding(4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = prod.name,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = NeuBlack
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Price: ₹${prod.price}  •  Stock Available: ${prod.stock}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NeuGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Quantity selector (- 1 +)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quantity to Sell:",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = NeuBlack
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .neuShadow(2.dp, 2.dp, NeuBlack, 6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeuSurface)
                                    .border(BorderStroke(1.5.dp, NeuBlack), RoundedCornerShape(6.dp))
                                    .clickable(enabled = quantityToSell > 1) { quantityToSell-- },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = NeuBlack, modifier = Modifier.size(16.dp))
                            }

                            Text(
                                text = "$quantityToSell",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = NeuBlack
                            )

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .neuShadow(2.dp, 2.dp, NeuBlack, 6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeuYellow)
                                    .border(BorderStroke(1.5.dp, NeuBlack), RoundedCornerShape(6.dp))
                                    .clickable { quantityToSell++ },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = NeuBlack, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (quantityToSell > prod.stock) {
                        NeuBadge(
                            text = "⚠️ Selling $quantityToSell units exceeds listed stock (${prod.stock}). Billing allowed.",
                            backgroundColor = NeuYellow,
                            textColor = NeuBlack,
                            shadowOffset = 2.dp
                        )
                    }
                } else {
                    // Search & New Product Form Mode
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                selectedProduct = null
                            },
                            label = { Text("Product Name", fontWeight = FontWeight.Bold) },
                            placeholder = { Text("Type to search inventory...") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Autocomplete Suggestions List
                        if (suggestions.isNotEmpty()) {
                            NeuCard(
                                backgroundColor = NeuBackground,
                                shadowOffset = 2.dp,
                                cornerRadius = 8.dp
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text(
                                        text = "Matching Store Items:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = NeuGray,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    suggestions.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedProduct = item
                                                    name = item.name
                                                }
                                                .padding(horizontal = 6.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = NeuBlack
                                            )
                                            Text(
                                                text = "₹${item.price} (Stock: ${item.stock})",
                                                fontSize = 12.sp,
                                                color = NeuBlue,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text("Price (₹)", fontWeight = FontWeight.Bold) },
                            placeholder = { Text("e.g. 50.00") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = stock,
                            onValueChange = { stock = it },
                            label = { Text("Available Stock Quantity", fontWeight = FontWeight.Bold) },
                            placeholder = { Text("e.g. 20") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Quantity to Sell row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Quantity to Sell:",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = NeuBlack
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .neuShadow(1.5.dp, 1.5.dp, NeuBlack, 6.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeuSurface)
                                        .border(BorderStroke(1.5.dp, NeuBlack), RoundedCornerShape(6.dp))
                                        .clickable(enabled = quantityToSell > 1) { quantityToSell-- },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = NeuBlack, modifier = Modifier.size(14.dp))
                                }

                                Text(
                                    text = "$quantityToSell",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = NeuBlack
                                )

                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .neuShadow(1.5.dp, 1.5.dp, NeuBlack, 6.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeuYellow)
                                        .border(BorderStroke(1.5.dp, NeuBlack), RoundedCornerShape(6.dp))
                                        .clickable { quantityToSell++ },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = NeuBlack, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            NeuButton(
                text = if (selectedProduct != null) "Add $quantityToSell to Bill" else "Save & Add",
                onClick = {
                    if (selectedProduct != null) {
                        onAddExistingProduct(selectedProduct!!, quantityToSell)
                    } else {
                        val priceVal = price.toDoubleOrNull() ?: 0.0
                        val stockVal = stock.toIntOrNull() ?: 10
                        if (name.isNotBlank() && priceVal > 0) {
                            onSaveNewProduct(name, priceVal, stockVal, category, barcode, quantityToSell)
                        }
                    }
                },
                backgroundColor = NeuYellow,
                textColor = NeuBlack,
                shadowOffset = 2.dp,
                cornerRadius = 8.dp
            )
        },
        dismissButton = {
            NeuButton(
                text = "Cancel",
                onClick = onDismiss,
                backgroundColor = NeuSurface,
                textColor = NeuBlack,
                shadowOffset = 2.dp,
                cornerRadius = 8.dp
            )
        }
    )
}
