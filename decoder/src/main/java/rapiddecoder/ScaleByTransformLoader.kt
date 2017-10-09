package rapiddecoder

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

internal class ScaleByTransformLoader(private val source: BitmapLoader,
                                      private val x: Float,
                                      private val y: Float) : BitmapLoader() {
    override val width: Int by lazy {
        Math.round(source.width * x)
    }
    override val height: Int by lazy {
        Math.round(source.height * y)
    }
    override val hasSize: Boolean
        get() = source.hasSize
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
            ScaleToTransformLoader(source, width.toFloat(), height.toFloat())
        }
    }

    override fun scaleBy(x: Float, y: Float): BitmapLoader {
        checkScaleByArguments(x, y)
        return if (x == 1f && y == 1f) {
            this
        } else {
            val newX = this.x * x
            val newY = this.y * y
            if (newX == 1f && newY == 1f) {
                source
            } else {
                ScaleByTransformLoader(source, newX, newY)
            }
        }
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        return source.region(
                Math.round(left / x),
                Math.round(top / y),
                Math.round(right / x),
                Math.round(bottom / y))
                .scaleTo(right - left, bottom - top)
    }

    override fun loadBitmap(options: LoadBitmapOptions): Bitmap {
        val newOptions = options.buildUpon()
                .setFinalScale(false)
                .setMutable(false)
                .setConfig(null)
                .build()
        val sourceBitmap = source.loadBitmap(newOptions)
        val finalWidth = Math.ceil(sourceBitmap.width.toDouble() * x).toInt()
        val finalHeight = Math.ceil(sourceBitmap.height.toDouble() * y).toInt()
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