package com.smartvendor.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingCart
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
import com.smartvendor.ai.network.models.NearbyStoreProduct
import com.smartvendor.ai.ui.components.NeuBadge
import com.smartvendor.ai.ui.components.NeuButton
import com.smartvendor.ai.ui.components.neuShadow
import com.smartvendor.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    onNavigateBack: () -> Unit,
    viewModel: AiChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }

    val quickSuggestions = listOf(
        "🍜 Is Maggi available?",
        "🔍 Where else can I find Maggi?",
        "🥛 What is the price of Amul milk?",
        "🍪 Show me biscuits"
    )

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
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = NeuBlack,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "ScanSnap Copilot",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = NeuBlack
                                )
                                NeuBadge(
                                    text = "AI",
                                    backgroundColor = NeuYellow,
                                    textColor = NeuBlack
                                )
                            }
                            Text(
                                text = "Smart Inventory & Inter-Store Ordering",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = NeuGray
                            )
                        }
                    }

                    if (uiState.cart.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .neuShadow(2.dp, 2.dp, NeuBlack, 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeuYellow)
                                .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(8.dp))
                                .clickable { viewModel.openCheckout() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "Cart",
                                    tint = NeuBlack,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${uiState.totalCartItems}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = NeuBlack
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = NeuBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Quick Suggestion Chips Carousel
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickSuggestions) { suggestion ->
                    val cleanText = suggestion.substringAfter(" ")
                    Box(
                        modifier = Modifier
                            .neuShadow(2.dp, 2.dp, NeuBlack, 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeuSurface)
                            .border(BorderStroke(1.5.dp, NeuBlack), RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.sendMessage(cleanText)
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = suggestion,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NeuBlack
                        )
                    }
                }
            }

            // Notification / Order Alert Banner
            if (uiState.orderNotification != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                    border = BorderStroke(1.5.dp, NeuGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "✅ ${uiState.orderNotification}",
                            color = NeuBlack,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.dismissOrderNotification() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = NeuBlack,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Message List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                reverseLayout = true
            ) {
                items(uiState.messages.reversed(), key = { it.id }) { message ->
                    ChatMessageItem(
                        message = message,
                        cart = uiState.cart,
                        onAddToCart = { product, qty ->
                            viewModel.addToCart(product, qty)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = NeuBlue,
                    trackColor = NeuSurface
                )
            }

            if (uiState.error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE4E6)),
                    border = BorderStroke(1.5.dp, NeuRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.error!!,
                            color = NeuRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        NeuButton(
                            text = "Retry",
                            onClick = { viewModel.retryLastMessage() },
                            backgroundColor = NeuYellow,
                            textColor = NeuBlack,
                            shadowOffset = 2.dp
                        )
                    }
                }
            }

            // Sticky Cart Floating Bar
            AnimatedVisibility(visible = uiState.cart.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .neuShadow(3.dp, 3.dp, NeuBlack, 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeuBlack)
                        .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
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
                            Text("🛒", fontSize = 22.sp)
                            Column {
                                Text(
                                    text = "${uiState.totalCartItems} item(s) selected",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Total: ₹${String.format("%.2f", uiState.totalCartAmount)}",
                                    color = NeuYellow,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        NeuButton(
                            text = "Checkout ➔",
                            onClick = { viewModel.openCheckout() },
                            backgroundColor = NeuYellow,
                            textColor = NeuBlack,
                            shadowOffset = 2.dp
                        )
                    }
                }
            }

            // Input Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeuSurface)
                    .border(BorderStroke(2.dp, NeuBlack))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                "Ask stock, price, or find elsewhere...",
                                fontSize = 13.sp,
                                color = NeuGray
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeuBlack,
                            unfocusedBorderColor = NeuBlack.copy(alpha = 0.5f),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !uiState.isLoading,
                        singleLine = true
                    )

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .neuShadow(2.dp, 2.dp, NeuBlack, 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (inputText.isNotBlank() && !uiState.isLoading) NeuGreen else NeuGray.copy(alpha = 0.3f))
                            .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(10.dp))
                            .clickable(enabled = !uiState.isLoading && inputText.isNotBlank()) {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendMessage(inputText.trim())
                                    inputText = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = NeuBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Checkout / Order Confirmation Dialog
    if (uiState.isCheckoutSheetVisible) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isPlacingOrder) viewModel.closeCheckout()
            },
            modifier = Modifier
                .fillMaxWidth()
                .neuShadow(4.dp, 4.dp, NeuBlack, 14.dp)
                .border(BorderStroke(2.5.dp, NeuBlack), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            containerColor = NeuSurface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "📦 Partner Store Order",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = NeuBlack
                    )
                    NeuBadge(
                        text = "B2B TRANSFER",
                        backgroundColor = NeuYellow,
                        textColor = NeuBlack
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.cart.isNotEmpty()) {
                        val sellerName = uiState.cart.first().product.storeName
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9))
                                .border(BorderStroke(1.5.dp, NeuBlack), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Supplier Store:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeuGray
                                )
                                Text(
                                    text = sellerName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeuBlack
                                )
                            }
                        }

                        Text(
                            text = "Items in Order:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NeuBlack
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.cart.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.product.productName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = NeuBlack
                                        )
                                        Text(
                                            text = "₹${item.product.price} each",
                                            fontSize = 11.sp,
                                            color = NeuGray
                                        )
                                    }

                                    // Quantity Adjuster
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(NeuSurface)
                                                .border(BorderStroke(1.dp, NeuBlack), RoundedCornerShape(6.dp))
                                                .clickable(enabled = !uiState.isPlacingOrder) {
                                                    viewModel.updateCartQuantity(item.product.productId, item.quantity - 1)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("-", fontWeight = FontWeight.Black, fontSize = 14.sp)
                                        }

                                        Text(
                                            text = "${item.quantity}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = NeuBlack
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(NeuYellow)
                                                .border(BorderStroke(1.dp, NeuBlack), RoundedCornerShape(6.dp))
                                                .clickable(enabled = !uiState.isPlacingOrder) {
                                                    viewModel.updateCartQuantity(item.product.productId, item.quantity + 1)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("+", fontWeight = FontWeight.Black, fontSize = 14.sp)
                                        }

                                        Text(
                                            text = "₹${String.format("%.1f", item.product.price * item.quantity)}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = NeuBlack,
                                            modifier = Modifier.padding(start = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = NeuBlack, thickness = 1.5.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Amount Due:",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = NeuBlack
                            )
                            Text(
                                text = "₹${String.format("%.2f", uiState.totalCartAmount)}",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = NeuBlack
                            )
                        }

                        Text(
                            text = "💡 Transfer terms: Pay on transfer pickup via UPI or Cash.",
                            fontSize = 11.sp,
                            color = NeuGray
                        )
                    }
                }
            },
            confirmButton = {
                NeuButton(
                    text = if (uiState.isPlacingOrder) "Placing..." else "Confirm & Order",
                    onClick = { viewModel.placeOrder() },
                    backgroundColor = NeuGreen,
                    textColor = NeuBlack,
                    enabled = !uiState.isPlacingOrder && uiState.cart.isNotEmpty(),
                    shadowOffset = 2.dp
                )
            },
            dismissButton = {
                if (!uiState.isPlacingOrder) {
                    NeuButton(
                        text = "Cancel",
                        onClick = { viewModel.closeCheckout() },
                        backgroundColor = NeuSurface,
                        textColor = NeuBlack,
                        shadowOffset = 2.dp
                    )
                }
            }
        )
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    cart: List<CartItem>,
    onAddToCart: (NearbyStoreProduct, Int) -> Unit
) {
    val isUser = message.isUser
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .neuShadow(1.dp, 1.dp, NeuBlack, 6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeuYellow)
                        .border(BorderStroke(1.5.dp, NeuBlack), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🤖", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .neuShadow(2.dp, 2.dp, NeuBlack, 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isUser) NeuBlue else NeuSurface)
                    .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    color = if (isUser) Color.White else NeuBlack,
                    fontWeight = if (isUser) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // Interactive Partner Store Options (Add to Cart Cards)
        if (!message.nearbyOptions.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .padding(start = 40.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "🏬 Select store to order & add to cart:",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = NeuBlack
                )

                message.nearbyOptions.forEach { option ->
                    NearbyStoreOrderCard(
                        option = option,
                        cart = cart,
                        onAddToCart = onAddToCart
                    )
                }
            }
        }
    }
}

@Composable
fun NearbyStoreOrderCard(
    option: NearbyStoreProduct,
    cart: List<CartItem>,
    onAddToCart: (NearbyStoreProduct, Int) -> Unit
) {
    var quantity by remember(option.productId) { mutableIntStateOf(1) }
    val inCartItem = cart.find { it.product.productId == option.productId }
    val inCartCount = inCartItem?.quantity ?: 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neuShadow(2.dp, 2.dp, NeuBlack, 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8FAFC))
            .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Store Name & Distance
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = option.storeName,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = NeuBlack,
                    modifier = Modifier.weight(1f)
                )

                val distStr = if (option.distanceKm != null) "${option.distanceKm} km" else "Nearby"
                NeuBadge(
                    text = distStr,
                    backgroundColor = NeuYellow,
                    textColor = NeuBlack
                )
            }

            // Product & Stock Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "₹${option.price}",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = NeuBlack
                    )
                    Text(
                        text = "/ unit",
                        fontSize = 11.sp,
                        color = NeuGray
                    )
                }

                NeuBadge(
                    text = "${option.stock} in stock",
                    backgroundColor = if (option.stock > 10) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                    textColor = NeuBlack
                )
            }

            if (!option.storeAddress.isNullOrBlank()) {
                Text(
                    text = "📍 ${option.storeAddress}",
                    fontSize = 11.sp,
                    color = NeuGray,
                    maxLines = 1
                )
            }

            HorizontalDivider(color = NeuBlack.copy(alpha = 0.15f), thickness = 1.dp)

            // Quantity & Add to Cart Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Quantity Selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .neuShadow(1.dp, 1.dp, NeuBlack, 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeuSurface)
                            .border(BorderStroke(1.5.dp, NeuBlack), RoundedCornerShape(6.dp))
                            .clickable {
                                if (quantity > 1) quantity--
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-", fontWeight = FontWeight.Black, fontSize = 16.sp, color = NeuBlack)
                    }

                    Box(
                        modifier = Modifier
                            .widthIn(min = 28.dp)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$quantity",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = NeuBlack
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .neuShadow(1.dp, 1.dp, NeuBlack, 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeuYellow)
                            .border(BorderStroke(1.5.dp, NeuBlack), RoundedCornerShape(6.dp))
                            .clickable {
                                if (quantity < option.stock) quantity++
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontWeight = FontWeight.Black, fontSize = 16.sp, color = NeuBlack)
                    }
                }

                // Add to Cart Button
                NeuButton(
                    text = if (inCartCount > 0) "✓ In Cart ($inCartCount)" else "🛒 Add to Cart",
                    onClick = {
                        onAddToCart(option, quantity)
                    },
                    backgroundColor = if (inCartCount > 0) NeuGreen else NeuYellow,
                    textColor = NeuBlack,
                    shadowOffset = 2.dp
                )
            }
        }
    }
}
