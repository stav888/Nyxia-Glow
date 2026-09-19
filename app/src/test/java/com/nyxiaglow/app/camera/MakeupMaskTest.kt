package com.nyxiaglow.app.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MakeupMaskTest {
    @Test
    fun smoothPolygonDoublesPointsPerIteration() {
        val square = listOf(0f to 0f, 1f to 0f, 1f to 1f, 0f to 1f)

        assertEquals(8, smoothClosedPolygon(square, iterations = 1).size)
        assertEquals(16, smoothClosedPolygon(square, iterations = 2).size)
    }

    @Test
    fun smoothPolygonKeepsCentroid() {
        val square = listOf(0f to 0f, 2f to 0f, 2f to 2f, 0f to 2f)
        val smoothed = smoothClosedPolygon(square, iterations = 3)
        val cx = smoothed.sumOf { it.first.toDouble() } / smoothed.size
        val cy = smoothed.sumOf { it.second.toDouble() } / smoothed.size

        assertEquals(1.0, cx, 0.001)
        assertEquals(1.0, cy, 0.001)
    }

    @Test
    fun smoothPolygonPassesThroughSmallInputs() {
        assertTrue(smoothClosedPolygon(emptyList()).isEmpty())
        val two = listOf(0f to 0f, 1f to 1f)
        assertEquals(two, smoothClosedPolygon(two))
    }

    @Test
    fun oneEuroFilterSnapsOnFirstSample() {
        val filter = OneEuroFilter()

        assertEquals(0.42f, filter.filter(0.42f, 0f), 0.0001f)
    }

    @Test
    fun oneEuroFilterConvergesToConstant() {
        val filter = OneEuroFilter()
        var out = filter.filter(0f, 0f)
        repeat(50) { i -> out = filter.filter(1f, 0.1f * (i + 1)) }

        assertEquals(1f, out, 0.05f)
    }

    @Test
    fun oneEuroFilterAttenuatesJitter() {
        val filter = OneEuroFilter()
        var rawError = 0f
        var filteredError = 0f
        repeat(40) { i ->
            val t = 0.1f * i
            val noisy = 0.5f + (if (i % 2 == 0) 0.02f else -0.02f)
            val out = filter.filter(noisy, t)
            rawError += abs(noisy - 0.5f)
            filteredError += abs(out - 0.5f)
        }

        assertTrue("filtered error $filteredError should be below raw $rawError", filteredError < rawError)
    }

    @Test
    fun oneEuroFilterResetSnapsAgain() {
        val filter = OneEuroFilter()
        filter.filter(0.1f, 0f)
        filter.reset()

        assertEquals(0.9f, filter.filter(0.9f, 1f), 0.0001f)
    }

    @Test
    fun landmarkStabilizerFiltersEachAxis() {
        val stabilizer = LandmarkStabilizer()
        val (x, y) = stabilizer.filter(61, 0.4f, 0.6f, 0f)

        assertEquals(0.4f, x, 0.0001f)
        assertEquals(0.6f, y, 0.0001f)
    }
}
