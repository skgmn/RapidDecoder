package rapiddecoder

import android.graphics.Bitmap

internal class ScaleHeightTransformDecoder(private val other: BitmapDecoder,
                                           private val targetHeight: Float,
                                           private val widthAdjustRatio: Float) : BitmapDecoder() {
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

    private val targetWidth: Float by lazy {
        targetHeight * (other.width.toFloat() / other.height) * widthAdjustRatio
    }

    override fun scaleTo(width: Int, height: Int): BitmapLoader {
        checkScaleToArguments(width, height)
        return if (other.hasSize && other.width == width && other.height == height) {
            other
        } else {
            ScaleToTransformDecoder(other, width.toFloat(), height.toFloat())
        }
    }

    override fun scaleHeight(height: Int): BitmapLoader {
        checkScaleToArguments(1, height)
        return if (other.hasSize && other.height == height) {
            other
        } else {
            val floatHeight = height.toFloat()
            if (floatHeight == targetHeight) {
                this
            } else {
                ScaleHeightTransformDecoder(other, floatHeight, widthAdjustRatio)
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
            val newHeight = targetHeight * y
            val newWidthAdjustRatio = widthAdjustRatio * (x / y)
            ScaleHeightTransformDecoder(other, newHeight, newWidthAdjustRatio)
        }
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        val scale = targetHeight / other.height
        return other.region(
                Math.round(left / scale),
                Math.round(top / scale),
                Math.round(right / scale),
                Math.round(bottom / scale))
                .scaleTo(right - left, bottom - top)
    }

    override fun decode(state: BitmapDecodeState): Bitmap {
        val scale = targetHeight / other.sourceHeight

        state.densityScale = false
        state.scaleX *= scale
        state.scaleY *= scale

        val bitmap = synchronized(other.decodeLock) { other.decode(state) }
        val height = height
        if (bitmap.height == height || !state.finalScale) {
            return bitmap
        }

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, state.filterBitmap)
        if (scaledBitmap !== bitmap) {
            bitmap.recycle()
        }
        return scaledBitmap
    }
}