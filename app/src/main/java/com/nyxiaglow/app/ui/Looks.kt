package com.nyxiaglow.app.ui

import com.nyxiaglow.app.camera.BeautyControls

data class GlowLook(
    val id: String,
    val title: String,
    val lipHex: String,
    val blushHex: String,
    val lipStrength: Float,
    val blushStrength: Float,
    val smooth: Float,
    val glow: Float,
    val lutIntensity: Float
) {
    fun toBeautyControls(texturePreservation: Float = 1f): BeautyControls = BeautyControls(
        skinSmooth = smooth,
        skinGlow = glow,
        texturePreservation = texturePreservation,
        lipIntensity = lipStrength,
        lipColor = GlowLooks.hexToRgb(lipHex),
        blushIntensity = blushStrength,
        blushColor = GlowLooks.hexToRgb(blushHex)
    )
}

object GlowLooks {
    val all: List<GlowLook> = listOf(
        GlowLook("natural", "Natural", "#C4787A", "#E8A0A0", 0.35f, 0.20f, 0.20f, 0.15f, 0.32f),
        GlowLook("nude", "Nude", "#C08A7A", "#E0B0A0", 0.40f, 0.22f, 0.25f, 0.18f, 0.42f),
        GlowLook("glam", "Glam", "#9B1B30", "#D96B6B", 0.70f, 0.28f, 0.35f, 0.30f, 0.68f),
        GlowLook("ruby", "Ruby", "#D21F3C", "#E07070", 0.72f, 0.22f, 0.30f, 0.25f, 0.62f),
        GlowLook("matte", "Matte", "#8B3A3A", "#C47A7A", 0.55f, 0.18f, 0.40f, 0.10f, 0.55f),
        GlowLook("dewy", "Dewy", "#B85C38", "#E09A8A", 0.45f, 0.30f, 0.22f, 0.40f, 0.48f)
    )

    val natural get() = byId("natural")
    val nude get() = byId("nude")
    val glam get() = byId("glam")
    val ruby get() = byId("ruby")
    val matte get() = byId("matte")
    val dewy get() = byId("dewy")

    fun byId(id: String): GlowLook = all.firstOrNull { it.id == id } ?: all.first()

    fun hexToRgb(hex: String): FloatArray {
        val normalized = hex.trim().removePrefix("#")
        if (normalized.length != 6) return floatArrayOf(0.8f, 0.2f, 0.3f)
        return try {
            val value = normalized.toInt(16)
            floatArrayOf(
                ((value shr 16) and 0xFF) / 255f,
                ((value shr 8) and 0xFF) / 255f,
                (value and 0xFF) / 255f
            )
        } catch (_: NumberFormatException) {
            floatArrayOf(0.8f, 0.2f, 0.3f)
        }
    }
}
