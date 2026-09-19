package com.nyxiaglow.app.camera

import com.nyxiaglow.app.ui.GlowLooks
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BeautyControlsTest {
    @Test
    fun defaultControlsAreIdentity() {
        val controls = BeautyControls().clamped()
        assertEquals(0f, controls.skinSmooth, 0.0001f)
        assertEquals(0f, controls.skinGlow, 0.0001f)
        assertEquals(0f, controls.lipIntensity, 0.0001f)
        assertEquals(0f, controls.blushIntensity, 0.0001f)
        assertEquals(1f, controls.texturePreservation, 0.0001f)
    }

    @Test
    fun clampedRejectsOutOfRangeAndNonFiniteValues() {
        val controls = BeautyControls(
            skinSmooth = 2f,
            lipIntensity = -1f,
            blushIntensity = Float.NaN,
            exposure = Float.POSITIVE_INFINITY,
            lipColor = floatArrayOf(1.4f, -0.2f),
            blushColor = floatArrayOf()
        ).clamped()

        assertEquals(1f, controls.skinSmooth, 0.0001f)
        assertEquals(0f, controls.lipIntensity, 0.0001f)
        assertEquals(0f, controls.blushIntensity, 0.0001f)
        assertEquals(0f, controls.exposure, 0.0001f)
        assertEquals(3, controls.lipColor.size)
        controls.lipColor.forEach { channel -> assertTrue(channel in 0f..1f) }
        assertEquals(3, controls.blushColor.size)
    }

    @Test
    fun equalsUsesColorContents() {
        val left = BeautyControls(lipIntensity = 0.4f, lipColor = floatArrayOf(0.7f, 0.1f, 0.2f))
        val right = BeautyControls(lipIntensity = 0.4f, lipColor = floatArrayOf(0.7f, 0.1f, 0.2f))
        assertEquals(left, right)
        assertEquals(left.hashCode(), right.hashCode())
        assertEquals(BeautyControls(), BeautyControls())
    }

    @Test
    fun glowLooksMapToDistinctShaderControls() {
        val natural = GlowLooks.natural.toBeautyControls().clamped()
        val ruby = GlowLooks.ruby.toBeautyControls().clamped()
        assertTrue(natural.skinSmooth > 0f)
        assertTrue(natural.lipIntensity > 0f)
        assertNotEquals(natural.skinSmooth, ruby.skinSmooth)
        assertNotEquals(natural.lipIntensity, ruby.lipIntensity)
        assertArrayEquals(GlowLooks.hexToRgb(GlowLooks.ruby.lipHex), ruby.lipColor, 0.001f)
    }

    @Test
    fun zeroIntensityKeepsFallbackColorsButNoMix() {
        val controls = GlowLooks.glam.toBeautyControls().copy(lipIntensity = 0f, blushIntensity = 0f, skinSmooth = 0f, skinGlow = 0f).clamped()
        assertEquals(0f, controls.lipIntensity, 0.0001f)
        assertEquals(0f, controls.skinSmooth, 0.0001f)
        assertTrue(controls.lipColor[0] > 0f)
    }
}
