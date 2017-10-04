package rapiddecoder

import android.graphics.Bitmap

internal class ScaleToTransformDecoder(private val other: BitmapDecoder,
                                       private val targetWidth: Float,
                                       private val targetHeight: Float) : BitmapDecoder() {
    override val width: Int
        get() = Math.round(targetWidth)
    override val height: Int
        get() = Math.round(targetHeight)
    override val hasSize: Boolean
        get() = true
    override val sourceWidth: Int
        get() = other.sourceWidth
    override val sourceHeight: Int
        get() = other.sourceHeight
    override val mimeType: String?
        get() = other.mimeType
    override val densityRatio: Float
        get() = other.densityRatio

    override fun scaleTo(width: Int, height: Int): BitmapLoader {
        checkScaleToArguments(width, height)
        return if (other.hasSize && other.width == width && other.height == height) {
            other
        } else {
            val floatWidth = width.toFloat()
            val floatHeight = height.toFloat()
            if (floatWidth == targetWidth && floatHeight == targetHeight) {
                this
            } else {
                ScaleToTransformDecoder(other, floatWidth, floatHeight)
            }
        }
    }

    override fun scaleBy(x: Float, y: Float): BitmapLoader {
        checkScaleByArguments(x, y)
        return if (x == 1f && y == 1f) {
            this
        } else {
            val newWidth = targetWidth * x
            val newHeight = targetHeight * y
            if (other.hasSize &&
                    other.width.toFloat() == newWidth &&
                    other.height.toFloat() == newHeight) {
                other
            } else {
                ScaleToTransformDecoder(other, newWidth, newHeight)
            }
        }
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        val sx = targetWidth / other.width
        val sy = targetHeight / other.height
        return other.region(
                Math.round(left / sx),
                Math.round(top / sy),
                Math.round(right / sx),
                Math.round(bottom / sy))
                .scaleTo(right - left, bottom - top)
    }

    override fun decode(state: BitmapDecodeState): Bitmap {
        state.densityScale = false
        state.scaleX *= targetWidth / other.sourceWidth.toFloat()
        state.scaleY *= targetHeight / other.sourceHeight.toFloat()

        val bitmap = synchronized(other.decodeLock) { other.decode(state) }

        val targetWidth = Math.round(targetWidth)
        val targetHeight = Math.round(targetHeight)
        if (bitmap.width == targetWidth && bitmap.height == targetHeight
                || !state.finalScale) {
            return bitmap
        }

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight,
                state.filterBitmap)
        if (scaledBitmap !== bitmap) {
            bitmap.recycle()
        }
        return scaledBitmap
    }
}