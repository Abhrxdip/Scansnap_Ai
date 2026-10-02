package com.smartvendor.ai.ui.screens

import android.content.Context
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvendor.ai.ai.YoloDetectionRepository
import com.smartvendor.ai.ai.YoloUtils
import com.smartvendor.ai.barcode.BarcodeScannerManager
import com.smartvendor.ai.model.Bill
import com.smartvendor.ai.model.BillItem
import com.smartvendor.ai.model.DetectionResult
import com.smartvendor.ai.model.Product
import com.smartvendor.ai.ocr.OcrResult
import com.smartvendor.ai.ocr.OcrScannerManager
import com.smartvendor.ai.repository.ProductRepository
import com.smartvendor.ai.repository.ProductRepositoryImpl
import com.smartvendor.ai.repository.SalesRepository
import com.smartvendor.ai.repository.SalesRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class ScanUiState(
    val aiStatus: String = "Initializing AI...",
    val isBarcodeActive: Boolean = false,
    val isOcrActive: Boolean = false,
    val activeDetections: List<DetectionResult> = emptyList(),
    val detectedProduct: Product? = null,
    val detectedProductsList: List<Product> = emptyList(),
    val selectedQuantity: Int = 1,
    val currentBill: Bill? = null,
    val consecutiveFailedDetections: Int = 0,
    val showManualEntryDialog: Boolean = false,
    val ocrPrefilledName: String = "",
    val ocrPrefilledPrice: String = "",
    val inventoryProducts: List<Product> = emptyList(),
    val errorMessage: String? = null,
    val isProcessingFrame: Boolean = false,
    val autoAddEnabled: Boolean = true,
    val lastAutoAddedProduct: Product? = null,
    val lastAutoAddedTimestamp: Long = 0L
)

class ScanViewModel(
    private val productRepository: ProductRepository = ProductRepositoryImpl(),
    private val salesRepository: SalesRepository = SalesRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val yoloDetector = YoloDetectionRepository()
    private val barcodeScanner = BarcodeScannerManager()
    private val ocrScanner = OcrScannerManager()
    
    private val detectionStabilityMap = mutableMapOf<String, Int>()
    private val latchedProductIds = mutableSetOf<String>()

    private var lastDetectedProductId: String? = null
    private var lastDetectedTimestamp: Long = 0L
    private val debounceWindowMs = 3000L

    private val dismissedProductIds = ConcurrentHashMap.newKeySet<String>()
    private val recentlyAddedTimestampMap = ConcurrentHashMap<String, Long>()
    private val addedCooldownMs = 5000L

    fun initialize(context: Context, billId: String) {
        viewModelScope.launch {
            detectionStabilityMap.clear()
            latchedProductIds.clear()
            _uiState.update { it.copy(aiStatus = "🔍 YOLO Object Detection Active") }
            yoloDetector.initialize(context)
            loadBill(billId)
            observeInventory()
        }
    }

    private fun observeInventory() {
        viewModelScope.launch {
            productRepository.getProductsStream().collect { products ->
                _uiState.update { it.copy(inventoryProducts = products) }
            }
        }
    }

    fun refreshBill() {
        val currentId = _uiState.value.currentBill?.billId
        if (!currentId.isNullOrBlank()) {
            loadBill(currentId)
        }
    }

    private fun loadBill(billId: String) {
        viewModelScope.launch {
            val validId = if (billId.isNotBlank()) billId else "BILL_${System.currentTimeMillis()}"
            salesRepository.getBillById(validId).onSuccess { bill ->
                if (bill != null) {
                    val activeProductIds = bill.items.map { it.productId }.toSet()
                    val activeNames = bill.items.map { it.name.lowercase() }.toSet()

                    // Un-suppress products that were deleted or removed from the bill
                    recentlyAddedTimestampMap.keys.retainAll { key ->
                        activeProductIds.contains(key) || activeNames.contains(key.lowercase())
                    }
                    dismissedProductIds.retainAll { key ->
                        activeProductIds.contains(key) || activeNames.contains(key.lowercase())
                    }

                    _uiState.update { it.copy(currentBill = bill) }
                } else {
                    val newBill = Bill(billId = validId, items = emptyList(), subtotal = 0.0, gst = 0.0, grandTotal = 0.0)
                    salesRepository.saveBill(newBill)
                    recentlyAddedTimestampMap.clear()
                    dismissedProductIds.clear()
                    _uiState.update { it.copy(currentBill = newBill) }
                }
            }.onFailure {
                val newBill = Bill(billId = validId, items = emptyList(), subtotal = 0.0, gst = 0.0, grandTotal = 0.0)
                salesRepository.saveBill(newBill)
                recentlyAddedTimestampMap.clear()
                dismissedProductIds.clear()
                _uiState.update { it.copy(currentBill = newBill) }
            }
        }
    }

    fun toggleScanMode(useOcr: Boolean) {
        if (useOcr) setOcrMode() else setObjectDetectionMode()
    }

    fun setBarcodeMode() {
        _uiState.update {
            it.copy(
                isBarcodeActive = true,
                isOcrActive = false,
                activeDetections = emptyList(),
                detectedProduct = null,
                detectedProductsList = emptyList(),
                aiStatus = "📷 Barcode Scanner Active"
            )
        }
    }

    fun setOcrMode() {
        _uiState.update {
            it.copy(
                isBarcodeActive = false,
                isOcrActive = true,
                activeDetections = emptyList(),
                detectedProduct = null,
                detectedProductsList = emptyList(),
                aiStatus = "📝 Live Label & Price OCR Active"
            )
        }
    }

    fun setObjectDetectionMode() {
        _uiState.update {
            it.copy(
                isBarcodeActive = false,
                isOcrActive = false,
                activeDetections = emptyList(),
                detectedProduct = null,
                detectedProductsList = emptyList(),
                aiStatus = "🔍 YOLO AI Object Detection Active"
            )
        }
    }

    private var lastFrameProcessTime: Long = 0L
    private val catalogSearchCache = java.util.concurrent.ConcurrentHashMap<String, List<com.smartvendor.ai.network.models.MasterCatalogResponse>>()

    fun processFrame(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (_uiState.value.isProcessingFrame || (now - lastFrameProcessTime < 180L)) {
            imageProxy.close()
            return
        }
        lastFrameProcessTime = now

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingFrame = true) }

            // 1. Dedicated Barcode Scanner Mode (Direct CameraX Image Analysis)
            if (_uiState.value.isBarcodeActive) {
                barcodeScanner.scanImage(
                    imageProxy = imageProxy,
                    onSuccess = { barcode ->
                        handleBarcodeDetected(barcode)
                    },
                    onNotFound = {
                        _uiState.update { it.copy(isProcessingFrame = false) }
                    },
                    onError = {
                        _uiState.update { it.copy(isProcessingFrame = false) }
                    }
                )
                return@launch
            }

            // Convert ImageProxy to Bitmap and close buffer immediately for smooth CameraX streaming
            val bitmap = try {
                val raw = imageProxy.toBitmap()
                val rot = imageProxy.imageInfo.rotationDegrees
                if (rot != 0) {
                    val mat = android.graphics.Matrix().apply { postRotate(rot.toFloat()) }
                    android.graphics.Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, mat, true)
                } else raw
            } catch (e: Exception) {
                null
            } finally {
                try { imageProxy.close() } catch (_: Exception) {}
            }

            if (bitmap == null) {
                _uiState.update { it.copy(isProcessingFrame = false) }
                return@launch
            }

            // 2. OCR Mode: Check for Barcode first (dual-scan support), then Text & Price OCR
            if (_uiState.value.isOcrActive) {
                barcodeScanner.scanBitmap(
                    bitmap = bitmap,
                    onSuccess = { detectedBarcode ->
                        handleBarcodeDetected(detectedBarcode)
                    },
                    onNotFound = {
                        // No barcode in view -> Process with ML Kit OCR for Brand, Product & Price
                        ocrScanner.processBitmap(
                            bitmap = bitmap,
                            onSuccess = { ocrResult ->
                                viewModelScope.launch {
                                    val matched = matchOcrProduct(ocrResult)
                                    if (!matched) {
                                        _uiState.update { it.copy(isProcessingFrame = false) }
                                    }
                                }
                            },
                            onNotFound = {
                                _uiState.update { it.copy(isProcessingFrame = false) }
                            },
                            onError = {
                                _uiState.update { it.copy(isProcessingFrame = false) }
                            }
                        )
                    },
                    onError = {
                        // On barcode analysis error, continue to OCR
                        ocrScanner.processBitmap(
                            bitmap = bitmap,
                            onSuccess = { ocrResult ->
                                viewModelScope.launch {
                                    val matched = matchOcrProduct(ocrResult)
                                    if (!matched) {
                                        _uiState.update { it.copy(isProcessingFrame = false) }
                                    }
                                }
                            },
                            onNotFound = {
                                _uiState.update { it.copy(isProcessingFrame = false) }
                            },
                            onError = {
                                _uiState.update { it.copy(isProcessingFrame = false) }
                            }
                        )
                    }
                )
            } else {
                // Pure Object & Image Detection Mode (YOLOv11 & Visual Classifier)
                val result = yoloDetector.detectFromBitmap(bitmap, confThreshold = 0.25f)
                if (result != null && result.detections.isNotEmpty()) {
                    _uiState.update { it.copy(consecutiveFailedDetections = 0) }
                    handleYoloMultiDetected(result.detections)
                } else {
                    detectionStabilityMap.clear()
                    latchedProductIds.clear()
                    _uiState.update { it.copy(isProcessingFrame = false, activeDetections = emptyList()) }
                }
            }
        }
    }

    private suspend fun matchOcrProduct(ocrResult: OcrResult): Boolean {
        return try {
            val products = _uiState.value.inventoryProducts
            val now = System.currentTimeMillis()

            // 1. Check Store Inventory with High Confidence Threshold
            val rankedInventoryMatches = ocrScanner.findRankedInventoryMatches(
                ocrResult = ocrResult,
                inventoryProducts = products,
                threshold = 0.60f
            )

            val currentBilledIds = _uiState.value.currentBill?.items?.map { it.productId }?.toSet() ?: emptySet()
            val currentBilledNames = _uiState.value.currentBill?.items?.map { it.name.lowercase() }?.toSet() ?: emptySet()

            for (storeMatch in rankedInventoryMatches) {
                // Suppress if already present in current bill
                if (currentBilledIds.contains(storeMatch.id) || currentBilledNames.contains(storeMatch.name.lowercase())) {
                    continue
                }

                val lastAdded = maxOf(
                    recentlyAddedTimestampMap[storeMatch.id] ?: 0L,
                    recentlyAddedTimestampMap[storeMatch.name.lowercase()] ?: 0L
                )

                val isRecentlyAdded = (now - lastAdded < addedCooldownMs)
                val isDismissed = (dismissedProductIds.contains(storeMatch.id) || dismissedProductIds.contains(storeMatch.name.lowercase()))

                if (isRecentlyAdded || isDismissed) {
                    continue
                }

                // Found high-confidence inventory match
                detectionStabilityMap.clear()
                latchedProductIds.clear()
                _uiState.update {
                    it.copy(
                        detectedProduct = storeMatch,
                        detectedProductsList = listOf(storeMatch),
                        selectedQuantity = 1,
                        aiStatus = "⚡ Detected: ${storeMatch.name}",
                        isProcessingFrame = false
                    )
                }
                return true
            }

            // 2. Strict Search in 6,000 Master Catalog Reference Items
            val fullQuery = ocrResult.productName.takeIf { it.isNotBlank() } ?: ocrResult.fullCombinedName
            val brandTokens = fullQuery.split(" ").filter { it.length >= 3 }

            if (brandTokens.isNotEmpty()) {
                val brandKeyword = brandTokens.first().lowercase()

                val catalogResult = catalogSearchCache.getOrPut(brandKeyword) {
                    val remoteRes = productRepository.searchMasterCatalog(brandKeyword).getOrDefault(emptyList())
                    if (remoteRes.isEmpty() && brandKeyword != fullQuery.lowercase()) {
                        productRepository.searchMasterCatalog(fullQuery).getOrDefault(emptyList())
                    } else {
                        remoteRes
                    }
                }

                val rankedCatalogMatches = ocrScanner.findRankedCatalogMatches(
                    ocrResult = ocrResult,
                    catalogItems = catalogResult,
                    threshold = 0.70f
                )

                for (matchedCatalogItem in rankedCatalogMatches) {
                    val isDismissed = (dismissedProductIds.contains(matchedCatalogItem.id ?: "") || dismissedProductIds.contains(matchedCatalogItem.name.lowercase()))
                    if (isDismissed) continue

                    val existingInInventory = products.firstOrNull {
                        it.name.equals(matchedCatalogItem.name, ignoreCase = true) ||
                                (!matchedCatalogItem.barcode.isNullOrBlank() && it.barcode == matchedCatalogItem.barcode)
                    }

                    val targetProduct = existingInInventory ?: Product(
                        id = matchedCatalogItem.id ?: "CAT_${System.currentTimeMillis()}",
                        name = matchedCatalogItem.name,
                        price = matchedCatalogItem.suggestedPrice ?: 10.0,
                        stock = 50,
                        category = matchedCatalogItem.category ?: "Grocery",
                        barcode = matchedCatalogItem.barcode ?: ""
                    )

                    val lastAdded = maxOf(
                        recentlyAddedTimestampMap[targetProduct.id] ?: 0L,
                        recentlyAddedTimestampMap[targetProduct.name.lowercase()] ?: 0L
                    )

                    if (now - lastAdded < addedCooldownMs) {
                        _uiState.update { it.copy(isProcessingFrame = false) }
                        return true
                    }

                    detectionStabilityMap.clear()
                    latchedProductIds.clear()
                    _uiState.update {
                        it.copy(
                            detectedProduct = targetProduct,
                            detectedProductsList = listOf(targetProduct),
                            selectedQuantity = 1,
                            aiStatus = "⚡ Catalog: ${targetProduct.name}",
                            isProcessingFrame = false
                        )
                    }
                    return true
                }
            }

            false
        } catch (ex: Exception) {
            false
        }
    }

    private fun handleOcrDetected(ocrResult: OcrResult) {
        viewModelScope.launch {
            matchOcrProduct(ocrResult)
        }
    }

    private fun handleLowConfidence() {
        _uiState.update { it.copy(isProcessingFrame = false) }
    }

    /**
     * Handles simultaneous multi-object YOLO detections in a single camera frame.
     */
    private fun handleYoloMultiDetected(detections: List<com.smartvendor.ai.network.models.YoloDetection>) {
        viewModelScope.launch {
            val products = _uiState.value.inventoryProducts
            if (products.isEmpty()) {
                _uiState.update { it.copy(isProcessingFrame = false) }
                return@launch
            }

            val now = System.currentTimeMillis()

            // 1. Build Overlay Bounding Boxes
            val overlayDetections = detections.mapIndexed { idx, det ->
                val bbox = if (det.bbox.size == 4) {
                    android.graphics.RectF(det.bbox[0], det.bbox[1], det.bbox[2], det.bbox[3])
                } else {
                    android.graphics.RectF(0f, 0f, 0f, 0f)
                }
                val friendly = when (det.label.lowercase().trim()) {
                    "appe_fizz" -> "Appy Fizz"
                    "surf_excel" -> "Surf Excel"
                    "hide_and_seek" -> "Hide & Seek"
                    "jim_jam" -> "Jim Jam"
                    "oreo" -> "Oreo"
                    "maggi" -> "Maggi"
                    "bourbon", "bourbon_biscuit" -> "Bourbon"
                    "nivea", "nivea_deodorant" -> "Nivea"
                    "amul_ice_cream" -> "Amul Ice Cream"
                    "cake" -> "Britannia Cake"
                    "cerave" -> "CeraVe Lotion"
                    "hns_shampoo" -> "Head & Shoulders Shampoo"
                    "nestle_milk_powder" -> "Nestle Milk Powder"
                    "plum" -> "Plum Skincare"
                    "thums_up" -> "Thums Up"
                    "wild_stone" -> "Wild Stone Deodorant"
                    else -> det.label.replace("_", " ")
                }
                DetectionResult(
                    classId = idx,
                    label = friendly,
                    confidence = det.confidence,
                    boundingBox = bbox
                )
            }

            // 2. Map all detections to inventory items (Suppress items already in current bill)
            val currentBilledIds = _uiState.value.currentBill?.items?.map { it.productId }?.toSet() ?: emptySet()
            val currentBilledNames = _uiState.value.currentBill?.items?.map { it.name.lowercase() }?.toSet() ?: emptySet()

            val matchedList = mutableListOf<Product>()
            val currentFrameMatchedIds = mutableSetOf<String>()
            for (det in detections) {
                // Multi-product confidence gating: capture all clear objects in view (threshold 0.35)
                if (det.confidence < 0.25f) continue
                if (det.label.lowercase().trim() == "maggi" && det.confidence < 0.55f) continue

                val labelLower = det.label.lowercase().trim()
                val explicitTarget = when (labelLower) {
                    "amul_ice_cream", "amul_icecream" -> "Amul Ice Cream Cup Vanilla Magic"
                    "cake", "britannia_cake", "treat_cake" -> "Britannia Treat Chocolate Cake"
                    "cerave", "cerave_lotion", "cerave_cream" -> "CeraVe Daily Moisturizing Lotion"
                    "hns_shampoo", "head_and_shoulders", "head_shoulders", "hns" -> "Head & Shoulders Cool Menthol Shampoo"
                    "nestle_milk_powder", "milk_powder", "everyday_milk" -> "Nestle Everyday Dairy Whitener"
                    "plum", "plum_skincare" -> "Plum Green Tea Face Wash"
                    "thums_up", "thumsup" -> "Thums Up Charged Carbonated Drink"
                    "wild_stone", "wildstone" -> "Wild Stone Code Platinum Deodorant"
                    "nivea", "nivea_deodorant" -> "Nivea Men Fresh Active Deodorant"
                    "bourbon", "bourbon_biscuit" -> "Britannia Bourbon Chocolate Biscuits"
                    "milky_biscuit", "milk_biscuit" -> "Britannia Milk Bikis Biscuits"
                    "maggi" -> "Maggi 2-Minute Masala Noodles"
                    "surf_excel", "surf" -> "Surf Excel Easy Wash Detergent"
                    "hide_and_seek", "hide_seek" -> "Parle Hide & Seek Choco Chip Biscuits"
                    "oreo" -> "Cadbury Oreo Original Biscuits"
                    "appe_fizz", "appy_fizz", "appe", "appy" -> "Appy Fizz Sparkling Apple Juice"
                    "jim_jam", "jimjam" -> "Britannia Treat Jim Jam Biscuits"
                    "parle_g", "parleg" -> "Parle-G Gold Biscuits"
                    "good_day", "goodday" -> "Britannia Good Day Butter Cookies"
                    else -> null
                }
                
                val normalizedLabel = labelLower.replace("_", " ")

                var matched = explicitTarget?.let { target -> products.firstOrNull { it.name.equals(target, ignoreCase = true) } }
                
                if (matched == null) {
                    matched = products.firstOrNull { it.name.equals(normalizedLabel, ignoreCase = true) }
                }
                
                if (matched == null) {
                    val matchingProducts = products.filter { product ->
                        val pName = product.name.lowercase()
                        when (labelLower) {
                            "appe_fizz", "appy_fizz", "appe", "appy" -> pName.contains("appy fizz")
                            "surf_excel", "surf" -> pName.contains("surf excel")
                            "hide_and_seek", "hide_seek" -> pName.contains("hide & seek") || pName.contains("hide and seek")
                            "oreo" -> pName.contains("oreo")
                            "maggi" -> pName.contains("maggi") && pName.contains("noodles")
                            "jim_jam", "jimjam" -> pName.contains("jim jam") || pName.contains("jimjam")
                            "bourbon", "bourbon_biscuit" -> pName.contains("bourbon") && pName.contains("biscuit")
                            "nivea", "nivea_deodorant" -> pName.contains("nivea") && pName.contains("deodorant")
                            "milky_biscuit", "milk_biscuit" -> pName.contains("milk bikis")
                            "parle_g", "parleg" -> pName.contains("parle-g") || pName.contains("parle g")
                            "good_day", "goodday" -> pName.contains("good day")
                            "amul_ice_cream", "amul_icecream" -> pName.contains("amul") && pName.contains("ice cream")
                            "cake", "britannia_cake", "treat_cake" -> pName.contains("cake") && pName.contains("treat")
                            "cerave", "cerave_lotion", "cerave_cream" -> pName.contains("cerave")
                            "hns_shampoo", "head_and_shoulders", "head_shoulders", "hns" -> pName.contains("head & shoulders") || pName.contains("head and shoulders")
                            "nestle_milk_powder", "milk_powder", "everyday_milk" -> pName.contains("everyday") && pName.contains("dairy")
                            "plum", "plum_skincare" -> pName.contains("plum") && pName.contains("green tea")
                            "thums_up", "thumsup" -> pName.contains("thums up")
                            "wild_stone", "wildstone" -> pName.contains("wild stone")
                            else -> {
                                val cleanTokens = labelLower.replace("_", " ").split(" ").filter { it.length > 3 }
                                if (cleanTokens.isEmpty()) false else cleanTokens.all { token -> pName.contains(token) }
                            }
                        }
                    }
                    if (matchingProducts.size == 1) {
                        matched = matchingProducts.first()
                    }
                }
                if (matched != null && !matchedList.any { it.id == matched.id }) {
                    val isNewInFrame = currentFrameMatchedIds.add(matched.id)
                    if (isNewInFrame) {
                        val isLatched = latchedProductIds.contains(matched.id)
                        val lastAdded = maxOf(
                            recentlyAddedTimestampMap[matched.id] ?: 0L,
                            recentlyAddedTimestampMap[matched.name.lowercase()] ?: 0L
                        )
                        val isDismissed = dismissedProductIds.contains(matched.id) ||
                                dismissedProductIds.contains(matched.name.lowercase())

                        // Proceed if not currently latched, not on cooldown, and not dismissed
                        if (!isLatched && now - lastAdded >= addedCooldownMs && !isDismissed) {
                            val count = detectionStabilityMap.getOrDefault(matched.id, 0) + 1
                            detectionStabilityMap[matched.id] = count

                            // Adaptive Confidence-Based Latching Speed:
                            // High confidence (>=0.80) adds immediately in 1 frame!
                            // Mid confidence (>=0.65) requires 2 frames.
                            // Lower confidence (<0.65) requires 3 frames to reject noise.
                            val requiredFrames = when {
                                det.confidence >= 0.65f -> 1
                                else -> 2
                            }

                            if (count >= requiredFrames) {
                                matchedList.add(matched)
                                detectionStabilityMap.remove(matched.id)
                                latchedProductIds.add(matched.id)
                            }
                        }
                    }
                }
            }

            detectionStabilityMap.keys.retainAll(currentFrameMatchedIds)
            latchedProductIds.retainAll(currentFrameMatchedIds)

            if (matchedList.isNotEmpty()) {
                val currentBillState = _uiState.value.currentBill ?: Bill(
                    billId = "BILL_${System.currentTimeMillis()}",
                    items = emptyList()
                )
                val existingItems = currentBillState.items.toMutableList()
                val newlyAdded = mutableListOf<Product>()

                for (product in matchedList) {
                    recentlyAddedTimestampMap[product.id] = now
                    recentlyAddedTimestampMap[product.name.lowercase()] = now

                    val existingIndex = existingItems.indexOfFirst { it.productId == product.id }
                    if (existingIndex >= 0) {
                        val oldItem = existingItems[existingIndex]
                        val newQty = oldItem.quantity + 1
                        existingItems[existingIndex] = oldItem.copy(
                            quantity = newQty,
                            lineTotal = (newQty * oldItem.unitPrice) + (((newQty * oldItem.unitPrice) * oldItem.gst) / 100.0)
                        )
                    } else {
                        existingItems.add(
                            BillItem(
                                productId = product.id,
                                name = product.name,
                                quantity = 1,
                                unitPrice = product.price,
                                gst = product.gst
                            )
                        )
                    }
                    newlyAdded.add(product)
                }

                val newSubtotal = existingItems.sumOf { it.quantity * it.unitPrice }
                val newGst = existingItems.sumOf { (it.quantity * it.unitPrice * it.gst) / 100.0 }
                val newGrandTotal = newSubtotal + newGst - currentBillState.discount

                val updatedBill = currentBillState.copy(
                    items = existingItems,
                    subtotal = newSubtotal,
                    gst = newGst,
                    grandTotal = newGrandTotal
                )

                salesRepository.saveBill(updatedBill)

                val statusText = if (newlyAdded.size == 1) {
                    "⚡ Added: ${newlyAdded.first().name} (₹${"%.2f".format(newlyAdded.first().price)})"
                } else {
                    "⚡ Added ${newlyAdded.size} items: ${newlyAdded.joinToString(", ") { it.name }}"
                }

                _uiState.update {
                    it.copy(
                        currentBill = updatedBill,
                        activeDetections = overlayDetections,
                        lastAutoAddedProduct = newlyAdded.lastOrNull(),
                        lastAutoAddedTimestamp = now,
                        aiStatus = statusText,
                        isProcessingFrame = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        activeDetections = overlayDetections,
                        isProcessingFrame = false
                    )
                }
            }
        }
    }

    private suspend fun fetchProductByClassId(classId: Int) {
        productRepository.getProductByClassId(classId).onSuccess { product ->
            if (product != null) {
                val now = System.currentTimeMillis()
                if (product.id == lastDetectedProductId && (now - lastDetectedTimestamp) < debounceWindowMs) {
                    _uiState.update { it.copy(isProcessingFrame = false) }
                    return@onSuccess
                }

                lastDetectedProductId = product.id
                lastDetectedTimestamp = now

                _uiState.update {
                    it.copy(
                        detectedProduct = product,
                        selectedQuantity = 1,
                        isProcessingFrame = false
                    )
                }
            } else {
                handleLowConfidence()
            }
        }.onFailure {
            handleLowConfidence()
        }
    }

    private var lastScannedBarcode: String = ""
    private var lastScannedBarcodeTime: Long = 0L

    private fun handleBarcodeDetected(barcode: String) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank()) {
            _uiState.update { it.copy(isProcessingFrame = false) }
            return
        }

        val now = System.currentTimeMillis()
        if (cleanBarcode == lastScannedBarcode && (now - lastScannedBarcodeTime < 2500L)) {
            _uiState.update { it.copy(isProcessingFrame = false) }
            return
        }
        lastScannedBarcode = cleanBarcode
        lastScannedBarcodeTime = now

        viewModelScope.launch {
            // 1. Direct local inventory check (instant, offline-first)
            val localMatch = _uiState.value.inventoryProducts.find { it.barcode.trim() == cleanBarcode }
            if (localMatch != null) {
                _uiState.update {
                    it.copy(
                        detectedProduct = localMatch,
                        detectedProductsList = listOf(localMatch),
                        selectedQuantity = 1,
                        aiStatus = "✅ ${localMatch.name} (Barcode)",
                        isProcessingFrame = false
                    )
                }
                appendProductToActiveBill(localMatch, 1)
                return@launch
            }

            // 2. Query repository (Local Store Inventory + Master Catalog + GS1 Brand Resolver)
            productRepository.getProductByBarcode(cleanBarcode).onSuccess { product ->
                val finalProduct = product ?: Product(
                    id = "BC_$cleanBarcode",
                    name = "Retail FMCG Item #$cleanBarcode",
                    price = 25.0,
                    stock = 50,
                    category = "Retail FMCG",
                    barcode = cleanBarcode
                )
                _uiState.update {
                    it.copy(
                        detectedProduct = finalProduct,
                        detectedProductsList = listOf(finalProduct),
                        selectedQuantity = 1,
                        aiStatus = "✅ ${finalProduct.name}",
                        showManualEntryDialog = false,
                        isProcessingFrame = false
                    )
                }
                appendProductToActiveBill(finalProduct, 1)
            }.onFailure { _ ->
                val fallbackProduct = Product(
                    id = "BC_$cleanBarcode",
                    name = "Retail FMCG Item #$cleanBarcode",
                    price = 25.0,
                    stock = 50,
                    category = "Retail FMCG",
                    barcode = cleanBarcode
                )
                _uiState.update {
                    it.copy(
                        detectedProduct = fallbackProduct,
                        detectedProductsList = listOf(fallbackProduct),
                        selectedQuantity = 1,
                        aiStatus = "✅ ${fallbackProduct.name}",
                        showManualEntryDialog = false,
                        isProcessingFrame = false
                    )
                }
                appendProductToActiveBill(fallbackProduct, 1)
            }
        }
    }

    fun increaseQuantity() {
        val currentQty = _uiState.value.selectedQuantity
        _uiState.update { it.copy(selectedQuantity = currentQty + 1) }
    }

    fun decreaseQuantity() {
        val currentQty = _uiState.value.selectedQuantity
        if (currentQty > 1) {
            _uiState.update { it.copy(selectedQuantity = currentQty - 1) }
        }
    }

    fun addProductToBill() {
        val product = _uiState.value.detectedProduct ?: return
        val qty = _uiState.value.selectedQuantity
        appendProductToActiveBill(product, qty)
    }

    fun addExistingProductToBill(product: Product, quantity: Int) {
        appendProductToActiveBill(product, quantity)
        _uiState.update {
            it.copy(
                showManualEntryDialog = false,
                ocrPrefilledName = "",
                ocrPrefilledPrice = ""
            )
        }
    }

    private fun appendProductToActiveBill(product: Product, qty: Int) {
        val now = System.currentTimeMillis()
        recentlyAddedTimestampMap[product.id] = now
        recentlyAddedTimestampMap[product.name.lowercase()] = now

        val currentBillState = _uiState.value.currentBill ?: Bill(
            billId = "BILL_${System.currentTimeMillis()}",
            items = emptyList()
        )

        viewModelScope.launch {
            val existingItems = currentBillState.items.toMutableList()
            val existingIndex = existingItems.indexOfFirst { it.productId == product.id }

            if (existingIndex >= 0) {
                val oldItem = existingItems[existingIndex]
                val newQty = oldItem.quantity + qty
                existingItems[existingIndex] = oldItem.copy(
                    quantity = newQty,
                    lineTotal = (newQty * oldItem.unitPrice) + (((newQty * oldItem.unitPrice) * oldItem.gst) / 100.0)
                )
            } else {
                existingItems.add(
                    BillItem(
                        productId = product.id,
                        name = product.name,
                        quantity = qty,
                        unitPrice = product.price,
                        gst = product.gst
                    )
                )
            }

            val newSubtotal = existingItems.sumOf { it.quantity * it.unitPrice }
            val newGst = existingItems.sumOf { (it.quantity * it.unitPrice * it.gst) / 100.0 }
            val newGrandTotal = newSubtotal + newGst - currentBillState.discount

            val updatedBill = currentBillState.copy(
                items = existingItems,
                subtotal = newSubtotal,
                gst = newGst,
                grandTotal = newGrandTotal
            )

            salesRepository.saveBill(updatedBill).onSuccess {
                _uiState.update { state ->
                    state.copy(
                        currentBill = updatedBill,
                        detectedProduct = null,
                        selectedQuantity = 1,
                        activeDetections = emptyList(),
                        showManualEntryDialog = false,
                        ocrPrefilledName = "",
                        ocrPrefilledPrice = "",
                        aiStatus = "Live Scanner Active"
                    )
                }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(
                        currentBill = updatedBill,
                        detectedProduct = null,
                        selectedQuantity = 1,
                        showManualEntryDialog = false,
                        ocrPrefilledName = "",
                        ocrPrefilledPrice = ""
                    )
                }
            }
        }
    }

    fun saveManualProduct(name: String, price: Double, stock: Int, category: String, barcode: String, quantity: Int = 1) {
        viewModelScope.launch {
            val matchedProduct = _uiState.value.inventoryProducts.firstOrNull {
                it.name.equals(name, ignoreCase = true) || (barcode.isNotBlank() && it.barcode == barcode)
            }

            if (matchedProduct != null) {
                appendProductToActiveBill(matchedProduct, quantity)
                _uiState.update {
                    it.copy(
                        showManualEntryDialog = false,
                        ocrPrefilledName = "",
                        ocrPrefilledPrice = ""
                    )
                }
                return@launch
            }

            val initialStock = if (stock > 0) stock else 10
            val newProduct = Product(
                name = name,
                price = price,
                category = category,
                barcode = barcode,
                stock = initialStock
            )

            productRepository.addProduct(newProduct).onSuccess { id ->
                val createdProduct = newProduct.copy(id = id)
                appendProductToActiveBill(createdProduct, quantity)
            }.onFailure {
                val fallbackProduct = newProduct.copy(id = "MANUAL_${System.currentTimeMillis()}")
                appendProductToActiveBill(fallbackProduct, quantity)
            }
        }
    }

    fun openManualEntryDialog() {
        _uiState.update { it.copy(showManualEntryDialog = true) }
    }

    fun dismissManualEntryDialog() {
        _uiState.update {
            it.copy(
                showManualEntryDialog = false,
                ocrPrefilledName = "",
                ocrPrefilledPrice = ""
            )
        }
    }

    fun addAllDetectedProductsToBill() {
        val list = _uiState.value.detectedProductsList
        if (list.isEmpty()) {
            addProductToBill()
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            var currentBillState = _uiState.value.currentBill ?: Bill(
                billId = "BILL_${System.currentTimeMillis()}",
                items = emptyList()
            )

            val existingItems = currentBillState.items.toMutableList()

            for (product in list) {
                recentlyAddedTimestampMap[product.id] = now
                recentlyAddedTimestampMap[product.name.lowercase()] = now

                val existingIndex = existingItems.indexOfFirst { it.productId == product.id }
                if (existingIndex >= 0) {
                    val oldItem = existingItems[existingIndex]
                    val newQty = oldItem.quantity + 1
                    existingItems[existingIndex] = oldItem.copy(
                        quantity = newQty,
                        lineTotal = (newQty * oldItem.unitPrice) + (((newQty * oldItem.unitPrice) * oldItem.gst) / 100.0)
                    )
                } else {
                    existingItems.add(
                        BillItem(
                            productId = product.id,
                            name = product.name,
                            quantity = 1,
                            unitPrice = product.price,
                            gst = product.gst
                        )
                    )
                }
            }

            val newSubtotal = existingItems.sumOf { it.quantity * it.unitPrice }
            val newGst = existingItems.sumOf { (it.quantity * it.unitPrice * it.gst) / 100.0 }
            val newGrandTotal = newSubtotal + newGst - currentBillState.discount

            val updatedBill = currentBillState.copy(
                items = existingItems,
                subtotal = newSubtotal,
                gst = newGst,
                grandTotal = newGrandTotal
            )

            salesRepository.saveBill(updatedBill).onSuccess {
                _uiState.update { state ->
                    state.copy(
                        currentBill = updatedBill,
                        detectedProduct = null,
                        detectedProductsList = emptyList(),
                        activeDetections = emptyList(),
                        selectedQuantity = 1,
                        aiStatus = "✅ Added ${list.size} items to bill"
                    )
                }
            }
        }
    }

    fun removeDetectedProductFromList(product: Product) {
        dismissedProductIds.add(product.id)
        dismissedProductIds.add(product.name.lowercase())

        val updatedList = _uiState.value.detectedProductsList.filter { it.id != product.id }
        val newSelected = if (_uiState.value.detectedProduct?.id == product.id) {
            updatedList.firstOrNull()
        } else {
            _uiState.value.detectedProduct
        }

        if (updatedList.isEmpty()) {
            _uiState.update {
                it.copy(
                    detectedProduct = null,
                    detectedProductsList = emptyList(),
                    activeDetections = emptyList(),
                    aiStatus = "Live Scanner Active"
                )
            }
        } else {
            val statusText = if (updatedList.size == 1) {
                "🎯 YOLO: ${updatedList.first().name}"
            } else {
                "🎯 ${updatedList.size} Products Detected (${updatedList.joinToString(", ") { it.name }})"
            }
            _uiState.update {
                it.copy(
                    detectedProduct = newSelected,
                    detectedProductsList = updatedList,
                    selectedQuantity = 1,
                    aiStatus = statusText
                )
            }
        }
    }

    fun selectDetectedProduct(product: Product) {
        _uiState.update {
            it.copy(
                detectedProduct = product,
                selectedQuantity = 1
            )
        }
    }

    fun cancelDetection() {
        detectionStabilityMap.clear()
        latchedProductIds.clear()
        val currentProduct = _uiState.value.detectedProduct
        if (currentProduct != null) {
            dismissedProductIds.add(currentProduct.id)
            dismissedProductIds.add(currentProduct.name.lowercase())
        }
        for (prod in _uiState.value.detectedProductsList) {
            dismissedProductIds.add(prod.id)
            dismissedProductIds.add(prod.name.lowercase())
        }
        _uiState.update {
            it.copy(
                detectedProduct = null,
                detectedProductsList = emptyList(),
                activeDetections = emptyList(),
                selectedQuantity = 1,
                aiStatus = "Live Scanner Active"
            )
        }
    }

    fun undoLastAutoAddedProduct() {
        val last = _uiState.value.lastAutoAddedProduct ?: return
        val current = _uiState.value.currentBill ?: return
        viewModelScope.launch {
            val updatedItems = current.items.filter { it.productId != last.id }
            val newSubtotal = updatedItems.sumOf { it.quantity * it.unitPrice }
            val newGst = updatedItems.sumOf { (it.quantity * it.unitPrice * it.gst) / 100.0 }
            val newGrandTotal = newSubtotal + newGst - current.discount

            val updatedBill = current.copy(
                items = updatedItems,
                subtotal = newSubtotal,
                gst = newGst,
                grandTotal = newGrandTotal
            )
            salesRepository.saveBill(updatedBill)
            dismissedProductIds.add(last.id)
            dismissedProductIds.add(last.name.lowercase())
            _uiState.update {
                it.copy(
                    currentBill = updatedBill,
                    lastAutoAddedProduct = null,
                    lastAutoAddedTimestamp = 0L,
                    aiStatus = "↩️ Removed ${last.name}"
                )
            }
        }
    }

    fun dismissLastAddedBanner() {
        _uiState.update { it.copy(lastAutoAddedProduct = null) }
    }

    override fun onCleared() {
        super.onCleared()
        barcodeScanner.close()
        ocrScanner.close()
    }
}
