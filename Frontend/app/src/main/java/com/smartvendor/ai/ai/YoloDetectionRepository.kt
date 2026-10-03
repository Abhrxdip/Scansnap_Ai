package com.smartvendor.ai.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Base64
import android.util.Log
import androidx.camera.core.ImageProxy
import com.smartvendor.ai.network.ApiClient
import com.smartvendor.ai.network.models.YoloDetectRequest
import com.smartvendor.ai.network.models.YoloDetectResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Fast Multi-Object YOLO Detection Repository
 * Supports both on-device TFLite inference and backend FastAPI inference with seamless failover.
 */
class YoloDetectionRepository(context: Context? = null) {

    private val api = ApiClient.apiService
    private val TAG = "YoloDetection"

    private var tfliteClassifier: TFLiteClassifier? = null

    init {
        context?.let { ctx ->
            tfliteClassifier = TFLiteClassifier(ctx)
        }
    }

    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        if (tfliteClassifier == null) {
            tfliteClassifier = TFLiteClassifier(context)
        }
        tfliteClassifier?.initialize()
    }

    /**
     * Convert CameraX ImageProxy to lightweight base64 JPEG and detect all products.
     * Always closes [imageProxy] in finally block so CameraX streaming never stalls.
     */
    suspend fun detectFromImageProxy(
        imageProxy: ImageProxy,
        confThreshold: Float = 0.30f
    ): YoloDetectResponse? = withContext(Dispatchers.IO) {
        val bitmap = imageProxyToRotatedBitmap(imageProxy)
        try {
            if (bitmap == null) return@withContext null
            detectFromBitmap(bitmap, confThreshold)
        } finally {
            try {
                imageProxy.close()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Convert a [Bitmap] to base64 JPEG and call backend; falls back seamlessly to on-device TFLite.
     */
    suspend fun detectFromBitmap(
        bitmap: Bitmap,
        confThreshold: Float = 0.25f
    ): YoloDetectResponse? = withContext(Dispatchers.IO) {
        // 1. Ultra-fast On-Device TFLite inference (~15ms, offline-first)
        if (tfliteClassifier?.isReady() == true) {
            try {
                val tfliteResponse = tfliteClassifier?.detectYolo(bitmap, confThreshold)
                if (tfliteResponse != null && tfliteResponse.detections.isNotEmpty()) {
                    Log.d(TAG, "⚡ On-Device TFLite found ${tfliteResponse.detections.size} products: ${tfliteResponse.detections.map { it.label }}")
                    return@withContext tfliteResponse
                }
            } catch (e: Exception) {
                Log.e(TAG, "On-device TFLite inference error: ${e.message}")
            }
        }

        // 2. Remote Backend YOLO endpoint (with fallback for any server error)
        try {
            val scaled = scaleBitmap(bitmap, maxDim = 480)
            val base64Jpeg = bitmapToBase64Jpeg(scaled)
            val response = api.detectFromBase64(
                YoloDetectRequest(image = base64Jpeg, conf = confThreshold)
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.detections.isNotEmpty()) {
                    Log.d(TAG, "Backend YOLO found ${body.detections.size} products: ${body.detections.map { it.label }}")
                    return@withContext body
                }
            } else {
                Log.w(TAG, "Detection API error: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Backend YOLO unreachable or slow (${e.message})")
        }

        null
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun imageProxyToRotatedBitmap(image: ImageProxy): Bitmap? {
        return try {
            val rawBitmap = image.toBitmap()
            val rotationDegrees = image.imageInfo.rotationDegrees
            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            } else {
                rawBitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed converting ImageProxy to Bitmap", e)
            null
        }
    }

    private fun bitmapToBase64Jpeg(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val scale = maxDim.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(
            bitmap,
            (w * scale).toInt(),
            (h * scale).toInt(),
            true
        )
    }
}
