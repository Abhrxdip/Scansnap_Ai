package com.smartvendor.ai.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.smartvendor.ai.model.DetectionResult
import com.smartvendor.ai.network.models.YoloDetection
import com.smartvendor.ai.network.models.YoloDetectResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min

class TFLiteClassifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var isInitialized = false
    private val modelInputSize = 640

    private var isChannelsFirst = true
    private var numChannels = 15
    private var numPredictions = 8400

    fun isReady(): Boolean = isInitialized && interpreter != null

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (isInitialized) return@withContext Result.success(Unit)

            labels = loadLabels()
            Log.d(TAG, "Loaded ${labels.size} labels: $labels")

            val modelBuffer = loadModelFile()
            if (modelBuffer != null) {
                val options = Interpreter.Options().apply {
                    setNumThreads(4)
                }
                interpreter = Interpreter(modelBuffer, options)
                inspectTensorShapes()
                warmUpModel()
                isInitialized = true
                Log.d(TAG, "TFLite Model loaded and warmed up successfully with $numChannels channels and $numPredictions predictions.")
                Result.success(Unit)
            } else {
                Log.w(TAG, "Model file best.tflite not found in assets, running fallback mode.")
                isInitialized = false
                Result.failure(IOException("best.tflite asset not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TFLite Classifier", e)
            Result.failure(e)
        }
    }

    private fun inspectTensorShapes() {
        val interp = interpreter ?: return
        try {
            val outputTensor = interp.getOutputTensor(0)
            val shape = outputTensor.shape() // e.g. [1, 15, 8400] or [1, 8400, 15]
            Log.d(TAG, "Output tensor shape: ${shape.contentToString()}")

            if (shape.size >= 3) {
                val expectedChannels = 4 + labels.size
                if (shape[1] == expectedChannels) {
                    isChannelsFirst = true
                    numChannels = shape[1]
                    numPredictions = shape[2]
                } else if (shape[2] == expectedChannels) {
                    isChannelsFirst = false
                    numPredictions = shape[1]
                    numChannels = shape[2]
                } else {
                    // Fallback to whatever dimension matches best
                    if (shape[1] < shape[2]) {
                        isChannelsFirst = true
                        numChannels = shape[1]
                        numPredictions = shape[2]
                    } else {
                        isChannelsFirst = false
                        numPredictions = shape[1]
                        numChannels = shape[2]
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not inspect output tensor shape, using defaults: $e")
            numChannels = 4 + labels.size
            numPredictions = 8400
            isChannelsFirst = true
        }
    }

    private fun loadModelFile(): MappedByteBuffer? {
        return try {
            val fileDescriptor = context.assets.openFd("best.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load best.tflite from assets: ${e.message}")
            null
        }
    }

    private fun loadLabels(): List<String> {
        return try {
            val loaded = context.assets.open("labels.txt").bufferedReader().useLines { lines ->
                lines.map { it.trim() }.filter { it.isNotBlank() }.toList()
            }
            if (loaded.isNotEmpty()) loaded else defaultLabels()
        } catch (e: Exception) {
            Log.w(TAG, "Could not load labels.txt, using default 11 classes: ${e.message}")
            defaultLabels()
        }
    }

    private fun defaultLabels(): List<String> = listOf(
        "amul_ice_cream", "cake", "cerave", "hns_shampoo", "nestle_milk_powder",
        "plum", "thums_up", "wild_stone", "nivea_deodorant", "bourbon_biscuit", "milky_biscuit"
    )

    private fun warmUpModel() {
        interpreter?.let { interp ->
            try {
                val dummyInput = java.nio.ByteBuffer.allocateDirect(4 * modelInputSize * modelInputSize * 3)
                dummyInput.order(java.nio.ByteOrder.nativeOrder())
                val dummyOutput = if (isChannelsFirst) {
                    Array(1) { Array(numChannels) { FloatArray(numPredictions) } }
                } else {
                    Array(1) { Array(numPredictions) { FloatArray(numChannels) } }
                }
                interp.run(dummyInput, dummyOutput)
                Log.d(TAG, "Warmup model pass succeeded.")
            } catch (e: Exception) {
                Log.w(TAG, "Warmup model pass note: ${e.message}")
            }
        }
    }

    /**
     * Run on-device TFLite inference returning standard [DetectionResult] list.
     */
    suspend fun detect(
        bitmap: Bitmap,
        confThreshold: Float = 0.35f
    ): List<DetectionResult> = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val interp = interpreter
        if (!isInitialized || interp == null) {
            return@withContext emptyList()
        }

        val (inputBuffer, letterbox) = YoloUtils.preprocessBitmapLetterbox(bitmap, modelInputSize)

        val rawDetections = mutableListOf<DetectionResult>()
        val classCount = labels.size

        try {
            if (isChannelsFirst) {
                val outputArray = Array(1) { Array(numChannels) { FloatArray(numPredictions) } }
                interp.run(inputBuffer, outputArray)
                val preds = outputArray[0] // [channels][predictions]

                for (i in 0 until numPredictions) {
                    var maxConfidence = 0.0f
                    var maxClassId = -1

                    for (c in 0 until classCount) {
                        val score = preds[4 + c][i]
                        if (score > maxConfidence) {
                            maxConfidence = score
                            maxClassId = c
                        }
                    }

                    if (maxConfidence >= confThreshold && maxClassId >= 0) {
                        val cx = preds[0][i]
                        val cy = preds[1][i]
                        val w = preds[2][i]
                        val h = preds[3][i]

                        // Invert letterbox
                        val origCx = (cx - letterbox.padX) / letterbox.scale
                        val origCy = (cy - letterbox.padY) / letterbox.scale
                        val origW = w / letterbox.scale
                        val origH = h / letterbox.scale

                        val left = max(0f, origCx - origW / 2f)
                        val top = max(0f, origCy - origH / 2f)
                        val right = min(letterbox.origWidth.toFloat(), origCx + origW / 2f)
                        val bottom = min(letterbox.origHeight.toFloat(), origCy + origH / 2f)

                        val rect = RectF(left, top, right, bottom)
                        val labelName = labels.getOrElse(maxClassId) { "Product_$maxClassId" }
                        val elapsedTime = System.currentTimeMillis() - startTime

                        rawDetections.add(
                            DetectionResult(
                                classId = maxClassId,
                                label = labelName,
                                confidence = maxConfidence,
                                boundingBox = rect,
                                inferenceTimeMs = elapsedTime
                            )
                        )
                    }
                }
            } else {
                val outputArray = Array(1) { Array(numPredictions) { FloatArray(numChannels) } }
                interp.run(inputBuffer, outputArray)
                val preds = outputArray[0] // [predictions][channels]

                for (i in 0 until numPredictions) {
                    var maxConfidence = 0.0f
                    var maxClassId = -1

                    for (c in 0 until classCount) {
                        val score = preds[i][4 + c]
                        if (score > maxConfidence) {
                            maxConfidence = score
                            maxClassId = c
                        }
                    }

                    if (maxConfidence >= confThreshold && maxClassId >= 0) {
                        val cx = preds[i][0]
                        val cy = preds[i][1]
                        val w = preds[i][2]
                        val h = preds[i][3]

                        // Invert letterbox
                        val origCx = (cx - letterbox.padX) / letterbox.scale
                        val origCy = (cy - letterbox.padY) / letterbox.scale
                        val origW = w / letterbox.scale
                        val origH = h / letterbox.scale

                        val left = max(0f, origCx - origW / 2f)
                        val top = max(0f, origCy - origH / 2f)
                        val right = min(letterbox.origWidth.toFloat(), origCx + origW / 2f)
                        val bottom = min(letterbox.origHeight.toFloat(), origCy + origH / 2f)

                        val rect = RectF(left, top, right, bottom)
                        val labelName = labels.getOrElse(maxClassId) { "Product_$maxClassId" }
                        val elapsedTime = System.currentTimeMillis() - startTime

                        rawDetections.add(
                            DetectionResult(
                                classId = maxClassId,
                                label = labelName,
                                confidence = maxConfidence,
                                boundingBox = rect,
                                inferenceTimeMs = elapsedTime
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Inference execution failed", e)
            return@withContext emptyList()
        }

        return@withContext YoloUtils.nonMaximumSuppression(rawDetections)
    }

    /**
     * Bridge method providing drop-in compatibility with backend [YoloDetectResponse].
     */
    suspend fun detectYolo(
        bitmap: Bitmap,
        confThreshold: Float = 0.35f
    ): YoloDetectResponse = withContext(Dispatchers.Default) {
        val origW = bitmap.width.toFloat()
        val origH = bitmap.height.toFloat()
        val results = detect(bitmap, confThreshold)

        val detections = results.map { res ->
            val normBbox = listOf(
                max(0f, min(1f, res.boundingBox.left / origW)),
                max(0f, min(1f, res.boundingBox.top / origH)),
                max(0f, min(1f, res.boundingBox.right / origW)),
                max(0f, min(1f, res.boundingBox.bottom / origH))
            )
            YoloDetection(
                label = res.label,
                confidence = res.confidence,
                bbox = normBbox
            )
        }

        val topItem = detections.maxByOrNull { it.confidence }
        YoloDetectResponse(
            detections = detections,
            topLabel = topItem?.label,
            topConfidence = topItem?.confidence
        )
    }

    fun close() {
        interpreter?.close()
        interpreter = null
        isInitialized = false
    }

    companion object {
        private const val TAG = "TFLiteClassifier"
    }
}
