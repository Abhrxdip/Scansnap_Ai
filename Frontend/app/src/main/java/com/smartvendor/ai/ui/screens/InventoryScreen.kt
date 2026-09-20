package com.smartvendor.ai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartvendor.ai.model.Product
import com.smartvendor.ai.ui.components.NeuBadge
import com.smartvendor.ai.ui.components.NeuButton
import com.smartvendor.ai.ui.components.NeuCard
import com.smartvendor.ai.ui.components.neuShadow
import com.smartvendor.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel = viewModel(),
    onNavigateToProductDetail: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

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
                                text = "Inventory Hub",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = NeuBlack
                            )
                            Text(
                                text = "${uiState.filteredProducts.size} Products in Stock",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = NeuGray
                            )
                        }
                    }

                    NeuBadge(
                        text = "STOCK OPS",
                        backgroundColor = NeuGreen,
                        textColor = NeuBlack
                    )
                }
            }
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .neuShadow(4.dp, 4.dp, NeuBlack, 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeuBlue)
                    .border(BorderStroke(2.5.dp, NeuBlack), RoundedCornerShape(12.dp))
                    .clickable { viewModel.openAddProductDialog() }
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "➕", fontSize = 16.sp)
                    Text(text = "Add Product", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
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
                // Neubrutalist Search Box
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
                        Icon(Icons.Outlined.Search, contentDescription = "Search", tint = NeuBlack, modifier = Modifier.size(20.dp))
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Search by name, category or barcode...", fontSize = 13.sp, color = NeuGray) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = NeuBlack, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Neubrutalist Category Chips Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.categories) { category ->
                        val isSelected = uiState.selectedCategory == category
                        Box(
                            modifier = Modifier
                                .neuShadow(
                                    offsetX = if (isSelected) 3.dp else 2.dp,
                                    offsetY = if (isSelected) 3.dp else 2.dp,
                                    cornerRadius = 8.dp
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeuBlue else NeuSurface)
                                .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(8.dp))
                                .clickable { viewModel.onCategorySelected(category) }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = category,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = if (isSelected) Color.White else NeuBlack
                            )
                        }
                    }
                }

                // Product List in NeuCard format
                if (uiState.filteredProducts.isNotEmpty()) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.filteredProducts, key = { it.id }) { product ->
                            InventoryProductCard(
                                product = product,
                                onEditClick = { viewModel.openEditProductDialog(product) },
                                onIncreaseStock = { viewModel.setExactStock(product.id, product.stock + 1) },
                                onDecreaseStock = { viewModel.setExactStock(product.id, product.stock - 1) }
                            )
                        }
                    }
                } else if (!uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "📦", fontSize = 48.sp)
                            Text(
                                text = "No Products Found",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = NeuBlack
                            )
                            Text(
                                text = "Tap '+ Add Product' to expand inventory.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NeuGray
                            )
                        }
                    }
                }
            }

            if (uiState.showAddDialog) {
                AddProductDialog(
                    onDismiss = { viewModel.closeAddProductDialog() },
                    onSave = { name, price, cat, stock, barcode, classId ->
                        viewModel.addNewProduct(name, price, cat, stock, barcode, classId)
                    }
                )
            }

            if (uiState.editingProduct != null) {
                EditProductStockDialog(
                    product = uiState.editingProduct!!,
                    onDismiss = { viewModel.closeEditProductDialog() },
                    onSave = { name, price, cat, stock ->
                        viewModel.updateProductDetails(uiState.editingProduct!!.id, name, price, cat, stock)
                    },
                    onDelete = {
                        viewModel.deleteProduct(uiState.editingProduct!!.id)
                    }
                )
            }
        }
    }
}

@Composable
fun InventoryProductCard(
    product: Product,
    onEditClick: () -> Unit,
    onIncreaseStock: () -> Unit,
    onDecreaseStock: () -> Unit
) {
    val (statusText, badgeColor, textColor) = when {
        product.stock > 20 -> Triple("In Stock (${product.stock})", NeuGreen, NeuBlack)
        product.stock in 6..20 -> Triple("Medium (${product.stock})", NeuYellow, NeuBlack)
        product.stock in 1..5 -> Triple("Low Stock (${product.stock})", NeuRed, Color.White)
        else -> Triple("Out of Stock (0)", NeuRed, Color.White)
    }

    NeuCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick),
        backgroundColor = NeuSurface,
        shadowOffset = 3.dp,
        cornerRadius = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        fontSize = 15.sp,
                        color = NeuBlack
                    )
                    Text(
                        text = "Category: ${product.category}  |  Barcode: ${product.barcode.ifBlank { "--" }}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = NeuGray
                    )
                }

                Text(
                    text = "₹${"%.2f".format(product.price)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = NeuBlue
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeuBadge(
                    text = statusText,
                    backgroundColor = badgeColor,
                    textColor = textColor,
                    shadowOffset = 1.5.dp
                )

                // Quick Stock - / + controls & Edit Button
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
                            .clickable(enabled = product.stock > 0) { onDecreaseStock() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "−", fontWeight = FontWeight.Black, fontSize = 16.sp, color = NeuBlack)
                    }

                    Text(
                        text = "${product.stock}",
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
                            .clickable { onIncreaseStock() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "+", fontWeight = FontWeight.Black, fontSize = 16.sp, color = NeuBlack)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .neuShadow(1.5.dp, 1.5.dp, NeuBlack, 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeuYellow)
                            .border(BorderStroke(1.8.dp, NeuBlack), RoundedCornerShape(6.dp))
                            .clickable { onEditClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = NeuBlack, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EditProductStockDialog(
    product: Product,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, Int) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var price by remember { mutableStateOf(product.price.toString()) }
    var category by remember { mutableStateOf(product.category) }
    var stock by remember { mutableStateOf(product.stock.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeuSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .border(BorderStroke(3.dp, NeuBlack), RoundedCornerShape(16.dp))
            .neuShadow(6.dp, 6.dp, NeuBlack, 16.dp),
        title = {
            Text(
                text = "Edit Stock & Details",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = NeuBlack
            )
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
                    label = { Text("Product Name", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price (₹)", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it },
                    label = { Text("Remaining Stock", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = NeuRed)
                }
                NeuButton(
                    text = "Save Stock",
                    onClick = {
                        val priceVal = price.toDoubleOrNull() ?: product.price
                        val stockVal = stock.toIntOrNull() ?: product.stock
                        if (name.isNotBlank()) {
                            onSave(name, priceVal, category, stockVal)
                        }
                    },
                    backgroundColor = NeuBlue,
                    textColor = Color.White,
                    shadowOffset = 2.dp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontWeight = FontWeight.Bold, color = NeuBlack)
            }
        }
    )
}

@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, String, Int, String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var stock by remember { mutableStateOf("50") }
    var barcode by remember { mutableStateOf("") }
    var classId by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeuSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .border(BorderStroke(3.dp, NeuBlack), RoundedCornerShape(16.dp))
            .neuShadow(6.dp, 6.dp, NeuBlack, 16.dp),
        title = {
            Text(
                text = "Add New Product",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = NeuBlack
            )
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
                    label = { Text("Product Name", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price (₹)", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it },
                    label = { Text("Stock Quantity", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode (Optional)", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            NeuButton(
                text = "Save Product",
                onClick = {
                    val priceVal = price.toDoubleOrNull() ?: 0.0
                    val stockVal = stock.toIntOrNull() ?: 0
                    val classIdVal = classId.toIntOrNull() ?: 0
                    if (name.isNotBlank() && priceVal > 0) {
                        onSave(name, priceVal, category, stockVal, barcode, classIdVal)
                    }
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
