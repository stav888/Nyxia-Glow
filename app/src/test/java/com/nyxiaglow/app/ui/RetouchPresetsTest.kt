package com.nyxiaglow.app.ui

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class RetouchPresetsTest {
    @Test
    fun hexToRgbParsesPrimaryColors() {
        assertArrayEquals(floatArrayOf(1f, 0f, 0f), hexToRgb("#FF0000"), 0.001f)
        assertArrayEquals(floatArrayOf(0f, 1f, 0f), hexToRgb("#00FF00"), 0.001f)
        assertArrayEquals(floatArrayOf(0f, 0f, 1f), hexToRgb("#0000FF"), 0.001f)
    }

    @Test
    fun hexToRgbFallsBackOnBadInput() {
        val fallback = floatArrayOf(0.8f, 0.2f, 0.3f)
        assertArrayEquals(fallback, hexToRgb("not-a-color"), 0.001f)
        assertArrayEquals(fallback, hexToRgb("#ZZZZZZ"), 0.001f)
        assertArrayEquals(fallback, hexToRgb("#FFF"), 0.001f)
    }

    @Test
    fun allPresetsResolveWithValidRanges() {
        listOf("Smooth", "Freckles", "Matte", "Dewy", "Refine").forEach { name ->
            val preset = retouchPreset(name)
            assertEquals(name, preset.name)
            preset.lipRgb.forEach { channel ->
                assertEquals(true, channel in 0f..1f)
            }
            preset.blushRgb.forEach { channel ->
                assertEquals(true, channel in 0f..1f)
            }
            assertEquals(true, preset.lipStrength in 0f..1f)
            assertEquals(true, preset.blushStrength in 0f..1f)
        }
    }

    @Test
    fun unknownPresetFallsBackToSmooth() {
        assertEquals("Smooth", retouchPreset("Nope").name)
    }

    @Test
    fun smoothPresetKeepsLegacyShaderLook() {
        val smooth = retouchPreset("Smooth")
        assertArrayEquals(floatArrayOf(0.8f, 0.2f, 0.3f), smooth.lipRgb, 0.01f)
        assertArrayEquals(floatArrayOf(1.0f, 0.4f, 0.4f), smooth.blushRgb, 0.01f)
    }
}
