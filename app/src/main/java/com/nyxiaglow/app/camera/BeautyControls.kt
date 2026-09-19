package com.nyxiaglow.app.camera

data class BeautyControls(
    val skinSmooth: Float = 0f,
    val skinGlow: Float = 0f,
    val skinWhitening: Float = 0f,
    val texturePreservation: Float = 1f,

    val lipIntensity: Float = 0f,
    val lipColor: FloatArray = floatArrayOf(0.75f, 0.20f, 0.30f),

    val blushIntensity: Float = 0f,
    val blushColor: FloatArray = floatArrayOf(0.85f, 0.35f, 0.40f),

    val eyeEnhancement: Float = 0f,
    val teethWhitening: Float = 0f,

    val faceSlim: Float = 0f,
    val jawRefinement: Float = 0f,
    val noseRefinement: Float = 0f,

    val sharpness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val exposure: Float = 0f,
    val temperature: Float = 0f
) {
    fun clamped(): BeautyControls {
        fun clamp(value: Float) = if (value.isFinite()) value.coerceIn(0f, 1f) else 0f
        return copy(
            skinSmooth = clamp(skinSmooth),
            skinGlow = clamp(skinGlow),
            skinWhitening = clamp(skinWhitening),
            texturePreservation = clamp(texturePreservation),
            lipIntensity = clamp(lipIntensity),
            lipColor = clampRgb(lipColor, 0.75f, 0.20f, 0.30f),
            blushIntensity = clamp(blushIntensity),
            blushColor = clampRgb(blushColor, 0.85f, 0.35f, 0.40f),
            eyeEnhancement = clamp(eyeEnhancement),
            teethWhitening = clamp(teethWhitening),
            faceSlim = clamp(faceSlim),
            jawRefinement = clamp(jawRefinement),
            noseRefinement = clamp(noseRefinement),
            sharpness = clamp(sharpness),
            contrast = clamp(contrast),
            saturation = clamp(saturation),
            exposure = clamp(exposure),
            temperature = clamp(temperature)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BeautyControls) return false
        return skinSmooth == other.skinSmooth &&
            skinGlow == other.skinGlow &&
            skinWhitening == other.skinWhitening &&
            texturePreservation == other.texturePreservation &&
            lipIntensity == other.lipIntensity &&
            lipColor.contentEquals(other.lipColor) &&
            blushIntensity == other.blushIntensity &&
            blushColor.contentEquals(other.blushColor) &&
            eyeEnhancement == other.eyeEnhancement &&
            teethWhitening == other.teethWhitening &&
            faceSlim == other.faceSlim &&
            jawRefinement == other.jawRefinement &&
            noseRefinement == other.noseRefinement &&
            sharpness == other.sharpness &&
            contrast == other.contrast &&
            saturation == other.saturation &&
            exposure == other.exposure &&
            temperature == other.temperature
    }

    override fun hashCode(): Int {
        var result = skinSmooth.hashCode()
        result = 31 * result + skinGlow.hashCode()
        result = 31 * result + skinWhitening.hashCode()
        result = 31 * result + texturePreservation.hashCode()
        result = 31 * result + lipIntensity.hashCode()
        result = 31 * result + lipColor.contentHashCode()
        result = 31 * result + blushIntensity.hashCode()
        result = 31 * result + blushColor.contentHashCode()
        result = 31 * result + eyeEnhancement.hashCode()
        result = 31 * result + teethWhitening.hashCode()
        result = 31 * result + faceSlim.hashCode()
        result = 31 * result + jawRefinement.hashCode()
        result = 31 * result + noseRefinement.hashCode()
        result = 31 * result + sharpness.hashCode()
        result = 31 * result + contrast.hashCode()
        result = 31 * result + saturation.hashCode()
        result = 31 * result + exposure.hashCode()
        result = 31 * result + temperature.hashCode()
        return result
    }

    companion object {
        fun clampRgb(source: FloatArray?, fallbackR: Float, fallbackG: Float, fallbackB: Float): FloatArray {
            val out = floatArrayOf(fallbackR, fallbackG, fallbackB)
            if (source == null) return out
            if (source.isNotEmpty()) out[0] = source[0].coerceIn(0f, 1f)
            if (source.size > 1) out[1] = source[1].coerceIn(0f, 1f)
            if (source.size > 2) out[2] = source[2].coerceIn(0f, 1f)
            return out
        }
    }
}
