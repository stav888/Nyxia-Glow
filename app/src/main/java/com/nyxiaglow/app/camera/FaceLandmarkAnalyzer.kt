package com.nyxiaglow.app.camera

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private data class InFlightFrame(
    val bitmap: Bitmap,
    val rotationDegrees: Int
)

private const val FACE_LANDMARK_MODEL_NAME = "face_landmarker.task"

class FaceLandmarkAnalyzer(
    context: Context,
    private val onLandmarksDetected: (FaceLandmarkerResult, Int) -> Unit,
    private val onLightChanged: (Float) -> Unit,
    private val onError: (String) -> Unit = {}
) : ImageAnalysis.Analyzer {
    private var lastFaceFrameTime = 0L
    private var lastLightSampleTime = 0L
    private var lastLandmarkTimestamp = 0L
    private val lock = ReentrantLock()
    private val recycleHandler = Handler(Looper.getMainLooper())
    private var inFlightFrame: InFlightFrame? = null
    private val closed = AtomicBoolean(false)
    private var faceLandmarker: FaceLandmarker? = createFaceLandmarker(
        context,
        onLandmarksDetected,
        onError,
        ::takeInFlightFrame,
        ::recycleLater,
        closed
    )

    override fun analyze(image: ImageProxy) {
        try {
            if (closed.get()) return
            val now = SystemClock.uptimeMillis()
            if (now - lastLightSampleTime >= LIGHT_SAMPLE_INTERVAL_MS) {
                lastLightSampleTime = now
                sampleLuminance(image)?.let(onLightChanged)
            }
            if (now - lastFaceFrameTime < FACE_SAMPLE_INTERVAL_MS) return
            val rotationDegrees = image.imageInfo.rotationDegrees
            val decodedBitmap = image.toRgbaBitmap() ?: return
            val bitmap = decodedBitmap.toMediaPipeBitmap()
            if (bitmap !== decodedBitmap) decodedBitmap.recycle()
            if (bitmap == null || bitmap.isRecycled) return
            lock.withLock {
                if (closed.get() || faceLandmarker == null || inFlightFrame != null) {
                    recycleLater(bitmap)
                    return
                }
                lastFaceFrameTime = now
                inFlightFrame = InFlightFrame(bitmap, rotationDegrees)
                try {
                    val mpImage = BitmapImageBuilder(bitmap).build()
                    val processingOptions = ImageProcessingOptions.builder()
                        .setRotationDegrees(rotationDegrees)
                        .build()
                    val timestamp = maxOf(now, lastLandmarkTimestamp + 1).also { lastLandmarkTimestamp = it }
                    faceLandmarker?.detectAsync(mpImage, processingOptions, timestamp)
                        ?: run {
                            inFlightFrame = null
                            recycleLater(bitmap)
                        }
                } catch (exception: Exception) {
                    if (inFlightFrame?.bitmap === bitmap) {
                        inFlightFrame = null
                        recycleLater(bitmap)
                    }
                    if (!closed.get()) {
                        onError("${exception.javaClass.simpleName}: ${exception.message ?: "Face landmarking failed"}")
                    }
                }
            }
        } catch (exception: Exception) {
            if (!closed.get()) {
                onError("${exception.javaClass.simpleName}: ${exception.message ?: "Face landmarking failed"}")
            }
        } finally {
            image.close()
        }
    }

    private fun sampleLuminance(image: ImageProxy): Float? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer.duplicate()
        if (!buffer.hasRemaining()) return null
        val columns = 12
        val rows = 10
        var sum = 0L
        var samples = 0
        for (row in 0 until rows) {
            val y = (row * image.height / rows).coerceIn(0, image.height - 1)
            for (column in 0 until columns) {
                val x = (column * image.width / columns).coerceIn(0, image.width - 1)
                val offset = buffer.position() + y * plane.rowStride + x * plane.pixelStride
                if (offset >= buffer.position() && offset < buffer.limit()) {
                    sum += buffer.get(offset).toInt() and 0xFF
                    samples++
                }
            }
        }
        return if (samples == 0) null else (sum.toFloat() / samples / 255f).coerceIn(0f, 1f)
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) return
        val pending: InFlightFrame?
        val marker: FaceLandmarker?
        lock.withLock {
            marker = faceLandmarker
            faceLandmarker = null
            pending = takeInFlightFrame()
        }
        runCatching { marker?.close() }
        pending?.bitmap?.let(::recycleLater)
    }

    private fun takeInFlightFrame(): InFlightFrame? = lock.withLock {
        val frame = inFlightFrame
        inFlightFrame = null
        frame
    }

    private fun recycleLater(bitmap: Bitmap) {
        recycleHandler.postDelayed({
            if (!bitmap.isRecycled) bitmap.recycle()
        }, BITMAP_RELEASE_DELAY_MS)
    }

    private companion object {
        const val FACE_SAMPLE_INTERVAL_MS = 100L
        const val LIGHT_SAMPLE_INTERVAL_MS = 250L
        const val BITMAP_RELEASE_DELAY_MS = 500L
    }
}

private fun ImageProxy.toRgbaBitmap(): Bitmap? {
    if (width < 16 || height < 16) return null
    val yPlane = planes.getOrNull(0) ?: return null
    val uPlane = planes.getOrNull(1) ?: return null
    val vPlane = planes.getOrNull(2) ?: return null
    val yBuffer = yPlane.buffer.duplicate()
    val uBuffer = uPlane.buffer.duplicate()
    val vBuffer = vPlane.buffer.duplicate()
    val pixels = IntArray(width * height)
    for (row in 0 until height) {
        for (column in 0 until width) {
            val yIndex = yBuffer.position() + row * yPlane.rowStride + column * yPlane.pixelStride
            if (yIndex < yBuffer.position() || yIndex >= yBuffer.limit()) return null
            val chromaRow = row / 2
            val chromaColumn = column / 2
            val uIndex = uBuffer.position() + chromaRow * uPlane.rowStride + chromaColumn * uPlane.pixelStride
            val vIndex = vBuffer.position() + chromaRow * vPlane.rowStride + chromaColumn * vPlane.pixelStride
            if (uIndex < uBuffer.position() || uIndex >= uBuffer.limit() ||
                vIndex < vBuffer.position() || vIndex >= vBuffer.limit()
            ) return null
            val y = (yBuffer.get(yIndex).toInt() and 0xFF) - 16
            val u = (uBuffer.get(uIndex).toInt() and 0xFF) - 128
            val v = (vBuffer.get(vIndex).toInt() and 0xFF) - 128
            val red = (1.164f * y + 1.596f * v).toInt().coerceIn(0, 255)
            val green = (1.164f * y - 0.392f * u - 0.813f * v).toInt().coerceIn(0, 255)
            val blue = (1.164f * y + 2.017f * u).toInt().coerceIn(0, 255)
            pixels[row * width + column] = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
        }
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}

private fun Bitmap.toMediaPipeBitmap(): Bitmap? {
    if (isRecycled || width <= 0 || height <= 0) return null
    return try {
        copy(Bitmap.Config.ARGB_8888, false)
    } catch (_: RuntimeException) {
        null
    }
}

private fun createFaceLandmarker(
    context: Context,
    onLandmarksDetected: (FaceLandmarkerResult, Int) -> Unit,
    onError: (String) -> Unit,
    takeInFlightFrame: () -> InFlightFrame?,
    recycleBitmap: (Bitmap) -> Unit,
    closed: AtomicBoolean
): FaceLandmarker? {
    fun options(delegate: Delegate): FaceLandmarker.FaceLandmarkerOptions =
        FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath(FACE_LANDMARK_MODEL_NAME)
                    .setDelegate(delegate)
                    .build()
            )
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumFaces(1)
            .setMinFaceDetectionConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setResultListener { result, _ ->
                val frame = takeInFlightFrame() ?: return@setResultListener
                recycleBitmap(frame.bitmap)
                if (!closed.get()) onLandmarksDetected(result, frame.rotationDegrees)
            }
            .setErrorListener { error ->
                takeInFlightFrame()?.bitmap?.let(recycleBitmap)
                if (!closed.get()) {
                    onError(error.message ?: "Face landmarking unavailable")
                }
            }
            .build()

    return try {
        FaceLandmarker.createFromOptions(context, options(Delegate.CPU))
    } catch (cpuException: Exception) {
        onError(cpuException.message ?: "Face landmark model unavailable")
        null
    }
}
