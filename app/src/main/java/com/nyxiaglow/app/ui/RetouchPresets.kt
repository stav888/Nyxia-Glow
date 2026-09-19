package com.nyxiaglow.app.ui

data class RetouchPreset(
    val name: String,
    val lipHex: String,
    val blushHex: String,
    val lipStrength: Float,
    val blushStrength: Float
) {
    val lipRgb: FloatArray get() = hexToRgb(lipHex)
    val blushRgb: FloatArray get() = hexToRgb(blushHex)
}

val RETOUCH_PRESETS: Map<String, RetouchPreset> = listOf(
    // Smooth keeps the historical hardcoded shader look: lip (0.8, 0.2, 0.3),
    // blush (1.0, 0.4, 0.4) at the default glow strength.
    RetouchPreset("Smooth", "#CC334D", "#FF6666", 0.68f, 0.68f),
    RetouchPreset("Freckles", "#B76E79", "#E89090", 0.50f, 0.40f),
    RetouchPreset("Matte", "#8E2A3C", "#C47A7A", 0.85f, 0.35f),
    RetouchPreset("Dewy", "#D96A7E", "#FF8A8A", 0.60f, 0.80f),
    RetouchPreset("Refine", "#A9505C", "#D08080", 0.70f, 0.50f)
).associateBy { it.name }

fun retouchPreset(name: String): RetouchPreset =
    RETOUCH_PRESETS[name] ?: RETOUCH_PRESETS.getValue("Smooth")

fun hexToRgb(hex: String): FloatArray {
    val clean = hex.trim().removePrefix("#")
    if (clean.length != 6) return floatArrayOf(0.8f, 0.2f, 0.3f)
    return try {
        floatArrayOf(
            clean.substring(0, 2).toInt(16) / 255f,
            clean.substring(2, 4).toInt(16) / 255f,
            clean.substring(4, 6).toInt(16) / 255f
        )
    } catch (_: NumberFormatException) {
        floatArrayOf(0.8f, 0.2f, 0.3f)
    }
}
