package com.nyxiaglow.app.camera

data class PreviewOrientation(
    val mirrorX: Boolean,
    val flipY: Boolean
) {
    val flipYSign: Float get() = if (flipY) -1f else 1f
    val mirrorAmount: Float get() = if (mirrorX) 1f else 0f

    companion object {
        val Front = PreviewOrientation(mirrorX = true, flipY = true)
        val Back = PreviewOrientation(mirrorX = false, flipY = false)

        fun forFrontFacing(isFrontFacing: Boolean): PreviewOrientation =
            if (isFrontFacing) Front else Back
    }
}

internal object PreviewCrop {
    fun scale(viewWidth: Int, viewHeight: Int, textureWidth: Int, textureHeight: Int): Pair<Float, Float> {
        val viewAspect = viewWidth.coerceAtLeast(1).toFloat() / viewHeight.coerceAtLeast(1).toFloat()
        val contentAspect = if (textureWidth >= textureHeight) {
            textureHeight.toFloat() / textureWidth.toFloat()
        } else {
            textureWidth.toFloat() / textureHeight.toFloat()
        }
        return if (contentAspect > viewAspect) {
            contentAspect / viewAspect to 1f
        } else {
            1f to viewAspect / contentAspect
        }
    }
}
