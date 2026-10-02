package com.smartvendor.ai.ocr

import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.smartvendor.ai.model.Product
import java.util.Locale

enum class PackagingColor {
    YELLOW,   // Maggi, Lays, Good Day, Parle G, Frooti, Maaza, Everest Turmeric
    BLUE,     // Oreo, Dairy Milk, Surf Excel, Tata Salt, Sprite, Thums Up
    RED,      // KitKat, Red Label Tea, Coca Cola, Kissan Ketchup, Appy Fizz, Vim Bar
    GREEN,    // Aashirvaad Atta, Patanjali Atta, Moong Dal
    PURPLE,   // Dairy Milk Silk, 5 Star, Jim Jam
    ORANGE,   // Kurkure, Bourbon, Hide and Seek, Haldiram Bhujia, Soya Sticks
    UNKNOWN
}

data class OcrResult(
    val productName: String,
    val quantityUnit: String? = null,
    val fullCombinedName: String,
    val price: Double? = null,
    val matchScore: Float = 0.0f,
    val detectedColor: PackagingColor = PackagingColor.UNKNOWN
)

class OcrScannerManager {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val productColorSignatures = mapOf(
        "oreo" to PackagingColor.BLUE,
        "maggi" to PackagingColor.YELLOW,
        "lays chips" to PackagingColor.YELLOW,
        "lays" to PackagingColor.YELLOW,
        "kitkat" to PackagingColor.RED,
        "good day" to PackagingColor.YELLOW,
        "parle g" to PackagingColor.YELLOW,
        "dairy milk" to PackagingColor.BLUE,
        "dairy milk silk" to PackagingColor.PURPLE,
        "jim jam" to PackagingColor.PURPLE,
        "jimjam" to PackagingColor.PURPLE,
        "soya sticks" to PackagingColor.ORANGE,
        "hide and seek" to PackagingColor.ORANGE,
        "bourbon" to PackagingColor.ORANGE,
        "kurkure" to PackagingColor.ORANGE,
        "aashirvaad atta" to PackagingColor.GREEN,
        "patanjali atta" to PackagingColor.GREEN,
        "surf excel" to PackagingColor.BLUE,
        "tata salt" to PackagingColor.BLUE,
        "coca cola" to PackagingColor.RED,
        "frooti" to PackagingColor.YELLOW,
        "maaza" to PackagingColor.YELLOW,
        "appy fizz" to PackagingColor.RED,
        "sprite" to PackagingColor.GREEN,
        "thums up" to PackagingColor.BLUE,
        "red label tea" to PackagingColor.RED,
        "dettol soap" to PackagingColor.GREEN
    )

    private val fontAliasesMap = mapOf(
        "ore0" to "oreo", "0reo" to "oreo", "oreq" to "oreo", "orco" to "oreo", "cakoy" to "oreo", "qikany" to "oreo",
        "naggi" to "maggi", "maggl" to "maggi", "meggi" to "maggi", "mggi" to "maggi", "2-minute" to "maggi", "2 minute" to "maggi", "masala maggi" to "maggi",
        "layss" to "lays chips", "lais" to "lays chips", "layz" to "lays chips", "lay's" to "lays chips",
        "ashirvad" to "aashirvaad atta", "asirvad" to "aashirvaad atta",
        "hide 3seek" to "hide and seek", "hde seek" to "hide and seek", "hide & seek" to "hide and seek",
        "jimjan" to "jim jam", "jimiam" to "jim jam", "jimyam" to "jim jam", "jimjam" to "jim jam", "naughty jam" to "jim jam",
        "soya stica" to "soya sticks", "soya stic" to "soya sticks",
        "surf excel" to "surf excel",
        "appe fizz" to "appy fizz",
        "bourbon" to "bourbon", "burbon" to "bourbon", "bourbonn" to "bourbon", "borbon" to "bourbon",
        "bourbon biscuit" to "bourbon", "choco bourbon" to "bourbon",
        "parle-g" to "parle g", "parleg" to "parle g",
        "goodday" to "good day", "good-day" to "good day",
        "kurkure" to "kurkure", "kur kure" to "kurkure",
        "dettol" to "dettol", "detol" to "dettol",
        "colgate" to "colgate", "colgat" to "colgate"
    )

    private val noiseWords = setOf(
        "net", "wt", "mfg", "exp", "batch", "pack", "ingredients", "made", "india",
        "mrp", "incl", "taxes", "tax", "customer", "care", "lic", "iso", "store",
        "cool", "dry", "place", "recyclable", "use", "best", "before", "date",
        "weight", "grams", "kilograms", "quantity", "address", "marketed",
        "manufactured", "ltd", "pvt", "corp", "inc", "product", "details", "contact",
        "nutrition", "nutritional", "facts", "information", "per", "serve", "serving",
        "size", "energy", "protein", "carbohydrate", "sugar", "fat", "saturated",
        "trans", "cholesterol", "sodium", "calcium", "iron", "vitamins", "minerals",
        "vegetarian", "veg", "green", "dot", "fssai", "license", "reg", "tm",
        "copyright", "all", "rights", "reserved", "keep", "away", "direct", "sunlight",
        "hygienic", "conditions", "dispose", "dustbin", "scan", "qr", "feedback",
        "helpline", "toll", "free", "email", "website", "www", "com", "in",
        "barcode", "dop", "pkd", "by", "months", "from", "packaging", "super",
        "saver", "offer", "inside", "new", "improved", "taste", "delicious",
        "crunchy", "crispy", "snack", "tasty", "yummy", "original", "formula",
        "imported", "distributed", "packed", "contains", "added", "flavour",
        "artificial", "natural", "identical", "flavouring", "substances", "preservative",
        "acidity", "regulator", "emulsifier", "stabilizer", "thickener", "color", "colour",
        "allergen", "advice", "may", "contain", "traces", "of", "milk", "wheat", "soy",
        "nuts", "gluten", "peanuts", "sesame", "warning", "caution", "safety", "seal"
    )

    fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1].equals(s2[j - 1], ignoreCase = true)) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[len1][len2]
    }

    fun charSimilarity(s1: String, s2: String): Float {
        val maxLen = maxOf(s1.length, s2.length)
        if (maxLen == 0) return 1.0f
        val dist = levenshteinDistance(s1.lowercase(Locale.getDefault()), s2.lowercase(Locale.getDefault()))
        return 1.0f - (dist.toFloat() / maxLen.toFloat())
    }

    private val antiConfusionGuards = listOf(
        Pair("maggi", "maaza"),
        Pair("maaza", "maggi"),
        Pair("maggi", "munch"),
        Pair("munch", "maggi"),
        Pair("soya sticks", "snickers"),
        Pair("snickers", "soya sticks"),
        Pair("soya", "snickers"),
        Pair("snickers", "soya"),
        Pair("lays chips", "lizol"),
        Pair("lizol", "lays chips"),
        Pair("colgate", "close up"),
        Pair("close up", "colgate"),
        Pair("surf excel", "soya sticks"),
        Pair("soya sticks", "surf excel")
    )

    private fun isConflictingPair(scanned: String, target: String): Boolean {
        val sRaw = scanned.lowercase(Locale.getDefault())
        val sNorm = sRaw.replace(Regex("(.)\\1+"), "$1")
        val tNorm = target.lowercase(Locale.getDefault()).replace(Regex("(.)\\1+"), "$1")

        for ((word1, word2) in antiConfusionGuards) {
            val w1 = word1.replace(Regex("(.)\\1+"), "$1")
            val w2 = word2.replace(Regex("(.)\\1+"), "$1")
            if ((sRaw.contains(word1) || sNorm.contains(w1)) && tNorm.contains(w2)) {
                return true
            }
        }

        if ((sRaw.contains("soya") || sNorm.contains("soya")) && !tNorm.contains("soya")) return true
        if ((sRaw.contains("snicker") || sNorm.contains("sniker")) && !tNorm.contains("sniker")) return true
        if ((sRaw.contains("maaza") || sNorm.contains("maza")) && !tNorm.contains("maza")) return true
        if ((sRaw.contains("maggi") || sNorm.contains("magi")) && !tNorm.contains("magi")) return true

        return false
    }

    private val priceRegex = Regex(
        """(?:₹|M\.?\s*R\.?\s*P\.?|Rs\.?|INR)\s*[:\.\-]?\s*(?:₹|Rs\.?)?\s*(\d+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val standalonePriceRegex = Regex("""\b(\d{1,4}(?:\.\d{1,2})?)\b""")

    private val quantityUnitRegex = Regex(
        """\b(\d+(?:\.\d+)?\s*(?:kg|g|gm|l|ml|ltr|litre|pack|pc|pcs|pouch|sachet))\b""",
        RegexOption.IGNORE_CASE
    )

    @OptIn(ExperimentalGetImage::class)
    fun processImage(
        imageProxy: ImageProxy,
        onSuccess: (OcrResult) -> Unit,
        onNotFound: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            onNotFound()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                imageProxy.close()

                val fullText = visionText.text
                if (fullText.isBlank()) {
                    onNotFound()
                    return@addOnSuccessListener
                }

                val allLines = visionText.textBlocks.flatMap { it.lines }
                
                val spatialLines = allLines.mapNotNull { line ->
                    val box = line.boundingBox ?: return@mapNotNull null
                    com.smartvendor.ai.ocr.spatial.SpatialTextLine(
                        text = line.text.trim(),
                        left = box.left,
                        top = box.top,
                        right = box.right,
                        bottom = box.bottom
                    )
                }
                
                val reconstructedSpatialLines = com.smartvendor.ai.ocr.spatial.SpatialTextReconstructor.reconstruct(spatialLines)
                
                val reconstructedFullText = if (reconstructedSpatialLines.isNotEmpty()) {
                    reconstructedSpatialLines.joinToString("\n") { it.text }
                } else {
                    fullText
                }

                var detectedPrice: Double? = null
                var detectedName: String? = null
                var detectedUnit: String? = null

                // 1. Extract Price using Spatially Reconstructed Text
                val priceMatch = priceRegex.find(reconstructedFullText)
                if (priceMatch != null) {
                    val priceStr = priceMatch.groupValues[1]
                    detectedPrice = priceStr.toDoubleOrNull()
                }

                // 2. Extract Quantity/Unit using Spatially Reconstructed Text
                val unitMatch = quantityUnitRegex.find(reconstructedFullText)
                if (unitMatch != null) {
                    detectedUnit = unitMatch.groupValues[1].uppercase(Locale.getDefault())
                }

                // 3. Extract Clean Brand/Product Lines
                val finalLinesData = if (reconstructedSpatialLines.isEmpty() && allLines.isNotEmpty()) {
                    allLines.map { Pair(it.text.trim(), it.boundingBox) }
                } else {
                    reconstructedSpatialLines.map { Pair(it.text, android.graphics.Rect(it.left, it.top, it.right, it.bottom)) }
                }

                val validLines = finalLinesData
                    .filter { data ->
                        val text = data.first
                        if (text.length < 3 || text.length > 40) return@filter false
                        if (priceRegex.containsMatchIn(text)) return@filter false

                        val lineLower = text.lowercase(Locale.getDefault())
                        val tokens = lineLower.split(Regex("""[\s\-_,.:;]+""")).filter { it.isNotBlank() }

                        // Check if line is meaningful non-noise text
                        val nonNoiseTokens = tokens.filter { t -> t !in noiseWords && t.length >= 2 }
                        nonNoiseTokens.isNotEmpty()
                    }
                    // Sort descending by text area so largest brand logo font is ranked first!
                    .sortedByDescending { data ->
                        val box = data.second ?: Rect()
                        box.width() * box.height()
                    }

                if (validLines.isNotEmpty()) {
                    val topCandidateText = validLines.first().first
                    detectedName = topCandidateText
                        .lowercase(Locale.getDefault())
                        .split(" ")
                        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
                }

                // Fallback Price detection
                if (detectedPrice == null) {
                    allLines.forEach { line ->
                        if (line.text.contains("₹") || line.text.contains("Rs", ignoreCase = true)) {
                            val match = standalonePriceRegex.find(line.text)
                            if (match != null) {
                                detectedPrice = match.groupValues[1].toDoubleOrNull()
                            }
                        }
                    }
                }

                val topBox = validLines.firstOrNull()?.second
                val sampledColor = detectDominantColor(mediaImage, topBox, imageProxy.imageInfo.rotationDegrees)

                if (!detectedName.isNullOrBlank()) {
                    val combinedName = if (!detectedUnit.isNullOrBlank() && !detectedName!!.contains(detectedUnit!!, ignoreCase = true)) {
                        "$detectedName $detectedUnit"
                    } else {
                        detectedName!!
                    }

                    onSuccess(
                        OcrResult(
                            productName = detectedName!!,
                            quantityUnit = detectedUnit,
                            fullCombinedName = combinedName,
                            price = detectedPrice,
                            detectedColor = sampledColor
                        )
                    )
                } else {
                    onNotFound()
                }
            }
            .addOnFailureListener { e ->
                imageProxy.close()
                Log.e(TAG, "OCR recognition error", e)
                onError(e)
            }
    }

    /**
     * Process a Bitmap directly using ML Kit Text Recognition.
     */
    fun processBitmap(
        bitmap: android.graphics.Bitmap,
        onSuccess: (OcrResult) -> Unit,
        onNotFound: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val ocrResult = parseOcrText(visionText)
                if (ocrResult != null) {
                    onSuccess(ocrResult)
                } else {
                    // ADAPTIVE PREPROCESSING SECOND PASS (Fallback for Glare/Low Contrast)
                    try {
                        val enhancedBitmap = preprocessBitmapForOCR(bitmap)
                        val enhancedImage = InputImage.fromBitmap(enhancedBitmap, 0)
                        
                        recognizer.process(enhancedImage)
                            .addOnSuccessListener { visionText2 ->
                                val ocrResult2 = parseOcrText(visionText2)
                                if (ocrResult2 != null) {
                                    onSuccess(ocrResult2)
                                } else {
                                    onNotFound()
                                }
                            }
                            .addOnFailureListener {
                                onNotFound()
                            }
                    } catch (e: Exception) {
                        onNotFound()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "OCR recognition error on bitmap", e)
                onError(e)
            }
    }

    private fun preprocessBitmapForOCR(original: android.graphics.Bitmap): android.graphics.Bitmap {
        val result = android.graphics.Bitmap.createBitmap(original.width, original.height, original.config ?: android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        val paint = android.graphics.Paint()
        
        // Enhance contrast by 1.5x, reduce brightness by -30 to recover glare regions
        val scale = 1.5f
        val translate = -30f
        
        val matrix = android.graphics.ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        
        // Also convert to grayscale to remove color noise for OCR
        val grayscaleMatrix = android.graphics.ColorMatrix()
        grayscaleMatrix.setSaturation(0f)
        
        grayscaleMatrix.postConcat(matrix)
        
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(grayscaleMatrix)
        canvas.drawBitmap(original, 0f, 0f, paint)
        
        return result
    }

    private fun parseOcrText(visionText: com.google.mlkit.vision.text.Text): OcrResult? {
        val fullText = visionText.text
        if (fullText.isBlank()) {
            return null
        }

        val allLines = visionText.textBlocks.flatMap { it.lines }
        
        val spatialLines = allLines.mapNotNull { line ->
            val box = line.boundingBox ?: return@mapNotNull null
            com.smartvendor.ai.ocr.spatial.SpatialTextLine(
                text = line.text.trim(),
                left = box.left,
                top = box.top,
                right = box.right,
                bottom = box.bottom
            )
        }
        
        val reconstructedSpatialLines = com.smartvendor.ai.ocr.spatial.SpatialTextReconstructor.reconstruct(spatialLines)
        
        val reconstructedFullText = if (reconstructedSpatialLines.isNotEmpty()) {
            reconstructedSpatialLines.joinToString("\n") { it.text }
        } else {
            fullText
        }

        var detectedPrice: Double? = null
        var detectedName: String? = null
        var detectedUnit: String? = null

        val priceMatch = priceRegex.find(reconstructedFullText)
        if (priceMatch != null) {
            val priceStr = priceMatch.groupValues[1]
            detectedPrice = priceStr.toDoubleOrNull()
        }

        val unitMatch = quantityUnitRegex.find(reconstructedFullText)
        if (unitMatch != null) {
            detectedUnit = unitMatch.groupValues[1].uppercase(Locale.getDefault())
        }

        val finalLinesData = if (reconstructedSpatialLines.isEmpty() && allLines.isNotEmpty()) {
            allLines.map { Pair(it.text.trim(), it.boundingBox) }
        } else {
            reconstructedSpatialLines.map { Pair(it.text, android.graphics.Rect(it.left, it.top, it.right, it.bottom)) }
        }

        val validLines = finalLinesData
            .filter { data ->
                val text = data.first
                if (text.length < 3 || text.length > 40) return@filter false
                if (priceRegex.containsMatchIn(text)) return@filter false

                val lineLower = text.lowercase(Locale.getDefault())
                val tokens = lineLower.split(Regex("""[\s\-_,.:;]+""")).filter { it.isNotBlank() }

                val nonNoiseTokens = tokens.filter { t -> t !in noiseWords && t.length >= 2 }
                nonNoiseTokens.isNotEmpty()
            }
            .sortedByDescending { data ->
                val box = data.second ?: Rect()
                box.width() * box.height()
            }

        if (validLines.isNotEmpty()) {
            val topCandidateText = validLines.first().first
            detectedName = topCandidateText
                .lowercase(Locale.getDefault())
                .split(" ")
                .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
        }

        if (detectedPrice == null) {
            allLines.forEach { line ->
                if (line.text.contains("₹") || line.text.contains("Rs", ignoreCase = true)) {
                    val match = standalonePriceRegex.find(line.text)
                    if (match != null) {
                        detectedPrice = match.groupValues[1].toDoubleOrNull()
                    }
                }
            }
        }

        if (!detectedName.isNullOrBlank()) {
            val combinedName = if (!detectedUnit.isNullOrBlank() && !detectedName!!.contains(detectedUnit!!, ignoreCase = true)) {
                "$detectedName $detectedUnit"
            } else {
                detectedName!!
            }

            return OcrResult(
                productName = detectedName!!,
                quantityUnit = detectedUnit,
                fullCombinedName = combinedName,
                price = detectedPrice,
                detectedColor = PackagingColor.UNKNOWN
            )
        }
        return null
    }

    private fun detectDominantColor(yuvImage: android.media.Image, boundingBox: Rect?, rotationDegrees: Int): PackagingColor {
        return try {
            val yBuffer = yuvImage.planes[0].buffer
            val uBuffer = yuvImage.planes[1].buffer
            val vBuffer = yuvImage.planes[2].buffer

            val width = yuvImage.width
            val height = yuvImage.height

            var startX = 0
            var endX = 0
            var startY = 0
            var endY = 0

            if (boundingBox != null) {
                var mappedLeft = 0
                var mappedTop = 0
                var mappedRight = 0
                var mappedBottom = 0

                when (rotationDegrees) {
                    0 -> {
                        mappedLeft = boundingBox.left
                        mappedTop = boundingBox.top
                        mappedRight = boundingBox.right
                        mappedBottom = boundingBox.bottom
                    }
                    90 -> {
                        mappedLeft = boundingBox.top
                        mappedTop = height - boundingBox.right
                        mappedRight = boundingBox.bottom
                        mappedBottom = height - boundingBox.left
                    }
                    180 -> {
                        mappedLeft = width - boundingBox.right
                        mappedTop = height - boundingBox.bottom
                        mappedRight = width - boundingBox.left
                        mappedBottom = height - boundingBox.top
                    }
                    270 -> {
                        mappedLeft = width - boundingBox.bottom
                        mappedTop = boundingBox.left
                        mappedRight = width - boundingBox.top
                        mappedBottom = boundingBox.right
                    }
                    else -> {
                        mappedLeft = boundingBox.left
                        mappedTop = boundingBox.top
                        mappedRight = boundingBox.right
                        mappedBottom = boundingBox.bottom
                    }
                }

                mappedLeft = mappedLeft.coerceIn(0, width - 1)
                mappedRight = mappedRight.coerceIn(0, width - 1)
                mappedTop = mappedTop.coerceIn(0, height - 1)
                mappedBottom = mappedBottom.coerceIn(0, height - 1)

                val tempStartX = minOf(mappedLeft, mappedRight)
                val tempEndX = maxOf(mappedLeft, mappedRight)
                val tempStartY = minOf(mappedTop, mappedBottom)
                val tempEndY = maxOf(mappedTop, mappedBottom)

                if (tempStartX < tempEndX && tempStartY < tempEndY) {
                    startX = tempStartX
                    endX = tempEndX
                    startY = tempStartY
                    endY = tempEndY
                }
            }

            if (startX >= endX || startY >= endY) return PackagingColor.UNKNOWN

            val stepX = maxOf(1, (endX - startX) / 8)
            val stepY = maxOf(1, (endY - startY) / 8)

            val hueCounts = mutableMapOf<PackagingColor, Int>()
            var validSampleCount = 0

            for (y in startY until endY step stepY) {
                for (x in startX until endX step stepX) {
                    val yIndex = y * width + x
                    val uvIndex = (y / 2) * (width / 2) + (x / 2)

                    if (yIndex < yBuffer.capacity() && uvIndex < uBuffer.capacity() && uvIndex < vBuffer.capacity()) {
                        val Y = yBuffer.get(yIndex).toInt() and 0xFF
                        if (Y > 235 || Y < 40) continue // Ignore near-white glare and very dark shadows

                        val U = uBuffer.get(uvIndex).toInt() and 0xFF - 128
                        val V = vBuffer.get(uvIndex).toInt() and 0xFF - 128

                        val R = (Y + 1.370705 * V).toInt().coerceIn(0, 255)
                        val G = (Y - 0.337633 * U - 0.698001 * V).toInt().coerceIn(0, 255)
                        val B = (Y + 1.732446 * U).toInt().coerceIn(0, 255)

                        val hsv = FloatArray(3)
                        android.graphics.Color.RGBToHSV(R, G, B, hsv)
                        
                        if (hsv[1] < 0.20f) continue // Ignore completely desaturated/grayish pixels

                        val color = when {
                            hsv[0] in 345f..360f || hsv[0] in 0f..20f -> PackagingColor.RED
                            hsv[0] in 21f..50f -> PackagingColor.ORANGE
                            hsv[0] in 51f..75f -> PackagingColor.YELLOW
                            hsv[0] in 76f..160f -> PackagingColor.GREEN
                            hsv[0] in 180f..260f -> PackagingColor.BLUE
                            hsv[0] in 261f..320f -> PackagingColor.PURPLE
                            else -> PackagingColor.UNKNOWN
                        }

                        if (color != PackagingColor.UNKNOWN) {
                            hueCounts[color] = hueCounts.getOrDefault(color, 0) + 1
                            validSampleCount++
                        }
                    }
                }
            }

            if (validSampleCount < 10) return PackagingColor.UNKNOWN

            val dominant = hueCounts.maxByOrNull { it.value }
            // Require a minimum proportion (50%) to agree to prevent highly mixed false positives
            if (dominant != null && dominant.value >= validSampleCount * 0.5f) {
                return dominant.key
            }
            
            return PackagingColor.UNKNOWN
        } catch (e: Exception) {
            return PackagingColor.UNKNOWN
        }
    }

    /**
     * Ranked Store Inventory Matcher Engine with High-Precision Filtering.
     */
    fun findRankedInventoryMatches(
        ocrResult: OcrResult,
        inventoryProducts: List<Product>,
        threshold: Float = 0.60f
    ): List<Product> {
        if (inventoryProducts.isEmpty()) return emptyList()

        val matches = mutableListOf<Pair<Product, Float>>()

        val scannedNameLower = ocrResult.fullCombinedName.lowercase(Locale.getDefault())
        val scannedTokens = scannedNameLower.split(Regex("""[\s\-_,.:;]+""")).filter { it.length >= 2 }

        if (scannedTokens.isEmpty()) return emptyList()

        for (product in inventoryProducts) {
            val catalogNameLower = product.name.lowercase(Locale.getDefault())
            val catalogTokens = catalogNameLower.split(Regex("""[\s\-_,.:;]+""")).filter { it.length >= 2 }

            if (catalogTokens.isEmpty()) continue
            if (isConflictingPair(scannedNameLower, catalogNameLower)) continue

            // 1. Direct Keyword / Token Containment (e.g. "maggi" in "Maggi 2-Minute")
            val matchingTokens = scannedTokens.count { token ->
                catalogTokens.any { catToken -> catToken.contains(token) || token.contains(catToken) }
            }

            var tokenScore = if (matchingTokens > 0) {
                matchingTokens.toFloat() / maxOf(scannedTokens.size, catalogTokens.size).toFloat()
            } else 0f

            // 2. Substring Match Boost
            if (scannedNameLower.contains(catalogNameLower)) {
                tokenScore = maxOf(tokenScore, 0.90f)
            } else if (catalogNameLower.contains(scannedNameLower)) {
                if (scannedTokens.size >= 2 || scannedNameLower.length >= 7) {
                    tokenScore = maxOf(tokenScore, 0.90f)
                } else {
                    tokenScore = maxOf(tokenScore, 0.65f)
                }
            }

            // 3. Known Stylized Font Alias Mapping Boost
            val aliasTarget = fontAliasesMap[scannedNameLower]
            if (aliasTarget != null && catalogNameLower.contains(aliasTarget)) {
                tokenScore = maxOf(tokenScore, 0.95f)
            }

            // 4. Levenshtein Character Distance Similarity Boost
            val levSim = charSimilarity(scannedNameLower, catalogNameLower)
            if (levSim >= 0.65f) {
                tokenScore = maxOf(tokenScore, levSim)
            }

            if (scannedNameLower == catalogNameLower) {
                tokenScore = 1.0f
            }

            // Color boost
            val targetColor = productColorSignatures[product.name.lowercase(Locale.getDefault())]
            if (targetColor != null && ocrResult.detectedColor != PackagingColor.UNKNOWN) {
                if (targetColor == ocrResult.detectedColor && tokenScore >= threshold) {
                    tokenScore = minOf(1.0f, tokenScore + 0.15f)
                }
            }

            if (tokenScore >= threshold) {
                matches.add(Pair(product, tokenScore))
            }
        }

        val sortedMatches = matches.sortedByDescending { it.second }
        if (sortedMatches.size > 1) {
            val topScore = sortedMatches[0].second
            val runnerUpScore = sortedMatches[1].second
            if (topScore < 1.0f && (topScore - runnerUpScore) < 0.05f) {
                return emptyList()
            }
        }

        return sortedMatches.map { it.first }
    }

    /**
     * Strict Catalog Matcher Engine for 6,000 Master Catalog Reference Items.
     */
    fun findRankedCatalogMatches(
        ocrResult: OcrResult,
        catalogItems: List<com.smartvendor.ai.network.models.MasterCatalogResponse>,
        threshold: Float = 0.75f
    ): List<com.smartvendor.ai.network.models.MasterCatalogResponse> {
        if (catalogItems.isEmpty()) return emptyList()

        val matches = mutableListOf<Pair<com.smartvendor.ai.network.models.MasterCatalogResponse, Float>>()

        val scannedNameLower = ocrResult.fullCombinedName.lowercase(Locale.getDefault())
        val scannedTokens = scannedNameLower.split(Regex("""[\s\-_,.:;]+""")).filter { it.length >= 2 }

        if (scannedTokens.isEmpty()) return emptyList()

        for (item in catalogItems) {
            val catalogNameLower = item.name.lowercase(Locale.getDefault())
            val catalogTokens = catalogNameLower.split(Regex("""[\s\-_,.:;]+""")).filter { it.length >= 2 }

            if (catalogTokens.isEmpty()) continue
            if (isConflictingPair(scannedNameLower, catalogNameLower)) continue

            var tokenScore = 0.0f

            if (scannedNameLower == catalogNameLower) {
                tokenScore = 1.0f
            } else {
                val matchingTokens = scannedTokens.count { token ->
                    catalogTokens.any { catToken -> catToken.contains(token) || token.contains(catToken) }
                }

                tokenScore = matchingTokens.toFloat() / maxOf(scannedTokens.size, catalogTokens.size).toFloat()

                if (scannedNameLower.contains(catalogNameLower)) {
                    tokenScore = maxOf(tokenScore, 0.90f)
                } else if (catalogNameLower.contains(scannedNameLower)) {
                    if (scannedTokens.size >= 2 || scannedNameLower.length >= 7) {
                        tokenScore = maxOf(tokenScore, 0.90f)
                    } else {
                        tokenScore = maxOf(tokenScore, 0.75f) // Matches original threshold
                    }
                }

                val levSim = charSimilarity(scannedNameLower, catalogNameLower)
                if (levSim >= 0.70f) {
                    tokenScore = maxOf(tokenScore, levSim)
                }

                val aliasTarget = fontAliasesMap[scannedNameLower]
                if (aliasTarget != null && catalogNameLower.contains(aliasTarget)) {
                    tokenScore = maxOf(tokenScore, 0.95f)
                }

                val targetColor = productColorSignatures[item.name.lowercase(Locale.getDefault())]
                if (targetColor != null && ocrResult.detectedColor != PackagingColor.UNKNOWN) {
                    if (targetColor == ocrResult.detectedColor && tokenScore >= threshold) {
                        tokenScore = minOf(1.0f, tokenScore + 0.10f)
                    }
                }
            }

            if (tokenScore >= threshold) {
                matches.add(Pair(item, tokenScore))
            }
        }

        val sortedMatches = matches.sortedByDescending { it.second }
        if (sortedMatches.size > 1) {
            val topScore = sortedMatches[0].second
            val runnerUpScore = sortedMatches[1].second
            if (topScore < 1.0f && (topScore - runnerUpScore) < 0.05f) {
                return emptyList()
            }
        }

        return sortedMatches.map { it.first }
    }

    fun close() {
        try {
            recognizer.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing OCR recognizer", e)
        }
    }

    companion object {
        private const val TAG = "OcrScannerManager"
    }
}
