package rapiddecoder

import android.graphics.Bitmap
import rapiddecoder.util.BitmapUtils

internal class ScaleToTransformLoader(private val other: BitmapLoader,
                                      private val targetWidth: Float,
                                      private val targetHeight: Float) : BitmapLoader() {
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
                ScaleToTransformLoader(other, floatWidth, floatHeight)
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
                ScaleToTransformLoader(other, newWidth, newHeight)
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

    override fun loadBitmap(options: LoadBitmapOptions): Bitmap {
        val newOptions = options.buildUpon()
                .setFinalScale(false)
                .setMutable(false)
                .setConfig(null)
                .build()
        val sourceBitmap = other.loadBitmap(newOptions)
        val finalWidth = Math.ceil(targetWidth.toDouble()).toInt()
        val finalHeight = Math.ceil(targetHeight.toDouble()).toInt()
        val scaledBitmap = if (options.shouldBeRedrawnFrom(sourceBitmap)) {
            BitmapUtils.copy(sourceBitmap, null, finalWidth, finalHeight,
                    options.config ?: sourceBitmap.config, options.filterBitmap)
        } else {
            Bitmap.createScaledBitmap(sourceBitmap, finalWidth, finalHeight,
                    options.filterBitmap)
        }
        if (sourceBitmap !== scaledBitmap) {
            sourceBitmap.recycle()
        }
        return scaledBitmap
    }
}