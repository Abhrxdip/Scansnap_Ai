package com.smartvendor.ai.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvendor.ai.network.ApiClient
import com.smartvendor.ai.network.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val nearbyOptions: List<NearbyStoreProduct>? = null
)

data class CartItem(
    val product: NearbyStoreProduct,
    val quantity: Int = 1
)

data class AiChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            text = "👋 Hello! I'm your ScanSnap Store Assistant.\n\nYou can ask me:\n• \"Is Maggi available?\"\n• \"What is the price of Amul milk?\"\n• \"Where else can I find Maggi?\"",
            isUser = false
        )
    ),
    val isLoading: Boolean = false,
    val error: String? = null,
    val cart: List<CartItem> = emptyList(),
    val isCheckoutSheetVisible: Boolean = false,
    val isPlacingOrder: Boolean = false,
    val orderNotification: String? = null
) {
    val totalCartAmount: Double
        get() = cart.sumOf { it.product.price * it.quantity }

    val totalCartItems: Int
        get() = cart.sumOf { it.quantity }
}

class AiChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()
    
    private var lastSentMessage: String? = null

    fun sendMessage(text: String) {
        val userMessage = ChatMessage(text = text, isUser = true)
        _uiState.update { it.copy(
            messages = it.messages + userMessage,
            isLoading = true,
            error = null
        ) }
        lastSentMessage = text
        fetchResponse(text)
    }
    
    fun retryLastMessage() {
        lastSentMessage?.let { text ->
            _uiState.update { it.copy(isLoading = true, error = null) }
            fetchResponse(text)
        }
    }

    private fun fetchResponse(message: String) {
        viewModelScope.launch {
            try {
                val apiResponse = ApiClient.apiService.sendAiChat(AiChatRequest(message = message))
                if (apiResponse.isSuccessful) {
                    val body = apiResponse.body()
                    if (body != null && body.success) {
                        val aiMessage = ChatMessage(
                            text = body.response ?: "No response received",
                            isUser = false,
                            nearbyOptions = body.nearbyOptions
                        )
                        _uiState.update { it.copy(
                            messages = it.messages + aiMessage,
                            isLoading = false
                        ) }
                    } else {
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = body?.error ?: "Failed to get AI response"
                        ) }
                    }
                } else {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Server error (${apiResponse.code()}): ${apiResponse.message()}"
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Network error connecting to backend"
                ) }
            }
        }
    }

    fun addToCart(product: NearbyStoreProduct, quantity: Int = 1) {
        _uiState.update { state ->
            val existing = state.cart.find { it.product.productId == product.productId }
            val newCart = if (existing != null) {
                state.cart.map {
                    if (it.product.productId == product.productId) {
                        it.copy(quantity = it.quantity + quantity)
                    } else it
                }
            } else {
                state.cart + CartItem(product = product, quantity = quantity)
            }
            state.copy(cart = newCart)
        }
    }

    fun updateCartQuantity(productId: String, newQuantity: Int) {
        _uiState.update { state ->
            val newCart = if (newQuantity <= 0) {
                state.cart.filter { it.product.productId != productId }
            } else {
                state.cart.map {
                    if (it.product.productId == productId) it.copy(quantity = newQuantity) else it
                }
            }
            state.copy(cart = newCart)
        }
    }

    fun removeFromCart(productId: String) {
        _uiState.update { state ->
            state.copy(cart = state.cart.filter { it.product.productId != productId })
        }
    }

    fun openCheckout() {
        _uiState.update { it.copy(isCheckoutSheetVisible = true) }
    }

    fun closeCheckout() {
        _uiState.update { it.copy(isCheckoutSheetVisible = false) }
    }

    fun dismissOrderNotification() {
        _uiState.update { it.copy(orderNotification = null) }
    }

    fun placeOrder() {
        val currentCart = _uiState.value.cart
        if (currentCart.isEmpty()) return

        val primaryStore = currentCart.first().product
        val sellerStoreId = primaryStore.storeId
        val sellerStoreName = primaryStore.storeName

        val items = currentCart.map {
            InterStoreOrderItem(
                productId = it.product.productId,
                productName = it.product.productName,
                quantity = it.quantity,
                unitPrice = it.product.price
            )
        }
        val total = _uiState.value.totalCartAmount

        _uiState.update { it.copy(isPlacingOrder = true, error = null) }

        viewModelScope.launch {
            try {
                val req = InterStoreOrderRequest(
                    sellerStoreId = sellerStoreId,
                    sellerStoreName = sellerStoreName,
                    items = items,
                    totalAmount = total,
                    deliveryNote = "Inter-store procurement transfer request via ScanSnap Copilot"
                )
                val response = ApiClient.apiService.createInterStoreOrder(req)
                if (response.isSuccessful && response.body() != null) {
                    val orderBody = response.body()!!
                    val orderSummaryText = buildString {
                        append("🎉 **Inter-Store Order Confirmed!**\n\n")
                        append("• **Order ID**: ${orderBody.orderId}\n")
                        append("• **Supplier**: ${orderBody.sellerStoreName}\n")
                        append("• **Items Ordered**:\n")
                        items.forEach { item ->
                            append("   - ${item.productName} × ${item.quantity} (₹${item.unitPrice * item.quantity})\n")
                        }
                        append("• **Total**: ₹${orderBody.totalAmount}\n\n")
                        append("📦 Transfer request has been dispatched to ${orderBody.sellerStoreName}. You can now pick up or receive the transferred stock!")
                    }

                    val confirmationMsg = ChatMessage(
                        text = orderSummaryText,
                        isUser = false
                    )

                    _uiState.update {
                        it.copy(
                            messages = it.messages + confirmationMsg,
                            cart = emptyList(),
                            isCheckoutSheetVisible = false,
                            isPlacingOrder = false,
                            orderNotification = "Order #${orderBody.orderId} placed with ${orderBody.sellerStoreName}!"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isPlacingOrder = false,
                            error = "Failed to place order (${response.code()}): ${response.message()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isPlacingOrder = false,
                        error = e.localizedMessage ?: "Failed to connect to backend for ordering"
                    )
                }
            }
        }
    }
}

