package com.smartvendor.ai.barcode

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeScannerManager {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_ALL_FORMATS
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    @OptIn(ExperimentalGetImage::class)
    fun scanImage(
        imageProxy: ImageProxy,
        onSuccess: (String) -> Unit,
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
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                imageProxy.close()
                if (barcodes.isNotEmpty()) {
                    val rawValue = barcodes.first().rawValue
                    if (!rawValue.isNullOrBlank()) {
                        onSuccess(rawValue)
                    } else {
                        onNotFound()
                    }
                } else {
                    onNotFound()
                }
            }
            .addOnFailureListener { e ->
                imageProxy.close()
                Log.e(TAG, "Barcode scan error", e)
                onError(e)
            }
    }

    fun scanBitmap(
        bitmap: android.graphics.Bitmap,
        onSuccess: (String) -> Unit,
        onNotFound: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val image = InputImage.fromBitmap(bitmap, 0)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    val rawValue = barcodes.first().rawValue
                    if (!rawValue.isNullOrBlank()) {
                        onSuccess(rawValue)
                    } else {
                        onNotFound()
                    }
                } else {
                    onNotFound()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Barcode scan bitmap error", e)
                onError(e)
            }
    }



    fun close() {
        try {
            scanner.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing barcode scanner", e)
        }
    }

    companion object {
        private const val TAG = "BarcodeScannerManager"
    }
}
