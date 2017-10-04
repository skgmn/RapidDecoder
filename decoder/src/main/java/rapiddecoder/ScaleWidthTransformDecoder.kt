package rapiddecoder

import android.graphics.Bitmap

internal class ScaleWidthTransformDecoder(private val other: BitmapDecoder,
                                          private val targetWidth: Float,
                                          private val heightAdjustRatio: Float) : BitmapDecoder() {
    override val width: Int
        get() = Math.round(targetWidth)
    override val height: Int
        get() = Math.round(targetHeight)
    override val hasSize: Boolean
        get() = other.hasSize
    override val sourceWidth: Int
        get() = other.sourceWidth
    override val sourceHeight: Int
        get() = other.sourceHeight
    override val mimeType: String?
        get() = other.mimeType
    override val densityRatio: Float
        get() = other.densityRatio

    private val targetHeight: Float by lazy {
        targetWidth * (other.height.toFloat() / other.width) * heightAdjustRatio
    }

    override fun scaleTo(width: Int, height: Int): BitmapLoader {
        checkScaleToArguments(width, height)
        return if (other.hasSize && other.width == width && other.height == height) {
            other
        } else {
            ScaleToTransformDecoder(other, width.toFloat(), height.toFloat())
        }
    }

    override fun scaleWidth(width: Int): BitmapLoader {
        checkScaleToArguments(width, 1)
        return if (other.hasSize && other.width == width) {
            other
        } else {
            val floatWidth = width.toFloat()
            if (floatWidth == targetWidth) {
                this
            } else {
                ScaleWidthTransformDecoder(other, floatWidth, heightAdjustRatio)
            }
        }
    }

    override fun scaleBy(x: Float, y: Float): BitmapLoader {
        checkScaleByArguments(x, y)
        return if (x == 1f && y == 1f) {
            this
        } else if (other.hasSize) {
            val newWidth = targetWidth * x
            val newHeight = targetHeight * y
            if (other.width.toFloat() == newWidth && other.height.toFloat() == newHeight) {
                other
            } else {
                ScaleToTransformDecoder(other, newWidth, newHeight)
            }
        } else {
            val newWidth = targetWidth * x
            val newHeightAdjustRatio = heightAdjustRatio * (y / x)
            ScaleWidthTransformDecoder(other, newWidth, newHeightAdjustRatio)
        }
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        val scale = targetWidth / other.width
        return other.region(
                Math.round(left / scale),
                Math.round(top / scale),
                Math.round(right / scale),
                Math.round(bottom / scale))
                .scaleTo(right - left, bottom - top)
    }

    override fun decode(state: BitmapDecodeState): Bitmap {
        val scale = targetWidth / other.sourceWidth

        state.densityScale = false
        state.scaleX *= scale
        state.scaleY *= scale

        val bitmap = synchronized(other.decodeLock) { other.decode(state) }
        val width = width
        if (bitmap.width == width || !state.finalScale) {
            return bitmap
        }

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, state.filterBitmap)
        if (scaledBitmap !== bitmap) {
            bitmap.recycle()
        }
        return scaledBitmap
    }
}