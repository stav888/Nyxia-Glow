package com.nyxiaglow.app.camera

import com.nyxiaglow.app.ui.GlowLooks
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LookLutTest {
    @Test
    fun identityGradeLeavesChannelsUnchanged() {
        val identity = LookLut.Grade()
        val mapped = LookLut.applyGrade(0.4f, 0.5f, 0.6f, identity)
        assertArrayEquals(floatArrayOf(0.4f, 0.5f, 0.6f), mapped, 0.0001f)
    }

    @Test
    fun identityLutSamplesPackedCorners() {
        val pixels = LookLut.buildPixels(LookLut.Grade())
        assertEquals(LookLut.SIZE * LookLut.SIZE, pixels.size)
        assertRgbClose(floatArrayOf(0f, 0f, 0f), LookLut.sample(pixels, 0f, 0f, 0f), 0.02f)
        assertRgbClose(floatArrayOf(1f, 0f, 0f), LookLut.sample(pixels, 1f, 0f, 0f), 0.02f)
        assertRgbClose(floatArrayOf(0f, 1f, 0f), LookLut.sample(pixels, 0f, 1f, 0f), 0.02f)
        assertRgbClose(floatArrayOf(0f, 0f, 1f), LookLut.sample(pixels, 0f, 0f, 1f), 0.02f)
        assertRgbClose(floatArrayOf(1f, 1f, 1f), LookLut.sample(pixels, 1f, 1f, 1f), 0.02f)
        assertRgbClose(floatArrayOf(0.5f, 0.5f, 0.5f), LookLut.sample(pixels, 0.5f, 0.5f, 0.5f), 0.03f)
    }

    @Test
    fun applyToArgbWithZeroIntensityIsNoOp() {
        val pixels = LookLut.buildPixels(LookLut.gradeFor("glam"))
        val source = 0xFF88A0C0.toInt()
        assertEquals(source, LookLut.applyToArgb(source, pixels, 0f))
    }

    @Test
    fun glamContrastDarkensShadowsAndLiftsHighlights() {
        val low = LookLut.applyGrade(0.3f, 0.3f, 0.3f, LookLut.gradeFor("glam"))
        val high = LookLut.applyGrade(0.75f, 0.75f, 0.75f, LookLut.gradeFor("glam"))
        assertTrue(low[0] < 0.3f)
        assertTrue(high[0] > 0.73f)
    }

    @Test
    fun rubyPushesRedRelativeToGreen() {
        val mapped = LookLut.applyGrade(0.6f, 0.6f, 0.6f, LookLut.gradeFor("ruby"))
        assertTrue(mapped[0] > mapped[1])
    }

    @Test
    fun matteRollsHighlights() {
        val mapped = LookLut.applyGrade(0.95f, 0.95f, 0.95f, LookLut.gradeFor("matte"))
        assertTrue(mapped[0] < 0.95f)
    }

    @Test
    fun everyLookHasADistinctGradeAndLutStrength() {
        val skin = floatArrayOf(0.76f, 0.55f, 0.46f)
        val mapped = GlowLooks.all.associate { look ->
            look.id to LookLut.applyGrade(skin[0], skin[1], skin[2], LookLut.gradeFor(look.id))
        }
        assertTrue(lookDistance(mapped.getValue("natural"), mapped.getValue("glam")) > 0.01f)
        assertTrue(lookDistance(mapped.getValue("nude"), mapped.getValue("matte")) > 0.01f)
        GlowLooks.all.forEach { look ->
            assertTrue(look.lutIntensity in 0f..1f)
            LookLut.gradeFor(look.id)
        }
    }

    @Test
    fun unknownLookUsesNaturalGrade() {
        assertEquals(LookLut.gradeFor("natural"), LookLut.gradeFor("not-a-look"))
    }

    @Test
    fun rgbaUploadBufferHasFourBytesPerPixel() {
        val pixels = intArrayOf(LookLut.packRgb(1f, 0f, 0f), LookLut.packRgb(0f, 1f, 0f))
        val buffer = LookLut.toRgbaBytes(pixels)
        assertEquals(8, buffer.remaining())
        assertEquals(255.toByte(), buffer.get())
        assertEquals(0.toByte(), buffer.get())
        assertEquals(0.toByte(), buffer.get())
        assertEquals(255.toByte(), buffer.get())
    }

    @Test
    fun applyGradeClampsOutOfRangeInput() {
        val mapped = LookLut.applyGrade(-1f, 2f, 0.5f, LookLut.Grade())
        mapped.forEach { channel ->
            assertTrue(channel in 0f..1f)
        }
    }

    private fun lookDistance(a: FloatArray, b: FloatArray): Float =
        abs(a[0] - b[0]) + abs(a[1] - b[1]) + abs(a[2] - b[2])

    private fun assertRgbClose(expected: FloatArray, actual: FloatArray, delta: Float) {
        assertArrayEquals(expected, actual, delta)
    }
}
