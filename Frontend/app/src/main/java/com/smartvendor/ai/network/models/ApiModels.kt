package com.smartvendor.ai.network.models

import com.google.gson.annotations.SerializedName

// ─── Store Profile ─────────────────────────────────────────────────────────────

data class StoreProfileRequest(
    val name: String = "",
    val address: String = "",
    val phone: String = "",
    val gst: String = "",
    val upi: String = ""
)

data class StoreProfileResponse(
    @SerializedName("user_id") val userId: String,
    val name: String,
    val address: String,
    val phone: String,
    val gst: String,
    val upi: String
)

// ─── Product ───────────────────────────────────────────────────────────────────

data class ProductRequest(
    val name: String,
    val barcode: String? = null,
    val category: String = "General",
    val price: Double,
    val stock: Int = 0,
    @SerializedName("low_stock_threshold") val lowStockThreshold: Int = 5,
    @SerializedName("image_url") val imageUrl: String? = null
)

data class ProductUpdateRequest(
    val name: String? = null,
    val barcode: String? = null,
    val category: String? = null,
    val price: Double? = null,
    val stock: Int? = null,
    @SerializedName("low_stock_threshold") val lowStockThreshold: Int? = null,
    @SerializedName("image_url") val imageUrl: String? = null
)

data class StockUpdateRequest(
    val stock: Int
)

data class ProductResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    val name: String,
    val barcode: String? = null,
    val category: String,
    val brand: String? = null,
    val size: String? = null,
    @SerializedName("available_sizes") val availableSizes: String? = null,
    val floor: String? = null,
    val section: String? = null,
    val aisle: String? = null,
    @SerializedName("rack_number") val rackNumber: String? = null,
    val price: Double,
    val stock: Int,
    @SerializedName("low_stock_threshold") val lowStockThreshold: Int = 5,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
) {
    fun toDomain(): com.smartvendor.ai.model.Product = com.smartvendor.ai.model.Product(
        id = id,
        name = name,
        barcode = barcode ?: "",
        category = category,
        price = price,
        stock = stock,
        lowStockThreshold = lowStockThreshold,
        imageUrl = imageUrl ?: "",
        brand = brand ?: "",
        size = size ?: "",
        availableSizes = availableSizes ?: "",
        floor = floor ?: "",
        section = section ?: "",
        aisle = aisle ?: "",
        rackNumber = rackNumber ?: ""
    )
}

// ─── Bill ──────────────────────────────────────────────────────────────────────

data class BillItemRequest(
    @SerializedName("product_id") val productId: String? = null,
    @SerializedName("product_name") val productName: String,
    val quantity: Int,
    @SerializedName("unit_price") val unitPrice: Double,
    @SerializedName("total_price") val totalPrice: Double
)

data class BillRequest(
    @SerializedName("idempotency_key") val idempotencyKey: String? = null,
    val items: List<BillItemRequest>,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("tax_amount") val taxAmount: Double = 0.0,
    @SerializedName("payment_mode") val paymentMode: String = "cash"
)

data class BillItemResponse(
    val id: String,
    @SerializedName("product_id") val productId: String?,
    @SerializedName("product_name") val productName: String,
    val quantity: Int,
    @SerializedName("unit_price") val unitPrice: Double,
    @SerializedName("total_price") val totalPrice: Double
)

data class BillResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("tax_amount") val taxAmount: Double,
    @SerializedName("payment_mode") val paymentMode: String,
    @SerializedName("created_at") val createdAt: String,
    val items: List<BillItemResponse> = emptyList()
)

// ─── Analytics ─────────────────────────────────────────────────────────────────

data class DailyRevenue(
    val date: String,
    val revenue: Double,
    @SerializedName("bill_count") val billCount: Int
)

data class TopProduct(
    @SerializedName("product_name") val productName: String,
    @SerializedName("quantity_sold") val quantitySold: Int,
    val revenue: Double
)

data class StockRecommendationItemResponse(
    @SerializedName("product_id") val productId: String,
    @SerializedName("product_name") val productName: String,
    @SerializedName("current_stock") val currentStock: Int,
    @SerializedName("recommended_reorder") val recommendedReorder: Int,
    val category: String,
    @SerializedName("peak_window") val peakWindow: String = "General Demand",
    @SerializedName("sales_velocity") val salesVelocity: String = "Moderate",
    val reasoning: String = "",
    @SerializedName("urgency_level") val urgencyLevel: String = "MEDIUM"
)

data class MarketTrendInsightResponse(
    val title: String,
    val description: String,
    @SerializedName("recommended_product") val recommendedProduct: String,
    @SerializedName("action_type") val actionType: String = "RESTOCK",
    @SerializedName("badge_label") val badgeLabel: String = "🌐 Market Trend"
)

data class MasterCatalogResponse(
    val id: String,
    val name: String,
    val category: String,
    @SerializedName("suggested_price") val suggestedPrice: Double,
    val barcode: String? = null
)

data class AnalyticsSummaryResponse(
    @SerializedName("total_revenue") val totalRevenue: Double,
    @SerializedName("total_bills") val totalBills: Int,
    @SerializedName("total_products") val totalProducts: Int,
    @SerializedName("low_stock_count") val lowStockCount: Int,
    @SerializedName("top_products") val topProducts: List<TopProduct>,
    @SerializedName("daily_revenue") val dailyRevenue: List<DailyRevenue>,
    @SerializedName("stock_recommendations") val stockRecommendations: List<StockRecommendationItemResponse> = emptyList(),
    @SerializedName("market_trends") val marketTrends: List<MarketTrendInsightResponse> = emptyList()
)

// ─── AI Chat & Inter-Store Ordering ──────────────────────────────────────────

data class NearbyStoreProduct(
    @SerializedName("product_id") val productId: String,
    @SerializedName("product_name") val productName: String,
    @SerializedName("store_id") val storeId: String,
    @SerializedName("store_name") val storeName: String,
    @SerializedName("store_address") val storeAddress: String? = "",
    @SerializedName("store_phone") val storePhone: String? = "",
    val price: Double,
    val stock: Int,
    @SerializedName("distance_km") val distanceKm: Double? = null,
    @SerializedName("time_ago") val timeAgo: String = "",
    val floor: String? = null,
    val section: String? = null,
    val aisle: String? = null,
    @SerializedName("rack_number") val rackNumber: String? = null,
    val size: String? = null
)

data class LocationMetadataResponse(
    @SerializedName("store_id") val storeId: String = "demo_user",
    @SerializedName("store_name") val storeName: String = "ScanSnap Express Kirana",
    val address: String? = "",
    val floor: String? = "Ground Floor",
    val section: String? = "Retail Section",
    val aisle: String? = "Aisle 1",
    @SerializedName("rack_number") val rackNumber: String? = "Rack A-01",
    @SerializedName("last_updated") val lastUpdated: String? = "Today"
)

data class SizeRecommendationResponse(
    @SerializedName("recommended_size") val recommendedSize: String? = null,
    @SerializedName("available_sizes") val availableSizes: List<String> = emptyList(),
    @SerializedName("unavailable_sizes") val unavailableSizes: List<String> = emptyList(),
    @SerializedName("size_matched_user_profile") val sizeMatchedUserProfile: Boolean = false
)

data class CheckoutPreviewResponse(
    val status: String = "COMING_NEXT",
    val label: String = "Checkout & Payment — Coming Next",
    val description: String = "Production payment gateway and checkout workflow ready for activation."
)

data class InstantFindResponse(
    val status: String,
    @SerializedName("match_type") val matchType: String,
    @SerializedName("match_label") val matchLabel: String,
    val confidence: Double,
    val product: ProductResponse,
    val location: LocationMetadataResponse,
    @SerializedName("size_recommendation") val sizeRecommendation: SizeRecommendationResponse? = null,
    val alternatives: List<NearbyStoreProduct> = emptyList(),
    @SerializedName("checkout_preview") val checkoutPreview: CheckoutPreviewResponse? = null
)

data class AiChatRequest(
    val message: String
)

data class AiChatResponse(
    val success: Boolean = true,
    val response: String? = null,
    @SerializedName("nearby_options") val nearbyOptions: List<NearbyStoreProduct>? = null,
    val error: String? = null
)

data class InterStoreOrderItem(
    @SerializedName("product_id") val productId: String,
    @SerializedName("product_name") val productName: String,
    val quantity: Int,
    @SerializedName("unit_price") val unitPrice: Double
)

data class InterStoreOrderRequest(
    @SerializedName("seller_store_id") val sellerStoreId: String,
    @SerializedName("seller_store_name") val sellerStoreName: String,
    val items: List<InterStoreOrderItem>,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("delivery_note") val deliveryNote: String? = "Inter-store transfer request"
)

data class InterStoreOrderResponse(
    @SerializedName("order_id") val orderId: String,
    val status: String,
    @SerializedName("seller_store_name") val sellerStoreName: String,
    @SerializedName("total_amount") val totalAmount: Double,
    val message: String
)

