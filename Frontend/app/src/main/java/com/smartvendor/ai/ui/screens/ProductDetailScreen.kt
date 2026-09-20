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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvendor.ai.model.Product
import com.smartvendor.ai.repository.ProductRepository
import com.smartvendor.ai.repository.ProductRepositoryImpl
import com.smartvendor.ai.ui.components.NeuBadge
import com.smartvendor.ai.ui.components.NeuButton
import com.smartvendor.ai.ui.components.NeuCard
import com.smartvendor.ai.ui.components.neuShadow
import com.smartvendor.ai.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProductDetailScreen(
    productId: String,
    productRepository: ProductRepository = remember { ProductRepositoryImpl() },
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var product by remember { mutableStateOf<Product?>(null) }
    var showRestockDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = productId) {
        productRepository.getProductsStream().collect { list ->
            product = list.find { it.id == productId }
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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

                    Text(
                        text = "Product Details",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = NeuBlack
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NeuBackground)
                .padding(innerPadding)
        ) {
            val p = product
            if (p != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Product Summary Card
                    NeuCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = NeuSurface,
                        shadowOffset = 5.dp,
                        cornerRadius = 18.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = p.name,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp,
                                        color = NeuBlack
                                    )
                                    Text(
                                        text = "Category: ${p.category}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = NeuGray
                                    )
                                }

                                NeuBadge(
                                    text = if (p.stock > 10) "IN STOCK (${p.stock})" else if (p.stock > 0) "LOW STOCK (${p.stock})" else "OUT OF STOCK",
                                    backgroundColor = if (p.stock > 10) NeuGreen else if (p.stock > 0) NeuYellow else NeuRed,
                                    textColor = if (p.stock > 10 || p.stock > 0) NeuBlack else NeuWhite,
                                    shadowOffset = 2.dp
                                )
                            }

                            HorizontalDivider(thickness = 2.dp, color = NeuBlack)

                            DetailRow("Price per Unit", "₹${"%.2f".format(p.price)}", isHighlight = true)
                            DetailRow("GST Tax Rate", "${p.gst}%")
                            DetailRow("Current Inventory", "${p.stock} units")
                            DetailRow("Barcode", p.barcode.ifBlank { "Not Assigned" })
                            DetailRow("YOLO AI Class ID", "${p.classId}")
                            DetailRow("Added Date", formatDate(p.createdAt))
                            DetailRow("Last Updated", formatDate(p.updatedAt))
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NeuButton(
                            text = "🔄 Restock Stock",
                            onClick = { showRestockDialog = true },
                            backgroundColor = NeuGreen,
                            textColor = NeuBlack,
                            modifier = Modifier.weight(1f),
                            shadowOffset = 3.dp,
                            cornerRadius = 12.dp
                        )

                        NeuButton(
                            text = "🗑️ Delete Product",
                            onClick = { showDeleteDialog = true },
                            backgroundColor = NeuRed,
                            textColor = NeuWhite,
                            modifier = Modifier.weight(1f),
                            shadowOffset = 3.dp,
                            cornerRadius = 12.dp
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeuBlack)
                }
            }

            if (showRestockDialog) {
                var restockQty by remember { mutableStateOf("20") }
                AlertDialog(
                    onDismissRequest = { showRestockDialog = false },
                    containerColor = NeuSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .border(BorderStroke(3.dp, NeuBlack), RoundedCornerShape(16.dp))
                        .neuShadow(6.dp, 6.dp, NeuBlack, 16.dp),
                    title = {
                        Text(
                            text = "Restock Product",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = NeuBlack
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Enter units to add to current stock (${product?.stock ?: 0}):",
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = NeuGray
                            )
                            OutlinedTextField(
                                value = restockQty,
                                onValueChange = { restockQty = it },
                                label = { Text("Additional Units", fontWeight = FontWeight.Bold) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        NeuButton(
                            text = "Confirm Restock",
                            onClick = {
                                val qty = restockQty.toIntOrNull() ?: 0
                                if (qty > 0 && product != null) {
                                    coroutineScope.launch {
                                        productRepository.updateStock(product!!.id, -qty)
                                        showRestockDialog = false
                                    }
                                }
                            },
                            backgroundColor = NeuGreen,
                            textColor = NeuBlack,
                            shadowOffset = 2.dp,
                            cornerRadius = 8.dp
                        )
                    },
                    dismissButton = {
                        NeuButton(
                            text = "Cancel",
                            onClick = { showRestockDialog = false },
                            backgroundColor = NeuSurface,
                            textColor = NeuBlack,
                            shadowOffset = 2.dp,
                            cornerRadius = 8.dp
                        )
                    }
                )
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    containerColor = NeuSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .border(BorderStroke(3.dp, NeuBlack), RoundedCornerShape(16.dp))
                        .neuShadow(6.dp, 6.dp, NeuBlack, 16.dp),
                    title = {
                        Text(
                            text = "Delete Product?",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = NeuBlack
                        )
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to permanently remove this product from inventory? This action cannot be undone.",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = NeuGray
                        )
                    },
                    confirmButton = {
                        NeuButton(
                            text = "Delete Permanently",
                            onClick = {
                                if (product != null) {
                                    coroutineScope.launch {
                                        productRepository.deleteProduct(product!!.id)
                                        showDeleteDialog = false
                                        onNavigateBack()
                                    }
                                }
                            },
                            backgroundColor = NeuRed,
                            textColor = NeuWhite,
                            shadowOffset = 2.dp,
                            cornerRadius = 8.dp
                        )
                    },
                    dismissButton = {
                        NeuButton(
                            text = "Cancel",
                            onClick = { showDeleteDialog = false },
                            backgroundColor = NeuSurface,
                            textColor = NeuBlack,
                            shadowOffset = 2.dp,
                            cornerRadius = 8.dp
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = NeuGray
        )
        Text(
            text = value,
            fontWeight = FontWeight.Black,
            fontSize = if (isHighlight) 16.sp else 13.sp,
            color = if (isHighlight) NeuBlue else NeuBlack
        )
    }
}

fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
}
