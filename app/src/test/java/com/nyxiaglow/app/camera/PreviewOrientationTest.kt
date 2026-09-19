package com.nyxiaglow.app.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewOrientationTest {
    @Test
    fun frontCameraMirrorsAndFlipsY() {
        val config = PreviewOrientation.forFrontFacing(true)
        assertTrue(config.mirrorX)
        assertTrue(config.flipY)
        assertEquals(1f, config.mirrorAmount, 0f)
        assertEquals(-1f, config.flipYSign, 0f)
    }

    @Test
    fun backCameraDoesNotMirrorOrFlipY() {
        val config = PreviewOrientation.forFrontFacing(false)
        assertFalse(config.mirrorX)
        assertFalse(config.flipY)
        assertEquals(0f, config.mirrorAmount, 0f)
        assertEquals(1f, config.flipYSign, 0f)
    }

    @Test
    fun frontAndBackPresetsMatchFactory() {
        assertEquals(PreviewOrientation.Front, PreviewOrientation.forFrontFacing(true))
        assertEquals(PreviewOrientation.Back, PreviewOrientation.forFrontFacing(false))
    }

    @Test
    fun cropScaleFillsPortraitViewFromLandscapeBuffer() {
        val (x, y) = PreviewCrop.scale(viewWidth = 1080, viewHeight = 2400, textureWidth = 1920, textureHeight = 1080)
        assertTrue("crop X $x should enlarge to fill", x > 1f)
        assertEquals(1f, y, 0.0001f)
    }

    @Test
    fun cropScaleFillsWideViewFromTallBuffer() {
        val (x, y) = PreviewCrop.scale(viewWidth = 2400, viewHeight = 1080, textureWidth = 1080, textureHeight = 1920)
        assertEquals(1f, x, 0.0001f)
        assertTrue("crop Y $y should enlarge to fill", y > 1f)
    }
}
