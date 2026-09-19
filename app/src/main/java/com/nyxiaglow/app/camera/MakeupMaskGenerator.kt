package com.nyxiaglow.app.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.SystemClock
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun smoothingAlpha(cutoff: Float, sampleIntervalSeconds: Float): Float {
    val tau = 1f / (2f * PI.toFloat() * cutoff)
    return 1f / (1f + tau / sampleIntervalSeconds)
}

class OneEuroFilter(
    private val minCutoff: Float = 1.0f,
    private val beta: Float = 0.05f,
    private val dcutoff: Float = 1.0f
) {
    private var lastTimeSeconds = Float.NaN
    private var filteredValue = 0f
    private var filteredDerivative = 0f

    fun reset() {
        lastTimeSeconds = Float.NaN
    }

    fun filter(value: Float, timeSeconds: Float): Float {
        if (lastTimeSeconds.isNaN()) {
            lastTimeSeconds = timeSeconds
            filteredValue = value
            return value
        }
        val dt = (timeSeconds - lastTimeSeconds).coerceAtLeast(1e-3f)
        lastTimeSeconds = timeSeconds
        val derivative = (value - filteredValue) / dt
        filteredDerivative += smoothingAlpha(dcutoff, dt) * (derivative - filteredDerivative)
        val cutoff = minCutoff + beta * abs(filteredDerivative)
        filteredValue += smoothingAlpha(cutoff, dt) * (value - filteredValue)
        return filteredValue
    }
}

class LandmarkStabilizer(
    private val minCutoff: Float = 1.0f,
    private val beta: Float = 0.05f
) {
    private val filters = mutableMapOf<Int, Pair<OneEuroFilter, OneEuroFilter>>()

    fun reset() = filters.clear()

    fun filter(index: Int, x: Float, y: Float, timeSeconds: Float): Pair<Float, Float> {
        val (filterX, filterY) = filters.getOrPut(index) {
            OneEuroFilter(minCutoff, beta) to OneEuroFilter(minCutoff, beta)
        }
        return filterX.filter(x, timeSeconds) to filterY.filter(y, timeSeconds)
    }
}

fun smoothClosedPolygon(points: List<Pair<Float, Float>>, iterations: Int = 2): List<Pair<Float, Float>> {
    if (points.size < 3 || iterations <= 0) return points
    var current = points
    repeat(iterations) {
        val next = ArrayList<Pair<Float, Float>>(current.size * 2)
        for (i in current.indices) {
            val p = current[i]
            val q = current[(i + 1) % current.size]
            next += (0.75f * p.first + 0.25f * q.first) to (0.75f * p.second + 0.25f * q.second)
            next += (0.25f * p.first + 0.75f * q.first) to (0.25f * p.second + 0.75f * q.second)
        }
        current = next
    }
    return current
}

class MakeupMaskGenerator {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private var maskBitmap = Bitmap.createBitmap(DEFAULT_MASK_WIDTH, DEFAULT_MASK_HEIGHT, Bitmap.Config.ARGB_8888)
    private var canvas = Canvas(maskBitmap)
    private val stabilizer = LandmarkStabilizer()
    private var lastStreamConfig: Triple<Int, Boolean, Int>? = null

    @Synchronized
    fun generateMask(
        result: FaceLandmarkerResult,
        rotationDegrees: Int = 0,
        mirrorX: Boolean = false,
        width: Int = DEFAULT_MASK_WIDTH,
        height: Int = DEFAULT_MASK_HEIGHT
    ): Bitmap {
        ensureSize(width, height)
        // MediaPipe image coordinates are rotated and optionally mirrored here once.
        // The renderer samples the camera texture without an additional mirror.
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val faces = result.faceLandmarks()
        if (faces.isEmpty()) {
            stabilizer.reset()
            lastStreamConfig = null
            return Bitmap.createBitmap(maskBitmap)
        }

        val landmarks = faces[0]
        val streamConfig = Triple(rotationDegrees, mirrorX, landmarks.size)
        if (streamConfig != lastStreamConfig) {
            stabilizer.reset()
            lastStreamConfig = streamConfig
        }
        val nowSeconds = SystemClock.uptimeMillis() / 1000f
        fun rawPoint(index: Int): Pair<Float, Float>? {
            if (index !in landmarks.indices) return null
            val landmark = landmarks[index]
            return stabilizer.filter(index, landmark.x(), landmark.y(), nowSeconds)
        }
        fun point(index: Int): Pair<Float, Float>? {
            val raw = rawPoint(index) ?: return null
            val normalized = CoordinateConverter.fromRotatedImage(raw.first, raw.second, rotationDegrees, mirrorX)
            return (normalized.first * maskBitmap.width) to (normalized.second * maskBitmap.height)
        }

        paint.shader = null
        paint.xfermode = null
        paint.color = Color.argb(205, 255, 0, 0)
        drawSmoothPolygon(
            smoothClosedPolygon(
                CoordinateConverter
                    .validIndices(LIP_INDICES, landmarks.size)
                    .asList()
                    .mapNotNull(::rawPoint)
                    .map { raw ->
                        CoordinateConverter.fromRotatedImage(raw.first, raw.second, rotationDegrees, mirrorX)
                    }
                    .map { (maskBitmap.width * it.first) to (maskBitmap.height * it.second) }
            )
        )

        val innerLip = smoothClosedPolygon(
            CoordinateConverter
                .validIndices(INNER_LIP_INDICES, landmarks.size)
                .asList()
                .mapNotNull(::rawPoint)
                .map { raw ->
                    CoordinateConverter.fromRotatedImage(raw.first, raw.second, rotationDegrees, mirrorX)
                }
                .map { (maskBitmap.width * it.first) to (maskBitmap.height * it.second) }
        )
        if (innerLip.size >= 3) {
            paint.shader = null
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            drawSmoothPolygon(innerLip)
            paint.xfermode = null
        }

        paint.color = Color.argb(145, 0, 255, 0)
        val faceWidth = faceWidthNorm(landmarks)
        val blushRadius = maskBitmap.width * 0.045f * (faceWidth / 0.5f).coerceIn(0.5f, 1.5f)
        drawBlushCluster(LEFT_CHEEK_INDICES, landmarks.size, ::point, blushRadius)
        drawBlushCluster(RIGHT_CHEEK_INDICES, landmarks.size, ::point, blushRadius)

        return Bitmap.createBitmap(maskBitmap)
    }

    companion object {
        fun hasVisiblePixels(bitmap: Bitmap): Boolean {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            return pixels.any { Color.alpha(it) != 0 }
        }

        const val DEFAULT_MASK_WIDTH = 640
        const val DEFAULT_MASK_HEIGHT = 480
        private val LIP_INDICES = intArrayOf(
            61, 185, 40, 39, 37, 0, 267, 269, 270, 409,
            291, 375, 321, 405, 314, 17, 84, 181, 91, 146
        )
        private val INNER_LIP_INDICES = intArrayOf(
            78, 191, 80, 81, 82, 13, 312, 311, 310, 415,
            308, 324, 318, 402, 317, 14, 87, 178, 88, 95
        )
        private val LEFT_CHEEK_INDICES = intArrayOf(116, 117, 118, 123, 147)
        private val RIGHT_CHEEK_INDICES = intArrayOf(345, 346, 347, 352, 376)
    }

    private fun ensureSize(width: Int, height: Int) {
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)
        if (maskBitmap.width == safeWidth && maskBitmap.height == safeHeight) return
        maskBitmap.recycle()
        maskBitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
        canvas = Canvas(maskBitmap)
    }

    private fun drawSmoothPolygon(points: List<Pair<Float, Float>>) {
        if (points.size < 3) return
        val path = Path().apply {
            moveTo(points.first().first, points.first().second)
            points.drop(1).forEach { lineTo(it.first, it.second) }
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawBlushCluster(
        indices: IntArray,
        landmarkCount: Int,
        point: (Int) -> Pair<Float, Float>?,
        radius: Float
    ) {
        CoordinateConverter
            .validIndices(indices, landmarkCount)
            .asList()
            .mapNotNull(point)
            .forEach { drawFeatheredCircle(it.first, it.second, radius) }
    }

    private fun faceWidthNorm(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Float {
        if (landmarks.isEmpty()) return 0.5f
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        landmarks.forEach {
            minX = min(minX, it.x())
            maxX = max(maxX, it.x())
        }
        return (maxX - minX).coerceIn(0.05f, 1f)
    }

    private fun drawFeatheredCircle(x: Float, y: Float, radius: Float = 28f) {
        paint.shader = android.graphics.RadialGradient(
            x,
            y,
            radius,
            Color.argb(145, 0, 255, 0),
            Color.TRANSPARENT,
            android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x, y, radius, paint)
        paint.shader = null
    }

    object CoordinateConverter {
        fun validIndices(indices: IntArray, landmarkCount: Int): IntArray =
            indices.filter { it in 0 until landmarkCount }.toIntArray()

        fun fromRotatedImage(x: Float, y: Float, rotationDegrees: Int, mirrorX: Boolean): Pair<Float, Float> {
            val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
            val rotated = when (normalizedRotation) {
                90 -> y to (1f - x)
                180 -> (1f - x) to (1f - y)
                270 -> (1f - y) to x
                else -> x to y
            }
            val convertedX = if (mirrorX) 1f - rotated.first else rotated.first
            return min(1f, max(0f, convertedX)) to min(1f, max(0f, rotated.second))
        }
    }

}
