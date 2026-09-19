package com.nyxiaglow.app.camera

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

/**
 * Original 64³ color LUTs packed as 8×8 tiles of 64×64 (512×512).
 *
 * Generated at runtime — no third-party LUT images. Sampling matches the
 * live GLSL lookup (blue selects the tile, red/green index inside the tile).
 */
object LookLut {
    const val GRID = 8
    const val CELL = 64
    const val SIZE = GRID * CELL
    const val LEVELS = CELL - 1

    data class Grade(
        val contrast: Float = 1f,
        val brightness: Float = 0f,
        val warm: Float = 0f,
        val saturation: Float = 1f,
        val shadowLift: Float = 0f,
        val highlightRoll: Float = 0f,
        val redPush: Float = 1f,
        val greenPush: Float = 1f,
        val bluePush: Float = 1f
    )

    fun gradeFor(lookId: String): Grade = when (lookId) {
        "nude" -> Grade(
            contrast = 0.96f,
            brightness = 0.02f,
            warm = 1.1f,
            saturation = 0.96f,
            shadowLift = 0.04f,
            redPush = 1.03f,
            bluePush = 0.97f
        )
        "glam" -> Grade(
            contrast = 1.16f,
            brightness = -0.01f,
            warm = -0.2f,
            saturation = 1.08f,
            highlightRoll = 0.15f
        )
        "ruby" -> Grade(
            contrast = 1.08f,
            warm = 0.4f,
            saturation = 1.06f,
            redPush = 1.12f,
            greenPush = 0.96f,
            bluePush = 0.98f
        )
        "matte" -> Grade(
            contrast = 0.88f,
            brightness = -0.02f,
            saturation = 0.92f,
            shadowLift = 0.02f,
            highlightRoll = 0.45f
        )
        "dewy" -> Grade(
            contrast = 1.02f,
            brightness = 0.03f,
            warm = 0.8f,
            saturation = 1.04f,
            shadowLift = 0.08f,
            highlightRoll = 0.05f
        )
        else -> Grade(
            contrast = 1.04f,
            brightness = 0.01f,
            warm = 0.6f,
            saturation = 1.02f
        )
    }

    fun applyGrade(r: Float, g: Float, b: Float, grade: Grade): FloatArray {
        var red = r.coerceIn(0f, 1f)
        var green = g.coerceIn(0f, 1f)
        var blue = b.coerceIn(0f, 1f)
        red += grade.shadowLift * (1f - red)
        green += grade.shadowLift * (1f - green)
        blue += grade.shadowLift * (1f - blue)
        red = (red - 0.5f) * grade.contrast + 0.5f + grade.brightness
        green = (green - 0.5f) * grade.contrast + 0.5f + grade.brightness
        blue = (blue - 0.5f) * grade.contrast + 0.5f + grade.brightness
        red += grade.warm * 0.04f
        blue -= grade.warm * 0.03f
        val luma = 0.299f * red + 0.587f * green + 0.114f * blue
        red = luma + (red - luma) * grade.saturation
        green = luma + (green - luma) * grade.saturation
        blue = luma + (blue - luma) * grade.saturation
        red *= grade.redPush
        green *= grade.greenPush
        blue *= grade.bluePush
        red = rollHighlight(red, grade.highlightRoll)
        green = rollHighlight(green, grade.highlightRoll)
        blue = rollHighlight(blue, grade.highlightRoll)
        return floatArrayOf(red.coerceIn(0f, 1f), green.coerceIn(0f, 1f), blue.coerceIn(0f, 1f))
    }

    fun buildPixels(grade: Grade): IntArray {
        val pixels = IntArray(SIZE * SIZE)
        val levels = LEVELS.toFloat()
        for (blueIndex in 0 until CELL) {
            val tileX = blueIndex % GRID
            val tileY = blueIndex / GRID
            val blue = blueIndex / levels
            for (greenIndex in 0 until CELL) {
                val green = greenIndex / levels
                for (redIndex in 0 until CELL) {
                    val red = redIndex / levels
                    val mapped = applyGrade(red, green, blue, grade)
                    val x = tileX * CELL + redIndex
                    val y = tileY * CELL + greenIndex
                    pixels[y * SIZE + x] = packRgb(mapped[0], mapped[1], mapped[2])
                }
            }
        }
        return pixels
    }

    fun sample(pixels: IntArray, r: Float, g: Float, b: Float): FloatArray {
        val blue = b.coerceIn(0f, 1f) * LEVELS
        val blue0 = floor(blue)
        val blue1 = min(LEVELS.toFloat(), ceil(blue))
        val mix = blue - blue0
        val first = sampleTile(pixels, r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), blue0.toInt())
        val second = sampleTile(pixels, r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), blue1.toInt())
        return floatArrayOf(
            first[0] + (second[0] - first[0]) * mix,
            first[1] + (second[1] - first[1]) * mix,
            first[2] + (second[2] - first[2]) * mix
        )
    }

    fun applyToArgb(argb: Int, pixels: IntArray, intensity: Float): Int {
        val amount = intensity.coerceIn(0f, 1f)
        if (amount <= 0f) return argb
        val red = ((argb shr 16) and 0xFF) / 255f
        val green = ((argb shr 8) and 0xFF) / 255f
        val blue = (argb and 0xFF) / 255f
        val mapped = sample(pixels, red, green, blue)
        val outRed = ((red + (mapped[0] - red) * amount) * 255f + 0.5f).toInt().coerceIn(0, 255)
        val outGreen = ((green + (mapped[1] - green) * amount) * 255f + 0.5f).toInt().coerceIn(0, 255)
        val outBlue = ((blue + (mapped[2] - blue) * amount) * 255f + 0.5f).toInt().coerceIn(0, 255)
        return (argb and -0x1000000) or (outRed shl 16) or (outGreen shl 8) or outBlue
    }

    fun toRgbaBytes(pixels: IntArray): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(pixels.size * 4).order(ByteOrder.nativeOrder())
        for (pixel in pixels) {
            buffer.put(((pixel shr 16) and 0xFF).toByte())
            buffer.put(((pixel shr 8) and 0xFF).toByte())
            buffer.put((pixel and 0xFF).toByte())
            buffer.put(((pixel ushr 24) and 0xFF).toByte())
        }
        buffer.position(0)
        return buffer
    }

    fun packRgb(r: Float, g: Float, b: Float): Int {
        val red = (r * 255f + 0.5f).toInt().coerceIn(0, 255)
        val green = (g * 255f + 0.5f).toInt().coerceIn(0, 255)
        val blue = (b * 255f + 0.5f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
    }

    private fun rollHighlight(value: Float, amount: Float): Float {
        if (amount <= 0f || value <= 0.7f) return value
        return 0.7f + (value - 0.7f) * (1f - amount.coerceIn(0f, 1f))
    }

    private fun sampleTile(pixels: IntArray, r: Float, g: Float, blueIndex: Int): FloatArray {
        val tile = blueIndex.coerceIn(0, LEVELS)
        val tileX = tile % GRID
        val tileY = tile / GRID
        val redF = r * LEVELS
        val greenF = g * LEVELS
        val x0 = floor(redF).toInt()
        val y0 = floor(greenF).toInt()
        val x1 = min(LEVELS, ceil(redF).toInt())
        val y1 = min(LEVELS, ceil(greenF).toInt())
        val fx = redF - x0
        val fy = greenF - y0
        val c00 = unpack(pixelAt(pixels, tileX * CELL + x0, tileY * CELL + y0))
        val c10 = unpack(pixelAt(pixels, tileX * CELL + x1, tileY * CELL + y0))
        val c01 = unpack(pixelAt(pixels, tileX * CELL + x0, tileY * CELL + y1))
        val c11 = unpack(pixelAt(pixels, tileX * CELL + x1, tileY * CELL + y1))
        return floatArrayOf(
            bilinear(c00[0], c10[0], c01[0], c11[0], fx, fy),
            bilinear(c00[1], c10[1], c01[1], c11[1], fx, fy),
            bilinear(c00[2], c10[2], c01[2], c11[2], fx, fy)
        )
    }

    private fun bilinear(c00: Float, c10: Float, c01: Float, c11: Float, fx: Float, fy: Float): Float {
        val x0 = c00 + (c10 - c00) * fx
        val x1 = c01 + (c11 - c01) * fx
        return x0 + (x1 - x0) * fy
    }

    private fun pixelAt(pixels: IntArray, x: Int, y: Int): Int =
        pixels[y.coerceIn(0, SIZE - 1) * SIZE + x.coerceIn(0, SIZE - 1)]

    private fun unpack(argb: Int): FloatArray = floatArrayOf(
        ((argb shr 16) and 0xFF) / 255f,
        ((argb shr 8) and 0xFF) / 255f,
        (argb and 0xFF) / 255f
    )
}
