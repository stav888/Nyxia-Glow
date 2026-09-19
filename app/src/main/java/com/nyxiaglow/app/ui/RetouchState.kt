package com.nyxiaglow.app.ui

data class RetouchState(
    val preserveTexture: Boolean = true,
    val smoothingIntensity: Float = 0.45f,
    val selectedTool: String = "Skin",
    val selectedPreset: String = "natural",
    val lipIntensity: Float = 0.35f,
    val blushIntensity: Float = 0.20f
) {
    fun reset(): RetouchState = RetouchState()
}

fun retouchApplyMessage(hasSourceImage: Boolean): String = if (hasSourceImage) {
    "Retouch applied to live preview; source image unchanged"
} else {
    "No source image; retouch applied to live preview"
}
