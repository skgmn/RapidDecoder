package rapiddecoder

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

internal class ScaleToTransformLoader(private val source: BitmapLoader,
                                      private val targetWidth: Float,
                                      private val targetHeight: Float) : BitmapLoader() {
    override val width: Int
        get() = Math.round(targetWidth)
    override val height: Int
        get() = Math.round(targetHeight)
    override val hasSize: Boolean
        get() = true
    override val sourceWidth: Int
        get() = source.sourceWidth
    override val sourceHeight: Int
        get() = source.sourceHeight
    override val mimeType: String?
        get() = source.mimeType

    override fun scaleTo(width: Int, height: Int): BitmapLoader {
        checkScaleToArguments(width, height)
        return if (source.hasSize && source.width == width && source.height == height) {
            source
        } else {
            val floatWidth = width.toFloat()
            val floatHeight = height.toFloat()
            if (floatWidth == targetWidth && floatHeight == targetHeight) {
                this
            } else {
                ScaleToTransformLoader(source, floatWidth, floatHeight)
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
            if (source.hasSize &&
                    source.width.toFloat() == newWidth &&
                    source.height.toFloat() == newHeight) {
                source
            } else {
                ScaleToTransformLoader(source, newWidth, newHeight)
            }
        }
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        val sx = targetWidth / source.width
        val sy = targetHeight / source.height
        return source.region(
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
        val sourceBitmap = source.loadBitmap(newOptions)
        val finalWidth = Math.ceil(targetWidth.toDouble()).toInt()
        val finalHeight = Math.ceil(targetHeight.toDouble()).toInt()
        val scaledBitmap = if (options.shouldBeRedrawnFrom(sourceBitmap)) {
            Bitmap.createBitmap(finalWidth, finalHeight,
                    options.config ?: sourceBitmap.config).also { scaledBitmap ->
                val canvas = Canvas(scaledBitmap)
                val paint = if (options.filterBitmap) {
                    Paint(Paint.FILTER_BITMAP_FLAG)
                } else {
                    null
                }
                canvas.drawBitmap(sourceBitmap, null,
                        Rect(0, 0, scaledBitmap.width, scaledBitmap.height), paint)
            }
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