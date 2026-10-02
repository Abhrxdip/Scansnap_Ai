package com.smartvendor.ai.repository

import com.smartvendor.ai.model.Product
import com.smartvendor.ai.network.ApiClient
import com.smartvendor.ai.network.models.ProductRequest
import com.smartvendor.ai.network.models.ProductResponse
import com.smartvendor.ai.network.models.ProductUpdateRequest
import com.smartvendor.ai.network.models.StockUpdateRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ProductRepositoryImpl : ProductRepository {

    private val api = ApiClient.apiService

    // ─── Map API response → domain model ──────────────────────────────────────

    private fun ProductResponse.toDomain() = Product(
        id = id,
        name = name,
        barcode = barcode ?: "",
        category = category,
        price = price,
        stock = stock,
        lowStockThreshold = lowStockThreshold,
        imageUrl = imageUrl ?: ""
    )

    // ─── Interface implementations ─────────────────────────────────────────────

    override suspend fun getProductByClassId(classId: Int): Result<Product?> {
        return try {
            val response = api.getProducts()
            if (response.isSuccessful) {
                val product = response.body()
                    ?.getOrNull(classId)
                    ?.toDomain()
                Result.success(product)
            } else {
                Result.success(null)
            }
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    private val masterCatalog = listOf(
        Product(id = "maggi", name = "Maggi 2-Minute Masala Noodles", price = 14.0, stock = 100, category = "Instant Foods", barcode = "8901058852311"),
        Product(id = "oreo", name = "Cadbury Oreo Original Biscuits", price = 35.0, stock = 100, category = "Snacks & Biscuits", barcode = "7622201737018"),
        Product(id = "amul_ice_cream", name = "Amul Ice Cream Cup Vanilla Magic 100ml", price = 30.0, stock = 100, category = "Dairy & Bakery", barcode = "8901262010014"),
        Product(id = "cake", name = "Britannia Cake Gobbles Choco Chill 65g", price = 30.0, stock = 100, category = "Dairy & Bakery", barcode = "8901063142018"),
        Product(id = "cerave", name = "CeraVe Hydrating Cleanser 236ml", price = 900.0, stock = 100, category = "Personal Care", barcode = "3337875597371"),
        Product(id = "hns_shampoo", name = "Head & Shoulders Cool Menthol Shampoo 180ml", price = 250.0, stock = 100, category = "Personal Care", barcode = "4902430730013"),
        Product(id = "nestle_milk_powder", name = "Nestle Everyday Dairy Whitener 20g", price = 10.0, stock = 100, category = "Dairy & Beverages", barcode = "8901058852314"),
        Product(id = "plum", name = "Plum Green Tea Pore Cleansing Face Wash 100ml", price = 350.0, stock = 100, category = "Personal Care", barcode = "8906118410214"),
        Product(id = "thums_up", name = "Thums Up Charged Carbonated Beverage 250ml", price = 20.0, stock = 100, category = "Beverages", barcode = "8901764012211"),
        Product(id = "wild_stone", name = "Wild Stone Forest Spice Deodorant Soap 125g", price = 70.0, stock = 100, category = "Personal Care", barcode = "8904006304218"),
        Product(id = "bourbon_biscuit", name = "Britannia Bourbon Chocolate Biscuits", price = 30.0, stock = 100, category = "Snacks & Biscuits", barcode = "8901063012014"),
        Product(id = "milky_biscuit", name = "Britannia Milk Bikis Biscuits", price = 20.0, stock = 100, category = "Snacks & Biscuits", barcode = "8901063141011"),
        Product(id = "surf_excel", name = "Surf Excel Easy Wash Detergent 1kg", price = 120.0, stock = 100, category = "Laundry & Household", barcode = "8901030012015"),
        Product(id = "hide_and_seek", name = "Parle Hide & Seek Choco Chip Biscuits", price = 30.0, stock = 100, category = "Snacks & Biscuits", barcode = "8901719101014"),
        Product(id = "appe_fizz", name = "Appy Fizz Sparkling Apple Juice 160ml", price = 35.0, stock = 100, category = "Beverages", barcode = "8902579100018"),
        Product(id = "jim_jam", name = "Britannia Treat Jim Jam Biscuits", price = 35.0, stock = 100, category = "Snacks & Biscuits", barcode = "8901063015015"),
        Product(id = "nivea_deodorant", name = "Nivea Men Fresh Active Deodorant 150ml", price = 199.0, stock = 100, category = "Personal Care", barcode = "4005808816033")
    )

    override suspend fun getProductByBarcode(barcode: String): Result<Product?> {
        val trimmed = barcode.trim()
        val localMatch = cachedProducts.find { it.barcode.trim() == trimmed }
            ?: masterCatalog.find { it.barcode.trim() == trimmed }

        if (localMatch != null) {
            return Result.success(localMatch)
        }

        return try {
            val response = api.getProductByBarcode(trimmed)
            if (response.isSuccessful) {
                Result.success(response.body()?.toDomain() ?: localMatch)
            } else if (response.code() == 404) {
                Result.success(localMatch)
            } else {
                if (localMatch != null) Result.success(localMatch)
                else Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (ex: Exception) {
            if (localMatch != null) Result.success(localMatch)
            else Result.failure(ex)
        }
    }

    override fun getProductsStream(): Flow<List<Product>> = flow {
        if (isLoaded) {
            emit(cachedProducts)
        }
        try {
            val response = api.getProducts()
            if (response.isSuccessful) {
                val list = response.body()?.map { it.toDomain() } ?: emptyList()
                cachedProducts = list
                isLoaded = true
                emit(list)
            } else if (!isLoaded) {
                emit(emptyList())
            }
        } catch (ex: Exception) {
            if (!isLoaded) {
                emit(emptyList())
            }
        }
    }

    companion object {
        @Volatile var cachedProducts: List<Product> = emptyList()
        @Volatile var isLoaded: Boolean = false
    }

    override suspend fun updateStock(productId: String, targetStock: Int): Result<Unit> {
        return try {
            val response = api.updateStock(productId, StockUpdateRequest(targetStock.coerceAtLeast(0)))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Stock update failed: ${response.code()}"))
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    override suspend fun updateProduct(product: Product): Result<Unit> {
        return try {
            val body = ProductUpdateRequest(
                name = product.name,
                barcode = product.barcode.ifBlank { null },
                category = product.category,
                price = product.price,
                stock = product.stock,
                lowStockThreshold = product.lowStockThreshold,
                imageUrl = product.imageUrl.ifBlank { null }
            )
            val response = api.updateProduct(product.id, body)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Update failed: ${response.code()}"))
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    override suspend fun addProduct(product: Product): Result<String> {
        return try {
            if (product.id.isBlank()) {
                val body = ProductRequest(
                    name = product.name,
                    barcode = product.barcode.ifBlank { null },
                    category = product.category,
                    price = product.price,
                    stock = product.stock,
                    lowStockThreshold = product.lowStockThreshold,
                    imageUrl = product.imageUrl.ifBlank { null }
                )
                val response = api.createProduct(body)
                if (response.isSuccessful) {
                    Result.success(response.body()?.id ?: "")
                } else {
                    Result.failure(Exception("Create failed: ${response.code()}"))
                }
            } else {
                val body = ProductUpdateRequest(
                    name = product.name,
                    barcode = product.barcode.ifBlank { null },
                    category = product.category,
                    price = product.price,
                    stock = product.stock,
                    lowStockThreshold = product.lowStockThreshold,
                    imageUrl = product.imageUrl.ifBlank { null }
                )
                val response = api.updateProduct(product.id, body)
                if (response.isSuccessful) {
                    Result.success(product.id)
                } else {
                    Result.failure(Exception("Update failed: ${response.code()}"))
                }
            }
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    override suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            val response = api.deleteProduct(productId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Delete failed: ${response.code()}"))
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    override suspend fun searchMasterCatalog(query: String): Result<List<com.smartvendor.ai.network.models.MasterCatalogResponse>> {
        return try {
            val response = api.searchMasterCatalog(search = query, limit = 20)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.success(emptyList())
            }
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }
}
