package com.nyxiaglow.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetouchStateTest {
    @Test
    fun defaultStateIsValid() {
        val state = RetouchState()

        assertTrue(state.preserveTexture)
        assertEquals(0.45f, state.smoothingIntensity, 0.0001f)
        assertEquals("Skin", state.selectedTool)
        assertEquals("natural", state.selectedPreset)
        assertEquals(0.35f, state.lipIntensity, 0.0001f)
        assertEquals(0.20f, state.blushIntensity, 0.0001f)
    }

    @Test
    fun resetRestoresDefaults() {
        val changed = RetouchState(false, 0.9f, "Makeup", "Dewy")

        assertEquals(RetouchState(), changed.reset())
    }

    @Test
    fun applyMessageDescribesPreviewOnlyBehavior() {
        assertTrue(retouchApplyMessage(false).contains("No source image"))
        assertTrue(retouchApplyMessage(true).contains("source image unchanged"))
    }
}