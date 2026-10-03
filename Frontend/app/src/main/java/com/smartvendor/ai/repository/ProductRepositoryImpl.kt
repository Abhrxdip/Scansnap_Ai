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

    companion object {
        @Volatile var cachedProducts: List<Product> = emptyList()
        @Volatile var isLoaded: Boolean = false

        val masterCatalog = listOf(
        // Flagship Hackathon Cross-Category Showcases
        Product(
            id = "puma_tshirt",
            name = "Puma Regular Fit T-Shirt",
            price = 1499.0,
            stock = 12,
            category = "Clothing",
            brand = "Puma",
            size = "M",
            availableSizes = "S, M, L, XL",
            floor = "1st Floor",
            section = "Fashion Section",
            aisle = "Aisle 4",
            rackNumber = "Rack A-12",
            storeName = "ScanSnap Express Kirana",
            barcode = "8901262010999"
        ),
        Product(
            id = "nike_shoes",
            name = "Nike Revolution 6 Running Shoes",
            price = 3695.0,
            stock = 8,
            category = "Footwear",
            brand = "Nike",
            size = "UK-8",
            availableSizes = "UK-7, UK-8, UK-9",
            floor = "2nd Floor",
            section = "Footwear Section",
            aisle = "Aisle 1",
            rackNumber = "Rack F-07",
            storeName = "ScanSnap Express Kirana",
            barcode = "8901262010998"
        ),
        Product(
            id = "boat_headphones",
            name = "boAt Rockerz 450 Bluetooth Headphones",
            price = 1499.0,
            stock = 20,
            category = "Electronics",
            brand = "boAt",
            floor = "Ground Floor",
            section = "Electronics & Audio",
            aisle = "Aisle 6",
            rackNumber = "Rack E-03",
            storeName = "ScanSnap Express Kirana",
            barcode = "8901262010997"
        ),
        Product(
            id = "nivia_football",
            name = "Nivia Storm Football Size 5",
            price = 549.0,
            stock = 15,
            category = "Sports",
            brand = "Nivia",
            size = "Size 5",
            availableSizes = "Size 4, Size 5",
            floor = "2nd Floor",
            section = "Sports & Fitness",
            aisle = "Aisle 5",
            rackNumber = "Rack S-02",
            storeName = "ScanSnap Express Kirana",
            barcode = "8901262010996"
        ),
        Product(
            id = "milton_flask",
            name = "Milton Thermosteel Flip Lid Flask 1000ml",
            price = 999.0,
            stock = 22,
            category = "Home",
            brand = "Milton",
            floor = "1st Floor",
            section = "Home & Kitchen",
            aisle = "Aisle 7",
            rackNumber = "Rack H-08",
            storeName = "ScanSnap Express Kirana",
            barcode = "8901262010995"
        ),

        // Hackathon Retail Dataset Packshots with Location Metadata
        Product(
            id = "maggi",
            name = "Maggi 2-Minute Masala Noodles",
            price = 14.0,
            stock = 0, // Out of stock to demonstrate Find Elsewhere
            category = "Instant Foods",
            brand = "Nestle",
            floor = "Ground Floor",
            section = "Instant Foods",
            aisle = "Aisle 3",
            rackNumber = "Rack G-02",
            storeName = "ScanSnap Express Kirana",
            barcode = "8901058852311"
        ),
        Product(
            id = "beardo",
            name = "Beardo Mariner Eau De Parfum 50ml",
            price = 799.0,
            stock = 45,
            category = "Personal Care",
            brand = "Beardo",
            floor = "Ground Floor",
            section = "Personal Care",
            aisle = "Aisle 3",
            rackNumber = "Rack P-05",
            storeName = "ScanSnap Express Kirana",
            barcode = "8906084796773"
        ),
        Product(
            id = "beardo_alt",
            name = "Beardo Mariner Perfume 50ml (Alt SKU)",
            price = 799.0,
            stock = 25,
            category = "Personal Care",
            brand = "Beardo",
            floor = "Ground Floor",
            section = "Personal Care",
            aisle = "Aisle 3",
            rackNumber = "Rack P-05",
            storeName = "ScanSnap Express Kirana",
            barcode = "8906084795998"
        ),
        Product(
            id = "amul_ice_cream",
            name = "Amul Ice Cream Cup Vanilla Magic 100ml",
            price = 30.0,
            stock = 100,
            category = "Dairy & Bakery",
            brand = "Amul",
            floor = "Ground Floor",
            section = "Dairy & Frozen",
            aisle = "Freezer Bay 1",
            rackNumber = "Rack D-01",
            storeName = "ScanSnap Express Kirana",
            barcode = "8901262010014"
        ),
        Product(
            id = "thums_up",
            name = "Thums Up Charged Carbonated Beverage 250ml",
            price = 20.0,
            stock = 100,
            category = "Beverages",
            brand = "Thums Up",
            floor = "Ground Floor",
            section = "Cold Beverages",
            aisle = "Chiller 2",
            rackNumber = "Rack B-04",
            storeName = "ScanSnap Express Kirana",
            barcode = "8901764012211"
        ),
        Product(
            id = "wild_stone",
            name = "Wild Stone Forest Spice Deodorant Soap 125g",
            price = 70.0,
            stock = 100,
            category = "Personal Care",
            brand = "Wild Stone",
            floor = "Ground Floor",
            section = "Personal Care",
            aisle = "Aisle 3",
            rackNumber = "Rack P-03",
            storeName = "ScanSnap Express Kirana",
            barcode = "8904006304218"
        ),
        Product(
            id = "oreo",
            name = "Cadbury Oreo Original Biscuits",
            price = 35.0,
            stock = 100,
            category = "Snacks & Biscuits",
            brand = "Cadbury",
            floor = "Ground Floor",
            section = "Snacks & Biscuits",
            aisle = "Aisle 2",
            rackNumber = "Rack S-04",
            storeName = "ScanSnap Express Kirana",
            barcode = "7622201737018"
        ),
        Product(
            id = "cake",
            name = "Britannia Cake Gobbles Choco Chill 65g",
            price = 30.0,
            stock = 100,
            category = "Dairy & Bakery",
            brand = "Britannia",
            floor = "Ground Floor",
            section = "Bakery",
            aisle = "Aisle 2",
            rackNumber = "Rack S-06",
            storeName = "ScanSnap Express Kirana",
            barcode = "8901063142018"
        ),
        Product(
            id = "cerave",
            name = "CeraVe Hydrating Cleanser 236ml",
            price = 900.0,
            stock = 100,
            category = "Personal Care",
            brand = "CeraVe",
            floor = "Ground Floor",
            section = "Personal Care",
            aisle = "Aisle 3",
            rackNumber = "Rack P-01",
            storeName = "ScanSnap Express Kirana",
            barcode = "3337875597371"
        ),
        Product(
            id = "hns_shampoo",
            name = "Head & Shoulders Cool Menthol Shampoo 180ml",
            price = 250.0,
            stock = 100,
            category = "Personal Care",
            brand = "Head & Shoulders",
            floor = "Ground Floor",
            section = "Personal Care",
            aisle = "Aisle 3",
            rackNumber = "Rack P-02",
            storeName = "ScanSnap Express Kirana",
            barcode = "4902430730013"
        ),
        Product(
            id = "nestle_milk_powder",
            name = "Nestle Everyday Dairy Whitener 20g",
            price = 10.0,
            stock = 100,
            category = "Dairy & Beverages",
            brand = "Nestle",
            floor = "Ground Floor",
            section = "Dairy",
            aisle = "Aisle 1",
            rackNumber = "Rack D-03",
            storeName = "ScanSnap Express Kirana",
            barcode = "8901058852314"
        ),
        Product(
            id = "plum",
            name = "Plum Green Tea Pore Cleansing Face Wash 100ml",
            price = 350.0,
            stock = 100,
            category = "Personal Care",
            brand = "Plum",
            floor = "Ground Floor",
            section = "Personal Care",
            aisle = "Aisle 3",
            rackNumber = "Rack P-06",
            storeName = "ScanSnap Express Kirana",
            barcode = "8906118410214"
        ),
        Product(id = "bourbon_biscuit", name = "Britannia Bourbon Chocolate Biscuits", price = 30.0, stock = 100, category = "Snacks & Biscuits", brand = "Britannia", floor = "Ground Floor", section = "Snacks", rackNumber = "Rack S-02", barcode = "8901063012014"),
        Product(id = "milky_biscuit", name = "Britannia Milk Bikis Biscuits", price = 20.0, stock = 100, category = "Snacks & Biscuits", brand = "Britannia", floor = "Ground Floor", section = "Snacks", rackNumber = "Rack S-03", barcode = "8901063141011"),
        Product(id = "surf_excel", name = "Surf Excel Easy Wash Detergent 1kg", price = 120.0, stock = 100, category = "Laundry & Household", brand = "Surf Excel", floor = "Ground Floor", section = "Cleaning", rackNumber = "Rack H-01", barcode = "8901030012015"),
        Product(id = "hide_and_seek", name = "Parle Hide & Seek Choco Chip Biscuits", price = 30.0, stock = 100, category = "Snacks & Biscuits", brand = "Parle", floor = "Ground Floor", section = "Snacks", rackNumber = "Rack S-01", barcode = "8901719101014"),
        Product(id = "appe_fizz", name = "Appy Fizz Sparkling Apple Juice 160ml", price = 35.0, stock = 100, category = "Beverages", brand = "Parle Agro", floor = "Ground Floor", section = "Beverages", rackNumber = "Rack B-02", barcode = "8902579100018"),
        Product(id = "jim_jam", name = "Britannia Treat Jim Jam Biscuits", price = 35.0, stock = 100, category = "Snacks & Biscuits", brand = "Britannia", floor = "Ground Floor", section = "Snacks", rackNumber = "Rack S-05", barcode = "8901063015015"),
        Product(id = "nivea_deodorant", name = "Nivea Men Fresh Active Deodorant 150ml", price = 199.0, stock = 100, category = "Personal Care", brand = "Nivea", floor = "Ground Floor", section = "Personal Care", rackNumber = "Rack P-04", barcode = "4005808816033"),

        // Popular Indian FMCG Grocery & Retail Products
        Product(id = "parle_g", name = "Parle-G Original Gluco Biscuits", price = 10.0, stock = 100, category = "Snacks & Biscuits", barcode = "8901719104046"),
        Product(id = "parle_g_alt", name = "Parle-G Gold Biscuits", price = 20.0, stock = 100, category = "Snacks & Biscuits", barcode = "8901719101007"),
        Product(id = "lays_magic", name = "Lay's India's Magic Masala Chips", price = 20.0, stock = 100, category = "Snacks & Namkeen", barcode = "8901491101835"),
        Product(id = "lays_cream", name = "Lay's American Style Cream & Onion", price = 20.0, stock = 100, category = "Snacks & Namkeen", barcode = "8901491101804"),
        Product(id = "lays_tomato", name = "Lay's Spanish Tomato Tango Chips", price = 20.0, stock = 100, category = "Snacks & Namkeen", barcode = "8901491101828"),
        Product(id = "kurkure_masala", name = "Kurkure Masala Munch Namkeen", price = 20.0, stock = 100, category = "Snacks & Namkeen", barcode = "8901491501017"),
        Product(id = "dairy_milk", name = "Cadbury Dairy Milk Chocolate", price = 40.0, stock = 100, category = "Chocolates & Sweets", barcode = "7622201737001"),
        Product(id = "dairy_milk_silk", name = "Cadbury Dairy Milk Silk Chocolate", price = 80.0, stock = 100, category = "Chocolates & Sweets", barcode = "7622201737049"),
        Product(id = "kitkat", name = "Nestle KitKat 4-Finger Chocolate", price = 25.0, stock = 100, category = "Chocolates & Sweets", barcode = "8901058852328"),
        Product(id = "good_day_butter", name = "Britannia Good Day Butter Cookies", price = 25.0, stock = 100, category = "Snacks & Biscuits", barcode = "8901063012021"),
        Product(id = "good_day_cashew", name = "Britannia Good Day Cashew Cookies", price = 30.0, stock = 100, category = "Snacks & Biscuits", barcode = "8901063012038"),
        Product(id = "marie_gold", name = "Britannia Marie Gold Tea Biscuits", price = 30.0, stock = 100, category = "Snacks & Biscuits", barcode = "8901063012052"),
        Product(id = "dettol_soap", name = "Dettol Original Germ Protection Soap", price = 45.0, stock = 100, category = "Personal Care", barcode = "8901030012022"),
        Product(id = "colgate", name = "Colgate Strong Teeth Dental Cream", price = 55.0, stock = 100, category = "Personal Care", barcode = "8901314010511"),
        Product(id = "coca_cola", name = "Coca-Cola Original Taste 250ml Can", price = 20.0, stock = 100, category = "Beverages & Drinks", barcode = "8901764012204"),
        Product(id = "sprite", name = "Sprite Lime Carbonated Drink 250ml", price = 20.0, stock = 100, category = "Beverages & Drinks", barcode = "8901764012235"),
        Product(id = "tata_salt", name = "Tata Salt Vacuum Evaporated Iodized 1kg", price = 28.0, stock = 100, category = "Grocery & Staples", barcode = "8901030012046"),
        Product(id = "aashirvaad_atta", name = "Aashirvaad Shuddha Chakki Atta 5kg", price = 230.0, stock = 100, category = "Atta, Rice & Grains", barcode = "8901030012053"),
        Product(id = "frooti", name = "Frooti Fresh 'N' Juicy Mango Drink 160ml", price = 15.0, stock = 100, category = "Beverages & Drinks", barcode = "8902579100025"),
        Product(id = "maaza", name = "Maaza Real Mango Beverage 250ml", price = 20.0, stock = 100, category = "Beverages & Drinks", barcode = "8901764012242"),
        Product(id = "red_label", name = "Brooke Bond Red Label Tea 250g", price = 135.0, stock = 100, category = "Dairy & Beverages", barcode = "8901058852342"),
        Product(id = "soya_sticks", name = "Soya Sticks Crispy Namkeen 150g", price = 30.0, stock = 100, category = "Snacks & Namkeen", barcode = "8901262010161")
    )
    }

    private fun isBarcodeMatch(b1: String, b2: String): Boolean {
        val s1 = b1.trim()
        val s2 = b2.trim()
        if (s1.isEmpty() || s2.isEmpty()) return false
        if (s1 == s2) return true
        val stripped1 = s1.trimStart('0')
        val stripped2 = s2.trimStart('0')
        if (stripped1.isNotEmpty() && stripped1 == stripped2) return true
        if (s1.length >= 8 && s2.length >= 8) {
            if (s1.startsWith(s2) || s2.startsWith(s1)) return true
        }
        return false
    }

    private fun resolveGs1Product(barcode: String): Product {
        val clean = barcode.trim()
        val stripped = clean.trimStart('0')

        return when {
            stripped.startsWith("8901058") -> Product(
                id = "nestle_$clean",
                name = "Nestle Maggi FMCG Pack",
                price = 14.0,
                stock = 50,
                category = "Instant Foods",
                barcode = clean
            )
            stripped.startsWith("8901063") -> Product(
                id = "britannia_$clean",
                name = "Britannia Bakery Product",
                price = 30.0,
                stock = 50,
                category = "Snacks & Biscuits",
                barcode = clean
            )
            stripped.startsWith("8901719") -> Product(
                id = "parle_$clean",
                name = "Parle Biscuit Pack",
                price = 10.0,
                stock = 50,
                category = "Snacks & Biscuits",
                barcode = clean
            )
            stripped.startsWith("8901491") -> Product(
                id = "lays_$clean",
                name = "Lay's / Kurkure Crispy Snack",
                price = 20.0,
                stock = 50,
                category = "Snacks & Namkeen",
                barcode = clean
            )
            stripped.startsWith("7622201") -> Product(
                id = "cadbury_$clean",
                name = "Cadbury Dairy Milk / Oreo",
                price = 35.0,
                stock = 50,
                category = "Chocolates & Biscuits",
                barcode = clean
            )
            stripped.startsWith("8901764") -> Product(
                id = "coke_$clean",
                name = "Coca-Cola / Thums Up Beverage",
                price = 20.0,
                stock = 50,
                category = "Beverages & Drinks",
                barcode = clean
            )
            stripped.startsWith("8901262") -> Product(
                id = "amul_$clean",
                name = "Amul Fresh Dairy Product",
                price = 30.0,
                stock = 50,
                category = "Dairy & Bakery",
                barcode = clean
            )
            stripped.startsWith("8901030") -> Product(
                id = "hul_$clean",
                name = "Hindustan Unilever Retail Product",
                price = 60.0,
                stock = 50,
                category = "Household & Personal",
                barcode = clean
            )
            stripped.startsWith("8902579") -> Product(
                id = "parle_agro_$clean",
                name = "Appy Fizz / Frooti Drink",
                price = 35.0,
                stock = 50,
                category = "Beverages & Drinks",
                barcode = clean
            )
            stripped.startsWith("8904006") -> Product(
                id = "wildstone_$clean",
                name = "Wild Stone Personal Care",
                price = 70.0,
                stock = 50,
                category = "Personal Care",
                barcode = clean
            )
            stripped.startsWith("8901314") -> Product(
                id = "colgate_$clean",
                name = "Colgate Dental Care",
                price = 55.0,
                stock = 50,
                category = "Personal Care",
                barcode = clean
            )
            stripped.startsWith("4902430") -> Product(
                id = "pg_$clean",
                name = "Head & Shoulders Shampoo",
                price = 250.0,
                stock = 50,
                category = "Personal Care",
                barcode = clean
            )
            stripped.startsWith("4005808") -> Product(
                id = "nivea_$clean",
                name = "Nivea Men Fresh Active Deodorant",
                price = 199.0,
                stock = 50,
                category = "Personal Care",
                barcode = clean
            )
            stripped.startsWith("3337875") -> Product(
                id = "cerave_$clean",
                name = "CeraVe Hydrating Cleanser",
                price = 900.0,
                stock = 50,
                category = "Personal Care",
                barcode = clean
            )
            stripped.startsWith("8906084") -> Product(
                id = "beardo_$clean",
                name = "Beardo Mariner Eau De Parfum 50ml",
                price = 799.0,
                stock = 45,
                category = "Personal Care",
                barcode = clean
            )
            stripped.startsWith("890") -> Product(
                id = "retail_$clean",
                name = "Retail FMCG Pack ($clean)",
                price = 25.0,
                stock = 50,
                category = "Grocery & Retail",
                barcode = clean
            )
            else -> Product(
                id = "prod_$clean",
                name = "Retail Product #$clean",
                price = 25.0,
                stock = 50,
                category = "General Retail",
                barcode = clean
            )
        }
    }

    override suspend fun getProductByBarcode(barcode: String): Result<Product?> {
        val trimmed = barcode.trim()
        val localMatch = cachedProducts.find { isBarcodeMatch(it.barcode, trimmed) }
            ?: masterCatalog.find { isBarcodeMatch(it.barcode, trimmed) }

        if (localMatch != null) {
            return Result.success(localMatch)
        }

        try {
            val response = api.getProductByBarcode(trimmed)
            if (response.isSuccessful && response.body() != null) {
                return Result.success(response.body()!!.toDomain())
            }
        } catch (_: Exception) {}

        // Fallback to GS1 brand resolver so product name is NEVER generic
        val gs1Product = resolveGs1Product(trimmed)
        return Result.success(gs1Product)
    }

    override fun getProductsStream(): Flow<List<Product>> = flow {
        if (isLoaded && cachedProducts.isNotEmpty()) {
            emit(cachedProducts)
        } else {
            emit(masterCatalog)
        }
        try {
            val response = api.getProducts()
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val list = response.body()!!.map { it.toDomain() }
                cachedProducts = list
                isLoaded = true
                emit(list)
            } else if (!isLoaded) {
                cachedProducts = masterCatalog
                isLoaded = true
                emit(masterCatalog)
            }
        } catch (ex: Exception) {
            if (!isLoaded) {
                cachedProducts = masterCatalog
                isLoaded = true
                emit(masterCatalog)
            }
        }
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
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!!)
            } else {
                val qLower = query.lowercase().trim()
                val localMatches = masterCatalog.filter {
                    it.name.lowercase().contains(qLower) || it.category.lowercase().contains(qLower)
                }.map {
                    com.smartvendor.ai.network.models.MasterCatalogResponse(
                        id = it.id,
                        name = it.name,
                        category = it.category,
                        suggestedPrice = it.price,
                        barcode = it.barcode
                    )
                }
                Result.success(localMatches)
            }
        } catch (ex: Exception) {
            val qLower = query.lowercase().trim()
            val localMatches = masterCatalog.filter {
                it.name.lowercase().contains(qLower) || it.category.lowercase().contains(qLower)
            }.map {
                com.smartvendor.ai.network.models.MasterCatalogResponse(
                    id = it.id,
                    name = it.name,
                    category = it.category,
                    suggestedPrice = it.price,
                    barcode = it.barcode
                )
            }
            Result.success(localMatches)
        }
    }
}
