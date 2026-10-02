package com.smartvendor.ai.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import com.smartvendor.ai.model.DetectionResult
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object YoloUtils {

    data class LetterboxInfo(
        val scale: Float,
        val padX: Float,
        val padY: Float,
        val origWidth: Int,
        val origHeight: Int
    )

    fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val bitmap = if (image.format == ImageFormat.YUV_420_888 || image.format == ImageFormat.NV21) {
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 100, out)
            val imageBytes = out.toByteArray()
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } else {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } ?: return null

        val matrix = Matrix()
        matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Letterbox preprocessing maintaining aspect ratio with neutral 114 gray background padding.
     */
    fun preprocessBitmapLetterbox(bitmap: Bitmap, inputSize: Int = 640): Pair<ByteBuffer, LetterboxInfo> {
        val origW = bitmap.width
        val origH = bitmap.height
        val scale = min(inputSize.toFloat() / origW.toFloat(), inputSize.toFloat() / origH.toFloat())
        val newW = (origW * scale).roundToInt()
        val newH = (origH * scale).roundToInt()
        val padX = (inputSize - newW) / 2f
        val padY = (inputSize - newH) / 2f

        val canvasBitmap = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBitmap)
        canvas.drawColor(Color.rgb(114, 114, 114))

        val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        canvas.drawBitmap(scaled, padX, padY, null)

        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputSize * inputSize)
        canvasBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

        var pixel = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val valPixel = intValues[pixel++]
                byteBuffer.putFloat(((valPixel shr 16 and 0xFF) / 255.0f))
                byteBuffer.putFloat(((valPixel shr 8 and 0xFF) / 255.0f))
                byteBuffer.putFloat(((valPixel and 0xFF) / 255.0f))
            }
        }

        return Pair(byteBuffer, LetterboxInfo(scale, padX, padY, origW, origH))
    }

    fun preprocessBitmap(bitmap: Bitmap, inputSize: Int): Pair<ByteBuffer, FloatArray> {
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputSize * inputSize)
        resized.getPixels(intValues, 0, resized.width, 0, 0, resized.width, resized.height)

        var pixel = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val valPixel = intValues[pixel++]
                byteBuffer.putFloat(((valPixel shr 16 and 0xFF) / 255.0f))
                byteBuffer.putFloat(((valPixel shr 8 and 0xFF) / 255.0f))
                byteBuffer.putFloat(((valPixel and 0xFF) / 255.0f))
            }
        }

        val scaleFactors = floatArrayOf(
            bitmap.width.toFloat() / inputSize.toFloat(),
            bitmap.height.toFloat() / inputSize.toFloat()
        )
        return Pair(byteBuffer, scaleFactors)
    }

    fun nonMaximumSuppression(
        detections: List<DetectionResult>,
        iouThreshold: Float = 0.45f
    ): List<DetectionResult> {
        val sorted = detections.sortedByDescending { it.confidence }
        val selected = mutableListOf<DetectionResult>()
        val active = BooleanArray(sorted.size) { true }

        for (i in sorted.indices) {
            if (!active[i]) continue
            val a = sorted[i]
            selected.add(a)

            for (j in i + 1 until sorted.size) {
                if (!active[j]) continue
                val b = sorted[j]
                if (calculateIoU(a.boundingBox, b.boundingBox) > iouThreshold) {
                    active[j] = false
                }
            }
        }
        return selected
    }

    private fun calculateIoU(a: RectF, b: RectF): Float {
        val intersectionLeft = max(a.left, b.left)
        val intersectionTop = max(a.top, b.top)
        val intersectionRight = min(a.right, b.right)
        val intersectionBottom = min(a.bottom, b.bottom)

        val intersectionArea = max(0f, intersectionRight - intersectionLeft) *
                max(0f, intersectionBottom - intersectionTop)

        val areaA = (a.right - a.left) * (a.bottom - a.top)
        val areaB = (b.right - b.left) * (b.bottom - b.top)
        val unionArea = areaA + areaB - intersectionArea

        return if (unionArea > 0f) intersectionArea / unionArea else 0f
    }
}
